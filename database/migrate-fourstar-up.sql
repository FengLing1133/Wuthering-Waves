-- 四星UP关联表迁移脚本
-- 将 gacha_pool.fourstar_up 中逗号分隔的ID迁移到新的 gacha_pool_fourstar_up 表
-- 执行前请备份数据库！
-- 执行后请在 Java 代码中移除对 fourstar_up 列的读取，仅保留作为备份

USE wuthering_waves_gacha;

-- 迁移逗号分隔的 fourstar_up 数据
-- MySQL 8.0+ 支持递归 CTE 拆分逗号分隔字符串
INSERT INTO gacha_pool_fourstar_up (pool_id, item_id)
SELECT
    p.id AS pool_id,
    CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(p.fourstar_up, ',', n.n), ',', -1) AS UNSIGNED) AS item_id
FROM gacha_pool p
CROSS JOIN (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) n
WHERE p.fourstar_up IS NOT NULL
  AND p.fourstar_up != ''
  AND CHAR_LENGTH(p.fourstar_up) - CHAR_LENGTH(REPLACE(p.fourstar_up, ',', '')) >= n.n - 1
  AND CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(p.fourstar_up, ',', n.n), ',', -1) AS UNSIGNED) > 0
ON DUPLICATE KEY UPDATE item_id = item_id;

-- 验证迁移结果
-- SELECT p.id, p.name, p.fourstar_up, GROUP_CONCAT(f.item_id) AS migrated_ids
-- FROM gacha_pool p
-- LEFT JOIN gacha_pool_fourstar_up f ON f.pool_id = p.id
-- WHERE p.fourstar_up IS NOT NULL AND p.fourstar_up != ''
-- GROUP BY p.id;
