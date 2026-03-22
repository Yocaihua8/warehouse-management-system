package com.yocaihua.wms.dto;

import lombok.Data;

@Data
public class CustomerUpdateDTO {

    private Long id;

    private String customerName;

    private String contactPerson;

    private String phone;

    private String address;

    private String remark;
}