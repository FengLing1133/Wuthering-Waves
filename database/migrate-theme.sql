-- 鸣潮主题数据库迁移脚本（适用于已存在的数据库）
-- 执行前请备份数据库

USE wuthering_waves_gacha;

-- 1. 插入鸣潮主题
INSERT INTO item_theme (id, name, description) VALUES (1, '鸣潮', '默认基础主题');

-- 2. 将已有分类 1-7 归入鸣潮主题
UPDATE item_category SET theme_id = 1 WHERE id IN (1, 2, 3, 4, 5, 6, 7);

-- 3. 删除特殊分类（确保无引用后执行）
-- 先检查是否有物品引用这些分类
-- SELECT * FROM gacha_items WHERE category_id IN (8, 9, 10, 11, 12);
-- 先检查是否有卡池引用这些分类
-- SELECT * FROM pool_category WHERE category_id IN (8, 9, 10, 11, 12);
DELETE FROM item_category WHERE id IN (8, 9, 10, 11, 12);
