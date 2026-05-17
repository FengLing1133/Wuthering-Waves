package com.wutheringwaves.gacha.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wutheringwaves.gacha.mapper.*;
import com.wutheringwaves.gacha.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class GachaService {

    private final GachaItemMapper gachaItemMapper;
    private final GachaRecordMapper gachaRecordMapper;
    private final GachaPityMapper gachaPityMapper;
    private final GachaPoolMapper gachaPoolMapper;
    private final UserService userService;

    @Transactional
    public Map<String, Object> pull(Long userId, String poolType, int count) {
        Map<String, Object> result = new HashMap<>();

        // 检查星声是否足够
        User user = userService.getUserById(userId);
        int cost = count == 10 ? 1500 : 160;
        if (user.getStarlight() < cost) {
            result.put("success", false);
            result.put("message", "星声不足");
            return result;
        }

        // 扣除星声
        userService.updateStarlight(userId, -cost);

        // 获取池子中的物品
        List<GachaItem> items = getItemsByPool(poolType);

        // 执行抽卡
        List<Map<String, Object>> pullResults = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> pullResult = doPull(userId, poolType, items);
            pullResults.add(pullResult);
        }

        // 统计结果
        int fiveStarCount = 0;
        int fourStarCount = 0;
        for (Map<String, Object> pr : pullResults) {
            int rarity = (int) pr.get("rarity");
            if (rarity == 5) fiveStarCount++;
            if (rarity == 4) fourStarCount++;
        }

        // 星辉返还
        int shardsEarned = fiveStarCount * 10 + fourStarCount * 2;
        if (shardsEarned > 0) {
            userService.addStarshards(userId, shardsEarned);
        }

        // 更新用户信息
        user = userService.getUserById(userId);

        result.put("success", true);
        result.put("results", pullResults);
        result.put("starlight", user.getStarlight());
        result.put("starshards", user.getStarshards());
        result.put("fiveStarCount", fiveStarCount);
        result.put("fourStarCount", fourStarCount);

        return result;
    }

    private Map<String, Object> doPull(Long userId, String poolType, List<GachaItem> items) {
        int currentPity = getCurrentPity(userId, poolType);
        boolean guaranteed = isGuaranteed(userId, poolType);

        int rarity = determineRarity(userId, poolType, currentPity, guaranteed);

        GachaPool poolConfig = getPoolConfig(poolType);

        GachaItem selectedItem = selectItem(items, rarity, poolType, guaranteed, poolConfig);

        updatePity(userId, poolType, rarity, selectedItem.getIsLimited());

        GachaRecord record = new GachaRecord();
        record.setUserId(userId);
        record.setPoolType(poolType);
        record.setItemName(selectedItem.getName());
        record.setItemRarity(selectedItem.getRarity());
        record.setItemType(selectedItem.getItemType());
        record.setIsLimited(selectedItem.getIsLimited());
        record.setPityCount(currentPity + 1);
        gachaRecordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();
        result.put("name", selectedItem.getName());
        result.put("rarity", selectedItem.getRarity());
        result.put("type", selectedItem.getItemType());
        result.put("isLimited", selectedItem.getIsLimited());
        result.put("pityCount", currentPity + 1);

        return result;
    }

    private GachaPool getPoolConfig(String poolType) {
        LambdaQueryWrapper<GachaPool> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GachaPool::getPoolType, poolType);
        wrapper.eq(GachaPool::getStatus, "active");
        wrapper.last("LIMIT 1");
        return gachaPoolMapper.selectOne(wrapper);
    }

    private int determineRarity(Long userId, String poolType, int currentPity, boolean guaranteed) {
        double rand = ThreadLocalRandom.current().nextDouble() * 100;

        GachaPool poolConfig = getPoolConfig(poolType);

        int maxPity = poolConfig != null ? poolConfig.getMaxPity() : 90;
        double fiveStarRate = poolConfig != null ? poolConfig.getFiveStarRate().doubleValue() : 0.8;
        double fourStarRate = poolConfig != null ? poolConfig.getFourStarRate().doubleValue() : 6.0;
        int softPityStart = poolConfig != null ? poolConfig.getSoftPityStart() : 75;
        double softPityInc = poolConfig != null ? poolConfig.getSoftPityIncrement().doubleValue() : 6.0;

        // 硬保底
        if (currentPity >= maxPity - 1) {
            return 5;
        }

        // 软保底
        if (currentPity >= softPityStart - 1) {
            double fiveStarChance = fiveStarRate + (currentPity - softPityStart + 1) * softPityInc;
            if (rand < fiveStarChance) {
                return 5;
            }
        }

        // 基础概率
        if (rand < fiveStarRate) {
            return 5;
        } else if (rand < fiveStarRate + fourStarRate) {
            return 4;
        }

        // 四星保底（10抽）
        int fourStarPity = getFourStarPity(userId, poolType);
        if (fourStarPity >= 9) {
            return 4;
        }

        return 3;
    }

    private GachaItem selectItem(List<GachaItem> items, int rarity, String poolType, boolean guaranteed, GachaPool poolConfig) {
        List<GachaItem> candidates = items.stream()
                .filter(item -> item.getRarity() == rarity)
                .toList();

        if (candidates.isEmpty()) {
            candidates = items.stream()
                    .filter(item -> item.getRarity() == 3)
                    .toList();
        }

        if (candidates.isEmpty()) {
            throw new IllegalStateException("物品池为空，poolType=" + poolType + ", rarity=" + rarity);
        }

        // 五星UP逻辑：仅限定池生效
        if (rarity == 5 && poolType.startsWith("limited-")) {
            List<GachaItem> upItems = getUpItems(candidates, poolConfig);

            if (!upItems.isEmpty()) {
                if (guaranteed) {
                    return upItems.get(ThreadLocalRandom.current().nextInt(upItems.size()));
                } else {
                    if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                        return upItems.get(ThreadLocalRandom.current().nextInt(upItems.size()));
                    } else {
                        List<GachaItem> standard = candidates.stream()
                                .filter(item -> !item.getIsLimited())
                                .toList();
                        if (!standard.isEmpty()) {
                            return standard.get(ThreadLocalRandom.current().nextInt(standard.size()));
                        }
                        return upItems.get(ThreadLocalRandom.current().nextInt(upItems.size()));
                    }
                }
            }
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private List<GachaItem> getUpItems(List<GachaItem> candidates, GachaPool poolConfig) {
        if (poolConfig != null && poolConfig.getUpItems() != null && !poolConfig.getUpItems().isEmpty()) {
            try {
                String upItemsStr = poolConfig.getUpItems().trim();
                if (upItemsStr.startsWith("[") && upItemsStr.endsWith("]")) {
                    upItemsStr = upItemsStr.substring(1, upItemsStr.length() - 1);
                    List<String> upNames = new ArrayList<>();
                    for (String name : upItemsStr.split(",")) {
                        name = name.trim().replace("\"", "").replace("'", "");
                        if (!name.isEmpty()) {
                            upNames.add(name);
                        }
                    }
                    List<GachaItem> matched = candidates.stream()
                            .filter(item -> upNames.contains(item.getName()))
                            .toList();
                    if (!matched.isEmpty()) {
                        return matched;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return candidates.stream()
                .filter(GachaItem::getIsLimited)
                .toList();
    }

    private List<GachaItem> getItemsByPool(String poolType) {
        LambdaQueryWrapper<GachaItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GachaItem::getPoolType, poolType);
        return gachaItemMapper.selectList(wrapper);
    }

    // ========== 保底相关：统一查 gacha_pity 表 ==========

    private GachaPity getOrCreatePity(Long userId, String poolType) {
        LambdaQueryWrapper<GachaPity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GachaPity::getUserId, userId);
        wrapper.eq(GachaPity::getPoolType, poolType);
        GachaPity pity = gachaPityMapper.selectOne(wrapper);
        if (pity == null) {
            pity = new GachaPity();
            pity.setUserId(userId);
            pity.setPoolType(poolType);
            pity.setFiveStarCount(0);
            pity.setFourStarCount(0);
            pity.setGuaranteedFive(false);
            gachaPityMapper.insert(pity);
        }
        return pity;
    }

    private int getCurrentPity(Long userId, String poolType) {
        GachaPity pity = getOrCreatePity(userId, poolType);
        return pity.getFiveStarCount() != null ? pity.getFiveStarCount() : 0;
    }

    private int getFourStarPity(Long userId, String poolType) {
        GachaPity pity = getOrCreatePity(userId, poolType);
        return pity.getFourStarCount() != null ? pity.getFourStarCount() : 0;
    }

    private boolean isGuaranteed(Long userId, String poolType) {
        GachaPity pity = getOrCreatePity(userId, poolType);
        return Boolean.TRUE.equals(pity.getGuaranteedFive());
    }

    private void updatePity(Long userId, String poolType, int rarity, boolean isLimited) {
        GachaPity pity = getOrCreatePity(userId, poolType);

        if (rarity == 5) {
            pity.setFiveStarCount(0);
            pity.setFourStarCount(pity.getFourStarCount() + 1);
            pity.setGuaranteedFive(!isLimited);
        } else if (rarity == 4) {
            pity.setFiveStarCount(pity.getFiveStarCount() + 1);
            pity.setFourStarCount(0);
        } else {
            pity.setFiveStarCount(pity.getFiveStarCount() + 1);
            pity.setFourStarCount(pity.getFourStarCount() + 1);
        }

        gachaPityMapper.updateById(pity);
    }

    // ========== 查询接口 ==========

    public Map<String, Object> getHistory(Long userId, String poolType, int page, int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<GachaRecord> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        LambdaQueryWrapper<GachaRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GachaRecord::getUserId, userId);
        if (poolType != null && !poolType.isEmpty()) {
            wrapper.eq(GachaRecord::getPoolType, poolType);
        }
        wrapper.orderByDesc(GachaRecord::getCreatedAt);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<GachaRecord> result =
                gachaRecordMapper.selectPage(pageParam, wrapper);

        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("page", page);
        response.put("size", size);
        return response;
    }

    public Map<String, Object> getStats(Long userId, String poolType) {
        Map<String, Object> stats = new HashMap<>();

        LambdaQueryWrapper<GachaRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GachaRecord::getUserId, userId);
        if (poolType != null && !poolType.isEmpty()) {
            wrapper.eq(GachaRecord::getPoolType, poolType);
        }

        List<GachaRecord> records = gachaRecordMapper.selectList(wrapper);

        int totalPulls = records.size();
        int fiveStarCount = (int) records.stream().filter(r -> r.getItemRarity() == 5).count();
        int fourStarCount = (int) records.stream().filter(r -> r.getItemRarity() == 4).count();
        int threeStarCount = (int) records.stream().filter(r -> r.getItemRarity() == 3).count();

        stats.put("totalPulls", totalPulls);
        stats.put("fiveStarCount", fiveStarCount);
        stats.put("fourStarCount", fourStarCount);
        stats.put("threeStarCount", threeStarCount);

        if (totalPulls > 0) {
            stats.put("fiveStarRate", String.format("%.2f%%", fiveStarCount * 100.0 / totalPulls));
            stats.put("fourStarRate", String.format("%.2f%%", fourStarCount * 100.0 / totalPulls));
        }

        if (poolType != null && !poolType.isEmpty()) {
            int currentPity = getCurrentPity(userId, poolType);
            stats.put("currentPity", currentPity);
        }

        return stats;
    }
}
