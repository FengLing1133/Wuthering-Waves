package com.wutheringwaves.gacha.util;

import com.wutheringwaves.gacha.base.BaseTest;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JwtUtil 单元测试")
class JwtUtilTest extends BaseTest {

    @Autowired
    private JwtUtil jwtUtil;

    private Long testUserId;
    private String testUsername;
    private String testRole;

    @BeforeEach
    void setUp() {
        testUserId = 1L;
        testUsername = "testuser";
        testRole = "user";
    }

    @Test
    @DisplayName("生成Token成功 - Token非空且格式正确")
    void generateToken_success() {
        String token = jwtUtil.generateToken(testUserId, testUsername, testRole);

        assertNotNull(token);
        assertTrue(token.length() > 0);
        assertTrue(token.contains("."));
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("生成Token包含正确的用户ID")
    void generateToken_containsUserId() {
        String token = jwtUtil.generateToken(testUserId, testUsername, testRole);
        Claims claims = jwtUtil.parseToken(token);

        assertEquals(testUserId, claims.get("userId", Long.class));
    }

    @Test
    @DisplayName("生成Token包含正确的用户名")
    void generateToken_containsUsername() {
        String token = jwtUtil.generateToken(testUserId, testUsername, testRole);
        Claims claims = jwtUtil.parseToken(token);

        assertEquals(testUsername, claims.getSubject());
    }

    @Test
    @DisplayName("生成Token包含正确的角色")
    void generateToken_containsRole() {
        String token = jwtUtil.generateToken(testUserId, testUsername, testRole);
        Claims claims = jwtUtil.parseToken(token);

        assertEquals(testRole, claims.get("role", String.class));
    }

    @Test
    @DisplayName("解析有效Token成功")
    void parseToken_validToken() {
        String token = jwtUtil.generateToken(testUserId, testUsername, testRole);

        Claims claims = jwtUtil.parseToken(token);

        assertNotNull(claims);
        assertEquals(testUsername, claims.getSubject());
        assertEquals(testUserId, claims.get("userId", Long.class));
    }

    @Test
    @DisplayName("从Token提取用户ID成功")
    void getUserIdFromToken_success() {
        String token = jwtUtil.generateToken(testUserId, testUsername, testRole);

        Long userId = jwtUtil.getUserIdFromToken(token);

        assertEquals(testUserId, userId);
    }

    @Test
    @DisplayName("从Token提取用户名成功")
    void getUsernameFromToken_success() {
        String token = jwtUtil.generateToken(testUserId, testUsername, testRole);

        String username = jwtUtil.getUsernameFromToken(token);

        assertEquals(testUsername, username);
    }

    @Test
    @DisplayName("从Token提取角色成功")
    void getRoleFromToken_success() {
        String token = jwtUtil.generateToken(testUserId, testUsername, testRole);

        String role = jwtUtil.getRoleFromToken(token);

        assertEquals(testRole, role);
    }

    @Test
    @DisplayName("验证有效Token返回true")
    void validateToken_validToken_returnTrue() {
        String token = jwtUtil.generateToken(testUserId, testUsername, testRole);

        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid);
    }

    @Test
    @DisplayName("验证无效Token返回false")
    void validateToken_invalidToken_returnFalse() {
        String invalidToken = "invalid.token.here";

        boolean isValid = jwtUtil.validateToken(invalidToken);

        assertFalse(isValid);
    }

    @Test
    @DisplayName("验证空Token返回false")
    void validateToken_emptyToken_returnFalse() {
        boolean isValid = jwtUtil.validateToken("");

        assertFalse(isValid);
    }

    @Test
    @DisplayName("不同用户生成不同Token")
    void generateToken_differentUsers_differentTokens() {
        String token1 = jwtUtil.generateToken(1L, "user1", "user");
        String token2 = jwtUtil.generateToken(2L, "user2", "user");

        assertNotEquals(token1, token2);
    }
}
