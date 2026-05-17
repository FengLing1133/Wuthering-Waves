package com.wutheringwaves.gacha.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wutheringwaves.gacha.base.BaseTest;
import com.wutheringwaves.gacha.model.GachaRecord;
import com.wutheringwaves.gacha.service.GachaService;
import com.wutheringwaves.gacha.factory.TestDataFactory;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GachaController 单元测试")
@AutoConfigureMockMvc
class GachaControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GachaService gachaService;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private String testToken;

    @BeforeEach
    void setUp() {
        testToken = "valid-token";
        when(jwtUtil.validateToken(testToken)).thenReturn(true);
        when(jwtUtil.getUserIdFromToken(testToken)).thenReturn(1L);
        when(jwtUtil.getUsernameFromToken(testToken)).thenReturn("testuser");
        when(jwtUtil.getRoleFromToken(testToken)).thenReturn("user");
    }

    @Test
    @DisplayName("单抽成功 - 返回200和物品")
    void pull_single_success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("poolType", "limited-character");
        request.put("count", 1);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("results", List.of(Map.of("name", "四星角色A", "rarity", 4, "type", "character", "isLimited", false, "pityCount", 1)));
        response.put("starlight", 1440);

        when(gachaService.pull(anyLong(), anyString(), anyInt())).thenReturn(response);

        mockMvc.perform(post("/api/gacha/pull")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    @DisplayName("十连抽成功 - 返回200")
    void pull_ten_success() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("poolType", "limited-character");
        request.put("count", 10);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("results", List.of(
                Map.of("name", "四星角色A", "rarity", 4, "type", "character", "isLimited", false, "pityCount", 1),
                Map.of("name", "三星武器A", "rarity", 3, "type", "weapon", "isLimited", false, "pityCount", 2)
        ));
        response.put("starlight", 100);

        when(gachaService.pull(anyLong(), anyString(), anyInt())).thenReturn(response);

        mockMvc.perform(post("/api/gacha/pull")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("抽卡失败 - 无效抽卡次数返回400")
    void pull_invalidCount_400() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("poolType", "limited-character");
        request.put("count", 5);

        mockMvc.perform(post("/api/gacha/pull")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("抽卡次数只能是1或10"));
    }

    @Test
    @DisplayName("抽卡失败 - 无效卡池类型返回400")
    void pull_invalidPoolType_400() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("poolType", "invalid");
        request.put("count", 1);

        mockMvc.perform(post("/api/gacha/pull")
                        .header("Authorization", "Bearer " + testToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("无效的池子类型"));
    }

    @Test
    @DisplayName("获取历史记录成功 - 返回200")
    void getHistory_success() throws Exception {
        List<GachaRecord> records = List.of(
                TestDataFactory.createRecord(1L, "limited-character", "四星角色A", 4, "character")
        );
        Map<String, Object> historyData = Map.of("records", records, "total", 1L, "page", 1, "size", 20);
        when(gachaService.getHistory(anyLong(), anyString(), anyInt(), anyInt())).thenReturn(historyData);

        mockMvc.perform(get("/api/gacha/history")
                        .header("Authorization", "Bearer " + testToken)
                        .param("poolType", "limited-character")
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.records").isArray());
    }

    @Test
    @DisplayName("获取统计信息成功 - 返回200")
    void getStats_success() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("fiveStarCount", 5);
        stats.put("fourStarCount", 50);
        stats.put("totalPulls", 100);
        when(gachaService.getStats(anyLong(), anyString())).thenReturn(stats);

        mockMvc.perform(get("/api/gacha/stats")
                        .header("Authorization", "Bearer " + testToken)
                        .param("poolType", "limited-character"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.stats.fiveStarCount").value(5))
                .andExpect(jsonPath("$.stats.totalPulls").value(100));
    }
}
