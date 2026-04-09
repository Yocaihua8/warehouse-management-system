package com.yocaihua.wms.desktop.module.product;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.ProductApi;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class ProductListView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final VBox root;
    private final TableView<ProductRow> tableView;
    private final TextField productCodeField;
    private final TextField productNameField;
    private final Label pageLabel;
    private final Label statusLabel;
    private final Button searchButton;
    private final Button resetButton;
    private final Button refreshButton;
    private final Button addButton;
    private final Button exportButton;
    private final Button returnButton;
    private final Button firstPageButton;
    private final Button previousButton;
    private final Button nextButton;
    private final Button lastPageButton;
    private final ComboBox<Integer> pageSizeBox;
    private final BiConsumer<String, String> onCreatedForBusiness;

    private int currentPage = 1;
    private int pageSize = 10;
    private long total = 0;

    public ProductListView(StartupContext startupContext, ApiClient apiClient) {
        this(startupContext, apiClient, null, null, null);
    }

    public ProductListView(StartupContext startupContext, ApiClient apiClient, String returnButtonText, Runnable onReturnToBusinessPage, BiConsumer<String, String> onCreatedForBusiness) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.onCreatedForBusiness = onCreatedForBusiness;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));

        VBox card = new VBox(16);
        card.getStyleClass().add("page-card");

        FlowPane queryRow = new FlowPane();
        queryRow.setHgap(12);
        queryRow.setVgap(12);
        queryRow.setAlignment(Pos.CENTER_LEFT);

        this.productCodeField = new TextField();
        this.productCodeField.setPromptText("商品编码");
        this.productCodeField.setPrefWidth(180);

        this.productNameField = new TextField();
        this.productNameField.setPromptText("商品名称");
        this.productNameField.setPrefWidth(220);

        this.searchButton = new Button("查询");
        this.resetButton = new Button("重置");
        this.refreshButton = new Button("刷新");
        this.addButton = new Button("新增商品");
        this.exportButton = new Button("导出Excel");
        this.returnButton = new Button(isBlank(returnButtonText) ? "返回业务页" : returnButtonText);
        this.returnButton.setVisible(onReturnToBusinessPage != null);
        this.returnButton.setManaged(onReturnToBusinessPage != null);
        if (onReturnToBusinessPage != null) {
            this.returnButton.setOnAction(event -> onReturnToBusinessPage.run());
        }
        Label noteLabel = new Label("说明：商品状态当前仅用于展示，桌面端编辑暂不修改状态；商品编码修改也未开放。删除失败通常表示商品仍有库存，或已被入库单 / 出库单引用。");
        noteLabel.getStyleClass().add("placeholder-note");
        noteLabel.setWrapText(true);

        this.pageSizeBox = new ComboBox<>();
        this.pageSizeBox.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        this.pageSizeBox.setValue(pageSize);
        this.pageSizeBox.setPrefWidth(100);

        queryRow.getChildren().addAll(
                new Label("商品编码"),
                productCodeField,
                new Label("商品名称"),
                productNameField,
                searchButton,
                resetButton,
                refreshButton,
                addButton,
                exportButton,
                returnButton,
                new Label("每页"),
                pageSizeBox
        );

        this.tableView = new TableView<>();
        this.tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        this.tableView.setPlaceholder(new Label("暂无商品数据"));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        buildColumns();

        this.pageLabel = new Label();
        this.pageLabel.getStyleClass().add("page-label");

        this.statusLabel = new Label("正在加载商品列表...");
        this.statusLabel.getStyleClass().add("page-subtitle");

        this.firstPageButton = new Button("首页");
        this.previousButton = new Button("上一页");
        this.nextButton = new Button("下一页");
        this.lastPageButton = new Button("末页");

        HBox footerRow = new HBox(12);
        footerRow.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        footerRow.getChildren().addAll(statusLabel, spacer, pageLabel, firstPageButton, previousButton, nextButton, lastPageButton);

        card.getChildren().addAll(queryRow, noteLabel, tableView, footerRow);
        root.getChildren().add(card);

        bindActions();
        loadProductPage(1);
    }

    public Parent getRoot() {
        return root;
    }

    private void bindActions() {
        searchButton.setOnAction(event -> loadProductPage(1));
        resetButton.setOnAction(event -> {
            productCodeField.clear();
            productNameField.clear();
            pageSize = 10;
            pageSizeBox.setValue(pageSize);
            loadProductPage(1);
        });
        refreshButton.setOnAction(event -> loadProductPage(currentPage));
        addButton.setOnAction(event -> openProductDialog(null));
        exportButton.setOnAction(event -> exportProductList());
        pageSizeBox.setOnAction(event -> {
            Integer selectedSize = pageSizeBox.getValue();
            pageSize = selectedSize == null || selectedSize < 1 ? 10 : selectedSize;
            loadProductPage(1);
        });
        firstPageButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadProductPage(1);
            }
        });
        previousButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadProductPage(currentPage - 1);
            }
        });
        nextButton.setOnAction(event -> {
            if (hasNextPage()) {
                loadProductPage(currentPage + 1);
            }
        });
        lastPageButton.setOnAction(event -> {
            long lastPage = calculateTotalPages();
            if (lastPage > currentPage) {
                loadProductPage((int) lastPage);
            }
        });
    }

    private void buildColumns() {
        tableView.getColumns().add(createTextColumn("ID", row -> row.getId() == null ? "" : String.valueOf(row.getId()), 80));
        tableView.getColumns().add(createTextColumn("商品编码", ProductRow::getProductCode, 140));
        tableView.getColumns().add(createTextColumn("商品名称", ProductRow::getProductName, 180));
        tableView.getColumns().add(createTextColumn("规格", ProductRow::getSpecification, 120));
        tableView.getColumns().add(createTextColumn("单位", ProductRow::getUnit, 80));
        tableView.getColumns().add(createTextColumn("分类", ProductRow::getCategory, 100));
        tableView.getColumns().add(createTextColumn("销售价", ProductRow::getSalePriceText, 100));
        tableView.getColumns().add(createTextColumn("备注", ProductRow::getRemark, 180));
        tableView.getColumns().add(createStatusColumn());
        tableView.getColumns().add(createActionColumn());
    }

    private TableColumn<ProductRow, String> createTextColumn(String title, ValueProvider provider, double width) {
        TableColumn<ProductRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(defaultText(provider.get(cellData.getValue()))));
        return column;
    }

    private TableColumn<ProductRow, String> createStatusColumn() {
        TableColumn<ProductRow, String> column = new TableColumn<>("状态");
        column.setPrefWidth(80);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatusText()));
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                ProductRow row = getTableRow() == null ? null : (ProductRow) getTableRow().getItem();
                if (row != null && Integer.valueOf(1).equals(row.getStatus())) {
                    setStyle("-fx-text-fill: #67c23a; -fx-font-weight: 700;");
                } else {
                    setStyle("-fx-text-fill: #909399;");
                }
            }
        });
        return column;
    }

    private TableColumn<ProductRow, Void> createActionColumn() {
        TableColumn<ProductRow, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(180);
        column.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("编辑");
            private final Button deleteButton = new Button("删除");
            private final HBox actionBox = new HBox(8, editButton, deleteButton);

            {
                actionBox.setAlignment(Pos.CENTER_LEFT);
                editButton.setOnAction(event -> {
                    ProductRow row = getTableRow() == null ? null : (ProductRow) getTableRow().getItem();
                    if (row != null) {
                        openProductDialog(row);
                    }
                });
                deleteButton.setOnAction(event -> {
                    ProductRow row = getTableRow() == null ? null : (ProductRow) getTableRow().getItem();
                    if (row != null) {
                        handleDeleteProduct(row);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                setGraphic(actionBox);
            }
        });
        return column;
    }

    private void loadProductPage(int targetPage) {
        setLoadingState(true, "正在加载商品列表...");

        Task<ProductPageData> loadTask = new Task<>() {
            @Override
            protected ProductPageData call() {
                ApiResponse<ProductPageData> response = apiClient.get(ProductApi.LIST, buildQueryParams(targetPage), ProductPageData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "商品列表加载失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "商品列表加载失败" : message);
                }
                return response.getData();
            }
        };

        loadTask.setOnSucceeded(event -> {
            ProductPageData pageData = loadTask.getValue();
            List<ProductRow> rows = pageData.getList();
            tableView.setItems(FXCollections.observableArrayList(rows));
            currentPage = safeInt(pageData.getPageNum(), targetPage);
            pageSize = safeInt(pageData.getPageSize(), pageSize);
            pageSizeBox.setValue(pageSize);
            total = pageData.getTotal() == null ? 0 : pageData.getTotal();
            refreshPageInfo();
            String status = total == 0
                    ? "未查到商品数据。"
                    : "商品列表加载完成，共 " + total + " 条记录。";
            setLoadingState(false, status);
        });

        loadTask.setOnFailed(event -> {
            tableView.setItems(FXCollections.observableArrayList());
            total = 0;
            currentPage = targetPage;
            refreshPageInfo();
            setLoadingState(false, resolveErrorMessage(loadTask.getException()));
        });

        Thread thread = new Thread(loadTask, "desktop-product-query");
        thread.setDaemon(true);
        thread.start();
    }

    private void openProductDialog(ProductRow existingRow) {
        boolean editing = existingRow != null;
        Dialog<ProductFormData> dialog = new Dialog<>();
        dialog.setTitle(editing ? "编辑商品" : "新增商品");
        dialog.setHeaderText(editing ? "请修改商品信息" : "请填写商品信息");

        ButtonType confirmButtonType = new ButtonType(editing ? "保存修改" : "新增商品", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        TextField productCodeInput = new TextField(editing ? inputText(existingRow.getProductCode()) : "");
        productCodeInput.setDisable(editing);
        TextField productNameInput = new TextField(editing ? inputText(existingRow.getProductName()) : "");
        TextField specificationInput = new TextField(editing ? inputText(existingRow.getSpecification()) : "");
        TextField unitInput = new TextField(editing ? inputText(existingRow.getUnit()) : "");
        TextField categoryInput = new TextField(editing ? inputText(existingRow.getCategory()) : "");
        TextField salePriceInput = new TextField(editing && existingRow.getSalePrice() != null
                ? existingRow.getSalePrice().stripTrailingZeros().toPlainString()
                : "");
        TextArea remarkInput = new TextArea(editing ? inputText(existingRow.getRemark()) : "");
        remarkInput.setPrefRowCount(3);
        remarkInput.setWrapText(true);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(12);
        formGrid.setPadding(new Insets(8, 0, 0, 0));

        int rowIndex = 0;
        formGrid.add(new Label("商品编码"), 0, rowIndex);
        formGrid.add(productCodeInput, 1, rowIndex++);
        formGrid.add(new Label("商品名称"), 0, rowIndex);
        formGrid.add(productNameInput, 1, rowIndex++);
        formGrid.add(new Label("规格"), 0, rowIndex);
        formGrid.add(specificationInput, 1, rowIndex++);
        formGrid.add(new Label("单位"), 0, rowIndex);
        formGrid.add(unitInput, 1, rowIndex++);
        formGrid.add(new Label("分类"), 0, rowIndex);
        formGrid.add(categoryInput, 1, rowIndex++);
        formGrid.add(new Label("销售价"), 0, rowIndex);
        formGrid.add(salePriceInput, 1, rowIndex++);
        formGrid.add(new Label("备注"), 0, rowIndex);
        formGrid.add(remarkInput, 1, rowIndex);

        dialog.getDialogPane().setContent(formGrid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != confirmButtonType) {
                return null;
            }

            String productCode = trimmed(productCodeInput.getText());
            String productName = trimmed(productNameInput.getText());
            String salePriceText = trimmed(salePriceInput.getText());
            if (isBlank(productCode) || isBlank(productName) || isBlank(salePriceText)) {
                showWarning("信息不完整", "商品编码、商品名称和销售价不能为空", "请补全必填信息后再提交。");
                return null;
            }

            BigDecimal salePrice;
            try {
                salePrice = new BigDecimal(salePriceText);
            } catch (NumberFormatException ex) {
                showWarning("价格格式错误", "销售价必须是有效数字", "请检查销售价输入格式。");
                return null;
            }

            if (salePrice.compareTo(BigDecimal.ZERO) < 0) {
                showWarning("价格不合法", "销售价不能小于 0", "请重新输入销售价。");
                return null;
            }

            ProductFormData formData = new ProductFormData();
            formData.id = editing ? existingRow.getId() : null;
            formData.productCode = productCode;
            formData.productName = productName;
            formData.specification = trimmed(specificationInput.getText());
            formData.unit = trimmed(unitInput.getText());
            formData.category = trimmed(categoryInput.getText());
            formData.salePrice = salePrice;
            formData.remark = trimmed(remarkInput.getText());
            return formData;
        });

        Optional<ProductFormData> result = dialog.showAndWait();
        result.ifPresent(formData -> submitProduct(formData, editing));
    }

    private void submitProduct(ProductFormData formData, boolean editing) {
        setLoadingState(true, editing ? "正在保存商品修改..." : "正在新增商品...");

        Task<String> submitTask = new Task<>() {
            @Override
            protected String call() {
                ApiResponse<String> response;
                if (editing) {
                    response = apiClient.put(ProductApi.UPDATE, buildUpdateRequest(formData), String.class);
                } else {
                    response = apiClient.post(ProductApi.ADD, buildAddRequest(formData), String.class);
                }

                if (response == null || !response.isSuccess()) {
                    String message = response == null ? null : response.getMessage();
                    throw new ApiException(isBlank(message) ? (editing ? "修改商品失败" : "新增商品失败") : message);
                }

                String message = response.getData();
                if (isBlank(message)) {
                    message = response.getMessage();
                }
                return isBlank(message) ? (editing ? "修改商品成功" : "新增商品成功") : message.trim();
            }
        };

        submitTask.setOnSucceeded(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(editing ? "保存成功" : "新增成功");
            alert.setHeaderText(editing ? "商品信息已更新" : "商品已新增");
            alert.setContentText(submitTask.getValue());
            alert.showAndWait();
            if (!editing && onCreatedForBusiness != null) {
                onCreatedForBusiness.accept(formData.productCode, formData.productName);
            }
            loadProductPage(editing ? currentPage : 1);
            if (!editing && returnButton.isVisible()) {
                statusLabel.setText("商品已新增，可点击“" + returnButton.getText() + "”继续返回业务确认页。");
            }
        });

        submitTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(submitTask.getException())));

        Thread thread = new Thread(submitTask, editing ? "desktop-product-update" : "desktop-product-add");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleDeleteProduct(ProductRow row) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("删除商品");
        confirmAlert.setHeaderText("确定要删除商品吗？");
        confirmAlert.setContentText("商品名称：" + defaultText(row.getProductName()));

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        setLoadingState(true, "正在删除商品...");

        Task<String> deleteTask = new Task<>() {
            @Override
            protected String call() {
                ApiResponse<String> response = apiClient.delete(ProductApi.DELETE_PREFIX + row.getId(), String.class);
                if (response == null || !response.isSuccess()) {
                    String message = response == null ? null : response.getMessage();
                    throw new ApiException(isBlank(message) ? "删除商品失败" : message);
                }
                String message = response.getData();
                if (isBlank(message)) {
                    message = response.getMessage();
                }
                return isBlank(message) ? "删除商品成功" : message.trim();
            }
        };

        deleteTask.setOnSucceeded(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("删除成功");
            alert.setHeaderText("商品已删除");
            alert.setContentText(deleteTask.getValue());
            alert.showAndWait();

            int targetPage = currentPage;
            if (tableView.getItems().size() == 1 && currentPage > 1) {
                targetPage = currentPage - 1;
            }
            loadProductPage(targetPage);
        });

        deleteTask.setOnFailed(event -> {
            String message = resolveErrorMessage(deleteTask.getException());
            setLoadingState(false, message);
            showWarning("删除失败", "商品未删除", message);
        });

        Thread thread = new Thread(deleteTask, "desktop-product-delete");
        thread.setDaemon(true);
        thread.start();
    }

    private ProductAddRequest buildAddRequest(ProductFormData formData) {
        ProductAddRequest request = new ProductAddRequest();
        request.setProductCode(formData.productCode);
        request.setProductName(formData.productName);
        request.setSpecification(formData.specification);
        request.setUnit(formData.unit);
        request.setCategory(formData.category);
        request.setSalePrice(formData.salePrice);
        request.setRemark(formData.remark);
        return request;
    }

    private ProductUpdateRequest buildUpdateRequest(ProductFormData formData) {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setId(formData.id);
        request.setProductCode(formData.productCode);
        request.setProductName(formData.productName);
        request.setSpecification(formData.specification);
        request.setUnit(formData.unit);
        request.setCategory(formData.category);
        request.setSalePrice(formData.salePrice);
        request.setRemark(formData.remark);
        return request;
    }

    private Map<String, Object> buildQueryParams(int targetPage) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("productCode", productCodeField.getText());
        params.put("productName", productNameField.getText());
        params.put("pageNum", targetPage);
        params.put("pageSize", pageSize);
        return params;
    }

    private void refreshPageInfo() {
        long totalPages = calculateTotalPages();
        pageLabel.setText("共 " + total + " 条，第 " + currentPage + " / " + totalPages + " 页");
        firstPageButton.setDisable(currentPage <= 1);
        previousButton.setDisable(currentPage <= 1);
        nextButton.setDisable(!hasNextPage());
        lastPageButton.setDisable(currentPage >= totalPages);
    }

    private long calculateTotalPages() {
        return pageSize <= 0 ? 1 : Math.max(1, (total + pageSize - 1) / pageSize);
    }

    private boolean hasNextPage() {
        return currentPage * (long) pageSize < total;
    }

    private void setLoadingState(boolean loading, String message) {
        searchButton.setDisable(loading);
        resetButton.setDisable(loading);
        refreshButton.setDisable(loading);
        addButton.setDisable(loading);
        exportButton.setDisable(loading);
        returnButton.setDisable(loading);
        pageSizeBox.setDisable(loading);
        firstPageButton.setDisable(loading || currentPage <= 1);
        previousButton.setDisable(loading || currentPage <= 1);
        nextButton.setDisable(loading || !hasNextPage());
        lastPageButton.setDisable(loading || currentPage >= calculateTotalPages());
        tableView.setDisable(loading);
        statusLabel.setText(message);
    }

    private void exportProductList() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出商品列表");
        fileChooser.setInitialFileName("商品列表.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));

        java.io.File targetFile = fileChooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (targetFile == null) {
            statusLabel.setText("已取消导出。");
            return;
        }

        setExportState(true, "正在导出商品列表...");

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws IOException {
                byte[] content = apiClient.download(ProductApi.EXPORT, buildExportParams());
                Files.write(Path.of(targetFile.toURI()), content);
                return null;
            }
        };

        exportTask.setOnSucceeded(event -> setExportState(false, "商品列表导出成功：" + targetFile.getName()));
        exportTask.setOnFailed(event -> setExportState(false, resolveErrorMessage(exportTask.getException())));

        Thread thread = new Thread(exportTask, "desktop-product-export");
        thread.setDaemon(true);
        thread.start();
    }

    private Map<String, Object> buildExportParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("productCode", productCodeField.getText());
        params.put("productName", productNameField.getText());
        return params;
    }

    private void setExportState(boolean exporting, String message) {
        searchButton.setDisable(exporting);
        resetButton.setDisable(exporting);
        refreshButton.setDisable(exporting);
        addButton.setDisable(exporting);
        exportButton.setDisable(exporting);
        returnButton.setDisable(exporting);
        pageSizeBox.setDisable(exporting);
        firstPageButton.setDisable(exporting || currentPage <= 1);
        previousButton.setDisable(exporting || currentPage <= 1);
        nextButton.setDisable(exporting || !hasNextPage());
        lastPageButton.setDisable(exporting || currentPage >= calculateTotalPages());
        tableView.setDisable(exporting);
        statusLabel.setText(message);
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null || value < 1 ? defaultValue : value;
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null || isBlank(throwable.getMessage())) {
            return "商品列表加载失败，请稍后重试。";
        }
        return throwable.getMessage().trim();
    }

    private String defaultText(String value) {
        return isBlank(value) ? "-" : value.trim();
    }

    private String inputText(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private void showWarning(String title, String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.showAndWait();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @FunctionalInterface
    private interface ValueProvider {
        String get(ProductRow row);
    }

    private static final class ProductFormData {
        private Long id;
        private String productCode;
        private String productName;
        private String specification;
        private String unit;
        private String category;
        private BigDecimal salePrice;
        private String remark;
    }

    private static final class ProductAddRequest {
        private String productCode;
        private String productName;
        private String specification;
        private String unit;
        private String category;
        private BigDecimal salePrice;
        private String remark;

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
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

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getSalePrice() {
            return salePrice;
        }

        public void setSalePrice(BigDecimal salePrice) {
            this.salePrice = salePrice;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }

    private static final class ProductUpdateRequest {
        private Long id;
        private String productCode;
        private String productName;
        private String specification;
        private String unit;
        private String category;
        private BigDecimal salePrice;
        private String remark;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getProductCode() {
            return productCode;
        }

        public void setProductCode(String productCode) {
            this.productCode = productCode;
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

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public BigDecimal getSalePrice() {
            return salePrice;
        }

        public void setSalePrice(BigDecimal salePrice) {
            this.salePrice = salePrice;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }
    }
}
