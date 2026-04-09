package com.yocaihua.wms.desktop.module.ai;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.AiApi;
import com.yocaihua.wms.desktop.api.endpoint.CustomerApi;
import com.yocaihua.wms.desktop.api.endpoint.ProductApi;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import com.yocaihua.wms.desktop.module.customer.CustomerPageData;
import com.yocaihua.wms.desktop.module.customer.CustomerRow;
import com.yocaihua.wms.desktop.module.product.ProductPageData;
import com.yocaihua.wms.desktop.module.product.ProductRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AiOutboundConfirmView {

    private final ApiClient apiClient;
    private final AiOutboundRecognizeData draftData;
    private final Runnable onBack;
    private final Consumer<Long> onConfirmed;
    private final Runnable onGoCustomerManagement;
    private final Consumer<Integer> onGoProductManagement;
    private final String initialCustomerCode;
    private final String initialCustomerName;
    private final Integer initialProductEditorIndex;
    private final String initialProductCode;
    private final String initialProductName;
    private final VBox root;
    private final TextField customerNameField;
    private final TextField customerCodeSearchField;
    private final TextField customerNameSearchField;
    private final Button customerSearchButton;
    private final ComboBox<CustomerRow> customerBox;
    private final ObservableList<CustomerRow> customerOptions;
    private final Label customerInfoLabel;
    private final TextField remarkField;
    private final TextArea rawTextArea;
    private final VBox itemContainer;
    private final Label uploadNoticeLabel;
    private final Label matchedCustomerIdValueLabel;
    private final Label customerMatchStatusValueLabel;
    private final Label recognitionStatusValueLabel;
    private final Label warningSummaryValueLabel;
    private final Label itemMatchSummaryLabel;
    private final Label statusLabel;
    private final Button confirmButton;
    private final Button addItemButton;
    private final List<ConfirmItemEditor> editors;
    private boolean pendingAutoCustomerSearchNotice;
    private String currentRecognitionStatus;
    private Long currentConfirmedOrderId;

    public AiOutboundConfirmView(
            StartupContext startupContext,
            ApiClient apiClient,
            AiOutboundRecognizeData draftData,
            Runnable onBack,
            Consumer<Long> onConfirmed,
            Runnable onGoCustomerManagement,
            Consumer<Integer> onGoProductManagement,
            String initialCustomerCode,
            String initialCustomerName,
            Integer initialProductEditorIndex,
            String initialProductCode,
            String initialProductName
    ) {
        this.apiClient = apiClient;
        this.draftData = draftData;
        this.onBack = onBack;
        this.onConfirmed = onConfirmed;
        this.onGoCustomerManagement = onGoCustomerManagement;
        this.onGoProductManagement = onGoProductManagement;
        this.initialCustomerCode = initialCustomerCode;
        this.initialCustomerName = initialCustomerName;
        this.initialProductEditorIndex = initialProductEditorIndex;
        this.initialProductCode = initialProductCode;
        this.initialProductName = initialProductName;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));
        this.customerOptions = FXCollections.observableArrayList();
        this.editors = new ArrayList<>();

        VBox card = new VBox(16);
        card.getStyleClass().add("page-card");

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("AI出库确认");
        titleLabel.getStyleClass().add("page-title");

        this.confirmButton = new Button("确认生成出库单");
        this.addItemButton = new Button("新增明细");
        Button backButton = new Button("返回AI列表");
        backButton.setOnAction(event -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(titleLabel, spacer, addItemButton, confirmButton, backButton);

        this.uploadNoticeLabel = new Label();
        this.uploadNoticeLabel.getStyleClass().add("page-label");
        this.uploadNoticeLabel.setWrapText(true);
        this.uploadNoticeLabel.setStyle("-fx-text-fill: #409eff; -fx-font-weight: 700;");

        GridPane summaryGrid = new GridPane();
        summaryGrid.setHgap(16);
        summaryGrid.setVgap(12);

        this.matchedCustomerIdValueLabel = createValueLabel();
        this.customerMatchStatusValueLabel = createValueLabel();
        this.recognitionStatusValueLabel = createValueLabel();
        this.warningSummaryValueLabel = createValueLabel();
        addSummaryRow(summaryGrid, 0, "匹配客户ID", matchedCustomerIdValueLabel, "客户匹配状态", customerMatchStatusValueLabel);
        addSummaryRow(summaryGrid, 1, "识别状态", recognitionStatusValueLabel, "Warning 摘要", warningSummaryValueLabel);

        VBox baseForm = new VBox(12);
        baseForm.setPadding(new Insets(4, 0, 4, 0));

        HBox customerNameRow = new HBox(12);
        customerNameRow.setAlignment(Pos.CENTER_LEFT);
        Label customerNameLabel = createFieldLabel("客户名称");
        this.customerNameField = new TextField();
        this.customerNameField.setPromptText("请输入客户名称");
        HBox.setHgrow(customerNameField, Priority.ALWAYS);
        customerNameRow.getChildren().addAll(customerNameLabel, customerNameField);

        HBox customerRow = new HBox(12);
        customerRow.setAlignment(Pos.CENTER_LEFT);
        Label customerLabel = createFieldLabel("匹配客户");
        this.customerCodeSearchField = new TextField();
        this.customerCodeSearchField.setPromptText("客户编码");
        this.customerCodeSearchField.setPrefWidth(140);
        this.customerNameSearchField = new TextField();
        this.customerNameSearchField.setPromptText("客户名称");
        this.customerNameSearchField.setPrefWidth(180);
        this.customerSearchButton = new Button("搜索客户");
        Button customerManageButton = new Button("去客户管理");
        customerManageButton.setOnAction(event -> handleGoCustomerManagement());
        this.customerBox = new ComboBox<>(customerOptions);
        this.customerBox.setPrefWidth(320);
        this.customerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CustomerRow object) {
                return formatCustomerOption(object);
            }

            @Override
            public CustomerRow fromString(String string) {
                return null;
            }
        });
        this.customerBox.setOnAction(event -> refreshCustomerInfo());
        HBox.setHgrow(customerBox, Priority.ALWAYS);
        customerRow.getChildren().addAll(
                customerLabel,
                customerCodeSearchField,
                customerNameSearchField,
                customerSearchButton,
                customerManageButton,
                customerBox
        );

        this.customerInfoLabel = new Label("请先按客户编码或名称搜索，再从下拉框中选择。");
        this.customerInfoLabel.getStyleClass().add("placeholder-note");
        this.customerInfoLabel.setWrapText(true);

        HBox remarkRow = new HBox(12);
        remarkRow.setAlignment(Pos.CENTER_LEFT);
        Label remarkLabel = createFieldLabel("单据备注");
        this.remarkField = new TextField();
        this.remarkField.setPromptText("请输入备注");
        HBox.setHgrow(remarkField, Priority.ALWAYS);
        remarkRow.getChildren().addAll(remarkLabel, remarkField);

        Label rawTextLabel = createFieldLabel("原始文本");
        this.rawTextArea = new TextArea();
        this.rawTextArea.setWrapText(true);
        this.rawTextArea.setPrefRowCount(4);

        baseForm.getChildren().addAll(customerNameRow, customerRow, customerInfoLabel, remarkRow, rawTextLabel, rawTextArea);

        Label itemTitle = new Label("确认明细");
        itemTitle.getStyleClass().add("page-title");
        itemTitle.setStyle("-fx-font-size: 18px;");

        this.itemMatchSummaryLabel = new Label();
        this.itemMatchSummaryLabel.getStyleClass().add("placeholder-note");
        this.itemMatchSummaryLabel.setWrapText(true);

        this.itemContainer = new VBox(12);
        ScrollPane scrollPane = new ScrollPane(itemContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(460);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.statusLabel = new Label("正在准备AI出库确认草稿...");
        this.statusLabel.getStyleClass().add("page-subtitle");

        card.getChildren().addAll(headerRow, uploadNoticeLabel, summaryGrid, baseForm, itemTitle, itemMatchSummaryLabel, scrollPane, statusLabel);
        root.getChildren().add(card);

        bindActions();
        renderDraft();
        setLoadingState(false, canConfirm() ? "请确认客户和商品匹配后生成正式出库单。" : "当前AI出库记录不能继续确认。");
    }

    public Parent getRoot() {
        return root;
    }

    private void bindActions() {
        addItemButton.setOnAction(event -> addItemEditor(null));
        confirmButton.setOnAction(event -> handleConfirm());
        customerSearchButton.setOnAction(event -> searchCustomers());
    }

    private void renderDraft() {
        currentRecognitionStatus = draftData == null ? null : draftData.getRecognitionStatus();
        currentConfirmedOrderId = draftData == null ? null : draftData.getConfirmedOrderId();

        String uploadNotice = buildUploadNotice(draftData);
        uploadNoticeLabel.setText(defaultText(uploadNotice, ""));
        uploadNoticeLabel.setManaged(!isBlank(uploadNotice));
        uploadNoticeLabel.setVisible(!isBlank(uploadNotice));
        matchedCustomerIdValueLabel.setText(detailText(draftData == null ? null : draftData.getMatchedCustomerIdText()));
        customerMatchStatusValueLabel.setText(detailText(draftData == null ? null : draftData.getCustomerMatchStatusText()));
        recognitionStatusValueLabel.setText(detailText(draftData == null ? null : draftData.getRecognitionStatusText()));
        warningSummaryValueLabel.setText(detailText(draftData == null ? null : draftData.getWarningText()));
        customerNameField.setText(defaultTextForInput(draftData == null ? null : draftData.getCustomerName()));
        remarkField.setText("AI识别确认生成出库单");
        rawTextArea.setText(draftData == null || draftData.getRawText() == null ? "" : draftData.getRawText());

        customerOptions.clear();
        customerBox.setValue(null);
        if (draftData != null && draftData.getMatchedCustomerId() != null) {
            CustomerRow customer = new CustomerRow();
            customer.setId(draftData.getMatchedCustomerId());
            customer.setCustomerCode("ID:" + draftData.getMatchedCustomerId());
            customer.setCustomerName(draftData.getCustomerName());
            customerOptions.add(customer);
            customerBox.setValue(customer);
        }
        refreshCustomerInfo();

        editors.clear();
        itemContainer.getChildren().clear();
        List<AiOutboundRecognizeItemRow> items = draftData == null ? null : draftData.getItemList();
        if (items == null || items.isEmpty()) {
            addItemEditor(null);
        } else {
            for (AiOutboundRecognizeItemRow item : items) {
                addItemEditor(item);
            }
        }
        refreshItemEditors();
        refreshItemMatchSummary();
        applyInitialCustomerSearch();
        applyInitialProductSearch();
    }

    private void addItemEditor(AiOutboundRecognizeItemRow item) {
        ConfirmItemEditor editor = new ConfirmItemEditor(item);
        editors.add(editor);
        itemContainer.getChildren().add(editor.getRoot());
        refreshItemEditors();
        refreshItemMatchSummary();
    }

    private void removeItemEditor(ConfirmItemEditor editor) {
        if (editors.size() <= 1) {
            statusLabel.setText("至少保留一条确认明细。");
            return;
        }
        editors.remove(editor);
        itemContainer.getChildren().remove(editor.getRoot());
        refreshItemEditors();
        refreshItemMatchSummary();
    }

    private void refreshItemEditors() {
        for (int i = 0; i < editors.size(); i++) {
            editors.get(i).refreshIndex(i + 1, editors.size() <= 1);
        }
    }

    private void refreshItemMatchSummary() {
        itemMatchSummaryLabel.setText(buildItemMatchSummary());
    }

    private void handleConfirm() {
        if (!canConfirm()) {
            statusLabel.setText("当前AI出库记录不能继续确认。");
            return;
        }

        try {
            AiOutboundConfirmRequest request = buildRequest();

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("确认生成出库单");
            confirmAlert.setHeaderText("确认后将生成正式出库单并扣减库存");
            confirmAlert.setContentText("是否继续确认？");
            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            setLoadingState(true, "正在确认生成出库单...");

            Task<Long> confirmTask = new Task<>() {
                @Override
                protected Long call() {
                    ApiResponse<Long> response = apiClient.post(AiApi.OUTBOUND_CONFIRM, request, Long.class);
                    if (response == null || !response.isSuccess()) {
                        String message = response == null ? "AI出库确认失败，请检查服务端是否可用" : response.getMessage();
                        throw new ApiException(isBlank(message) ? "AI出库确认失败" : message);
                    }
                    return response.getData();
                }
            };

            confirmTask.setOnSucceeded(event -> {
                Long orderId = confirmTask.getValue();
                setLoadingState(false, orderId == null ? "AI出库确认成功。" : "AI出库确认成功，已生成正式出库单，ID=" + orderId);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("确认成功");
                alert.setHeaderText("AI识别记录已生成正式出库单");
                alert.setContentText(orderId == null ? "AI出库确认成功。" : "正式出库单ID：" + orderId);
                alert.showAndWait();
                onConfirmed.accept(orderId);
            });

            confirmTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(confirmTask.getException(), "AI出库确认失败，请稍后重试。")));

            Thread thread = new Thread(confirmTask, "desktop-ai-outbound-confirm");
            thread.setDaemon(true);
            thread.start();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private void handleGoCustomerManagement() {
        if (onGoCustomerManagement == null) {
            return;
        }
        if (!confirmLeaveForManagement("客户管理")) {
            return;
        }
        onGoCustomerManagement.run();
    }

    private void handleGoProductManagement(ConfirmItemEditor editor) {
        if (onGoProductManagement == null || editor == null) {
            return;
        }
        if (!confirmLeaveForManagement("商品管理")) {
            return;
        }
        int editorIndex = editors.indexOf(editor);
        onGoProductManagement.accept(editorIndex < 0 ? 0 : editorIndex);
    }

    private AiOutboundConfirmRequest buildRequest() {
        if (editors.isEmpty()) {
            throw new IllegalArgumentException("确认明细不能为空。");
        }

        CustomerRow selectedCustomer = customerBox.getValue();
        String customerName = trimText(customerNameField.getText());
        if (selectedCustomer == null && customerName.isEmpty()) {
            throw new IllegalArgumentException("客户名称不能为空。");
        }

        List<AiOutboundConfirmItemRequest> itemList = new ArrayList<>();
        for (int i = 0; i < editors.size(); i++) {
            ConfirmItemEditor editor = editors.get(i);
            ProductRow product = editor.getSelectedProduct();
            if (product == null || product.getId() == null) {
                throw new IllegalArgumentException("第 " + (i + 1) + " 行请先选择匹配商品。");
            }

            Integer lineNo = parsePositiveInteger(editor.getLineNoText(), "第 " + (i + 1) + " 行行号必须大于 0。");
            Integer quantity = parsePositiveInteger(editor.getQuantityText(), "第 " + (i + 1) + " 行数量必须大于 0。");
            BigDecimal unitPrice = parseNonNegativeDecimal(editor.getUnitPriceText(), "第 " + (i + 1) + " 行单价不能小于 0。");
            BigDecimal amount = unitPrice.multiply(BigDecimal.valueOf(quantity));

            AiOutboundConfirmItemRequest item = new AiOutboundConfirmItemRequest();
            item.setLineNo(lineNo);
            item.setProductName(trimText(editor.getProductNameText()));
            item.setSpecification(trimToNull(editor.getSpecificationText()));
            item.setUnit(trimToNull(editor.getUnitText()));
            item.setMatchedProductId(product.getId());
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setAmount(amount);
            item.setRemark(trimToNull(editor.getRemarkText()));
            itemList.add(item);
        }

        AiOutboundConfirmRequest request = new AiOutboundConfirmRequest();
        request.setRecordId(draftData == null ? null : draftData.getRecordId());
        request.setCustomerId(selectedCustomer == null ? null : selectedCustomer.getId());
        request.setCustomerName(customerName);
        request.setRawText(trimToNull(rawTextArea.getText()));
        request.setRemark(trimToNull(remarkField.getText()));
        request.setItemList(itemList);
        return request;
    }

    private void searchCustomers() {
        String customerCode = trimText(customerCodeSearchField.getText());
        String customerName = trimText(customerNameSearchField.getText());
        if (customerCode.isEmpty() && customerName.isEmpty()) {
            statusLabel.setText("请至少输入客户编码或客户名称中的一个查询条件。");
            return;
        }

        customerCodeSearchField.setDisable(true);
        customerNameSearchField.setDisable(true);
        customerSearchButton.setDisable(true);
        customerBox.setDisable(true);
        customerInfoLabel.setText("正在搜索客户...");
        statusLabel.setText("正在搜索客户...");

        Task<List<CustomerRow>> searchTask = new Task<>() {
            @Override
            protected List<CustomerRow> call() {
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("customerCode", customerCode);
                params.put("customerName", customerName);
                params.put("pageNum", 1);
                params.put("pageSize", 100);

                ApiResponse<CustomerPageData> response = apiClient.get(CustomerApi.LIST, params, CustomerPageData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "客户搜索失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "客户搜索失败" : message);
                }
                return response.getData().getList();
            }
        };

        searchTask.setOnSucceeded(event -> {
            List<CustomerRow> customers = searchTask.getValue();
            CustomerRow currentSelected = customerBox.getValue();
            customerOptions.setAll(customers);
            if (customers.isEmpty()) {
                customerBox.setValue(null);
                if (pendingAutoCustomerSearchNotice) {
                    customerInfoLabel.setText("已自动回填新建客户信息，但暂未自动命中，请检查客户编码或名称后重试。");
                    statusLabel.setText("已回填新建客户信息，但未自动匹配到客户。");
                } else {
                    customerInfoLabel.setText("未找到匹配客户，请调整查询条件后重试。");
                    statusLabel.setText("未找到匹配客户。");
                }
            } else {
                CustomerRow preferred = findPreferredCustomer(customers, currentSelected, customerCode, customerName);
                CustomerRow selected = preferred == null ? customers.get(0) : preferred;
                customerBox.setValue(selected);
                refreshCustomerInfo();
                if (pendingAutoCustomerSearchNotice && selected != null) {
                    customerInfoLabel.setText(buildAutoSelectedCustomerNotice(selected));
                    statusLabel.setText("已自动搜索并选中新建客户，可继续确认出库单。");
                } else {
                    statusLabel.setText("客户搜索完成，共找到 " + customers.size() + " 条结果。");
                }
            }
            pendingAutoCustomerSearchNotice = false;
            customerCodeSearchField.setDisable(false);
            customerNameSearchField.setDisable(false);
            customerSearchButton.setDisable(false);
            customerBox.setDisable(false);
        });

        searchTask.setOnFailed(event -> {
            if (pendingAutoCustomerSearchNotice) {
                customerInfoLabel.setText("已自动回填新建客户信息，但自动搜索失败，请稍后重试。");
                statusLabel.setText(resolveErrorMessage(searchTask.getException(), "自动搜索新建客户失败，请稍后重试。"));
            } else {
                customerInfoLabel.setText("客户搜索失败，请稍后重试。");
                statusLabel.setText(resolveErrorMessage(searchTask.getException(), "客户搜索失败，请稍后重试。"));
            }
            pendingAutoCustomerSearchNotice = false;
            customerCodeSearchField.setDisable(false);
            customerNameSearchField.setDisable(false);
            customerSearchButton.setDisable(false);
            customerBox.setDisable(false);
        });

        Thread thread = new Thread(searchTask, "desktop-ai-search-customer");
        thread.setDaemon(true);
        thread.start();
    }

    private void applyInitialCustomerSearch() {
        if (isBlank(initialCustomerCode) && isBlank(initialCustomerName)) {
            return;
        }
        customerCodeSearchField.setText(defaultTextForInput(initialCustomerCode));
        customerNameSearchField.setText(defaultTextForInput(initialCustomerName));
        customerInfoLabel.setText("已自动回填新建客户信息，正在搜索匹配客户...");
        pendingAutoCustomerSearchNotice = true;
        searchCustomers();
    }

    private void applyInitialProductSearch() {
        if (isBlank(initialProductCode) && isBlank(initialProductName)) {
            return;
        }
        if (editors.isEmpty()) {
            return;
        }
        int targetIndex = initialProductEditorIndex == null ? 0 : initialProductEditorIndex;
        if (targetIndex < 0 || targetIndex >= editors.size()) {
            targetIndex = 0;
        }
        editors.get(targetIndex).applyInitialProductSearch(initialProductCode, initialProductName);
    }

    private CustomerRow findPreferredCustomer(List<CustomerRow> customers,
                                              CustomerRow currentSelected,
                                              String customerCode,
                                              String customerName) {
        if (currentSelected != null && currentSelected.getId() != null) {
            CustomerRow matchedById = customers.stream()
                    .filter(item -> item.getId() != null && item.getId().equals(currentSelected.getId()))
                    .findFirst()
                    .orElse(null);
            if (matchedById != null) {
                return matchedById;
            }
        }

        if (!isBlank(customerCode)) {
            CustomerRow matchedByCode = customers.stream()
                    .filter(item -> sameText(item.getCustomerCode(), customerCode))
                    .findFirst()
                    .orElse(null);
            if (matchedByCode != null) {
                return matchedByCode;
            }

            CustomerRow fuzzyMatchedByCode = findBestFuzzyCustomer(customers, customerCode, true);
            if (fuzzyMatchedByCode != null) {
                return fuzzyMatchedByCode;
            }
        }

        if (!isBlank(customerName)) {
            CustomerRow matchedByName = customers.stream()
                    .filter(item -> sameText(item.getCustomerName(), customerName))
                    .findFirst()
                    .orElse(null);
            if (matchedByName != null) {
                return matchedByName;
            }

            CustomerRow fuzzyMatchedByName = findBestFuzzyCustomer(customers, customerName, false);
            if (fuzzyMatchedByName != null) {
                return fuzzyMatchedByName;
            }
        }

        return null;
    }

    private CustomerRow findBestFuzzyCustomer(List<CustomerRow> customers, String keyword, boolean matchCode) {
        String normalizedKeyword = normalizeText(keyword);
        CustomerRow best = null;
        int bestScore = Integer.MAX_VALUE;
        for (CustomerRow customer : customers) {
            String candidate = normalizeText(matchCode ? customer.getCustomerCode() : customer.getCustomerName());
            int score = fuzzyScore(candidate, normalizedKeyword);
            if (score >= 0 && score < bestScore) {
                best = customer;
                bestScore = score;
            }
        }
        return best;
    }

    private String buildAutoSelectedCustomerNotice(CustomerRow customer) {
        return "已自动选中刚从客户管理页新增的客户："
                + defaultText(customer.getCustomerCode(), "-")
                + " / "
                + defaultText(customer.getCustomerName(), "-")
                + "。";
    }

    private void refreshCustomerInfo() {
        CustomerRow customer = customerBox.getValue();
        if (customer == null) {
            customerInfoLabel.setText("请先按客户编码或名称搜索，再从下拉框中选择。");
            return;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("客户名称：").append(defaultText(customer.getCustomerName(), "-"));
        builder.append("，编码：").append(defaultText(customer.getCustomerCode(), "-"));
        builder.append("，联系人：").append(defaultText(customer.getContactPerson(), "-"));
        builder.append("，电话：").append(defaultText(customer.getPhone(), "-"));
        customerInfoLabel.setText(builder.toString());
    }

    private void addSummaryRow(GridPane gridPane, int rowIndex, String label1, Label value1, String label2, Label value2) {
        Label leftLabel = new Label(label1 + "：");
        leftLabel.getStyleClass().add("page-label");
        Label rightLabel = new Label(label2 + "：");
        rightLabel.getStyleClass().add("page-label");

        gridPane.add(leftLabel, 0, rowIndex);
        gridPane.add(value1, 1, rowIndex);
        gridPane.add(rightLabel, 2, rowIndex);
        gridPane.add(value2, 3, rowIndex);
    }

    private Label createValueLabel() {
        Label label = new Label("-");
        label.getStyleClass().add("page-label");
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-label");
        label.setMinWidth(80);
        return label;
    }

    private Integer parsePositiveInteger(String value, String message) {
        try {
            int parsed = Integer.parseInt(trimText(value));
            if (parsed <= 0) {
                throw new IllegalArgumentException(message);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private BigDecimal parseNonNegativeDecimal(String value, String message) {
        try {
            BigDecimal parsed = new BigDecimal(trimText(value));
            if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(message);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(message);
        }
    }

    private void setLoadingState(boolean loading, String message) {
        customerNameField.setDisable(loading);
        customerCodeSearchField.setDisable(loading);
        customerNameSearchField.setDisable(loading);
        customerSearchButton.setDisable(loading);
        customerBox.setDisable(loading);
        remarkField.setDisable(loading);
        rawTextArea.setDisable(loading);
        confirmButton.setDisable(loading || !canConfirm());
        addItemButton.setDisable(loading || !canConfirm());
        for (ConfirmItemEditor editor : editors) {
            editor.setDisable(loading || !canConfirm());
        }
        statusLabel.setText(message);
    }

    private boolean canConfirm() {
        return "success".equalsIgnoreCase(currentRecognitionStatus) && currentConfirmedOrderId == null;
    }

    private String resolveErrorMessage(Throwable throwable, String fallback) {
        if (throwable == null || isBlank(throwable.getMessage())) {
            return fallback;
        }
        return throwable.getMessage().trim();
    }

    private boolean confirmLeaveForManagement(String targetName) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("离开当前确认页");
        confirmAlert.setHeaderText("前往" + targetName + "前请确认");
        confirmAlert.setContentText("当前未提交的 AI 出库确认修改不会保留，是否继续前往？");
        return confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private String buildUploadNotice(AiOutboundRecognizeData detailData) {
        if (detailData == null) {
            return "这是本次刚上传的 AI 出库识别结果，请确认客户和商品后生成正式出库单。";
        }
        int itemCount = detailData.getItemList() == null ? 0 : detailData.getItemList().size();
        String fileName = defaultText(detailData.getSourceFileName(), "未命名文件");
        String recognitionStatus = defaultText(detailData.getRecognitionStatusText(), "未知");
        String warningText = defaultText(detailData.getWarningText(), "-");
        if ("-".equals(warningText)) {
            return "这是本次刚上传的 AI 出库识别结果：文件「" + fileName + "」，识别状态「" + recognitionStatus + "」，识别明细 " + itemCount + " 条。";
        }
        return "这是本次刚上传的 AI 出库识别结果：文件「" + fileName + "」，识别状态「" + recognitionStatus + "」，识别明细 "
                + itemCount + " 条。当前提示：" + warningText;
    }

    private String buildItemMatchSummary() {
        if (editors.isEmpty()) {
            return "当前没有识别明细，请手动新增并补齐匹配商品。";
        }

        int exactCount = 0;
        int fuzzyCount = 0;
        int manualCount = 0;
        int unmatchedCount = 0;

        for (ConfirmItemEditor editor : editors) {
            String status = editor.resolveCurrentMatchStatus();
            if ("matched_exact".equalsIgnoreCase(status)) {
                exactCount++;
            } else if ("matched_fuzzy".equalsIgnoreCase(status)) {
                fuzzyCount++;
            } else if (status.startsWith("manual_")) {
                manualCount++;
            } else {
                unmatchedCount++;
            }
        }

        return "明细匹配状态：共 " + editors.size() + " 条，精确匹配 " + exactCount
                + " 条，模糊匹配 " + fuzzyCount
                + " 条，人工处理 " + manualCount
                + " 条，未匹配 " + unmatchedCount + " 条。";
    }

    private String formatCustomerOption(CustomerRow customer) {
        if (customer == null) {
            return "";
        }
        return defaultText(customer.getCustomerCode(), "-") + " / " + defaultText(customer.getCustomerName(), "-");
    }

    private String formatProductOption(ProductRow product) {
        if (product == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(defaultText(product.getProductCode(), "-"));
        builder.append(" / ");
        builder.append(defaultText(product.getProductName(), "-"));
        if (!isBlank(product.getSpecification())) {
            builder.append(" / ").append(product.getSpecification().trim());
        }
        if (!isBlank(product.getUnit())) {
            builder.append(" / ").append(product.getUnit().trim());
        }
        return builder.toString();
    }

    private String defaultText(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private String detailText(String value) {
        return defaultText(value, "-");
    }

    private String defaultTextForInput(String value) {
        return value == null ? "" : value;
    }

    private String trimText(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        String text = trimText(value);
        return text.isEmpty() ? null : text;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean sameText(String left, String right) {
        if (isBlank(left) || isBlank(right)) {
            return false;
        }
        return trimText(left).equalsIgnoreCase(trimText(right));
    }

    private String normalizeText(String value) {
        return trimText(value).toLowerCase();
    }

    private int fuzzyScore(String candidate, String keyword) {
        if (candidate.isEmpty() || keyword.isEmpty()) {
            return -1;
        }

        int candidateContainsIndex = candidate.indexOf(keyword);
        if (candidateContainsIndex >= 0) {
            return candidateContainsIndex * 1000 + Math.abs(candidate.length() - keyword.length());
        }

        int keywordContainsIndex = keyword.indexOf(candidate);
        if (keywordContainsIndex >= 0) {
            return 500000 + keywordContainsIndex * 1000 + Math.abs(candidate.length() - keyword.length());
        }

        return -1;
    }

    private class ConfirmItemEditor {

        private final VBox root;
        private final Label titleLabel;
        private final TextField lineNoField;
        private final TextField productNameField;
        private final TextField specificationField;
        private final TextField unitField;
        private final TextField productCodeSearchField;
        private final TextField productNameSearchField;
        private final Button productSearchButton;
        private final Button productManageButton;
        private final ComboBox<ProductRow> productBox;
        private final ObservableList<ProductRow> productOptions;
        private final Label productInfoLabel;
        private final TextField quantityField;
        private final TextField unitPriceField;
        private final Label amountLabel;
        private final TextField remarkField;
        private final Button removeButton;
        private final Long originalMatchedProductId;
        private final String originalMatchStatus;
        private boolean pendingAutoProductSearchNotice;

        ConfirmItemEditor(AiOutboundRecognizeItemRow item) {
            this.root = new VBox(8);
            this.root.setPadding(new Insets(12));
            this.root.setStyle("-fx-border-color: #e4e7ed; -fx-border-radius: 8; -fx-background-color: #fafafa; -fx-background-radius: 8;");
            this.originalMatchedProductId = item == null ? null : item.getMatchedProductId();
            this.originalMatchStatus = item == null ? null : item.getMatchStatus();

            this.titleLabel = new Label("明细");
            this.titleLabel.getStyleClass().add("page-label");

            this.lineNoField = new TextField(item == null || item.getLineNo() == null ? "" : String.valueOf(item.getLineNo()));
            this.lineNoField.setPromptText("行号");
            this.lineNoField.setPrefWidth(80);

            this.productNameField = new TextField(item == null ? "" : defaultTextForInput(item.getProductName()));
            this.productNameField.setPromptText("商品名称");
            this.productNameField.setPrefWidth(180);

            this.specificationField = new TextField(item == null ? "" : defaultTextForInput(item.getSpecification()));
            this.specificationField.setPromptText("规格");
            this.specificationField.setPrefWidth(120);

            this.unitField = new TextField(item == null ? "" : defaultTextForInput(item.getUnit()));
            this.unitField.setPromptText("单位");
            this.unitField.setPrefWidth(90);

            this.productCodeSearchField = new TextField();
            this.productCodeSearchField.setPromptText("商品编码");
            this.productCodeSearchField.setPrefWidth(120);

            this.productNameSearchField = new TextField();
            this.productNameSearchField.setPromptText("商品名称");
            this.productNameSearchField.setPrefWidth(160);

            this.productSearchButton = new Button("搜索商品");
            this.productSearchButton.setOnAction(event -> searchProducts());
            this.productManageButton = new Button("去商品管理");
            this.productManageButton.setOnAction(event -> handleGoProductManagement(this));

            this.productOptions = FXCollections.observableArrayList();
            this.productBox = new ComboBox<>(productOptions);
            this.productBox.setPrefWidth(360);
            this.productBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(ProductRow object) {
                    return formatProductOption(object);
                }

                @Override
                public ProductRow fromString(String string) {
                    return null;
                }
            });

            this.productInfoLabel = new Label("请先按编码或名称搜索商品，再从下拉框中选择。");
            this.productInfoLabel.getStyleClass().add("placeholder-note");
            this.productInfoLabel.setWrapText(true);

            this.quantityField = new TextField(item == null || item.getQuantity() == null ? "1" : String.valueOf(item.getQuantity()));
            this.quantityField.setPromptText("数量");
            this.quantityField.setPrefWidth(100);

            this.unitPriceField = new TextField(item == null || item.getUnitPrice() == null ? "0" : item.getUnitPrice().stripTrailingZeros().toPlainString());
            this.unitPriceField.setPromptText("单价");
            this.unitPriceField.setPrefWidth(120);

            this.amountLabel = new Label("金额：0");
            this.amountLabel.getStyleClass().add("page-label");

            this.remarkField = new TextField(item == null ? "" : defaultTextForInput(item.getRemark()));
            this.remarkField.setPromptText("备注");

            this.removeButton = new Button("删除");
            this.removeButton.setOnAction(event -> removeItemEditor(this));

            productBox.setOnAction(event -> refreshProductInfo());
            quantityField.textProperty().addListener((obs, oldValue, newValue) -> refreshAmount());
            unitPriceField.textProperty().addListener((obs, oldValue, newValue) -> refreshAmount());

            HBox firstRow = new HBox(12);
            firstRow.setAlignment(Pos.CENTER_LEFT);
            firstRow.getChildren().addAll(titleLabel, new Label("行号"), lineNoField, new Label("商品名称"), productNameField, new Label("规格"), specificationField, new Label("单位"), unitField);

            HBox secondRow = new HBox(12);
            secondRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(productBox, Priority.ALWAYS);
            secondRow.getChildren().addAll(new Label("编码"), productCodeSearchField, new Label("名称"), productNameSearchField, productSearchButton, productManageButton, new Label("匹配商品"), productBox);

            HBox thirdRow = new HBox(12);
            thirdRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(remarkField, Priority.ALWAYS);
            thirdRow.getChildren().addAll(new Label("数量"), quantityField, new Label("单价"), unitPriceField, amountLabel, removeButton, remarkField);

            root.getChildren().addAll(firstRow, secondRow, productInfoLabel, thirdRow);

            if (item != null && item.getMatchedProductId() != null) {
                ProductRow product = new ProductRow();
                product.setId(item.getMatchedProductId());
                product.setProductCode("ID:" + item.getMatchedProductId());
                product.setProductName(item.getProductName());
                product.setSpecification(item.getSpecification());
                product.setUnit(item.getUnit());
                productOptions.add(product);
                productBox.setValue(product);
            }
            refreshProductInfo();
            refreshAmount();
        }

        Parent getRoot() {
            return root;
        }

        void refreshIndex(int index, boolean disableRemove) {
            titleLabel.setText("明细 " + index);
            removeButton.setDisable(disableRemove);
        }

        void refreshProductInfo() {
            ProductRow product = getSelectedProduct();
            if (product == null) {
                productInfoLabel.setText("请先按编码或名称搜索商品，再从下拉框中选择。");
            } else {
                if (!isBlank(product.getProductName())) {
                    productNameField.setText(product.getProductName().trim());
                }
                if (!isBlank(product.getSpecification())) {
                    specificationField.setText(product.getSpecification().trim());
                }
                if (!isBlank(product.getUnit())) {
                    unitField.setText(product.getUnit().trim());
                }
                StringBuilder builder = new StringBuilder();
                builder.append("商品名称：").append(defaultText(product.getProductName(), "-"));
                builder.append("，规格：").append(defaultText(product.getSpecification(), "-"));
                builder.append("，单位：").append(defaultText(product.getUnit(), "-"));
                productInfoLabel.setText(builder.toString());
            }
            if (editors.contains(this)) {
                refreshItemMatchSummary();
            }
        }

        void searchProducts() {
            String productCode = trimText(productCodeSearchField.getText());
            String productName = trimText(productNameSearchField.getText());
            if (productCode.isEmpty() && productName.isEmpty()) {
                statusLabel.setText("请至少输入商品编码或商品名称中的一个查询条件。");
                return;
            }

            setSearchState(true, "正在搜索商品...");

            Task<List<ProductRow>> searchTask = new Task<>() {
                @Override
                protected List<ProductRow> call() {
                    Map<String, Object> params = new LinkedHashMap<>();
                    params.put("productCode", productCode);
                    params.put("productName", productName);
                    params.put("pageNum", 1);
                    params.put("pageSize", 100);

                    ApiResponse<ProductPageData> response = apiClient.get(ProductApi.LIST, params, ProductPageData.class);
                    if (response == null || !response.isSuccess() || response.getData() == null) {
                        String message = response == null ? "商品搜索失败，请检查服务端是否可用" : response.getMessage();
                        throw new ApiException(isBlank(message) ? "商品搜索失败" : message);
                    }
                    return response.getData().getList();
                }
            };

            searchTask.setOnSucceeded(event -> {
                List<ProductRow> products = searchTask.getValue();
                ProductRow currentSelected = getSelectedProduct();
                productOptions.setAll(products);
                if (products.isEmpty()) {
                    productBox.setValue(null);
                    if (pendingAutoProductSearchNotice) {
                        productInfoLabel.setText("已自动回填新建商品信息，但暂未自动命中，请检查商品编码或名称后重试。");
                        statusLabel.setText("第 " + getDisplayIndex() + " 行已回填新建商品信息，但未自动匹配到商品。");
                    } else {
                        productInfoLabel.setText("未找到匹配商品，请调整编码或名称后重试。");
                        statusLabel.setText("未找到匹配商品。");
                    }
                } else {
                    ProductRow preferred = findPreferredProduct(products, currentSelected, productCode, productName);
                    ProductRow selected = preferred == null ? products.get(0) : preferred;
                    productBox.setValue(selected);
                    refreshProductInfo();
                    if (pendingAutoProductSearchNotice && selected != null) {
                        productInfoLabel.setText(buildAutoSelectedProductNotice(selected));
                        statusLabel.setText("第 " + getDisplayIndex() + " 行已自动搜索并选中新建商品。");
                    } else {
                        statusLabel.setText("商品搜索完成，共找到 " + products.size() + " 条结果。");
                    }
                }
                pendingAutoProductSearchNotice = false;
                setSearchState(false, null);
            });

            searchTask.setOnFailed(event -> {
                productBox.setValue(null);
                if (pendingAutoProductSearchNotice) {
                    productInfoLabel.setText("已自动回填新建商品信息，但自动搜索失败，请稍后重试。");
                    statusLabel.setText(resolveErrorMessage(searchTask.getException(), "自动搜索新建商品失败，请稍后重试。"));
                } else {
                    productInfoLabel.setText("商品搜索失败，请稍后重试。");
                    statusLabel.setText(resolveErrorMessage(searchTask.getException(), "商品搜索失败，请稍后重试。"));
                }
                pendingAutoProductSearchNotice = false;
                setSearchState(false, null);
            });

            Thread thread = new Thread(searchTask, "desktop-ai-search-outbound-product");
            thread.setDaemon(true);
            thread.start();
        }

        void applyInitialProductSearch(String productCode, String productName) {
            productCodeSearchField.setText(defaultTextForInput(productCode));
            productNameSearchField.setText(defaultTextForInput(productName));
            productInfoLabel.setText("已自动回填新建商品信息，正在搜索匹配商品...");
            pendingAutoProductSearchNotice = true;
            searchProducts();
        }

        private String buildAutoSelectedProductNotice(ProductRow product) {
            return "已自动选中刚从商品管理页新增的商品："
                    + defaultText(product.getProductCode(), "-")
                    + " / "
                    + defaultText(product.getProductName(), "-")
                    + "。";
        }

        private int getDisplayIndex() {
            int index = editors.indexOf(this);
            return index < 0 ? 1 : index + 1;
        }

        void refreshAmount() {
            try {
                Integer quantity = parsePositiveInteger(quantityField.getText(), "数量必须大于 0。");
                BigDecimal unitPrice = parseNonNegativeDecimal(unitPriceField.getText(), "单价不能小于 0。");
                amountLabel.setText("金额：" + unitPrice.multiply(BigDecimal.valueOf(quantity)).stripTrailingZeros().toPlainString());
            } catch (IllegalArgumentException ex) {
                amountLabel.setText("金额：-");
            }
        }

        void setDisable(boolean disable) {
            lineNoField.setDisable(disable);
            productNameField.setDisable(disable);
            specificationField.setDisable(disable);
            unitField.setDisable(disable);
            productCodeSearchField.setDisable(disable);
            productNameSearchField.setDisable(disable);
            productSearchButton.setDisable(disable);
            productManageButton.setDisable(disable);
            productBox.setDisable(disable);
            quantityField.setDisable(disable);
            unitPriceField.setDisable(disable);
            remarkField.setDisable(disable);
            removeButton.setDisable(disable || editors.size() <= 1);
        }

        void setSearchState(boolean searching, String hint) {
            productCodeSearchField.setDisable(searching);
            productNameSearchField.setDisable(searching);
            productSearchButton.setDisable(searching);
            productManageButton.setDisable(searching);
            productBox.setDisable(searching);
            if (searching && hint != null) {
                productInfoLabel.setText(hint);
            } else {
                refreshProductInfo();
            }
        }

        ProductRow getSelectedProduct() {
            return productBox.getValue();
        }

        private ProductRow findPreferredProduct(List<ProductRow> products,
                                                ProductRow currentSelected,
                                                String productCode,
                                                String productName) {
            if (currentSelected != null && currentSelected.getId() != null) {
                ProductRow matchedById = products.stream()
                        .filter(item -> item.getId() != null && item.getId().equals(currentSelected.getId()))
                        .findFirst()
                        .orElse(null);
                if (matchedById != null) {
                    return matchedById;
                }
            }

            if (!isBlank(productCode)) {
                ProductRow matchedByCode = products.stream()
                        .filter(item -> sameText(item.getProductCode(), productCode))
                        .findFirst()
                        .orElse(null);
                if (matchedByCode != null) {
                    return matchedByCode;
                }

                ProductRow fuzzyMatchedByCode = findBestFuzzyProduct(products, productCode, true);
                if (fuzzyMatchedByCode != null) {
                    return fuzzyMatchedByCode;
                }
            }

            if (!isBlank(productName)) {
                ProductRow matchedByName = products.stream()
                        .filter(item -> sameText(item.getProductName(), productName))
                        .findFirst()
                        .orElse(null);
                if (matchedByName != null) {
                    return matchedByName;
                }

                ProductRow fuzzyMatchedByName = findBestFuzzyProduct(products, productName, false);
                if (fuzzyMatchedByName != null) {
                    return fuzzyMatchedByName;
                }
            }

            return null;
        }

        private ProductRow findBestFuzzyProduct(List<ProductRow> products, String keyword, boolean matchCode) {
            String normalizedKeyword = normalizeText(keyword);
            ProductRow best = null;
            int bestScore = Integer.MAX_VALUE;
            for (ProductRow product : products) {
                String candidate = normalizeText(matchCode ? product.getProductCode() : product.getProductName());
                int score = fuzzyScore(candidate, normalizedKeyword);
                if (score >= 0 && score < bestScore) {
                    best = product;
                    bestScore = score;
                }
            }
            return best;
        }

        String resolveCurrentMatchStatus() {
            ProductRow product = getSelectedProduct();
            if (product == null || product.getId() == null) {
                return "unmatched";
            }
            if (originalMatchedProductId != null
                    && originalMatchedProductId.equals(product.getId())
                    && !isBlank(originalMatchStatus)) {
                return originalMatchStatus.trim();
            }
            return "manual_selected";
        }

        String getLineNoText() {
            return lineNoField.getText();
        }

        String getProductNameText() {
            return productNameField.getText();
        }

        String getSpecificationText() {
            return specificationField.getText();
        }

        String getUnitText() {
            return unitField.getText();
        }

        String getQuantityText() {
            return quantityField.getText();
        }

        String getUnitPriceText() {
            return unitPriceField.getText();
        }

        String getRemarkText() {
            return remarkField.getText();
        }
    }

    public static class AiOutboundConfirmRequest {
        private Long recordId;
        private Long customerId;
        private String customerName;
        private String rawText;
        private String remark;
        private List<AiOutboundConfirmItemRequest> itemList;

        public Long getRecordId() {
            return recordId;
        }

        public void setRecordId(Long recordId) {
            this.recordId = recordId;
        }

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public String getRawText() {
            return rawText;
        }

        public void setRawText(String rawText) {
            this.rawText = rawText;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public List<AiOutboundConfirmItemRequest> getItemList() {
            return itemList;
        }

        public void setItemList(List<AiOutboundConfirmItemRequest> itemList) {
            this.itemList = itemList;
        }
    }

    public static class AiOutboundConfirmItemRequest {
        private Integer lineNo;
        private String productName;
        private String specification;
        private String unit;
        private Long matchedProductId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal amount;
        private String remark;

        public Integer getLineNo() {
            return lineNo;
        }

        public void setLineNo(Integer lineNo) {
            this.lineNo = lineNo;
        }

        public String getProductName() {
            return productName;
        }

        public void setProductName(String productName) {
            this.productName = productName;
        }

        public String getSpecification() {
            return specification;
        }

        public void setSpecification(String specification) {
            this.specification = specification;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }

        public Long getMatchedProductId() {
            return matchedProductId;
        }

        public void setMatchedProductId(Long matchedProductId) {
            this.matchedProductId = matchedProductId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
