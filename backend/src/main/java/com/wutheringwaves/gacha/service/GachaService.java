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
    private final CharacterPityMapper characterPityMapper;
    private final WeaponPityMapper weaponPityMapper;
    private final LimitedPityMapper limitedPityMapper;
    private final UserService userService;

    private static final int CHARACTER_MAX_PITY = 90;
    private static final int WEAPON_MAX_PITY = 80;

    @Transactional
    public Map<String, Object> pull(Long userId, String poolType, int count) {
        Map<String, Object> result = new HashMap<>();

        // 检查星声是否足够
        User user = userService.getUserById(userId);
        int cost = count == 10 ? 1500 : 160;  // 十连 1500，单抽 160
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

        // 四星给星辉，五星给星辉
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
        // 获取保底信息
        int currentPity = getCurrentPity(userId, poolType);
        boolean guaranteed = isGuaranteed(userId, poolType);

        // 判断出货稀有度
        int rarity = determineRarity(userId, poolType, currentPity, guaranteed);

        // 从对应稀有度中随机选择物品
        GachaItem selectedItem = selectItem(items, rarity, poolType, guaranteed);

        // 更新保底计数
        updatePity(userId, poolType, rarity, selectedItem.getIsLimited());

        // 记录抽卡结果
        GachaRecord record = new GachaRecord();
        record.setUserId(userId);
        record.setPoolType(poolType);
        record.setItemName(selectedItem.getName());
        record.setItemRarity(selectedItem.getRarity());
        record.setItemType(selectedItem.getItemType());
        record.setIsLimited(selectedItem.getIsLimited());
        record.setPityCount(currentPity + 1);
        gachaRecordMapper.insert(record);

        // 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("name", selectedItem.getName());
        result.put("rarity", selectedItem.getRarity());
        result.put("type", selectedItem.getItemType());
        result.put("isLimited", selectedItem.getIsLimited());
        result.put("pityCount", currentPity + 1);

        return result;
    }

    private int determineRarity(Long userId, String poolType, int currentPity, boolean guaranteed) {
        double rand = ThreadLocalRandom.current().nextDouble() * 100;

        int maxPity = poolType.equals("weapon") ? WEAPON_MAX_PITY : CHARACTER_MAX_PITY;

        // 硬保底
        if (currentPity >= maxPity - 1) {
            return 5;
        }

        // 软保底（75抽后概率递增）
        if (currentPity >= 74) {
            double fiveStarChance = 0.8 + (currentPity - 74) * 6.0;
            if (rand < fiveStarChance) {
                return 5;
            }
        }

        // 基础概率
        double fiveStarRate = poolType.equals("weapon") ? 0.7 : 0.8;
        double fourStarRate = 6.0;

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

    private GachaItem selectItem(List<GachaItem> items, int rarity, String poolType, boolean guaranteed) {
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

        // 如果是大保底且是五星，优先出限定
        if (rarity == 5 && guaranteed) {
            List<GachaItem> limited = candidates.stream()
                    .filter(GachaItem::getIsLimited)
                    .toList();
            if (!limited.isEmpty()) {
                return limited.get(ThreadLocalRandom.current().nextInt(limited.size()));
            }
        }

        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private List<GachaItem> getItemsByPool(String poolType) {
        LambdaQueryWrapper<GachaItem> wrapper = new LambdaQueryWrapper<>();
        if (poolType.equals("limited")) {
            wrapper.eq(GachaItem::getPoolType, "limited");
        } else if (poolType.equals("weapon")) {
            wrapper.eq(GachaItem::getPoolType, "weapon");
        } else {
            wrapper.eq(GachaItem::getPoolType, "character");
        }
        return gachaItemMapper.selectList(wrapper);
    }

    private int getCurrentPity(Long userId, String poolType) {
        return switch (poolType) {
            case "character" -> {
                CharacterPity pity = characterPityMapper.selectById(userId);
                yield pity != null ? pity.getFiveStarCount() : 0;
            }
            case "weapon" -> {
                WeaponPity pity = weaponPityMapper.selectById(userId);
                yield pity != null ? pity.getFiveStarCount() : 0;
            }
            case "limited" -> {
                LimitedPity pity = limitedPityMapper.selectById(userId);
                yield pity != null ? pity.getFiveStarCount() : 0;
            }
            default -> 0;
        };
    }

    private int getFourStarPity(Long userId, String poolType) {
        return switch (poolType) {
            case "character" -> {
                CharacterPity pity = characterPityMapper.selectById(userId);
                yield pity != null ? pity.getFourStarCount() : 0;
            }
            case "weapon" -> {
                WeaponPity pity = weaponPityMapper.selectById(userId);
                yield pity != null ? pity.getFourStarCount() : 0;
            }
            case "limited" -> {
                LimitedPity pity = limitedPityMapper.selectById(userId);
                yield pity != null ? pity.getFourStarCount() : 0;
            }
            default -> 0;
        };
    }

    private boolean isGuaranteed(Long userId, String poolType) {
        return switch (poolType) {
            case "character" -> {
                CharacterPity pity = characterPityMapper.selectById(userId);
                yield pity != null && pity.getGuaranteedFive();
            }
            case "weapon" -> {
                WeaponPity pity = weaponPityMapper.selectById(userId);
                yield pity != null && pity.getGuaranteedFive();
            }
            case "limited" -> {
                LimitedPity pity = limitedPityMapper.selectById(userId);
                yield pity != null && pity.getGuaranteedFive();
            }
            default -> false;
        };
    }

    private void updatePity(Long userId, String poolType, int rarity, boolean isLimited) {
        if (rarity == 5) {
            // 五星重置保底
            switch (poolType) {
                case "character" -> {
                    CharacterPity pity = characterPityMapper.selectById(userId);
                    if (pity == null) {
                        pity = new CharacterPity();
                        pity.setUserId(userId);
                    }
                    pity.setFiveStarCount(0);
                    pity.setFourStarCount(pity.getFourStarCount() + 1);
                    pity.setGuaranteedFive(!isLimited);  // 如果不是限定，下次大保底
                    characterPityMapper.insertOrUpdate(pity);
                }
                case "weapon" -> {
                    WeaponPity pity = weaponPityMapper.selectById(userId);
                    if (pity == null) {
                        pity = new WeaponPity();
                        pity.setUserId(userId);
                    }
                    pity.setFiveStarCount(0);
                    pity.setFourStarCount(pity.getFourStarCount() + 1);
                    pity.setGuaranteedFive(!isLimited);
                    weaponPityMapper.insertOrUpdate(pity);
                }
                case "limited" -> {
                    LimitedPity pity = limitedPityMapper.selectById(userId);
                    if (pity == null) {
                        pity = new LimitedPity();
                        pity.setUserId(userId);
                    }
                    pity.setFiveStarCount(0);
                    pity.setFourStarCount(pity.getFourStarCount() + 1);
                    pity.setGuaranteedFive(!isLimited);
                    limitedPityMapper.insertOrUpdate(pity);
                }
            }
        } else if (rarity == 4) {
            // 四星重置
            switch (poolType) {
                case "character" -> {
                    CharacterPity pity = characterPityMapper.selectById(userId);
                    if (pity == null) {
                        pity = new CharacterPity();
                        pity.setUserId(userId);
                    }
                    pity.setFiveStarCount(pity.getFiveStarCount() + 1);
                    pity.setFourStarCount(0);
                    characterPityMapper.insertOrUpdate(pity);
                }
                case "weapon" -> {
                    WeaponPity pity = weaponPityMapper.selectById(userId);
                    if (pity == null) {
                        pity = new WeaponPity();
                        pity.setUserId(userId);
                    }
                    pity.setFiveStarCount(pity.getFiveStarCount() + 1);
                    pity.setFourStarCount(0);
                    weaponPityMapper.insertOrUpdate(pity);
                }
                case "limited" -> {
                    LimitedPity pity = limitedPityMapper.selectById(userId);
                    if (pity == null) {
                        pity = new LimitedPity();
                        pity.setUserId(userId);
                    }
                    pity.setFiveStarCount(pity.getFiveStarCount() + 1);
                    pity.setFourStarCount(0);
                    limitedPityMapper.insertOrUpdate(pity);
                }
            }
        } else {
            // 三星递增计数
            switch (poolType) {
                case "character" -> {
                    CharacterPity pity = characterPityMapper.selectById(userId);
                    if (pity == null) {
                        pity = new CharacterPity();
                        pity.setUserId(userId);
                    }
                    pity.setFiveStarCount(pity.getFiveStarCount() + 1);
                    pity.setFourStarCount(pity.getFourStarCount() + 1);
                    characterPityMapper.insertOrUpdate(pity);
                }
                case "weapon" -> {
                    WeaponPity pity = weaponPityMapper.selectById(userId);
                    if (pity == null) {
                        pity = new WeaponPity();
                        pity.setUserId(userId);
                    }
                    pity.setFiveStarCount(pity.getFiveStarCount() + 1);
                    pity.setFourStarCount(pity.getFourStarCount() + 1);
                    weaponPityMapper.insertOrUpdate(pity);
                }
                case "limited" -> {
                    LimitedPity pity = limitedPityMapper.selectById(userId);
                    if (pity == null) {
                        pity = new LimitedPity();
                        pity.setUserId(userId);
                    }
                    pity.setFiveStarCount(pity.getFiveStarCount() + 1);
                    pity.setFourStarCount(pity.getFourStarCount() + 1);
                    limitedPityMapper.insertOrUpdate(pity);
                }
            }
        }
    }

    public List<GachaRecord> getHistory(Long userId, String poolType, int page, int size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<GachaRecord> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        LambdaQueryWrapper<GachaRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GachaRecord::getUserId, userId);
        if (poolType != null && !poolType.isEmpty()) {
            wrapper.eq(GachaRecord::getPoolType, poolType);
        }
        wrapper.orderByDesc(GachaRecord::getCreatedAt);
        return gachaRecordMapper.selectPage(pageParam, wrapper).getRecords();
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

        // 获取当前保底计数（仅在指定池子时返回）
        if (poolType != null && !poolType.isEmpty()) {
            int currentPity = getCurrentPity(userId, poolType);
            stats.put("currentPity", currentPity);
        }

        return stats;
    }
}
