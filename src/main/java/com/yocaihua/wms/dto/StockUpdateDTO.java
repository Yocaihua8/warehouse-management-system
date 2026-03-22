package com.yocaihua.wms.dto;

import lombok.Data;

@Data
public class StockUpdateDTO {

    private Long productId;

    private Integer quantity;

    private Integer warningQuantity;
}