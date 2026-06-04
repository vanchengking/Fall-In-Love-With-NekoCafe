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

固定验证码：**8888**（沙箱环境）

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

预约状态机：`booked → seated → finished / cancelled / no_show`，非法跳转会被拒绝。

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

## API 端点总览（18个）

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | /api/auth/sms/send | 公开 | 发送验证码（沙箱） |
| POST | /api/auth/login | 公开 | 登录，返回 JWT |
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
| PATCH | /api/reservations/{id}/status | staff/manager/operator/admin | 预约状态流转 |
| GET | /api/reservations/{id}/events | 公开 | 预约事件日志 |
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
| M1 用户与会员 | Login.vue, CustomerProfile.vue | SMS 登录、JWT 持久化、会员等级/积分展示、偏好标签 |
| M2 门店与桌位 | CustomerStores.vue, CustomerStoreDetail.vue, StaffTables.vue | 门店列表、猫咪/桌位/菜单详情、可视化选桌、排队显示 |
| M3 预约与点单 | CustomerReservation.vue, CustomerOrder.vue, CustomerPayment.vue | 可视化选桌+偏好标签+推荐、购物车、支付沙箱 |
| M4 订单与履约 | CustomerProfile.vue, StaffOrders.vue | 订单列表、取消预约、状态机（booked→seated→finished） |
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

- Schema：`db/migrations/V001__init.sql` + `V002__d01_upgrade.sql`
- MySQL 字符集：`utf8mb4`（docker-compose 中配置 `--character-set-server=utf8mb4`）
- 种子数据包含：6个用户、2个门店、3只猫、3道菜品、17条预约、11个订单、12条评价、12条健康记录

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
| 照片上传失败 | 确保用店长/管理员/猫咪管家角色登录，检查后端日志 |

## 团队

- 组长：高范铖（gaovancheng@gmail.com）/ G17 · T-01
