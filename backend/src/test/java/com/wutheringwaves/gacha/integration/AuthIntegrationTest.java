package com.wutheringwaves.gacha.integration;

import com.wutheringwaves.gacha.base.BaseTest;
import com.wutheringwaves.gacha.mapper.UserMapper;
import com.wutheringwaves.gacha.model.User;
import com.wutheringwaves.gacha.service.UserService;
import com.wutheringwaves.gacha.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("认证流程集成测试")
class AuthIntegrationTest extends BaseTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("完整注册流程 - 用户创建、密码加密、Token生成")
    void register_fullFlow() {
        String username = "newuser_" + System.nanoTime();
        String password = "password123";

        Map<String, Object> result = userService.register(username, password);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("token"));
        assertNotNull(result.get("user"));

        // 验证用户已创建
        User createdUser = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("username", username));
        assertNotNull(createdUser);
        assertEquals(username, createdUser.getUsername());

        // 验证密码已加密
        assertTrue(passwordEncoder.matches(password, createdUser.getPassword()));

        // 验证初始资源
        assertEquals(100000, createdUser.getStarlight());
        assertEquals("user", createdUser.getRole());
    }

    @Test
    @DisplayName("完整登录流程 - 密码验证、Token生成")
    void login_fullFlow() {
        String username = "loginuser_" + System.nanoTime();
        String password = "password123";
        userService.register(username, password);

        Map<String, Object> result = userService.login(username, password);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("token"));

        // 验证Token有效性
        String token = (String) result.get("token");
        assertTrue(jwtUtil.validateToken(token));
        assertEquals(username, jwtUtil.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("认证流程 - Token验证、用户信息获取")
    void authenticate_fullFlow() {
        String username = "authuser_" + System.nanoTime();
        String password = "password123";
        Map<String, Object> registerResult = userService.register(username, password);
        String token = (String) registerResult.get("token");

        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userService.getUserById(userId);

        assertNotNull(user);
        assertEquals(username, user.getUsername());
    }

    @Test
    @DisplayName("重复注册检测 - 用户名重复返回失败")
    void register_duplicate_fullFlow() {
        String username = "duplicateuser_" + System.nanoTime();
        String password = "password123";
        userService.register(username, password);

        Map<String, Object> result = userService.register(username, password);

        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertEquals("用户名已存在", result.get("message"));
    }

    @Test
    @DisplayName("密码加密验证 - BCrypt加密正确")
    void register_passwordEncrypted_fullFlow() {
        String username = "encryptuser_" + System.nanoTime();
        String password = "password123";

        userService.register(username, password);

        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                        .eq("username", username));
        assertNotNull(user);

        // 验证密码是BCrypt格式
        assertTrue(user.getPassword().startsWith("$2a$"));
        assertTrue(passwordEncoder.matches(password, user.getPassword()));
    }
}
