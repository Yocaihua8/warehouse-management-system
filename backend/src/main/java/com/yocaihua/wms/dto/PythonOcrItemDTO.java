package com.yocaihua.wms.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PythonOcrItemDTO {
    private Integer lineNo;
    private String productName;
    private String specification;
    private String unit;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
}