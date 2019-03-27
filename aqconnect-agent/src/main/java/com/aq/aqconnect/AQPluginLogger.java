package com.aq.aqconnect;

import jetbrains.buildServer.agent.BuildProgressLogger;

/**
 * Created by Vinay on 6/20/2018.
 */
public class AQPluginLogger {

    private BuildProgressLogger logger;

    public AQPluginLogger(BuildProgressLogger logger) {
        this.logger = logger;
    }

    public void print(String msg) {
        this.logger.message(msg);
    }

    public void println() {
        print("\n");
    }

    public void println(String msg) {
        print(msg + "\n");
    }
    
    public void logStatus(String status, String msg) {
        this.logger.logMessage(DefaultMessagesInfo.createTextMessage(String.format("##teamcity[buildStatus status='%s' text='%s']", status, msg)));
    }
}

