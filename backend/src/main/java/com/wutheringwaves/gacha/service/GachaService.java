package com.wutheringwaves.gacha.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wutheringwaves.gacha.mapper.*;
import com.wutheringwaves.gacha.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GachaService {

    private final GachaItemMapper gachaItemMapper;
    private final GachaRecordMapper gachaRecordMapper;
    private final GachaPityMapper gachaPityMapper;
    private final GachaPoolMapper gachaPoolMapper;
    private final PoolCategoryMapper poolCategoryMapper;
    private final UserService userService;

    @Transactional
    public Map<String, Object> pull(Long userId, String poolType, Long poolId, int count) {
        Map<String, Object> result = new HashMap<>();

        if (poolId == null) {
            result.put("success", false);
            result.put("message", "缺少卡池ID");
            return result;
        }

        User user = userService.getUserById(userId);
        int cost = count == 10 ? 1600 : 160;
        if (user.getStarlight() < cost) {
            result.put("success", false);
            result.put("message", "星声不足");
            return result;
        }

        userService.updateStarlight(userId, -cost);

        GachaPool pool = getPoolConfig(poolId);
        // 常驻武器池：使用用户自选UP
        if ("standard-weapon".equals(poolType)) {
            Long userSelectedUp = userService.getSelectedWeaponUp(userId);
            if (userSelectedUp != null) {
                pool.setFivestarUp(userSelectedUp);
            }
        }
        List<GachaItem> items = getItemsByPool(pool);

        List<Map<String, Object>> pullResults = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Map<String, Object> pullResult = doPull(userId, poolType, pool, items);
            pullResults.add(pullResult);
        }

        int fiveStarCount = 0;
        int fourStarCount = 0;
        for (Map<String, Object> pr : pullResults) {
            int rarity = (int) pr.get("rarity");
            if (rarity == 5) fiveStarCount++;
            if (rarity == 4) fourStarCount++;
        }

        user = userService.getUserById(userId);

        result.put("success", true);
        result.put("results", pullResults);
        result.put("starlight", user.getStarlight());
        result.put("fiveStarCount", fiveStarCount);
        result.put("fourStarCount", fourStarCount);

        return result;
    }

    private Map<String, Object> doPull(Long userId, String poolType,
                                         GachaPool pool, List<GachaItem> items) {
        int currentPity = getCurrentPity(userId, poolType);
        boolean guaranteedFive = isGuaranteedFive(userId, poolType);
        boolean guaranteedFour = isGuaranteedFour(userId, poolType);

        int rarity = determineRarity(userId, poolType, pool.getId(), currentPity);

        boolean isLimitedPool = poolType.startsWith("limited-");

        GachaItem selectedItem = selectItem(items, pool, rarity, poolType,
                guaranteedFive, guaranteedFour, isLimitedPool);

        boolean isUp = isUpItem(selectedItem, pool);

        updatePity(userId, poolType, rarity, isUp);

        GachaRecord record = new GachaRecord();
        record.setUserId(userId);
        record.setPoolType(poolType);
        record.setItemName(selectedItem.getName());
        record.setItemRarity(selectedItem.getRarity());
        record.setItemType(selectedItem.getItemType());
        record.setIsLimited(isUp);
        record.setPityCount(currentPity + 1);
        gachaRecordMapper.insert(record);

        Map<String, Object> result = new HashMap<>();
        result.put("name", selectedItem.getName());
        result.put("rarity", selectedItem.getRarity());
        result.put("type", selectedItem.getItemType());
        result.put("isLimited", isUp);
        result.put("pityCount", currentPity + 1);

        return result;
    }

    private GachaPool getPoolConfig(Long poolId) {
        if (poolId == null) return null;
        return gachaPoolMapper.selectById(poolId);
    }

    private int determineRarity(Long userId, String poolType, Long poolId, int currentPity) {
        double rand = ThreadLocalRandom.current().nextDouble() * 100;

        GachaPool poolConfig = getPoolConfig(poolId);

        int maxPity = poolConfig != null ? poolConfig.getMaxPity() : 80;
        double fiveStarRate = poolConfig != null ? poolConfig.getFiveStarRate().doubleValue() : 0.8;
        double fourStarRate = poolConfig != null ? poolConfig.getFourStarRate().doubleValue() : 6.0;
        int softPityStart = poolConfig != null ? poolConfig.getSoftPityStart() : 65;
        double softPityInc = poolConfig != null ? poolConfig.getSoftPityIncrement().doubleValue() : 6.0;

        if (fiveStarRate >= 100) {
            return 5;
        }

        if (currentPity >= maxPity - 1) {
            return 5;
        }

        if (currentPity >= softPityStart - 1) {
            double fiveStarChance = fiveStarRate + (currentPity - softPityStart + 1) * softPityInc;
            if (rand < fiveStarChance) {
                return 5;
            }
        }

        if (rand < fiveStarRate) {
            return 5;
        } else if (rand < fiveStarRate + fourStarRate) {
            return 4;
        }

        int fourStarPity = getFourStarPity(userId, poolType);
        if (fourStarPity >= 9) {
            return 4;
        }

        return 3;
    }

    private GachaItem selectItem(List<GachaItem> items, GachaPool pool,
                                  int rarity, String poolType,
                                  boolean guaranteedFive, boolean guaranteedFour,
                                  boolean isLimitedPool) {
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

        Long fivestarUp = pool.getFivestarUp();
        Set<Long> fourstarUpIds = parseFourstarUp(pool.getFourstarUp());

        List<GachaItem> upItems = candidates.stream()
                .filter(item -> {
                    if (rarity == 5) return fivestarUp != null && item.getId().equals(fivestarUp);
                    if (rarity == 4) return fourstarUpIds.contains(item.getId());
                    return false;
                })
                .toList();
        List<GachaItem> nonUpItems = candidates.stream()
                .filter(item -> {
                    if (rarity == 5) return fivestarUp == null || !item.getId().equals(fivestarUp);
                    if (rarity == 4) return !fourstarUpIds.contains(item.getId());
                    return true;
                })
                .toList();

        if (!isLimitedPool || upItems.isEmpty()) {
            return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        }

        // 武器池不歪：直接出UP
        boolean isWeaponPool = poolType.contains("weapon");
        if (isWeaponPool && !upItems.isEmpty()) {
            return upItems.get(ThreadLocalRandom.current().nextInt(upItems.size()));
        }

        if (rarity == 5) {
            if (guaranteedFive) {
                return upItems.get(ThreadLocalRandom.current().nextInt(upItems.size()));
            }
            if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                return upItems.get(ThreadLocalRandom.current().nextInt(upItems.size()));
            } else {
                if (!nonUpItems.isEmpty()) {
                    return nonUpItems.get(ThreadLocalRandom.current().nextInt(nonUpItems.size()));
                }
                return upItems.get(ThreadLocalRandom.current().nextInt(upItems.size()));
            }
        }

        if (rarity == 4) {
            if (guaranteedFour) {
                return upItems.get(ThreadLocalRandom.current().nextInt(upItems.size()));
            }
            if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                return upItems.get(ThreadLocalRandom.current().nextInt(upItems.size()));
            } else {
                if (!nonUpItems.isEmpty()) {
                    return nonUpItems.get(ThreadLocalRandom.current().nextInt(nonUpItems.size()));
                }
                return upItems.get(ThreadLocalRandom.current().nextInt(upItems.size()));
            }
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    // ========== 按分类查询池内物品 ==========

    private List<GachaItem> getItemsByPool(GachaPool pool) {
        LambdaQueryWrapper<PoolCategory> pcWrapper = new LambdaQueryWrapper<>();
        pcWrapper.eq(PoolCategory::getPoolId, pool.getId());
        List<PoolCategory> poolCategories = poolCategoryMapper.selectList(pcWrapper);

        if (poolCategories.isEmpty()) return Collections.emptyList();

        List<Long> categoryIds = poolCategories.stream()
                .map(PoolCategory::getCategoryId)
                .toList();

        LambdaQueryWrapper<GachaItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(GachaItem::getCategoryId, categoryIds);
        return gachaItemMapper.selectList(itemWrapper);
    }

    // ========== UP 物品判断辅助 ==========

    private boolean isUpItem(GachaItem item, GachaPool pool) {
        if (item.getRarity() == 5) {
            return pool.getFivestarUp() != null && item.getId().equals(pool.getFivestarUp());
        }
        if (item.getRarity() == 4) {
            return parseFourstarUp(pool.getFourstarUp()).contains(item.getId());
        }
        return false;
    }

    private Set<Long> parseFourstarUp(String fourstarUp) {
        if (fourstarUp == null || fourstarUp.isBlank()) return Collections.emptySet();
        return Arrays.stream(fourstarUp.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }

    // ========== 保底相关 ==========

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
            pity.setGuaranteedFour(false);
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

    private boolean isGuaranteedFive(Long userId, String poolType) {
        GachaPity pity = getOrCreatePity(userId, poolType);
        return Boolean.TRUE.equals(pity.getGuaranteedFive());
    }

    private boolean isGuaranteedFour(Long userId, String poolType) {
        GachaPity pity = getOrCreatePity(userId, poolType);
        return Boolean.TRUE.equals(pity.getGuaranteedFour());
    }

    private void updatePity(Long userId, String poolType, int rarity, boolean isUp) {
        GachaPity pity = getOrCreatePity(userId, poolType);

        if (rarity == 5) {
            pity.setFiveStarCount(0);
            pity.setFourStarCount(pity.getFourStarCount() + 1);
            pity.setGuaranteedFive(!isUp);
        } else if (rarity == 4) {
            pity.setFiveStarCount(pity.getFiveStarCount() + 1);
            pity.setFourStarCount(0);
            pity.setGuaranteedFour(!isUp);
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
