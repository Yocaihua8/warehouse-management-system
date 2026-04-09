package com.yocaihua.wms.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiRecognitionRecord {

    private Long id;

    private String taskNo;

    private String docType;

    private String sourceFileName;

    private String sourceFilePath;

    private String recognitionStatus;

    private String supplierName;

    private String rawText;

    private String warningsJson;

    private String resultJson;

    private String errorMessage;

    private Long confirmedOrderId;

    private String createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}