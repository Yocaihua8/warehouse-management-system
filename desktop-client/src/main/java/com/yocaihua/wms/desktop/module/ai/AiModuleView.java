package com.yocaihua.wms.desktop.module.ai;

import com.yocaihua.wms.desktop.api.ApiClient;
import com.yocaihua.wms.desktop.bootstrap.StartupContext;
import com.yocaihua.wms.desktop.module.inbound.InboundOrderDetailView;
import com.yocaihua.wms.desktop.module.outbound.OutboundOrderDetailView;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AiModuleView {

    private final StartupContext startupContext;
    private final ApiClient apiClient;
    private final Consumer<AiOutboundRecognizeData> onGoCustomerManagementFromOutboundConfirm;
    private final BiConsumer<AiOutboundRecognizeData, Integer> onGoProductManagementFromOutboundConfirm;
    private final StackPane root;
    private AiInboundRecordListView listView;
    private String pendingUploadResultNotice;
    private AiOutboundRecognizeData initialOutboundConfirmData;
    private final String initialOutboundCustomerSearchCode;
    private final String initialOutboundCustomerSearchName;
    private final Integer initialOutboundProductEditorIndex;
    private final String initialOutboundProductSearchCode;
    private final String initialOutboundProductSearchName;

    public AiModuleView(StartupContext startupContext, ApiClient apiClient) {
        this(startupContext, apiClient, null, null, null, null, null, null, null, null);
    }

    public AiModuleView(
            StartupContext startupContext,
            ApiClient apiClient,
            Consumer<AiOutboundRecognizeData> onGoCustomerManagementFromOutboundConfirm,
            BiConsumer<AiOutboundRecognizeData, Integer> onGoProductManagementFromOutboundConfirm,
            AiOutboundRecognizeData initialOutboundConfirmData,
            String initialOutboundCustomerSearchCode,
            String initialOutboundCustomerSearchName,
            Integer initialOutboundProductEditorIndex,
            String initialOutboundProductSearchCode,
            String initialOutboundProductSearchName
    ) {
        this.startupContext = startupContext;
        this.apiClient = apiClient;
        this.onGoCustomerManagementFromOutboundConfirm = onGoCustomerManagementFromOutboundConfirm;
        this.onGoProductManagementFromOutboundConfirm = onGoProductManagementFromOutboundConfirm;
        this.root = new StackPane();
        this.initialOutboundConfirmData = initialOutboundConfirmData;
        this.initialOutboundCustomerSearchCode = initialOutboundCustomerSearchCode;
        this.initialOutboundCustomerSearchName = initialOutboundCustomerSearchName;
        this.initialOutboundProductEditorIndex = initialOutboundProductEditorIndex;
        this.initialOutboundProductSearchCode = initialOutboundProductSearchCode;
        this.initialOutboundProductSearchName = initialOutboundProductSearchName;
        if (initialOutboundConfirmData != null) {
            showOutboundConfirm(
                    initialOutboundConfirmData,
                    initialOutboundCustomerSearchCode,
                    initialOutboundCustomerSearchName,
                    initialOutboundProductEditorIndex,
                    initialOutboundProductSearchCode,
                    initialOutboundProductSearchName
            );
        } else {
            showList(false);
        }
    }

    public Parent getRoot() {
        return root;
    }

    private void showList(boolean reload) {
        if (listView == null) {
            listView = new AiInboundRecordListView(
                    startupContext,
                    apiClient,
                    this::showDetail,
                    this::showConfirm,
                    this::showUploadedDetail,
                    this::showOutboundConfirm
            );
        } else if (reload) {
            listView.reloadRecords();
        }
        root.getChildren().setAll(listView.getRoot());
    }

    private void showDetail(Long recordId) {
        String uploadNotice = consumePendingUploadResultNotice(recordId);
        root.getChildren().setAll(
                new AiInboundRecordDetailView(
                        startupContext,
                        apiClient,
                        recordId,
                        () -> showList(false),
                        this::showConfirm,
                        orderId -> showInboundOrderDetail(orderId, recordId),
                        uploadNotice
                ).getRoot()
        );
    }

    private void showUploadedDetail(AiInboundRecordDetailData detailData) {
        if (detailData == null || detailData.getRecordId() == null) {
            showList(true);
            return;
        }
        pendingUploadResultNotice = buildUploadResultNotice(detailData);
        showDetail(detailData.getRecordId());
    }

    private void showConfirm(Long recordId) {
        root.getChildren().setAll(
                new AiInboundConfirmView(
                        startupContext,
                        apiClient,
                        recordId,
                        () -> showDetail(recordId),
                        orderId -> showInboundOrderDetail(orderId, recordId)
                ).getRoot()
        );
    }

    private void showOutboundConfirm(AiOutboundRecognizeData detailData) {
        showOutboundConfirm(detailData, null, null, null, null, null);
    }

    private void showOutboundConfirm(
            AiOutboundRecognizeData detailData,
            String initialCustomerCode,
            String initialCustomerName,
            Integer initialProductEditorIndex,
            String initialProductCode,
            String initialProductName
    ) {
        if (detailData == null || detailData.getRecordId() == null) {
            showList(true);
            return;
        }
        initialOutboundConfirmData = detailData;
        root.getChildren().setAll(
                new AiOutboundConfirmView(
                        startupContext,
                        apiClient,
                        detailData,
                        () -> showList(true),
                        this::showOutboundOrderDetail,
                        () -> handleGoCustomerManagement(detailData),
                        editorIndex -> handleGoProductManagement(detailData, editorIndex),
                        initialCustomerCode,
                        initialCustomerName,
                        initialProductEditorIndex,
                        initialProductCode,
                        initialProductName
                ).getRoot()
        );
    }

    private void handleGoCustomerManagement(AiOutboundRecognizeData detailData) {
        if (onGoCustomerManagementFromOutboundConfirm != null) {
            onGoCustomerManagementFromOutboundConfirm.accept(detailData);
        }
    }

    private void handleGoProductManagement(AiOutboundRecognizeData detailData, Integer editorIndex) {
        if (onGoProductManagementFromOutboundConfirm != null) {
            onGoProductManagementFromOutboundConfirm.accept(detailData, editorIndex);
        }
    }

    private void showInboundOrderDetail(Long orderId, Long recordId) {
        if (orderId == null) {
            showDetail(recordId);
            return;
        }
        root.getChildren().setAll(new InboundOrderDetailView(startupContext, apiClient, orderId, () -> showDetail(recordId)).getRoot());
    }

    private void showOutboundOrderDetail(Long orderId) {
        if (orderId == null) {
            showList(true);
            return;
        }
        root.getChildren().setAll(new OutboundOrderDetailView(startupContext, apiClient, orderId, () -> showList(true)).getRoot());
    }

    private String buildUploadResultNotice(AiInboundRecordDetailData detailData) {
        int itemCount = detailData.getItemList() == null ? 0 : detailData.getItemList().size();
        String fileName = defaultText(detailData.getSourceFileName(), "未命名文件");
        String recognitionStatus = defaultText(detailData.getRecognitionStatusText(), "未知");
        String warningText = defaultText(detailData.getWarningText(), "-");
        if ("-".equals(warningText)) {
            return "这是本次刚上传的识别结果：文件「" + fileName + "」，识别状态「" + recognitionStatus + "」，识别明细 " + itemCount + " 条。";
        }
        return "这是本次刚上传的识别结果：文件「" + fileName + "」，识别状态「" + recognitionStatus + "」，识别明细 "
                + itemCount + " 条。当前提示：" + warningText;
    }

    private String consumePendingUploadResultNotice(Long recordId) {
        if (recordId == null) {
            pendingUploadResultNotice = null;
            return null;
        }
        String notice = pendingUploadResultNotice;
        pendingUploadResultNotice = null;
        return notice;
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }
}
