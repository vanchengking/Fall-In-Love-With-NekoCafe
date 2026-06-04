# Fall-In-Love-With-NekoCafe

NekoCafe 智慧餐饮预约平台，面向猫咪主题餐厅的门店浏览、桌位预约、点单履约、会员运营、猫咪档案和经营看板系统。

本仓库保留课程模板目录结构。**正式后端**为 Spring Boot 3（位于 `backend/neko-server/`），按 D-01 技术路线实现；`backend/` 下的 Express 代码为早期业务原型，保留作参考，不再是启动路径（见 `docs/adr/0002-spring-boot-migration.md`）。

## 技术栈

| 层 | 选型 |
|----|------|
| 前端 | Vue 3 + Vite（图标 lucide） |
| 后端 | Spring Boot 3.3 · Java 21 · Maven |
| ORM | MyBatis-Plus 3.5 |
| 安全 | Spring Security 6 + JWT（jjwt） |
| 数据库 | MySQL 8 |
| 缓存/并发 | Redis 7（短信验证码、热点门店缓存） |
| API 文档 | springdoc-openapi（Swagger UI） |
| 部署 | Docker Compose |
| 测试 | JUnit 5（后端单元测试，随 `mvn package` 执行） |

## 前置依赖

- Docker Desktop 24+（含 Docker Compose v2）
- 仅本地开发前端时：Node.js 20+ / npm 10+
- 注意：后端为 Java 21 + Maven，但**无需本地安装**——构建在 Docker 多阶段镜像内完成。

## 一键启动（评委推荐）

```bash
docker compose up --build
```

首次构建会在容器内用 Maven 拉取依赖（国内已配置阿里云镜像加速）。启动后：

- 前端：http://localhost:5173
- 后端健康检查：http://localhost:8080/healthz
- 后端 API：http://localhost:8080/api
- API 文档（Swagger）：http://localhost:8080/swagger-ui.html
- MySQL：localhost:3306，库 `neko_cafe`，用户 `neko` / `neko`
- Redis：localhost:6379

数据库 schema 由 `db/migrations/V001__init.sql` + `V002__d01_upgrade.sql` 经 MySQL 初始化脚本自动建立并写入种子数据；重复启动不会重复建表或重复插入。

## 演示账号（短信验证码沙箱）

固定验证码：**8888**（沙箱）。种子账号（按手机号区分角色）：

| 角色 | 手机号 |
|------|--------|
| customer 顾客 | 13800000001 |
| staff 店员 | 13800000002 |
| manager 店长 | 13800000003 |
| operator 运营 | 13800000004 |
| cat_keeper 猫咪管家 | 13800000005 |
| admin 管理员 | 13800000006 |

前端默认以 admin 沙箱登录，便于一个页面贯通顾客/店员/猫咪/看板各 Tab 的写操作；后端 RBAC 仍严格生效（可用 Swagger 以不同角色 Token 验证权限）。

## 5 分钟主流程演示

注册/登录 → 浏览门店 → 选择日期/时段/人数/桌位 + 偏好标签（规则推荐）→ 创建预约 → 点单支付沙箱 → 店员入座/用餐中/完桌 → 运营看板。

预约状态机：`created → booked → seated → dining → finished / cancelled / no_show`，非法跳转会被拒绝，每次变更写入 `reservation_events`。

## 本地开发

```bash
# 前端
npm --prefix frontend install
npm --prefix frontend run dev   # http://localhost:5173，已代理 /api 到 8080

# 后端（需本地 JDK 21 + Maven 时）
mvn -f backend/neko-server/pom.xml spring-boot:run
```

## 验证 / 冒烟测试

```bash
curl http://localhost:8080/healthz
curl http://localhost:8080/api/stores

# 短信沙箱登录拿 JWT
curl -s -X POST http://localhost:8080/api/auth/sms/send \
  -H 'Content-Type: application/json' -d '{"data":{"mobileNumber":"13800000002"}}'
curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' -d '{"data":{"mobileNumber":"13800000002","code":"8888"}}'
```

后端单元测试随 Docker 镜像构建（`mvn package`）执行；也可单独跑：

```bash
mvn -f backend/neko-server/pom.xml test
```

## 已覆盖模块

- M1 用户与会员：短信验证码沙箱登录、JWT、会员等级/积分、偏好标签
- M2 门店与桌位：门店列表（Redis 缓存）、桌位容量、时段可用性
- M3 预约与点单：预约创建（事务 + 桌位行锁 + 双重唯一约束）、点单、支付沙箱流水
- M4 订单与履约：订单/支付状态、预约状态机 + 事件记录
- M5 店员后台：今日预约、按状态筛选、入座/用餐中/完桌/取消/no_show、异常告警
- M6 数据看板：预约量、翻台、收入、复购近似指标
- 选做：基于偏好/猫咪性格/桌位/菜品标签的规则推荐；猫咪健康与疫苗记录

## 常见故障

- **首次构建慢/超时**：Maven 依赖较多，已配置阿里云镜像；网络不稳时重试 `docker compose build backend`。
- **8080/5173/3306/6379 端口被占用**：先停止占用进程或修改 `docker-compose.yml` 端口映射。
- **数据库需要重置**：`docker compose down -v` 清空数据卷后再 `up --build`（会重新执行迁移与种子）。
- **后端起不来**：确认 `mysql` 容器先变为 healthy；查看 `docker compose logs backend`。

## 团队

- 组长：高范铖（gaovancheng@gmail.com）/ G17 · T-01
