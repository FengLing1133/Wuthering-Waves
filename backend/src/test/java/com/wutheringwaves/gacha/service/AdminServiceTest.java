package com.wutheringwaves.gacha.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.wutheringwaves.gacha.base.BaseTest;
import com.wutheringwaves.gacha.factory.TestDataFactory;
import com.wutheringwaves.gacha.mapper.GachaItemMapper;
import com.wutheringwaves.gacha.mapper.GachaPoolMapper;
import com.wutheringwaves.gacha.mapper.GachaRecordMapper;
import com.wutheringwaves.gacha.mapper.UserMapper;
import com.wutheringwaves.gacha.model.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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

@DisplayName("AdminService 单元测试")
class AdminServiceTest extends BaseTest {

    @Autowired
    private AdminService adminService;

    @MockBean
    private GachaPoolMapper poolMapper;

    @MockBean
    private GachaItemMapper itemMapper;

    @MockBean
    private GachaRecordMapper recordMapper;

    @MockBean
    private UserMapper userMapper;

    private GachaPool testPool;

    @BeforeAll
    static void initLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, GachaRecord.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, GachaPool.class);
        TableInfoHelper.initTableInfo(assistant, GachaItem.class);
    }

    @BeforeEach
    void setUp() {
        testPool = TestDataFactory.createPool("character", "限定角色池");
    }

    @Test
    @DisplayName("查询所有卡池成功")
    void listPools_all() {
        List<GachaPool> pools = List.of(testPool);
        when(poolMapper.selectList(any())).thenReturn(pools);

        List<GachaPool> result = adminService.listPools(null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("按类型查询卡池成功")
    void listPools_byType() {
        List<GachaPool> pools = List.of(testPool);
        when(poolMapper.selectList(any())).thenReturn(pools);

        List<GachaPool> result = adminService.listPools("character", null);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("创建卡池成功 - 状态默认为active")
    void createPool_success() {
        GachaPool newPool = TestDataFactory.createPool("weapon", "武器池");
        newPool.setId(null);
        newPool.setStatus(null);
        when(poolMapper.insert(any(GachaPool.class))).thenReturn(1);

        GachaPool result = adminService.createPool(newPool);

        assertNotNull(result);
        assertEquals("active", result.getStatus());
        verify(poolMapper).insert(any(GachaPool.class));
    }

    @Test
    @DisplayName("更新卡池成功")
    void updatePool_success() {
        testPool.setName("更新后的卡池");
        when(poolMapper.updateById(any(GachaPool.class))).thenReturn(1);
        when(poolMapper.selectById(1L)).thenReturn(testPool);

        GachaPool result = adminService.updatePool(testPool);

        assertNotNull(result);
        assertEquals("更新后的卡池", result.getName());
        verify(poolMapper).updateById(any(GachaPool.class));
    }

    @Test
    @DisplayName("切换卡池状态成功 - 从active到inactive")
    void togglePoolStatus_success() {
        testPool.setStatus("active");
        when(poolMapper.selectById(1L)).thenReturn(testPool);
        when(poolMapper.updateById(any(GachaPool.class))).thenReturn(1);

        boolean result = adminService.togglePoolStatus(1L);

        assertTrue(result);
        assertEquals("inactive", testPool.getStatus());
        verify(poolMapper).updateById(any(GachaPool.class));
    }

    @Test
    @DisplayName("切换卡池状态失败 - 卡池不存在")
    void togglePoolStatus_notFound() {
        when(poolMapper.selectById(999L)).thenReturn(null);

        boolean result = adminService.togglePoolStatus(999L);

        assertFalse(result);
    }

    @Test
    @DisplayName("获取仪表盘统计数据成功")
    void getDashboardStats_correct() {
        when(userMapper.selectCount(null)).thenReturn(100L);
        when(recordMapper.selectList(any())).thenReturn(List.of());
        when(recordMapper.selectCount(any())).thenReturn(1000L);

        Map<String, Object> result = adminService.getDashboardStats();

        assertNotNull(result);
        assertEquals(100L, result.get("totalUsers"));
        assertEquals(1000L, result.get("totalPulls"));
        assertNotNull(result.get("dailyActiveUsers"));
        assertNotNull(result.get("dailyPulls"));
        assertNotNull(result.get("totalConsumedStarlight"));
        assertNotNull(result.get("dailyConsumedStarlight"));
    }

    @Test
    @DisplayName("获取用户成功")
    void getPoolById_success() {
        when(poolMapper.selectById(1L)).thenReturn(testPool);

        GachaPool result = adminService.getPoolById(1L);

        assertNotNull(result);
        assertEquals(testPool.getName(), result.getName());
    }

    @Test
    @DisplayName("更新用户资源成功")
    void updateUserResources_success() {
        com.wutheringwaves.gacha.model.User user = TestDataFactory.createUser(1L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(userMapper.updateById(any(com.wutheringwaves.gacha.model.User.class))).thenReturn(1);

        boolean result = adminService.updateUserResources(1L, 5000, 200);

        assertTrue(result);
        assertEquals(5000, user.getStarlight());
        assertEquals(200, user.getStarshards());
    }

    @Test
    @DisplayName("更新用户资源失败 - 用户不存在")
    void updateUserResources_notFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        boolean result = adminService.updateUserResources(999L, 5000, 200);

        assertFalse(result);
    }
}
