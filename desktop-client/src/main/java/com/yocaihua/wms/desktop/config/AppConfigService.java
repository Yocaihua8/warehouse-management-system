package com.yocaihua.wms.desktop.config;

import java.nio.file.Path;

public class AppConfigService {

    public static final String DEFAULT_BASE_URL = "http://127.0.0.1:8080";

    private final LocalConfigStore localConfigStore;
    private AppConfig cachedConfig;

    public AppConfigService() {
        this.localConfigStore = new LocalConfigStore();
    }

    public AppConfig load() {
        if (cachedConfig == null) {
            cachedConfig = normalizeForSession(localConfigStore.load());
        }
        return cachedConfig;
    }

    public void save(AppConfig appConfig) {
        this.cachedConfig = normalizeForSession(copyOf(appConfig));
        localConfigStore.save(copyForPersistence(cachedConfig));
    }

    public Path getConfigPath() {
        return localConfigStore.getConfigPath();
    }

    private AppConfig normalizeForSession(AppConfig appConfig) {
        AppConfig normalized = copyOf(appConfig);
        if (normalized.getServerConfig() == null) {
            normalized.setServerConfig(new ServerConfig());
        }
        String baseUrl = normalized.getServerConfig().getBaseUrl();
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            normalized.getServerConfig().setBaseUrl(DEFAULT_BASE_URL);
        } else {
            normalized.getServerConfig().setBaseUrl(baseUrl.trim());
        }
        return normalized;
    }

    private AppConfig copyForPersistence(AppConfig appConfig) {
        AppConfig persisted = copyOf(appConfig);
        if (!persisted.isRememberServer()) {
            if (persisted.getServerConfig() == null) {
                persisted.setServerConfig(new ServerConfig());
            }
            persisted.getServerConfig().setBaseUrl(DEFAULT_BASE_URL);
        }
        return persisted;
    }

    private AppConfig copyOf(AppConfig source) {
        AppConfig target = new AppConfig();
        if (source == null) {
            return target;
        }

        ServerConfig serverConfig = new ServerConfig();
        if (source.getServerConfig() != null) {
            serverConfig.setBaseUrl(source.getServerConfig().getBaseUrl());
        }
        target.setServerConfig(serverConfig);
        target.setToken(source.getToken());
        target.setLastUsername(source.getLastUsername());
        target.setRememberServer(source.isRememberServer());
        return target;
    }
}
