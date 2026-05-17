package com.wutheringwaves.gacha.integration;

import com.wutheringwaves.gacha.base.BaseTest;
import com.wutheringwaves.gacha.mapper.GachaPityMapper;
import com.wutheringwaves.gacha.mapper.GachaRecordMapper;
import com.wutheringwaves.gacha.mapper.UserMapper;
import com.wutheringwaves.gacha.model.GachaPity;
import com.wutheringwaves.gacha.model.GachaRecord;
import com.wutheringwaves.gacha.model.User;
import com.wutheringwaves.gacha.service.GachaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("抽卡流程集成测试")
class GachaIntegrationTest extends BaseTest {

    @Autowired
    private GachaService gachaService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GachaPityMapper gachaPityMapper;

    @Autowired
    private GachaRecordMapper recordMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long testUserId;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        User user = new User();
        user.setUsername("gacha_test_" + System.nanoTime());
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole("user");
        user.setStarlight(10000);
        user.setStarshards(0);
        userMapper.insert(user);
        testUserId = user.getId();

        // 初始化保底计数
        GachaPity pity = new GachaPity();
        pity.setUserId(testUserId);
        pity.setPoolType("limited-character");
        pity.setFiveStarCount(0);
        pity.setFourStarCount(0);
        pity.setGuaranteedFive(false);
        gachaPityMapper.insert(pity);
    }

    @Test
    @DisplayName("完整抽卡流程 - 单抽扣费、记录保存")
    void pull_singlePull_fullFlow() {
        User user = userMapper.selectById(testUserId);
        int initialStarlight = user.getStarlight();

        Map<String, Object> result = gachaService.pull(testUserId, "limited-character", 1);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));

        // 验证星声扣除
        User updatedUser = userMapper.selectById(testUserId);
        assertEquals(initialStarlight - 160, updatedUser.getStarlight());

        // 验证抽卡记录保存
        List<GachaRecord> records = recordMapper.selectList(null);
        assertFalse(records.isEmpty());
    }

    @Test
    @DisplayName("完整抽卡流程 - 十连抽扣费、记录保存")
    void pull_tenPull_fullFlow() {
        User user = userMapper.selectById(testUserId);
        int initialStarlight = user.getStarlight();

        Map<String, Object> result = gachaService.pull(testUserId, "limited-character", 10);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));

        // 验证星声扣除
        User updatedUser = userMapper.selectById(testUserId);
        assertEquals(initialStarlight - 1500, updatedUser.getStarlight());

        // 验证抽卡记录保存
        List<GachaRecord> records = recordMapper.selectList(null);
        assertTrue(records.size() >= 10);
    }

    @Test
    @DisplayName("硬保底验证 - 连续抽到保底触发")
    void pull_hardPity_fullFlow() {
        // 设置保底计数接近硬保底
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<GachaPity> wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(GachaPity::getUserId, testUserId);
        wrapper.eq(GachaPity::getPoolType, "limited-character");
        GachaPity pity = gachaPityMapper.selectOne(wrapper);
        pity.setFiveStarCount(85);
        gachaPityMapper.updateById(pity);

        // 连续抽卡直到触发保底
        boolean fiveStarObtained = false;
        for (int i = 0; i < 10; i++) {
            Map<String, Object> result = gachaService.pull(testUserId, "limited-character", 1);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            Map<String, Object> firstResult = results.get(0);
            if (5 == (int) firstResult.get("rarity")) {
                fiveStarObtained = true;
                break;
            }
        }

        assertTrue(fiveStarObtained, "在硬保底范围内应获得五星");
    }

    @Test
    @DisplayName("资源扣除验证 - 星声正确扣除")
    void pull_deductStarlight_fullFlow() {
        // 设置用户星声为320
        User user = userMapper.selectById(testUserId);
        user.setStarlight(320);
        userMapper.updateById(user);

        // 执行两次单抽
        gachaService.pull(testUserId, "limited-character", 1);
        gachaService.pull(testUserId, "limited-character", 1);

        // 验证星声为0
        User updatedUser = userMapper.selectById(testUserId);
        assertEquals(0, updatedUser.getStarlight());
    }

    @Test
    @DisplayName("历史记录验证 - 抽卡记录正确保存")
    void pull_recordHistory_fullFlow() {
        gachaService.pull(testUserId, "limited-character", 1);

        Map<String, Object> historyData = gachaService.getHistory(testUserId, "limited-character", 1, 20);
        assertNotNull(historyData);
        List<GachaRecord> history = (List<GachaRecord>) historyData.get("records");
        assertNotNull(history);
        assertFalse(history.isEmpty());
        assertEquals(testUserId, history.get(0).getUserId());
        assertEquals("limited-character", history.get(0).getPoolType());
    }

    @Test
    @DisplayName("统计信息验证 - 统计数据正确计算")
    void getStats_fullFlow() {
        gachaService.pull(testUserId, "limited-character", 10);

        Map<String, Object> stats = gachaService.getStats(testUserId, "limited-character");

        assertNotNull(stats);
        assertNotNull(stats.get("fiveStarCount"));
        assertNotNull(stats.get("fourStarCount"));
        assertNotNull(stats.get("totalPulls"));
        assertEquals(10, stats.get("totalPulls"));
    }
}
