package com.yocaihua.wms.dto;

import lombok.Data;

@Data
public class CustomerAddDTO {

    private String customerCode;

    private String customerName;

    private String contactPerson;

    private String phone;

    private String address;

    private String remark;
}