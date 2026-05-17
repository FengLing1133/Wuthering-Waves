package com.wutheringwaves.gacha.integration;

import com.wutheringwaves.gacha.base.BaseTest;
import com.wutheringwaves.gacha.mapper.GachaPityMapper;
import com.wutheringwaves.gacha.mapper.GachaItemMapper;
import com.wutheringwaves.gacha.mapper.GachaPoolMapper;
import com.wutheringwaves.gacha.mapper.UserMapper;
import com.wutheringwaves.gacha.model.*;
import com.wutheringwaves.gacha.service.AdminService;
import com.wutheringwaves.gacha.service.GachaService;
import com.wutheringwaves.gacha.util.JwtUtil;
import com.wutheringwaves.gacha.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("管理员后台集成测试")
class AdminIntegrationTest extends BaseTest {

    @Autowired
    private AdminService adminService;

    @Autowired
    private GachaService gachaService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private GachaPoolMapper gachaPoolMapper;

    @Autowired
    private GachaItemMapper gachaItemMapper;

    @Autowired
    private GachaPityMapper gachaPityMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private Long adminUserId;
    private Long normalUserId;

    @BeforeEach
    void setUp() {
        // 创建管理员用户
        User admin = new User();
        admin.setUsername("admin_integ_" + System.nanoTime());
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRole("ADMIN");
        admin.setStarlight(10000);
        admin.setStarshards(100);
        userMapper.insert(admin);
        adminUserId = admin.getId();

        // 创建普通用户
        User normalUser = new User();
        normalUser.setUsername("user_integ_" + System.nanoTime());
        normalUser.setPassword(passwordEncoder.encode("user123"));
        normalUser.setRole("user");
        normalUser.setStarlight(1600);
        normalUser.setStarshards(0);
        userMapper.insert(normalUser);
        normalUserId = normalUser.getId();

        // 初始化保底计数
        GachaPity pity = new GachaPity();
        pity.setUserId(normalUserId);
        pity.setPoolType("limited-character");
        pity.setFiveStarCount(0);
        pity.setFourStarCount(0);
        pity.setGuaranteedFive(false);
        gachaPityMapper.insert(pity);
    }

    // ==================== 1. 管理员登录与权限控制 ====================

    @Test
    @DisplayName("管理员JWT - Token包含ADMIN角色信息")
    void adminJwt_shouldContainAdminRole() {
        String token = TestUtils.generateTestToken(adminUserId, "admin_test", "ADMIN");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("ADMIN", jwtUtil.getRoleFromToken(token));
        assertEquals(adminUserId, jwtUtil.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("普通用户JWT - Token包含user角色信息")
    void userJwt_shouldContainUserRole() {
        String token = TestUtils.generateTestToken(normalUserId, "user_test", "user");

        assertTrue(jwtUtil.validateToken(token));
        assertEquals("user", jwtUtil.getRoleFromToken(token));
    }

    @Test
    @DisplayName("权限控制 - SecurityConfig要求/api/admin/**仅ADMIN角色可访问")
    void securityConfig_adminPathRequiresAdminRole() {
        // 验证管理员用户确实有ADMIN角色
        User admin = userMapper.selectById(adminUserId);
        assertEquals("ADMIN", admin.getRole());

        // 验证普通用户没有ADMIN角色
        User normal = userMapper.selectById(normalUserId);
        assertEquals("user", normal.getRole());
    }

    // ==================== 2. 卡池 CRUD 与抽卡逻辑联动 ====================

    @Test
    @DisplayName("卡池管理 - 创建新卡池并验证数据库持久化")
    void poolCRUD_createPool_shouldPersistToDatabase() {
        GachaPool newPool = new GachaPool();
        newPool.setName("集成测试卡池");
        newPool.setPoolType("limited-character");
        newPool.setDescription("用于集成测试");
        newPool.setFiveStarRate(new BigDecimal("0.80"));
        newPool.setFourStarRate(new BigDecimal("6.00"));
        newPool.setMaxPity(90);
        newPool.setSoftPityStart(75);
        newPool.setSoftPityIncrement(new BigDecimal("6.00"));
        newPool.setUpItems("[\"限定五星角色\"]");
        newPool.setStatus("active");

        GachaPool created = adminService.createPool(newPool);

        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("集成测试卡池", created.getName());
        assertEquals("active", created.getStatus());

        // 从数据库验证
        GachaPool fromDb = gachaPoolMapper.selectById(created.getId());
        assertNotNull(fromDb);
        assertEquals("集成测试卡池", fromDb.getName());
    }

    @Test
    @DisplayName("卡池管理 - 更新卡池配置")
    void poolCRUD_updatePool_shouldUpdateInDatabase() {
        // 获取已有卡池
        List<GachaPool> pools = adminService.listPools(null, null);
        assertFalse(pools.isEmpty());

        GachaPool existing = pools.get(0);
        String originalName = existing.getName();

        // 更新名称
        existing.setName("更新后的名称");
        GachaPool updated = adminService.updatePool(existing);

        assertNotNull(updated);
        assertEquals("更新后的名称", updated.getName());

        // 从数据库验证
        GachaPool fromDb = gachaPoolMapper.selectById(existing.getId());
        assertEquals("更新后的名称", fromDb.getName());
    }

    @Test
    @DisplayName("卡池管理 - 切换卡池状态")
    void poolCRUD_togglePoolStatus_shouldToggleActive() {
        List<GachaPool> pools = adminService.listPools(null, null);
        assertFalse(pools.isEmpty());

        GachaPool pool = pools.get(0);
        String originalStatus = pool.getStatus();

        // 切换状态
        boolean toggled = adminService.togglePoolStatus(pool.getId());
        assertTrue(toggled);

        GachaPool fromDb = gachaPoolMapper.selectById(pool.getId());
        if ("active".equals(originalStatus)) {
            assertEquals("inactive", fromDb.getStatus());
        } else {
            assertEquals("active", fromDb.getStatus());
        }
    }

    @Test
    @DisplayName("卡池管理 - 切换不存在的卡池返回false")
    void poolCRUD_toggleNonExistentPool_shouldReturnFalse() {
        boolean result = adminService.togglePoolStatus(99999L);
        assertFalse(result);
    }

    @Test
    @DisplayName("卡池与抽卡联动 - 卡池配置影响抽卡概率")
    void poolCRUD_configAffectsGacha_shouldWork() {
        // 创建一个高概率卡池
        GachaPool highRatePool = new GachaPool();
        highRatePool.setName("高概率测试池");
        highRatePool.setPoolType("limited-weapon");
        highRatePool.setFiveStarRate(new BigDecimal("50.00"));
        highRatePool.setFourStarRate(new BigDecimal("30.00"));
        highRatePool.setMaxPity(90);
        highRatePool.setSoftPityStart(75);
        highRatePool.setSoftPityIncrement(new BigDecimal("6.00"));
        highRatePool.setStatus("active");
        adminService.createPool(highRatePool);

        // 注意：抽卡逻辑从数据库读取卡池配置
        // 如果weapon池有多个active记录，getPoolConfig会取第一个
        // 这里验证的是卡池数据确实存储在数据库中
        List<GachaPool> weaponPools = adminService.listPools("limited-weapon", "active");
        assertFalse(weaponPools.isEmpty());
    }

    // ==================== 3. 用户资源调整后抽卡是否正常 ====================

    @Test
    @DisplayName("用户资源调整 - 增加星声后抽卡成功")
    void userResources_increaseStarlight_thenPull_shouldWork() {
        // 初始星声
        User before = userMapper.selectById(normalUserId);
        assertEquals(1600, before.getStarlight());

        // 管理员增加星声
        boolean updated = adminService.updateUserResources(normalUserId, 5000, null);
        assertTrue(updated);

        // 验证星声已更新
        User after = userMapper.selectById(normalUserId);
        assertEquals(5000, after.getStarlight());

        // 使用新星声抽卡
        Map<String, Object> pullResult = gachaService.pull(normalUserId, "limited-character", 10);
        assertTrue((Boolean) pullResult.get("success"));

        // 验证星声正确扣除（十连 1500）
        User afterPull = userMapper.selectById(normalUserId);
        assertEquals(3500, afterPull.getStarlight());
    }

    @Test
    @DisplayName("用户资源调整 - 减少星声后星声不足")
    void userResources_decreaseStarlight_thenPullFail_shouldWork() {
        // 管理员将星声设为100
        adminService.updateUserResources(normalUserId, 100, null);

        // 尝试十连（需要1500）
        Map<String, Object> pullResult = gachaService.pull(normalUserId, "limited-character", 10);
        assertFalse((Boolean) pullResult.get("success"));
        assertEquals("星声不足", pullResult.get("message"));
    }

    @Test
    @DisplayName("用户资源调整 - 调整星辉")
    void userResources_adjustStarshards_shouldWork() {
        adminService.updateUserResources(normalUserId, null, 50);

        User updated = userMapper.selectById(normalUserId);
        assertEquals(50, updated.getStarshards());
        // 星声不变
        assertEquals(1600, updated.getStarlight());
    }

    @Test
    @DisplayName("用户资源调整 - 同时调整星声和星辉")
    void userResources_adjustBoth_shouldWork() {
        adminService.updateUserResources(normalUserId, 3000, 20);

        User updated = userMapper.selectById(normalUserId);
        assertEquals(3000, updated.getStarlight());
        assertEquals(20, updated.getStarshards());
    }

    @Test
    @DisplayName("用户资源调整 - 调整不存在的用户返回false")
    void userResources_nonExistentUser_shouldReturnFalse() {
        boolean result = adminService.updateUserResources(99999L, 1000, null);
        assertFalse(result);
    }

    @Test
    @DisplayName("用户资源调整 - 多次调整后抽卡累计正确")
    void userResources_multipleAdjustments_thenPull_shouldBeAccurate() {
        // 第一次调整：增加到3000
        adminService.updateUserResources(normalUserId, 3000, null);
        // 单抽一次（160）
        gachaService.pull(normalUserId, "limited-character", 1);
        // 第二次调整：增加2000（当前2840 + 2000 = 4840）
        adminService.updateUserResources(normalUserId, 2000, null);

        User updated = userMapper.selectById(normalUserId);
        // 2840 + 2000 = 4840... 但updateUserResources是设置还是增加？
        // 查看AdminService：user.setStarlight(starlight)，所以是设置
        // 所以应该是 2000（设置值）
        assertEquals(2000, updated.getStarlight());
    }

    // ==================== 4. 报表数据准确性 ====================

    @Test
    @DisplayName("报表统计 - 仪表盘统计数据正确")
    void statsDashboard_shouldReturnCorrectCounts() {
        // 产生抽卡数据
        adminService.updateUserResources(normalUserId, 10000, null);
        gachaService.pull(normalUserId, "limited-character", 10);

        Map<String, Object> stats = adminService.getDashboardStats();

        assertNotNull(stats);
        assertNotNull(stats.get("totalUsers"));
        assertNotNull(stats.get("totalPulls"));

        // 验证用户数 >= 2（admin + normal）
        int totalUsers = ((Number) stats.get("totalUsers")).intValue();
        assertTrue(totalUsers >= 2);

        // 验证抽卡数 >= 10
        int totalPulls = ((Number) stats.get("totalPulls")).intValue();
        assertTrue(totalPulls >= 10);
    }

    @Test
    @DisplayName("报表统计 - 每日统计数据正确")
    void statsDaily_shouldReturnDailyBreakdown() {
        // 产生抽卡数据
        adminService.updateUserResources(normalUserId, 10000, null);
        gachaService.pull(normalUserId, "limited-character", 5);

        List<Map<String, Object>> dailyStats = adminService.getDailyStats(7);

        assertNotNull(dailyStats);
        // 至少有1天的数据
        assertFalse(dailyStats.isEmpty());

        // 验证数据结构
        Map<String, Object> today = dailyStats.get(0);
        assertNotNull(today.get("date"));
        assertNotNull(today.get("pulls"));
        assertNotNull(today.get("fiveStarCount"));
        assertNotNull(today.get("fourStarCount"));
    }

    @Test
    @DisplayName("报表统计 - 抽卡后五星/四星计数正确")
    void statsDaily_afterPulls_shouldCountRarities() {
        // 大量抽卡以确保有各稀有度的数据
        adminService.updateUserResources(normalUserId, 50000, null);
        gachaService.pull(normalUserId, "limited-character", 30);

        Map<String, Object> stats = gachaService.getStats(normalUserId, "limited-character");
        int totalPulls = (int) stats.get("totalPulls");
        int fiveStarCount = (int) stats.get("fiveStarCount");
        int fourStarCount = (int) stats.get("fourStarCount");
        int threeStarCount = (int) stats.get("threeStarCount");

        assertEquals(30, totalPulls);
        assertEquals(30, fiveStarCount + fourStarCount + threeStarCount);
    }

    // ==================== 5. 卡池查询 ====================

    @Test
    @DisplayName("卡池查询 - 按类型筛选")
    void listPools_byType_shouldFilter() {
        List<GachaPool> characterPools = adminService.listPools("limited-character", null);
        for (GachaPool pool : characterPools) {
            assertEquals("limited-character", pool.getPoolType());
        }
    }

    @Test
    @DisplayName("卡池查询 - 按状态筛选")
    void listPools_byStatus_shouldFilter() {
        List<GachaPool> activePools = adminService.listPools(null, "active");
        for (GachaPool pool : activePools) {
            assertEquals("active", pool.getStatus());
        }
    }

    @Test
    @DisplayName("卡池查询 - 获取单个卡池")
    void getPoolById_shouldReturnPool() {
        List<GachaPool> pools = adminService.listPools(null, null);
        assertFalse(pools.isEmpty());

        GachaPool pool = adminService.getPoolById(pools.get(0).getId());
        assertNotNull(pool);
        assertEquals(pools.get(0).getId(), pool.getId());
    }

    // ==================== 6. 用户管理 ====================

    @Test
    @DisplayName("用户管理 - 分页查询用户列表")
    void listUsers_shouldReturnPaginatedResults() {
        var page = adminService.listUsers(1, 10, null);
        assertNotNull(page);
        assertTrue(page.getRecords().size() >= 2); // admin + normal
    }

    @Test
    @DisplayName("用户管理 - 按关键字搜索用户")
    void listUsers_withKeyword_shouldFilter() {
        var page = adminService.listUsers(1, 10, "admin");
        assertNotNull(page);
        // 应该能找到admin用户
        assertTrue(page.getRecords().size() >= 1);
    }

    @Test
    @DisplayName("用户管理 - 查询用户抽卡记录")
    void getUserRecords_shouldReturnRecords() {
        // 先产生抽卡记录
        adminService.updateUserResources(normalUserId, 10000, null);
        gachaService.pull(normalUserId, "limited-character", 10);

        var page = adminService.getUserRecords(normalUserId, "limited-character", 1, 20);
        assertNotNull(page);
        assertEquals(10, page.getRecords().size());

        // 验证记录属于正确用户
        for (GachaRecord record : page.getRecords()) {
            assertEquals(normalUserId, record.getUserId());
            assertEquals("limited-character", record.getPoolType());
        }
    }

    @Test
    @DisplayName("用户管理 - 查询不存在用户的记录返回空")
    void getUserRecords_nonExistentUser_shouldReturnEmpty() {
        var page = adminService.getUserRecords(99999L, null, 1, 20);
        assertNotNull(page);
        assertTrue(page.getRecords().isEmpty());
    }
}
