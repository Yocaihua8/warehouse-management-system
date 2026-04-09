package com.yocaihua.wms.desktop.config;

public class AppConfig {

    private ServerConfig serverConfig = new ServerConfig();
    private String token = "";
    private String lastUsername = "";
    private boolean rememberServer = true;

    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getLastUsername() {
        return lastUsername;
    }

    public void setLastUsername(String lastUsername) {
        this.lastUsername = lastUsername;
    }

    public boolean isRememberServer() {
        return rememberServer;
    }

    public void setRememberServer(boolean rememberServer) {
        this.rememberServer = rememberServer;
    }
}
