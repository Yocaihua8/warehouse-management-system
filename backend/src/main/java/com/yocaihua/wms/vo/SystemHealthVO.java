package com.yocaihua.wms.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemHealthVO {

    private String overallStatus;

    private String appStatus;

    private String appMessage;

    private String databaseStatus;

    private String databaseMessage;

    private String aiStatus;

    private String aiMessage;

    private String aiBaseUrl;

    private LocalDateTime checkedAt;
}
