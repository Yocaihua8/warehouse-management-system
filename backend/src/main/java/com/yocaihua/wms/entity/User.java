package com.yocaihua.wms.entity;

import lombok.Data;

@Data
public class User {

    private Long id;

    private String username;

    private String password;

    private String nickname;

    private Integer status;

    private String role;
}
