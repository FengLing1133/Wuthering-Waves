# CLAUDE.md

Please always communicate, explain code, and answer questions in **Chinese**.

本文件为 Claude Code (claude.ai/code) 在本仓库中工作时提供指导。

## 项目概述

鸣潮抽卡模拟器 — 一个全栈 Web 应用，模拟游戏中的抽卡系统，包含角色池、武器池和限定池。支持用户认证、保底机制、货币管理和抽卡记录。

## 技术栈

- **后端**: Spring Boot 3.5.5, Java 21, MyBatis-Plus 3.5.16, Spring Security + JWT (jjwt 0.12.5)
- **数据库**: MySQL 8.0
- **前端**: 原生 HTML/CSS/JS（作为 Spring Boot 静态资源托管）

## 构建与运行

```bash
# 构建（在 backend/ 目录下执行）
cd backend
mvn clean package

# 运行
mvn spring-boot:run

# 运行测试
mvn test

# 运行单个测试类
mvn test -Dtest=ClassName

# 运行单个测试方法
mvn test -Dtest=ClassName#methodName
```

应用启动后访问 `http://localhost:8080`。前端页面：`/login.html` 和 `/index.html`。

## 数据库初始化

对 MySQL 执行 `database/init.sql` 创建 `wuthering_waves_gacha` 数据库及所有表和初始数据。应用连接 `localhost:3306`，数据库凭据在 `backend/src/main/resources/application.yml` 中配置。

**注意**: `application.yml` 中包含硬编码的数据库密码和 JWT 密钥，部署前应改为外部配置。

## 架构

### 包结构 (`backend/src/main/java/com/wutheringwaves/gacha/`)

- **controller/** — REST 接口
  - `AuthController` (`/api/auth/**`)：注册、登录、获取用户信息，公开接口，无需认证
  - `GachaController` (`/api/gacha/**`)：抽卡、历史记录、统计，需要 JWT 认证
- **service/** — 业务逻辑
  - `UserService`：注册、登录（bcrypt 加密）、用户货币操作
  - `GachaService`：抽卡逻辑、保底系统、历史记录、统计
- **model/** — MyBatis-Plus 实体类：`User`、`GachaRecord`、`GachaItem`、`CharacterPity`、`WeaponPity`、`LimitedPity`
- **mapper/** — MyBatis-Plus Mapper 接口（通过 `BaseMapper` 自动生成 CRUD）
- **config/** — Spring Security 配置（无状态 JWT 认证，CORS 全开放）、JWT 过滤器
- **util/** — `JwtUtil` 用于 Token 生成/验证

### 抽卡/保底系统（GachaService 核心业务逻辑）

三种卡池类型：`character`（角色池）、`weapon`（武器池）、`limited`（限定池），各自独立追踪保底计数。

**概率**：
- 角色池：五星 0.8%，四星 6%，三星 93.2%，90 抽硬保底
- 武器池：五星 0.7%，四星 6%，三星 93.3%，80 抽硬保底
- 软保底从第 75 抽开始：五星概率每抽递增约 6%
- 四星每 10 抽保底一次

**大保底机制**：如果抽到非限定五星，`guaranteed_five` 标记置为 true，下次出五星时必定为当期限定 UP 物品。

**货币系统**：
- 星声：唯一抽卡货币，单抽 160，十连 1500，新用户初始 100000

### 前端

静态文件位于 `backend/src/main/resources/static/`：
- `js/api.js` — API 客户端，封装 JWT Token 管理
- `js/auth.js` — 登录/注册页面逻辑
- `js/gacha.js` — 抽卡逻辑与动画
- `js/app.js` — 主应用编排
- `css/` — 深色主题样式，鸣潮风格（渐变、光效）

## API 接口

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/api/auth/register` | 否 | 注册（用户名 3-20 字符，密码 6 位以上） |
| POST | `/api/auth/login` | 否 | 登录，返回 JWT |
| GET | `/api/auth/user` | 是 | 获取当前用户信息 |
| POST | `/api/gacha/pull` | 是 | 抽卡（`poolType` 卡池类型，`count` 次数：1 或 10） |
| GET | `/api/gacha/history` | 是 | 抽卡历史记录（分页） |
| GET | `/api/gacha/stats` | 是 | 抽卡统计与当前保底计数 |
