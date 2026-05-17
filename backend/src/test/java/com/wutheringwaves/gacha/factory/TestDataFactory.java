package com.wutheringwaves.gacha.factory;

import com.wutheringwaves.gacha.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public class TestDataFactory {

    public static User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("testuser_" + id);
        user.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        user.setRole("user");
        user.setStarlight(1600);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    public static User createAdmin(Long id) {
        User user = createUser(id);
        user.setUsername("admin_" + id);
        user.setRole("ADMIN");
        user.setStarlight(10000);
        return user;
    }

    public static GachaPool createPool(String type, String name) {
        GachaPool pool = new GachaPool();
        pool.setId(1L);
        pool.setName(name);
        pool.setPoolType(type);
        pool.setDescription("测试卡池");
        pool.setFiveStarRate(new BigDecimal("0.80"));
        pool.setFourStarRate(new BigDecimal("6.00"));
        pool.setMaxPity(90);
        pool.setSoftPityStart(75);
        pool.setSoftPityIncrement(new BigDecimal("6.00"));
        pool.setStatus("active");
        pool.setFivestarUp(1001L);
        pool.setFourstarUp(null);
        pool.setCreatedAt(LocalDateTime.now());
        pool.setUpdatedAt(LocalDateTime.now());
        return pool;
    }

    public static GachaItem createItem(Long id, int rarity, String name, String itemType) {
        return createItem(id, rarity, name, itemType, null);
    }

    public static GachaItem createItem(Long id, int rarity, String name, String itemType, Long categoryId) {
        GachaItem item = new GachaItem();
        item.setId(id);
        item.setName(name);
        item.setRarity(rarity);
        item.setItemType(itemType);
        item.setCategoryId(categoryId);
        return item;
    }

    public static ItemCategory createCategory(Long id, String name, int rarity, String itemType) {
        ItemCategory cat = new ItemCategory();
        cat.setId(id);
        cat.setName(name);
        cat.setRarity(rarity);
        cat.setItemType(itemType);
        cat.setSortOrder(id.intValue());
        return cat;
    }

    public static PoolCategory createPoolCategory(Long poolId, Long categoryId) {
        PoolCategory pc = new PoolCategory();
        pc.setPoolId(poolId);
        pc.setCategoryId(categoryId);
        return pc;
    }

    public static List<GachaItem> createCharacterPoolItems() {
        List<GachaItem> items = new ArrayList<>();
        items.add(createItem(1001L, 5, "限定五星角色", "character", 7L));
        items.add(createItem(1002L, 5, "常驻五星角色", "character", 4L));
        items.add(createItem(2001L, 4, "四星角色A", "character", 2L));
        items.add(createItem(2002L, 4, "四星角色B", "character", 2L));
        items.add(createItem(2003L, 4, "四星武器A", "weapon", 3L));
        items.add(createItem(3001L, 3, "三星武器A", "weapon", 1L));
        items.add(createItem(3002L, 3, "三星武器B", "weapon", 1L));
        items.add(createItem(3003L, 3, "三星武器C", "weapon", 1L));
        return items;
    }

    public static List<GachaItem> createWeaponPoolItems() {
        List<GachaItem> items = new ArrayList<>();
        items.add(createItem(1003L, 5, "限定五星武器", "weapon", 6L));
        items.add(createItem(1004L, 5, "常驻五星武器", "weapon", 5L));
        items.add(createItem(2004L, 4, "四星武器A", "weapon", 3L));
        items.add(createItem(2005L, 4, "四星武器B", "weapon", 3L));
        items.add(createItem(2006L, 4, "四星角色A", "character", 2L));
        items.add(createItem(3004L, 3, "三星武器A", "weapon", 1L));
        items.add(createItem(3005L, 3, "三星武器B", "weapon", 1L));
        items.add(createItem(3006L, 3, "三星武器C", "weapon", 1L));
        return items;
    }

    public static List<PoolCategory> createCharacterPoolCategories() {
        return List.of(
                createPoolCategory(1L, 1L),
                createPoolCategory(1L, 2L),
                createPoolCategory(1L, 4L),
                createPoolCategory(1L, 7L)
        );
    }

    public static List<PoolCategory> createWeaponPoolCategories() {
        return List.of(
                createPoolCategory(2L, 1L),
                createPoolCategory(2L, 3L),
                createPoolCategory(2L, 5L),
                createPoolCategory(2L, 6L)
        );
    }

    public static GachaPity createGachaPity(Long userId, String poolType, int fiveStarCount, int fourStarCount, boolean guaranteed) {
        GachaPity pity = new GachaPity();
        pity.setUserId(userId);
        pity.setPoolType(poolType);
        pity.setFiveStarCount(fiveStarCount);
        pity.setFourStarCount(fourStarCount);
        pity.setGuaranteedFive(guaranteed);
        pity.setGuaranteedFour(false);
        pity.setUpdatedAt(LocalDateTime.now());
        return pity;
    }

    public static GachaRecord createRecord(Long userId, String poolType, String itemName, int itemRarity, String itemType) {
        GachaRecord record = new GachaRecord();
        record.setUserId(userId);
        record.setPoolType(poolType);
        record.setItemName(itemName);
        record.setItemRarity(itemRarity);
        record.setItemType(itemType);
        record.setIsLimited(false);
        record.setPityCount(0);
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }
}
