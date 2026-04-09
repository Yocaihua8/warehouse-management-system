package com.yocaihua.wms.desktop.module.ai;

public class AiInboundRecordRow {

    private Long id;
    private String taskNo;
    private String sourceFileName;
    private String supplierName;
    private String supplierMatchStatus;
    private String recognitionStatus;
    private Long confirmedOrderId;
    private String confirmedOrderNo;
    private String createdTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierMatchStatus() {
        return supplierMatchStatus;
    }

    public void setSupplierMatchStatus(String supplierMatchStatus) {
        this.supplierMatchStatus = supplierMatchStatus;
    }

    public String getRecognitionStatus() {
        return recognitionStatus;
    }

    public void setRecognitionStatus(String recognitionStatus) {
        this.recognitionStatus = recognitionStatus;
    }

    public Long getConfirmedOrderId() {
        return confirmedOrderId;
    }

    public void setConfirmedOrderId(Long confirmedOrderId) {
        this.confirmedOrderId = confirmedOrderId;
    }

    public String getConfirmedOrderNo() {
        return confirmedOrderNo;
    }

    public void setConfirmedOrderNo(String confirmedOrderNo) {
        this.confirmedOrderNo = confirmedOrderNo;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getSupplierMatchStatusText() {
        if ("matched_exact".equalsIgnoreCase(supplierMatchStatus)) {
            return "精确匹配";
        }
        if ("matched_fuzzy".equalsIgnoreCase(supplierMatchStatus)) {
            return "模糊匹配";
        }
        if ("manual_confirmed".equalsIgnoreCase(supplierMatchStatus)) {
            return "人工确认";
        }
        if ("manual_selected".equalsIgnoreCase(supplierMatchStatus)) {
            return "人工选择";
        }
        if ("manual_created".equalsIgnoreCase(supplierMatchStatus)) {
            return "新建后回填";
        }
        if ("unmatched".equalsIgnoreCase(supplierMatchStatus)) {
            return "未匹配";
        }
        return supplierMatchStatus == null || supplierMatchStatus.trim().isEmpty() ? "-" : supplierMatchStatus.trim();
    }

    public String getRecognitionStatusText() {
        if ("confirmed".equalsIgnoreCase(recognitionStatus)) {
            return "已确认";
        }
        if ("success".equalsIgnoreCase(recognitionStatus)) {
            return "待确认";
        }
        if ("failed".equalsIgnoreCase(recognitionStatus)) {
            return "失败";
        }
        if ("pending".equalsIgnoreCase(recognitionStatus)) {
            return "待处理";
        }
        return recognitionStatus == null || recognitionStatus.trim().isEmpty() ? "-" : recognitionStatus.trim();
    }

    public String getConfirmedOrderNoText() {
        if (confirmedOrderId == null) {
            return "-";
        }
        if (confirmedOrderNo != null && !confirmedOrderNo.trim().isEmpty()) {
            return confirmedOrderNo.trim();
        }
        return "ID:" + confirmedOrderId;
    }
}
