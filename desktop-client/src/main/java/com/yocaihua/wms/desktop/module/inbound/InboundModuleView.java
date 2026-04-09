package com.yocaihua.wms.desktop.module.inbound;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.api.OrderCreatedData;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class InboundModuleView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final Runnable onGoSupplierManagement;
    private final java.util.function.Consumer<String> onDraftStateChanged;
    private final boolean openCreateInitially;
    private final String initialSupplierCode;
    private final String initialSupplierName;
    private final StackPane root;
    private InboundOrderListView listView;
    private OrderCreatedData pendingCreatedOrder;

    public InboundModuleView(StartupContext startupContext,
                             ApiClient apiClient,
                             Runnable onGoSupplierManagement,
                             java.util.function.Consumer<String> onDraftStateChanged,
                             boolean openCreateInitially,
                             String initialSupplierCode,
                             String initialSupplierName) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.onGoSupplierManagement = onGoSupplierManagement;
        this.onDraftStateChanged = onDraftStateChanged;
        this.openCreateInitially = openCreateInitially;
        this.initialSupplierCode = initialSupplierCode;
        this.initialSupplierName = initialSupplierName;
        this.root = new StackPane();
        if (openCreateInitially) {
            showCreate();
        } else {
            showList(false);
        }
    }

    public Parent getRoot() {
        return root;
    }

    private void showList(boolean reload) {
        notifyDraftState("未打开");
        if (listView == null) {
            listView = new InboundOrderListView(startupContext, apiClient, this::showDetail, this::showCreate);
        } else if (pendingCreatedOrder != null) {
            listView.revealCreatedOrder(pendingCreatedOrder.getId(), pendingCreatedOrder.getOrderNo());
            pendingCreatedOrder = null;
        } else if (reload) {
            listView.reloadCurrentPage();
        }
        root.getChildren().setAll(listView.getRoot());
    }

    private void showCreate() {
        notifyDraftState("编辑中（手工入库草稿）");
        root.getChildren().setAll(new InboundOrderCreateView(
                startupContext,
                apiClient,
                () -> showList(false),
                this::showCreatedDetail,
                onGoSupplierManagement,
                initialSupplierCode,
                initialSupplierName
        ).getRoot());
    }

    private void showDetail(Long orderId) {
        notifyDraftState("未打开");
        root.getChildren().setAll(new InboundOrderDetailView(startupContext, apiClient, orderId, () -> showList(true)).getRoot());
    }

    private void showCreatedDetail(OrderCreatedData createdOrder) {
        this.pendingCreatedOrder = createdOrder;
        if (createdOrder == null || createdOrder.getId() == null) {
            showList(true);
            return;
        }
        showDetail(createdOrder.getId());
    }

    private void notifyDraftState(String statusText) {
        if (onDraftStateChanged != null) {
            onDraftStateChanged.accept(statusText);
        }
    }
}
