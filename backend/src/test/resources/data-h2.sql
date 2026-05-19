-- 插入测试用户 (密码为 'password123' 的 BCrypt 加密)
INSERT INTO users (id, username, password, role, starlight) VALUES
(1, 'testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user', 1600);
INSERT INTO users (id, username, password, role, starlight) VALUES
(2, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 10000);

-- 插入物品分类
INSERT INTO item_category (id, name, rarity, item_type, description, sort_order) VALUES
(1, '三星武器', 3, 'weapon', '三星通用武器', 1),
(2, '四星角色', 4, 'character', '四星角色', 2),
(3, '四星武器', 4, 'weapon', '四星武器', 3),
(4, '五星常驻角色', 5, 'character', '常驻池可出的五星角色', 4),
(5, '五星常驻武器', 5, 'weapon', '常驻池可出的五星武器', 5),
(6, '五星限定武器', 5, 'weapon', '限定池专精五星武器', 6),
(7, '五星限定角色', 5, 'character', '限定UP五星角色', 7),
(8, '五星特殊角色', 5, 'character', '特殊卡池限定五星角色', 8),
(9, '五星特殊武器', 5, 'weapon', '特殊卡池限定五星武器', 9),
(10, '三星特殊武器', 3, 'weapon', '三星特殊活动武器', 10),
(11, '四星特殊角色', 4, 'character', '特殊卡池四星角色', 11),
(12, '四星特殊武器', 4, 'weapon', '特殊卡池四星武器', 12);

-- 插入卡池配置（含UP物品）
INSERT INTO gacha_pool (id, name, pool_type, description, five_star_rate, four_star_rate, max_pity, soft_pity_start, soft_pity_increment, status, fivestar_up, fourstar_up, allow_lose) VALUES
(1, '限定角色池', 'limited-character', '限定角色UP池', 0.80, 6.00, 80, 65, 6.00, 'active', 1001, NULL, TRUE),
(2, '限定武器池', 'limited-weapon', '限定武器UP池', 0.80, 6.00, 80, 65, 6.00, 'active', 1003, NULL, TRUE),
(3, '常驻角色池', 'standard-character', '常驻角色池', 0.80, 6.00, 80, 65, 6.00, 'active', NULL, NULL, TRUE),
(4, '常驻武器池', 'standard-weapon', '常驻武器池', 0.80, 6.00, 80, 65, 6.00, 'active', NULL, NULL, TRUE),
(5, '特殊角色唤取', 'special-character', '特殊角色卡池', 0.80, 6.00, 80, 65, 6.00, 'active', 1005, NULL, TRUE),
(6, '特殊武器唤取', 'special-weapon', '特殊武器卡池', 0.80, 6.00, 80, 65, 6.00, 'active', 1007, NULL, TRUE);

-- 插入抽卡物品（统一物品表）
INSERT INTO gacha_items (id, name, rarity, item_type, category_id) VALUES
(1001, '限定五星角色', 5, 'character', 7),
(1002, '常驻五星角色', 5, 'character', 4),
(1003, '限定五星武器', 5, 'weapon', 6),
(1004, '常驻五星武器', 5, 'weapon', 5),
(2001, '四星角色A', 4, 'character', 2),
(2002, '四星角色B', 4, 'character', 2),
(2003, '四星武器A', 4, 'weapon', 3),
(2004, '四星武器B', 4, 'weapon', 3),
(3001, '三星武器A', 3, 'weapon', 1),
(3002, '三星武器B', 3, 'weapon', 1),
(3003, '三星武器C', 3, 'weapon', 1),
(1005, '特殊五星角色A', 5, 'character', 8),
(1006, '特殊五星角色B', 5, 'character', 8),
(1007, '特殊五星武器A', 5, 'weapon', 9),
(1008, '特殊五星武器B', 5, 'weapon', 9),
(4001, '特殊三星武器A', 3, 'weapon', 10),
(4002, '特殊三星武器B', 3, 'weapon', 10),
(5001, '特殊四星角色A', 4, 'character', 11),
(5002, '特殊四星角色B', 4, 'character', 11),
(6001, '特殊四星武器A', 4, 'weapon', 12),
(6002, '特殊四星武器B', 4, 'weapon', 12);

-- 插入卡池-分类关联
INSERT INTO pool_category (pool_id, category_id) VALUES
-- 限定角色池：三星武器 + 四星角色 + 五星常驻角色 + 五星限定角色
(1, 1), (1, 2), (1, 4), (1, 7),
-- 限定武器池：三星武器 + 四星武器 + 五星常驻武器 + 五星限定武器
(2, 1), (2, 3), (2, 5), (2, 6),
-- 常驻角色池：三星武器 + 四星角色 + 五星常驻角色 + 五星限定角色
(3, 1), (3, 2), (3, 4), (3, 7),
-- 常驻武器池：三星武器 + 四星武器 + 五星常驻武器 + 五星限定武器
(4, 1), (4, 3), (4, 5), (4, 6),
-- 特殊角色池：三星特殊武器 + 四星特殊角色 + 五星常驻角色（歪） + 五星特殊角色（UP）
(5, 10), (5, 11), (5, 4), (5, 8),
-- 特殊武器池：三星特殊武器 + 四星特殊武器 + 五星常驻武器（歪） + 五星特殊武器（UP）
(6, 10), (6, 12), (6, 5), (6, 9);

-- 初始化保底计数器
INSERT INTO gacha_pity (user_id, pool_type, five_star_count, four_star_count, guaranteed_five, guaranteed_four) VALUES
(1, 'limited-character', 0, 0, FALSE, FALSE),
(1, 'limited-weapon', 0, 0, FALSE, FALSE),
(1, 'standard-character', 0, 0, FALSE, FALSE),
(1, 'standard-weapon', 0, 0, FALSE, FALSE),
(2, 'limited-character', 0, 0, FALSE, FALSE),
(2, 'limited-weapon', 0, 0, FALSE, FALSE),
(2, 'standard-character', 0, 0, FALSE, FALSE),
(2, 'standard-weapon', 0, 0, FALSE, FALSE),
(1, 'special-character', 0, 0, FALSE, FALSE),
(1, 'special-weapon', 0, 0, FALSE, FALSE),
(2, 'special-character', 0, 0, FALSE, FALSE),
(2, 'special-weapon', 0, 0, FALSE, FALSE);
