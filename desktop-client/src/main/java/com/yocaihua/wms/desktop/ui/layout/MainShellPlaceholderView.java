package com.yocaihua.wms.desktop.ui.layout;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import com.yocaihua.wms.desktop.bootstrap.StartupState;
import com.yocaihua.wms.desktop.config.AppConfigService;
import com.yocaihua.wms.desktop.module.ai.AiOutboundRecognizeData;
import com.yocaihua.wms.desktop.module.ai.AiModuleView;
import com.yocaihua.wms.desktop.module.customer.CustomerListView;
import com.yocaihua.wms.desktop.module.home.HomeDashboardView;
import com.yocaihua.wms.desktop.module.inbound.InboundModuleView;
import com.yocaihua.wms.desktop.module.outbound.OutboundModuleView;
import com.yocaihua.wms.desktop.module.product.ProductListView;
import com.yocaihua.wms.desktop.module.settings.SettingsView;
import com.yocaihua.wms.desktop.module.stock.StockModuleView;
import com.yocaihua.wms.desktop.module.supplier.SupplierListView;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class MainShellPlaceholderView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final AppConfigService appConfigService;
    private final Runnable onLogout;
    private final Runnable onGoLogin;
    private final BorderPane root;
    private final StackPane contentContainer;
    private final Map<ShellModule, Button> navButtons;
    private final Label moduleTitleLabel;
    private final Label moduleSubtitleLabel;
    private final Label userLabel;
    private final Label statusLabel;
    private final Label hintLabel;
    private final Label sidebarStatusLabel;
    private final Label sidebarRefreshLabel;
    private final HBox globalNoticeBox;
    private final Label globalNoticeLabel;
    private final Button globalNoticeActionButton;
    private final HBox draftNavBar;
    private final HBox draftTabContainer;
    private final List<DraftTab> openDraftTabs;
    private int inboundDraftSequence;
    private int outboundDraftSequence;
    private String activeDraftTabKey;
    private ShellModule currentModule;
    private ShellModule pendingBusinessReturnModule;
    private boolean openCreateAfterSwitch;
    private String pendingSupplierSearchCode;
    private String pendingSupplierSearchName;
    private String pendingCustomerSearchCode;
    private String pendingCustomerSearchName;
    private boolean openAiOutboundConfirmAfterSwitch;
    private AiOutboundRecognizeData pendingAiOutboundConfirmData;
    private String pendingAiOutboundCustomerSearchCode;
    private String pendingAiOutboundCustomerSearchName;
    private Integer pendingAiOutboundProductEditorIndex;
    private String pendingAiOutboundProductSearchCode;
    private String pendingAiOutboundProductSearchName;

    public MainShellPlaceholderView(StartupContext startupContext, ApiClient apiClient, AppConfigService appConfigService, Runnable onLogout, Runnable onGoLogin) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.appConfigService = appConfigService;
        this.onLogout = onLogout;
        this.onGoLogin = onGoLogin;
        this.root = new BorderPane();
        this.contentContainer = new StackPane();
        this.navButtons = new EnumMap<>(ShellModule.class);
        this.moduleTitleLabel = new Label();
        this.moduleSubtitleLabel = new Label();
        this.userLabel = new Label();
        this.statusLabel = new Label();
        this.hintLabel = new Label();
        this.sidebarStatusLabel = new Label();
        this.sidebarRefreshLabel = new Label();
        this.globalNoticeBox = new HBox();
        this.globalNoticeLabel = new Label();
        this.globalNoticeActionButton = new Button();
        this.draftNavBar = new HBox(12);
        this.draftTabContainer = new HBox(8);
        this.openDraftTabs = new ArrayList<>();
        this.inboundDraftSequence = 0;
        this.outboundDraftSequence = 0;
        this.activeDraftTabKey = null;
        this.root.getStyleClass().add("shell-root");

        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(20));
        sidebar.getStyleClass().add("shell-sidebar");

        Label brandLabel = new Label(startupContext.getAppDisplayName());
        brandLabel.getStyleClass().add("shell-brand");

        Label versionLabel = new Label("版本 " + defaultText(startupContext.getAppVersion(), "未配置"));
        versionLabel.getStyleClass().add("placeholder-note");

        Label sidebarStatusTitleLabel = new Label("桌面端状态");
        sidebarStatusTitleLabel.getStyleClass().add("page-label");
        sidebarStatusLabel.getStyleClass().add("page-label");
        sidebarStatusLabel.setWrapText(true);
        sidebarRefreshLabel.getStyleClass().add("placeholder-note");
        sidebarRefreshLabel.setWrapText(true);

        VBox sidebarStatusBox = new VBox(6, sidebarStatusTitleLabel, sidebarStatusLabel, sidebarRefreshLabel);
        sidebarStatusBox.setPadding(new Insets(10, 0, 4, 0));

        VBox navGroup = new VBox(8);
        navGroup.getStyleClass().add("shell-nav-group");
        for (ShellModule module : ShellModule.values()) {
            navGroup.getChildren().add(createNavButton(module));
        }

        sidebar.getChildren().addAll(brandLabel, versionLabel, sidebarStatusBox, navGroup);

        VBox header = new VBox(6);
        header.setPadding(new Insets(18, 20, 18, 20));
        header.getStyleClass().add("shell-header");

        moduleTitleLabel.getStyleClass().add("page-title");
        moduleSubtitleLabel.getStyleClass().add("page-subtitle");

        userLabel.getStyleClass().add("page-label");
        statusLabel.getStyleClass().add("page-label");

        Button logoutButton = new Button("退出登录");
        logoutButton.setOnAction(event -> onLogout.run());
        logoutButton.getStyleClass().add("shell-logout-button");

        VBox titleBox = new VBox(6, moduleTitleLabel, moduleSubtitleLabel);
        HBox userBox = new HBox(12, userLabel, statusLabel, logoutButton);
        userBox.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox headerTopRow = new HBox(16, titleBox, spacer, userBox);
        headerTopRow.setAlignment(Pos.CENTER_LEFT);

        hintLabel.getStyleClass().add("placeholder-note");

        globalNoticeBox.getStyleClass().add("shell-global-notice");
        globalNoticeBox.setManaged(false);
        globalNoticeBox.setVisible(false);
        globalNoticeBox.setAlignment(Pos.CENTER_LEFT);
        globalNoticeBox.setSpacing(12);
        globalNoticeBox.setPadding(new Insets(10, 20, 10, 20));
        globalNoticeLabel.getStyleClass().add("shell-global-notice-text");
        globalNoticeLabel.setWrapText(true);
        globalNoticeActionButton.getStyleClass().add("shell-global-notice-action");
        globalNoticeActionButton.setVisible(false);
        globalNoticeActionButton.setManaged(false);
        globalNoticeBox.getChildren().addAll(globalNoticeLabel, globalNoticeActionButton);

        MenuBar menuBar = createMenuBar();
        HBox toolBar = createToolBar();
        HBox draftNavBarView = createDraftNavBar();

        VBox topContainer = new VBox(menuBar, toolBar, draftNavBarView, header, globalNoticeBox);
        header.getChildren().addAll(headerTopRow, hintLabel);

        contentContainer.getStyleClass().add("shell-content");

        ScrollPane sidebarScrollPane = new ScrollPane(sidebar);
        sidebarScrollPane.setFitToWidth(true);
        sidebarScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScrollPane.getStyleClass().add("shell-sidebar-scroll");

        root.setLeft(sidebarScrollPane);
        root.setTop(topContainer);
        root.setCenter(contentContainer);

        refreshHeaderStatus();
        switchModule(ShellModule.HOME);
    }

    public Parent getRoot() {
        return root;
    }

    private Button createNavButton(ShellModule module) {
        Button button = new Button(module.getNavTitle());
        button.getStyleClass().add("shell-nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> switchModule(module));
        navButtons.put(module, button);
        return button;
    }

    private void switchModule(ShellModule module) {
        clearPendingBusinessReturnIfNeeded(module);
        activateModuleContent(module, createModuleRoot(module));
    }

    private Parent createModuleRoot(ShellModule module) {
        if (module == ShellModule.HOME) {
            return new HomeDashboardView(startupContext, apiClient).getRoot();
        }
        if (module == ShellModule.PRODUCT) {
            String returnLabel = pendingBusinessReturnModule == ShellModule.AI ? "返回AI出库确认页" : null;
            Runnable onReturn = pendingBusinessReturnModule == ShellModule.AI ? this::returnToPendingBusinessPage : null;
            BiConsumer<String, String> onCreatedForBusiness = pendingBusinessReturnModule == ShellModule.AI
                    ? this::rememberCreatedProductForAiOutbound
                    : null;
            return new ProductListView(startupContext, apiClient, returnLabel, onReturn, onCreatedForBusiness).getRoot();
        }
        if (module == ShellModule.CUSTOMER) {
            boolean returnToOutbound = pendingBusinessReturnModule == ShellModule.OUTBOUND;
            boolean returnToAi = pendingBusinessReturnModule == ShellModule.AI;
            String returnLabel = returnToOutbound ? "返回出库草稿页" : (returnToAi ? "返回AI出库确认页" : null);
            Runnable onReturn = (returnToOutbound || returnToAi) ? this::returnToPendingBusinessPage : null;
            BiConsumer<String, String> onCreatedForBusiness = returnToOutbound
                    ? this::rememberCreatedCustomerForOutbound
                    : (returnToAi ? this::rememberCreatedCustomerForAiOutbound : null);
            return new CustomerListView(startupContext, apiClient, returnLabel, onReturn, onCreatedForBusiness).getRoot();
        }
        if (module == ShellModule.SUPPLIER) {
            String returnLabel = pendingBusinessReturnModule == ShellModule.INBOUND ? "返回入库草稿页" : null;
            Runnable onReturn = pendingBusinessReturnModule == ShellModule.INBOUND ? this::returnToPendingBusinessPage : null;
            BiConsumer<String, String> onCreatedForBusiness = pendingBusinessReturnModule == ShellModule.INBOUND
                    ? this::rememberCreatedSupplierForInbound
                    : null;
            return new SupplierListView(startupContext, apiClient, returnLabel, onReturn, onCreatedForBusiness).getRoot();
        }
        if (module == ShellModule.STOCK) {
            return new StockModuleView(startupContext, apiClient).getRoot();
        }
        if (module == ShellModule.INBOUND) {
            boolean openCreate = consumeOpenCreateFlag(ShellModule.INBOUND);
            return new InboundModuleView(
                    startupContext,
                    apiClient,
                    this::openSupplierManagementFromInboundCreate,
                    this::handleInboundDraftStateChanged,
                    openCreate,
                    consumePendingSupplierSearchCode(),
                    consumePendingSupplierSearchName()
            ).getRoot();
        }
        if (module == ShellModule.OUTBOUND) {
            boolean openCreate = consumeOpenCreateFlag(ShellModule.OUTBOUND);
            return new OutboundModuleView(
                    startupContext,
                    apiClient,
                    this::openCustomerManagementFromOutboundCreate,
                    this::handleOutboundDraftStateChanged,
                    openCreate,
                    consumePendingCustomerSearchCode(),
                    consumePendingCustomerSearchName()
            ).getRoot();
        }
        if (module == ShellModule.AI) {
            boolean openOutboundConfirm = consumeOpenAiOutboundConfirmFlag();
            return new AiModuleView(
                    startupContext,
                    apiClient,
                    this::openCustomerManagementFromAiOutboundConfirm,
                    this::openProductManagementFromAiOutboundConfirm,
                    openOutboundConfirm ? consumePendingAiOutboundConfirmData() : null,
                    openOutboundConfirm ? consumePendingAiOutboundCustomerSearchCode() : null,
                    openOutboundConfirm ? consumePendingAiOutboundCustomerSearchName() : null,
                    openOutboundConfirm ? consumePendingAiOutboundProductEditorIndex() : null,
                    openOutboundConfirm ? consumePendingAiOutboundProductSearchCode() : null,
                    openOutboundConfirm ? consumePendingAiOutboundProductSearchName() : null
            ).getRoot();
        }
        if (module == ShellModule.SETTINGS) {
            return new SettingsView(startupContext, apiClient, appConfigService, this::handleStartupContextRefreshed).getRoot();
        }
        return new ModulePlaceholderView(module, startupContext).getRoot();
    }

    private void refreshNavState() {
        for (Map.Entry<ShellModule, Button> entry : navButtons.entrySet()) {
            Button button = entry.getValue();
            button.getStyleClass().remove("shell-nav-button-active");
            if (entry.getKey() == currentModule) {
                button.getStyleClass().add("shell-nav-button-active");
            }
        }
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu systemMenu = new Menu("系统");
        MenuItem settingsItem = new MenuItem("系统设置");
        settingsItem.setOnAction(event -> switchModule(ShellModule.SETTINGS));
        MenuItem logoutItem = new MenuItem("退出登录");
        logoutItem.setOnAction(event -> onLogout.run());
        systemMenu.getItems().addAll(settingsItem, logoutItem);

        Menu viewMenu = new Menu("视图");
        MenuItem homeItem = new MenuItem("工作台");
        homeItem.setOnAction(event -> switchModule(ShellModule.HOME));
        MenuItem stockItem = new MenuItem("库存管理");
        stockItem.setOnAction(event -> switchModule(ShellModule.STOCK));
        viewMenu.getItems().addAll(homeItem, stockItem);

        Menu bizMenu = new Menu("业务");
        MenuItem openInboundDraftItem = new MenuItem("入库草稿");
        openInboundDraftItem.setOnAction(event -> openDraftModule(ShellModule.INBOUND));
        MenuItem openOutboundDraftItem = new MenuItem("出库草稿");
        openOutboundDraftItem.setOnAction(event -> openDraftModule(ShellModule.OUTBOUND));
        bizMenu.getItems().addAll(openInboundDraftItem, openOutboundDraftItem);

        menuBar.getMenus().addAll(systemMenu, viewMenu, bizMenu);
        return menuBar;
    }

    private HBox createToolBar() {
        HBox toolBar = new HBox(10);
        toolBar.setPadding(new Insets(8, 20, 8, 20));
        toolBar.setAlignment(Pos.CENTER_LEFT);

        Button homeButton = new Button("工作台");
        homeButton.setOnAction(event -> switchModule(ShellModule.HOME));
        Button inboundDraftButton = new Button("入库草稿");
        inboundDraftButton.setOnAction(event -> openDraftModule(ShellModule.INBOUND));
        Button outboundDraftButton = new Button("出库草稿");
        outboundDraftButton.setOnAction(event -> openDraftModule(ShellModule.OUTBOUND));
        Button settingsButton = new Button("系统设置");
        settingsButton.setOnAction(event -> switchModule(ShellModule.SETTINGS));

        toolBar.getChildren().addAll(homeButton, inboundDraftButton, outboundDraftButton, settingsButton);
        return toolBar;
    }

    private HBox createDraftNavBar() {
        draftNavBar.setPadding(new Insets(8, 20, 8, 20));
        draftNavBar.setAlignment(Pos.CENTER_LEFT);
        draftNavBar.getStyleClass().add("shell-global-notice");

        Label navTitle = new Label("草稿导航");
        navTitle.getStyleClass().add("page-label");

        draftTabContainer.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(draftTabContainer, Priority.ALWAYS);

        Button quickAddInboundButton = new Button("+ 入库草稿");
        quickAddInboundButton.setOnAction(event -> openDraftModule(ShellModule.INBOUND));

        Button quickAddOutboundButton = new Button("+ 出库草稿");
        quickAddOutboundButton.setOnAction(event -> openDraftModule(ShellModule.OUTBOUND));

        draftNavBar.getChildren().addAll(navTitle, draftTabContainer, quickAddInboundButton, quickAddOutboundButton);
        refreshDraftNavBar();
        return draftNavBar;
    }

    private void openDraftModule(ShellModule module) {
        if (module != ShellModule.INBOUND && module != ShellModule.OUTBOUND) {
            return;
        }
        DraftTab draftTab = createDraftTab(module);
        openDraftTabs.add(draftTab);
        activeDraftTabKey = draftTab.key();
        openDraftTab(draftTab.key());
        refreshDraftNavBar();
    }

    private void handleInboundDraftStateChanged(String statusText) {
        handleDraftStateChanged(ShellModule.INBOUND, statusText);
    }

    private void handleOutboundDraftStateChanged(String statusText) {
        handleDraftStateChanged(ShellModule.OUTBOUND, statusText);
    }

    private void refreshDraftNavBar() {
        draftTabContainer.getChildren().clear();
        if (openDraftTabs.isEmpty()) {
            draftNavBar.setVisible(false);
            draftNavBar.setManaged(false);
            return;
        }

        draftNavBar.setVisible(true);
        draftNavBar.setManaged(true);
        for (DraftTab draftTab : openDraftTabs) {
            Button tabButton = new Button(draftTab.title());
            if (draftTab.key().equals(activeDraftTabKey)) {
                tabButton.setStyle("-fx-font-weight: bold;");
            }
            tabButton.setOnAction(event -> openDraftTab(draftTab.key()));

            Button closeButton = new Button("×");
            closeButton.setOnAction(event -> closeDraftTab(draftTab.key()));

            HBox tabBox = new HBox(6, tabButton, closeButton);
            tabBox.setAlignment(Pos.CENTER_LEFT);
            draftTabContainer.getChildren().add(tabBox);
        }
    }

    private void openDraftTab(String tabKey) {
        DraftTab draftTab = findDraftTabByKey(tabKey);
        if (draftTab == null) {
            return;
        }
        activeDraftTabKey = draftTab.key();
        clearPendingBusinessReturnIfNeeded(draftTab.module());
        activateModuleContent(draftTab.module(), draftTab.contentRoot());
        refreshDraftNavBar();
    }

    private void closeDraftTab(String tabKey) {
        int index = findDraftTabIndexByKey(tabKey);
        if (index < 0) {
            return;
        }
        openDraftTabs.remove(index);
        if (openDraftTabs.isEmpty()) {
            activeDraftTabKey = null;
            refreshDraftNavBar();
            return;
        }

        if (tabKey.equals(activeDraftTabKey)) {
            DraftTab nextTab = openDraftTabs.get(Math.max(0, index - 1));
            activeDraftTabKey = nextTab.key();
            openDraftTab(nextTab.key());
            return;
        }
        refreshDraftNavBar();
    }

    private void handleDraftStateChanged(ShellModule module, String statusText) {
        String normalizedText = defaultText(statusText, "未打开");
        if (normalizedText.contains("编辑中")) {
            ensureActiveDraftTab(module);
            refreshDraftNavBar();
            return;
        }
        if (normalizedText.contains("未打开")) {
            closeActiveDraftTabByModule(module);
            return;
        }
        refreshDraftNavBar();
    }

    private void ensureActiveDraftTab(ShellModule module) {
        DraftTab activeTab = findDraftTabByKey(activeDraftTabKey);
        if (activeTab != null && activeTab.module() == module) {
            return;
        }

        DraftTab existedTab = openDraftTabs.stream()
                .filter(item -> item.module() == module)
                .findFirst()
                .orElse(null);
        if (existedTab != null) {
            activeDraftTabKey = existedTab.key();
            return;
        }

        DraftTab draftTab = createDraftTab(module);
        openDraftTabs.add(draftTab);
        activeDraftTabKey = draftTab.key();
    }

    private void closeActiveDraftTabByModule(ShellModule module) {
        DraftTab activeTab = findDraftTabByKey(activeDraftTabKey);
        if (activeTab == null || activeTab.module() != module) {
            refreshDraftNavBar();
            return;
        }
        closeDraftTab(activeTab.key());
    }

    private DraftTab createDraftTab(ShellModule module) {
        if (module == ShellModule.INBOUND) {
            inboundDraftSequence += 1;
            String key = "INBOUND-" + inboundDraftSequence;
            return new DraftTab(key, module, "入库草稿#" + inboundDraftSequence, createDraftModuleRoot(module));
        }
        outboundDraftSequence += 1;
        String key = "OUTBOUND-" + outboundDraftSequence;
        return new DraftTab(key, module, "出库草稿#" + outboundDraftSequence, createDraftModuleRoot(module));
    }

    private Parent createDraftModuleRoot(ShellModule module) {
        if (module == ShellModule.INBOUND) {
            return new InboundModuleView(
                    startupContext,
                    apiClient,
                    this::openSupplierManagementFromInboundCreate,
                    this::handleInboundDraftStateChanged,
                    true,
                    null,
                    null
            ).getRoot();
        }
        return new OutboundModuleView(
                startupContext,
                apiClient,
                this::openCustomerManagementFromOutboundCreate,
                this::handleOutboundDraftStateChanged,
                true,
                null,
                null
        ).getRoot();
    }

    private DraftTab findDraftTabByKey(String tabKey) {
        if (tabKey == null || tabKey.trim().isEmpty()) {
            return null;
        }
        return openDraftTabs.stream()
                .filter(item -> item.key().equals(tabKey))
                .findFirst()
                .orElse(null);
    }

    private int findDraftTabIndexByKey(String tabKey) {
        if (tabKey == null || tabKey.trim().isEmpty()) {
            return -1;
        }
        for (int i = 0; i < openDraftTabs.size(); i++) {
            if (tabKey.equals(openDraftTabs.get(i).key())) {
                return i;
            }
        }
        return -1;
    }

    private void activateModuleContent(ShellModule module, Parent contentRoot) {
        this.currentModule = module;
        moduleTitleLabel.setText(startupContext.getAppDisplayName() + " / " + module.getPageTitle());
        moduleSubtitleLabel.setText(module.getDescription());
        contentContainer.getChildren().setAll(wrapMainContent(contentRoot));
        refreshNavState();
        refreshDraftNavBar();
    }

    private Parent wrapMainContent(Parent contentRoot) {
        ScrollPane scrollPane = new ScrollPane(contentRoot);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(false);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.getStyleClass().add("shell-content-scroll");
        return scrollPane;
    }

    private void refreshHeaderStatus() {
        String startupStateText = resolveStartupStateText();
        String refreshTimeText = defaultText(startupContext.getLastStatusRefreshTime(), "未记录");
        userLabel.setText("当前用户：" + defaultText(startupContext.getCurrentUsername(), "未知"));
        statusLabel.setText(
                "服务状态：后端 " + defaultText(startupContext.getAppStatus(), "UNKNOWN")
                        + " / 数据库 " + defaultText(startupContext.getDatabaseStatus(), "UNKNOWN")
                        + " / AI " + defaultText(startupContext.getAiStatus(), "UNKNOWN")
        );
        hintLabel.setText(startupStateText + "｜" + defaultText(startupContext.getStatusMessage(), "状态待更新") + "｜最近检查：" + refreshTimeText);
        sidebarStatusLabel.setText(
                "运行状态：" + startupStateText
                        + "\n后端 " + defaultText(startupContext.getAppStatus(), "UNKNOWN")
                        + " / 数据库 " + defaultText(startupContext.getDatabaseStatus(), "UNKNOWN")
                        + " / AI " + defaultText(startupContext.getAiStatus(), "UNKNOWN")
        );
        sidebarRefreshLabel.setText("最近检查：" + refreshTimeText);
    }

    private void handleStartupContextRefreshed(StartupContext refreshedContext) {
        refreshHeaderStatus();
        if (refreshedContext == null) {
            return;
        }
        if (refreshedContext.getStartupState() == StartupState.ERROR || refreshedContext.getStartupState() == StartupState.NEEDS_LOGIN) {
            showGlobalNotice(refreshedContext);
        } else {
            hideGlobalNotice();
        }
    }

    private void showGlobalNotice(StartupContext refreshedContext) {
        globalNoticeLabel.setText(defaultText(resolveGlobalNoticeText(refreshedContext), "状态已更新"));
        configureGlobalNoticeAction(refreshedContext);
        globalNoticeBox.setManaged(true);
        globalNoticeBox.setVisible(true);

        PauseTransition pauseTransition = new PauseTransition(Duration.seconds(6));
        pauseTransition.setOnFinished(event -> hideGlobalNotice());
        pauseTransition.play();
    }

    private void hideGlobalNotice() {
        globalNoticeBox.setVisible(false);
        globalNoticeBox.setManaged(false);
        globalNoticeActionButton.setVisible(false);
        globalNoticeActionButton.setManaged(false);
        globalNoticeActionButton.setOnAction(null);
    }

    private String resolveGlobalNoticeText(StartupContext refreshedContext) {
        String statusMessage = defaultText(refreshedContext.getStatusMessage(), "请检查当前桌面端连接状态。");
        if (refreshedContext.getStartupState() == StartupState.NEEDS_LOGIN) {
            return "启动检查结果：当前需要重新登录。 " + statusMessage;
        }
        return "启动检查结果：当前服务不可正常进入桌面端。 " + statusMessage;
    }

    private void configureGlobalNoticeAction(StartupContext refreshedContext) {
        if (refreshedContext.getStartupState() == StartupState.NEEDS_LOGIN) {
            globalNoticeActionButton.setText("去登录");
            globalNoticeActionButton.setOnAction(event -> onGoLogin.run());
            globalNoticeActionButton.setVisible(true);
            globalNoticeActionButton.setManaged(true);
            return;
        }
        if (refreshedContext.getStartupState() == StartupState.ERROR) {
            globalNoticeActionButton.setText("去设置页");
            globalNoticeActionButton.setOnAction(event -> switchModule(ShellModule.SETTINGS));
            globalNoticeActionButton.setVisible(true);
            globalNoticeActionButton.setManaged(true);
            return;
        }
        globalNoticeActionButton.setVisible(false);
        globalNoticeActionButton.setManaged(false);
        globalNoticeActionButton.setOnAction(null);
    }

    private String resolveStartupStateText() {
        if (startupContext.getStartupState() == null) {
            return "状态未知";
        }
        return switch (startupContext.getStartupState()) {
            case BOOTSTRAPPING -> "启动检查中";
            case NEEDS_LOGIN -> "等待登录";
            case READY -> "已就绪";
            case ERROR -> "启动失败";
        };
    }

    private void openSupplierManagementFromInboundCreate() {
        pendingBusinessReturnModule = ShellModule.INBOUND;
        switchModule(ShellModule.SUPPLIER);
    }

    private void openCustomerManagementFromOutboundCreate() {
        pendingBusinessReturnModule = ShellModule.OUTBOUND;
        switchModule(ShellModule.CUSTOMER);
    }

    private void openCustomerManagementFromAiOutboundConfirm(AiOutboundRecognizeData detailData) {
        pendingBusinessReturnModule = ShellModule.AI;
        pendingAiOutboundConfirmData = detailData;
        switchModule(ShellModule.CUSTOMER);
    }

    private void openProductManagementFromAiOutboundConfirm(AiOutboundRecognizeData detailData, Integer editorIndex) {
        pendingBusinessReturnModule = ShellModule.AI;
        pendingAiOutboundConfirmData = detailData;
        pendingAiOutboundProductEditorIndex = editorIndex;
        switchModule(ShellModule.PRODUCT);
    }

    private void rememberCreatedSupplierForInbound(String supplierCode, String supplierName) {
        pendingSupplierSearchCode = blankToNull(supplierCode);
        pendingSupplierSearchName = blankToNull(supplierName);
    }

    private void rememberCreatedCustomerForOutbound(String customerCode, String customerName) {
        pendingCustomerSearchCode = blankToNull(customerCode);
        pendingCustomerSearchName = blankToNull(customerName);
    }

    private void rememberCreatedCustomerForAiOutbound(String customerCode, String customerName) {
        pendingAiOutboundCustomerSearchCode = blankToNull(customerCode);
        pendingAiOutboundCustomerSearchName = blankToNull(customerName);
    }

    private void rememberCreatedProductForAiOutbound(String productCode, String productName) {
        pendingAiOutboundProductSearchCode = blankToNull(productCode);
        pendingAiOutboundProductSearchName = blankToNull(productName);
    }

    private void returnToPendingBusinessPage() {
        if (pendingBusinessReturnModule == null) {
            return;
        }
        ShellModule targetModule = pendingBusinessReturnModule;
        if (targetModule == ShellModule.AI) {
            openAiOutboundConfirmAfterSwitch = true;
        } else {
            openCreateAfterSwitch = true;
        }
        pendingBusinessReturnModule = null;
        switchModule(targetModule);
    }

    private boolean consumeOpenCreateFlag(ShellModule module) {
        if (!openCreateAfterSwitch) {
            return false;
        }
        if (module != ShellModule.INBOUND && module != ShellModule.OUTBOUND) {
            return false;
        }
        openCreateAfterSwitch = false;
        return true;
    }

    private boolean consumeOpenAiOutboundConfirmFlag() {
        if (!openAiOutboundConfirmAfterSwitch) {
            return false;
        }
        openAiOutboundConfirmAfterSwitch = false;
        return true;
    }

    private String consumePendingSupplierSearchCode() {
        String value = pendingSupplierSearchCode;
        pendingSupplierSearchCode = null;
        return value;
    }

    private String consumePendingSupplierSearchName() {
        String value = pendingSupplierSearchName;
        pendingSupplierSearchName = null;
        return value;
    }

    private String consumePendingCustomerSearchCode() {
        String value = pendingCustomerSearchCode;
        pendingCustomerSearchCode = null;
        return value;
    }

    private String consumePendingCustomerSearchName() {
        String value = pendingCustomerSearchName;
        pendingCustomerSearchName = null;
        return value;
    }

    private AiOutboundRecognizeData consumePendingAiOutboundConfirmData() {
        AiOutboundRecognizeData value = pendingAiOutboundConfirmData;
        pendingAiOutboundConfirmData = null;
        return value;
    }

    private String consumePendingAiOutboundCustomerSearchCode() {
        String value = pendingAiOutboundCustomerSearchCode;
        pendingAiOutboundCustomerSearchCode = null;
        return value;
    }

    private String consumePendingAiOutboundCustomerSearchName() {
        String value = pendingAiOutboundCustomerSearchName;
        pendingAiOutboundCustomerSearchName = null;
        return value;
    }

    private Integer consumePendingAiOutboundProductEditorIndex() {
        Integer value = pendingAiOutboundProductEditorIndex;
        pendingAiOutboundProductEditorIndex = null;
        return value;
    }

    private String consumePendingAiOutboundProductSearchCode() {
        String value = pendingAiOutboundProductSearchCode;
        pendingAiOutboundProductSearchCode = null;
        return value;
    }

    private String consumePendingAiOutboundProductSearchName() {
        String value = pendingAiOutboundProductSearchName;
        pendingAiOutboundProductSearchName = null;
        return value;
    }

    private void clearPendingBusinessReturnIfNeeded(ShellModule targetModule) {
        if (pendingBusinessReturnModule == null || currentModule == null) {
            return;
        }
        if (currentModule == ShellModule.SUPPLIER
                && pendingBusinessReturnModule == ShellModule.INBOUND
                && targetModule != ShellModule.SUPPLIER
                && targetModule != ShellModule.INBOUND) {
            pendingBusinessReturnModule = null;
            openCreateAfterSwitch = false;
            pendingSupplierSearchCode = null;
            pendingSupplierSearchName = null;
            return;
        }
        if (currentModule == ShellModule.CUSTOMER
                && pendingBusinessReturnModule == ShellModule.OUTBOUND
                && targetModule != ShellModule.CUSTOMER
                && targetModule != ShellModule.OUTBOUND) {
            pendingBusinessReturnModule = null;
            openCreateAfterSwitch = false;
            pendingCustomerSearchCode = null;
            pendingCustomerSearchName = null;
            return;
        }
        if ((currentModule == ShellModule.CUSTOMER || currentModule == ShellModule.PRODUCT)
                && pendingBusinessReturnModule == ShellModule.AI
                && targetModule != ShellModule.CUSTOMER
                && targetModule != ShellModule.PRODUCT
                && targetModule != ShellModule.AI) {
            pendingBusinessReturnModule = null;
            openAiOutboundConfirmAfterSwitch = false;
            pendingAiOutboundConfirmData = null;
            pendingAiOutboundCustomerSearchCode = null;
            pendingAiOutboundCustomerSearchName = null;
            pendingAiOutboundProductEditorIndex = null;
            pendingAiOutboundProductSearchCode = null;
            pendingAiOutboundProductSearchName = null;
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private record DraftTab(String key, ShellModule module, String title, Parent contentRoot) {
    }
}
