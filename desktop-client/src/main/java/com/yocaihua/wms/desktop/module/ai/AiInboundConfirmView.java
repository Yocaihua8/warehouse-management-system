package com.yocaihua.wms.desktop.module.ai;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.AiApi;
import com.yocaihua.wms.desktop.api.endpoint.ProductApi;
import com.yocaihua.wms.desktop.api.endpoint.SupplierApi;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import com.yocaihua.wms.desktop.module.product.ProductPageData;
import com.yocaihua.wms.desktop.module.product.ProductRow;
import com.yocaihua.wms.desktop.module.supplier.SupplierPageData;
import com.yocaihua.wms.desktop.module.supplier.SupplierRow;
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

public class AiInboundConfirmView {

    private final ApiClient apiClient;
    private final Long recordId;
    private final Runnable onBack;
    private final Consumer<Long> onConfirmed;
    private final VBox root;
    private final TextField supplierNameField;
    private final TextField supplierCodeSearchField;
    private final TextField supplierNameSearchField;
    private final Button supplierSearchButton;
    private final ComboBox<SupplierRow> supplierBox;
    private final ObservableList<SupplierRow> supplierOptions;
    private final Label supplierInfoLabel;
    private final TextField remarkField;
    private final TextArea rawTextArea;
    private final VBox itemContainer;
    private final Label statusLabel;
    private final Button confirmButton;
    private final Button addItemButton;
    private final List<ConfirmItemEditor> editors;
    private String currentRecognitionStatus;
    private Long currentConfirmedOrderId;

    public AiInboundConfirmView(
            StartupContext startupContext,
            ApiClient apiClient,
            Long recordId,
            Runnable onBack,
            Consumer<Long> onConfirmed
    ) {
        this.apiClient = apiClient;
        this.recordId = recordId;
        this.onBack = onBack;
        this.onConfirmed = onConfirmed;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));
        this.supplierOptions = FXCollections.observableArrayList();
        this.editors = new ArrayList<>();

        VBox card = new VBox(16);
        card.getStyleClass().add("page-card");

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("AI入库确认");
        titleLabel.getStyleClass().add("page-title");

        this.confirmButton = new Button("确认生成入库单");
        this.addItemButton = new Button("新增明细");
        Button backButton = new Button("返回详情");
        backButton.setOnAction(event -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(titleLabel, spacer, addItemButton, confirmButton, backButton);

        VBox baseForm = new VBox(12);
        baseForm.setPadding(new Insets(4, 0, 4, 0));

        HBox supplierNameRow = new HBox(12);
        supplierNameRow.setAlignment(Pos.CENTER_LEFT);
        Label supplierNameLabel = createFieldLabel("供应商名称");
        this.supplierNameField = new TextField();
        this.supplierNameField.setPromptText("请输入供应商名称");
        HBox.setHgrow(supplierNameField, Priority.ALWAYS);
        supplierNameRow.getChildren().addAll(supplierNameLabel, supplierNameField);

        HBox supplierRow = new HBox(12);
        supplierRow.setAlignment(Pos.CENTER_LEFT);
        Label supplierLabel = createFieldLabel("匹配供应商");
        this.supplierCodeSearchField = new TextField();
        this.supplierCodeSearchField.setPromptText("供应商编码");
        this.supplierCodeSearchField.setPrefWidth(140);
        this.supplierNameSearchField = new TextField();
        this.supplierNameSearchField.setPromptText("供应商名称");
        this.supplierNameSearchField.setPrefWidth(180);
        this.supplierSearchButton = new Button("搜索供应商");
        this.supplierBox = new ComboBox<>(supplierOptions);
        this.supplierBox.setPrefWidth(320);
        this.supplierBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SupplierRow object) {
                return formatSupplierOption(object);
            }

            @Override
            public SupplierRow fromString(String string) {
                return null;
            }
        });
        this.supplierBox.setOnAction(event -> refreshSupplierInfo());
        HBox.setHgrow(supplierBox, Priority.ALWAYS);
        supplierRow.getChildren().addAll(
                supplierLabel,
                supplierCodeSearchField,
                supplierNameSearchField,
                supplierSearchButton,
                supplierBox
        );

        this.supplierInfoLabel = new Label("可按供应商编码或名称搜索并回填匹配结果。");
        this.supplierInfoLabel.getStyleClass().add("placeholder-note");
        this.supplierInfoLabel.setWrapText(true);

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

        baseForm.getChildren().addAll(supplierNameRow, supplierRow, supplierInfoLabel, remarkRow, rawTextLabel, rawTextArea);

        Label itemTitle = new Label("确认明细");
        itemTitle.getStyleClass().add("page-title");
        itemTitle.setStyle("-fx-font-size: 18px;");

        this.itemContainer = new VBox(12);

        ScrollPane scrollPane = new ScrollPane(itemContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(460);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.statusLabel = new Label("正在加载AI确认草稿...");
        this.statusLabel.getStyleClass().add("page-subtitle");

        card.getChildren().addAll(headerRow, baseForm, itemTitle, scrollPane, statusLabel);
        root.getChildren().add(card);

        bindActions();
        loadDraft();
    }

    public Parent getRoot() {
        return root;
    }

    private void bindActions() {
        addItemButton.setOnAction(event -> addItemEditor(null));
        confirmButton.setOnAction(event -> handleConfirm());
        supplierSearchButton.setOnAction(event -> searchSuppliers());
    }

    private void loadDraft() {
        setLoadingState(true, "正在加载AI确认草稿...");

        Task<AiInboundRecordDetailData> loadTask = new Task<>() {
            @Override
            protected AiInboundRecordDetailData call() {
                ApiResponse<AiInboundRecordDetailData> response = apiClient.get(AiApi.INBOUND_DETAIL_PREFIX + recordId, AiInboundRecordDetailData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "AI确认草稿加载失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "AI确认草稿加载失败" : message);
                }
                return response.getData();
            }
        };

        loadTask.setOnSucceeded(event -> {
            renderDraft(loadTask.getValue());
            setLoadingState(false, canConfirm() ? "请确认供应商和商品匹配后生成正式入库单。" : "当前AI记录不能继续确认。");
        });

        loadTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(loadTask.getException(), "AI确认草稿加载失败，请稍后重试。")));

        Thread thread = new Thread(loadTask, "desktop-ai-inbound-confirm-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderDraft(AiInboundRecordDetailData detail) {
        currentRecognitionStatus = detail.getRecognitionStatus();
        currentConfirmedOrderId = detail.getConfirmedOrderId();

        supplierNameField.setText(defaultTextForInput(detail.getSupplierName()));
        remarkField.setText("AI识别确认生成入库单");
        rawTextArea.setText(detail.getRawText() == null ? "" : detail.getRawText());

        supplierOptions.clear();
        supplierBox.setValue(null);
        if (detail.getMatchedSupplierId() != null) {
            SupplierRow supplier = new SupplierRow();
            supplier.setId(detail.getMatchedSupplierId());
            supplier.setSupplierCode("ID:" + detail.getMatchedSupplierId());
            supplier.setSupplierName(detail.getSupplierName());
            supplierOptions.add(supplier);
            supplierBox.setValue(supplier);
        }
        refreshSupplierInfo();

        editors.clear();
        itemContainer.getChildren().clear();
        List<AiInboundRecordItemRow> items = detail.getItemList();
        if (items == null || items.isEmpty()) {
            addItemEditor(null);
        } else {
            for (AiInboundRecordItemRow item : items) {
                addItemEditor(item);
            }
        }
        refreshItemEditors();
    }

    private void addItemEditor(AiInboundRecordItemRow item) {
        ConfirmItemEditor editor = new ConfirmItemEditor(item);
        editors.add(editor);
        itemContainer.getChildren().add(editor.getRoot());
        refreshItemEditors();
    }

    private void removeItemEditor(ConfirmItemEditor editor) {
        if (editors.size() <= 1) {
            statusLabel.setText("至少保留一条确认明细。");
            return;
        }
        editors.remove(editor);
        itemContainer.getChildren().remove(editor.getRoot());
        refreshItemEditors();
    }

    private void refreshItemEditors() {
        for (int i = 0; i < editors.size(); i++) {
            editors.get(i).refreshIndex(i + 1, editors.size() <= 1);
        }
    }

    private void handleConfirm() {
        if (!canConfirm()) {
            statusLabel.setText("当前AI记录不能继续确认。");
            return;
        }

        try {
            AiInboundConfirmRequest request = buildRequest();

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("确认生成入库单");
            confirmAlert.setHeaderText("确认后将生成正式入库单并增加库存");
            confirmAlert.setContentText("是否继续确认？");
            if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                return;
            }

            setLoadingState(true, "正在确认生成入库单...");

            Task<Long> confirmTask = new Task<>() {
                @Override
                protected Long call() {
                    ApiResponse<Long> response = apiClient.post(AiApi.INBOUND_CONFIRM, request, Long.class);
                    if (response == null || !response.isSuccess()) {
                        String message = response == null ? "AI确认失败，请检查服务端是否可用" : response.getMessage();
                        throw new ApiException(isBlank(message) ? "AI确认失败" : message);
                    }
                    return response.getData();
                }
            };

            confirmTask.setOnSucceeded(event -> {
                Long orderId = confirmTask.getValue();
                setLoadingState(false, orderId == null ? "AI确认成功。" : "AI确认成功，已生成正式入库单，ID=" + orderId);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("确认成功");
                alert.setHeaderText("AI识别记录已生成正式入库单");
                alert.setContentText(orderId == null ? "AI确认成功。" : "正式入库单ID：" + orderId);
                alert.showAndWait();
                onConfirmed.accept(orderId);
            });

            confirmTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(confirmTask.getException(), "AI确认失败，请稍后重试。")));

            Thread thread = new Thread(confirmTask, "desktop-ai-inbound-confirm");
            thread.setDaemon(true);
            thread.start();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private AiInboundConfirmRequest buildRequest() {
        if (editors.isEmpty()) {
            throw new IllegalArgumentException("确认明细不能为空。");
        }

        SupplierRow selectedSupplier = supplierBox.getValue();
        String supplierName = trimText(supplierNameField.getText());
        if (selectedSupplier == null && supplierName.isEmpty()) {
            throw new IllegalArgumentException("供应商名称不能为空。");
        }

        List<AiInboundConfirmItemRequest> itemList = new ArrayList<>();
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

            AiInboundConfirmItemRequest item = new AiInboundConfirmItemRequest();
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

        AiInboundConfirmRequest request = new AiInboundConfirmRequest();
        request.setRecordId(recordId);
        request.setSupplierId(selectedSupplier == null ? null : selectedSupplier.getId());
        request.setSupplierName(supplierName);
        request.setRawText(trimToNull(rawTextArea.getText()));
        request.setRemark(trimToNull(remarkField.getText()));
        request.setItemList(itemList);
        return request;
    }

    private void searchSuppliers() {
        String supplierCode = trimText(supplierCodeSearchField.getText());
        String supplierName = trimText(supplierNameSearchField.getText());
        if (supplierCode.isEmpty() && supplierName.isEmpty()) {
            statusLabel.setText("请至少输入供应商编码或供应商名称中的一个查询条件。");
            return;
        }

        supplierCodeSearchField.setDisable(true);
        supplierNameSearchField.setDisable(true);
        supplierSearchButton.setDisable(true);
        supplierBox.setDisable(true);
        supplierInfoLabel.setText("正在搜索供应商...");
        statusLabel.setText("正在搜索供应商...");

        Task<List<SupplierRow>> searchTask = new Task<>() {
            @Override
            protected List<SupplierRow> call() {
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("supplierCode", supplierCode);
                params.put("supplierName", supplierName);
                params.put("pageNum", 1);
                params.put("pageSize", 100);

                ApiResponse<SupplierPageData> response = apiClient.get(SupplierApi.LIST, params, SupplierPageData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "供应商搜索失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "供应商搜索失败" : message);
                }
                return response.getData().getList();
            }
        };

        searchTask.setOnSucceeded(event -> {
            List<SupplierRow> suppliers = searchTask.getValue();
            SupplierRow currentSelected = supplierBox.getValue();
            supplierOptions.setAll(suppliers);
            if (suppliers.isEmpty()) {
                supplierBox.setValue(null);
                supplierInfoLabel.setText("未找到匹配供应商，请调整查询条件后重试。");
                statusLabel.setText("未找到匹配供应商。");
            } else {
                if (currentSelected != null) {
                    SupplierRow matched = suppliers.stream()
                            .filter(item -> item.getId() != null && item.getId().equals(currentSelected.getId()))
                            .findFirst()
                            .orElse(null);
                    supplierBox.setValue(matched);
                } else {
                    supplierBox.setValue(suppliers.get(0));
                }
                refreshSupplierInfo();
                statusLabel.setText("供应商搜索完成，共找到 " + suppliers.size() + " 条结果。");
            }
            supplierCodeSearchField.setDisable(false);
            supplierNameSearchField.setDisable(false);
            supplierSearchButton.setDisable(false);
            supplierBox.setDisable(false);
        });

        searchTask.setOnFailed(event -> {
            supplierInfoLabel.setText("供应商搜索失败，请稍后重试。");
            statusLabel.setText(resolveErrorMessage(searchTask.getException(), "供应商搜索失败，请稍后重试。"));
            supplierCodeSearchField.setDisable(false);
            supplierNameSearchField.setDisable(false);
            supplierSearchButton.setDisable(false);
            supplierBox.setDisable(false);
        });

        Thread thread = new Thread(searchTask, "desktop-ai-search-supplier");
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshSupplierInfo() {
        SupplierRow supplier = supplierBox.getValue();
        if (supplier == null) {
            supplierInfoLabel.setText("可按供应商编码或名称搜索并回填匹配结果。");
            return;
        }
        if (!isBlank(supplier.getSupplierName())) {
            supplierNameField.setText(supplier.getSupplierName().trim());
        }
        StringBuilder builder = new StringBuilder();
        builder.append("供应商名称：").append(defaultText(supplier.getSupplierName(), "-"));
        builder.append("，编码：").append(defaultText(supplier.getSupplierCode(), "-"));
        builder.append("，联系人：").append(defaultText(supplier.getContactPerson(), "-"));
        builder.append("，电话：").append(defaultText(supplier.getPhone(), "-"));
        supplierInfoLabel.setText(builder.toString());
    }

    private Integer parsePositiveInteger(String value, String errorMessage) {
        String text = trimText(value);
        if (text.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        try {
            int parsed = Integer.parseInt(text);
            if (parsed <= 0) {
                throw new IllegalArgumentException(errorMessage);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private BigDecimal parseNonNegativeDecimal(String value, String errorMessage) {
        String text = trimText(value);
        if (text.isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
        try {
            BigDecimal parsed = new BigDecimal(text);
            if (parsed.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(errorMessage);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    private void setLoadingState(boolean loading, String message) {
        supplierNameField.setDisable(loading);
        supplierCodeSearchField.setDisable(loading);
        supplierNameSearchField.setDisable(loading);
        supplierSearchButton.setDisable(loading);
        supplierBox.setDisable(loading);
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

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("page-label");
        label.setMinWidth(80);
        return label;
    }

    private String defaultText(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value.trim();
    }

    private String defaultTextForInput(String value) {
        return value == null ? "" : value;
    }

    private String resolveErrorMessage(Throwable throwable, String fallback) {
        if (throwable == null || isBlank(throwable.getMessage())) {
            return fallback;
        }
        return throwable.getMessage().trim();
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

    private String formatSupplierOption(SupplierRow supplier) {
        if (supplier == null) {
            return "";
        }
        return defaultText(supplier.getSupplierCode(), "-") + " / " + defaultText(supplier.getSupplierName(), "-");
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
        private final ComboBox<ProductRow> productBox;
        private final ObservableList<ProductRow> productOptions;
        private final Label productInfoLabel;
        private final TextField quantityField;
        private final TextField unitPriceField;
        private final Label amountLabel;
        private final TextField remarkField;
        private final Button removeButton;

        ConfirmItemEditor(AiInboundRecordItemRow item) {
            this.root = new VBox(8);
            this.root.setPadding(new Insets(12));
            this.root.setStyle("-fx-border-color: #e4e7ed; -fx-border-radius: 8; -fx-background-color: #fafafa; -fx-background-radius: 8;");

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
            secondRow.getChildren().addAll(new Label("编码"), productCodeSearchField, new Label("名称"), productNameSearchField, productSearchButton, new Label("匹配商品"), productBox);

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
                return;
            }
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
                    productInfoLabel.setText("未找到匹配商品，请调整编码或名称后重试。");
                    statusLabel.setText("未找到匹配商品。");
                } else {
                    if (currentSelected != null) {
                        ProductRow matched = products.stream()
                                .filter(item -> item.getId() != null && item.getId().equals(currentSelected.getId()))
                                .findFirst()
                                .orElse(null);
                        productBox.setValue(matched);
                    } else {
                        productBox.setValue(products.get(0));
                    }
                    refreshProductInfo();
                    statusLabel.setText("商品搜索完成，共找到 " + products.size() + " 条结果。");
                }
                setSearchState(false, null);
            });

            searchTask.setOnFailed(event -> {
                productBox.setValue(null);
                productInfoLabel.setText("商品搜索失败，请稍后重试。");
                statusLabel.setText(resolveErrorMessage(searchTask.getException(), "商品搜索失败，请稍后重试。"));
                setSearchState(false, null);
            });

            Thread thread = new Thread(searchTask, "desktop-ai-search-product");
            thread.setDaemon(true);
            thread.start();
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

    public static class AiInboundConfirmRequest {
        private Long recordId;
        private Long supplierId;
        private String supplierName;
        private String rawText;
        private String remark;
        private List<AiInboundConfirmItemRequest> itemList;

        public Long getRecordId() {
            return recordId;
        }

        public void setRecordId(Long recordId) {
            this.recordId = recordId;
        }

        public Long getSupplierId() {
            return supplierId;
        }

        public void setSupplierId(Long supplierId) {
            this.supplierId = supplierId;
        }

        public String getSupplierName() {
            return supplierName;
        }

        public void setSupplierName(String supplierName) {
            this.supplierName = supplierName;
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

        public List<AiInboundConfirmItemRequest> getItemList() {
            return itemList;
        }

        public void setItemList(List<AiInboundConfirmItemRequest> itemList) {
            this.itemList = itemList;
        }
    }

    public static class AiInboundConfirmItemRequest {
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
