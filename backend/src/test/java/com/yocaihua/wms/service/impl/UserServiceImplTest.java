package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.CurrentUserContext;
import com.yocaihua.wms.common.OperationLogActionConstant;
import com.yocaihua.wms.common.PageResult;
import com.yocaihua.wms.common.TokenStore;
import com.yocaihua.wms.common.UserRoleConstant;
import com.yocaihua.wms.dto.LoginDTO;
import com.yocaihua.wms.dto.UserAddDTO;
import com.yocaihua.wms.dto.UserUpdateDTO;
import com.yocaihua.wms.entity.User;
import com.yocaihua.wms.mapper.UserMapper;
import com.yocaihua.wms.service.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private TokenStore tokenStore;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OperationLogService operationLogService;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        CurrentUserContext.clear();
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void login_shouldThrow_whenUserMissing() {
        when(userMapper.selectByUsername("tester")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(loginDto("tester", "123456")));

        assertEquals("用户名或密码错误", exception.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(tokenStore, never()).saveToken(any(), any(), any());
    }

    @Test
    void login_shouldThrow_whenPasswordMismatch() {
        when(userMapper.selectByUsername("tester")).thenReturn(user(1L, "tester", "ENC", "测试员", 1, UserRoleConstant.OPERATOR));
        when(passwordEncoder.matches("123456", "ENC")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(loginDto("tester", "123456")));

        assertEquals("用户名或密码错误", exception.getMessage());
        verify(tokenStore, never()).saveToken(any(), any(), any());
    }

    @Test
    void login_shouldThrow_whenUserDisabled() {
        when(userMapper.selectByUsername("tester")).thenReturn(user(1L, "tester", "ENC", "测试员", 0, UserRoleConstant.OPERATOR));
        when(passwordEncoder.matches("123456", "ENC")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.login(loginDto("tester", "123456")));

        assertEquals("用户已被禁用", exception.getMessage());
        verify(tokenStore, never()).saveToken(any(), any(), any());
    }

    @Test
    void login_shouldReturnTokenAndRecordLog_whenCredentialsValid() {
        User user = user(10L, "tester", "ENC", "测试员", 1, UserRoleConstant.OPERATOR);
        when(userMapper.selectByUsername("tester")).thenReturn(user);
        when(passwordEncoder.matches("123456", "ENC")).thenReturn(true);

        Map<String, Object> result = userService.login(loginDto("tester", "123456"));

        String token = (String) result.get("token");
        assertNotNull(token);
        assertEquals(32, token.length());
        assertEquals("tester", result.get("username"));
        assertEquals("测试员", result.get("nickname"));
        assertEquals(UserRoleConstant.OPERATOR, result.get("role"));

        verify(tokenStore).saveToken(token, "tester", UserRoleConstant.OPERATOR);
        verify(operationLogService).recordSuccess(
                OperationLogActionConstant.LOGIN_SUCCESS,
                "用户认证",
                "USER",
                10L,
                null,
                "tester",
                "用户登录成功"
        );
    }

    @Test
    void login_shouldFallbackAdminRole_whenUsernameIsAdminAndRoleMissing() {
        User admin = user(1L, "admin", "ENC", "管理员", 1, null);
        when(userMapper.selectByUsername("admin")).thenReturn(admin);
        when(passwordEncoder.matches("admin123", "ENC")).thenReturn(true);

        Map<String, Object> result = userService.login(loginDto("admin", "admin123"));

        assertEquals(UserRoleConstant.ADMIN, result.get("role"));
        verify(tokenStore).saveToken((String) result.get("token"), "admin", UserRoleConstant.ADMIN);
    }

    @Test
    void logout_shouldIgnoreBlankToken() {
        userService.logout("   ");

        verify(tokenStore, never()).removeToken(any());
    }

    @Test
    void logout_shouldTrimTokenBeforeRemoval() {
        userService.logout("  abc123  ");

        verify(tokenStore).removeToken("abc123");
    }

    @Test
    void getCurrentUser_shouldThrow_whenNoCurrentUsername() {
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getCurrentUser());

        assertEquals("未登录或登录已失效", exception.getMessage());
    }

    @Test
    void getCurrentUser_shouldThrow_whenUserMissing() {
        CurrentUserContext.setUsername("tester");
        when(userMapper.selectByUsername("tester")).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getCurrentUser());

        assertEquals("当前用户不存在", exception.getMessage());
    }

    @Test
    void getCurrentUser_shouldThrow_whenUserDisabled() {
        CurrentUserContext.setUsername("tester");
        when(userMapper.selectByUsername("tester")).thenReturn(user(1L, "tester", "ENC", "测试员", 0, UserRoleConstant.OPERATOR));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getCurrentUser());

        assertEquals("用户已被禁用", exception.getMessage());
    }

    @Test
    void getCurrentUser_shouldReturnCurrentUserInfo() {
        CurrentUserContext.setUsername("admin");
        when(userMapper.selectByUsername("admin")).thenReturn(user(1L, "admin", "ENC", "管理员", 1, null));

        Map<String, Object> result = userService.getCurrentUser();

        assertEquals("admin", result.get("username"));
        assertEquals("管理员", result.get("nickname"));
        assertEquals(1, result.get("status"));
        assertEquals(UserRoleConstant.ADMIN, result.get("role"));
    }

    @Test
    void getUserPage_shouldThrow_whenCurrentUserIsNotAdmin() {
        CurrentUserContext.setRole(UserRoleConstant.OPERATOR);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.getUserPage(null, null, 1, 10));

        assertEquals("仅管理员可执行：查询用户列表", exception.getMessage());
    }

    @Test
    void getUserPage_shouldNormalizeKeywordsAndClampPageSize() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        User user = user(1L, "tester", "ENC", "测试员", 1, UserRoleConstant.OPERATOR);
        when(userMapper.count("tester", "测试员")).thenReturn(1L);
        when(userMapper.selectPage("tester", "测试员", 0, 200)).thenReturn(List.of(user));

        PageResult<User> result = userService.getUserPage("  tester  ", "  测试员  ", 0, 999);

        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getPageNum());
        assertEquals(200, result.getPageSize());
        assertEquals(1, result.getList().size());
        verify(userMapper).count("tester", "测试员");
        verify(userMapper).selectPage("tester", "测试员", 0, 200);
    }

    @Test
    void addUser_shouldThrow_whenCurrentUserIsNotAdmin() {
        CurrentUserContext.setRole(UserRoleConstant.OPERATOR);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.addUser(userAddDto("tester", "123456")));

        assertEquals("仅管理员可执行：新增用户", exception.getMessage());
    }

    @Test
    void addUser_shouldThrow_whenUsernameAlreadyExists() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        UserAddDTO dto = userAddDto("tester", "123456");
        when(userMapper.selectByUsername("tester")).thenReturn(user(1L, "tester", "ENC", "旧用户", 1, UserRoleConstant.OPERATOR));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.addUser(dto));

        assertEquals("用户名已存在", exception.getMessage());
        verify(userMapper, never()).insert(any());
    }

    @Test
    void addUser_shouldDefaultStatusAndRoleAndEncodePassword_whenValid() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        UserAddDTO dto = userAddDto("  tester  ", "  123456  ");
        dto.setNickname("  测试员  ");
        dto.setRole(null);
        dto.setStatus(null);
        when(userMapper.selectByUsername("tester")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("ENC");
        when(userMapper.insert(any(User.class))).thenReturn(1);

        String result = userService.addUser(dto);

        assertEquals("新增用户成功", result);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertEquals("tester", saved.getUsername());
        assertEquals("ENC", saved.getPassword());
        assertEquals("测试员", saved.getNickname());
        assertEquals(1, saved.getStatus());
        assertEquals(UserRoleConstant.OPERATOR, saved.getRole());
    }

    @Test
    void addUser_shouldThrow_whenInsertFails() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        UserAddDTO dto = userAddDto("tester", "123456");
        when(userMapper.selectByUsername("tester")).thenReturn(null);
        when(passwordEncoder.encode("123456")).thenReturn("ENC");
        when(userMapper.insert(any(User.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.addUser(dto));

        assertEquals("新增用户失败", exception.getMessage());
    }

    @Test
    void updateUser_shouldThrow_whenCurrentUserIsNotAdmin() {
        CurrentUserContext.setRole(UserRoleConstant.OPERATOR);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateUser(userUpdateDto(1L)));

        assertEquals("仅管理员可执行：编辑用户", exception.getMessage());
    }

    @Test
    void updateUser_shouldThrow_whenUserMissing() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        when(userMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateUser(userUpdateDto(1L)));

        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void updateUser_shouldThrow_whenDisableDefaultAdmin() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        UserUpdateDTO dto = userUpdateDto(1L);
        dto.setStatus(0);
        when(userMapper.selectById(1L)).thenReturn(user(1L, "admin", "ENC", "管理员", 1, UserRoleConstant.ADMIN));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateUser(dto));

        assertEquals("默认管理员不能被禁用", exception.getMessage());
    }

    @Test
    void updateUser_shouldThrow_whenDisableCurrentLoggedInUser() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        CurrentUserContext.setUsername("tester");
        UserUpdateDTO dto = userUpdateDto(2L);
        dto.setStatus(0);
        when(userMapper.selectById(2L)).thenReturn(user(2L, "tester", "ENC", "测试员", 1, UserRoleConstant.OPERATOR));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateUser(dto));

        assertEquals("不能禁用当前登录用户", exception.getMessage());
    }

    @Test
    void updateUser_shouldThrow_whenStatusInvalid() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        UserUpdateDTO dto = userUpdateDto(2L);
        dto.setStatus(2);
        when(userMapper.selectById(2L)).thenReturn(user(2L, "tester", "ENC", "测试员", 1, UserRoleConstant.OPERATOR));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateUser(dto));

        assertEquals("用户状态参数无效", exception.getMessage());
    }

    @Test
    void updateUser_shouldEncodePassword_whenPasswordProvided() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        UserUpdateDTO dto = userUpdateDto(2L);
        dto.setNickname("  新昵称  ");
        dto.setPassword("  newpass  ");
        dto.setRole(null);
        dto.setStatus(1);
        when(userMapper.selectById(2L)).thenReturn(user(2L, "tester", "OLD", "测试员", 1, UserRoleConstant.OPERATOR));
        when(passwordEncoder.encode("newpass")).thenReturn("ENC2");
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        String result = userService.updateUser(dto);

        assertEquals("修改用户成功", result);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        User updated = captor.getValue();
        assertEquals(2L, updated.getId());
        assertEquals("新昵称", updated.getNickname());
        assertEquals(1, updated.getStatus());
        assertEquals(UserRoleConstant.OPERATOR, updated.getRole());
        assertEquals("ENC2", updated.getPassword());
    }

    @Test
    void updateUser_shouldThrow_whenUpdateFails() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        UserUpdateDTO dto = userUpdateDto(2L);
        dto.setStatus(1);
        dto.setRole(UserRoleConstant.OPERATOR);
        when(userMapper.selectById(2L)).thenReturn(user(2L, "tester", "OLD", "测试员", 1, UserRoleConstant.OPERATOR));
        when(userMapper.updateById(any(User.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.updateUser(dto));

        assertEquals("修改用户失败", exception.getMessage());
    }

    @Test
    void deleteUser_shouldThrow_whenCurrentUserIsNotAdmin() {
        CurrentUserContext.setRole(UserRoleConstant.OPERATOR);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.deleteUser(1L));

        assertEquals("仅管理员可执行：删除用户", exception.getMessage());
    }

    @Test
    void deleteUser_shouldThrow_whenUserMissing() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        when(userMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.deleteUser(1L));

        assertEquals("用户不存在", exception.getMessage());
    }

    @Test
    void deleteUser_shouldThrow_whenDeletingDefaultAdmin() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        when(userMapper.selectById(1L)).thenReturn(user(1L, "admin", "ENC", "管理员", 1, UserRoleConstant.ADMIN));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.deleteUser(1L));

        assertEquals("默认管理员不能删除", exception.getMessage());
    }

    @Test
    void deleteUser_shouldThrow_whenDeletingCurrentUser() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        CurrentUserContext.setUsername("tester");
        when(userMapper.selectById(2L)).thenReturn(user(2L, "tester", "ENC", "测试员", 1, UserRoleConstant.OPERATOR));

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.deleteUser(2L));

        assertEquals("不能删除当前登录用户", exception.getMessage());
    }

    @Test
    void deleteUser_shouldThrow_whenDeleteFails() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        when(userMapper.selectById(2L)).thenReturn(user(2L, "tester", "ENC", "测试员", 1, UserRoleConstant.OPERATOR));
        when(userMapper.deleteById(2L)).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.deleteUser(2L));

        assertEquals("删除用户失败", exception.getMessage());
    }

    @Test
    void deleteUser_shouldDeleteSuccessfully_whenValid() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        when(userMapper.selectById(2L)).thenReturn(user(2L, "tester", "ENC", "测试员", 1, UserRoleConstant.OPERATOR));
        when(userMapper.deleteById(2L)).thenReturn(1);

        String result = userService.deleteUser(2L);

        assertEquals("删除用户成功", result);
    }

    private LoginDTO loginDto(String username, String password) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    private UserAddDTO userAddDto(String username, String password) {
        UserAddDTO dto = new UserAddDTO();
        dto.setUsername(username);
        dto.setPassword(password);
        return dto;
    }

    private UserUpdateDTO userUpdateDto(Long id) {
        UserUpdateDTO dto = new UserUpdateDTO();
        dto.setId(id);
        return dto;
    }

    private User user(Long id,
                      String username,
                      String password,
                      String nickname,
                      Integer status,
                      String role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname);
        user.setStatus(status);
        user.setRole(role);
        return user;
    }
}
