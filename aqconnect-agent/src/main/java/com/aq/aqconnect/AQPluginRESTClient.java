package com.aq.aqconnect;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Vinay on 7/31/2016.
 */
public class AQPluginRESTClient {

    private static final AQPluginRESTClient aqRESTClient = new AQPluginRESTClient();
    private final JSONParser jsonParser = new JSONParser();

    //Security Tokens and Project related
    private String ACCESS_TOKEN;
    private String CLIENT_ID;
    private String REFRESH_TOKEN;
    private String TENANT_CODE;
    private String PROJECT_NAME;

    //Base URL and extensions
    private static String BASE_URL;
    private static String API_ENDPOINT;

    private static String LOGIN_URL;
    private static String USERS_URL;
    private static String JOB_TRIGGER_URL;
    private static String JOB_SUMMARY_URL;


    public static AQPluginRESTClient getInstance() {
        return aqRESTClient;
    }

    public String getBaseURL() {
        return BASE_URL;
    }

    public String getResultExternalAccessURL(String jobPid) {
        return String.format(getBaseURL() + AQPluginConstants.EXT_JOB_WEB_LINK, TENANT_CODE, PROJECT_NAME, jobPid);
    }

    public void setUpBaseURL(String baseURL) {
        BASE_URL = baseURL.charAt(baseURL.length() - 1) == '/'?(baseURL):(baseURL + '/');
        API_ENDPOINT =  BASE_URL + "awb/api/%s/%s/";

        LOGIN_URL = BASE_URL + "awb/api/aq_global/security/login";
        USERS_URL = API_ENDPOINT + AQPluginConstants.API_VERSION + "/conf/users";
        JOB_TRIGGER_URL = API_ENDPOINT + AQPluginConstants.API_VERSION + "/test-exec/jobs/%s/trigger-ci";
        JOB_SUMMARY_URL = API_ENDPOINT + AQPluginConstants.API_VERSION + "/test-exec/runs/%s?onlySummary=true";
    }

    private CloseableHttpClient getHttpsClient() {
        try {
            SSLContext sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, new TrustStrategy() {
                        @Override
                        public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                            return true;
                        }
                    }).build();

            CloseableHttpClient client = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build();

            return client;
        } catch(Exception e) {
            return null;
        }
    }

    public JSONObject getJobSummary(long jobPid) {
        CloseableHttpClient httpClient = getHttpsClient();
        HttpGet httpGet = new HttpGet(String.format(JOB_SUMMARY_URL, TENANT_CODE, PROJECT_NAME, Long.toString(jobPid)));
        httpGet.addHeader("User-Agent", AQPluginConstants.USER_AGENT);
        httpGet.addHeader("access_token", ACCESS_TOKEN);
        httpGet.addHeader("client_id", CLIENT_ID);
        httpGet.addHeader("refresh_token", REFRESH_TOKEN);
        try {
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpResponse.getEntity().getContent()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            JSONObject summaryObj = (JSONObject) jsonParser.parse(response.toString());
            httpClient.close();
            return summaryObj;
        } catch(IOException ioe) {
            ioe.printStackTrace();
            return null;
        } catch (ParseException pe) {
            pe.printStackTrace();
            return null;
        }
    }

    public JSONObject triggerJob(long jobPid, String jsonPayload) {
        CloseableHttpClient httpClient = getHttpsClient();
        HttpPut httpPut = new HttpPut(String.format(JOB_TRIGGER_URL, TENANT_CODE, PROJECT_NAME, Long.toString(jobPid)));
        httpPut.addHeader("User-Agent", AQPluginConstants.USER_AGENT);
        httpPut.addHeader("access_token", ACCESS_TOKEN);
        httpPut.addHeader("client_id", CLIENT_ID);
        httpPut.addHeader("refresh_token", REFRESH_TOKEN);
        httpPut.addHeader("Content-Type", "application/json");
        if(jsonPayload != null && !jsonPayload.equals("")) {
            StringEntity requestEntity = new StringEntity(jsonPayload, org.apache.http.entity.ContentType.APPLICATION_JSON);
            httpPut.setEntity(requestEntity);
        }
        try {
            CloseableHttpResponse httpResponse = httpClient.execute(httpPut);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpResponse.getEntity().getContent()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            JSONObject jobInfo = (JSONObject) jsonParser.parse(response.toString());
            httpClient.close();
            return jobInfo;//(long) jobInfo.get("pid");
        } catch(IOException ioe) {
            ioe.printStackTrace();
            return null;
        } catch (Exception pe) {
            pe.printStackTrace();
            return null;
        }
    }
    public List<String> getUsers() {
        CloseableHttpClient httpClient = getHttpsClient();
        HttpGet httpGet = new HttpGet(String.format(USERS_URL, TENANT_CODE, PROJECT_NAME));
        httpGet.addHeader("User-Agent", AQPluginConstants.USER_AGENT);
        httpGet.addHeader("access_token", ACCESS_TOKEN);
        httpGet.addHeader("client_id", CLIENT_ID);
        httpGet.addHeader("refresh_token", REFRESH_TOKEN);
        try {
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet);

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpResponse.getEntity().getContent()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            JSONArray userListReponse = (JSONArray) jsonParser.parse(response.toString());
            List<String> userList = new ArrayList<String>();
            for (Object userObj: userListReponse) {
                userList.add((String) ((JSONObject) userObj).get("userFname"));
            }
            httpClient.close();
            return userList;
        } catch(IOException ioe) {
            ioe.printStackTrace();
            return null;
        } catch (ParseException pe) {
            pe.printStackTrace();
            return null;
        }
    }

    public boolean doLogin(String userName, String secretKey, String projectName) {
        CloseableHttpClient httpClient = getHttpsClient();
        HttpPost httpPost = new HttpPost(LOGIN_URL);
        httpPost.addHeader("User-Agent", AQPluginConstants.USER_AGENT);
        httpPost.addHeader("invalidate-active-tokens", "true");

        List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
        urlParameters.add(new BasicNameValuePair("username", userName));
        urlParameters.add(new BasicNameValuePair("password", new String(Base64.encodeBase64String(secretKey.getBytes()))));

        try {
            HttpEntity postParams = new UrlEncodedFormEntity(urlParameters);
            httpPost.setEntity(postParams);

            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);

            if(httpResponse.getStatusLine().getStatusCode() != 200)
                return false;

            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpResponse.getEntity().getContent()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = reader.readLine()) != null) {
                response.append(inputLine);
            }
            reader.close();
            JSONObject loginResponse = (JSONObject) jsonParser.parse(response.toString());
            setSecurityTokens(loginResponse, projectName);
            reader.close();
            httpClient.close();
            return true;
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        } catch (ParseException pe) {
            pe.printStackTrace();
            return false;
        }
    }

    private boolean setSecurityTokens(JSONObject loginResponse, String projectName) {
        ACCESS_TOKEN = (String) loginResponse.get("access_token");
        CLIENT_ID = (String) loginResponse.get("client_id");
        REFRESH_TOKEN = (String) loginResponse.get("refresh_token");
        TENANT_CODE = (String) loginResponse.get("tenantCode");

        JSONArray userProjectList = (JSONArray) loginResponse.get("userProjects");
        for(Object userProject: userProjectList) {
            String projectName_ = (String)((JSONObject)userProject).get("projectDisplayName");
            if(projectName_.toLowerCase().trim().equals(projectName.toLowerCase().trim())) {
                PROJECT_NAME = (String)((JSONObject)userProject).get("projectName");
                break;
            }
        }
        if(PROJECT_NAME == null)
            return false;
        return true;
    }
}
