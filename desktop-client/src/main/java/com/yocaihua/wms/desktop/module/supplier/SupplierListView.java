package com.yocaihua.wms.desktop.module.supplier;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.SupplierApi;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class SupplierListView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final VBox root;
    private final TableView<SupplierRow> tableView;
    private final TextField supplierCodeField;
    private final TextField supplierNameField;
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
    private final javafx.scene.control.ComboBox<Integer> pageSizeBox;
    private final BiConsumer<String, String> onCreatedForBusiness;

    private int currentPage = 1;
    private int pageSize = 10;
    private long total = 0;

    public SupplierListView(StartupContext startupContext, ApiClient apiClient) {
        this(startupContext, apiClient, null, null, null);
    }

    public SupplierListView(StartupContext startupContext, ApiClient apiClient, String returnButtonText, Runnable onReturnToBusinessPage, BiConsumer<String, String> onCreatedForBusiness) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.onCreatedForBusiness = onCreatedForBusiness;
        this.root = new VBox(16);
        this.root.getStyleClass().add("page-root");
        this.root.setPadding(new Insets(24));

        VBox card = new VBox(16);
        card.getStyleClass().add("page-card");

        Label titleLabel = new Label("供应商管理");
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label("支持供应商查询、分页、新增、编辑、删除和导出。");
        subtitleLabel.getStyleClass().add("page-subtitle");

        FlowPane queryRow = new FlowPane();
        queryRow.setHgap(12);
        queryRow.setVgap(12);
        queryRow.setAlignment(Pos.CENTER_LEFT);

        this.supplierCodeField = new TextField();
        this.supplierCodeField.setPromptText("供应商编码");
        this.supplierCodeField.setPrefWidth(180);

        this.supplierNameField = new TextField();
        this.supplierNameField.setPromptText("供应商名称");
        this.supplierNameField.setPrefWidth(220);

        this.searchButton = new Button("查询");
        this.resetButton = new Button("重置");
        this.refreshButton = new Button("刷新");
        this.addButton = new Button("新增供应商");
        this.exportButton = new Button("导出Excel");
        this.returnButton = new Button(isBlank(returnButtonText) ? "返回业务页" : returnButtonText);
        this.returnButton.setVisible(onReturnToBusinessPage != null);
        this.returnButton.setManaged(onReturnToBusinessPage != null);
        if (onReturnToBusinessPage != null) {
            this.returnButton.setOnAction(event -> onReturnToBusinessPage.run());
        }

        this.pageSizeBox = new javafx.scene.control.ComboBox<>();
        this.pageSizeBox.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        this.pageSizeBox.setValue(pageSize);
        this.pageSizeBox.setPrefWidth(100);

        queryRow.getChildren().addAll(
                new Label("供应商编码"),
                supplierCodeField,
                new Label("供应商名称"),
                supplierNameField,
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
        this.tableView.setPlaceholder(new Label("暂无供应商数据"));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        buildColumns();

        this.pageLabel = new Label();
        this.pageLabel.getStyleClass().add("page-label");

        this.statusLabel = new Label("正在加载供应商列表...");
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

        card.getChildren().addAll(titleLabel, subtitleLabel, queryRow, tableView, footerRow);
        root.getChildren().add(card);

        bindActions();
        loadSupplierPage(1);
    }

    public Parent getRoot() {
        return root;
    }

    private void bindActions() {
        searchButton.setOnAction(event -> loadSupplierPage(1));
        resetButton.setOnAction(event -> {
            supplierCodeField.clear();
            supplierNameField.clear();
            pageSize = 10;
            pageSizeBox.setValue(pageSize);
            loadSupplierPage(1);
        });
        refreshButton.setOnAction(event -> loadSupplierPage(currentPage));
        addButton.setOnAction(event -> openSupplierDialog(null));
        exportButton.setOnAction(event -> exportSupplierList());
        pageSizeBox.setOnAction(event -> {
            Integer selectedSize = pageSizeBox.getValue();
            pageSize = selectedSize == null || selectedSize < 1 ? 10 : selectedSize;
            loadSupplierPage(1);
        });
        firstPageButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadSupplierPage(1);
            }
        });
        previousButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadSupplierPage(currentPage - 1);
            }
        });
        nextButton.setOnAction(event -> {
            if (hasNextPage()) {
                loadSupplierPage(currentPage + 1);
            }
        });
        lastPageButton.setOnAction(event -> {
            long lastPage = calculateTotalPages();
            if (lastPage > currentPage) {
                loadSupplierPage((int) lastPage);
            }
        });
    }

    private void buildColumns() {
        tableView.getColumns().add(createTextColumn("ID", row -> row.getId() == null ? "" : String.valueOf(row.getId()), 80));
        tableView.getColumns().add(createTextColumn("供应商编码", SupplierRow::getSupplierCode, 140));
        tableView.getColumns().add(createTextColumn("供应商名称", SupplierRow::getSupplierName, 180));
        tableView.getColumns().add(createTextColumn("联系人", SupplierRow::getContactPerson, 120));
        tableView.getColumns().add(createTextColumn("联系电话", SupplierRow::getPhone, 140));
        tableView.getColumns().add(createTextColumn("地址", SupplierRow::getAddress, 220));
        tableView.getColumns().add(createTextColumn("备注", SupplierRow::getRemark, 180));
        tableView.getColumns().add(createStatusColumn());
        tableView.getColumns().add(createActionColumn());
    }

    private TableColumn<SupplierRow, String> createTextColumn(String title, ValueProvider provider, double width) {
        TableColumn<SupplierRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(defaultText(provider.get(cellData.getValue()))));
        return column;
    }

    private TableColumn<SupplierRow, String> createStatusColumn() {
        TableColumn<SupplierRow, String> column = new TableColumn<>("状态");
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
                SupplierRow row = getTableRow() == null ? null : (SupplierRow) getTableRow().getItem();
                if (row != null && Integer.valueOf(1).equals(row.getStatus())) {
                    setStyle("-fx-text-fill: #67c23a; -fx-font-weight: 700;");
                } else {
                    setStyle("-fx-text-fill: #909399;");
                }
            }
        });
        return column;
    }

    private TableColumn<SupplierRow, Void> createActionColumn() {
        TableColumn<SupplierRow, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(180);
        column.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("编辑");
            private final Button deleteButton = new Button("删除");
            private final HBox actionBox = new HBox(8, editButton, deleteButton);

            {
                actionBox.setAlignment(Pos.CENTER_LEFT);
                editButton.setOnAction(event -> {
                    SupplierRow row = getTableRow() == null ? null : (SupplierRow) getTableRow().getItem();
                    if (row != null) {
                        openSupplierDialog(row);
                    }
                });
                deleteButton.setOnAction(event -> {
                    SupplierRow row = getTableRow() == null ? null : (SupplierRow) getTableRow().getItem();
                    if (row != null) {
                        handleDeleteSupplier(row);
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

    private void loadSupplierPage(int targetPage) {
        setLoadingState(true, "正在加载供应商列表...");

        Task<SupplierPageData> loadTask = new Task<>() {
            @Override
            protected SupplierPageData call() {
                ApiResponse<SupplierPageData> response = apiClient.get(SupplierApi.LIST, buildQueryParams(targetPage), SupplierPageData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "供应商列表加载失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "供应商列表加载失败" : message);
                }
                return response.getData();
            }
        };

        loadTask.setOnSucceeded(event -> {
            SupplierPageData pageData = loadTask.getValue();
            List<SupplierRow> rows = pageData.getList();
            tableView.setItems(FXCollections.observableArrayList(rows));
            currentPage = safeInt(pageData.getPageNum(), targetPage);
            pageSize = safeInt(pageData.getPageSize(), pageSize);
            pageSizeBox.setValue(pageSize);
            total = pageData.getTotal() == null ? 0 : pageData.getTotal();
            refreshPageInfo();
            String status = total == 0
                    ? "未查到供应商数据。"
                    : "供应商列表加载完成，共 " + total + " 条记录。";
            setLoadingState(false, status);
        });

        loadTask.setOnFailed(event -> {
            tableView.setItems(FXCollections.observableArrayList());
            total = 0;
            currentPage = targetPage;
            refreshPageInfo();
            setLoadingState(false, resolveErrorMessage(loadTask.getException()));
        });

        Thread thread = new Thread(loadTask, "desktop-supplier-query");
        thread.setDaemon(true);
        thread.start();
    }

    private Map<String, Object> buildQueryParams(int targetPage) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("supplierCode", supplierCodeField.getText());
        params.put("supplierName", supplierNameField.getText());
        params.put("pageNum", targetPage);
        params.put("pageSize", pageSize);
        return params;
    }

    private void openSupplierDialog(SupplierRow existingRow) {
        boolean editing = existingRow != null;
        Dialog<SupplierAddRequest> dialog = new Dialog<>();
        dialog.setTitle(editing ? "编辑供应商" : "新增供应商");
        dialog.setHeaderText(editing ? "请修改供应商信息" : "请填写供应商信息");

        ButtonType confirmButtonType = new ButtonType(editing ? "保存修改" : "新增供应商", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        TextField supplierCodeInput = new TextField(editing ? inputText(existingRow.getSupplierCode()) : "");
        TextField supplierNameInput = new TextField(editing ? inputText(existingRow.getSupplierName()) : "");
        TextField contactPersonInput = new TextField(editing ? inputText(existingRow.getContactPerson()) : "");
        TextField phoneInput = new TextField(editing ? inputText(existingRow.getPhone()) : "");
        TextField addressInput = new TextField(editing ? inputText(existingRow.getAddress()) : "");
        TextArea remarkInput = new TextArea();
        if (editing) {
            remarkInput.setText(inputText(existingRow.getRemark()));
        }
        remarkInput.setPrefRowCount(3);
        remarkInput.setWrapText(true);
        ComboBox<String> statusBox = new ComboBox<>();
        statusBox.setItems(FXCollections.observableArrayList("启用", "停用"));
        statusBox.setValue(editing && !Integer.valueOf(1).equals(existingRow.getStatus()) ? "停用" : "启用");
        statusBox.setPrefWidth(160);

        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(12);
        formGrid.setPadding(new Insets(8, 0, 0, 0));

        int rowIndex = 0;
        formGrid.add(new Label("供应商编码"), 0, rowIndex);
        formGrid.add(supplierCodeInput, 1, rowIndex++);
        formGrid.add(new Label("供应商名称"), 0, rowIndex);
        formGrid.add(supplierNameInput, 1, rowIndex++);
        formGrid.add(new Label("联系人"), 0, rowIndex);
        formGrid.add(contactPersonInput, 1, rowIndex++);
        formGrid.add(new Label("联系电话"), 0, rowIndex);
        formGrid.add(phoneInput, 1, rowIndex++);
        formGrid.add(new Label("地址"), 0, rowIndex);
        formGrid.add(addressInput, 1, rowIndex++);
        formGrid.add(new Label("备注"), 0, rowIndex);
        formGrid.add(remarkInput, 1, rowIndex);
        if (editing) {
            rowIndex++;
            formGrid.add(new Label("状态"), 0, rowIndex);
            formGrid.add(statusBox, 1, rowIndex);
        }

        dialog.getDialogPane().setContent(formGrid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != confirmButtonType) {
                return null;
            }

            String supplierCode = trimmed(supplierCodeInput.getText());
            String supplierName = trimmed(supplierNameInput.getText());
            if (isBlank(supplierCode) || isBlank(supplierName)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("信息不完整");
                alert.setHeaderText("供应商编码和供应商名称不能为空");
                alert.setContentText("请补全必填信息后再提交。");
                alert.showAndWait();
                return null;
            }

            SupplierAddRequest request = new SupplierAddRequest();
            request.setId(editing ? existingRow.getId() : null);
            request.setSupplierCode(supplierCode);
            request.setSupplierName(supplierName);
            request.setContactPerson(trimmed(contactPersonInput.getText()));
            request.setPhone(trimmed(phoneInput.getText()));
            request.setAddress(trimmed(addressInput.getText()));
            request.setRemark(trimmed(remarkInput.getText()));
            request.setStatus(editing && "停用".equals(statusBox.getValue()) ? 0 : 1);
            return request;
        });

        Optional<SupplierAddRequest> result = dialog.showAndWait();
        result.ifPresent(request -> submitSupplier(request, editing));
    }

    private void submitSupplier(SupplierAddRequest request, boolean editing) {
        setLoadingState(true, editing ? "正在保存供应商修改..." : "正在新增供应商...");

        Task<String> submitTask = new Task<>() {
            @Override
            protected String call() {
                ApiResponse<String> response;
                if (editing) {
                    response = apiClient.put(SupplierApi.UPDATE, request, String.class);
                } else {
                    response = apiClient.post(SupplierApi.ADD, request, String.class);
                }
                if (response == null || !response.isSuccess()) {
                    String message = response == null ? null : response.getMessage();
                    throw new ApiException(isBlank(message) ? (editing ? "修改供应商失败" : "新增供应商失败") : message);
                }
                String message = response.getData();
                if (isBlank(message)) {
                    message = response.getMessage();
                }
                return isBlank(message) ? (editing ? "修改供应商成功" : "新增供应商成功") : message.trim();
            }
        };

        submitTask.setOnSucceeded(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(editing ? "保存成功" : "新增成功");
            alert.setHeaderText(editing ? "供应商信息已更新" : "供应商已新增");
            alert.setContentText(submitTask.getValue());
            alert.showAndWait();
            if (!editing && onCreatedForBusiness != null) {
                onCreatedForBusiness.accept(request.getSupplierCode(), request.getSupplierName());
            }
            loadSupplierPage(editing ? currentPage : 1);
            if (!editing && returnButton.isVisible()) {
                statusLabel.setText("供应商已新增，可点击“" + returnButton.getText() + "”继续填写业务单据。");
            }
        });

        submitTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(submitTask.getException())));

        Thread thread = new Thread(submitTask, editing ? "desktop-supplier-update" : "desktop-supplier-add");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleDeleteSupplier(SupplierRow row) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("删除供应商");
        confirmAlert.setHeaderText("确定要删除供应商吗？");
        confirmAlert.setContentText("供应商名称：" + defaultText(row.getSupplierName()));

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        setLoadingState(true, "正在删除供应商...");

        Task<String> deleteTask = new Task<>() {
            @Override
            protected String call() {
                ApiResponse<String> response = apiClient.delete(SupplierApi.DELETE_PREFIX + row.getId(), String.class);
                if (response == null || !response.isSuccess()) {
                    String message = response == null ? null : response.getMessage();
                    throw new ApiException(isBlank(message) ? "删除供应商失败" : message);
                }
                String message = response.getData();
                if (isBlank(message)) {
                    message = response.getMessage();
                }
                return isBlank(message) ? "删除供应商成功" : message.trim();
            }
        };

        deleteTask.setOnSucceeded(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("删除成功");
            alert.setHeaderText("供应商已删除");
            alert.setContentText(deleteTask.getValue());
            alert.showAndWait();

            int targetPage = currentPage;
            if (tableView.getItems().size() == 1 && currentPage > 1) {
                targetPage = currentPage - 1;
            }
            loadSupplierPage(targetPage);
        });

        deleteTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(deleteTask.getException())));

        Thread thread = new Thread(deleteTask, "desktop-supplier-delete");
        thread.setDaemon(true);
        thread.start();
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

    private void exportSupplierList() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出供应商列表");
        fileChooser.setInitialFileName("供应商列表.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));

        java.io.File targetFile = fileChooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (targetFile == null) {
            statusLabel.setText("已取消导出。");
            return;
        }

        setExportState(true, "正在导出供应商列表...");

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws IOException {
                byte[] content = apiClient.download(SupplierApi.EXPORT, buildExportParams());
                Files.write(Path.of(targetFile.toURI()), content);
                return null;
            }
        };

        exportTask.setOnSucceeded(event -> setExportState(false, "供应商列表导出成功：" + targetFile.getName()));
        exportTask.setOnFailed(event -> setExportState(false, resolveErrorMessage(exportTask.getException())));

        Thread thread = new Thread(exportTask, "desktop-supplier-export");
        thread.setDaemon(true);
        thread.start();
    }

    private Map<String, Object> buildExportParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("supplierCode", supplierCodeField.getText());
        params.put("supplierName", supplierNameField.getText());
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
            return "供应商操作失败，请稍后重试。";
        }
        return throwable.getMessage().trim();
    }

    private String defaultText(String value) {
        return isBlank(value) ? "-" : value.trim();
    }

    private String trimmed(String value) {
        return value == null ? "" : value.trim();
    }

    private String inputText(String value) {
        return isBlank(value) ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @FunctionalInterface
    private interface ValueProvider {
        String get(SupplierRow row);
    }

    private static final class SupplierAddRequest {
        private Long id;
        private String supplierCode;
        private String supplierName;
        private String contactPerson;
        private String phone;
        private String address;
        private String remark;
        private Integer status;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getSupplierCode() {
            return supplierCode;
        }

        public void setSupplierCode(String supplierCode) {
            this.supplierCode = supplierCode;
        }

        public String getSupplierName() {
            return supplierName;
        }

        public void setSupplierName(String supplierName) {
            this.supplierName = supplierName;
        }

        public String getContactPerson() {
            return contactPerson;
        }

        public void setContactPerson(String contactPerson) {
            this.contactPerson = contactPerson;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }
}
