package com.yocaihua.wms.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InboundDetailVO {
    private Long id;
    private String inboundNo;
    private Long supplierId;
    private String supplierName;
    private LocalDateTime inboundTime;
    private Integer status;
    private String remark;
    private BigDecimal totalAmount;
    private List<InboundItemVO> itemList;
}
