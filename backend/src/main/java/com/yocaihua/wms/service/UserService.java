package com.yocaihua.wms.service;

import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.dto.UserAddDTO;
import com.yocaihua.wms.dto.UserResetPasswordDTO;
import com.yocaihua.wms.dto.UserUpdateDTO;
import com.yocaihua.wms.entity.User;
import com.yocaihua.wms.dto.LoginDTO;

import java.util.List;
import java.util.Map;

public interface UserService {

    Map<String, Object> login(LoginDTO loginDTO);

    void logout(String token);

    Map<String, Object> getCurrentUser();

    PageResult<User> getUserPage(String username, String nickname, Integer pageNum, Integer pageSize);

    String addUser(UserAddDTO userAddDTO);

    String updateUser(UserUpdateDTO userUpdateDTO);

    String resetPassword(UserResetPasswordDTO userResetPasswordDTO);

    String deleteUser(Long id);
}
