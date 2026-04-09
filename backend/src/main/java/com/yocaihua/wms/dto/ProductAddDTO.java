package com.yocaihua.wms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductAddDTO {

    private Long id;

    @NotBlank(message = "商品编码不能为空")
    private String productCode;

    @NotBlank(message = "商品名称不能为空")
    private String productName;

    private String specification;

    private String unit;

    private String category;

    @NotNull(message = "销售单价不能为空")
    @DecimalMin(value = "0.00", inclusive = true, message = "销售单价不能小于0")
    private BigDecimal salePrice;

    private String customFieldsJson;

    private String remark;

    private Integer status;
}
