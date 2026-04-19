package com.yocaihua.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SupplierUpdateDTO {

    @NotNull(message = "供应商ID不能为空")
    private Long id;

    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;

    @NotBlank(message = "供应商名称不能为空")
    private String supplierName;

    private String contactPerson;

    private String phone;

    private String address;

    private String customFieldsJson;

    private String remark;

    @NotNull(message = "状态不能为空")
    private Integer status;
}
