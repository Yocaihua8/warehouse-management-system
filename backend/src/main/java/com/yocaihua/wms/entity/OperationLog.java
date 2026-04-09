package com.yocaihua.wms.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLog {

    private Long id;

    private String actionType;

    private String moduleName;

    private String bizType;

    private Long bizId;

    private String bizNo;

    private String operatorName;

    private String resultStatus;

    private String message;

    private LocalDateTime createdTime;
}

