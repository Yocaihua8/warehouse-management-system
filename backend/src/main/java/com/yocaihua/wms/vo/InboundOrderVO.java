package com.yocaihua.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InboundOrderVO {

    private Long id;

    private String orderNo;

    private Long supplierId;

    private String supplierName;

    private BigDecimal totalAmount;

    private Integer orderStatus;

    private String sourceType;

    private String remark;

    private LocalDateTime createdTime;
}
