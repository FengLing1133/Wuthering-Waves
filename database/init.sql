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
    starshards INT DEFAULT 0 COMMENT '星辉（兑换货币）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 预置管理员账户（admin/123456）
INSERT INTO users (username, password, role, starlight, starshards) VALUES
('admin', '$2a$10$Q/6lluMY0rON1Q//Tvf0k.0xaAbYKkAYfphbajDj3LG5xPvldhGg.', 'admin', 999999, 999999)
ON DUPLICATE KEY UPDATE role = 'admin';

-- 抽卡记录表
CREATE TABLE IF NOT EXISTS gacha_records (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    pool_type VARCHAR(20) NOT NULL COMMENT '池子类型：character/weapon/limited',
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

-- 角色池保底计数表
CREATE TABLE IF NOT EXISTS character_pity (
    user_id BIGINT PRIMARY KEY,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    five_star_count INT DEFAULT 0 COMMENT '距离五星保底剩余抽数',
    four_star_count INT DEFAULT 0 COMMENT '距离四星保底剩余抽数',
    guaranteed_five BOOLEAN DEFAULT FALSE COMMENT '是否大保底（下次必出UP）',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 武器池保底计数表
CREATE TABLE IF NOT EXISTS weapon_pity (
    user_id BIGINT PRIMARY KEY,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    five_star_count INT DEFAULT 0 COMMENT '距离五星保底剩余抽数',
    four_star_count INT DEFAULT 0 COMMENT '距离四星保底剩余抽数',
    guaranteed_five BOOLEAN DEFAULT FALSE COMMENT '是否大保底（下次必出UP）',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 限定池保底计数表
CREATE TABLE IF NOT EXISTS limited_pity (
    user_id BIGINT PRIMARY KEY,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    five_star_count INT DEFAULT 0 COMMENT '距离五星保底剩余抽数',
    four_star_count INT DEFAULT 0 COMMENT '距离四星保底剩余抽数',
    guaranteed_five BOOLEAN DEFAULT FALSE COMMENT '是否大保底（下次必出UP）',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 卡池配置表
CREATE TABLE IF NOT EXISTS gacha_pool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '卡池名称',
    pool_type VARCHAR(20) NOT NULL COMMENT '池子类型：character/weapon/limited',
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
('角色活动唤取', 'character', '限定角色概率UP！本期UP角色：吟霖', 0.80, 6.00, 90, 75, 6.00, '["吟霖"]', 'active'),
('武器活动唤取', 'weapon', '限定武器概率UP！', 0.70, 6.00, 80, 75, 6.00, '[]', 'active'),
('限定活动唤取', 'limited', '限定角色与武器概率UP！', 0.80, 6.00, 90, 75, 6.00, '["吟霖","吟霖的专武"]', 'active');

-- 抽卡物品配置表
CREATE TABLE IF NOT EXISTS gacha_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    rarity INT NOT NULL COMMENT '稀有度：3/4/5',
    item_type VARCHAR(20) NOT NULL COMMENT '类型：character/weapon',
    pool_type VARCHAR(20) NOT NULL COMMENT '所属池子：character/weapon/limited/standard',
    is_limited BOOLEAN DEFAULT FALSE COMMENT '是否为限定物品',
    image_url VARCHAR(255) COMMENT '图片路径',
    description TEXT COMMENT '描述'
);

-- 插入默认抽卡物品数据
INSERT INTO gacha_items (name, rarity, item_type, pool_type, is_limited) VALUES
-- 三星武器（角色池填充物）
('训练佩刀', 3, 'weapon', 'character', FALSE),
('训练长刃', 3, 'weapon', 'character', FALSE),
('训练拳套', 3, 'weapon', 'character', FALSE),
('训练佩枪', 3, 'weapon', 'character', FALSE),
('训练音感仪', 3, 'weapon', 'character', FALSE),

-- 三星武器（限定池填充物）
('训练佩刀', 3, 'weapon', 'limited', FALSE),
('训练长刃', 3, 'weapon', 'limited', FALSE),
('训练拳套', 3, 'weapon', 'limited', FALSE),
('训练佩枪', 3, 'weapon', 'limited', FALSE),
('训练音感仪', 3, 'weapon', 'limited', FALSE),

-- 五星角色（常驻池）
('漂泊者', 5, 'character', 'character', FALSE),
('安可', 5, 'character', 'character', FALSE),
('维里奈', 5, 'character', 'character', FALSE),
('卡卡罗', 5, 'character', 'character', FALSE),
('鉴心', 5, 'character', 'character', FALSE),
('凌阳', 5, 'character', 'character', FALSE),

-- 四星角色
('丹瑾', 4, 'character', 'character', FALSE),
('白芷', 4, 'character', 'character', FALSE),
('散华', 4, 'character', 'character', FALSE),
('秋水', 4, 'character', 'character', FALSE),
('渊武', 4, 'character', 'character', FALSE),
('莫特斐', 4, 'character', 'character', FALSE),
('炽霞', 4, 'character', 'character', FALSE),
('秧秧', 4, 'character', 'character', FALSE),
('桃祈', 4, 'character', 'character', FALSE),

-- 三星武器（常驻）
('训练佩刀', 3, 'weapon', 'weapon', FALSE),
('训练长刃', 3, 'weapon', 'weapon', FALSE),
('训练拳套', 3, 'weapon', 'weapon', FALSE),
('训练佩枪', 3, 'weapon', 'weapon', FALSE),
('训练音感仪', 3, 'weapon', 'weapon', FALSE),

-- 四星武器
('不归孤军', 4, 'weapon', 'weapon', FALSE),
('永夜星辉', 4, 'weapon', 'weapon', FALSE),
('行进序曲', 4, 'weapon', 'weapon', FALSE),
('今州守望', 4, 'weapon', 'weapon', FALSE),
('骇行', 4, 'weapon', 'weapon', FALSE),

-- 五星武器（常驻池）
('浩境粼光', 5, 'weapon', 'weapon', FALSE),
('漪澜浮录', 5, 'weapon', 'weapon', FALSE),
('琼枝冰绡', 5, 'weapon', 'weapon', FALSE),
('悲喜剧', 5, 'weapon', 'weapon', FALSE),
('千古洑流', 5, 'weapon', 'weapon', FALSE),

-- 限定池示例（当前UP角色）
('吟霖', 5, 'character', 'limited', TRUE),
('吟霖的专武', 5, 'weapon', 'limited', TRUE);
