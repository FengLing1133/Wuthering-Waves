-- 删除已有表（按外键依赖顺序）
DROP TABLE IF EXISTS gacha_records;
DROP TABLE IF EXISTS gacha_pity;
DROP TABLE IF EXISTS pool_category;
DROP TABLE IF EXISTS gacha_items;
DROP TABLE IF EXISTS item_category;
DROP TABLE IF EXISTS gacha_pool;
DROP TABLE IF EXISTS users;

-- 用户表
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) DEFAULT 'user',
    starlight INT DEFAULT 1600,
    selected_standard_weapon_up BIGINT DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 物品分类枚举表
CREATE TABLE item_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    rarity INT NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    description VARCHAR(200),
    sort_order INT DEFAULT 0
);

-- 抽卡物品表
CREATE TABLE gacha_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rarity INT NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    category_id BIGINT NOT NULL,
    image_url VARCHAR(255),
    description VARCHAR(500)
);

-- 卡池配置表
CREATE TABLE gacha_pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    pool_type VARCHAR(30) NOT NULL,
    description VARCHAR(500),
    five_star_rate DECIMAL(5,2) DEFAULT 0.80,
    four_star_rate DECIMAL(5,2) DEFAULT 6.00,
    max_pity INT DEFAULT 90,
    soft_pity_start INT DEFAULT 75,
    soft_pity_increment DECIMAL(5,2) DEFAULT 6.00,
    status VARCHAR(20) DEFAULT 'active',
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    sidebar_visible BOOLEAN DEFAULT FALSE,
    sidebar_order INT DEFAULT 0,
    bg_image_url VARCHAR(255),
    thumbnail_url VARCHAR(255),
    fivestar_up BIGINT,
    fourstar_up VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 卡池-分类关联表
CREATE TABLE pool_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pool_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    UNIQUE (pool_id, category_id)
);

-- 统一保底表
CREATE TABLE gacha_pity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pool_type VARCHAR(30) NOT NULL,
    five_star_count INT DEFAULT 0,
    four_star_count INT DEFAULT 0,
    guaranteed_five BOOLEAN DEFAULT FALSE,
    guaranteed_four BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, pool_type)
);

-- 抽卡记录表
CREATE TABLE gacha_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    pool_type VARCHAR(30) NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    item_rarity INT NOT NULL,
    item_type VARCHAR(20) NOT NULL,
    is_limited BOOLEAN DEFAULT FALSE,
    pity_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
