package com.yocaihua.wms.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Customer {

    private Long id;

    private String customerCode;

    private String customerName;

    private String contactPerson;

    private String phone;

    private String address;

    private String customFieldsJson;

    private String remark;

    private Integer status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
