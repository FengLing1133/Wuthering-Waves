package com.wutheringwaves.gacha.service;

import com.wutheringwaves.gacha.base.BaseTest;
import com.wutheringwaves.gacha.factory.TestDataFactory;
import com.wutheringwaves.gacha.mapper.UserMapper;
import com.wutheringwaves.gacha.model.User;
import com.wutheringwaves.gacha.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("UserService 单元测试")
class UserServiceTest extends BaseTest {

    @Autowired
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser(1L);
    }

    @Test
    @DisplayName("注册成功 - 返回Token和用户信息")
    void register_success() {
        String username = "newuser";
        String password = "password123";
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(3L);
            return 1;
        });

        Map<String, Object> result = userService.register(username, password);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("token"));
        assertNotNull(result.get("user"));
        verify(userMapper).insert(any(User.class));
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在返回失败结果")
    void register_duplicateUsername_returnFailure() {
        String username = "existinguser";
        String password = "password123";
        when(userMapper.selectOne(any())).thenReturn(testUser);

        Map<String, Object> result = userService.register(username, password);

        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertEquals("用户名已存在", result.get("message"));
    }

    @Test
    @DisplayName("登录成功 - 返回Token和用户信息")
    void login_success() {
        String username = "testuser";
        String password = "password123";
        testUser.setPassword(passwordEncoder.encode(password));
        when(userMapper.selectOne(any())).thenReturn(testUser);

        Map<String, Object> result = userService.login(username, password);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("token"));
        assertNotNull(result.get("user"));
    }

    @Test
    @DisplayName("登录失败 - 密码错误返回失败结果")
    void login_wrongPassword_returnFailure() {
        String username = "testuser";
        String password = "wrongpassword";
        testUser.setPassword(passwordEncoder.encode("correctpassword"));
        when(userMapper.selectOne(any())).thenReturn(testUser);

        Map<String, Object> result = userService.login(username, password);

        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertEquals("用户名或密码错误", result.get("message"));
    }

    @Test
    @DisplayName("登录失败 - 用户不存在返回失败结果")
    void login_userNotFound_returnFailure() {
        String username = "nonexistent";
        String password = "password123";
        when(userMapper.selectOne(any())).thenReturn(null);

        Map<String, Object> result = userService.login(username, password);

        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertEquals("用户名或密码错误", result.get("message"));
    }

    @Test
    @DisplayName("获取用户成功 - 返回用户信息")
    void getUserById_exist() {
        when(userMapper.selectById(1L)).thenReturn(testUser);

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getUsername(), result.getUsername());
    }

    @Test
    @DisplayName("获取用户失败 - 用户不存在返回null")
    void getUserById_notExist_returnNull() {
        when(userMapper.selectById(999L)).thenReturn(null);

        User result = userService.getUserById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("更新星声成功")
    void updateStarlight_success() {
        when(userMapper.updateStarlight(anyLong(), anyInt())).thenReturn(1);

        userService.updateStarlight(1L, -160);

        verify(userMapper).updateStarlight(1L, -160);
    }

    @Test
    @DisplayName("添加星辉成功")
    void addStarshards_success() {
        when(userMapper.addStarshards(anyLong(), anyInt())).thenReturn(1);

        userService.addStarshards(1L, 10);

        verify(userMapper).addStarshards(1L, 10);
    }

    @Test
    @DisplayName("注册成功 - 初始资源正确")
    void register_success_initialResources() {
        String username = "newuser";
        String password = "password123";
        when(userMapper.selectOne(any())).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(3L);
            return 1;
        });

        Map<String, Object> result = userService.register(username, password);

        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) result.get("user");
        assertEquals(1600, userMap.get("starlight"));
        assertEquals(0, userMap.get("starshards"));
        assertEquals("user", userMap.get("role"));
    }
}
