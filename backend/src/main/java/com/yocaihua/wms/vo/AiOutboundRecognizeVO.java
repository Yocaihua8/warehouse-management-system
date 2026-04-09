package com.yocaihua.wms.vo;

import lombok.Data;

import java.util.List;

@Data
public class AiOutboundRecognizeVO {

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

    private String confirmedOrderNo;

    private List<AiOutboundRecognizeItemVO> itemList;
}
