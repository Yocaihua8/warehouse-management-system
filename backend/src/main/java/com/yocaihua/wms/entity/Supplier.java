package com.yocaihua.wms.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Supplier {

    private Long id;

    private String supplierCode;

    private String supplierName;

    private String contactPerson;

    private String phone;

    private String address;

    private String remark;

    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
