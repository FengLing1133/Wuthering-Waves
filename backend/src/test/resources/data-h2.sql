-- 插入测试用户 (密码为 'password123' 的 BCrypt 加密)
INSERT INTO users (id, username, password, role, starlight, starshards) VALUES
(1, 'testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user', 1600, 0);
INSERT INTO users (id, username, password, role, starlight, starshards) VALUES
(2, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 10000, 100);

-- 插入卡池配置
INSERT INTO gacha_pool (id, name, pool_type, description, five_star_rate, four_star_rate, max_pity, soft_pity_start, soft_pity_increment, up_items, status) VALUES
(1, '限定角色池', 'limited-character', '限定角色UP池', 0.80, 6.00, 90, 75, 6.00, '1001', 'active'),
(2, '限定武器池', 'limited-weapon', '限定武器UP池', 0.70, 6.00, 80, 65, 7.00, '1003', 'active'),
(3, '常驻角色池', 'standard-character', '常驻角色池', 0.80, 6.00, 90, 75, 6.00, NULL, 'active'),
(4, '常驻武器池', 'standard-weapon', '常驻武器池', 0.70, 6.00, 80, 65, 7.00, NULL, 'active');

-- 插入抽卡物品
INSERT INTO gacha_items (id, name, rarity, item_type, pool_type, is_limited) VALUES
-- 限定角色池物品
(1001, '限定五星角色', 5, 'character', 'limited-character', TRUE),
(1002, '常驻五星角色', 5, 'character', 'limited-character', FALSE),
(2001, '四星角色A', 4, 'character', 'limited-character', FALSE),
(2002, '四星角色B', 4, 'character', 'limited-character', FALSE),
(3001, '三星武器A', 3, 'weapon', 'limited-character', FALSE),
(3002, '三星武器B', 3, 'weapon', 'limited-character', FALSE),
(3003, '三星武器C', 3, 'weapon', 'limited-character', FALSE),
-- 限定武器池物品
(1003, '限定五星武器', 5, 'weapon', 'limited-weapon', TRUE),
(1004, '常驻五星武器', 5, 'weapon', 'limited-weapon', FALSE),
(2003, '四星武器A', 4, 'weapon', 'limited-weapon', FALSE),
(2004, '四星武器B', 4, 'weapon', 'limited-weapon', FALSE),
(3004, '三星武器D', 3, 'weapon', 'limited-weapon', FALSE),
(3005, '三星武器E', 3, 'weapon', 'limited-weapon', FALSE),
(3006, '三星武器F', 3, 'weapon', 'limited-weapon', FALSE);

-- 初始化保底计数器
INSERT INTO gacha_pity (user_id, pool_type, five_star_count, four_star_count, guaranteed_five) VALUES
(1, 'limited-character', 0, 0, FALSE),
(1, 'limited-weapon', 0, 0, FALSE),
(1, 'standard-character', 0, 0, FALSE),
(1, 'standard-weapon', 0, 0, FALSE),
(2, 'limited-character', 0, 0, FALSE),
(2, 'limited-weapon', 0, 0, FALSE),
(2, 'standard-character', 0, 0, FALSE),
(2, 'standard-weapon', 0, 0, FALSE);
