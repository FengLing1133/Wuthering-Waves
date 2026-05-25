package com.wutheringwaves.gacha.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wutheringwaves.gacha.mapper.GachaItemMapper;
import com.wutheringwaves.gacha.mapper.GachaPoolMapper;
import com.wutheringwaves.gacha.mapper.GachaRecordMapper;
import com.wutheringwaves.gacha.mapper.UserMapper;
import com.wutheringwaves.gacha.mapper.PoolCategoryMapper;
import com.wutheringwaves.gacha.mapper.ItemCategoryMapper;
import com.wutheringwaves.gacha.mapper.ItemThemeMapper;
import com.wutheringwaves.gacha.model.GachaItem;
import com.wutheringwaves.gacha.model.GachaPool;
import com.wutheringwaves.gacha.model.GachaRecord;
import com.wutheringwaves.gacha.model.User;
import com.wutheringwaves.gacha.model.PoolCategory;
import com.wutheringwaves.gacha.model.ItemCategory;
import com.wutheringwaves.gacha.model.ItemTheme;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final GachaPoolMapper gachaPoolMapper;
    private final GachaItemMapper gachaItemMapper;
    private final GachaRecordMapper gachaRecordMapper;
    private final UserMapper userMapper;
    private final PoolCategoryMapper poolCategoryMapper;
    private final ItemCategoryMapper itemCategoryMapper;
    private final ItemThemeMapper itemThemeMapper;

    // ========== 卡池管理 ==========

    public List<GachaPool> listPools(String poolType, String status) {
        LambdaQueryWrapper<GachaPool> wrapper = new LambdaQueryWrapper<>();
        if (poolType != null && !poolType.isEmpty()) {
            wrapper.eq(GachaPool::getPoolType, poolType);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(GachaPool::getStatus, status);
        }
        wrapper.orderByDesc(GachaPool::getCreatedAt);
        return gachaPoolMapper.selectList(wrapper);
    }

    public GachaPool getPoolById(Long id) {
        return gachaPoolMapper.selectById(id);
    }

    public GachaPool createPool(GachaPool pool) {
        if (pool.getStatus() == null) {
            pool.setStatus("active");
        }
        gachaPoolMapper.insert(pool);
        return pool;
    }

    public GachaPool updatePool(GachaPool pool) {
        gachaPoolMapper.updateById(pool);
        return gachaPoolMapper.selectById(pool.getId());
    }

    @Transactional
    public boolean togglePoolStatus(Long id) {
        GachaPool pool = gachaPoolMapper.selectById(id);
        if (pool == null) return false;
        boolean activating = "inactive".equals(pool.getStatus());
        pool.setStatus(activating ? "active" : "inactive");
        pool.setSidebarVisible(activating);
        gachaPoolMapper.updateById(pool);
        return true;
    }

    @Transactional
    public boolean deletePool(Long id) {
        GachaPool pool = gachaPoolMapper.selectById(id);
        if (pool == null) return false;
        // 删除 pool_category 关联
        LambdaQueryWrapper<PoolCategory> pcWrapper = new LambdaQueryWrapper<>();
        pcWrapper.eq(PoolCategory::getPoolId, id);
        poolCategoryMapper.delete(pcWrapper);
        gachaPoolMapper.deleteById(id);
        return true;
    }

    // ========== 分类管理 ==========

    public List<ItemCategory> listCategories() {
        return itemCategoryMapper.selectList(
                new LambdaQueryWrapper<ItemCategory>().orderByAsc(ItemCategory::getSortOrder));
    }

    // ========== 主题管理 ==========

    public List<Map<String, Object>> listThemes() {
        List<ItemTheme> themes = itemThemeMapper.selectList(
                new LambdaQueryWrapper<ItemTheme>().orderByDesc(ItemTheme::getCreatedAt));

        // 一次查询获取所有分类，按 themeId 分组计数，避免 N+1
        List<ItemCategory> allCategories = itemCategoryMapper.selectList(null);
        Map<Long, Long> countByTheme = allCategories.stream()
                .filter(c -> c.getThemeId() != null)
                .collect(Collectors.groupingBy(ItemCategory::getThemeId, Collectors.counting()));

        return themes.stream().map(theme -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", theme.getId());
            map.put("name", theme.getName());
            map.put("description", theme.getDescription());
            map.put("createdAt", theme.getCreatedAt());
            map.put("updatedAt", theme.getUpdatedAt());
            map.put("categoryCount", countByTheme.getOrDefault(theme.getId(), 0L));
            return map;
        }).toList();
    }

    public ItemTheme getThemeById(Long id) {
        return itemThemeMapper.selectById(id);
    }

    @Transactional
    public ItemTheme createTheme(String name, String description, List<String> generateCategories) {
        ItemTheme theme = new ItemTheme();
        theme.setName(name);
        theme.setDescription(description);
        itemThemeMapper.insert(theme);

        if (generateCategories != null && !generateCategories.isEmpty()) {
            for (String type : generateCategories) {
                ItemCategory cat = buildCategoryFromType(type, theme.getId());
                if (cat != null) {
                    itemCategoryMapper.insert(cat);
                }
            }
        }
        return theme;
    }

    @Transactional
    public ItemTheme updateTheme(Long id, ItemTheme update) {
        ItemTheme theme = itemThemeMapper.selectById(id);
        if (theme == null) return null;
        if (update.getName() != null) theme.setName(update.getName());
        if (update.getDescription() != null) theme.setDescription(update.getDescription());
        itemThemeMapper.updateById(theme);
        return theme;
    }

    @Transactional
    public boolean deleteTheme(Long id) {
        ItemTheme theme = itemThemeMapper.selectById(id);
        if (theme == null) return false;

        LambdaQueryWrapper<ItemCategory> catWrapper = new LambdaQueryWrapper<>();
        catWrapper.eq(ItemCategory::getThemeId, id);
        List<ItemCategory> categories = itemCategoryMapper.selectList(catWrapper);

        if (!categories.isEmpty()) {
            List<Long> catIds = categories.stream().map(ItemCategory::getId).toList();

            // 批量检查是否有卡池引用
            LambdaQueryWrapper<PoolCategory> pcWrapper = new LambdaQueryWrapper<>();
            pcWrapper.in(PoolCategory::getCategoryId, catIds);
            if (poolCategoryMapper.selectCount(pcWrapper) > 0) return false;

            // 批量检查是否有物品引用
            LambdaQueryWrapper<GachaItem> itemWrapper = new LambdaQueryWrapper<>();
            itemWrapper.in(GachaItem::getCategoryId, catIds);
            if (gachaItemMapper.selectCount(itemWrapper) > 0) return false;

            for (ItemCategory cat : categories) {
                itemCategoryMapper.deleteById(cat.getId());
            }
        }
        itemThemeMapper.deleteById(id);
        return true;
    }

    @Transactional
    public ItemTheme copyTheme(String name, String description, Long sourceThemeId) {
        ItemTheme source = itemThemeMapper.selectById(sourceThemeId);
        if (source == null) return null;

        ItemTheme newTheme = new ItemTheme();
        newTheme.setName(name);
        newTheme.setDescription(description);
        itemThemeMapper.insert(newTheme);

        LambdaQueryWrapper<ItemCategory> catWrapper = new LambdaQueryWrapper<>();
        catWrapper.eq(ItemCategory::getThemeId, sourceThemeId);
        List<ItemCategory> sourceCategories = itemCategoryMapper.selectList(catWrapper);
        for (ItemCategory srcCat : sourceCategories) {
            ItemCategory newCat = new ItemCategory();
            newCat.setName(srcCat.getName());
            newCat.setRarity(srcCat.getRarity());
            newCat.setItemType(srcCat.getItemType());
            newCat.setDescription(srcCat.getDescription());
            newCat.setSortOrder(srcCat.getSortOrder());
            newCat.setThemeId(newTheme.getId());
            itemCategoryMapper.insert(newCat);
        }
        return newTheme;
    }

    public List<ItemCategory> getCategoriesByTheme(Long themeId) {
        LambdaQueryWrapper<ItemCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ItemCategory::getThemeId, themeId);
        wrapper.orderByAsc(ItemCategory::getSortOrder);
        return itemCategoryMapper.selectList(wrapper);
    }

    public ItemCategory createCategory(ItemCategory category) {
        if (category.getThemeId() != null && itemThemeMapper.selectById(category.getThemeId()) == null) {
            return null;
        }
        itemCategoryMapper.insert(category);
        return category;
    }

    public ItemCategory updateCategory(Long id, ItemCategory update) {
        ItemCategory category = itemCategoryMapper.selectById(id);
        if (category == null) return null;
        if (update.getName() != null) category.setName(update.getName());
        if (update.getRarity() != null) category.setRarity(update.getRarity());
        if (update.getItemType() != null) category.setItemType(update.getItemType());
        if (update.getDescription() != null) category.setDescription(update.getDescription());
        if (update.getThemeId() != null) {
            if (itemThemeMapper.selectById(update.getThemeId()) == null) return null;
            category.setThemeId(update.getThemeId());
        }
        itemCategoryMapper.updateById(category);
        return category;
    }

    public boolean deleteCategory(Long id) {
        ItemCategory category = itemCategoryMapper.selectById(id);
        if (category == null) return false;
        LambdaQueryWrapper<PoolCategory> pcWrapper = new LambdaQueryWrapper<>();
        pcWrapper.eq(PoolCategory::getCategoryId, id);
        if (poolCategoryMapper.selectCount(pcWrapper) > 0) return false;
        LambdaQueryWrapper<GachaItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(GachaItem::getCategoryId, id);
        if (gachaItemMapper.selectCount(itemWrapper) > 0) return false;
        itemCategoryMapper.deleteById(id);
        return true;
    }

    private ItemCategory buildCategoryFromType(String type, Long themeId) {
        return switch (type) {
            case "3-star-weapon" -> {
                ItemCategory cat = new ItemCategory();
                cat.setName("三星武器");
                cat.setRarity(3);
                cat.setItemType("weapon");
                cat.setDescription("三星武器");
                cat.setSortOrder(1);
                cat.setThemeId(themeId);
                yield cat;
            }
            case "3-star-character" -> {
                ItemCategory cat = new ItemCategory();
                cat.setName("三星角色");
                cat.setRarity(3);
                cat.setItemType("character");
                cat.setDescription("三星角色");
                cat.setSortOrder(2);
                cat.setThemeId(themeId);
                yield cat;
            }
            case "4-star-character" -> {
                ItemCategory cat = new ItemCategory();
                cat.setName("四星角色");
                cat.setRarity(4);
                cat.setItemType("character");
                cat.setDescription("四星角色");
                cat.setSortOrder(3);
                cat.setThemeId(themeId);
                yield cat;
            }
            case "4-star-weapon" -> {
                ItemCategory cat = new ItemCategory();
                cat.setName("四星武器");
                cat.setRarity(4);
                cat.setItemType("weapon");
                cat.setDescription("四星武器");
                cat.setSortOrder(4);
                cat.setThemeId(themeId);
                yield cat;
            }
            default -> null;
        };
    }

    // ========== 卡池物品管理（基于分类） ==========

    public List<GachaItem> getPoolItems(Long poolId) {
        List<Long> categoryIds = getCategoryIdsByPool(poolId);
        if (categoryIds.isEmpty()) return Collections.emptyList();
        LambdaQueryWrapper<GachaItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(GachaItem::getCategoryId, categoryIds);
        return gachaItemMapper.selectList(wrapper);
    }

    public List<Map<String, Object>> getPoolUpItems(Long poolId) {
        GachaPool pool = gachaPoolMapper.selectById(poolId);
        if (pool == null) return Collections.emptyList();

        List<Map<String, Object>> result = new ArrayList<>();
        if (pool.getFivestarUp() != null) {
            GachaItem item = gachaItemMapper.selectById(pool.getFivestarUp());
            if (item != null) result.add(itemToMap(item));
        }
        if (pool.getFourstarUp() != null && !pool.getFourstarUp().isBlank()) {
            for (Long id : parseFourstarUp(pool.getFourstarUp())) {
                GachaItem item = gachaItemMapper.selectById(id);
                if (item != null) result.add(itemToMap(item));
            }
        }
        return result;
    }

    public List<Map<String, Object>> getPoolFourStarItems(Long poolId) {
        GachaPool pool = gachaPoolMapper.selectById(poolId);
        if (pool == null || pool.getFourstarUp() == null || pool.getFourstarUp().isBlank()) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Long id : parseFourstarUp(pool.getFourstarUp())) {
            GachaItem item = gachaItemMapper.selectById(id);
            if (item != null) {
                Map<String, Object> m = new HashMap<>();
                m.put("id", item.getId());
                m.put("name", item.getName());
                m.put("imageUrl", item.getImageUrl());
                result.add(m);
            }
        }
        return result;
    }

    @Transactional
    public void updatePoolConfig(Long poolId, List<Long> categoryIds,
                                  Long fivestarUp, List<Long> fourstarUpIds) {
        // 更新分类关联
        LambdaQueryWrapper<PoolCategory> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(PoolCategory::getPoolId, poolId);
        poolCategoryMapper.delete(deleteWrapper);

        if (categoryIds != null) {
            for (Long catId : categoryIds) {
                PoolCategory pc = new PoolCategory();
                pc.setPoolId(poolId);
                pc.setCategoryId(catId);
                poolCategoryMapper.insert(pc);
            }
        }

        // 更新UP物品
        GachaPool pool = gachaPoolMapper.selectById(poolId);
        pool.setFivestarUp(fivestarUp);
        pool.setFourstarUp(fourstarUpIds != null && !fourstarUpIds.isEmpty()
                ? fourstarUpIds.stream().map(String::valueOf).collect(Collectors.joining(","))
                : null);
        gachaPoolMapper.updateById(pool);
    }

    // ========== 辅助方法 ==========

    public GachaItem getItemById(Long id) {
        return gachaItemMapper.selectById(id);
    }

    // ========== 物品管理 ==========

    public Page<GachaItem> listItems(int page, int size, Integer rarity,
                                      String itemType, String keyword, Long categoryId) {
        Page<GachaItem> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<GachaItem> wrapper = new LambdaQueryWrapper<>();
        if (rarity != null) wrapper.eq(GachaItem::getRarity, rarity);
        if (itemType != null && !itemType.isEmpty()) wrapper.eq(GachaItem::getItemType, itemType);
        if (categoryId != null) wrapper.eq(GachaItem::getCategoryId, categoryId);
        if (keyword != null && !keyword.isEmpty()) wrapper.like(GachaItem::getName, keyword);
        wrapper.orderByAsc(GachaItem::getId);
        return gachaItemMapper.selectPage(pageParam, wrapper);
    }

    public GachaItem createItem(GachaItem item) {
        if (item.getCategoryId() == null) return null;
        ItemCategory cat = itemCategoryMapper.selectById(item.getCategoryId());
        if (cat == null) return null;
        gachaItemMapper.insert(item);
        return item;
    }

    public GachaItem updateItem(GachaItem item) {
        GachaItem existing = gachaItemMapper.selectById(item.getId());
        if (existing == null) return null;
        if (item.getCategoryId() != null) {
            ItemCategory cat = itemCategoryMapper.selectById(item.getCategoryId());
            if (cat == null) return null;
        }
        gachaItemMapper.updateById(item);
        return gachaItemMapper.selectById(item.getId());
    }

    public boolean deleteItem(Long id) {
        GachaItem item = gachaItemMapper.selectById(id);
        if (item == null) return false;
        // 检查是否被卡池引用为UP物品
        LambdaQueryWrapper<GachaPool> poolWrapper = new LambdaQueryWrapper<>();
        poolWrapper.and(w -> w
                .eq(GachaPool::getFivestarUp, id)
                .or().apply("FIND_IN_SET({0}, fourstar_up)", id));
        if (gachaPoolMapper.selectCount(poolWrapper) > 0) return false;
        gachaItemMapper.deleteById(id);
        return true;
    }

    private List<Long> getCategoryIdsByPool(Long poolId) {
        LambdaQueryWrapper<PoolCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PoolCategory::getPoolId, poolId);
        return poolCategoryMapper.selectList(wrapper).stream()
                .map(PoolCategory::getCategoryId)
                .toList();
    }

    public Map<String, Object> itemToMap(GachaItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("name", item.getName());
        map.put("rarity", item.getRarity());
        map.put("itemType", item.getItemType());
        map.put("imageUrl", item.getImageUrl());
        map.put("videoUrl", item.getVideoUrl());
        map.put("loopVideoUrl", item.getLoopVideoUrl());
        return map;
    }

    private Set<Long> parseFourstarUp(String fourstarUp) {
        if (fourstarUp == null || fourstarUp.isBlank()) return Collections.emptySet();
        return Arrays.stream(fourstarUp.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }

    // ========== 用户管理 ==========

    public Page<User> listUsers(int page, int size, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(User::getUsername, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return userMapper.selectPage(pageParam, wrapper);
    }

    public boolean updateUserResources(Long userId, Integer starlight) {
        User user = userMapper.selectById(userId);
        if (user == null) return false;
        if (starlight != null) user.setStarlight(starlight);
        userMapper.updateById(user);
        return true;
    }

    public Page<GachaRecord> getUserRecords(Long userId, String poolType, int page, int size) {
        Page<GachaRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<GachaRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GachaRecord::getUserId, userId);
        if (poolType != null && !poolType.isEmpty()) {
            wrapper.eq(GachaRecord::getPoolType, poolType);
        }
        wrapper.orderByDesc(GachaRecord::getCreatedAt);
        return gachaRecordMapper.selectPage(pageParam, wrapper);
    }

    // ========== 数据报表 ==========

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalUsers = userMapper.selectCount(null);
        stats.put("totalUsers", totalUsers);

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<GachaRecord> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.ge(GachaRecord::getCreatedAt, todayStart);
        activeWrapper.select(GachaRecord::getUserId);
        List<GachaRecord> todayRecords = gachaRecordMapper.selectList(activeWrapper);
        long dailyActiveUsers = todayRecords.stream()
                .map(GachaRecord::getUserId)
                .distinct()
                .count();
        stats.put("dailyActiveUsers", dailyActiveUsers);

        long totalPulls = gachaRecordMapper.selectCount(null);
        stats.put("totalPulls", totalPulls);

        LambdaQueryWrapper<GachaRecord> todayPullsWrapper = new LambdaQueryWrapper<>();
        todayPullsWrapper.ge(GachaRecord::getCreatedAt, todayStart);
        long dailyPulls = gachaRecordMapper.selectCount(todayPullsWrapper);
        stats.put("dailyPulls", dailyPulls);

        long totalConsumed = totalPulls * 160;
        stats.put("totalConsumedStarlight", totalConsumed);

        long dailyConsumed = dailyPulls * 160;
        stats.put("dailyConsumedStarlight", dailyConsumed);

        return stats;
    }

    public List<Map<String, Object>> getDailyStats(int days) {
        List<Map<String, Object>> dailyStats = new ArrayList<>();

        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dayEnd = LocalDateTime.of(date, LocalTime.MAX);

            LambdaQueryWrapper<GachaRecord> wrapper = new LambdaQueryWrapper<>();
            wrapper.ge(GachaRecord::getCreatedAt, dayStart);
            wrapper.le(GachaRecord::getCreatedAt, dayEnd);

            List<GachaRecord> dayRecords = gachaRecordMapper.selectList(wrapper);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.toString());
            dayData.put("pulls", dayRecords.size());
            dayData.put("consumedStarlight", dayRecords.size() * 160L);
            dayData.put("activeUsers", dayRecords.stream()
                    .map(GachaRecord::getUserId).distinct().count());

            dayData.put("fiveStarCount", dayRecords.stream()
                    .filter(r -> r.getItemRarity() == 5).count());
            dayData.put("fourStarCount", dayRecords.stream()
                    .filter(r -> r.getItemRarity() == 4).count());

            dailyStats.add(dayData);
        }

        return dailyStats;
    }
}
