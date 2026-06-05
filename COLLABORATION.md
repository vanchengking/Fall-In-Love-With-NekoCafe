# NekoCafe 团队协作指南

## 一、数据库一致性方案

**不需要云端数据库。** 采用 Docker 本地数据库 + SQL 迁移脚本管理。

### 原则

所有数据库结构变更必须通过 `db/migrations/` 下的 SQL 脚本管理，**禁止手动改库**。

### 日常操作

```bash
# 拉取最新代码后，重建数据库（开发环境推荐）
docker compose down -v
docker compose up --build

# 迁移脚本会按文件名顺序自动执行：
# V001__init.sql → V002__d01_upgrade.sql → V003__m1_xxx.sql → ...
```

### 新增数据库变更规范

| 规则 | 说明 |
|------|------|
| 文件命名 | `V003__m{模块编号}_{描述}.sql`，如 `V003__m1_user_module.sql` |
| 开头声明 | 必须加 `SET NAMES utf8mb4; SET CHARACTER SET utf8mb4;` |
| 幂等设计 | 使用 `CREATE TABLE IF NOT EXISTS`、`INSERT IGNORE`、`ALTER TABLE ... ADD COLUMN IF NOT EXISTS` |
| 命名规范 | 表名、列名统一 snake_case |
| 不改旧文件 | 已提交的 V001/V002 不再修改，所有变更放 V003 及以后 |
| PR 审查 | SQL 脚本必须在 PR 中由至少一人 review 后才能合并 |

### 各模块迁移文件分配

| 成员 | 迁移文件 | 内容 |
|------|---------|------|
| 葛宇晨 | `V003__m1_user_module.sql` | users 表扩展（头像、实名、偏好详细字段） |
| 成睿轩 | `V003__m2_store_table.sql` | stores/dining_tables 扩展（设备、区域详情、照片） |
| 高范铖 | `V003__m3_reservation.sql` | reservations 状态机扩展、推荐相关表 |
| 葛宇晨 | `V003__m4_order_module.sql` | orders 扩展（取消原因、退款沙箱） |
| 成睿轩 | `V003__m6_dashboard.sql` | 看板统计所需索引、物化视图（如有） |

---

## 二、分支策略

`main` 分支受保护，禁止直接 push。每人按负责模块创建 feature 分支：

```
main
├── feat/m1-user-member        ← 葛宇晨：用户注册登录、会员管理
├── feat/m2-store-table        ← 成睿轩：门店桌位管理、数据看板
├── feat/m3-reservation        ← 高范铖：预约核心、订单状态机、推荐
├── feat/m4-order-fulfill      ← 葛宇晨：订单履约、支付沙箱
├── feat/m5-staff-backend      ← 高范铖/葛宇晨：店员后台
└── feat/frontend-pages        ← 卢欣怡：全部 Vue 3 前端页面
```

**操作流程：**

```bash
git clone https://github.com/vanchengking/Fall-In-Love-With-NekoCafe.git
cd Fall-In-Love-With-NekoCafe
git checkout -b feat/mX-xxx
# 开发...
git push -u origin feat/mX-xxx
# 在 GitHub 上创建 Pull Request → main，指定其他成员 review
```

**PR 合并前必须：CI 全绿 + 至少一人 review 通过。**

---

## 三、每人细化任务

### 高范铖（项目经理 / 架构师）

| 任务 | 文件 | 说明 |
|------|------|------|
| 预约创建与桌位分配 | `ReservationController.java` | 可视化选桌、自动分配、唯一约束防重复预约 |
| 预约状态机 | `ReservationStateMachine.java` | created→booked→seated→dining→finished/cancelled/no_show |
| 推荐服务 | `RecommendationService.java` | 人-桌-菜-猫四维匹配评分 |
| 预约 Mapper | `ReservationMapper.java` | 从 CatalogMapper 拆分出预约相关查询 |
| 预约事件记录 | `ReservationEventMapper.java` | 状态变更审计 |
| 数据库迁移 | `V003__m3_reservation.sql` | 预约状态机扩展、推荐相关表 |
| 单元测试 | `ReservationStateMachineTest.java` | 状态机合法/非法跳转全覆盖 |
| 项目统筹 | — | 进度跟踪、代码审查、答辩汇报 |

**备份人：成睿轩**

### 成睿轩（数据库 / 全栈）

| 任务 | 文件 | 说明 |
|------|------|------|
| 门店列表与详情 | `StoreController.java` | 门店 CRUD、营业时间、猫咪数量 |
| 桌位管理 | `TableController.java` / `TableMapper.java` | 桌位库存、区域分类、实时可用性查询 |
| 数据看板 | `DashboardController.java` | 预约量、翻台率、坪效、会员复购率统计 |
| 门店 Mapper | `StoreMapper.java` | 从 CatalogMapper 拆分出门店/桌位查询 |
| 看板 Mapper | `DashboardMapper.java` | 从 CatalogMapper 拆分出看板查询 |
| 数据库迁移 | `V003__m2_store_table.sql`、`V003__m6_dashboard.sql` | 门店桌位扩展、看板索引优化 |
| 数据库设计文档 | ER 图、数据字典更新 | D-05 数据库设计交付 |
| 运维手册 | Runbook 文档 | D-07 部署文档中的运维部分 |

**备份人：卢欣怡**

### 葛宇晨（后端 / 需求 / 测试）

| 任务 | 文件 | 说明 |
|------|------|------|
| 用户注册登录 | `UserController.java`、`UserService.java` | 手机号+短信验证码（沙箱8888）、JWT 签发 |
| 会员管理 | `UserController.java` | 会员等级（青铜/白银/黄金/铂金）、积分、偏好标签 |
| 订单模块 | `OrderController.java`、`OrderService.java` | 菜品下单、购物车、订单状态查询 |
| 评价模块 | `ReviewController.java`、`ReviewMapper.java` | 顾客评价（评分+内容） |
| 数据库迁移 | `V003__m1_user_module.sql`、`V003__m4_order_module.sql` | 用户表扩展、订单表扩展 |
| 单元测试 | 各模块测试类 | 覆盖率 ≥ 60%，核心域 ≥ 80% |
| 测试计划 | 测试用例文档、E2E 测试脚本 | D-06 测试报告 |
| 需求文档 | SRS 撰写 | D-02 软件需求规格说明书 |

**备份人：高范铖**

### 卢欣怡（前端 / DevOps）

| 任务 | 文件 | 说明 |
|------|------|------|
| 顾客首页 | `views/customer/CustomerHome.vue` | 推荐展示、快捷入口 |
| 门店浏览 | `views/customer/CustomerStores.vue`、`CustomerStoreDetail.vue` | 门店列表、详情（猫咪/菜品/桌位） |
| 预约页面 | `views/customer/CustomerReservation.vue` | 可视化选桌、日期时段选择 |
| 点单页面 | `views/customer/CustomerOrder.vue` | 菜品分类浏览、购物车 |
| 支付页面 | `views/customer/CustomerPayment.vue` | 支付沙箱模拟 |
| 个人中心 | `views/customer/CustomerProfile.vue` | 会员信息、偏好、预约历史 |
| 评价页面 | `views/customer/CustomerReviews.vue` | 评分+内容提交 |
| 店员今日看板 | `views/staff/StaffToday.vue` | 今日预约列表、10 秒自动刷新 |
| 店员预约管理 | `views/staff/StaffReservations.vue` | 预约状态筛选、入座/完桌操作 |
| 店员订单管理 | `views/staff/StaffOrders.vue` | 订单状态查询、异常处理 |
| 店员桌位管理 | `views/staff/StaffTables.vue` | 桌位状态可视化 |
| 管理员看板 | `views/admin/AdminDashboard.vue` | ECharts 数据看板（预约量/翻台率/收入） |
| 管理员门店 | `views/admin/AdminStores.vue` | 门店 CRUD |
| 管理员猫咪 | `views/admin/AdminCats.vue` | 猫咪档案管理（照片/体重/疫苗） |
| 管理员员工 | `views/admin/AdminStaff.vue` | 人员管理 |
| 管理员活动 | `views/admin/AdminCampaigns.vue` | 优惠券、活动配置 |
| 路由与状态 | `router/index.ts`、`stores/auth.ts`、`stores/cart.ts` | Vue Router 路由、Pinia 状态管理 |
| Docker / CI | `docker-compose.yml`、`Dockerfile`、`.github/workflows/` | 容器编排、CI 流水线维护 |
| 用户手册 | 使用说明文档 | D-07 部署文档中的用户手册部分 |

**备份人：成睿轩**

---

## 四、文件归属（避免冲突）

| 成员 | 后端新建文件 | 前端负责 |
|------|-------------|---------|
| 高范铖 | `Reservation*.java`、`RecommendationService.java`、`V003__m3_*.sql` | — |
| 成睿轩 | `StoreMapper.java`、`TableMapper.java`、`DashboardMapper.java`、`V003__m2/m6_*.sql` | — |
| 葛宇晨 | `UserController.java`、`OrderController.java`、`ReviewController.java`、`V003__m1/m4_*.sql`、测试类 | — |
| 卢欣怡 | — | `views/`、`components/`、`router/`、`stores/`、Dockerfile、CI |

**核心原则：每人新建自己的文件，不修改别人负责的文件。**

### 共享文件处理

- `CatalogMapper.java`：各成员新建自己的 Mapper，逐步将查询迁移出去，不在原文件追加
- `router/index.ts`：各模块新增路由写在自己的路由文件，主路由文件只做 import 聚合
- `types/index.ts`：新增类型写在各自的 `types/mX.ts`，主文件只做 export 聚合
- `docker-compose.yml` / `application.yml`：如需修改，先在群里沟通
- `SecurityConfig.java`：新增接口权限时，追加自己的规则，不删别人的

---

## 五、本地开发环境

```bash
# 一键启动（首次或数据库结构变更后）
docker compose down -v
docker compose up --build

# 仅启动基础设施（推荐开发时用）
docker compose up mysql redis -d
# 前端热更新
cd frontend && npm install && npm run dev
# 后端在 IDE 中直接运行 NekoServerApplication
```

**访问地址：**

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| MySQL | localhost:3307，库 `neko_cafe`，用户 `neko` / `neko` |
| Redis | localhost:6379 |

---

## 六、演示账号

固定验证码：**8888**

| 角色 | 手机号 | 登录后页面 | 对应模块 |
|------|--------|-----------|---------|
| 顾客 | 13800000001 | 顾客首页 | M1/M3 |
| 店员 | 13800000002 | 今日预约看板 | M5 |
| 店长 | 13800000003 | 数据看板 | M6 |
| 总部运营 | 13800000004 | 全局数据 | M6 |
| 猫咪管家 | 13800000005 | 猫咪档案 | 选做2 |
| 管理员 | 13800000006 | 数据看板 | 全部 |

---

## 七、里程碑与验收标准

| 里程碑 | 日期 | 交付物 | 负责人 | 验收标准 |
|--------|------|--------|--------|---------|
| M1 立项完成 | D1 (06-01) | D-01 开题报告、kickoff tag | 高范铖 | 开题报告通过 |
| M2 需求基线 | D3 (06-03) | D-02 SRS、UML 模型集 | 高范铖、葛宇晨 | 用例覆盖全部 Must 功能 |
| M3 设计基线 | D5 (06-05) | C4 架构图、ER 图、ADR | 成睿轩、高范铖 | 数据库至少 8 张表 |
| M4 编码完成 | D9 (06-09) | 前后端代码、单元测试 | 全员 | Must 功能可演示，覆盖率 ≥ 60% |
| M5 测试通过 | D11 (06-11) | 测试报告、缺陷台账 | 葛宇晨、高范铖 | P95 ≤ 350ms，ZAP 高危 0 |
| M6 部署就绪 | D12 (06-12) | Docker Compose、用户手册 | 卢欣怡、成睿轩 | docker compose up 一键启动成功 |
| M7 验收完成 | D14 (06-14) | 总结报告、答辩PPT、final tag | 高范铖、成睿轩 | 答辩通过 |

---

## 八、CI / CD

每次 push 或 PR 会自动运行 GitHub Actions：

- `backend-springboot`：Maven 构建 + JUnit 测试
- `prototype-backend`：Express 原型语法检查 + 测试
- `frontend`：`npm run build` 构建检查
- `docker-build`：后端 Docker 镜像构建检查

**PR 合并前必须 CI 全绿。**

---

## 九、代码规范

- Java：Controller → Service → Mapper 三层分离，Controller 不写业务逻辑
- TypeScript/Vue：Composition API（`<script setup>`），组件文件 PascalCase
- 提交信息：`feat/fix/refactor/test/docs: 简要描述`
- 一个 commit 只做一件事
- 新接口必须加 Swagger 注解（`@Operation`、`@Tag`）
- 新接口必须在 `SecurityConfig.java` 中配置权限

---

## 十、每日协作节奏

| 时间 | 活动 | 参与人 |
|------|------|--------|
| 每日 10:00 | 站会（15 分钟）：昨日进展、今日计划、阻塞项 | 全员 |
| 每周五 17:00 | 周报：里程碑进度、风险暴露、下周计划 | 高范铖汇总 |
| 随时 | PR 提交后 24 小时内完成 review | 指定 reviewer |
