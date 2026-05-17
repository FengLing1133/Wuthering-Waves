package com.wutheringwaves.gacha.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wutheringwaves.gacha.base.BaseTest;
import com.wutheringwaves.gacha.model.User;
import com.wutheringwaves.gacha.service.UserService;
import com.wutheringwaves.gacha.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController 单元测试")
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("注册成功 - 返回200和Token")
    void register_success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");
        request.put("password", "password123");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", "jwt-token");
        response.put("user", Map.of("id", 1, "username", "testuser"));

        when(userService.register(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @DisplayName("注册失败 - 用户名太短返回400")
    void register_shortUsername_400() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "ab");
        request.put("password", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名长度需在3-20之间"));
    }

    @Test
    @DisplayName("注册失败 - 密码太短返回400")
    void register_shortPassword_400() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");
        request.put("password", "12345");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("密码长度不能少于6位"));
    }

    @Test
    @DisplayName("注册失败 - 用户名已存在返回200（业务失败）")
    void register_duplicateUsername() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "existinguser");
        request.put("password", "password123");

        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "用户名已存在");

        when(userService.register(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名已存在"));
    }

    @Test
    @DisplayName("登录成功 - 返回200和Token")
    void login_success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "testuser");
        request.put("password", "password123");

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", "jwt-token");
        response.put("user", Map.of("id", 1, "username", "testuser"));

        when(userService.login(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @DisplayName("登录失败 - 空参数返回400")
    void login_emptyParams_400() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("username", "");
        request.put("password", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("获取用户信息成功 - 返回200")
    void getUserInfo_success() throws Exception {
        String token = "valid-token";
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setRole("user");
        user.setStarlight(1600);
        user.setStarshards(0);

        when(jwtUtil.validateToken(token)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(token)).thenReturn(1L);
        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/auth/user")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.user.username").value("testuser"));
    }

    @Test
    @DisplayName("获取用户信息失败 - 无Token返回400")
    void getUserInfo_noToken_400() throws Exception {
        mockMvc.perform(get("/api/auth/user"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("获取用户信息失败 - 无效Token返回401")
    void getUserInfo_invalidToken_401() throws Exception {
        when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/auth/user")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }
}
