package com.yocaihua.wms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateDTO {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    private String password;

    private String nickname;

    private String role;

    private Integer status;
}
