package com.aq.aqconnect;

import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.RunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class AQConnectRunType extends RunType {

    public AQConnectRunType(final RunTypeRegistry registry){
        registry.registerRunType(this);
    }

    @NotNull
    @Override
    public String getType() {
        return "aqconnect";
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return "accelQ Connect";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Trigger accelQ Runs via jobPid";
    }

    @Nullable
    @Override
    public PropertiesProcessor getRunnerPropertiesProcessor() {
        return null;
    }

    @Nullable
    @Override
    public String getEditRunnerParamsJspFilePath() {
        return "aqconnect.jsp";
    }

    @Nullable
    @Override
    public String getViewRunnerParamsJspFilePath() {
        return "aqconnect.jsp";
    }

    @Nullable
    @Override
    public Map<String, String> getDefaultRunnerProperties() {
        return null;
    }
}