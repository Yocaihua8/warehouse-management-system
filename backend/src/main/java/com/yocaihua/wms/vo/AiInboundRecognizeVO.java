package com.yocaihua.wms.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiInboundRecognizeVO {

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

    private List<AiInboundRecognizeItemVO> itemList;

}
