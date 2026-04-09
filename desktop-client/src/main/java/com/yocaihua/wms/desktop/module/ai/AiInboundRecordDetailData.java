package com.yocaihua.wms.desktop.module.ai;

import java.util.List;

public class AiInboundRecordDetailData {

    private Long recordId;
    private String taskNo;
    private String docType;
    private String recognitionStatus;
    private String sourceFileName;
    private String supplierName;
    private Long matchedSupplierId;
    private String supplierMatchStatus;
    private String rawText;
    private String warningsJson;
    private List<String> warnings;
    private Long confirmedOrderId;
    private String confirmedOrderNo;
    private List<AiInboundRecordItemRow> itemList;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getTaskNo() {
        return taskNo;
    }

    public void setTaskNo(String taskNo) {
        this.taskNo = taskNo;
    }

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getRecognitionStatus() {
        return recognitionStatus;
    }

    public void setRecognitionStatus(String recognitionStatus) {
        this.recognitionStatus = recognitionStatus;
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

    public Long getMatchedSupplierId() {
        return matchedSupplierId;
    }

    public void setMatchedSupplierId(Long matchedSupplierId) {
        this.matchedSupplierId = matchedSupplierId;
    }

    public String getSupplierMatchStatus() {
        return supplierMatchStatus;
    }

    public void setSupplierMatchStatus(String supplierMatchStatus) {
        this.supplierMatchStatus = supplierMatchStatus;
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
    }

    public String getWarningsJson() {
        return warningsJson;
    }

    public void setWarningsJson(String warningsJson) {
        this.warningsJson = warningsJson;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
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

    public List<AiInboundRecordItemRow> getItemList() {
        return itemList;
    }

    public void setItemList(List<AiInboundRecordItemRow> itemList) {
        this.itemList = itemList;
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

    public String getWarningText() {
        if (warnings != null && !warnings.isEmpty()) {
            return String.join("；", warnings);
        }
        if (warningsJson == null || warningsJson.trim().isEmpty()) {
            return "-";
        }
        return warningsJson.trim();
    }

    public String getMatchedSupplierIdText() {
        return matchedSupplierId == null ? "-" : String.valueOf(matchedSupplierId);
    }

    public String getConfirmedOrderIdText() {
        return confirmedOrderId == null ? "-" : String.valueOf(confirmedOrderId);
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
