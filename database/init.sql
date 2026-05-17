-- 鸣潮抽卡系统数据库初始化脚本

CREATE DATABASE IF NOT EXISTS wuthering_waves_gacha DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE wuthering_waves_gacha;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'user' COMMENT '角色：user/admin',
    starlight INT DEFAULT 100000 COMMENT '星声（抽卡货币）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 预置管理员账户（admin/123456）
INSERT INTO users (username, password, role, starlight) VALUES
('admin', '$2a$10$Q/6lluMY0rON1Q//Tvf0k.0xaAbYKkAYfphbajDj3LG5xPvldhGg.', 'admin', 999999)
ON DUPLICATE KEY UPDATE role = 'admin';

-- 抽卡记录表
CREATE TABLE IF NOT EXISTS gacha_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    pool_type VARCHAR(30) NOT NULL COMMENT '池子类型：limited-character/limited-weapon/standard-character/standard-weapon',
    item_name VARCHAR(100) NOT NULL COMMENT '物品名称',
    item_rarity INT NOT NULL COMMENT '稀有度：3/4/5',
    item_type VARCHAR(20) NOT NULL COMMENT '类型：character/weapon',
    is_limited BOOLEAN DEFAULT FALSE COMMENT '是否为限定物品',
    pity_count INT COMMENT '当前保底计数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_pool_type (pool_type),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 统一保底计数表（替代原 character_pity / weapon_pity / limited_pity）
CREATE TABLE IF NOT EXISTS gacha_pity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    pool_type VARCHAR(30) NOT NULL COMMENT '池子类型：limited-character/limited-weapon/standard-character/standard-weapon',
    five_star_count INT DEFAULT 0 COMMENT '距离五星保底累计抽数',
    four_star_count INT DEFAULT 0 COMMENT '距离四星保底累计抽数',
    guaranteed_five BOOLEAN DEFAULT FALSE COMMENT '是否大保底（下次必出UP）',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_pool (user_id, pool_type)
);

-- 卡池配置表
CREATE TABLE IF NOT EXISTS gacha_pool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '卡池名称',
    pool_type VARCHAR(30) NOT NULL COMMENT '池子类型：limited-character/limited-weapon/standard-character/standard-weapon',
    description TEXT COMMENT '卡池描述',
    image_url VARCHAR(255) COMMENT '卡池封面图片路径',
    five_star_rate DECIMAL(5,2) DEFAULT 0.80 COMMENT '五星基础概率(%)',
    four_star_rate DECIMAL(5,2) DEFAULT 6.00 COMMENT '四星基础概率(%)',
    max_pity INT DEFAULT 90 COMMENT '硬保底抽数',
    soft_pity_start INT DEFAULT 75 COMMENT '软保底起始抽数',
    soft_pity_increment DECIMAL(5,2) DEFAULT 6.00 COMMENT '软保底每抽概率递增(%)',
    up_items VARCHAR(500) COMMENT 'UP物品名称列表（JSON数组）',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active/inactive',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pool_type (pool_type),
    INDEX idx_status (status)
);

-- 初始卡池数据
INSERT INTO gacha_pool (name, pool_type, description, five_star_rate, four_star_rate, max_pity, soft_pity_start, soft_pity_increment, up_items, status) VALUES
('限定角色活动唤取', 'limited-character', '限定五星角色概率UP', 0.80, 6.00, 90, 75, 6.00, '["吟霖"]', 'active'),
('限定武器活动唤取', 'limited-weapon', '限定五星武器概率UP', 0.70, 6.00, 80, 65, 7.00, '["吟霖的专武"]', 'active'),
('常驻角色唤取', 'standard-character', '常驻五星角色', 0.80, 6.00, 90, 75, 6.00, NULL, 'active'),
('常驻武器唤取', 'standard-weapon', '常驻五星武器', 0.70, 6.00, 80, 65, 7.00, NULL, 'active');

-- 抽卡物品配置表
CREATE TABLE IF NOT EXISTS gacha_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    rarity INT NOT NULL COMMENT '稀有度：3/4/5',
    item_type VARCHAR(20) NOT NULL COMMENT '类型：character/weapon',
    pool_type VARCHAR(30) NOT NULL COMMENT '所属池子：limited-character/limited-weapon/standard-character/standard-weapon',
    is_limited BOOLEAN DEFAULT FALSE COMMENT '是否为限定物品',
    image_url VARCHAR(255) COMMENT '图片路径',
    description TEXT COMMENT '描述'
);

-- 插入默认抽卡物品数据
INSERT INTO gacha_items (name, rarity, item_type, pool_type, is_limited) VALUES
-- ===== 限定角色池 =====
-- 三星武器（填充物）
('训练佩刀', 3, 'weapon', 'limited-character', FALSE),
('训练长刃', 3, 'weapon', 'limited-character', FALSE),
('训练拳套', 3, 'weapon', 'limited-character', FALSE),
('训练佩枪', 3, 'weapon', 'limited-character', FALSE),
('训练音感仪', 3, 'weapon', 'limited-character', FALSE),
-- 四星角色
('丹瑾', 4, 'character', 'limited-character', FALSE),
('白芷', 4, 'character', 'limited-character', FALSE),
('散华', 4, 'character', 'limited-character', FALSE),
('秋水', 4, 'character', 'limited-character', FALSE),
('渊武', 4, 'character', 'limited-character', FALSE),
('莫特斐', 4, 'character', 'limited-character', FALSE),
('炽霞', 4, 'character', 'limited-character', FALSE),
('秧秧', 4, 'character', 'limited-character', FALSE),
('桃祈', 4, 'character', 'limited-character', FALSE),
-- 五星常驻角色（歪出）
('漂泊者', 5, 'character', 'limited-character', FALSE),
('安可', 5, 'character', 'limited-character', FALSE),
('维里奈', 5, 'character', 'limited-character', FALSE),
('卡卡罗', 5, 'character', 'limited-character', FALSE),
('鉴心', 5, 'character', 'limited-character', FALSE),
('凌阳', 5, 'character', 'limited-character', FALSE),
-- 限定UP角色
('吟霖', 5, 'character', 'limited-character', TRUE),

-- ===== 限定武器池 =====
-- 三星武器（填充物）
('训练佩刀', 3, 'weapon', 'limited-weapon', FALSE),
('训练长刃', 3, 'weapon', 'limited-weapon', FALSE),
('训练拳套', 3, 'weapon', 'limited-weapon', FALSE),
('训练佩枪', 3, 'weapon', 'limited-weapon', FALSE),
('训练音感仪', 3, 'weapon', 'limited-weapon', FALSE),
-- 四星武器
('不归孤军', 4, 'weapon', 'limited-weapon', FALSE),
('永夜星辉', 4, 'weapon', 'limited-weapon', FALSE),
('行进序曲', 4, 'weapon', 'limited-weapon', FALSE),
('今州守望', 4, 'weapon', 'limited-weapon', FALSE),
('骇行', 4, 'weapon', 'limited-weapon', FALSE),
-- 五星常驻武器（歪出）
('浩境粼光', 5, 'weapon', 'limited-weapon', FALSE),
('漪澜浮录', 5, 'weapon', 'limited-weapon', FALSE),
('琼枝冰绡', 5, 'weapon', 'limited-weapon', FALSE),
('悲喜剧', 5, 'weapon', 'limited-weapon', FALSE),
('千古洑流', 5, 'weapon', 'limited-weapon', FALSE),
-- 限定UP武器
('吟霖的专武', 5, 'weapon', 'limited-weapon', TRUE),

-- ===== 常驻角色池 =====
-- 三星武器（填充物）
('训练佩刀', 3, 'weapon', 'standard-character', FALSE),
('训练长刃', 3, 'weapon', 'standard-character', FALSE),
('训练拳套', 3, 'weapon', 'standard-character', FALSE),
('训练佩枪', 3, 'weapon', 'standard-character', FALSE),
('训练音感仪', 3, 'weapon', 'standard-character', FALSE),
-- 四星角色
('丹瑾', 4, 'character', 'standard-character', FALSE),
('白芷', 4, 'character', 'standard-character', FALSE),
('散华', 4, 'character', 'standard-character', FALSE),
('秋水', 4, 'character', 'standard-character', FALSE),
('渊武', 4, 'character', 'standard-character', FALSE),
('莫特斐', 4, 'character', 'standard-character', FALSE),
('炽霞', 4, 'character', 'standard-character', FALSE),
('秧秧', 4, 'character', 'standard-character', FALSE),
('桃祈', 4, 'character', 'standard-character', FALSE),
-- 五星常驻角色
('漂泊者', 5, 'character', 'standard-character', FALSE),
('安可', 5, 'character', 'standard-character', FALSE),
('维里奈', 5, 'character', 'standard-character', FALSE),
('卡卡罗', 5, 'character', 'standard-character', FALSE),
('鉴心', 5, 'character', 'standard-character', FALSE),
('凌阳', 5, 'character', 'standard-character', FALSE),

-- ===== 常驻武器池 =====
-- 三星武器（填充物）
('训练佩刀', 3, 'weapon', 'standard-weapon', FALSE),
('训练长刃', 3, 'weapon', 'standard-weapon', FALSE),
('训练拳套', 3, 'weapon', 'standard-weapon', FALSE),
('训练佩枪', 3, 'weapon', 'standard-weapon', FALSE),
('训练音感仪', 3, 'weapon', 'standard-weapon', FALSE),
-- 四星武器
('不归孤军', 4, 'weapon', 'standard-weapon', FALSE),
('永夜星辉', 4, 'weapon', 'standard-weapon', FALSE),
('行进序曲', 4, 'weapon', 'standard-weapon', FALSE),
('今州守望', 4, 'weapon', 'standard-weapon', FALSE),
('骇行', 4, 'weapon', 'standard-weapon', FALSE),
-- 五星常驻武器
('浩境粼光', 5, 'weapon', 'standard-weapon', FALSE),
('漪澜浮录', 5, 'weapon', 'standard-weapon', FALSE),
('琼枝冰绡', 5, 'weapon', 'standard-weapon', FALSE),
('悲喜剧', 5, 'weapon', 'standard-weapon', FALSE),
('千古洑流', 5, 'weapon', 'standard-weapon', FALSE);
