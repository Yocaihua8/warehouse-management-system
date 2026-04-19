package com.yocaihua.wms.controller;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.Result;
import com.yocaihua.wms.dto.LoginDTO;
import com.yocaihua.wms.dto.UserAddDTO;
import com.yocaihua.wms.dto.UserResetPasswordDTO;
import com.yocaihua.wms.dto.UserUpdateDTO;
import com.yocaihua.wms.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import com.yocaihua.wms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@Tag(name = "用户认证", description = "登录、登出、获取当前用户信息")
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "用户登录", description = "返回 token，后续请求在 Header 中携带 `token` 字段")
    @PostMapping("/user/login")
    public Result<Map<String, Object>> login(@RequestBody @Valid LoginDTO loginDTO) {
        return Result.success(userService.login(loginDTO));
    }

    @Operation(summary = "退出登录", description = "使当前 token 失效")
    @SecurityRequirement(name = "token")
    @PostMapping("/user/logout")
    public Result<String> logout(HttpServletRequest request) {
        userService.logout(request.getHeader("token"));
        return Result.success("退出登录成功");
    }

    @Operation(summary = "获取当前登录用户信息")
    @SecurityRequirement(name = "token")
    @PostMapping("/user/me")
    public Result<Map<String, Object>> currentUser() {
        return Result.success(userService.getCurrentUser());
    }

    @Operation(summary = "用户列表（管理员）")
    @SecurityRequirement(name = "token")
    @GetMapping("/user/list")
    public Result<PageResult<User>> getUserList(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(userService.getUserPage(username, nickname, pageNum, pageSize));
    }

    @Operation(summary = "新增用户（管理员）")
    @SecurityRequirement(name = "token")
    @PostMapping("/user/add")
    public Result<String> addUser(@RequestBody @Valid UserAddDTO userAddDTO) {
        return Result.success(userService.addUser(userAddDTO));
    }

    @Operation(summary = "修改用户（管理员）")
    @SecurityRequirement(name = "token")
    @PutMapping("/user/update")
    public Result<String> updateUser(@RequestBody @Valid UserUpdateDTO userUpdateDTO) {
        return Result.success(userService.updateUser(userUpdateDTO));
    }

    @Operation(summary = "重置用户密码（管理员）")
    @SecurityRequirement(name = "token")
    @PutMapping("/user/reset-password")
    public Result<String> resetPassword(@RequestBody @Valid UserResetPasswordDTO userResetPasswordDTO) {
        return Result.success(userService.resetPassword(userResetPasswordDTO));
    }

    @Operation(summary = "删除用户（管理员）")
    @SecurityRequirement(name = "token")
    @DeleteMapping("/user/delete/{id}")
    public Result<String> deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        return Result.success(userService.deleteUser(id));
    }
}
