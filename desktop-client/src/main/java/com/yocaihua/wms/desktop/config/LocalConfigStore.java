package com.yocaihua.wms.desktop.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class LocalConfigStore {

    private static final String LEGACY_CONFIG_PATH = "desktop-client/data/app-config.json";
    private static final String CONFIG_DIRECTORY_NAME = "warehouse-management-system";
    private static final String CONFIG_FILE_NAME = "app-config.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AppConfig load() {
        Path path = resolvePath();
        if (!Files.exists(path)) {
            return new AppConfig();
        }

        try {
            AppConfig appConfig = objectMapper.readValue(path.toFile(), AppConfig.class);
            return appConfig == null ? new AppConfig() : appConfig;
        } catch (IOException ex) {
            return new AppConfig();
        }
    }

    public void save(AppConfig appConfig) {
        Path path = resolvePath();
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), appConfig);
        } catch (IOException ex) {
            throw new IllegalStateException("保存桌面端本地配置失败", ex);
        }
    }

    public Path getConfigPath() {
        return resolvePath();
    }

    private Path resolvePath() {
        Path primaryPath = resolvePrimaryPath();
        if (Files.exists(primaryPath)) {
            return primaryPath;
        }

        Path legacyPath = Paths.get(LEGACY_CONFIG_PATH).toAbsolutePath().normalize();
        if (!Files.exists(legacyPath)) {
            return primaryPath;
        }

        try {
            Path parent = primaryPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(legacyPath, primaryPath, StandardCopyOption.REPLACE_EXISTING);
            return primaryPath;
        } catch (IOException ex) {
            return legacyPath;
        }
    }

    private Path resolvePrimaryPath() {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData != null && !localAppData.trim().isEmpty()) {
            return Paths.get(localAppData, CONFIG_DIRECTORY_NAME, CONFIG_FILE_NAME).toAbsolutePath().normalize();
        }

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.trim().isEmpty()) {
            return Paths.get(appData, CONFIG_DIRECTORY_NAME, CONFIG_FILE_NAME).toAbsolutePath().normalize();
        }

        String userHome = System.getProperty("user.home", ".");
        return Paths.get(userHome, "." + CONFIG_DIRECTORY_NAME, CONFIG_FILE_NAME).toAbsolutePath().normalize();
    }
}
