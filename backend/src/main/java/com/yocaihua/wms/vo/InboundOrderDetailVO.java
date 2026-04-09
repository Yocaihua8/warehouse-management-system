package com.yocaihua.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InboundOrderDetailVO {

    private Long id;

    private String orderNo;

    private Long supplierId;

    private String supplierName;

    private BigDecimal totalAmount;

    private Integer orderStatus;

    private String sourceType;

    private Long aiRecordId;

    private String aiTaskNo;

    private String aiSourceFileName;

    private String remark;

    private LocalDateTime createdTime;

    private List<InboundOrderItemVO> itemList;
}
