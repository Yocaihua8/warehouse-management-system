package com.yocaihua.wms.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiInboundConfirmDTO {

    private Long recordId;

    private Long supplierId;

    private String supplierName;

    private String rawText;

    private String remark;

    private List<AiInboundConfirmItemDTO> itemList;
}
