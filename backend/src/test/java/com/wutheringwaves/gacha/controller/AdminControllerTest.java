package com.wutheringwaves.gacha.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wutheringwaves.gacha.base.BaseTest;
import com.wutheringwaves.gacha.factory.TestDataFactory;
import com.wutheringwaves.gacha.model.GachaPool;
import com.wutheringwaves.gacha.service.AdminService;
import com.wutheringwaves.gacha.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AdminController 单元测试")
@AutoConfigureMockMvc
class AdminControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_TOKEN = "admin-test-token";

    @BeforeEach
    void setUp() {
        when(jwtUtil.validateToken(ADMIN_TOKEN)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(ADMIN_TOKEN)).thenReturn(2L);
        when(jwtUtil.getRoleFromToken(ADMIN_TOKEN)).thenReturn("ADMIN");
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder addAuth(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + ADMIN_TOKEN);
    }

    @Test
    @DisplayName("无认证访问管理接口 - 返回403")
    void noAuth_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/pools"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("查询卡池列表成功 - 返回200")
    void listPools_success() throws Exception {
        List<GachaPool> pools = List.of(TestDataFactory.createPool("character", "限定角色池"));
        when(adminService.listPools(any(), any())).thenReturn(pools);

        mockMvc.perform(addAuth(get("/api/admin/pools")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pools").isArray())
                .andExpect(jsonPath("$.pools[0].name").value("限定角色池"));
    }

    @Test
    @DisplayName("获取卡池详情成功 - 返回200")
    void getPool_success() throws Exception {
        GachaPool pool = TestDataFactory.createPool("character", "限定角色池");
        when(adminService.getPoolById(1L)).thenReturn(pool);

        mockMvc.perform(addAuth(get("/api/admin/pools/1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pool.name").value("限定角色池"));
    }

    @Test
    @DisplayName("获取卡池详情失败 - 卡池不存在返回400")
    void getPool_notFound_400() throws Exception {
        when(adminService.getPoolById(999L)).thenReturn(null);

        mockMvc.perform(addAuth(get("/api/admin/pools/999")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("卡池不存在"));
    }

    @Test
    @DisplayName("创建卡池成功 - 返回200")
    void createPool_success() throws Exception {
        GachaPool newPool = TestDataFactory.createPool("weapon", "武器池");
        newPool.setId(null);
        when(adminService.createPool(any(GachaPool.class))).thenReturn(newPool);

        mockMvc.perform(addAuth(post("/api/admin/pools"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newPool)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pool.name").value("武器池"));
    }

    @Test
    @DisplayName("更新卡池成功 - 返回200")
    void updatePool_success() throws Exception {
        GachaPool pool = TestDataFactory.createPool("character", "更新后的卡池");
        when(adminService.updatePool(any(GachaPool.class))).thenReturn(pool);

        mockMvc.perform(addAuth(put("/api/admin/pools/1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pool)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pool.name").value("更新后的卡池"));
    }

    @Test
    @DisplayName("切换卡池状态成功 - 返回200")
    void togglePoolStatus_success() throws Exception {
        when(adminService.togglePoolStatus(anyLong())).thenReturn(true);

        mockMvc.perform(addAuth(post("/api/admin/pools/1/toggle")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("切换卡池状态失败 - 卡池不存在返回400")
    void togglePoolStatus_notFound_400() throws Exception {
        when(adminService.togglePoolStatus(999L)).thenReturn(false);

        mockMvc.perform(addAuth(post("/api/admin/pools/999/toggle")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("卡池不存在"));
    }

    @Test
    @DisplayName("获取仪表盘统计成功 - 返回200")
    void getDashboardStats_success() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", 100);
        stats.put("totalPulls", 1000);
        when(adminService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(addAuth(get("/api/admin/stats/dashboard")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.totalUsers").value(100))
                .andExpect(jsonPath("$.stats.totalPulls").value(1000));
    }

    @Test
    @DisplayName("更新用户资源成功 - 返回200")
    void updateUserResources_success() throws Exception {
        when(adminService.updateUserResources(anyLong(), any())).thenReturn(true);

        Map<String, Object> request = new HashMap<>();
        request.put("starlight", 5000);

        mockMvc.perform(addAuth(put("/api/admin/users/1/resources"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("更新用户资源失败 - 用户不存在返回400")
    void updateUserResources_notFound_400() throws Exception {
        when(adminService.updateUserResources(anyLong(), any())).thenReturn(false);

        Map<String, Object> request = new HashMap<>();
        request.put("starlight", 5000);

        mockMvc.perform(addAuth(put("/api/admin/users/999/resources"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户不存在"));
    }
}
