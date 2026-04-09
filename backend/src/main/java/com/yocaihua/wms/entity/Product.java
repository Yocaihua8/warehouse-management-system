package com.yocaihua.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Product {

    private Long id;

    private String productCode;

    private String productName;

    private String specification;

    private String unit;

    private String category;

    private BigDecimal salePrice;

    private String customFieldsJson;

    private String remark;

    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
