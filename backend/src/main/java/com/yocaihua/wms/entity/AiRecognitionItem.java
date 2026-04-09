package com.yocaihua.wms.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiRecognitionItem {

    private Long id;

    private Long recordId;

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

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}