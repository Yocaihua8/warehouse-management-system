package com.yocaihua.wms.desktop.module.outbound;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.OrderCreatedData;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.CustomerApi;
import com.yocaihua.wms.desktop.api.endpoint.OutboundOrderApi;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class OutboundOrderCreateView {

    private final ApiClient apiClient;
    private final Runnable onBack;
    private final Consumer<OrderCreatedData> onCreated;
    private final Runnable onGoCustomerManagement;
    private final String initialCustomerCode;
    private final String initialCustomerName;
    private final VBox root;
    private final TextField customerCodeSearchField;
    private final TextField customerNameSearchField;
    private final Button customerSearchButton;
    private final ComboBox<CustomerRow> customerBox;
    private final ObservableList<CustomerRow> customerOptions;
    private final Label customerInfoLabel;
    private final TextField remarkField;
    private final VBox itemContainer;
    private final Label statusLabel;
    private final Button saveButton;
    private final Button addItemButton;
    private final List<DraftItemEditor> editors;

    public OutboundOrderCreateView(StartupContext startupContext, ApiClient apiClient, Runnable onBack, Consumer<OrderCreatedData> onCreated, Runnable onGoCustomerManagement, String initialCustomerCode, String initialCustomerName) {
        this.apiClient = apiClient;
        this.onBack = onBack;
        this.onCreated = onCreated;
        this.onGoCustomerManagement = onGoCustomerManagement;
        this.initialCustomerCode = initialCustomerCode;
        this.initialCustomerName = initialCustomerName;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));
        this.customerOptions = FXCollections.observableArrayList();
        this.editors = new ArrayList<>();

        VBox card = new VBox(16);
        card.getStyleClass().add("page-card");

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("新增出库草稿");
        titleLabel.getStyleClass().add("page-title");

        this.saveButton = new Button("保存草稿");
        this.addItemButton = new Button("新增明细");
        Button backButton = new Button("返回列表");
        backButton.setOnAction(event -> onBack.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().addAll(titleLabel, spacer, addItemButton, saveButton, backButton);

        VBox baseForm = new VBox(12);
        baseForm.setPadding(new Insets(4, 0, 4, 0));

        HBox customerRow = new HBox(12);
        customerRow.setAlignment(Pos.CENTER_LEFT);
        Label customerLabel = createFieldLabel("客户");
        this.customerCodeSearchField = new TextField();
        this.customerCodeSearchField.setPromptText("客户编码");
        this.customerCodeSearchField.setPrefWidth(140);
        this.customerNameSearchField = new TextField();
        this.customerNameSearchField.setPromptText("客户名称");
        this.customerNameSearchField.setPrefWidth(180);
        this.customerSearchButton = new Button("搜索客户");
        Button manageCustomerButton = new Button("去客户管理");
        manageCustomerButton.setOnAction(event -> handleGoCustomerManagement());
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
                manageCustomerButton,
                customerBox
        );

        this.customerInfoLabel = new Label("请先按客户编码或名称搜索，再从下拉框中选择。");
        this.customerInfoLabel.getStyleClass().add("placeholder-note");
        this.customerInfoLabel.setWrapText(true);

        HBox remarkRow = new HBox(12);
        remarkRow.setAlignment(Pos.CENTER_LEFT);
        Label remarkLabel = createFieldLabel("备注");
        this.remarkField = new TextField();
        this.remarkField.setPromptText("请输入备注");
        HBox.setHgrow(remarkField, Priority.ALWAYS);
        remarkRow.getChildren().addAll(remarkLabel, remarkField);

        baseForm.getChildren().addAll(customerRow, customerInfoLabel, remarkRow);

        Label itemTitle = new Label("出库明细");
        itemTitle.getStyleClass().add("page-title");
        itemTitle.setStyle("-fx-font-size: 18px;");

        this.itemContainer = new VBox(12);

        ScrollPane scrollPane = new ScrollPane(itemContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(420);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        this.statusLabel = new Label("请先搜索客户，再按商品编码或商品名称搜索商品。");
        this.statusLabel.getStyleClass().add("page-subtitle");

        card.getChildren().addAll(headerRow, baseForm, itemTitle, scrollPane, statusLabel);
        root.getChildren().add(card);

        addItemEditor();
        bindActions();
        applyInitialCustomerSearch();
    }

    public Parent getRoot() {
        return root;
    }

    private void bindActions() {
        addItemButton.setOnAction(event -> addItemEditor());
        saveButton.setOnAction(event -> handleSaveDraft());
        customerSearchButton.setOnAction(event -> searchCustomers());
    }

    private void addItemEditor() {
        DraftItemEditor editor = new DraftItemEditor();
        editors.add(editor);
        itemContainer.getChildren().add(editor.getRoot());
        refreshItemEditors();
    }

    private void removeItemEditor(DraftItemEditor editor) {
        if (editors.size() <= 1) {
            statusLabel.setText("至少保留一条出库明细。");
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

    private void handleSaveDraft() {
        try {
            OutboundOrderDraftRequest request = buildRequest();
            setLoadingState(true, "正在保存出库草稿...");

            Task<OrderCreatedData> saveTask = new Task<>() {
                @Override
                protected OrderCreatedData call() {
                    ApiResponse<OrderCreatedData> response = apiClient.post(OutboundOrderApi.ADD, request, OrderCreatedData.class);
                    if (response == null || !response.isSuccess()) {
                        String message = response == null ? "保存出库草稿失败，请检查服务端是否可用" : response.getMessage();
                        throw new ApiException(isBlank(message) ? "保存出库草稿失败" : message);
                    }
                    return response.getData();
                }
            };

            saveTask.setOnSucceeded(event -> {
                OrderCreatedData createdData = saveTask.getValue();
                String successMessage = buildSaveSuccessMessage(createdData, "保存出库草稿成功。");
                setLoadingState(false, successMessage);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("保存成功");
                alert.setHeaderText("出库草稿已保存");
                alert.setContentText(successMessage);
                alert.showAndWait();
                if (createdData != null && createdData.getId() != null) {
                    onCreated.accept(createdData);
                    return;
                }
                onBack.run();
            });

            saveTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(saveTask.getException(), "保存出库草稿失败，请稍后重试。")));

            Thread thread = new Thread(saveTask, "desktop-outbound-save-draft");
            thread.setDaemon(true);
            thread.start();
        } catch (IllegalArgumentException ex) {
            statusLabel.setText(ex.getMessage());
        }
    }

    private OutboundOrderDraftRequest buildRequest() {
        CustomerRow selectedCustomer = customerBox.getValue();
        if (selectedCustomer == null || selectedCustomer.getId() == null) {
            throw new IllegalArgumentException("请选择客户。");
        }
        if (editors.isEmpty()) {
            throw new IllegalArgumentException("出库明细不能为空。");
        }

        List<OutboundOrderDraftItemRequest> itemList = new ArrayList<>();
        Set<Long> selectedProductIds = new LinkedHashSet<>();

        for (int i = 0; i < editors.size(); i++) {
            DraftItemEditor editor = editors.get(i);
            ProductRow product = editor.getSelectedProduct();
            if (product == null || product.getId() == null) {
                throw new IllegalArgumentException("第 " + (i + 1) + " 行商品不能为空。");
            }
            if (!selectedProductIds.add(product.getId())) {
                throw new IllegalArgumentException("同一商品不能重复出现在出库草稿中。");
            }

            int quantity = parseQuantity(editor.getQuantityText(), i + 1);
            BigDecimal unitPrice = parseUnitPrice(editor.getUnitPriceText(), i + 1);

            OutboundOrderDraftItemRequest item = new OutboundOrderDraftItemRequest();
            item.setProductId(product.getId());
            item.setQuantity(quantity);
            item.setUnitPrice(unitPrice);
            item.setRemark(trimToNull(editor.getRemarkText()));
            itemList.add(item);
        }

        OutboundOrderDraftRequest request = new OutboundOrderDraftRequest();
        request.setCustomerId(selectedCustomer.getId());
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
                customerInfoLabel.setText("未找到匹配客户，请调整查询条件后重试。");
                statusLabel.setText("未找到匹配客户。");
            } else {
                CustomerRow preferred = findPreferredCustomer(customers, currentSelected, customerCode, customerName);
                customerBox.setValue(preferred == null ? customers.get(0) : preferred);
                refreshCustomerInfo();
                statusLabel.setText("客户搜索完成，共找到 " + customers.size() + " 条结果。");
            }
            customerCodeSearchField.setDisable(false);
            customerNameSearchField.setDisable(false);
            customerSearchButton.setDisable(false);
            customerBox.setDisable(false);
        });

        searchTask.setOnFailed(event -> {
            customerOptions.clear();
            customerBox.setValue(null);
            customerInfoLabel.setText("客户搜索失败，请稍后重试。");
            statusLabel.setText(resolveErrorMessage(searchTask.getException(), "客户搜索失败，请稍后重试。"));
            customerCodeSearchField.setDisable(false);
            customerNameSearchField.setDisable(false);
            customerSearchButton.setDisable(false);
            customerBox.setDisable(false);
        });

        Thread thread = new Thread(searchTask, "desktop-outbound-search-customer");
        thread.setDaemon(true);
        thread.start();
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

    private void handleGoCustomerManagement() {
        if (onGoCustomerManagement == null) {
            statusLabel.setText("当前未配置客户管理入口。");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("前往客户管理");
        confirmAlert.setHeaderText("确定要离开当前出库草稿页吗？");
        confirmAlert.setContentText("当前未保存的出库草稿内容将丢失。");
        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        onGoCustomerManagement.run();
    }

    private void applyInitialCustomerSearch() {
        if (isBlank(initialCustomerCode) && isBlank(initialCustomerName)) {
            return;
        }
        customerCodeSearchField.setText(trimText(initialCustomerCode));
        customerNameSearchField.setText(trimText(initialCustomerName));
        statusLabel.setText("已回填新建客户信息，正在自动搜索客户...");
        customerInfoLabel.setText("已回填客户编码 / 名称，正在自动搜索刚新增的客户。");
        searchCustomers();
    }

    private int parseQuantity(String value, int rowIndex) {
        String text = trimText(value);
        if (text.isEmpty()) {
            throw new IllegalArgumentException("第 " + rowIndex + " 行数量不能为空。");
        }
        try {
            int quantity = Integer.parseInt(text);
            if (quantity <= 0) {
                throw new IllegalArgumentException("第 " + rowIndex + " 行数量必须大于 0。");
            }
            return quantity;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("第 " + rowIndex + " 行数量必须是整数。");
        }
    }

    private BigDecimal parseUnitPrice(String value, int rowIndex) {
        String text = trimText(value);
        if (text.isEmpty()) {
            throw new IllegalArgumentException("第 " + rowIndex + " 行单价不能为空。");
        }
        try {
            BigDecimal price = new BigDecimal(text);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("第 " + rowIndex + " 行单价不能小于 0。");
            }
            return price;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("第 " + rowIndex + " 行单价格式不正确。");
        }
    }

    private void setLoadingState(boolean loading, String message) {
        customerCodeSearchField.setDisable(loading);
        customerNameSearchField.setDisable(loading);
        customerSearchButton.setDisable(loading);
        customerBox.setDisable(loading);
        remarkField.setDisable(loading);
        saveButton.setDisable(loading);
        addItemButton.setDisable(loading);
        for (DraftItemEditor editor : editors) {
            editor.setDisable(loading);
        }
        statusLabel.setText(message);
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

    private String buildSaveSuccessMessage(OrderCreatedData createdData, String defaultValue) {
        if (createdData == null) {
            return defaultValue;
        }
        if (!isBlank(createdData.getOrderNo())) {
            return "保存出库草稿成功，单号：" + createdData.getOrderNo().trim();
        }
        if (createdData.getId() != null) {
            return "保存出库草稿成功，ID=" + createdData.getId();
        }
        return defaultValue;
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

    private String formatCustomerOption(CustomerRow customer) {
        if (customer == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(defaultText(customer.getCustomerCode(), "-"));
        builder.append(" / ");
        builder.append(defaultText(customer.getCustomerName(), "-"));
        return builder.toString();
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
            builder.append(" / ");
            builder.append(product.getSpecification().trim());
        }
        if (!isBlank(product.getUnit())) {
            builder.append(" / ");
            builder.append(product.getUnit().trim());
        }
        return builder.toString();
    }

    private class DraftItemEditor {

        private final VBox root;
        private final Label titleLabel;
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

        DraftItemEditor() {
            this.root = new VBox(8);
            this.root.setPadding(new Insets(12));
            this.root.setStyle("-fx-border-color: #e4e7ed; -fx-border-radius: 8; -fx-background-color: #fafafa; -fx-background-radius: 8;");

            this.titleLabel = new Label("明细");
            this.titleLabel.getStyleClass().add("page-label");

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

            this.quantityField = new TextField("1");
            this.quantityField.setPromptText("数量");
            this.quantityField.setPrefWidth(100);

            this.unitPriceField = new TextField("0");
            this.unitPriceField.setPromptText("单价");
            this.unitPriceField.setPrefWidth(120);

            this.amountLabel = new Label("金额：0");
            this.amountLabel.getStyleClass().add("page-label");

            this.remarkField = new TextField();
            this.remarkField.setPromptText("备注");

            this.removeButton = new Button("删除");
            this.removeButton.setOnAction(event -> removeItemEditor(this));

            productBox.setOnAction(event -> refreshProductInfo());
            quantityField.textProperty().addListener((obs, oldValue, newValue) -> refreshAmount());
            unitPriceField.textProperty().addListener((obs, oldValue, newValue) -> refreshAmount());

            HBox firstRow = new HBox(12);
            firstRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(productBox, Priority.ALWAYS);
            firstRow.getChildren().addAll(
                    titleLabel,
                    new Label("编码"),
                    productCodeSearchField,
                    new Label("名称"),
                    productNameSearchField,
                    productSearchButton,
                    new Label("商品"),
                    productBox
            );

            HBox secondRow = new HBox(12);
            secondRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(remarkField, Priority.ALWAYS);
            secondRow.getChildren().addAll(new Label("数量"), quantityField, new Label("单价"), unitPriceField, amountLabel, removeButton, remarkField);

            HBox thirdRow = new HBox(12);
            thirdRow.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(productInfoLabel, Priority.ALWAYS);
            thirdRow.getChildren().addAll(productInfoLabel);

            root.getChildren().addAll(firstRow, secondRow, thirdRow);
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
                productOptions.clear();
                productBox.setValue(null);
                productInfoLabel.setText("商品搜索失败，请稍后重试。");
                statusLabel.setText(resolveErrorMessage(searchTask.getException(), "商品搜索失败，请稍后重试。"));
                setSearchState(false, null);
            });

            Thread thread = new Thread(searchTask, "desktop-outbound-search-product");
            thread.setDaemon(true);
            thread.start();
        }

        void refreshAmount() {
            String quantityText = trimText(quantityField.getText());
            String unitPriceText = trimText(unitPriceField.getText());
            try {
                int quantity = quantityText.isEmpty() ? 0 : Integer.parseInt(quantityText);
                BigDecimal unitPrice = unitPriceText.isEmpty() ? BigDecimal.ZERO : new BigDecimal(unitPriceText);
                if (quantity < 0 || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
                    amountLabel.setText("金额：-");
                    return;
                }
                amountLabel.setText("金额：" + unitPrice.multiply(BigDecimal.valueOf(quantity)).stripTrailingZeros().toPlainString());
            } catch (NumberFormatException ex) {
                amountLabel.setText("金额：-");
            }
        }

        void setDisable(boolean disable) {
            productCodeSearchField.setDisable(disable);
            productNameSearchField.setDisable(disable);
            productSearchButton.setDisable(disable);
            productBox.setDisable(disable);
            quantityField.setDisable(disable);
            unitPriceField.setDisable(disable);
            remarkField.setDisable(disable);
            removeButton.setDisable(disable || editors.size() <= 1);
        }

        void setSearchState(boolean searching, String searchingHint) {
            productCodeSearchField.setDisable(searching);
            productNameSearchField.setDisable(searching);
            productSearchButton.setDisable(searching);
            productBox.setDisable(searching);
            if (searching && searchingHint != null) {
                productInfoLabel.setText(searchingHint);
            } else {
                refreshProductInfo();
            }
        }

        ProductRow getSelectedProduct() {
            return productBox.getValue();
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

    public static class OutboundOrderDraftRequest {
        private Long customerId;
        private String remark;
        private List<OutboundOrderDraftItemRequest> itemList;

        public Long getCustomerId() {
            return customerId;
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public List<OutboundOrderDraftItemRequest> getItemList() {
            return itemList;
        }

        public void setItemList(List<OutboundOrderDraftItemRequest> itemList) {
            this.itemList = itemList;
        }
    }

    public static class OutboundOrderDraftItemRequest {
        private Long productId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private String remark;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
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

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
