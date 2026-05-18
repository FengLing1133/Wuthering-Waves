package com.wutheringwaves.gacha.service;

import com.wutheringwaves.gacha.base.BaseTest;
import com.wutheringwaves.gacha.factory.TestDataFactory;
import com.wutheringwaves.gacha.mapper.*;
import com.wutheringwaves.gacha.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("GachaService 单元测试")
class GachaServiceTest extends BaseTest {

    @Autowired
    private GachaService gachaService;

    @MockBean
    private GachaItemMapper gachaItemMapper;

    @MockBean
    private GachaRecordMapper gachaRecordMapper;

    @MockBean
    private GachaPityMapper gachaPityMapper;

    @MockBean
    private GachaPoolMapper gachaPoolMapper;

    @MockBean
    private PoolCategoryMapper poolCategoryMapper;

    @MockBean
    private UserService userService;

    private User testUser;
    private GachaPool testPool;
    private List<GachaItem> testItems;
    private GachaPity testPity;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser(1L);
        testPool = TestDataFactory.createPool("limited-character", "限定角色池");
        testItems = TestDataFactory.createCharacterPoolItems();
        testPity = TestDataFactory.createGachaPity(1L, "limited-character", 0, 0, false);
    }

    private void setupDefaultMocks() {
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(gachaPoolMapper.selectById(anyLong())).thenReturn(testPool);
        when(gachaPityMapper.selectOne(any())).thenReturn(testPity);
        // Mock 分类关联查询
        when(poolCategoryMapper.selectList(any())).thenReturn(TestDataFactory.createCharacterPoolCategories());
        // Mock 按分类查询物品
        when(gachaItemMapper.selectList(any())).thenReturn(testItems);
        when(gachaPityMapper.updateById(any(GachaPity.class))).thenReturn(1);
        when(gachaRecordMapper.insert(any(GachaRecord.class))).thenReturn(1);
    }

    @Test
    @DisplayName("单抽成功 - 扣除星声、返回物品")
    void pull_singlePull_success() {
        setupDefaultMocks();

        Map<String, Object> result = gachaService.pull(1L, "limited-character", 1L, 1);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("results"));
        List<?> results = (List<?>) result.get("results");
        assertEquals(1, results.size());
        verify(userService).updateStarlight(1L, -160);
        verify(gachaRecordMapper).insert(any(GachaRecord.class));
    }

    @Test
    @DisplayName("十连抽成功 - 扣除星声（1500）、返回10个物品")
    void pull_tenPull_success() {
        setupDefaultMocks();

        Map<String, Object> result = gachaService.pull(1L, "limited-character", 1L, 10);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        List<?> results = (List<?>) result.get("results");
        assertEquals(10, results.size());
        verify(userService).updateStarlight(1L, -1600);
        verify(gachaRecordMapper, times(10)).insert(any(GachaRecord.class));
    }

    @Test
    @DisplayName("抽卡失败 - 星声不足返回失败结果")
    void pull_insufficientStarlight_returnFailure() {
        testUser.setStarlight(100);
        when(userService.getUserById(1L)).thenReturn(testUser);

        Map<String, Object> result = gachaService.pull(1L, "limited-character", 1L, 1);

        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertEquals("星声不足", result.get("message"));
    }

    @Test
    @DisplayName("抽卡失败 - 缺少卡池ID")
    void pull_missingPoolId_returnFailure() {
        Map<String, Object> result = gachaService.pull(1L, "limited-character", null, 1);

        assertNotNull(result);
        assertFalse((Boolean) result.get("success"));
        assertEquals("缺少卡池ID", result.get("message"));
    }

    @Test
    @DisplayName("第90抽硬保底触发 - 必出五星")
    void pull_hardPityTriggered() {
        testPity.setFiveStarCount(89);
        setupDefaultMocks();

        Map<String, Object> result = gachaService.pull(1L, "limited-character", 1L, 1);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        List<?> results = (List<?>) result.get("results");
        Map<?, ?> firstResult = (Map<?, ?>) results.get(0);
        assertEquals(5, firstResult.get("rarity"));
    }

    @Test
    @DisplayName("大保底触发 - 下次必出限定角色")
    void pull_guaranteedLimited() {
        testPity.setGuaranteedFive(true);
        testPity.setFiveStarCount(89);
        setupDefaultMocks();

        Map<String, Object> result = gachaService.pull(1L, "limited-character", 1L, 1);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        List<?> results = (List<?>) result.get("results");
        Map<?, ?> firstResult = (Map<?, ?>) results.get(0);
        assertEquals(5, firstResult.get("rarity"));
        assertTrue((boolean) firstResult.get("isLimited"), "大保底应出限定角色");
    }

    @Test
    @DisplayName("每10抽必出四星或以上")
    void pull_fourStarPity() {
        testPity.setFourStarCount(9);
        setupDefaultMocks();

        Map<String, Object> result = gachaService.pull(1L, "limited-character", 1L, 1);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        List<?> results = (List<?>) result.get("results");
        Map<?, ?> firstResult = (Map<?, ?>) results.get(0);
        int rarity = (int) firstResult.get("rarity");
        assertTrue(rarity >= 4, "第10抽应出四星或以上");
    }

    @Test
    @DisplayName("获取历史记录 - 分页查询正确")
    void getHistory_pagination() {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<GachaRecord> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(1, 20);
        List<GachaRecord> records = List.of(
                TestDataFactory.createRecord(1L, "limited-character", "限定五星角色", 5, "character"),
                TestDataFactory.createRecord(1L, "limited-character", "四星角色A", 4, "character")
        );
        page.setRecords(records);
        when(gachaRecordMapper.selectPage(any(), any())).thenReturn(page);

        Map<String, Object> result = gachaService.getHistory(1L, "limited-character", 1, 20);

        assertNotNull(result);
        List<GachaRecord> resultRecords = (List<GachaRecord>) result.get("records");
        assertEquals(2, resultRecords.size());
    }

    @Test
    @DisplayName("获取统计信息 - 保底计数、抽卡次数统计正确")
    void getStats_correctCalculation() {
        List<GachaRecord> records = List.of(
                TestDataFactory.createRecord(1L, "limited-character", "限定五星角色", 5, "character"),
                TestDataFactory.createRecord(1L, "limited-character", "四星角色A", 4, "character"),
                TestDataFactory.createRecord(1L, "limited-character", "三星武器A", 3, "weapon")
        );
        when(gachaRecordMapper.selectList(any())).thenReturn(records);
        when(gachaPityMapper.selectOne(any())).thenReturn(testPity);

        Map<String, Object> result = gachaService.getStats(1L, "limited-character");

        assertNotNull(result);
        assertEquals(3, result.get("totalPulls"));
        assertEquals(1, result.get("fiveStarCount"));
        assertEquals(1, result.get("fourStarCount"));
        assertEquals(1, result.get("threeStarCount"));
    }

    @Test
    @DisplayName("抽卡结果包含正确字段")
    void pull_resultContainsCorrectFields() {
        setupDefaultMocks();

        Map<String, Object> result = gachaService.pull(1L, "limited-character", 1L, 1);

        assertNotNull(result);
        assertTrue((Boolean) result.get("success"));
        assertNotNull(result.get("starlight"));
        assertNotNull(result.get("fiveStarCount"));
        assertNotNull(result.get("fourStarCount"));

        List<?> results = (List<?>) result.get("results");
        Map<?, ?> firstResult = (Map<?, ?>) results.get(0);
        assertNotNull(firstResult.get("name"));
        assertNotNull(firstResult.get("rarity"));
        assertNotNull(firstResult.get("type"));
        assertNotNull(firstResult.get("isLimited"));
        assertNotNull(firstResult.get("pityCount"));
    }

    @Test
    @DisplayName("1000抽统计 - 保底系统累积验证（含软保底和硬保底）")
    void pull_1000Pulls_pityAccumulation() {
        testPity.setFiveStarCount(0);
        testPity.setGuaranteedFive(false);
        setupDefaultMocks();

        int totalFiveStar = 0;
        int beforeSoftPity = 0; // 保底计数 < 64 时出的5星
        int atSoftPity = 0;     // 软保底期间（64-78）出的5星
        int atHardPity = 0;     // 硬保底（79）出的5星

        for (int i = 0; i < 1000; i++) {
            Map<String, Object> result = gachaService.pull(1L, "limited-character", 1L, 1);
            List<?> results = (List<?>) result.get("results");
            Map<?, ?> firstResult = (Map<?, ?>) results.get(0);
            if (5 == (int) firstResult.get("rarity")) {
                totalFiveStar++;
                int pity = (int) firstResult.get("pityCount");
                if (pity <= 64) beforeSoftPity++;
                else if (pity < 80) atSoftPity++;
                else atHardPity++;
            }
        }

        System.out.println("===== 1000抽保底累积测试 =====");
        System.out.println("5星总数: " + totalFiveStar);
        System.out.println("5星率: " + String.format("%.2f%%", totalFiveStar / 10.0));
        System.out.println("基础概率出5星（<=64抽）: " + beforeSoftPity);
        System.out.println("软保底出5星（65-79抽）: " + atSoftPity);
        System.out.println("硬保底出5星（80抽）: " + atHardPity);

        assertTrue(totalFiveStar >= 12,
                "1000抽保底机制至少应出12个5星，实际: " + totalFiveStar);
    }

    @Test
    @DisplayName("1000抽统计 - 验证限定池50/50歪机制（含保底累积）")
    void pull_1000Pulls_limitedPoolFiftyFifty() {
        testPity.setFiveStarCount(0);
        testPity.setGuaranteedFive(false);
        setupDefaultMocks();

        int upCount = 0;
        int nonUpCount = 0;

        for (int i = 0; i < 1000; i++) {
            Map<String, Object> result = gachaService.pull(1L, "limited-character", 1L, 1);
            List<?> results = (List<?>) result.get("results");
            Map<?, ?> firstResult = (Map<?, ?>) results.get(0);
            if (5 == (int) firstResult.get("rarity")) {
                if ((boolean) firstResult.get("isLimited")) {
                    upCount++;
                } else {
                    nonUpCount++;
                }
            }
        }

        int totalFiveStar = upCount + nonUpCount;
        System.out.println("===== 1000抽50/50测试 =====");
        System.out.println("5星总数: " + totalFiveStar);
        System.out.println("UP 5星: " + upCount);
        System.out.println("非UP 5星: " + nonUpCount);
        if (totalFiveStar > 0) {
            System.out.println("UP占比: " + String.format("%.1f%%", upCount * 100.0 / totalFiveStar));
        }

        assertTrue(totalFiveStar >= 12,
                "1000抽保底机制至少应出12个5星，实际: " + totalFiveStar);
        assertTrue(nonUpCount > 0,
                "限定池应有非UP5星（50/50），实际UP=" + upCount + " 非UP=" + nonUpCount);
        assertTrue(upCount > 0,
                "限定池应有UP5星，实际UP=" + upCount + " 非UP=" + nonUpCount);
    }

    @Test
    @DisplayName("软保底概率递增验证 - 第81抽概率显著提高")
    void pull_softPityTriggered() {
        testPity.setFiveStarCount(80);
        setupDefaultMocks();

        int fiveStarCount = 0;
        for (int i = 0; i < 100; i++) {
            testPity.setFiveStarCount(80);
            Map<String, Object> result = gachaService.pull(1L, "limited-character", 1L, 1);
            List<?> results = (List<?>) result.get("results");
            Map<?, ?> firstResult = (Map<?, ?>) results.get(0);
            if (5 == (int) firstResult.get("rarity")) {
                fiveStarCount++;
            }
        }

        double probability = fiveStarCount / 100.0;
        assertTrue(probability > 0.1, "软保底概率应高于10%，实际: " + probability);
    }
}
