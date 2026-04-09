package com.yocaihua.wms.desktop.module.stock;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.StockApi;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Button;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StockListView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final Runnable onViewLogList;
    private final VBox root;
    private final TableView<StockRow> tableView;
    private final TextField productCodeField;
    private final TextField productNameField;
    private final Label pageLabel;
    private final Label statusLabel;
    private final Button searchButton;
    private final Button resetButton;
    private final Button refreshButton;
    private final Button exportButton;
    private final Button exportCsvButton;
    private final Button logButton;
    private final Button firstPageButton;
    private final Button previousButton;
    private final Button nextButton;
    private final Button lastPageButton;
    private final ComboBox<Integer> pageSizeBox;

    private int currentPage = 1;
    private int pageSize = 10;
    private long total = 0;

    public StockListView(StartupContext startupContext, ApiClient apiClient) {
        this(startupContext, apiClient, null);
    }

    public StockListView(StartupContext startupContext, ApiClient apiClient, Runnable onViewLogList) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.onViewLogList = onViewLogList;
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
        this.exportButton = new Button("导出Excel");
        this.exportCsvButton = new Button("导出CSV");
        this.logButton = new Button("库存流水");

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
                exportButton,
                exportCsvButton,
                logButton,
                new Label("每页"),
                pageSizeBox
        );

        logButton.setVisible(onViewLogList != null);
        logButton.setManaged(onViewLogList != null);

        this.tableView = new TableView<>();
        this.tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        this.tableView.setPlaceholder(new Label("暂无库存数据"));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        buildColumns();

        this.pageLabel = new Label();
        this.pageLabel.getStyleClass().add("page-label");

        this.statusLabel = new Label("正在加载库存列表...");
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

        card.getChildren().addAll(queryRow, tableView, footerRow);
        root.getChildren().add(card);

        bindActions();
        loadStockPage(1);
    }

    public Parent getRoot() {
        return root;
    }

    private void bindActions() {
        searchButton.setOnAction(event -> loadStockPage(1));
        resetButton.setOnAction(event -> {
            productCodeField.clear();
            productNameField.clear();
            pageSize = 10;
            pageSizeBox.setValue(pageSize);
            loadStockPage(1);
        });
        refreshButton.setOnAction(event -> loadStockPage(currentPage));
        exportButton.setOnAction(event -> exportStockList("excel"));
        exportCsvButton.setOnAction(event -> exportStockList("csv"));
        logButton.setOnAction(event -> {
            if (onViewLogList != null) {
                onViewLogList.run();
            }
        });
        pageSizeBox.setOnAction(event -> {
            Integer selectedSize = pageSizeBox.getValue();
            pageSize = selectedSize == null || selectedSize < 1 ? 10 : selectedSize;
            loadStockPage(1);
        });
        firstPageButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadStockPage(1);
            }
        });
        previousButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadStockPage(currentPage - 1);
            }
        });
        nextButton.setOnAction(event -> {
            if (hasNextPage()) {
                loadStockPage(currentPage + 1);
            }
        });
        lastPageButton.setOnAction(event -> {
            long lastPage = calculateTotalPages();
            if (lastPage > currentPage) {
                loadStockPage((int) lastPage);
            }
        });
    }

    private void buildColumns() {
        tableView.getColumns().add(createTextColumn("商品编码", StockRow::getProductCode, 120));
        tableView.getColumns().add(createTextColumn("商品名称", StockRow::getProductName, 180));
        tableView.getColumns().add(createTextColumn("规格", StockRow::getSpecification, 120));
        tableView.getColumns().add(createTextColumn("单位", StockRow::getUnit, 80));
        tableView.getColumns().add(createTextColumn("分类", StockRow::getCategory, 120));
        tableView.getColumns().add(createNumberColumn("当前库存", StockRow::getQuantity, 100));
        tableView.getColumns().add(createNumberColumn("预警值", StockRow::getWarningQuantity, 100));
        tableView.getColumns().add(createLowStockColumn());
        tableView.getColumns().add(createActionColumn());
    }

    private TableColumn<StockRow, String> createTextColumn(String title, ValueProvider<String> provider, double width) {
        TableColumn<StockRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(defaultText(provider.get(cellData.getValue()))));
        return column;
    }

    private TableColumn<StockRow, Integer> createNumberColumn(String title, ValueProvider<Integer> provider, double width) {
        TableColumn<StockRow, Integer> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> new SimpleObjectProperty<>(provider.get(cellData.getValue())));
        return column;
    }

    private TableColumn<StockRow, String> createLowStockColumn() {
        TableColumn<StockRow, String> column = new TableColumn<>("低库存");
        column.setPrefWidth(80);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getLowStockText()));
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
                StockRow row = getTableRow() == null ? null : (StockRow) getTableRow().getItem();
                if (row != null && Integer.valueOf(1).equals(row.getLowStock())) {
                    setStyle("-fx-text-fill: #f56c6c; -fx-font-weight: 700;");
                } else {
                    setStyle("-fx-text-fill: #67c23a;");
                }
            }
        });
        return column;
    }

    private TableColumn<StockRow, Void> createActionColumn() {
        TableColumn<StockRow, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(140);
        column.setCellFactory(col -> new TableCell<>() {
            private final Button adjustButton = new Button("调整库存");

            {
                adjustButton.setOnAction(event -> {
                    StockRow row = getTableRow() == null ? null : (StockRow) getTableRow().getItem();
                    if (row != null) {
                        openAdjustDialog(row);
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
                setGraphic(adjustButton);
            }
        });
        return column;
    }

    private void openAdjustDialog(StockRow row) {
        Dialog<StockAdjustRequest> dialog = new Dialog<>();
        dialog.setTitle("库存调整");
        dialog.setHeaderText("商品：" + defaultText(row.getProductName()) + "（" + defaultText(row.getProductCode()) + "）");

        ButtonType confirmButtonType = new ButtonType("保存调整", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        TextField quantityInput = new TextField(String.valueOf(row.getQuantity() == null ? 0 : row.getQuantity()));
        TextField warningQuantityInput = new TextField(String.valueOf(row.getWarningQuantity() == null ? 0 : row.getWarningQuantity()));
        TextArea reasonInput = new TextArea("盘点差异修正");
        reasonInput.setPrefRowCount(3);
        reasonInput.setWrapText(true);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(12);
        formGrid.setPadding(new Insets(8, 0, 0, 0));

        formGrid.add(new Label("当前库存"), 0, 0);
        formGrid.add(quantityInput, 1, 0);
        formGrid.add(new Label("预警库存"), 0, 1);
        formGrid.add(warningQuantityInput, 1, 1);
        formGrid.add(new Label("调整原因"), 0, 2);
        formGrid.add(reasonInput, 1, 2);

        dialog.getDialogPane().setContent(formGrid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != confirmButtonType) {
                return null;
            }

            Integer quantity = parseNonNegativeInt(quantityInput.getText());
            Integer warningQuantity = parseNonNegativeInt(warningQuantityInput.getText());
            String reason = reasonInput.getText() == null ? "" : reasonInput.getText().trim();

            if (quantity == null || warningQuantity == null) {
                showWarning("库存和预警库存必须是大于等于 0 的整数。");
                return null;
            }
            if (isBlank(reason)) {
                showWarning("调整原因不能为空。");
                return null;
            }

            StockAdjustRequest request = new StockAdjustRequest();
            request.productId = row.getProductId();
            request.quantity = quantity;
            request.warningQuantity = warningQuantity;
            request.reason = reason;
            return request;
        });

        dialog.showAndWait().ifPresent(this::submitAdjustStock);
    }

    private void submitAdjustStock(StockAdjustRequest request) {
        setLoadingState(true, "正在保存库存调整...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                ApiResponse<String> response = apiClient.put(StockApi.UPDATE, request, String.class);
                if (response == null || !response.isSuccess()) {
                    String message = response == null ? null : response.getMessage();
                    throw new ApiException(isBlank(message) ? "库存调整失败" : message);
                }
                String message = response.getData();
                if (isBlank(message)) {
                    message = response.getMessage();
                }
                return isBlank(message) ? "库存调整成功" : message.trim();
            }
        };

        task.setOnSucceeded(event -> {
            statusLabel.setText(task.getValue() + "，正在刷新库存列表...");
            loadStockPage(currentPage);
        });

        task.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(task.getException())));

        Thread thread = new Thread(task, "desktop-stock-adjust");
        thread.setDaemon(true);
        thread.start();
    }

    private void loadStockPage(int targetPage) {
        setLoadingState(true, "正在加载库存列表...");

        Task<StockPageData> loadTask = new Task<>() {
            @Override
            protected StockPageData call() {
                ApiResponse<StockPageData> response = apiClient.get(StockApi.LIST, buildQueryParams(targetPage), StockPageData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "库存列表加载失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "库存列表加载失败" : message);
                }
                return response.getData();
            }
        };

        loadTask.setOnSucceeded(event -> {
            StockPageData pageData = loadTask.getValue();
            List<StockRow> rows = pageData.getList();
            tableView.setItems(FXCollections.observableArrayList(rows));
            currentPage = safeInt(pageData.getPageNum(), targetPage);
            pageSize = safeInt(pageData.getPageSize(), pageSize);
            pageSizeBox.setValue(pageSize);
            total = pageData.getTotal() == null ? 0 : pageData.getTotal();
            refreshPageInfo();
            String status = total == 0
                    ? "未查到库存数据。"
                    : "库存列表加载完成，共 " + total + " 条记录。";
            setLoadingState(false, status);
        });

        loadTask.setOnFailed(event -> {
            tableView.setItems(FXCollections.observableArrayList());
            total = 0;
            currentPage = targetPage;
            refreshPageInfo();
            setLoadingState(false, resolveErrorMessage(loadTask.getException()));
        });

        Thread thread = new Thread(loadTask, "desktop-stock-query");
        thread.setDaemon(true);
        thread.start();
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

    private boolean hasNextPage() {
        return currentPage * (long) pageSize < total;
    }

    private long calculateTotalPages() {
        return pageSize <= 0 ? 1 : Math.max(1, (total + pageSize - 1) / pageSize);
    }

    private void setLoadingState(boolean loading, String message) {
        searchButton.setDisable(loading);
        resetButton.setDisable(loading);
        refreshButton.setDisable(loading);
        exportButton.setDisable(loading);
        exportCsvButton.setDisable(loading);
        logButton.setDisable(loading || onViewLogList == null);
        pageSizeBox.setDisable(loading);
        firstPageButton.setDisable(loading || currentPage <= 1);
        previousButton.setDisable(loading || currentPage <= 1);
        nextButton.setDisable(loading || !hasNextPage());
        lastPageButton.setDisable(loading || currentPage >= calculateTotalPages());
        tableView.setDisable(loading);
        statusLabel.setText(message);
    }

    private void exportStockList(String format) {
        String normalizedFormat = "csv".equalsIgnoreCase(format) ? "csv" : "excel";
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出库存列表");
        if ("csv".equals(normalizedFormat)) {
            fileChooser.setInitialFileName("库存列表.csv");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV 文件", "*.csv"));
        } else {
            fileChooser.setInitialFileName("库存列表.xlsx");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));
        }

        java.io.File targetFile = fileChooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (targetFile == null) {
            statusLabel.setText("已取消导出。");
            return;
        }

        String loadingText = "csv".equals(normalizedFormat) ? "正在导出库存列表CSV..." : "正在导出库存列表Excel...";
        setExportState(true, loadingText);

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws IOException {
                byte[] content = apiClient.download(StockApi.EXPORT, buildExportParams(normalizedFormat));
                Files.write(Path.of(targetFile.toURI()), content);
                return null;
            }
        };

        exportTask.setOnSucceeded(event -> {
            String successText = "csv".equals(normalizedFormat) ? "库存列表CSV导出成功：" : "库存列表Excel导出成功：";
            setExportState(false, successText + targetFile.getName());
        });

        exportTask.setOnFailed(event -> {
            setExportState(false, resolveErrorMessage(exportTask.getException()));
        });

        Thread thread = new Thread(exportTask, "desktop-stock-export");
        thread.setDaemon(true);
        thread.start();
    }

    private Map<String, Object> buildExportParams(String format) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("productCode", productCodeField.getText());
        params.put("productName", productNameField.getText());
        params.put("format", format);
        return params;
    }

    private void setExportState(boolean exporting, String message) {
        exportButton.setDisable(exporting);
        exportCsvButton.setDisable(exporting);
        searchButton.setDisable(exporting);
        resetButton.setDisable(exporting);
        refreshButton.setDisable(exporting);
        logButton.setDisable(exporting || onViewLogList == null);
        pageSizeBox.setDisable(exporting);
        firstPageButton.setDisable(exporting || currentPage <= 1);
        previousButton.setDisable(exporting || currentPage <= 1);
        nextButton.setDisable(exporting || !hasNextPage());
        lastPageButton.setDisable(exporting || currentPage >= calculateTotalPages());
        statusLabel.setText(message);
    }

    private int safeInt(Integer value, int defaultValue) {
        return value == null || value < 1 ? defaultValue : value;
    }

    private String resolveErrorMessage(Throwable throwable) {
        if (throwable == null || isBlank(throwable.getMessage())) {
            return "库存列表加载失败，请稍后重试。";
        }
        return throwable.getMessage().trim();
    }

    private String defaultText(String value) {
        return isBlank(value) ? "-" : value.trim();
    }

    private Integer parseNonNegativeInt(String text) {
        if (isBlank(text)) {
            return null;
        }
        try {
            int value = Integer.parseInt(text.trim());
            return value < 0 ? null : value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("输入有误");
        alert.setHeaderText("库存调整参数不合法");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static final class StockAdjustRequest {
        private Long productId;
        private Integer quantity;
        private Integer warningQuantity;
        private String reason;
    }

    @FunctionalInterface
    private interface ValueProvider<T> {
        T get(StockRow row);
    }
}
