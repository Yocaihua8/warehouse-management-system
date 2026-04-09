package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.CurrentUserContext;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.TokenStore;
import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.OperationLogActionConstant;
import com.yocaihua.wms.common.UserRoleConstant;
import com.yocaihua.wms.dto.LoginDTO;
import com.yocaihua.wms.dto.UserAddDTO;
import com.yocaihua.wms.dto.UserUpdateDTO;
import com.yocaihua.wms.entity.User;
import com.yocaihua.wms.mapper.UserMapper;
import com.yocaihua.wms.service.OperationLogService;
import com.yocaihua.wms.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private static final int MAX_PAGE_SIZE = 200;

    private final UserMapper userMapper;
    private final TokenStore tokenStore;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService operationLogService;

    public UserServiceImpl(UserMapper userMapper,
                           TokenStore tokenStore,
                           PasswordEncoder passwordEncoder,
                           OperationLogService operationLogService) {
        this.userMapper = userMapper;
        this.tokenStore = tokenStore;
        this.passwordEncoder = passwordEncoder;
        this.operationLogService = operationLogService;
    }

    @Override
    public Map<String, Object> login(LoginDTO loginDTO) {
        User user = userMapper.selectByUsername(loginDTO.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("用户已被禁用");
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        String role = resolveRole(user);
        tokenStore.saveToken(token, user.getUsername(), role);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("role", role);

        operationLogService.recordSuccess(
                OperationLogActionConstant.LOGIN_SUCCESS,
                "用户认证",
                "USER",
                user.getId(),
                null,
                user.getUsername(),
                "用户登录成功"
        );

        return result;
    }

    @Override
    public void logout(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        tokenStore.removeToken(token.trim());
    }

    @Override
    public Map<String, Object> getCurrentUser() {
        String username = CurrentUserContext.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new BusinessException("未登录或登录已失效");
        }

        User user = userMapper.selectByUsername(username.trim());
        if (user == null) {
            throw new BusinessException("当前用户不存在");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("用户已被禁用");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("status", user.getStatus());
        result.put("role", resolveRole(user));
        return result;
    }

    @Override
    public PageResult<User> getUserPage(String username, String nickname, Integer pageNum, Integer pageSize) {
        ensureAdminPermission("查询用户列表");

        String normalizedUsername = normalizeKeyword(username);
        String normalizedNickname = normalizeKeyword(nickname);
        int currentPageNum = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentPageSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, MAX_PAGE_SIZE);
        int offset = (currentPageNum - 1) * currentPageSize;

        Long total = userMapper.count(normalizedUsername, normalizedNickname);
        List<User> list = userMapper.selectPage(normalizedUsername, normalizedNickname, offset, currentPageSize);
        return new PageResult<>(total, currentPageNum, currentPageSize, list);
    }

    @Override
    public String addUser(UserAddDTO userAddDTO) {
        ensureAdminPermission("新增用户");

        String normalizedUsername = normalizeRequired(userAddDTO.getUsername(), "用户名不能为空");
        String normalizedPassword = normalizeRequired(userAddDTO.getPassword(), "密码不能为空");
        if (userMapper.selectByUsername(normalizedUsername) != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        user.setUsername(normalizedUsername);
        user.setPassword(passwordEncoder.encode(normalizedPassword));
        user.setNickname(normalizeOptional(userAddDTO.getNickname()));
        user.setStatus(normalizeStatus(userAddDTO.getStatus()));
        user.setRole(UserRoleConstant.normalize(userAddDTO.getRole()));

        int rows = userMapper.insert(user);
        if (rows <= 0) {
            throw new BusinessException("新增用户失败");
        }
        return "新增用户成功";
    }

    @Override
    public String updateUser(UserUpdateDTO userUpdateDTO) {
        ensureAdminPermission("编辑用户");

        User existing = userMapper.selectById(userUpdateDTO.getId());
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }

        int normalizedStatus = normalizeStatus(userUpdateDTO.getStatus());
        String normalizedRole = UserRoleConstant.normalize(userUpdateDTO.getRole());

        if ("admin".equalsIgnoreCase(existing.getUsername()) && normalizedStatus == 0) {
            throw new BusinessException("默认管理员不能被禁用");
        }

        String currentUsername = CurrentUserContext.getUsername();
        if (currentUsername != null
                && currentUsername.equalsIgnoreCase(existing.getUsername())
                && normalizedStatus == 0) {
            throw new BusinessException("不能禁用当前登录用户");
        }

        User user = new User();
        user.setId(existing.getId());
        user.setNickname(normalizeOptional(userUpdateDTO.getNickname()));
        user.setStatus(normalizedStatus);
        user.setRole(normalizedRole);

        String rawPassword = normalizeOptional(userUpdateDTO.getPassword());
        if (rawPassword != null && !rawPassword.isEmpty()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }

        int rows = userMapper.updateById(user);
        if (rows <= 0) {
            throw new BusinessException("修改用户失败");
        }
        return "修改用户成功";
    }

    @Override
    public String deleteUser(Long id) {
        ensureAdminPermission("删除用户");

        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("用户不存在");
        }
        if ("admin".equalsIgnoreCase(existing.getUsername())) {
            throw new BusinessException("默认管理员不能删除");
        }

        String currentUsername = CurrentUserContext.getUsername();
        if (currentUsername != null && currentUsername.equalsIgnoreCase(existing.getUsername())) {
            throw new BusinessException("不能删除当前登录用户");
        }

        int rows = userMapper.deleteById(id);
        if (rows <= 0) {
            throw new BusinessException("删除用户失败");
        }
        return "删除用户成功";
    }

    private String resolveRole(User user) {
        if (user == null) {
            return UserRoleConstant.OPERATOR;
        }
        String role = user.getRole();
        if ((role == null || role.trim().isEmpty()) && "admin".equalsIgnoreCase(user.getUsername())) {
            return UserRoleConstant.ADMIN;
        }
        return UserRoleConstant.normalize(role);
    }

    private void ensureAdminPermission(String action) {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException("仅管理员可执行：" + action);
        }
    }

    private String normalizeKeyword(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeOptional(value);
        if (normalized == null || normalized.isEmpty()) {
            throw new BusinessException(message);
        }
        return normalized;
    }

    private int normalizeStatus(Integer status) {
        if (status == null) {
            return 1;
        }
        if (status != 0 && status != 1) {
            throw new BusinessException("用户状态参数无效");
        }
        return status;
    }
}
