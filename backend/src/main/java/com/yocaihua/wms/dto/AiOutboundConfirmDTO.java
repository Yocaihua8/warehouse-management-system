package com.yocaihua.wms.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiOutboundConfirmDTO {

    private Long recordId;

    private Long customerId;

    private String customerName;

    private String rawText;

    private String remark;

    private List<AiOutboundConfirmItemDTO> itemList;
}
