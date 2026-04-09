package com.yocaihua.wms.desktop.bootstrap;

public class StartupContext {

    private StartupState startupState;
    private String appDisplayName;
    private String appVersion;
    private String serverBaseUrl;
    private String statusMessage;
    private String lastStatusRefreshTime;
    private boolean hasLocalToken;
    private String lastUsername;
    private String appStatus;
    private String databaseStatus;
    private String aiStatus;
    private String aiMessage;
    private String currentUsername;
    private boolean authRequired = true;
    private boolean desktopSupported = true;

    public StartupState getStartupState() {
        return startupState;
    }

    public void setStartupState(StartupState startupState) {
        this.startupState = startupState;
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

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public void setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getLastStatusRefreshTime() {
        return lastStatusRefreshTime;
    }

    public void setLastStatusRefreshTime(String lastStatusRefreshTime) {
        this.lastStatusRefreshTime = lastStatusRefreshTime;
    }

    public boolean isHasLocalToken() {
        return hasLocalToken;
    }

    public void setHasLocalToken(boolean hasLocalToken) {
        this.hasLocalToken = hasLocalToken;
    }

    public String getLastUsername() {
        return lastUsername;
    }

    public void setLastUsername(String lastUsername) {
        this.lastUsername = lastUsername;
    }

    public String getAppStatus() {
        return appStatus;
    }

    public void setAppStatus(String appStatus) {
        this.appStatus = appStatus;
    }

    public String getDatabaseStatus() {
        return databaseStatus;
    }

    public void setDatabaseStatus(String databaseStatus) {
        this.databaseStatus = databaseStatus;
    }

    public String getAiStatus() {
        return aiStatus;
    }

    public void setAiStatus(String aiStatus) {
        this.aiStatus = aiStatus;
    }

    public String getAiMessage() {
        return aiMessage;
    }

    public void setAiMessage(String aiMessage) {
        this.aiMessage = aiMessage;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }

    public void setCurrentUsername(String currentUsername) {
        this.currentUsername = currentUsername;
    }

    public boolean isAuthRequired() {
        return authRequired;
    }

    public void setAuthRequired(boolean authRequired) {
        this.authRequired = authRequired;
    }

    public boolean isDesktopSupported() {
        return desktopSupported;
    }

    public void setDesktopSupported(boolean desktopSupported) {
        this.desktopSupported = desktopSupported;
    }
}
