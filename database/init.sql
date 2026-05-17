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
    selected_standard_weapon_up BIGINT DEFAULT NULL COMMENT '常驻武器池自选UP武器ID',
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
    is_limited BOOLEAN DEFAULT FALSE COMMENT '是否为UP物品（抽卡时的快照）',
    pity_count INT COMMENT '当前保底计数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_pool_type (pool_type),
    INDEX idx_created_at (created_at),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 统一保底计数表
CREATE TABLE IF NOT EXISTS gacha_pity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    pool_type VARCHAR(30) NOT NULL COMMENT '池子类型：limited-character/limited-weapon/standard-character/standard-weapon',
    five_star_count INT DEFAULT 0 COMMENT '距离五星保底累计抽数',
    four_star_count INT DEFAULT 0 COMMENT '距离四星保底累计抽数',
    guaranteed_five BOOLEAN DEFAULT FALSE COMMENT '五星大保底（下次五星必出UP）',
    guaranteed_four BOOLEAN DEFAULT FALSE COMMENT '四星大保底（下次四星必出UP）',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_pool (user_id, pool_type)
);

-- ========== 物品分类枚举表 ==========
CREATE TABLE IF NOT EXISTS item_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    rarity INT NOT NULL COMMENT '稀有度：3/4/5',
    item_type VARCHAR(20) NOT NULL COMMENT '类型：character/weapon',
    description VARCHAR(200) COMMENT '分类描述',
    sort_order INT DEFAULT 0 COMMENT '排序'
);

INSERT INTO item_category (id, name, rarity, item_type, description, sort_order) VALUES
(1, '三星武器', 3, 'weapon', '三星通用武器', 1),
(2, '四星角色', 4, 'character', '四星角色', 2),
(3, '四星武器', 4, 'weapon', '四星武器', 3),
(4, '五星常驻角色', 5, 'character', '常驻池可出的五星角色', 4),
(5, '五星常驻武器', 5, 'weapon', '常驻池可出的五星武器', 5),
(6, '五星限定武器', 5, 'weapon', '限定池专精五星武器', 6),
(7, '五星限定角色', 5, 'character', '限定UP五星角色', 7);

-- ========== 统一抽卡物品表 ==========
CREATE TABLE IF NOT EXISTS gacha_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    rarity INT NOT NULL COMMENT '稀有度：3/4/5',
    item_type VARCHAR(20) NOT NULL COMMENT '类型：character/weapon',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    image_url VARCHAR(255) COMMENT '图片路径',
    description TEXT COMMENT '描述',
    FOREIGN KEY (category_id) REFERENCES item_category(id)
);

INSERT INTO gacha_items (id, name, rarity, item_type, category_id) VALUES
-- ===== 三星武器 (category_id=1) =====
(1, '暗夜长刃·玄明', 3, 'weapon', 1),
(2, '暗夜迅刀·黑闪', 3, 'weapon', 1),
(3, '暗夜佩枪·暗星', 3, 'weapon', 1),
(4, '暗夜臂铠·夜芒', 3, 'weapon', 1),
(5, '暗夜矩阵·暝光', 3, 'weapon', 1),
(6, '远行者长刃·辟路', 3, 'weapon', 1),
(7, '远行者迅刀·旅迹', 3, 'weapon', 1),
(8, '远行者佩枪·洞察', 3, 'weapon', 1),
(9, '远行者臂铠·破障', 3, 'weapon', 1),
(10, '远行者矩阵·探幽', 3, 'weapon', 1),
(11, '源能长刃·测壹', 3, 'weapon', 1),
(12, '源能迅刀·测贰', 3, 'weapon', 1),
(13, '源能佩枪·测叁', 3, 'weapon', 1),
(14, '源能臂铠·测肆', 3, 'weapon', 1),
(15, '源能音感仪·测五', 3, 'weapon', 1),
-- ===== 四星角色 (category_id=2) =====
(16, '渊武', 4, 'character', 2),
(17, '秋水', 4, 'character', 2),
(18, '白芷', 4, 'character', 2),
(19, '灯灯', 4, 'character', 2),
(20, '卜灵', 4, 'character', 2),
(21, '釉瑚', 4, 'character', 2),
(22, '丹瑾', 4, 'character', 2),
(23, '炽霞', 4, 'character', 2),
(24, '散华', 4, 'character', 2),
(25, '桃祈', 4, 'character', 2),
(26, '秧秧', 4, 'character', 2),
(27, '莫特斐', 4, 'character', 2),
-- ===== 四星武器 (category_id=3) =====
(28, '永夜长明', 4, 'weapon', 3),
(29, '不归孤军', 4, 'weapon', 3),
(30, '无眠烈火', 4, 'weapon', 3),
(31, '袍泽之固', 4, 'weapon', 3),
(32, '今州守望', 4, 'weapon', 3),
(33, '异响空灵', 4, 'weapon', 3),
(34, '行进序曲', 4, 'weapon', 3),
(35, '华彩乐段', 4, 'weapon', 3),
(36, '呼啸重音', 4, 'weapon', 3),
(37, '奇幻变奏', 4, 'weapon', 3),
(38, '东落', 4, 'weapon', 3),
(39, '西升', 4, 'weapon', 3),
(40, '飞逝', 4, 'weapon', 3),
(41, '骇行', 4, 'weapon', 3),
(42, '异度', 4, 'weapon', 3),
(43, '凋亡频移', 4, 'weapon', 3),
(44, '永续坍缩', 4, 'weapon', 3),
(45, '悖论喷流', 4, 'weapon', 3),
(46, '尘云旋臂', 4, 'weapon', 3),
(47, '核熔星盘', 4, 'weapon', 3),
-- ===== 五星常驻角色 (category_id=4) =====
(48, '维里奈', 5, 'character', 4),
(49, '安可', 5, 'character', 4),
(50, '卡卡罗', 5, 'character', 4),
(51, '凌阳', 5, 'character', 4),
(52, '鉴心', 5, 'character', 4),
-- ===== 五星常驻武器 (category_id=5) =====
(53, '浩境粼光', 5, 'weapon', 5),
(54, '千古洑流', 5, 'weapon', 5),
(55, '停驻之烟', 5, 'weapon', 5),
(56, '擎渊怒涛', 5, 'weapon', 5),
(57, '漪澜浮录', 5, 'weapon', 5),
(71, '源能机锋', 5, 'weapon', 5),
(72, '相位涟漪', 5, 'weapon', 5),
(73, '脉冲协臂', 5, 'weapon', 5),
(74, '玻色星仪', 5, 'weapon', 5),
(75, '镭射切变', 5, 'weapon', 5),
-- ===== 五星限定武器 (category_id=6) =====
(58, '苍鳞千嶂', 5, 'weapon', 6),
(59, '掣傀之手', 5, 'weapon', 6),
(60, '时和岁稔', 5, 'weapon', 6),
(61, '赫奕流明', 5, 'weapon', 6),
(62, '琼枝冰绡', 5, 'weapon', 6),
(63, '诸方玄枢', 5, 'weapon', 6),
(64, '裁春', 5, 'weapon', 6),
(65, '悲喜剧', 5, 'weapon', 6),
(66, '不灭航路', 5, 'weapon', 6),
(67, '星序协响', 5, 'weapon', 6),
(68, '死与舞', 5, 'weapon', 6),
(69, '海的呢喃', 5, 'weapon', 6),
(70, '幽冥的忘忧章', 5, 'weapon', 6),
(76, '林间的咏叹调', 5, 'weapon', 6),
(77, '不屈命定之冠', 5, 'weapon', 6),
(78, '驭冕铸雷之权', 5, 'weapon', 6),
(79, '万物持存的注释', 5, 'weapon', 6),
(80, '焰痕', 5, 'weapon', 6),
(81, '光影双生', 5, 'weapon', 6),
(82, '昙切', 5, 'weapon', 6),
(83, '焰光裁定', 5, 'weapon', 6),
(84, '和光回唱', 5, 'weapon', 6),
(85, '溢彩荧辉', 5, 'weapon', 6),
(86, '宙算仪轨', 5, 'weapon', 6),
(87, '裁竹', 5, 'weapon', 6),
(88, '永远的启明星', 5, 'weapon', 6),
(89, '白昼之脊', 5, 'weapon', 6),
(90, '昭日译注', 5, 'weapon', 6),
(91, '灼霜', 5, 'weapon', 6),
-- ===== 五星限定角色 (category_id=7) =====
(92, '忌炎', 5, 'character', 7),
(93, '吟霖', 5, 'character', 7),
(94, '今汐', 5, 'character', 7),
(95, '长离', 5, 'character', 7),
(96, '相里要', 5, 'character', 7),
(97, '折枝', 5, 'character', 7),
(98, '守岸人', 5, 'character', 7),
(99, '椿', 5, 'character', 7),
(100, '珂莱塔', 5, 'character', 7),
(101, '洛可可', 5, 'character', 7),
(102, '布兰特', 5, 'character', 7),
(103, '菲比', 5, 'character', 7),
(104, '坎特蕾拉', 5, 'character', 7),
(105, '赞妮', 5, 'character', 7),
(106, '夏空', 5, 'character', 7),
(107, '卡提希娅', 5, 'character', 7),
(108, '露帕', 5, 'character', 7),
(109, '弗洛洛', 5, 'character', 7),
(110, '奥古斯塔', 5, 'character', 7),
(111, '尤诺', 5, 'character', 7),
(112, '嘉贝莉娜', 5, 'character', 7),
(113, '仇远', 5, 'character', 7),
(114, '千咲', 5, 'character', 7),
(115, '琳奈', 5, 'character', 7),
(116, '莫宁', 5, 'character', 7),
(117, '爱弥斯', 5, 'character', 7),
(118, '陆·赫斯', 5, 'character', 7),
(119, '西格莉卡', 5, 'character', 7),
(120, '绯雪', 5, 'character', 7);

-- ========== 卡池配置表（含UP物品信息） ==========
CREATE TABLE IF NOT EXISTS gacha_pool (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '卡池名称',
    pool_type VARCHAR(30) NOT NULL COMMENT '池子类型：limited-character/limited-weapon/standard-character/standard-weapon',
    description TEXT COMMENT '卡池描述',
    five_star_rate DECIMAL(5,2) DEFAULT 0.80 COMMENT '五星基础概率(%)',
    four_star_rate DECIMAL(5,2) DEFAULT 6.00 COMMENT '四星基础概率(%)',
    max_pity INT DEFAULT 90 COMMENT '硬保底抽数',
    soft_pity_start INT DEFAULT 75 COMMENT '软保底起始抽数',
    soft_pity_increment DECIMAL(5,2) DEFAULT 6.00 COMMENT '软保底每抽概率递增(%)',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态：active/inactive',
    start_time DATETIME COMMENT '上架时间',
    end_time DATETIME COMMENT '下架时间',
    sidebar_visible BOOLEAN DEFAULT FALSE COMMENT '是否显示在抽卡页面侧栏',
    sidebar_order INT DEFAULT 0 COMMENT '侧栏排序（越小越靠前）',
    bg_image_url VARCHAR(255) COMMENT '抽卡页面主背景大图',
    thumbnail_url VARCHAR(255) COMMENT '侧栏缩略图',
    fivestar_up BIGINT COMMENT '五星UP物品ID（单个）',
    fourstar_up VARCHAR(200) COMMENT '四星UP物品ID列表，逗号分隔',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_pool_type (pool_type),
    INDEX idx_status (status)
);

-- 初始卡池数据
INSERT INTO gacha_pool (name, pool_type, description, five_star_rate, four_star_rate, max_pity, soft_pity_start, soft_pity_increment, status, sidebar_visible, sidebar_order, fivestar_up, fourstar_up) VALUES
('限定角色活动唤取', 'limited-character', '限定五星角色概率UP', 0.80, 6.00, 80, 65, 6.00, 'active', TRUE, 1, 92, NULL),
('限定武器活动唤取', 'limited-weapon', '限定五星武器概率UP', 0.80, 6.00, 80, 65, 6.00, 'active', TRUE, 2, 58, NULL),
('常驻角色唤取', 'standard-character', '常驻五星角色', 0.80, 6.00, 80, 65, 6.00, 'active', TRUE, 3, NULL, NULL),
('常驻武器唤取', 'standard-weapon', '常驻五星武器', 0.80, 6.00, 80, 65, 6.00, 'active', TRUE, 4, NULL, NULL);

-- ========== 卡池-分类关联表（替代 pool_item） ==========
CREATE TABLE IF NOT EXISTS pool_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pool_id BIGINT NOT NULL COMMENT '卡池ID',
    category_id BIGINT NOT NULL COMMENT '分类ID',
    FOREIGN KEY (pool_id) REFERENCES gacha_pool(id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES item_category(id) ON DELETE CASCADE,
    UNIQUE KEY uk_pool_category (pool_id, category_id)
);

INSERT INTO pool_category (pool_id, category_id) VALUES
-- 限定角色池：三星武器 + 四星角色 + 五星常驻角色 + 五星限定角色
(1, 1), (1, 2), (1, 4), (1, 7),
-- 限定武器池：三星武器 + 四星武器 + 五星常驻武器 + 五星限定武器
(2, 1), (2, 3), (2, 5), (2, 6),
-- 常驻角色池：三星武器 + 四星角色 + 五星常驻角色
(3, 1), (3, 2), (3, 4),
-- 常驻武器池：三星武器 + 四星武器 + 五星常驻武器 + 五星限定武器
(4, 1), (4, 3), (4, 5), (4, 6);

-- ========== 管理员卡池编辑功能扩展 ==========

-- 四星角色头像表
CREATE TABLE IF NOT EXISTS four_star_avatars (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '角色名称',
    avatar_url VARCHAR(255) NOT NULL COMMENT '头像图片路径',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 卡池-四星头像关联表
CREATE TABLE IF NOT EXISTS pool_four_star (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pool_id BIGINT NOT NULL COMMENT '卡池ID',
    avatar_id BIGINT NOT NULL COMMENT '四星头像ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    FOREIGN KEY (pool_id) REFERENCES gacha_pool(id) ON DELETE CASCADE,
    FOREIGN KEY (avatar_id) REFERENCES four_star_avatars(id) ON DELETE CASCADE,
    UNIQUE KEY uk_pool_avatar (pool_id, avatar_id)
);
