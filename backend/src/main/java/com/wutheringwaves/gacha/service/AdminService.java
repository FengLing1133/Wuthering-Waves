package com.wutheringwaves.gacha.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wutheringwaves.gacha.mapper.*;
import com.wutheringwaves.gacha.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final GachaPoolMapper gachaPoolMapper;
    private final GachaItemMapper gachaItemMapper;
    private final GachaRecordMapper gachaRecordMapper;
    private final UserMapper userMapper;

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
        pool.setStatus("active".equals(pool.getStatus()) ? "inactive" : "active");
        gachaPoolMapper.updateById(pool);
        return true;
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

        // 总用户数
        long totalUsers = userMapper.selectCount(null);
        stats.put("totalUsers", totalUsers);

        // 今日活跃用户（今天有抽卡记录的用户）
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

        // 总抽卡次数
        long totalPulls = gachaRecordMapper.selectCount(null);
        stats.put("totalPulls", totalPulls);

        // 今日抽卡次数
        LambdaQueryWrapper<GachaRecord> todayPullsWrapper = new LambdaQueryWrapper<>();
        todayPullsWrapper.ge(GachaRecord::getCreatedAt, todayStart);
        long dailyPulls = gachaRecordMapper.selectCount(todayPullsWrapper);
        stats.put("dailyPulls", dailyPulls);

        // 总消耗星声（每次单抽160，十连1500，但记录中没有区分，按单抽160计算）
        long totalConsumed = totalPulls * 160;
        stats.put("totalConsumedStarlight", totalConsumed);

        // 今日消耗星声
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

            // 统计出货
            dayData.put("fiveStarCount", dayRecords.stream()
                    .filter(r -> r.getItemRarity() == 5).count());
            dayData.put("fourStarCount", dayRecords.stream()
                    .filter(r -> r.getItemRarity() == 4).count());

            dailyStats.add(dayData);
        }

        return dailyStats;
    }
}
