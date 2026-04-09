package com.yocaihua.wms.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiOutboundRecognizeItemVO {

    private Integer lineNo;

    private String productName;

    private String specification;

    private String unit;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal amount;

    private Long matchedProductId;

    private String matchStatus;

    private String remark;
}
