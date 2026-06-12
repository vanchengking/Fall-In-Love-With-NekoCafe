# Fall-In-Love-With-NekoCafe

NekoCafe 智慧餐饮预约平台，面向猫咪主题餐厅的门店浏览、桌位预约、点单履约、会员运营、猫咪档案和经营看板系统。

## 技术栈

| 层 | 选型 |
|----|------|
| 前端 | Vue 3 + TypeScript + Vite 6 + Element Plus + ECharts + Pinia + Vue Router |
| 后端 | Spring Boot 3.3 · Java 21 · Maven |
| ORM | MyBatis-Plus 3.5 |
| 安全 | Spring Security 6 + JWT（jjwt） |
| 数据库 | MySQL 8 |
| 缓存 | Redis 7（短信验证码、门店缓存） |
| API 文档 | springdoc-openapi（Swagger UI） |
| 部署 | Docker Compose |

## 前置依赖

- Docker Desktop 24+（含 Docker Compose v2）
- 仅本地开发前端时：Node.js 20+ / npm 10+

## 一键启动

```bash
docker compose up --build
```

启动后访问：

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端健康检查 | http://localhost:8080/healthz |
| API 文档（Swagger） | http://localhost:8080/swagger-ui.html |
| MySQL | localhost:3307，库 `neko_cafe`，用户 `neko` / `neko` |
| Redis | localhost:6379 |

## 演示账号

固定验证码：**123456**（沙箱环境）

登录流程：`POST /api/auth/sms/send` 发送验证码（沙箱直接返回），再 `POST /api/auth/login` 携带手机号与验证码 `123456`。登录响应：

```json
{
  "data": {
    "access_token": "<JWT>",
    "token_type": "Bearer",
    "expires_in": 86400,
    "user": { "id": 1, "name": "林小满", "role": "customer", "...": "..." },
    "token": "<JWT>（兼容旧字段，等同 access_token）"
  }
}
```

JWT 解码后含显式 `userId` 与 `role` claim（FR-AUTH-002）。

| 角色 | 手机号 | 登录后页面 | 权限范围 |
|------|--------|-----------|----------|
| 顾客 | 13800000001 | 温馨首页 | 浏览门店、预约桌位、在线点单、评价、个人中心 |
| 店员 | 13800000002 | 今日预约看板 | 预约管理、订单管理、桌位状态、入座/完桌操作 |
| 店长 | 13800000003 | 数据看板 | 门店经营数据、人员管理、猫咪档案、门店管理 |
| 总部运营 | 13800000004 | 全局数据 | 跨门店分析、活动配置、门店管理 |
| 猫咪管家 | 13800000005 | 猫咪档案 | 猫咪健康档案、体重记录、疫苗管理、照片上传 |
| 管理员 | 13800000006 | 数据看板 | 全部管理权限 |

## 主流程演示

1. 访问 http://localhost:5173 → 自动跳转登录页
2. 选择演示账号登录 → 进入对应角色界面
3. 顾客：浏览门店 → 预约桌位（可视化选桌 + 偏好标签 + 推荐）→ 点单 → 支付沙箱 → 评价
4. 店员：今日预约看板（10秒自动刷新）→ 入座/完桌操作 → 订单管理
5. 管理员：数据看板（ECharts 图表）→ 猫咪档案管理（照片上传 + 体重趋势图）→ 门店管理

预约状态机：`created → booked → seated → dining → finished / cancelled / no_show`，完桌必须经过 `dining`，非法跳转返回 400、重复预约返回 409。数据库与 API 统一存英文状态，前端展示对应中文标签。

## 前端架构

```
frontend/src/
├── main.ts                          # 入口
├── App.vue                          # 根组件（认证恢复）
├── router/index.ts                  # 路由 + 角色守卫
├── stores/
│   ├── auth.ts                      # 认证状态（token/user）
│   └── cart.ts                      # 购物车状态
├── layouts/
│   ├── CustomerLayout.vue           # 顾客端布局（顶部导航）
│   ├── StaffLayout.vue              # 店员端布局
│   └── AdminLayout.vue              # 管理端布局
├── components/
│   ├── CustomerNavbar.vue           # 顶部导航（角色自适应）
│   ├── PreferenceChips.vue          # 偏好标签组件
│   ├── Recommendations.vue          # 推荐面板
│   ├── ReservationForm.vue          # 预约表单
│   └── PhotoUpload.vue              # 照片上传组件
├── views/
│   ├── Login.vue                    # 登录页（6角色演示账号）
│   ├── NotFound.vue                 # 404页面
│   ├── customer/
│   │   ├── CustomerHome.vue         # 顾客首页（快捷入口+推荐）
│   │   ├── CustomerStores.vue       # 门店列表
│   │   ├── CustomerStoreDetail.vue  # 门店详情（猫咪/桌位/菜单）
│   │   ├── CustomerReservation.vue  # 预约页（可视化选桌+推荐+点单）
│   │   ├── CustomerOrder.vue        # 点单页（购物车）
│   │   ├── CustomerPayment.vue      # 支付沙箱页
│   │   ├── CustomerReviews.vue      # 评价页（星级评分+文字评价）
│   │   └── CustomerProfile.vue      # 个人中心（预约/订单历史）
│   ├── staff/
│   │   ├── StaffToday.vue           # 今日预约看板（自动刷新）
│   │   ├── StaffReservations.vue    # 预约管理
│   │   ├── StaffOrders.vue          # 订单管理
│   │   └── StaffTables.vue          # 桌位状态（排队显示）
│   └── admin/
│       ├── AdminDashboard.vue       # 数据看板（KPI+ECharts图表）
│       ├── AdminCats.vue            # 猫咪健康档案（照片+体重趋势图）
│       ├── AdminStores.vue          # 门店管理（桌位+菜品照片上传）
│       ├── AdminStaff.vue           # 人员管理（店长专用）
│       └── AdminCampaigns.vue       # 活动配置（运营专用）
├── types/index.ts                   # TypeScript 类型定义
├── utils/
│   ├── http.ts                      # Axios 客户端（JWT拦截器+401处理）
│   ├── format.ts                    # 格式化工具（cents/statusLabel等）
│   └── fallback.ts                  # 离线降级数据
├── composables/
│   └── usePolling.ts                # 轮询 composable（店员自动刷新）
└── styles/
    ├── index.css                    # 样式入口
    ├── variables.css                # CSS 自定义属性
    ├── element-overrides.css        # Element Plus 主题覆盖
    ├── layout.css                   # 布局样式
    └── components.css               # 组件样式
```

## 后端架构

```
backend/neko-server/src/main/java/com/nekocafe/
├── NekoServerApplication.java       # 启动类
├── config/
│   ├── SecurityConfig.java          # JWT + RBAC 安全配置
│   └── WebConfig.java               # 静态资源（uploads目录）
├── common/
│   ├── ApiResponse.java             # 统一响应 {data: ...}
│   ├── ApiException.java            # 业务异常
│   ├── GlobalExceptionHandler.java  # 全局异常处理
│   └── Payloads.java                # 请求体解包工具
├── security/
│   ├── JwtService.java              # JWT 生成/验证
│   ├── JwtAuthFilter.java           # JWT 过滤器
│   └── AuthUser.java                # 认证用户主体
├── entity/                          # MyBatis-Plus 实体
├── mapper/                          # MyBatis-Plus Mapper
├── service/                         # 业务服务层
├── dto/                             # 请求/响应 DTO
└── web/                             # REST 控制器
    ├── AuthController.java          # 登录/验证码
    ├── CatalogController.java       # 门店/桌位/菜品/猫咪/推荐/看板
    ├── OrderController.java         # 订单/支付
    ├── ReservationController.java   # 预约/状态机
    ├── ReviewController.java        # 评价
    ├── HealthController.java        # 健康检查
    └── UploadController.java        # 文件上传
```

## API 端点总览

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | /api/auth/sms/send | 公开 | 发送验证码（沙箱固定 123456） |
| POST | /api/auth/login | 公开 | 登录，返回 access_token（JWT 含 userId/role） |
| GET | /api/users/me | 需登录 | 当前用户资料 |
| PUT | /api/users/me | 需登录 | 更新姓名/手机号/偏好（手机号唯一性校验，冲突 409） |
| GET | /api/users/me/member | 需登录 | 会员等级与权益 |
| GET | /api/users/me/points/history | 需登录 | 积分明细（按时间倒序，含来源与变动后余额） |
| ANY | /api/admin/dashboard/** | manager/operator/admin | 管理看板查询 |
| ANY | /api/admin/stores/** | manager/operator/admin | 门店管理 |
| ANY | /api/admin/tables/** | manager/operator/admin | 桌位管理 |
| GET | /api/stores | 公开 | 门店列表 |
| GET | /api/tables | 公开 | 桌位列表+时段可用性 |
| GET | /api/menu-items | 公开 | 菜品列表 |
| GET | /api/cats | 公开 | 猫咪档案列表 |
| GET | /api/cat-health-records | 公开 | 猫咪健康记录 |
| POST | /api/cat-health-records | cat_keeper/manager/admin | 新增健康记录 |
| GET | /api/vaccine-records | 公开 | 疫苗记录 |
| GET | /api/recommendations | 公开 | 规则推荐 |
| GET | /api/dashboard/summary | 公开 | 运营看板汇总 |
| GET | /api/reservations | 公开 | 预约列表 |
| POST | /api/reservations | 需登录 | 创建预约 |
| PATCH | /api/reservations/{id}/status | staff/manager/operator/admin | 预约状态流转（严格状态机） |
| PATCH | /api/reservations/{id}/cancel | 需登录 | 取消预约（顾客取消本人 created/booked，后台可代客取消） |
| GET | /api/reservations/{id}/events | 公开 | 预约事件日志（含 created_at 与中文状态标签） |
| GET | /api/orders | 公开 | 订单列表 |
| POST | /api/orders | 需登录 | 创建订单+支付沙箱 |
| GET | /api/reviews | 公开 | 评价列表 |
| POST | /api/reviews | 需登录 | 提交评价 |
| POST | /api/upload | 需登录 | 上传图片 |
| PATCH | /api/cats/{id}/photo | 需登录 | 更新猫咪照片 |
| PATCH | /api/menu-items/{id}/photo | 需登录 | 更新菜品照片 |

## M1~M6 模块覆盖

| 模块 | 前端页面 | 功能说明 |
|------|----------|----------|
| M1 用户与会员 | Login.vue, CustomerProfile.vue | SMS 登录、JWT 持久化、资料维护（姓名/手机号/偏好）、会员等级/积分展示、积分明细 |
| M2 门店与桌位 | CustomerStores.vue, CustomerStoreDetail.vue, StaffTables.vue | 门店列表、猫咪/桌位/菜单详情、可视化选桌、排队显示 |
| M3 预约与点单 | CustomerReservation.vue, CustomerOrder.vue, CustomerPayment.vue | 可视化选桌+偏好标签+推荐、购物车、支付沙箱 |
| M4 订单与履约 | CustomerProfile.vue, StaffOrders.vue | 订单列表、取消预约、状态机（created→booked→seated→dining→finished） |
| M5 店员后台 | StaffToday.vue, StaffReservations.vue, StaffOrders.vue | 今日看板（10s轮询）、预约/订单管理、异常告警 |
| M6 数据看板 | AdminDashboard.vue | KPI卡片、ECharts柱状图/饼图/折线图、告警+评价 |

## 选做功能

| 功能 | 状态 | 说明 |
|------|------|------|
| AI 规则推荐 | ✅ | 基于偏好标签匹配猫咪/桌位/菜品，/api/recommendations |
| 猫咪健康档案 | ✅ | AdminCats.vue：增删查、体重趋势图（ECharts）、疫苗记录、照片上传 |
| 顾客评价 | ✅ | CustomerReviews.vue：星级评分+文字评价+关联猫咪 |
| 照片上传 | ✅ | PhotoUpload.vue：猫咪/菜品照片上传，顾客端展示 |

## 数据库

- Schema 迁移（Flyway 按文件名顺序执行）：
  - `db/migrations/V001__init.sql`：基础表结构与初始种子。
  - `db/migrations/V002__d01_upgrade.sql`：D-01 升级——预约状态机扩展（活跃状态 `created/booked/seated/dining`）、桌位/用户双重活跃唯一约束、`reservation_events` 审计表等。
  - `db/migrations/V003__reservation_backend_completion.sql`：预约后台补全——新增推荐日志表 `recommendation_logs`、`reservation_events` 状态索引，并幂等兜底唯一约束（兼容已执行过 V001/V002 的库）。
  - `db/migrations/V004__m6_dashboard.sql` / `V005__m2_store_table.sql` / `V006__m1_review_module.sql`：看板、门店桌位管理、评价模块扩展。
  - `db/migrations/V007__m1_member_points.sql`：会员积分流水表 `point_transactions`——记录每次积分变更的 delta 与变动后余额，`(source_type, source_id)` 唯一约束防重复发放（预约完成 +10 分、订单支付/撤销均落流水）。
- 演示数据：`db/seed_data.sql`（非 Flyway 管理，按需手动导入）——审计事件遵循 `booked→seated→dining→finished` 状态机口径。
- MySQL 字符集：`utf8mb4`（docker-compose 中配置 `--character-set-server=utf8mb4`）
- 种子数据包含：6个用户、2个门店、4只猫、5道菜品、17条预约、11个订单、12条评价、12条健康记录

## 本地开发

```bash
# 前端
npm --prefix frontend install
npm --prefix frontend run dev   # http://localhost:5173

# 后端（需本地 JDK 21 + Maven）
mvn -f backend/neko-server/pom.xml spring-boot:run
```

## 常见问题

| 问题 | 解决方案 |
|------|----------|
| Docker 镜像拉取失败（429） | 更换 Docker 镜像源，编辑 ~/.docker/daemon.json |
| 中文乱码 | 确保 MySQL 使用 `--character-set-server=utf8mb4`，Nginx 配置 `charset utf-8` |
| 端口被占用 | 停止占用进程或修改 docker-compose.yml 端口映射 |
| 数据库需要重置 | `docker compose down -v` 清空数据卷后再 `up --build` |
| 全新本地库（local-db）V005 迁移报语法错误 | `V005__m2_store_table.sql` 使用了 `ADD COLUMN IF NOT EXISTS`，团队 RDS（阿里云 MySQL 8.0.36）可执行但本地 mysql:8.4 不支持。临时方案：手工执行去掉 `IF NOT EXISTS` 的 V005 及后续迁移，再以 `SPRING_FLYWAY_ENABLED=false` 启动后端（修改 V005 文件会导致 RDS 上 Flyway 校验和不匹配，需配合 repair，谨慎处理） |
| 照片上传失败 | 确保用店长/管理员/猫咪管家角色登录，检查后端日志 |

## 团队

- 组长：高范铖（gaovancheng@gmail.com）/ G17 · T-01
