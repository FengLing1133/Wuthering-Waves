package com.wutheringwaves.gacha.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wutheringwaves.gacha.mapper.GachaItemMapper;
import com.wutheringwaves.gacha.mapper.GachaPoolMapper;
import com.wutheringwaves.gacha.mapper.GachaRecordMapper;
import com.wutheringwaves.gacha.mapper.UserMapper;
import com.wutheringwaves.gacha.mapper.PoolCategoryMapper;
import com.wutheringwaves.gacha.mapper.ItemCategoryMapper;
import com.wutheringwaves.gacha.model.GachaItem;
import com.wutheringwaves.gacha.model.GachaPool;
import com.wutheringwaves.gacha.model.GachaRecord;
import com.wutheringwaves.gacha.model.User;
import com.wutheringwaves.gacha.model.PoolCategory;
import com.wutheringwaves.gacha.model.ItemCategory;
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

    private List<Long> getCategoryIdsByPool(Long poolId) {
        LambdaQueryWrapper<PoolCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PoolCategory::getPoolId, poolId);
        return poolCategoryMapper.selectList(wrapper).stream()
                .map(PoolCategory::getCategoryId)
                .toList();
    }

    private Map<String, Object> itemToMap(GachaItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", item.getId());
        map.put("name", item.getName());
        map.put("rarity", item.getRarity());
        map.put("itemType", item.getItemType());
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
