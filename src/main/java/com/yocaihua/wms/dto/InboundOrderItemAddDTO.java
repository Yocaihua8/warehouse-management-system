package com.yocaihua.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InboundOrderItemAddDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "入库数量不能为空")
    @Positive(message = "入库数量必须大于0")
    private Integer quantity;

    @NotNull(message = "入库单价不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "入库单价不能小于0")
    private BigDecimal unitPrice;

    private String remark;
}