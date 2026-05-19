-- ============================================================
-- 迁移脚本：special-activity → special-character + special-weapon
-- 执行前请先备份数据库！
-- ============================================================

USE wuthering_waves_gacha;

-- ========== 1. 新增物品分类 ==========
INSERT IGNORE INTO item_category (id, name, rarity, item_type, description, sort_order) VALUES
(10, '三星特殊武器', 3, 'weapon', '三星特殊活动武器', 10),
(11, '四星特殊角色', 4, 'character', '特殊卡池四星角色', 11),
(12, '四星特殊武器', 4, 'weapon', '特殊卡池四星武器', 12);

-- ========== 2. 新增占位物品 ==========
INSERT IGNORE INTO gacha_items (id, name, rarity, item_type, category_id, image_url) VALUES
-- 三星特殊武器 (category_id=10)
(125, '特殊三星武器A', 3, 'weapon', 10, '/images/weapons/special_3star_a.png'),
(126, '特殊三星武器B', 3, 'weapon', 10, '/images/weapons/special_3star_b.png'),
(127, '特殊三星武器C', 3, 'weapon', 10, '/images/weapons/special_3star_c.png'),
-- 四星特殊角色 (category_id=11)
(128, '特殊四星角色A', 4, 'character', 11, '/images/avatars/special_4star_char_a.png'),
(129, '特殊四星角色B', 4, 'character', 11, '/images/avatars/special_4star_char_b.png'),
-- 四星特殊武器 (category_id=12)
(130, '特殊四星武器A', 4, 'weapon', 12, '/images/weapons/special_4star_weapon_a.png'),
(131, '特殊四星武器B', 4, 'weapon', 12, '/images/weapons/special_4star_weapon_b.png');

-- ========== 3. 删除旧的 special-activity 卡池及其关联 ==========

-- 先删除 pool_category 关联
DELETE pc FROM pool_category pc
INNER JOIN gacha_pool gp ON pc.pool_id = gp.id
WHERE gp.pool_type = 'special-activity';

-- 再删除卡池本身
DELETE FROM gacha_pool WHERE pool_type = 'special-activity';

-- ========== 4. 插入新的特殊角色卡池 ==========
INSERT INTO gacha_pool (name, pool_type, description, five_star_rate, four_star_rate,
    max_pity, soft_pity_start, soft_pity_increment, status, sidebar_visible,
    sidebar_order, allow_lose, fivestar_up)
VALUES ('特殊角色唤取', 'special-character', '特殊角色卡池', 0.80, 6.00,
    80, 65, 6.00, 'active', TRUE, 5, TRUE, 121);

SET @special_char_pool_id = LAST_INSERT_ID();

INSERT IGNORE INTO pool_category (pool_id, category_id) VALUES
(@special_char_pool_id, 10),  -- 三星特殊武器
(@special_char_pool_id, 11),  -- 四星特殊角色
(@special_char_pool_id, 4),   -- 五星常驻角色（歪的候选）
(@special_char_pool_id, 8);   -- 五星特殊角色（UP）

-- ========== 5. 插入新的特殊武器卡池 ==========
INSERT INTO gacha_pool (name, pool_type, description, five_star_rate, four_star_rate,
    max_pity, soft_pity_start, soft_pity_increment, status, sidebar_visible,
    sidebar_order, allow_lose, fivestar_up)
VALUES ('特殊武器唤取', 'special-weapon', '特殊武器卡池', 0.80, 6.00,
    80, 65, 6.00, 'active', TRUE, 6, TRUE, 123);

SET @special_wpn_pool_id = LAST_INSERT_ID();

INSERT IGNORE INTO pool_category (pool_id, category_id) VALUES
(@special_wpn_pool_id, 10),  -- 三星特殊武器
(@special_wpn_pool_id, 12),  -- 四星特殊武器
(@special_wpn_pool_id, 5),   -- 五星常驻武器（歪的候选）
(@special_wpn_pool_id, 9);   -- 五星特殊武器（UP）

-- ========== 6. 迁移旧保底数据 ==========

-- 为每个有 special-activity 保底记录的用户，复制到 special-character 和 special-weapon
INSERT INTO gacha_pity (user_id, pool_type, five_star_count, four_star_count, guaranteed_five, guaranteed_four)
SELECT user_id, 'special-character', five_star_count, four_star_count, guaranteed_five, guaranteed_four
FROM gacha_pity WHERE pool_type = 'special-activity';

INSERT INTO gacha_pity (user_id, pool_type, five_star_count, four_star_count, guaranteed_five, guaranteed_four)
SELECT user_id, 'special-weapon', five_star_count, four_star_count, guaranteed_five, guaranteed_four
FROM gacha_pity WHERE pool_type = 'special-activity';

-- 删除旧的 special-activity 保底记录
DELETE FROM gacha_pity WHERE pool_type = 'special-activity';

-- ========== 7. 迁移旧抽卡记录 ==========

-- 将旧的 special-activity 记录归入 special-character（可根据需要调整）
UPDATE gacha_records SET pool_type = 'special-character' WHERE pool_type = 'special-activity';

-- ========== 完成 ==========
SELECT '迁移完成！' AS status;
