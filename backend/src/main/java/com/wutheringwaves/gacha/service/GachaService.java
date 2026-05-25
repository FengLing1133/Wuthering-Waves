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

        boolean isLimitedPool = poolType.startsWith("limited-") || poolType.startsWith("special-");

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
        result.put("id", selectedItem.getId());
        result.put("name", selectedItem.getName());
        result.put("rarity", selectedItem.getRarity());
        result.put("type", selectedItem.getItemType());
        result.put("isLimited", isUp);
        result.put("pityCount", currentPity + 1);
        result.put("imageUrl", selectedItem.getImageUrl());
        result.put("videoUrl", selectedItem.getVideoUrl());
        result.put("loopVideoUrl", selectedItem.getLoopVideoUrl());

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
        int softPityStart = poolConfig != null ? poolConfig.getSoftPityStart() : 60;
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
        List<GachaItem> candidates = filterByRarity(items, rarity, poolType);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("物品池为空，poolType=" + poolType + ", rarity=" + rarity);
        }

        Long fivestarUp = pool.getFivestarUp();
        Set<Long> fourstarUpIds = parseFourstarUp(pool.getFourstarUp());
        List<GachaItem> upItems = filterUpItems(candidates, rarity, fivestarUp, fourstarUpIds);
        List<GachaItem> nonUpItems = filterNonUpItems(candidates, rarity, fivestarUp, fourstarUpIds, isLimitedPool);

        // 非限定池或无UP物品：完全随机
        if (!isLimitedPool || upItems.isEmpty()) {
            return randomPick(candidates);
        }

        // 限定武器池五星：100%出UP（不歪）
        if ("limited-weapon".equals(poolType) && rarity == 5 && !upItems.isEmpty()) {
            return randomPick(upItems);
        }

        // 特殊卡池：根据 allowLose 配置
        if (poolType.startsWith("special-") && rarity == 5 && !upItems.isEmpty()
                && pool.getAllowLose() != null && !pool.getAllowLose()) {
            return randomPick(upItems);
        }

        // 五星/四星 50/50 概率判定
        if ((rarity == 5 && guaranteedFive) || (rarity == 4 && guaranteedFour)) {
            return randomPick(upItems);
        }
        boolean win5050 = ThreadLocalRandom.current().nextDouble() < 0.5;
        if ((rarity == 5 || rarity == 4) && win5050) {
            return randomPick(upItems);
        }
        if ((rarity == 5 || rarity == 4) && !nonUpItems.isEmpty()) {
            return randomPick(nonUpItems);
        }
        return randomPick(upItems.isEmpty() ? candidates : upItems);
    }

    private GachaItem randomPick(List<GachaItem> items) {
        return items.get(ThreadLocalRandom.current().nextInt(items.size()));
    }

    private List<GachaItem> filterByRarity(List<GachaItem> items, int rarity, String poolType) {
        List<GachaItem> candidates = items.stream()
                .filter(item -> item.getRarity() == rarity)
                .toList();
        if (!candidates.isEmpty()) return candidates;
        // 稀有度物品缺失时回退到三星
        return items.stream().filter(item -> item.getRarity() == 3).toList();
    }

    private List<GachaItem> filterUpItems(List<GachaItem> candidates, int rarity,
                                           Long fivestarUp, Set<Long> fourstarUpIds) {
        return candidates.stream()
                .filter(item -> {
                    if (rarity == 5) return fivestarUp != null && item.getId().equals(fivestarUp);
                    if (rarity == 4) return fourstarUpIds.contains(item.getId());
                    return false;
                })
                .toList();
    }

    private List<GachaItem> filterNonUpItems(List<GachaItem> candidates, int rarity,
                                              Long fivestarUp, Set<Long> fourstarUpIds,
                                              boolean isLimitedPool) {
        if (!isLimitedPool) {
            return candidates.stream()
                    .filter(item -> {
                        if (rarity == 5) return fivestarUp == null || !item.getId().equals(fivestarUp);
                        if (rarity == 4) return !fourstarUpIds.contains(item.getId());
                        return true;
                    })
                    .toList();
        }
        // 限定池歪只能歪到常驻分类
        Set<Long> standardCategoryIds = getStandardCategoryIds(rarity);
        return candidates.stream()
                .filter(item -> {
                    boolean isNotUp;
                    if (rarity == 5) isNotUp = fivestarUp == null || !item.getId().equals(fivestarUp);
                    else if (rarity == 4) isNotUp = !fourstarUpIds.contains(item.getId());
                    else isNotUp = true;
                    return isNotUp && standardCategoryIds.contains(item.getCategoryId());
                })
                .toList();
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

    /**
     * 获取常驻分类ID集合（限定池歪的时候只能歪到这些分类）
     * 五星：4=五星常驻角色，5=五星常驻武器
     * 四星：2=四星角色，3=四星武器
     */
    private Set<Long> getStandardCategoryIds(int rarity) {
        if (rarity == 5) return Set.of(4L, 5L);
        if (rarity == 4) return Set.of(2L, 3L);
        return Set.of(1L); // 三星武器
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

    /**
     * 获取抽卡分析数据
     */
    public Map<String, Object> getAnalysis(Long userId) {
        Map<String, Object> analysis = new HashMap<>();

        // 查询所有抽卡记录
        LambdaQueryWrapper<GachaRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GachaRecord::getUserId, userId);
        wrapper.orderByAsc(GachaRecord::getCreatedAt);
        List<GachaRecord> allRecords = gachaRecordMapper.selectList(wrapper);

        int totalPulls = allRecords.size();
        analysis.put("totalPulls", totalPulls);

        if (totalPulls == 0) {
            analysis.put("title", "初入江湖");
            analysis.put("titleDesc", "还没有抽卡记录");
            analysis.put("avgFiveStarPity", 0);
            analysis.put("totalFiveStar", 0);
            analysis.put("limitedFiveStar", 0);
            analysis.put("standardFiveStar", 0);
            analysis.put("notLostRate", "0%");
            analysis.put("fiveStarItems", new ArrayList<>());
            analysis.put("poolStats", new HashMap<>());
            return analysis;
        }

        // 筛选五星记录
        List<GachaRecord> fiveStarRecords = allRecords.stream()
                .filter(r -> r.getItemRarity() == 5)
                .toList();

        int totalFiveStar = fiveStarRecords.size();
        analysis.put("totalFiveStar", totalFiveStar);

        // 计算平均出金抽数
        double avgFiveStarPity = fiveStarRecords.stream()
                .mapToInt(GachaRecord::getPityCount)
                .average()
                .orElse(0);
        analysis.put("avgFiveStarPity", Math.round(avgFiveStarPity * 10) / 10.0);

        // 限定五星和常驻五星统计
        int limitedFiveStar = (int) fiveStarRecords.stream()
                .filter(r -> r.getPoolType().startsWith("limited-") || r.getPoolType().startsWith("special-"))
                .filter(GachaRecord::getIsLimited)
                .count();
        int standardFiveStar = totalFiveStar - limitedFiveStar;
        analysis.put("limitedFiveStar", limitedFiveStar);
        analysis.put("standardFiveStar", standardFiveStar);

        // 计算小保底不歪概率（限定池中未歪的次数 / 限定池五星总数）
        long limitedPoolFiveStars = fiveStarRecords.stream()
                .filter(r -> r.getPoolType().startsWith("limited-") || r.getPoolType().startsWith("special-"))
                .count();
        long notLostCount = fiveStarRecords.stream()
                .filter(r -> r.getPoolType().startsWith("limited-") || r.getPoolType().startsWith("special-"))
                .filter(GachaRecord::getIsLimited)
                .count();
        double notLostRate = limitedPoolFiveStars > 0 ? (notLostCount * 100.0 / limitedPoolFiveStars) : 0;
        analysis.put("notLostRate", String.format("%.1f%%", notLostRate));

        // 计算每UP角色/武器平均抽数
        Map<String, Object> poolStats = new HashMap<>();

        // 角色池统计
        List<GachaRecord> characterPoolRecords = fiveStarRecords.stream()
                .filter(r -> "limited-character".equals(r.getPoolType()))
                .toList();
        if (!characterPoolRecords.isEmpty()) {
            double avgCharacterPity = characterPoolRecords.stream()
                    .mapToInt(GachaRecord::getPityCount)
                    .average()
                    .orElse(0);
            poolStats.put("avgCharacterPity", Math.round(avgCharacterPity * 10) / 10.0);
        } else {
            poolStats.put("avgCharacterPity", 0);
        }

        // 武器池统计
        List<GachaRecord> weaponPoolRecords = fiveStarRecords.stream()
                .filter(r -> "limited-weapon".equals(r.getPoolType()))
                .toList();
        if (!weaponPoolRecords.isEmpty()) {
            double avgWeaponPity = weaponPoolRecords.stream()
                    .mapToInt(GachaRecord::getPityCount)
                    .average()
                    .orElse(0);
            poolStats.put("avgWeaponPity", Math.round(avgWeaponPity * 10) / 10.0);
        } else {
            poolStats.put("avgWeaponPity", 0);
        }

        // 特殊角色池统计
        List<GachaRecord> specialCharPoolRecords = fiveStarRecords.stream()
                .filter(r -> "special-character".equals(r.getPoolType()))
                .toList();
        if (!specialCharPoolRecords.isEmpty()) {
            double avgSpecialCharPity = specialCharPoolRecords.stream()
                    .mapToInt(GachaRecord::getPityCount)
                    .average()
                    .orElse(0);
            poolStats.put("avgSpecialCharacterPity", Math.round(avgSpecialCharPity * 10) / 10.0);
        } else {
            poolStats.put("avgSpecialCharacterPity", 0);
        }

        // 特殊武器池统计
        List<GachaRecord> specialWpnPoolRecords = fiveStarRecords.stream()
                .filter(r -> "special-weapon".equals(r.getPoolType()))
                .toList();
        if (!specialWpnPoolRecords.isEmpty()) {
            double avgSpecialWpnPity = specialWpnPoolRecords.stream()
                    .mapToInt(GachaRecord::getPityCount)
                    .average()
                    .orElse(0);
            poolStats.put("avgSpecialWeaponPity", Math.round(avgSpecialWpnPity * 10) / 10.0);
        } else {
            poolStats.put("avgSpecialWeaponPity", 0);
        }

        analysis.put("poolStats", poolStats);

        // 获取所有五星物品信息（带图标）
        List<Map<String, Object>> fiveStarItems = new ArrayList<>();
        Map<String, Integer> itemCountMap = new HashMap<>();

        for (GachaRecord record : fiveStarRecords) {
            String itemName = record.getItemName();
            itemCountMap.merge(itemName, 1, Integer::sum);
        }

        // 查询物品信息
        LambdaQueryWrapper<GachaItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.in(GachaItem::getRarity, 5);
        List<GachaItem> allFiveStarItems = gachaItemMapper.selectList(itemWrapper);

        // 按记录时间倒序分组（最新的在前）
        Map<String, GachaRecord> latestRecords = new HashMap<>();
        for (GachaRecord record : fiveStarRecords) {
            latestRecords.putIfAbsent(record.getItemName(), record);
        }

        // 构建五星物品列表（按记录时间倒序）
        Set<String> addedItems = new HashSet<>();
        for (GachaRecord record : fiveStarRecords) {
            String itemName = record.getItemName();
            if (addedItems.contains(itemName)) continue;
            addedItems.add(itemName);

            Map<String, Object> itemInfo = new HashMap<>();
            itemInfo.put("name", itemName);
            itemInfo.put("count", itemCountMap.get(itemName));
            itemInfo.put("pityCount", record.getPityCount());
            itemInfo.put("isLimited", record.getIsLimited());
            itemInfo.put("poolType", record.getPoolType());
            itemInfo.put("itemType", record.getItemType());

            // 查找图标
            String imageUrl = null;
            for (GachaItem item : allFiveStarItems) {
                if (item.getName().equals(itemName)) {
                    imageUrl = item.getImageUrl();
                    break;
                }
            }
            itemInfo.put("imageUrl", imageUrl);

            fiveStarItems.add(itemInfo);
        }

        analysis.put("fiveStarItems", fiveStarItems);

        // 按卡池类型分组（不按名称去重，每条五星记录独立显示）
        Map<String, List<Map<String, Object>>> poolGroupedItems = new HashMap<>();
        String[] poolTypes = {"limited-character", "limited-weapon", "standard-character", "standard-weapon", "special-character", "special-weapon"};

        Map<String, String> imageUrlMap = new HashMap<>();
        for (GachaItem item : allFiveStarItems) {
            imageUrlMap.put(item.getName(), item.getImageUrl());
        }

        for (String poolType : poolTypes) {
            List<Map<String, Object>> poolItems = new ArrayList<>();
            for (GachaRecord record : fiveStarRecords) {
                if (!poolType.equals(record.getPoolType())) continue;
                Map<String, Object> itemInfo = new HashMap<>();
                itemInfo.put("name", record.getItemName());
                itemInfo.put("pityCount", record.getPityCount());
                itemInfo.put("isLimited", record.getIsLimited());
                itemInfo.put("poolType", record.getPoolType());
                itemInfo.put("itemType", record.getItemType());
                itemInfo.put("imageUrl", imageUrlMap.get(record.getItemName()));
                itemInfo.put("createdAt", record.getCreatedAt() != null
                        ? record.getCreatedAt().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null);
                poolItems.add(itemInfo);
            }
            poolGroupedItems.put(poolType, poolItems);
        }
        analysis.put("poolGroupedItems", poolGroupedItems);

        // 计算每个卡池的当前垫抽数
        Map<String, Integer> poolCurrentPity = new HashMap<>();
        for (String poolType : poolTypes) {
            poolCurrentPity.put(poolType, getCurrentPity(userId, poolType));
        }
        analysis.put("poolCurrentPity", poolCurrentPity);

        // 根据平均出金抽数计算称号
        String title;
        String titleDesc;
        if (avgFiveStarPity <= 30) {
            title = "万里挑一至尊欧皇";
            titleDesc = "你的运气简直逆天！";
        } else if (avgFiveStarPity <= 40) {
            title = "天选之子";
            titleDesc = "欧皇附体，运气极佳！";
        } else if (avgFiveStarPity <= 50) {
            title = "运气极佳";
            titleDesc = "你的运气很不错哦！";
        } else if (avgFiveStarPity <= 60) {
            title = "中规中矩";
            titleDesc = "运气一般般，继续努力！";
        } else if (avgFiveStarPity <= 70) {
            title = "有点非酋";
            titleDesc = "运气不太好，下次一定！";
        } else if (avgFiveStarPity <= 80) {
            title = "非酋本酋";
            titleDesc = "你的运气需要提升了！";
        } else {
            title = "究极非酋";
            titleDesc = "你就是传说中的非酋之王！";
        }
        analysis.put("title", title);
        analysis.put("titleDesc", titleDesc);

        return analysis;
    }
}
