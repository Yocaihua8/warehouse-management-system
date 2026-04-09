package com.yocaihua.wms.desktop.module.customer;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.ApiException;
import com.yocaihua.wms.desktop.api.ApiResponse;
import com.yocaihua.wms.desktop.api.endpoint.CustomerApi;
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

public class CustomerListView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final VBox root;
    private final TableView<CustomerRow> tableView;
    private final TextField customerCodeField;
    private final TextField customerNameField;
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

    public CustomerListView(StartupContext startupContext, ApiClient apiClient) {
        this(startupContext, apiClient, null, null, null);
    }

    public CustomerListView(StartupContext startupContext, ApiClient apiClient, String returnButtonText, Runnable onReturnToBusinessPage, BiConsumer<String, String> onCreatedForBusiness) {
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

        this.customerCodeField = new TextField();
        this.customerCodeField.setPromptText("客户编码");
        this.customerCodeField.setPrefWidth(180);

        this.customerNameField = new TextField();
        this.customerNameField.setPromptText("客户名称");
        this.customerNameField.setPrefWidth(220);

        this.searchButton = new Button("查询");
        this.resetButton = new Button("重置");
        this.refreshButton = new Button("刷新");
        this.addButton = new Button("新增客户");
        this.exportButton = new Button("导出Excel");
        this.returnButton = new Button(isBlank(returnButtonText) ? "返回业务页" : returnButtonText);
        this.returnButton.setVisible(onReturnToBusinessPage != null);
        this.returnButton.setManaged(onReturnToBusinessPage != null);
        if (onReturnToBusinessPage != null) {
            this.returnButton.setOnAction(event -> onReturnToBusinessPage.run());
        }

        this.pageSizeBox = new ComboBox<>();
        this.pageSizeBox.setItems(FXCollections.observableArrayList(10, 20, 50, 100));
        this.pageSizeBox.setValue(pageSize);
        this.pageSizeBox.setPrefWidth(100);

        queryRow.getChildren().addAll(
                new Label("客户编码"),
                customerCodeField,
                new Label("客户名称"),
                customerNameField,
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
        this.tableView.setPlaceholder(new Label("暂无客户数据"));
        VBox.setVgrow(tableView, Priority.ALWAYS);
        buildColumns();

        this.pageLabel = new Label();
        this.pageLabel.getStyleClass().add("page-label");

        this.statusLabel = new Label("正在加载客户列表...");
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
        loadCustomerPage(1);
    }

    public Parent getRoot() {
        return root;
    }

    private void bindActions() {
        searchButton.setOnAction(event -> loadCustomerPage(1));
        resetButton.setOnAction(event -> {
            customerCodeField.clear();
            customerNameField.clear();
            pageSize = 10;
            pageSizeBox.setValue(pageSize);
            loadCustomerPage(1);
        });
        refreshButton.setOnAction(event -> loadCustomerPage(currentPage));
        addButton.setOnAction(event -> openCustomerDialog(null));
        exportButton.setOnAction(event -> exportCustomerList());
        pageSizeBox.setOnAction(event -> {
            Integer selectedSize = pageSizeBox.getValue();
            pageSize = selectedSize == null || selectedSize < 1 ? 10 : selectedSize;
            loadCustomerPage(1);
        });
        firstPageButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadCustomerPage(1);
            }
        });
        previousButton.setOnAction(event -> {
            if (currentPage > 1) {
                loadCustomerPage(currentPage - 1);
            }
        });
        nextButton.setOnAction(event -> {
            if (hasNextPage()) {
                loadCustomerPage(currentPage + 1);
            }
        });
        lastPageButton.setOnAction(event -> {
            long lastPage = calculateTotalPages();
            if (lastPage > currentPage) {
                loadCustomerPage((int) lastPage);
            }
        });
    }

    private void buildColumns() {
        tableView.getColumns().add(createTextColumn("ID", row -> row.getId() == null ? "" : String.valueOf(row.getId()), 80));
        tableView.getColumns().add(createTextColumn("客户编码", CustomerRow::getCustomerCode, 140));
        tableView.getColumns().add(createTextColumn("客户名称", CustomerRow::getCustomerName, 180));
        tableView.getColumns().add(createTextColumn("联系人", CustomerRow::getContactPerson, 120));
        tableView.getColumns().add(createTextColumn("联系电话", CustomerRow::getPhone, 140));
        tableView.getColumns().add(createTextColumn("地址", CustomerRow::getAddress, 220));
        tableView.getColumns().add(createTextColumn("备注", CustomerRow::getRemark, 180));
        tableView.getColumns().add(createStatusColumn());
        tableView.getColumns().add(createActionColumn());
    }

    private TableColumn<CustomerRow, String> createTextColumn(String title, ValueProvider provider, double width) {
        TableColumn<CustomerRow, String> column = new TableColumn<>(title);
        column.setPrefWidth(width);
        column.setCellValueFactory(cellData -> new SimpleStringProperty(defaultText(provider.get(cellData.getValue()))));
        return column;
    }

    private TableColumn<CustomerRow, String> createStatusColumn() {
        TableColumn<CustomerRow, String> column = new TableColumn<>("状态");
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
                CustomerRow row = getTableRow() == null ? null : (CustomerRow) getTableRow().getItem();
                if (row != null && Integer.valueOf(1).equals(row.getStatus())) {
                    setStyle("-fx-text-fill: #67c23a; -fx-font-weight: 700;");
                } else {
                    setStyle("-fx-text-fill: #909399;");
                }
            }
        });
        return column;
    }

    private TableColumn<CustomerRow, Void> createActionColumn() {
        TableColumn<CustomerRow, Void> column = new TableColumn<>("操作");
        column.setPrefWidth(180);
        column.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("编辑");
            private final Button deleteButton = new Button("删除");
            private final HBox actionBox = new HBox(8, editButton, deleteButton);

            {
                actionBox.setAlignment(Pos.CENTER_LEFT);
                editButton.setOnAction(event -> {
                    CustomerRow row = getTableRow() == null ? null : (CustomerRow) getTableRow().getItem();
                    if (row != null) {
                        openCustomerDialog(row);
                    }
                });
                deleteButton.setOnAction(event -> {
                    CustomerRow row = getTableRow() == null ? null : (CustomerRow) getTableRow().getItem();
                    if (row != null) {
                        handleDeleteCustomer(row);
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

    private void loadCustomerPage(int targetPage) {
        setLoadingState(true, "正在加载客户列表...");

        Task<CustomerPageData> loadTask = new Task<>() {
            @Override
            protected CustomerPageData call() {
                ApiResponse<CustomerPageData> response = apiClient.get(CustomerApi.LIST, buildQueryParams(targetPage), CustomerPageData.class);
                if (response == null || !response.isSuccess() || response.getData() == null) {
                    String message = response == null ? "客户列表加载失败，请检查服务端是否可用" : response.getMessage();
                    throw new ApiException(isBlank(message) ? "客户列表加载失败" : message);
                }
                return response.getData();
            }
        };

        loadTask.setOnSucceeded(event -> {
            CustomerPageData pageData = loadTask.getValue();
            List<CustomerRow> rows = pageData.getList();
            tableView.setItems(FXCollections.observableArrayList(rows));
            currentPage = safeInt(pageData.getPageNum(), targetPage);
            pageSize = safeInt(pageData.getPageSize(), pageSize);
            pageSizeBox.setValue(pageSize);
            total = pageData.getTotal() == null ? 0 : pageData.getTotal();
            refreshPageInfo();
            String status = total == 0
                    ? "未查到客户数据。"
                    : "客户列表加载完成，共 " + total + " 条记录。";
            setLoadingState(false, status);
        });

        loadTask.setOnFailed(event -> {
            tableView.setItems(FXCollections.observableArrayList());
            total = 0;
            currentPage = targetPage;
            refreshPageInfo();
            setLoadingState(false, resolveErrorMessage(loadTask.getException()));
        });

        Thread thread = new Thread(loadTask, "desktop-customer-query");
        thread.setDaemon(true);
        thread.start();
    }

    private Map<String, Object> buildQueryParams(int targetPage) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("customerCode", customerCodeField.getText());
        params.put("customerName", customerNameField.getText());
        params.put("pageNum", targetPage);
        params.put("pageSize", pageSize);
        return params;
    }

    private void openCustomerDialog(CustomerRow existingRow) {
        boolean editing = existingRow != null;
        Dialog<CustomerFormData> dialog = new Dialog<>();
        dialog.setTitle(editing ? "编辑客户" : "新增客户");
        dialog.setHeaderText(editing ? "请修改客户信息" : "请填写客户信息");

        ButtonType confirmButtonType = new ButtonType(editing ? "保存修改" : "新增客户", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        TextField customerCodeInput = new TextField(editing ? inputText(existingRow.getCustomerCode()) : "");
        TextField customerNameInput = new TextField(editing ? inputText(existingRow.getCustomerName()) : "");
        TextField contactPersonInput = new TextField(editing ? inputText(existingRow.getContactPerson()) : "");
        TextField phoneInput = new TextField(editing ? inputText(existingRow.getPhone()) : "");
        TextField addressInput = new TextField(editing ? inputText(existingRow.getAddress()) : "");
        TextArea remarkInput = new TextArea(editing ? inputText(existingRow.getRemark()) : "");
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
        formGrid.add(new Label("客户编码"), 0, rowIndex);
        formGrid.add(customerCodeInput, 1, rowIndex++);
        formGrid.add(new Label("客户名称"), 0, rowIndex);
        formGrid.add(customerNameInput, 1, rowIndex++);
        formGrid.add(new Label("联系人"), 0, rowIndex);
        formGrid.add(contactPersonInput, 1, rowIndex++);
        formGrid.add(new Label("联系电话"), 0, rowIndex);
        formGrid.add(phoneInput, 1, rowIndex++);
        formGrid.add(new Label("地址"), 0, rowIndex);
        formGrid.add(addressInput, 1, rowIndex++);
        formGrid.add(new Label("备注"), 0, rowIndex);
        formGrid.add(remarkInput, 1, rowIndex++);
        if (editing) {
            formGrid.add(new Label("状态"), 0, rowIndex);
            formGrid.add(statusBox, 1, rowIndex);
        }

        dialog.getDialogPane().setContent(formGrid);

        dialog.setResultConverter(buttonType -> {
            if (buttonType != confirmButtonType) {
                return null;
            }

            String customerCode = trimmed(customerCodeInput.getText());
            String customerName = trimmed(customerNameInput.getText());
            if (isBlank(customerCode) || isBlank(customerName)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("信息不完整");
                alert.setHeaderText("客户编码和客户名称不能为空");
                alert.setContentText("请补全必填信息后再提交。");
                alert.showAndWait();
                return null;
            }

            CustomerFormData formData = new CustomerFormData();
            formData.id = editing ? existingRow.getId() : null;
            formData.customerCode = customerCode;
            formData.customerName = customerName;
            formData.contactPerson = trimmed(contactPersonInput.getText());
            formData.phone = trimmed(phoneInput.getText());
            formData.address = trimmed(addressInput.getText());
            formData.remark = trimmed(remarkInput.getText());
            formData.status = editing && "停用".equals(statusBox.getValue()) ? 0 : 1;
            return formData;
        });

        Optional<CustomerFormData> result = dialog.showAndWait();
        result.ifPresent(formData -> submitCustomer(formData, editing));
    }

    private void submitCustomer(CustomerFormData formData, boolean editing) {
        setLoadingState(true, editing ? "正在保存客户修改..." : "正在新增客户...");

        Task<String> submitTask = new Task<>() {
            @Override
            protected String call() {
                ApiResponse<String> response;
                if (editing) {
                    response = apiClient.put(CustomerApi.UPDATE, buildUpdateRequest(formData), String.class);
                } else {
                    response = apiClient.post(CustomerApi.ADD, buildAddRequest(formData), String.class);
                }

                if (response == null || !response.isSuccess()) {
                    String message = response == null ? null : response.getMessage();
                    throw new ApiException(isBlank(message) ? (editing ? "修改客户失败" : "新增客户失败") : message);
                }

                String message = response.getData();
                if (isBlank(message)) {
                    message = response.getMessage();
                }
                return isBlank(message) ? (editing ? "修改客户成功" : "新增客户成功") : message.trim();
            }
        };

        submitTask.setOnSucceeded(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(editing ? "保存成功" : "新增成功");
            alert.setHeaderText(editing ? "客户信息已更新" : "客户已新增");
            alert.setContentText(submitTask.getValue());
            alert.showAndWait();
            if (!editing && onCreatedForBusiness != null) {
                onCreatedForBusiness.accept(formData.customerCode, formData.customerName);
            }
            loadCustomerPage(editing ? currentPage : 1);
            if (!editing && returnButton.isVisible()) {
                statusLabel.setText("客户已新增，可点击“" + returnButton.getText() + "”继续填写业务单据。");
            }
        });

        submitTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(submitTask.getException())));

        Thread thread = new Thread(submitTask, editing ? "desktop-customer-update" : "desktop-customer-add");
        thread.setDaemon(true);
        thread.start();
    }

    private void handleDeleteCustomer(CustomerRow row) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("删除客户");
        confirmAlert.setHeaderText("确定要删除客户吗？");
        confirmAlert.setContentText("客户名称：" + defaultText(row.getCustomerName()));

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        setLoadingState(true, "正在删除客户...");

        Task<String> deleteTask = new Task<>() {
            @Override
            protected String call() {
                ApiResponse<String> response = apiClient.delete(CustomerApi.DELETE_PREFIX + row.getId(), String.class);
                if (response == null || !response.isSuccess()) {
                    String message = response == null ? null : response.getMessage();
                    throw new ApiException(isBlank(message) ? "删除客户失败" : message);
                }
                String message = response.getData();
                if (isBlank(message)) {
                    message = response.getMessage();
                }
                return isBlank(message) ? "删除客户成功" : message.trim();
            }
        };

        deleteTask.setOnSucceeded(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("删除成功");
            alert.setHeaderText("客户已删除");
            alert.setContentText(deleteTask.getValue());
            alert.showAndWait();

            int targetPage = currentPage;
            if (tableView.getItems().size() == 1 && currentPage > 1) {
                targetPage = currentPage - 1;
            }
            loadCustomerPage(targetPage);
        });

        deleteTask.setOnFailed(event -> setLoadingState(false, resolveErrorMessage(deleteTask.getException())));

        Thread thread = new Thread(deleteTask, "desktop-customer-delete");
        thread.setDaemon(true);
        thread.start();
    }

    private CustomerAddRequest buildAddRequest(CustomerFormData formData) {
        CustomerAddRequest request = new CustomerAddRequest();
        request.customerCode = formData.customerCode;
        request.customerName = formData.customerName;
        request.contactPerson = formData.contactPerson;
        request.phone = formData.phone;
        request.address = formData.address;
        request.remark = formData.remark;
        return request;
    }

    private CustomerUpdateRequest buildUpdateRequest(CustomerFormData formData) {
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.id = formData.id;
        request.customerCode = formData.customerCode;
        request.customerName = formData.customerName;
        request.contactPerson = formData.contactPerson;
        request.phone = formData.phone;
        request.address = formData.address;
        request.remark = formData.remark;
        request.status = formData.status;
        return request;
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

    private void exportCustomerList() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("导出客户列表");
        fileChooser.setInitialFileName("客户列表.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel 文件", "*.xlsx"));

        java.io.File targetFile = fileChooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (targetFile == null) {
            statusLabel.setText("已取消导出。");
            return;
        }

        setExportState(true, "正在导出客户列表...");

        Task<Void> exportTask = new Task<>() {
            @Override
            protected Void call() throws IOException {
                byte[] content = apiClient.download(CustomerApi.EXPORT, buildExportParams());
                Files.write(Path.of(targetFile.toURI()), content);
                return null;
            }
        };

        exportTask.setOnSucceeded(event -> setExportState(false, "客户列表导出成功：" + targetFile.getName()));
        exportTask.setOnFailed(event -> setExportState(false, resolveErrorMessage(exportTask.getException())));

        Thread thread = new Thread(exportTask, "desktop-customer-export");
        thread.setDaemon(true);
        thread.start();
    }

    private Map<String, Object> buildExportParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("customerCode", customerCodeField.getText());
        params.put("customerName", customerNameField.getText());
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
            return "客户操作失败，请稍后重试。";
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @FunctionalInterface
    private interface ValueProvider {
        String get(CustomerRow row);
    }

    private static final class CustomerFormData {
        private Long id;
        private String customerCode;
        private String customerName;
        private String contactPerson;
        private String phone;
        private String address;
        private String remark;
        private Integer status;
    }

    private static final class CustomerAddRequest {
        private String customerCode;
        private String customerName;
        private String contactPerson;
        private String phone;
        private String address;
        private String remark;

        public String getCustomerCode() {
            return customerCode;
        }

        public void setCustomerCode(String customerCode) {
            this.customerCode = customerCode;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
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
    }

    private static final class CustomerUpdateRequest {
        private Long id;
        private String customerCode;
        private String customerName;
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

        public String getCustomerCode() {
            return customerCode;
        }

        public void setCustomerCode(String customerCode) {
            this.customerCode = customerCode;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
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
