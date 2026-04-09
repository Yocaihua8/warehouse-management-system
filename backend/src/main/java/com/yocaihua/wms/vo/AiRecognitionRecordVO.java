package com.yocaihua.wms.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AiRecognitionRecordVO {

    private Long id;

    private String taskNo;

    private String sourceFileName;

    private String supplierName;

    private String supplierMatchStatus;

    private String customerName;

    private String customerMatchStatus;

    private String recognitionStatus;

    private Long confirmedOrderId;

    private String confirmedOrderNo;

    private LocalDateTime createdTime;
}
