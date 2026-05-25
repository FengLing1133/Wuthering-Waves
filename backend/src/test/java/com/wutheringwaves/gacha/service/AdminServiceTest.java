package com.wutheringwaves.gacha.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.wutheringwaves.gacha.base.BaseTest;
import com.wutheringwaves.gacha.factory.TestDataFactory;
import com.wutheringwaves.gacha.mapper.GachaItemMapper;
import com.wutheringwaves.gacha.mapper.GachaPoolMapper;
import com.wutheringwaves.gacha.mapper.GachaRecordMapper;
import com.wutheringwaves.gacha.mapper.ItemCategoryMapper;
import com.wutheringwaves.gacha.mapper.ItemThemeMapper;
import com.wutheringwaves.gacha.mapper.PoolCategoryMapper;
import com.wutheringwaves.gacha.mapper.PoolFourStarUpMapper;
import com.wutheringwaves.gacha.mapper.UserMapper;
import com.wutheringwaves.gacha.model.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
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

    @MockBean
    private ItemThemeMapper itemThemeMapper;

    @MockBean
    private ItemCategoryMapper itemCategoryMapper;

    @MockBean
    private PoolCategoryMapper poolCategoryMapper;

    @MockBean
    private PoolFourStarUpMapper poolFourStarUpMapper;

    private GachaPool testPool;

    @BeforeAll
    static void initLambdaCache() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, GachaRecord.class);
        TableInfoHelper.initTableInfo(assistant, User.class);
        TableInfoHelper.initTableInfo(assistant, GachaPool.class);
        TableInfoHelper.initTableInfo(assistant, GachaItem.class);
        TableInfoHelper.initTableInfo(assistant, ItemTheme.class);
        TableInfoHelper.initTableInfo(assistant, ItemCategory.class);
        TableInfoHelper.initTableInfo(assistant, PoolCategory.class);
        TableInfoHelper.initTableInfo(assistant, PoolFourStarUp.class);
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

        boolean result = adminService.updateUserResources(1L, 5000);

        assertTrue(result);
        assertEquals(5000, user.getStarlight());
    }

    @Test
    @DisplayName("更新用户资源失败 - 用户不存在")
    void updateUserResources_notFound() {
        when(userMapper.selectById(999L)).thenReturn(null);

        boolean result = adminService.updateUserResources(999L, 5000);

        assertFalse(result);
    }

    // ========== 主题管理测试 ==========

    @Test
    @DisplayName("创建主题成功 - 含自动生成分类")
    void createTheme_withCategories() {
        ItemTheme theme = TestDataFactory.createTheme(1L, "鸣潮1.0");
        when(itemThemeMapper.insert(any(ItemTheme.class))).thenReturn(1);
        when(itemCategoryMapper.insert(any(ItemCategory.class))).thenReturn(1);

        ItemTheme result = adminService.createTheme("鸣潮1.0", "测试", List.of("3-star-weapon", "4-star-character"));

        assertNotNull(result);
        assertEquals("鸣潮1.0", result.getName());
        verify(itemThemeMapper).insert(any(ItemTheme.class));
        verify(itemCategoryMapper, times(2)).insert(any(ItemCategory.class));
    }

    @Test
    @DisplayName("创建主题成功 - 不生成分类")
    void createTheme_noCategories() {
        ItemTheme theme = TestDataFactory.createTheme(1L, "鸣潮1.0");
        when(itemThemeMapper.insert(any(ItemTheme.class))).thenReturn(1);

        ItemTheme result = adminService.createTheme("鸣潮1.0", "测试", null);

        assertNotNull(result);
        verify(itemThemeMapper).insert(any(ItemTheme.class));
        verify(itemCategoryMapper, never()).insert(any(ItemCategory.class));
    }

    @Test
    @DisplayName("更新主题成功")
    void updateTheme_success() {
        ItemTheme existing = TestDataFactory.createTheme(1L, "旧名称");
        when(itemThemeMapper.selectById(1L)).thenReturn(existing);
        when(itemThemeMapper.updateById(any(ItemTheme.class))).thenReturn(1);

        ItemTheme update = new ItemTheme();
        update.setName("新名称");
        ItemTheme result = adminService.updateTheme(1L, update);

        assertNotNull(result);
        assertEquals("新名称", result.getName());
        verify(itemThemeMapper).updateById(any(ItemTheme.class));
    }

    @Test
    @DisplayName("更新主题失败 - 主题不存在")
    void updateTheme_notFound() {
        when(itemThemeMapper.selectById(999L)).thenReturn(null);

        ItemTheme result = adminService.updateTheme(999L, new ItemTheme());

        assertNull(result);
    }

    @Test
    @DisplayName("删除主题成功 - 无引用")
    void deleteTheme_success() {
        ItemTheme theme = TestDataFactory.createTheme(1L, "鸣潮1.0");
        when(itemThemeMapper.selectById(1L)).thenReturn(theme);
        when(itemCategoryMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(itemThemeMapper.deleteById(1L)).thenReturn(1);

        boolean result = adminService.deleteTheme(1L);

        assertTrue(result);
        verify(itemThemeMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除主题失败 - 分类被卡池引用")
    void deleteTheme_categoryReferencedByPool() {
        ItemTheme theme = TestDataFactory.createTheme(1L, "鸣潮1.0");
        ItemCategory cat = TestDataFactory.createCategory(100L, "测试分类", 3, "weapon");
        when(itemThemeMapper.selectById(1L)).thenReturn(theme);
        when(itemCategoryMapper.selectList(any())).thenReturn(List.of(cat));
        when(poolCategoryMapper.selectCount(any())).thenReturn(1L);

        boolean result = adminService.deleteTheme(1L);

        assertFalse(result);
        verify(itemThemeMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("复制主题成功")
    void copyTheme_success() {
        ItemTheme source = TestDataFactory.createTheme(1L, "鸣潮1.0");
        ItemCategory srcCat = TestDataFactory.createCategory(100L, "鸣潮1.0 三星武器", 3, "weapon");
        when(itemThemeMapper.selectById(1L)).thenReturn(source);
        when(itemThemeMapper.insert(any(ItemTheme.class))).thenReturn(1);
        when(itemCategoryMapper.selectList(any())).thenReturn(List.of(srcCat));
        when(itemCategoryMapper.insert(any(ItemCategory.class))).thenReturn(1);

        ItemTheme result = adminService.copyTheme("鸣潮2.0", "复制版", 1L);

        assertNotNull(result);
        assertEquals("鸣潮2.0", result.getName());
        verify(itemCategoryMapper).insert(any(ItemCategory.class));
    }

    // ========== 分类 CRUD 测试 ==========

    @Test
    @DisplayName("创建分类成功")
    void createCategory_success() {
        ItemCategory cat = TestDataFactory.createCategory(null, "新分类", 3, "weapon");
        when(itemCategoryMapper.insert(any(ItemCategory.class))).thenReturn(1);

        ItemCategory result = adminService.createCategory(cat);

        assertNotNull(result);
        verify(itemCategoryMapper).insert(any(ItemCategory.class));
    }

    @Test
    @DisplayName("更新分类成功")
    void updateCategory_success() {
        ItemCategory existing = TestDataFactory.createCategory(1L, "旧分类", 3, "weapon");
        when(itemCategoryMapper.selectById(1L)).thenReturn(existing);
        when(itemCategoryMapper.updateById(any(ItemCategory.class))).thenReturn(1);

        ItemCategory update = new ItemCategory();
        update.setName("新分类名");
        ItemCategory result = adminService.updateCategory(1L, update);

        assertNotNull(result);
        assertEquals("新分类名", result.getName());
    }

    @Test
    @DisplayName("删除分类失败 - 被卡池引用")
    void deleteCategory_referencedByPool() {
        ItemCategory cat = TestDataFactory.createCategory(1L, "测试", 3, "weapon");
        when(itemCategoryMapper.selectById(1L)).thenReturn(cat);
        when(poolCategoryMapper.selectCount(any())).thenReturn(1L);

        boolean result = adminService.deleteCategory(1L);

        assertFalse(result);
        verify(itemCategoryMapper, never()).deleteById(anyLong());
    }
}
