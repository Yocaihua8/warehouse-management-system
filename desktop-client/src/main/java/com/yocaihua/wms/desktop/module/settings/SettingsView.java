package com.yocaihua.wms.desktop.module.settings;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.SystemApi;
import com.yocaihua.wms.desktop.auth.AuthService;
import com.yocaihua.wms.desktop.bootstrap.StartupCoordinator;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import com.yocaihua.wms.desktop.bootstrap.StartupState;
import com.yocaihua.wms.desktop.bootstrap.SystemBootstrapData;
import com.yocaihua.wms.desktop.bootstrap.SystemHealthData;
import com.yocaihua.wms.desktop.config.AppConfig;
import com.yocaihua.wms.desktop.config.AppConfigService;
import com.yocaihua.wms.desktop.config.ServerConfig;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class SettingsView {

    private static final String DEFAULT_BASE_URL = AppConfigService.DEFAULT_BASE_URL;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String BACKEND_LOG_FILE_NAME = "backend-service.log";
    private static final String AI_LOG_FILE_NAME = "python-ai-service.log";
    private static final String BACKEND_PID_FILE_NAME = "backend-service.pid";
    private static final String AI_PID_FILE_NAME = "python-ai-service.pid";

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final AppConfigService appConfigService;
    private final Consumer<StartupContext> onStartupContextRefreshed;
    private final VBox root;
    private final TextField baseUrlField;
    private final CheckBox rememberServerCheckBox;
    private final Label currentBaseUrlValueLabel;
    private final Label configPathValueLabel;
    private final Label logDirPathValueLabel;
    private final Label logFileNamesValueLabel;
    private final Label lastUsernameValueLabel;
    private final Label startupStateValueLabel;
    private final Label startupFailureReasonValueLabel;
    private final Label lastRefreshTimeValueLabel;
    private final Label overallStatusValueLabel;
    private final Label backendStatusValueLabel;
    private final Label databaseStatusValueLabel;
    private final Label aiStatusValueLabel;
    private final Label appDisplayNameValueLabel;
    private final Label appVersionValueLabel;
    private final Label desktopSupportedValueLabel;
    private final Label authRequiredValueLabel;
    private final Label aiBaseUrlValueLabel;
    private final Label statusLabel;
    private final Button saveButton;
    private final Button testButton;
    private final Button recheckButton;
    private final Button startBackendButton;
    private final Button startAiButton;
    private final Button stopBackendButton;
    private final Button stopAiButton;
    private final Button openLogDirButton;
    private final Button resetButton;

    public SettingsView(StartupContext startupContext, ApiClient apiClient, AppConfigService appConfigService, Consumer<StartupContext> onStartupContextRefreshed) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.appConfigService = appConfigService;
        this.onStartupContextRefreshed = onStartupContextRefreshed;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));

        VBox card = new VBox(16);
        card.getStyleClass().add("page-card");

        Label titleLabel = new Label("系统设置");
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("当前页只处理服务地址、本地配置和连接测试；取消“记住服务地址”后，地址只对本次会话生效。");
        subtitleLabel.getStyleClass().add("page-subtitle");

        GridPane configGrid = new GridPane();
        configGrid.setHgap(16);
        configGrid.setVgap(12);

        this.currentBaseUrlValueLabel = createValueLabel();
        this.configPathValueLabel = createValueLabel();
        this.logDirPathValueLabel = createValueLabel();
        this.logFileNamesValueLabel = createValueLabel();
        this.lastUsernameValueLabel = createValueLabel();
        this.startupStateValueLabel = createValueLabel();
        this.startupFailureReasonValueLabel = createValueLabel();
        this.lastRefreshTimeValueLabel = createValueLabel();
        this.baseUrlField = new TextField();
        this.baseUrlField.setPromptText(DEFAULT_BASE_URL);
        this.rememberServerCheckBox = new CheckBox("记住服务地址");
        this.saveButton = new Button("保存配置");
        this.testButton = new Button("测试连接");
        this.recheckButton = new Button("重新执行启动检查");
        this.startBackendButton = new Button("启动后端");
        this.startAiButton = new Button("启动 AI 服务");
        this.stopBackendButton = new Button("停止后端");
        this.stopAiButton = new Button("停止 AI 服务");
        this.openLogDirButton = new Button("打开日志目录");
        this.resetButton = new Button("恢复默认");

        HBox baseUrlActionRow = new HBox(12);
        baseUrlActionRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(baseUrlField, Priority.ALWAYS);
        baseUrlActionRow.getChildren().addAll(baseUrlField, saveButton, testButton, recheckButton, resetButton);

        HBox serviceActionRow = new HBox(12);
        serviceActionRow.setAlignment(Pos.CENTER_LEFT);
        serviceActionRow.getChildren().addAll(startBackendButton, stopBackendButton, startAiButton, stopAiButton, openLogDirButton);

        configGrid.add(createFieldLabel("当前服务地址"), 0, 0);
        configGrid.add(currentBaseUrlValueLabel, 1, 0);
        configGrid.add(createFieldLabel("配置文件路径"), 0, 1);
        configGrid.add(configPathValueLabel, 1, 1);
        configGrid.add(createFieldLabel("日志目录路径"), 0, 2);
        configGrid.add(logDirPathValueLabel, 1, 2);
        configGrid.add(createFieldLabel("启动日志文件"), 0, 3);
        configGrid.add(logFileNamesValueLabel, 1, 3);
        configGrid.add(createFieldLabel("最近登录用户"), 0, 4);
        configGrid.add(lastUsernameValueLabel, 1, 4);
        configGrid.add(createFieldLabel("服务地址"), 0, 5);
        configGrid.add(baseUrlActionRow, 1, 5);
        configGrid.add(createFieldLabel("配置选项"), 0, 6);
        configGrid.add(rememberServerCheckBox, 1, 6);
        configGrid.add(createFieldLabel("本地服务"), 0, 7);
        configGrid.add(serviceActionRow, 1, 7);

        Label healthTitleLabel = new Label("连接测试结果");
        healthTitleLabel.getStyleClass().add("page-title");
        healthTitleLabel.setStyle("-fx-font-size: 18px;");

        GridPane healthGrid = new GridPane();
        healthGrid.setHgap(16);
        healthGrid.setVgap(12);

        this.overallStatusValueLabel = createValueLabel();
        this.backendStatusValueLabel = createValueLabel();
        this.databaseStatusValueLabel = createValueLabel();
        this.aiStatusValueLabel = createValueLabel();
        this.appDisplayNameValueLabel = createValueLabel();
        this.appVersionValueLabel = createValueLabel();
        this.desktopSupportedValueLabel = createValueLabel();
        this.authRequiredValueLabel = createValueLabel();
        this.aiBaseUrlValueLabel = createValueLabel();

        addSummaryRow(healthGrid, 0, "总体状态", overallStatusValueLabel, "后端状态", backendStatusValueLabel);
        addSummaryRow(healthGrid, 1, "数据库状态", databaseStatusValueLabel, "AI状态", aiStatusValueLabel);
        addSummaryRow(healthGrid, 2, "应用名称", appDisplayNameValueLabel, "应用版本", appVersionValueLabel);
        addSummaryRow(healthGrid, 3, "桌面端支持", desktopSupportedValueLabel, "登录要求", authRequiredValueLabel);
        addSummaryRow(healthGrid, 4, "AI服务地址", aiBaseUrlValueLabel, "当前用户", createStaticValueLabel(defaultText(startupContext.getCurrentUsername(), "-")));
        addSummaryRow(healthGrid, 5, "启动状态", startupStateValueLabel, "状态刷新时间", lastRefreshTimeValueLabel);

        VBox errorBox = new VBox(8);
        Label startupFailureLabel = createFieldLabel("最近失败原因");
        errorBox.getChildren().addAll(startupFailureLabel, startupFailureReasonValueLabel);

        this.statusLabel = new Label("可先修改服务地址，再点击“测试连接”检查后端、数据库和 AI 服务状态。");
        this.statusLabel.getStyleClass().add("page-subtitle");

        card.getChildren().addAll(titleLabel, subtitleLabel, configGrid, healthTitleLabel, healthGrid, errorBox, statusLabel);

        ScrollPane scrollPane = new ScrollPane(card);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("settings-scroll");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        root.getChildren().add(scrollPane);

        bindActions();
        loadConfigSnapshot();
        applyStartupSnapshot();
    }

    public Parent getRoot() {
        return root;
    }

    private void bindActions() {
        saveButton.setOnAction(event -> handleSaveConfig());
        testButton.setOnAction(event -> handleTestConnection());
        recheckButton.setOnAction(event -> handleRerunStartupCheck());
        startBackendButton.setOnAction(event -> handleStartBackend());
        startAiButton.setOnAction(event -> handleStartAiService());
        stopBackendButton.setOnAction(event -> handleStopBackend());
        stopAiButton.setOnAction(event -> handleStopAiService());
        openLogDirButton.setOnAction(event -> handleOpenLogDirectory());
        resetButton.setOnAction(event -> baseUrlField.setText(DEFAULT_BASE_URL));
    }

    private void loadConfigSnapshot() {
        AppConfig appConfig = appConfigService.load();
        String baseUrl = resolveConfiguredBaseUrl(appConfig);
        currentBaseUrlValueLabel.setText(baseUrl);
        configPathValueLabel.setText(appConfigService.getConfigPath().toString());
        logDirPathValueLabel.setText(resolveLogDir().toString());
        logFileNamesValueLabel.setText(buildLogFileNamesText());
        baseUrlField.setText(baseUrl);
        lastUsernameValueLabel.setText(defaultText(appConfig == null ? null : appConfig.getLastUsername(), "-"));
        rememberServerCheckBox.setSelected(appConfig == null || appConfig.isRememberServer());
    }

    private void applyStartupSnapshot() {
        startupStateValueLabel.setText(resolveStartupStateText(startupContext.getStartupState()));
        startupFailureReasonValueLabel.setText(resolveFailureReasonText(startupContext.getStartupState(), startupContext.getStatusMessage()));
        lastRefreshTimeValueLabel.setText(defaultText(startupContext.getLastStatusRefreshTime(), "启动阶段未记录"));
        overallStatusValueLabel.setText(defaultText(startupContext.getAppStatus(), "-"));
        backendStatusValueLabel.setText(buildStatusText(startupContext.getAppStatus(), startupContext.getStatusMessage()));
        databaseStatusValueLabel.setText(defaultText(startupContext.getDatabaseStatus(), "-"));
        aiStatusValueLabel.setText(buildStatusText(startupContext.getAiStatus(), startupContext.getAiMessage()));
        appDisplayNameValueLabel.setText(defaultText(startupContext.getAppDisplayName(), "-"));
        appVersionValueLabel.setText(defaultText(startupContext.getAppVersion(), "-"));
        desktopSupportedValueLabel.setText(startupContext.isDesktopSupported() ? "是" : "否");
        authRequiredValueLabel.setText(startupContext.isAuthRequired() ? "需要" : "不需要");
        aiBaseUrlValueLabel.setText(defaultText(startupContext.getAiBaseUrl(), "http://127.0.0.1:9000"));
    }

    private void handleSaveConfig() {
        String baseUrl = normalizeBaseUrl(baseUrlField.getText());
        if (baseUrl == null) {
            statusLabel.setText("服务地址不能为空，且必须以 http:// 或 https:// 开头。");
            return;
        }

        AppConfig appConfig = appConfigService.load();
        if (appConfig.getServerConfig() == null) {
            appConfig.setServerConfig(new ServerConfig());
        }
        appConfig.getServerConfig().setBaseUrl(baseUrl);
        appConfig.setRememberServer(rememberServerCheckBox.isSelected());
        appConfigService.save(appConfig);

        currentBaseUrlValueLabel.setText(baseUrl);
        statusLabel.setText("服务地址已保存。后续新请求会使用新地址，已打开页面如需更新请手动刷新。");
        if (!rememberServerCheckBox.isSelected()) {
            statusLabel.setText("服务地址已保存到当前会话。重启桌面端后会恢复默认地址 " + DEFAULT_BASE_URL + "。");
        }
    }

    private void handleTestConnection() {
        String baseUrl = normalizeBaseUrl(baseUrlField.getText());
        if (baseUrl == null) {
            statusLabel.setText("测试连接前请先输入合法的服务地址。");
            return;
        }

        setLoadingState(true, "正在测试服务连接...");

        Task<ConnectionTestResult> testTask = new Task<>() {
            @Override
            protected ConnectionTestResult call() {
                ApiClient testClient = new ApiClient(new TemporaryConfigService(baseUrl));
                ApiResponse<SystemHealthData> healthResponse = testClient.get(SystemApi.HEALTH, SystemHealthData.class);
                SystemHealthData healthData = unwrapResponse(healthResponse, "系统健康检查失败");
                ApiResponse<SystemBootstrapData> bootstrapResponse = testClient.get(SystemApi.BOOTSTRAP, SystemBootstrapData.class);
                SystemBootstrapData bootstrapData = unwrapResponse(bootstrapResponse, "系统启动配置读取失败");
                return new ConnectionTestResult(healthData, bootstrapData);
            }
        };

        testTask.setOnSucceeded(event -> {
            ConnectionTestResult result = testTask.getValue();
            renderTestResult(result);
            startupFailureReasonValueLabel.setText("无");
            lastRefreshTimeValueLabel.setText(formatNow());
            setLoadingState(false, "连接测试完成。");
        });

        testTask.setOnFailed(event -> {
            String message = resolveErrorMessage(testTask.getException(), "连接测试失败，请检查服务地址和本地服务状态。");
            startupFailureReasonValueLabel.setText(message);
            lastRefreshTimeValueLabel.setText(formatNow());
            setLoadingState(false, message);
        });

        Thread thread = new Thread(testTask, "desktop-settings-test-connection");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleRerunStartupCheck() {
        String baseUrl = normalizeBaseUrl(baseUrlField.getText());
        if (baseUrl == null) {
            statusLabel.setText("重新执行启动检查前请先输入合法的服务地址。");
            return;
        }

        AppConfig currentConfig = appConfigService.load();
        String effectiveBaseUrl = resolveConfiguredBaseUrl(currentConfig);
        boolean rememberServerChanged = rememberServerCheckBox.isSelected() != (currentConfig != null && currentConfig.isRememberServer());
        if (!baseUrl.equals(effectiveBaseUrl) || rememberServerChanged) {
            statusLabel.setText("请先保存配置，再重新执行启动检查。");
            return;
        }

        setLoadingState(true, "正在重新执行桌面端启动检查...");

        Task<StartupContext> recheckTask = new Task<>() {
            @Override
            protected StartupContext call() {
                StartupCoordinator startupCoordinator = new StartupCoordinator(appConfigService, new AuthService(appConfigService));
                return startupCoordinator.prepareInitialContext();
            }
        };

        recheckTask.setOnSucceeded(event -> {
            StartupContext refreshedContext = recheckTask.getValue();
            applyRefreshedStartupContext(refreshedContext);
            setLoadingState(false, "桌面端启动检查已重新执行。");
        });

        recheckTask.setOnFailed(event -> {
            String message = resolveErrorMessage(recheckTask.getException(), "重新执行启动检查失败。");
            startupFailureReasonValueLabel.setText(message);
            lastRefreshTimeValueLabel.setText(formatNow());
            setLoadingState(false, message);
        });

        Thread thread = new Thread(recheckTask, "desktop-settings-rerun-startup");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleStartBackend() {
        if (hasUnsavedConnectionConfig()) {
            statusLabel.setText("请先保存配置，再启动本地后端服务。");
            return;
        }

        Path backendDir = resolveProjectRoot().resolve("backend");
        Path backendCommand = backendDir.resolve("mvnw.cmd");
        if (!Files.exists(backendCommand)) {
            statusLabel.setText("未找到 backend/mvnw.cmd，无法从设置页启动后端。");
            return;
        }

        List<String> command = List.of("cmd.exe", "/c", "mvnw.cmd", "spring-boot:run");
        handleStartService(
                "后端",
                backendDir,
                command,
                "正在启动本地后端服务...",
                BACKEND_LOG_FILE_NAME,
                StartupExpectation.BACKEND_READY
        );
    }

    private void handleStartAiService() {
        if (hasUnsavedConnectionConfig()) {
            statusLabel.setText("请先保存配置，再启动本地 AI 服务。");
            return;
        }

        Path aiDir = resolveProjectRoot().resolve("python-ai-service");
        if (!Files.isDirectory(aiDir)) {
            statusLabel.setText("未找到 python-ai-service 目录，无法从设置页启动 AI 服务。");
            return;
        }
        List<String> command = buildAiStartCommand(aiDir);

        handleStartService(
                "AI 服务",
                aiDir,
                command,
                "正在启动本地 AI 服务...",
                AI_LOG_FILE_NAME,
                StartupExpectation.AI_READY
        );
    }

    private void handleStartService(
            String serviceName,
            Path workingDirectory,
            List<String> command,
            String loadingMessage,
            String logFileName,
            StartupExpectation expectation
    ) {
        if (!Files.isDirectory(workingDirectory)) {
            statusLabel.setText(serviceName + "目录不存在：" + workingDirectory);
            return;
        }

        setLoadingState(true, loadingMessage);

        Task<ServiceStartAttempt> startTask = new Task<>() {
            @Override
            protected ServiceStartAttempt call() throws Exception {
                StartupContext beforeStartContext = prepareLatestStartupContext();
                if (isExpectationReady(beforeStartContext, expectation)) {
                    return new ServiceStartAttempt(beforeStartContext, null, true, false, null);
                }

                Path logPath;
                try {
                    logPath = prepareLogPath(logFileName);
                    Path pidPath = preparePidPath(resolvePidFileName(expectation));
                    startDetachedProcess(command, workingDirectory, logPath, pidPath);
                } catch (Exception ex) {
                    StartupContext refreshedContext = prepareLatestStartupContext();
                    return new ServiceStartAttempt(
                            refreshedContext,
                            resolveLogDir().resolve(logFileName),
                            isExpectationReady(refreshedContext, expectation),
                            false,
                            resolveErrorMessage(ex, serviceName + "启动失败。")
                    );
                }

                ServiceStartResult result = rerunStartupCheck(expectation);
                return new ServiceStartAttempt(result.startupContext(), logPath, result.serviceReady(), true, null);
            }
        };

        startTask.setOnSucceeded(event -> {
            ServiceStartAttempt result = startTask.getValue();
            applyRefreshedStartupContext(result.startupContext());
            if (result.serviceReady()) {
                String successMessage = result.commandStarted()
                        ? serviceName + "启动命令已执行，并已自动完成启动检查。"
                        : serviceName + "当前已可用，无需重复启动。";
                startupFailureReasonValueLabel.setText("无");
                lastRefreshTimeValueLabel.setText(defaultText(
                        result.startupContext() == null ? null : result.startupContext().getLastStatusRefreshTime(),
                        formatNow()
                ));
                setLoadingState(false, successMessage);
                return;
            }
            if (!result.commandStarted() && !isBlank(result.errorMessage())) {
                startupFailureReasonValueLabel.setText(result.errorMessage());
                lastRefreshTimeValueLabel.setText(formatNow());
                setLoadingState(false, result.errorMessage());
                return;
            }
            String message = serviceName + "启动命令已执行，但启动检查仍未确认服务可用。可稍后重试，必要时查看日志：" + result.logPath();
            startupFailureReasonValueLabel.setText(message);
            lastRefreshTimeValueLabel.setText(formatNow());
            setLoadingState(false, message);
        });

        startTask.setOnFailed(event -> {
            String message = resolveErrorMessage(startTask.getException(), serviceName + "启动失败。");
            startupFailureReasonValueLabel.setText(message);
            lastRefreshTimeValueLabel.setText(formatNow());
            setLoadingState(false, message);
        });

        Thread thread = new Thread(startTask, "desktop-settings-start-" + serviceName);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleStopBackend() {
        if (hasUnsavedConnectionConfig()) {
            statusLabel.setText("请先保存配置，再停止本地后端服务。");
            return;
        }

        int backendPort = resolvePort(normalizeBaseUrl(currentBaseUrlValueLabel.getText()), 8080);
        Path pidPath = resolveLogDir().resolve(BACKEND_PID_FILE_NAME);

        handleStopService(
                "后端",
                pidPath,
                backendPort,
                "正在停止本地后端服务...",
                StartupExpectation.BACKEND_DOWN
        );
    }

    private void handleStopAiService() {
        if (hasUnsavedConnectionConfig()) {
            statusLabel.setText("请先保存配置，再停止本地 AI 服务。");
            return;
        }

        int aiPort = resolvePort(aiBaseUrlValueLabel.getText(), 9000);
        Path pidPath = resolveLogDir().resolve(AI_PID_FILE_NAME);

        handleStopService(
                "AI 服务",
                pidPath,
                aiPort,
                "正在停止本地 AI 服务...",
                StartupExpectation.AI_DOWN
        );
    }

    private void handleStopService(
            String serviceName,
            Path pidPath,
            int fallbackPort,
            String loadingMessage,
            StartupExpectation expectation
    ) {
        setLoadingState(true, loadingMessage);

        Task<ServiceStopResult> stopTask = new Task<>() {
            @Override
            protected ServiceStopResult call() throws Exception {
                boolean stoppedByPid = stopProcessByPidFile(pidPath);
                boolean stoppedByPort = stopProcessByPort(fallbackPort);
                ServiceStartResult result = rerunStartupCheck(expectation);
                boolean stopped = stoppedByPid || stoppedByPort || result.serviceReady();
                return new ServiceStopResult(result.startupContext(), stopped, stoppedByPid, stoppedByPort, fallbackPort);
            }
        };

        stopTask.setOnSucceeded(event -> {
            ServiceStopResult result = stopTask.getValue();
            applyRefreshedStartupContext(result.startupContext());
            if (result.stopped()) {
                setLoadingState(false, serviceName + "停止命令已执行，并已自动完成启动检查。");
                return;
            }
            String message = serviceName + "未检测到可停止进程（端口 " + result.fallbackPort() + "）。如仍异常，请手动检查任务管理器。";
            startupFailureReasonValueLabel.setText(message);
            lastRefreshTimeValueLabel.setText(formatNow());
            setLoadingState(false, message);
        });

        stopTask.setOnFailed(event -> {
            String message = resolveErrorMessage(stopTask.getException(), serviceName + "停止失败。");
            startupFailureReasonValueLabel.setText(message);
            lastRefreshTimeValueLabel.setText(formatNow());
            setLoadingState(false, message);
        });

        Thread thread = new Thread(stopTask, "desktop-settings-stop-" + serviceName);
        thread.setDaemon(true);
        thread.start();
    }

    private void handleOpenLogDirectory() {
        try {
            Path logDir = resolveLogDir();
            Files.createDirectories(logDir);
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                statusLabel.setText("当前环境不支持直接打开日志目录，可手动前往：" + logDir);
                return;
            }
            Desktop.getDesktop().open(logDir.toFile());
            statusLabel.setText("已打开日志目录：" + logDir);
        } catch (IOException ex) {
            statusLabel.setText("打开日志目录失败：" + resolveErrorMessage(ex, "请手动前往日志目录。"));
        }
    }

    private void renderTestResult(ConnectionTestResult result) {
        SystemHealthData health = result.healthData();
        SystemBootstrapData bootstrap = result.bootstrapData();

        overallStatusValueLabel.setText(defaultText(health == null ? null : health.getOverallStatus(), "-"));
        backendStatusValueLabel.setText(buildStatusText(health == null ? null : health.getAppStatus(), health == null ? null : health.getAppMessage()));
        databaseStatusValueLabel.setText(buildStatusText(health == null ? null : health.getDatabaseStatus(), health == null ? null : health.getDatabaseMessage()));
        aiStatusValueLabel.setText(buildStatusText(health == null ? null : health.getAiStatus(), health == null ? null : health.getAiMessage()));
        appDisplayNameValueLabel.setText(defaultText(bootstrap == null ? null : bootstrap.getAppDisplayName(), "-"));
        appVersionValueLabel.setText(defaultText(bootstrap == null ? null : bootstrap.getAppVersion(), "-"));
        desktopSupportedValueLabel.setText(Boolean.TRUE.equals(bootstrap == null ? null : bootstrap.getDesktopSupported()) ? "是" : "否");
        authRequiredValueLabel.setText(Boolean.TRUE.equals(bootstrap == null ? null : bootstrap.getAuthRequired()) ? "需要" : "不需要");
        aiBaseUrlValueLabel.setText(defaultText(bootstrap == null ? null : bootstrap.getAiBaseUrl(), defaultText(health == null ? null : health.getAiBaseUrl(), "-")));
    }

    private void setLoadingState(boolean loading, String message) {
        baseUrlField.setDisable(loading);
        rememberServerCheckBox.setDisable(loading);
        saveButton.setDisable(loading);
        testButton.setDisable(loading);
        recheckButton.setDisable(loading);
        startBackendButton.setDisable(loading);
        startAiButton.setDisable(loading);
        stopBackendButton.setDisable(loading);
        stopAiButton.setDisable(loading);
        openLogDirButton.setDisable(loading);
        resetButton.setDisable(loading);
        statusLabel.setText(message);
    }

    private String normalizeBaseUrl(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (!text.startsWith("http://") && !text.startsWith("https://")) {
            return null;
        }
        while (text.endsWith("/")) {
            text = text.substring(0, text.length() - 1);
        }
        return text;
    }

    private String resolveConfiguredBaseUrl(AppConfig appConfig) {
        if (appConfig == null || appConfig.getServerConfig() == null) {
            return DEFAULT_BASE_URL;
        }
        String baseUrl = normalizeBaseUrl(appConfig.getServerConfig().getBaseUrl());
        return baseUrl == null ? DEFAULT_BASE_URL : baseUrl;
    }

    private boolean hasUnsavedConnectionConfig() {
        String inputBaseUrl = normalizeBaseUrl(baseUrlField.getText());
        if (inputBaseUrl == null) {
            return true;
        }

        AppConfig currentConfig = appConfigService.load();
        String effectiveBaseUrl = resolveConfiguredBaseUrl(currentConfig);
        boolean rememberServerChanged = rememberServerCheckBox.isSelected() != (currentConfig == null || currentConfig.isRememberServer());
        return !inputBaseUrl.equals(effectiveBaseUrl) || rememberServerChanged;
    }

    private ServiceStartResult rerunStartupCheck(StartupExpectation expectation) throws InterruptedException {
        StartupContext latestContext = prepareLatestStartupContext();
        if (isExpectationReady(latestContext, expectation)) {
            return new ServiceStartResult(latestContext, null, true);
        }

        for (int attempt = 0; attempt < 12; attempt++) {
            Thread.sleep(2000L);
            latestContext = prepareLatestStartupContext();
            if (isExpectationReady(latestContext, expectation)) {
                return new ServiceStartResult(latestContext, null, true);
            }
        }
        return new ServiceStartResult(latestContext, null, false);
    }

    private boolean isExpectationReady(StartupContext context, StartupExpectation expectation) {
        if (context == null || expectation == null) {
            return false;
        }
        return switch (expectation) {
            case BACKEND_READY -> !"DOWN".equalsIgnoreCase(defaultText(context.getAppStatus(), "DOWN"));
            case AI_READY -> !"DOWN".equalsIgnoreCase(defaultText(context.getAiStatus(), "DOWN"));
            case BACKEND_DOWN -> "DOWN".equalsIgnoreCase(defaultText(context.getAppStatus(), "DOWN"));
            case AI_DOWN -> "DOWN".equalsIgnoreCase(defaultText(context.getAiStatus(), "DOWN"));
        };
    }

    private void startDetachedProcess(List<String> command, Path workingDirectory, Path logPath, Path pidPath) throws IOException {
        Files.createDirectories(logPath.getParent());
        ProcessBuilder processBuilder = new ProcessBuilder(new ArrayList<>(command));
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(logPath.toFile()));
        Process process = processBuilder.start();
        long pid = process.pid();
        writePidFile(pidPath, pid);
    }

    private Path prepareLogPath(String fileName) throws IOException {
        Path logDir = resolveLogDir();
        Files.createDirectories(logDir);
        return logDir.resolve(fileName);
    }

    private Path preparePidPath(String fileName) throws IOException {
        Path logDir = resolveLogDir();
        Files.createDirectories(logDir);
        return logDir.resolve(fileName);
    }

    private Path resolveLogDir() {
        return appConfigService.getConfigPath().getParent().resolve("logs");
    }

    private String buildLogFileNamesText() {
        return "后端：" + BACKEND_LOG_FILE_NAME + "；AI：" + AI_LOG_FILE_NAME;
    }

    private String resolvePidFileName(StartupExpectation expectation) {
        return switch (expectation) {
            case BACKEND_READY, BACKEND_DOWN -> BACKEND_PID_FILE_NAME;
            case AI_READY, AI_DOWN -> AI_PID_FILE_NAME;
        };
    }

    private void writePidFile(Path pidPath, long pid) throws IOException {
        if (pid <= 0) {
            return;
        }
        Files.createDirectories(pidPath.getParent());
        Files.writeString(pidPath, String.valueOf(pid));
    }

    private boolean stopProcessByPidFile(Path pidPath) {
        if (pidPath == null || !Files.exists(pidPath)) {
            return false;
        }
        try {
            String text = Files.readString(pidPath).trim();
            if (text.isEmpty()) {
                Files.deleteIfExists(pidPath);
                return false;
            }
            long pid = Long.parseLong(text);
            boolean stopped = stopProcessByPid(pid);
            Files.deleteIfExists(pidPath);
            return stopped;
        } catch (Exception ignored) {
            try {
                Files.deleteIfExists(pidPath);
            } catch (IOException ignoredDelete) {
            }
            return false;
        }
    }

    private boolean stopProcessByPort(int port) {
        if (port <= 0) {
            return false;
        }
        Set<Long> pidSet = findListeningPids(port);
        if (pidSet.isEmpty()) {
            return false;
        }
        boolean stoppedAny = false;
        for (Long pid : pidSet) {
            if (pid != null && pid > 0 && stopProcessByPid(pid)) {
                stoppedAny = true;
            }
        }
        return stoppedAny;
    }

    private boolean stopProcessByPid(long pid) {
        try {
            ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
            if (handle == null || !handle.isAlive()) {
                return false;
            }
            handle.destroy();
            for (int i = 0; i < 5; i++) {
                if (!handle.isAlive()) {
                    return true;
                }
                Thread.sleep(200L);
            }
            handle.destroyForcibly();
            for (int i = 0; i < 10; i++) {
                if (!handle.isAlive()) {
                    return true;
                }
                Thread.sleep(200L);
            }
            return !handle.isAlive();
        } catch (Exception ignored) {
            return false;
        }
    }

    private Set<Long> findListeningPids(int port) {
        Set<Long> pidSet = new LinkedHashSet<>();
        Process process = null;
        try {
            process = new ProcessBuilder("cmd.exe", "/c", "netstat -ano -p tcp").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.contains("LISTENING")) {
                        continue;
                    }
                    if (!containsPort(line, port)) {
                        continue;
                    }
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length < 5) {
                        continue;
                    }
                    String pidText = parts[parts.length - 1];
                    try {
                        pidSet.add(Long.parseLong(pidText));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            process.waitFor();
        } catch (Exception ignored) {
        } finally {
            if (process != null) {
                process.destroyForcibly();
            }
        }
        return pidSet;
    }

    private boolean containsPort(String netstatLine, int port) {
        String marker = ":" + port;
        int index = netstatLine.indexOf(marker);
        if (index < 0) {
            return false;
        }
        int endIndex = index + marker.length();
        if (endIndex >= netstatLine.length()) {
            return true;
        }
        char next = netstatLine.charAt(endIndex);
        return Character.isWhitespace(next);
    }

    private int resolvePort(String baseUrl, int defaultPort) {
        if (isBlank(baseUrl)) {
            return defaultPort;
        }
        try {
            URI uri = URI.create(baseUrl.trim());
            if (uri.getPort() > 0) {
                return uri.getPort();
            }
            if ("https".equalsIgnoreCase(uri.getScheme())) {
                return 443;
            }
            return 80;
        } catch (Exception ignored) {
            return defaultPort;
        }
    }

    private List<String> buildAiStartCommand(Path aiDir) {
        Path venvPython = aiDir.resolve(".venv").resolve("Scripts").resolve("python.exe");
        if (Files.exists(venvPython)) {
            return List.of(
                    venvPython.toString(),
                    "-m",
                    "uvicorn",
                    "app:app",
                    "--host",
                    "127.0.0.1",
                    "--port",
                    "9000"
            );
        }

        return List.of(
                "py",
                "-3",
                "-m",
                "uvicorn",
                "app:app",
                "--host",
                "127.0.0.1",
                "--port",
                "9000"
        );
    }

    private Path resolveProjectRoot() {
        Path currentDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (containsDesktopProjectRoots(currentDirectory)) {
            return currentDirectory;
        }

        Path parentDirectory = currentDirectory.getParent();
        if (parentDirectory != null && containsDesktopProjectRoots(parentDirectory)) {
            return parentDirectory;
        }
        return currentDirectory;
    }

    private boolean containsDesktopProjectRoots(Path directory) {
        if (directory == null) {
            return false;
        }
        return Files.isDirectory(directory.resolve("backend"))
                && Files.isDirectory(directory.resolve("python-ai-service"));
    }

    private <T> T unwrapResponse(ApiResponse<T> response, String fallbackMessage) {
        if (response == null) {
            throw new ApiException(fallbackMessage);
        }
        if (!response.isSuccess() || response.getData() == null) {
            String message = response.getMessage();
            throw new ApiException(isBlank(message) ? fallbackMessage : message.trim());
        }
        return response.getData();
    }

    private String resolveErrorMessage(Throwable throwable, String fallbackMessage) {
        String message = extractDeepestMessage(throwable);
        if (isBlank(message)) {
            return fallbackMessage;
        }
        String normalizedMessage = normalizeNativeMessage(message);
        if (containsUnreadableReplacement(normalizedMessage)) {
            return buildEncodedErrorFallback(fallbackMessage);
        }
        return normalizedMessage;
    }

    private void addSummaryRow(GridPane gridPane, int rowIndex, String label1, Label value1, String label2, Label value2) {
        Label leftLabel = createFieldLabel(label1);
        Label rightLabel = createFieldLabel(label2);
        gridPane.add(leftLabel, 0, rowIndex);
        gridPane.add(value1, 1, rowIndex);
        gridPane.add(rightLabel, 2, rowIndex);
        gridPane.add(value2, 3, rowIndex);
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text + "：");
        label.getStyleClass().add("page-label");
        label.setMinWidth(90);
        return label;
    }

    private Label createValueLabel() {
        Label label = new Label("-");
        label.getStyleClass().add("page-label");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label createStaticValueLabel(String text) {
        Label label = createValueLabel();
        label.setText(text);
        return label;
    }

    private String buildStatusText(String status, String message) {
        String normalizedStatus = defaultText(status, "-");
        String normalizedMessage = defaultText(message, "");
        if (normalizedMessage.isEmpty()) {
            return normalizedStatus;
        }
        return normalizedStatus + " / " + normalizedMessage;
    }

    private void applyRefreshedStartupContext(StartupContext refreshedContext) {
        if (refreshedContext == null) {
            startupFailureReasonValueLabel.setText("重新执行启动检查未返回有效结果。");
            lastRefreshTimeValueLabel.setText(formatNow());
            return;
        }

        startupContext.setStartupState(refreshedContext.getStartupState());
        startupContext.setAppDisplayName(refreshedContext.getAppDisplayName());
        startupContext.setAppVersion(refreshedContext.getAppVersion());
        startupContext.setServerBaseUrl(refreshedContext.getServerBaseUrl());
        startupContext.setStatusMessage(refreshedContext.getStatusMessage());
        startupContext.setLastStatusRefreshTime(refreshedContext.getLastStatusRefreshTime());
        startupContext.setHasLocalToken(refreshedContext.isHasLocalToken());
        startupContext.setLastUsername(refreshedContext.getLastUsername());
        startupContext.setAppStatus(refreshedContext.getAppStatus());
        startupContext.setDatabaseStatus(refreshedContext.getDatabaseStatus());
        startupContext.setAiStatus(refreshedContext.getAiStatus());
        startupContext.setAiMessage(refreshedContext.getAiMessage());
        startupContext.setAiBaseUrl(refreshedContext.getAiBaseUrl());
        startupContext.setCurrentUsername(refreshedContext.getCurrentUsername());
        startupContext.setAuthRequired(refreshedContext.isAuthRequired());
        startupContext.setDesktopSupported(refreshedContext.isDesktopSupported());

        currentBaseUrlValueLabel.setText(resolveConfiguredBaseUrl(appConfigService.load()));
        lastUsernameValueLabel.setText(defaultText(startupContext.getLastUsername(), "-"));
        applyStartupSnapshot();
        if (onStartupContextRefreshed != null) {
            onStartupContextRefreshed.accept(startupContext);
        }
    }

    private String resolveStartupStateText(StartupState startupState) {
        if (startupState == null) {
            return "-";
        }
        return switch (startupState) {
            case BOOTSTRAPPING -> "启动检查中";
            case NEEDS_LOGIN -> "等待登录";
            case READY -> "已就绪";
            case ERROR -> "启动失败";
        };
    }

    private String resolveFailureReasonText(StartupState startupState, String statusMessage) {
        if (startupState == StartupState.ERROR && !isBlank(statusMessage)) {
            return statusMessage.trim();
        }
        return "无";
    }

    private StartupContext prepareLatestStartupContext() {
        StartupCoordinator startupCoordinator = new StartupCoordinator(appConfigService, new AuthService(appConfigService));
        return startupCoordinator.prepareInitialContext();
    }

    private String formatNow() {
        return LocalDateTime.now().format(TIME_FORMATTER);
    }

    private String extractDeepestMessage(Throwable throwable) {
        if (throwable == null) {
            return null;
        }
        String message = null;
        Throwable current = throwable;
        while (current != null) {
            if (!isBlank(current.getMessage())) {
                message = current.getMessage().trim();
            }
            current = current.getCause();
        }
        return message;
    }

    private String normalizeNativeMessage(String message) {
        if (message == null) {
            return "";
        }
        return message
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsUnreadableReplacement(String message) {
        return message != null && message.indexOf('\uFFFD') >= 0;
    }

    private String buildEncodedErrorFallback(String fallbackMessage) {
        if (fallbackMessage != null && fallbackMessage.contains("AI 服务")) {
            return fallbackMessage + " 原始系统错误信息存在乱码，请优先确认 python-ai-service/.venv/Scripts/python.exe 或 py 启动器可用，再点击“重新执行启动检查”。";
        }
        return fallbackMessage + " 原始系统错误信息存在乱码，请查看日志目录或重新执行启动检查。";
    }

    private String defaultText(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class TemporaryConfigService extends AppConfigService {
        private final AppConfig appConfig;

        private TemporaryConfigService(String baseUrl) {
            this.appConfig = new AppConfig();
            ServerConfig serverConfig = new ServerConfig();
            serverConfig.setBaseUrl(baseUrl);
            this.appConfig.setServerConfig(serverConfig);
        }

        @Override
        public AppConfig load() {
            return appConfig;
        }

        @Override
        public void save(AppConfig appConfig) {
        }
    }

    private record ConnectionTestResult(SystemHealthData healthData, SystemBootstrapData bootstrapData) {
    }

    private record ServiceStartResult(StartupContext startupContext, Path logPath, boolean serviceReady) {
    }

    private record ServiceStartAttempt(
            StartupContext startupContext,
            Path logPath,
            boolean serviceReady,
            boolean commandStarted,
            String errorMessage
    ) {
    }

    private record ServiceStopResult(
            StartupContext startupContext,
            boolean stopped,
            boolean stoppedByPid,
            boolean stoppedByPort,
            int fallbackPort
    ) {
    }

    private enum StartupExpectation {
        BACKEND_READY,
        AI_READY,
        BACKEND_DOWN,
        AI_DOWN
    }
}
