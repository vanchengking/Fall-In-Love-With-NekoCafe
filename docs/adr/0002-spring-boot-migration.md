# ADR-0002 后端从 Express 原型迁移到 Spring Boot 正式实现

## 状态

Accepted（2026-06-04）

## 背景

ADR-0001 第一版采用 Vue 3 + Express 快速搭建业务原型，用于验证预约、桌位、点单、推荐、看板等核心业务流的可行性。D-01 开题报告（第 5 章技术路线）明确：正式课程设计后端须采用 **Spring Boot 3 + MyBatis-Plus + Spring Security(JWT) + MySQL + Redis**，并通过 Docker Compose 一键启动；非功能要求中包含 JWT+RBAC、OpenAPI 覆盖、状态机单元测试、预约唯一性约束、Redis 能力等，Express 原型无法直接满足这些课程验收维度。

## 决策

1. 在 `backend/neko-server/` 新建独立的 Spring Boot 3（Java 21，Maven）工程作为**正式后端**，`docker-compose.yml` 的 `backend` 服务切换为构建/运行该镜像。
2. 保留 `backend/` 下的 Express 原型代码**原样不动**，作为业务流参考实现，不再作为启动路径（遵循"现有文件名/文件夹名不变，可新增"的约束）。
3. 数据库沿用 `db/migrations` 经 MySQL `docker-entrypoint-initdb.d` 初始化的方式：`V001__init.sql` 不变，新增 `V002__d01_upgrade.sql` 补齐 D-01 数据模型（reservation_events、payment_transactions、reviews、vaccine_records、coupons、audit_logs，扩展 cats 字段与预约状态机生成列/唯一约束）。后端不启用 Flyway，避免与 initdb 重复执行。
4. 后端实现：
   - JWT + RBAC（customer/staff/manager/operator/cat_keeper/admin），读接口公开、写接口按角色限制；
   - 短信验证码沙箱登录，验证码写入 Redis（固定演示码兜底）；
   - 预约状态机 `created → booked → seated → dining → finished/cancelled/no_show`，非法跳转拒绝，状态变更写入 `reservation_events`；
   - 预约并发：桌位行锁（`FOR UPDATE [SKIP LOCKED]`）+ 双重唯一约束（桌位-时段、用户-门店-时段）；
   - Redis 热点门店缓存；点单写入 `payment_transactions` 支付沙箱流水；
   - springdoc-openapi 暴露 `/swagger-ui.html`、`/v3/api-docs`。
5. 前端继续 Vue 3 + Vite，本轮仅做最小改造：登录沙箱接入 + 携带 JWT；Router/Pinia/Element Plus/ECharts 完整重构列入后续迭代。

## 后果

- 正式启动路径清晰单一：`docker compose up --build` → Spring Boot 后端 + MySQL + Redis + 前端。
- 保留 API 契约（统一 `{data:...}` / `{error:{message,status}}`）与数据库设计，前端无需大改即可端到端演示。
- 本机仅有 JDK8 且无 Maven，Spring Boot 的构建与测试统一在 Docker（maven + temurin-21 多阶段）内完成；镜像构建成功即代表 JUnit 单元测试通过。CI 另设 JDK21 Maven 任务与 docker build 任务。
- 待办：Testcontainers/Mapper 集成测试、Playwright E2E、k6 压测、前端组件化重构、reviews/疫苗/优惠券的完整 CRUD 与页面。
