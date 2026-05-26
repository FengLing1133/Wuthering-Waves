# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Please always communicate, explain code, and answer questions in **Chinese**.

## 项目概述

鸣潮抽卡模拟器 — 一个全栈 Web 应用，模拟游戏中的抽卡系统，包含角色池、武器池和限定池。支持用户认证、保底机制、货币管理、抽卡记录和后台管理。

## 技术栈

- **后端**: Spring Boot 3.5.5, Java 21, MyBatis-Plus 3.5.16, Spring Security + JWT (jjwt 0.12.5)
- **数据库**: MySQL 8.0
- **前端**: 原生 HTML/CSS/JS（作为 Spring Boot 静态资源托管）

## 构建与运行

需要环境变量：`JWT_SECRET`（JWT 签名密钥）、`DB_PASSWORD`（MySQL 密码）。所有 Maven 命令在 `backend/` 目录下执行。

```bash
cd backend

# 运行（Windows PowerShell）
$env:JWT_SECRET="my-secret"; $env:DB_PASSWORD="root"; mvn spring-boot:run

# 运行（bash）
JWT_SECRET=my-secret DB_PASSWORD=root mvn spring-boot:run

# 构建
mvn clean package

# 运行全部测试
mvn test

# 运行单个测试类
mvn test -Dtest=ClassName

# 跳过测试构建
mvn clean package -DskipTests
```

应用启动后访问 `http://localhost:8080`。前端页面：`/login.html`（登录注册）、`/index.html`（抽卡主界面）、`/admin.html`（后台管理）。

## CSS 架构

- **CSS 变量**：`:root` 定义深色主题变量（`style.css:9-31`），admin.html 内联样式覆盖为浅色主题
- **抽卡页**：深色背景 + 毛玻璃顶部栏（`.gacha-top-bar`），`.banner-bg` 为 fixed 全屏背景图
- **弹窗**：历史记录/抽卡分析/武器选择弹窗统一浅色暖色调（`linear-gradient(180deg, rgba(245,240,230,0.98), rgba(240,235,225,0.95))`）
- **按钮**：顶部/底部图标按钮用 `background-image` 图标为载体，hover 时 `::after` 伪元素白色脉冲扩散环
- **管理后台**：独立 `.admin-top-bar` 类，高度 50px，浅色背景，不继承抽卡页毛玻璃和渐变

## 数据库

初始化脚本：`database/init.sql`，创建 `wuthering_waves_gacha` 数据库及所有表和初始数据。连接 `localhost:3306`，凭据在 `backend/src/main/resources/application.yml` 中配置（支持 `DB_PASSWORD` 和 `JWT_SECRET` 环境变量覆盖）。

**核心表**：

| 表 | 用途 |
|---|---|
| `users` | 用户账号（用户名、bcrypt 密码、角色、星声、自选武器 UP） |
| `gacha_records` | 抽卡记录（用户、卡池、物品、稀有度、是否限定） |
| `gacha_pity` | 保底计数（用户、卡池类型、五星/四星计数、大保底标记） |
| `gacha_items` | 全部可抽物品（120 项：角色+武器，含稀有度、图片 URL） |
| `gacha_pool` | 卡池配置（概率、保底阈值、UP 物品、调度时间、UI 元数据） |
| `item_category` | 物品分类枚举（12 类：三星武器 ~ 五星特殊角色） |
| `pool_category` | 卡池与物品分类的多对多关联表 |

## 架构

### 包结构 (`backend/src/main/java/com/wutheringwaves/gacha/`)

- **controller/** — REST 接口
  - `AuthController` (`/api/auth/**`)：注册、登录、获取用户信息，公开接口
  - `GachaController` (`/api/gacha/**`)：抽卡、历史记录、统计，需 JWT 认证
  - `AdminController` (`/api/admin/**`)：物品和卡池 CRUD、图片上传，需 JWT 认证
- **service/** — 业务逻辑
  - `UserService`：注册、登录（bcrypt 加密）、用户货币操作
  - `GachaService`：抽卡逻辑、保底系统、历史记录、统计
  - `AdminService`：物品管理、卡池管理、图片上传
- **model/** — MyBatis-Plus 实体类：`User`、`GachaRecord`、`GachaItem`、`GachaPity`、`GachaPool`、`ItemCategory`、`PoolCategory`
- **mapper/** — MyBatis-Plus Mapper 接口 + XML 映射文件（`resources/mapper/`）
- **config/** — Spring Security（无状态 JWT，CORS 全开放）、JWT 过滤器、MyBatis 配置、静态资源映射（`/uploads/**`）
- **util/** — `JwtUtil` Token 生成/验证

### 抽卡/保底系统

六种卡池类型，各自独立追踪保底计数：

| poolType | 说明 | UP 机制 |
|---|---|---|
| `limited-character` | 限定角色 | 50/50 + 大保底 |
| `limited-weapon` | 限定武器 | 100% UP（不歪） |
| `standard-character` | 常驻角色 | 完全随机 |
| `standard-weapon` | 常驻武器 | 100% 用户自选 UP |
| `special-character` | 特殊角色 | 由 `allowLose` 配置决定 |
| `special-weapon` | 特殊武器 | 由 `allowLose` 配置决定 |

卡池配置存储在 `gacha_pool` 表中（含概率、保底阈值、UP 物品等），而非硬编码。

**概率**（默认值，可由卡池配置覆盖）：
- 五星 0.8%，四星 6%，三星 93.2%
- 软保底从第 75 抽开始：五星概率每抽递增约 6%
- 四星每 10 抽保底一次

**大保底机制**：如果抽到非 UP 五星，`guaranteed_five` 标记置为 true，下次出五星时必定为当期 UP 物品（仅限 `limited-*` 和 `special-*` 池）。

**自选 UP**（`standard-weapon`）：用户可在更换界面自选五星武器，存储在 `users.selected_standard_weapon_up`。抽卡时 `GachaService.pull()` 读取该字段覆盖池默认 `fivestarUp`，`selectItem()` 中 100% 返回自选武器。详见 `GachaService.java:45-52` 和 `GachaService.java:183-186`。

**货币系统**：星声为唯一抽卡货币，单抽 160，十连 1500，新用户初始 100000。

### 抽卡动画流程

`Gacha.js` 中 `pull()` 控制完整流程：

1. `playGachaVideo(maxRarity)` — 播放 `{rarity}star.mp4`，返回 `'end'` / `'five'` / `'summary'`
2. 十连：`showTenPullAnimation(results, videoResult)` — 根据返回值决定是否展示物品或直接跳汇总
3. 单个物品展示 `showItemShowcase()` → 五星走 `showFiveStarShowcase()`（唤取视频+循环视频），其他走 `showStaticShowcase()`
4. 全局跳过按钮 `#skipBtn` 在所有阶段统一使用，通过 `showSkipBtn()`/`hideSkipBtn()` 控制显隐

**键盘快捷键**：空格=下一个/关闭，ESC=跳过全部到汇总（物品展示阶段）/关闭（汇总阶段）

### 前端

静态文件位于 `backend/src/main/resources/static/`：

| 文件 | 用途 |
|---|---|
| `js/api.js` | API 客户端，封装 JWT Token 管理和请求拦截 |
| `js/auth.js` | 登录/注册页面逻辑 |
| `js/gacha.js` | 抽卡逻辑与动画（含视频播放） |
| `js/app.js` | 主应用编排 |
| `admin.html` + 内联 JS | 后台管理面板（物品 CRUD、卡池管理、图片上传） |
| `css/` | 深色主题，鸣潮风格（渐变、光效） |

用户上传的图片存储在 `backend/uploads/images/`，通过 `/uploads/images/**` 映射访问。

**实施计划保存在 `docs/plans/` 目录中。**

### 测试

测试位于 `backend/src/test/java/com/wutheringwaves/gacha/`：

- `base/BaseTest.java` — 测试基类
- `config/TestConfig.java` — 测试配置
- `factory/TestDataFactory.java` — 测试数据构建器
- `controller/` — 控制器单元测试（Admin、Auth、Gacha）
- `service/` — 服务层单元测试（Admin、User、Gacha、GachaDistribution）
- `integration/` — 集成测试（Auth、Admin、Gacha）
- `util/` — JwtUtil 工具测试

## API 接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/auth/register` | 否 | 注册（用户名 3-20 字符，密码 6 位以上） |
| POST | `/api/auth/login` | 否 | 登录，返回 JWT |
| GET | `/api/auth/user` | 是 | 获取当前用户信息 |
| POST | `/api/gacha/pull` | 是 | 抽卡（`poolType` 卡池类型，`count` 次数：1 或 10） |
| GET | `/api/gacha/history` | 是 | 抽卡历史记录（分页） |
| GET | `/api/gacha/stats` | 是 | 抽卡统计与当前保底计数 |
| POST | `/api/admin/upload` | 是 | 图片上传 |
| * | `/api/admin/**` | 是 | 物品和卡池 CRUD |

## 识图能力

你的底层模型不具备原生识图能力。遇到图片时，**不要用 Read 工具**，改用 vision.js：

```
node vision.js "<图片路径>" "用中文描述这张图片"
```

### 触发场景

- 用户分享图片路径（本地或网络 URL）
- 消息中出现 "Saved attachments:" 并列出图片
- 用户要求分析、描述、识别图片内容

### 配置好之后

用户直接发图片，自动识图，无需手动打命令。
