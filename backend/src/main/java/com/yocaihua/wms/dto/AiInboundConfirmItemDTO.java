package com.yocaihua.wms.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiInboundConfirmItemDTO {

    private Integer lineNo;

    private String productName;

    private String specification;

    private String unit;

    private Long matchedProductId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private String remark;
}
