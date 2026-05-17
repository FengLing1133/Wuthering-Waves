-- 插入测试用户 (密码为 'password123' 的 BCrypt 加密)
INSERT INTO users (id, username, password, role, starlight, starshards) VALUES
(1, 'testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user', 1600, 0);
INSERT INTO users (id, username, password, role, starlight, starshards) VALUES
(2, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', 10000, 100);

-- 插入卡池配置
INSERT INTO gacha_pool (id, name, pool_type, description, five_star_rate, four_star_rate, max_pity, soft_pity_start, soft_pity_increment, up_items, status) VALUES
(1, '限定角色池', 'character', '限定角色UP池', 0.80, 6.00, 90, 75, 6.00, '1001', 'active'),
(2, '武器池', 'weapon', '武器UP池', 0.70, 6.00, 80, 65, 7.00, '1002', 'active'),
(3, '常驻池', 'limited', '常驻角色池', 0.60, 5.14, 90, 75, 6.00, NULL, 'active');

-- 插入抽卡物品
INSERT INTO gacha_items (id, name, rarity, item_type, pool_type, is_limited) VALUES
(1001, '限定五星角色', 5, 'character', 'character', TRUE),
(1002, '常驻五星角色', 5, 'character', 'character', FALSE),
(1003, '五星武器A', 5, 'weapon', 'weapon', FALSE),
(2001, '四星角色A', 4, 'character', 'character', FALSE),
(2002, '四星角色B', 4, 'character', 'character', FALSE),
(2003, '四星武器A', 4, 'weapon', 'weapon', FALSE),
(2004, '四星武器B', 4, 'weapon', 'weapon', FALSE),
(3001, '三星武器A', 3, 'weapon', 'character', FALSE),
(3002, '三星武器B', 3, 'weapon', 'character', FALSE),
(3003, '三星武器C', 3, 'weapon', 'character', FALSE),
(3004, '三星武器D', 3, 'weapon', 'weapon', FALSE),
(3005, '三星武器E', 3, 'weapon', 'weapon', FALSE),
(3006, '三星武器F', 3, 'weapon', 'weapon', FALSE);

-- 初始化保底计数器
INSERT INTO character_pity (user_id, five_star_count, four_star_count, guaranteed_five) VALUES
(1, 0, 0, FALSE),
(2, 0, 0, FALSE);

INSERT INTO weapon_pity (user_id, five_star_count, four_star_count, guaranteed_five) VALUES
(1, 0, 0, FALSE),
(2, 0, 0, FALSE);

INSERT INTO limited_pity (user_id, five_star_count, four_star_count, guaranteed_five) VALUES
(1, 0, 0, FALSE),
(2, 0, 0, FALSE);
