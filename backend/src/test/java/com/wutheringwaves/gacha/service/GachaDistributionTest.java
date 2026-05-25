package com.wutheringwaves.gacha.service;

import com.wutheringwaves.gacha.base.BaseTest;
import com.wutheringwaves.gacha.factory.TestDataFactory;
import com.wutheringwaves.gacha.mapper.*;
import com.wutheringwaves.gacha.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@DisplayName("抽卡分布统计测试")
class GachaDistributionTest extends BaseTest {

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
        testUser.setStarlight(Integer.MAX_VALUE);
        testPool = TestDataFactory.createPool("limited-character", "限定角色池");
        testItems = TestDataFactory.createCharacterPoolItems();
        testPity = TestDataFactory.createGachaPity(1L, "limited-character", 0, 0, false);

        when(userService.getUserById(1L)).thenReturn(testUser);
        when(gachaPoolMapper.selectById(anyLong())).thenReturn(testPool);
        when(gachaPityMapper.selectOne(any())).thenReturn(testPity);
        when(poolCategoryMapper.selectList(any())).thenReturn(TestDataFactory.createCharacterPoolCategories());
        when(gachaItemMapper.selectList(any())).thenReturn(testItems);
        when(gachaPityMapper.updateById(any(GachaPity.class))).thenReturn(1);
        when(gachaRecordMapper.insert(any(GachaRecord.class))).thenReturn(1);
    }

    @ParameterizedTest(name = "softPityStart={0}, totalPulls={1}")
    @CsvSource({
            "45, 1000", "45, 2000", "45, 3000", "45, 5000", "45, 10000",
            "50, 1000", "50, 2000", "50, 3000", "50, 5000", "50, 10000",
            "55, 1000", "55, 2000", "55, 3000", "55, 5000", "55, 10000",
            "60, 1000", "60, 2000", "60, 3000", "60, 5000", "60, 10000",
            "65, 1000", "65, 2000", "65, 3000", "65, 5000", "65, 10000"
    })
    @DisplayName("五星抽数区间分布")
    void distributionBySoftPityStart(int softPityStart, int totalPulls) {
        testPool.setSoftPityStart(softPityStart);
        testPity.setFiveStarCount(0);
        testPity.setFourStarCount(0);
        testPity.setGuaranteedFive(false);
        testPity.setGuaranteedFour(false);

        int[] buckets = new int[8]; // 1-10, 11-20, 21-30, 31-40, 41-50, 51-60, 61-70, 71-80
        int totalFiveStar = 0;

        for (int i = 0; i < totalPulls; i++) {
            Map<String, Object> result = gachaService.pull(1L, "limited-character", 1L, 1);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            Map<String, Object> pull = results.get(0);
            int rarity = (int) pull.get("rarity");
            if (rarity == 5) {
                totalFiveStar++;
                int pityCount = (int) pull.get("pityCount");
                int bucketIndex = Math.min(pityCount - 1, 79) / 10;
                if (bucketIndex > 7) bucketIndex = 7;
                buckets[bucketIndex]++;
            }
        }

        // 验证五星率在合理范围内（0.5% ~ 20%）
        double fiveStarRate = totalFiveStar * 100.0 / totalPulls;
        assertTrue(fiveStarRate > 0.5,
                String.format("五星率过低: %.2f%% (softPityStart=%d)", fiveStarRate, softPityStart));
        assertTrue(fiveStarRate < 20.0,
                String.format("五星率过高: %.2f%% (softPityStart=%d)", fiveStarRate, softPityStart));

        // 验证区间分布非空（至少有一个区间有五星出货）
        int nonEmptyBuckets = 0;
        for (int count : buckets) {
            if (count > 0) nonEmptyBuckets++;
        }
        assertTrue(nonEmptyBuckets >= 2,
                String.format("五星区间分布异常: 仅有 %d 个区间有出货 (softPityStart=%d)", nonEmptyBuckets, softPityStart));

        System.out.println();
        System.out.println("===== soft_pity_start=" + softPityStart + ", 总抽数=" + totalPulls + " =====");
        System.out.println("5星总数: " + totalFiveStar + " (五星率: " + String.format("%.2f%%", fiveStarRate) + ")");
        System.out.printf("%-10s | %-6s | %s%n", "区间", "金数", "占比");
        System.out.println("-----------|--------|--------");
        String[] labels = {"1~10", "11~20", "21~30", "31~40", "41~50", "51~60", "61~70", "71~80"};
        for (int i = 0; i < 8; i++) {
            double pct = totalFiveStar > 0 ? buckets[i] * 100.0 / totalFiveStar : 0;
            System.out.printf("%-10s | %-6d | %.1f%%%n", labels[i], buckets[i], pct);
        }
    }
}
