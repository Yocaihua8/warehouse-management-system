package com.yocaihua.wms.desktop.bootstrap;

public class SystemBootstrapData {

    private String appName;
    private String appDisplayName;
    private String appVersion;
    private Boolean desktopSupported;
    private Boolean authRequired;
    private Integer sessionTimeoutMinutes;
    private Boolean aiEnabled;
    private String aiBaseUrl;
    private String healthPath;
    private String loginPath;
    private String logoutPath;
    private String currentUserPath;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppDisplayName() {
        return appDisplayName;
    }

    public void setAppDisplayName(String appDisplayName) {
        this.appDisplayName = appDisplayName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public Boolean getDesktopSupported() {
        return desktopSupported;
    }

    public void setDesktopSupported(Boolean desktopSupported) {
        this.desktopSupported = desktopSupported;
    }

    public Boolean getAuthRequired() {
        return authRequired;
    }

    public void setAuthRequired(Boolean authRequired) {
        this.authRequired = authRequired;
    }

    public Integer getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }

    public void setSessionTimeoutMinutes(Integer sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }

    public Boolean getAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(Boolean aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public String getAiBaseUrl() {
        return aiBaseUrl;
    }

    public void setAiBaseUrl(String aiBaseUrl) {
        this.aiBaseUrl = aiBaseUrl;
    }

    public String getHealthPath() {
        return healthPath;
    }

    public void setHealthPath(String healthPath) {
        this.healthPath = healthPath;
    }

    public String getLoginPath() {
        return loginPath;
    }

    public void setLoginPath(String loginPath) {
        this.loginPath = loginPath;
    }

    public String getLogoutPath() {
        return logoutPath;
    }

    public void setLogoutPath(String logoutPath) {
        this.logoutPath = logoutPath;
    }

    public String getCurrentUserPath() {
        return currentUserPath;
    }

    public void setCurrentUserPath(String currentUserPath) {
        this.currentUserPath = currentUserPath;
    }
}
