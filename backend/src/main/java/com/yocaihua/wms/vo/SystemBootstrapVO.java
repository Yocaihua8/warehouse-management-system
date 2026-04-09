package com.yocaihua.wms.vo;

import lombok.Data;

@Data
public class SystemBootstrapVO {

    private String appName;

    private String appDisplayName;

    private String appVersion;

    private Boolean desktopSupported;

    private Boolean authRequired;

    private Integer sessionTimeoutMinutes;

    private Boolean aiEnabled;

    private String aiBaseUrl;

    private String healthPath;

    private String loginPath;

    private String logoutPath;

    private String currentUserPath;
}
