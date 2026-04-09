package com.yocaihua.wms.desktop.module.ai;

import java.util.List;

public class AiOutboundRecognizeData {

    private Long recordId;
    private String taskNo;
    private String docType;
    private String recognitionStatus;
    private String sourceFileName;
    private String customerName;
    private Long matchedCustomerId;
    private String customerMatchStatus;
    private String rawText;
    private String warningsJson;
    private List<String> warnings;
    private Long confirmedOrderId;
    private List<AiOutboundRecognizeItemRow> itemList;

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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getMatchedCustomerId() {
        return matchedCustomerId;
    }

    public void setMatchedCustomerId(Long matchedCustomerId) {
        this.matchedCustomerId = matchedCustomerId;
    }

    public String getCustomerMatchStatus() {
        return customerMatchStatus;
    }

    public void setCustomerMatchStatus(String customerMatchStatus) {
        this.customerMatchStatus = customerMatchStatus;
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

    public List<AiOutboundRecognizeItemRow> getItemList() {
        return itemList;
    }

    public void setItemList(List<AiOutboundRecognizeItemRow> itemList) {
        this.itemList = itemList;
    }

    public String getCustomerMatchStatusText() {
        if ("matched_exact".equalsIgnoreCase(customerMatchStatus)) {
            return "精确匹配";
        }
        if ("matched_fuzzy".equalsIgnoreCase(customerMatchStatus)) {
            return "模糊匹配";
        }
        if ("manual_confirmed".equalsIgnoreCase(customerMatchStatus)) {
            return "人工确认";
        }
        if ("manual_selected".equalsIgnoreCase(customerMatchStatus)) {
            return "人工选择";
        }
        if ("manual_created".equalsIgnoreCase(customerMatchStatus)) {
            return "新建后回填";
        }
        if ("unmatched".equalsIgnoreCase(customerMatchStatus)) {
            return "未匹配";
        }
        return customerMatchStatus == null || customerMatchStatus.trim().isEmpty() ? "-" : customerMatchStatus.trim();
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

    public String getMatchedCustomerIdText() {
        return matchedCustomerId == null ? "-" : String.valueOf(matchedCustomerId);
    }

    public String getConfirmedOrderIdText() {
        return confirmedOrderId == null ? "-" : String.valueOf(confirmedOrderId);
    }
}
