package com.aq.aqconnect;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class AQConnectBuildProcess implements BuildProcess{
    private final AgentRunningBuild myBuild;
    private final BuildRunnerContext myContext;
    private BuildProgressLogger buildLogger;

    public static final String APP_URL = "appURL";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String PROJECT_NAME = "projectName";
    public static final String JOB_PID = "jobPid";
    public static final String RUN_PARAM_STR = "runParamStr";
    public long passCount = 0, failCount = 0, runningCount = 0, totalCount = 0, notRunCount = 0;
    public String resultAccessURL = null;
    BuildFinishedStatus buildStatus = BuildFinishedStatus.FINISHED_SUCCESS;

    public AQConnectBuildProcess(@NotNull AgentRunningBuild build, @NotNull BuildRunnerContext context){
        myBuild = build;
        myContext = context;
    }

    @Override
    public void start() throws RunBuildException {
        buildLogger = myBuild.getBuildLogger();
        final AQPluginLogger logger = new AQPluginLogger(buildLogger);
        Map<String, String> conf = new HashMap<String, String>(myContext.getRunnerParameters());
        boolean isJobFailed = false, isConnectionFailed = false, isAgentNotAvailable = false;
        //plugin main logic starts here
        try {
            logger.println("**************************************");
            logger.println("*** ACCELQ PLUGIN PROCESSING START ***");
            logger.println("**************************************");
            logger.println();

            //login via AQ REST client
            AQPluginRESTClient aqPluginRESTClient = AQPluginRESTClient.getInstance();
            aqPluginRESTClient.setUpBaseURL(conf.get(APP_URL).trim());
            if(aqPluginRESTClient.doLogin(conf.get(USERNAME), conf.get(PASSWORD), conf.get(PROJECT_NAME))) {
                logger.println(AQPluginConstants.LOG_DELIMITER + "Connection Successful");
                logger.println();
                String runParamJsonPayload = getRunParamJsonPayload(conf.get(RUN_PARAM_STR));
                JSONObject realJobObj = aqPluginRESTClient.triggerJob(Integer.parseInt(conf.get(JOB_PID)), runParamJsonPayload);
                if(realJobObj.get("cause") != null) {
                    //throw new AQPluginException((String) realJobObj.get("cause"));
                    buildStatus = BuildFinishedStatus.FINISHED_FAILED;
                }
                long realJobPid = (Long) realJobObj.get("pid");
                String jobPurpose = (String) realJobObj.get("purpose");
                String jobStatus = "";
                resultAccessURL = aqPluginRESTClient.getResultExternalAccessURL(Long.toString(realJobPid));
                JSONObject summaryObj;
                int attempt = 0;
                logger.println("Purpose: " + jobPurpose);
                logger.println();
                do {
                    summaryObj = aqPluginRESTClient.getJobSummary(realJobPid);
                    if(summaryObj.get("cause") != null) {
                        //throw new AQPluginException((String) summaryObj.get("cause"));
                        buildStatus = BuildFinishedStatus.FINISHED_FAILED;
                    }
                    if(summaryObj.get("summary") != null) {
                        summaryObj = (JSONObject) summaryObj.get("summary");
                    }
                    passCount = (Long) summaryObj.get("pass");
                    failCount = (Long) summaryObj.get("fail");
                    notRunCount = (Long) summaryObj.get("notRun");
                    logger.println("Status: " + summaryObj.get("status"));
                    logger.println("Pass: " + passCount);
                    logger.println("Fail: " + failCount);
                    //logger.println("Running: " + runningCount);
                    logger.println("Not Run: " + notRunCount);
                    logger.println();
                    jobStatus = ((String) summaryObj.get("status")).toUpperCase();
                    //customBuildData.put(AQPluginConstants.AQ_RESULT_INFO_KEY, prepareAQBuildData(((String) summaryObj.get("status")), passCount, failCount, notRunCount, resultAccessURL));
                    if(jobStatus.equals(AQPluginConstants.TEST_JOB_STATUS.SCHEDULED.getStatus().toUpperCase()))
                        ++attempt;
                    if(attempt == AQPluginConstants.JOB_PICKUP_RETRY_COUNT) {
                        //throw new AQPluginException("No agent available to pickup the job");
                        buildStatus = BuildFinishedStatus.FINISHED_FAILED;
                        buildLogger.error(AQPluginConstants.LOG_DELIMITER + "No agent available to pickup the job");
                        buildLogger.logMessage(DefaultMessagesInfo.createTextMessage(String.format("##teamcity[buildStatus status='%s' text='%s']", "FAILURE", "No agent available to pickup the job")));
                        isAgentNotAvailable = true;
                        break;
                    }
                    Thread.sleep(AQPluginConstants.JOB_STATUS_POLL_TIME);
                } while(!jobStatus.equals(AQPluginConstants.TEST_JOB_STATUS.COMPLETED.getStatus().toUpperCase())
                        && !jobStatus.equals(AQPluginConstants.TEST_JOB_STATUS.ABORTED.getStatus().toUpperCase())
                        && !jobStatus.equals(AQPluginConstants.TEST_JOB_STATUS.FAILED.getStatus().toUpperCase()));

                isJobFailed = (failCount > 0) || jobStatus.equals(AQPluginConstants.TEST_JOB_STATUS.ABORTED.getStatus().toUpperCase()) || jobStatus.equals(AQPluginConstants.TEST_JOB_STATUS.FAILED.getStatus().toUpperCase());
                if(!isJobFailed) {
                    logger.println("Go to " + resultAccessURL + " for a detailed report");
                    logger.println();
                }

                logger.println("**************************************");
                logger.println("**** ACCELQ PLUGIN PROCESSING END ****");
                logger.println("**************************************");
                logger.println();

                //generate final report with app test result report redirection url
                //new AQPluginTestResultReportGenerator().generateReport(taskContext.getRootDirectory().getAbsolutePath(), resultAccessURL);
            } else {
                isConnectionFailed = true;
                buildStatus = BuildFinishedStatus.FINISHED_FAILED;
                buildLogger.error(AQPluginConstants.LOG_DELIMITER + "Connection Failed");
                buildLogger.logMessage(DefaultMessagesInfo.createTextMessage(String.format("##teamcity[buildStatus status='%s' text='%s']", "FAILURE", "Connection Failed")));
            }

            if(!isConnectionFailed && !isAgentNotAvailable) {
                if (isJobFailed) {
                    buildStatus = BuildFinishedStatus.FINISHED_FAILED;
                    buildLogger.error(AQPluginConstants.LOG_DELIMITER + "Job Failed");
                    buildLogger.logMessage(DefaultMessagesInfo.createTextMessage(String.format("##teamcity[buildStatus status='%s' text='%s']", "FAILURE", String.format("Tests Passed: %d, Failure: %d, Not Run: %d\nResult Access Link: %s\n\n", passCount, failCount, notRunCount, resultAccessURL))));
                } else {
                    buildLogger.logMessage(DefaultMessagesInfo.createTextMessage(String.format("##teamcity[buildStatus status='%s' text='%s']", "SUCCESS", String.format("Tests Passed: %d, Failure: %d, Not Run: %d\nResult Access Link: %s\n\n", passCount, failCount, notRunCount, resultAccessURL))));
                }
            }

        }
        catch (Exception exception){
            buildStatus = BuildFinishedStatus.FINISHED_FAILED;
            buildLogger.error(AQPluginConstants.LOG_DELIMITER + "Internal Error");
            buildLogger.logMessage(DefaultMessagesInfo.createTextMessage(String.format("##teamcity[buildStatus status='%s' text='%s']", "FAILURE", "Internal Error")));
        }
    }

    private String getRunParamJsonPayload(String runParamStr) {
        if(runParamStr == null || runParamStr.trim().length() == 0)
            return null;
        JSONObject json = new JSONObject();
        String[] splitOnAmp = runParamStr.split("&");
        for(String split: splitOnAmp) {
            String[] splitOnEquals = split.split("=");
            if(splitOnEquals.length == 2) {
                String key = splitOnEquals[0].trim(), value = splitOnEquals[1].trim();
                if(!key.equals("") && !value.equals("")) {
                    json.put(key, value);
                }
            }
        }
        return json.toJSONString();
    }

    @Override
    public boolean isInterrupted() {
        return false;
    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void interrupt() {
    }

    @NotNull
    @Override
    public BuildFinishedStatus waitFor() throws RunBuildException {
        return buildStatus;
    }
}
