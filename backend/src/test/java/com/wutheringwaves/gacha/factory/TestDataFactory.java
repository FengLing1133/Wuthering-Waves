package com.wutheringwaves.gacha.factory;

import com.wutheringwaves.gacha.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TestDataFactory {

    public static User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("testuser_" + id);
        user.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        user.setRole("user");
        user.setStarlight(1600);
        user.setStarshards(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    public static User createAdmin(Long id) {
        User user = createUser(id);
        user.setUsername("admin_" + id);
        user.setRole("ADMIN");
        user.setStarlight(10000);
        user.setStarshards(100);
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
        pool.setUpItems("1001");
        pool.setStatus("active");
        pool.setCreatedAt(LocalDateTime.now());
        pool.setUpdatedAt(LocalDateTime.now());
        return pool;
    }

    public static GachaItem createItem(Long id, int rarity, String name, String itemType, String poolType) {
        GachaItem item = new GachaItem();
        item.setId(id);
        item.setName(name);
        item.setRarity(rarity);
        item.setItemType(itemType);
        item.setPoolType(poolType);
        item.setIsLimited(false);
        return item;
    }

    public static GachaItem createLimitedItem(Long id, int rarity, String name, String itemType, String poolType) {
        GachaItem item = createItem(id, rarity, name, itemType, poolType);
        item.setIsLimited(true);
        return item;
    }

    public static List<GachaItem> createCharacterPoolItems() {
        List<GachaItem> items = new ArrayList<>();
        items.add(createLimitedItem(1001L, 5, "限定五星角色", "character", "limited-character"));
        items.add(createItem(1002L, 5, "常驻五星角色", "character", "limited-character"));
        items.add(createItem(2001L, 4, "四星角色A", "character", "limited-character"));
        items.add(createItem(2002L, 4, "四星角色B", "character", "limited-character"));
        items.add(createItem(2003L, 4, "四星武器A", "weapon", "limited-character"));
        items.add(createItem(3001L, 3, "三星武器A", "weapon", "limited-character"));
        items.add(createItem(3002L, 3, "三星武器B", "weapon", "limited-character"));
        items.add(createItem(3003L, 3, "三星武器C", "weapon", "limited-character"));
        return items;
    }

    public static List<GachaItem> createWeaponPoolItems() {
        List<GachaItem> items = new ArrayList<>();
        items.add(createLimitedItem(1003L, 5, "限定五星武器", "weapon", "limited-weapon"));
        items.add(createItem(1004L, 5, "常驻五星武器", "weapon", "limited-weapon"));
        items.add(createItem(2004L, 4, "四星武器A", "weapon", "limited-weapon"));
        items.add(createItem(2005L, 4, "四星武器B", "weapon", "limited-weapon"));
        items.add(createItem(2006L, 4, "四星角色A", "character", "limited-weapon"));
        items.add(createItem(3004L, 3, "三星武器A", "weapon", "limited-weapon"));
        items.add(createItem(3005L, 3, "三星武器B", "weapon", "limited-weapon"));
        items.add(createItem(3006L, 3, "三星武器C", "weapon", "limited-weapon"));
        return items;
    }

    public static GachaPity createGachaPity(Long userId, String poolType, int fiveStarCount, int fourStarCount, boolean guaranteed) {
        GachaPity pity = new GachaPity();
        pity.setUserId(userId);
        pity.setPoolType(poolType);
        pity.setFiveStarCount(fiveStarCount);
        pity.setFourStarCount(fourStarCount);
        pity.setGuaranteedFive(guaranteed);
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
