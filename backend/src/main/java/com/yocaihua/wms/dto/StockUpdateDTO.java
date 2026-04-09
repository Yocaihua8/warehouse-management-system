package com.yocaihua.wms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockUpdateDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能小于0")
    private Integer quantity;

    @NotNull(message = "预警库存不能为空")
    @Min(value = 0, message = "预警库存不能小于0")
    private Integer warningQuantity;

    private String reason;
}
