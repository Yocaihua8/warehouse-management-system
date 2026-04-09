package com.yocaihua.wms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerAddDTO {

    @NotBlank(message = "客户编码不能为空")
    @Size(max = 50, message = "客户编码长度不能超过50")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 100, message = "客户名称长度不能超过100")
    private String customerName;

    @Size(max = 50, message = "联系人长度不能超过50")
    private String contactPerson;

    @Size(max = 30, message = "联系电话长度不能超过30")
    private String phone;

    @Size(max = 255, message = "地址长度不能超过255")
    private String address;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;

    private Integer status;
}
