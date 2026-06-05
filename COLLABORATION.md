# NekoCafe 团队协作指南

## 一、分支策略

`main` 分支受保护，禁止直接 push。每人按负责模块创建 feature 分支：

```
main
├── feat/m3-reservation        ← 高范铖：预约核心模块、订单状态机
├── feat/m2-store-table        ← 成睿轩：门店与桌位后端、数据看板
├── feat/m1-member-m4-order    ← 葛宇晨：会员模块、订单后端、测试
└── feat/frontend-pages        ← 卢欣怡：全部 Vue 3 前端页面
```

**操作流程：**

```bash
# 1. 克隆仓库
git clone https://github.com/vanchengking/Fall-In-Love-With-NekoCafe.git
cd Fall-In-Love-With-NekoCafe

# 2. 创建自己的分支
git checkout -b feat/mX-xxx

# 3. 开发完成后推送
git push -u origin feat/mX-xxx

# 4. 在 GitHub 上创建 Pull Request → main，指定其他成员 review
```

---

## 二、文件归属（避免冲突）

| 成员 | 后端新建文件 | 前端负责 |
|------|-------------|---------|
| 高范铖 | `Reservation*.java`、`RecommendationService.java`、`V003__m3_*.sql` | — |
| 成睿轩 | `StoreMapper.java`、`DashboardMapper.java`、`V003__m2_*.sql` | — |
| 葛宇晨 | `UserController.java`、`OrderController.java`、`ReviewController.java`、测试类 | — |
| 卢欣怡 | — | `views/`、`components/`、`router/`、`stores/` |

**核心原则：每人新建自己的文件，不修改别人负责的文件。**

### 共享文件处理

- `CatalogMapper.java`：各成员新建自己的 Mapper，逐步将查询迁移出去，不在原文件追加
- `router/index.ts`：各模块新增路由写在自己的路由文件，主路由文件只做 import 聚合
- `types/index.ts`：新增类型写在各自的 `types/mX.ts`，主文件只做 export 聚合
- `docker-compose.yml` / `application.yml`：如需修改，先在群里沟通

---

## 三、数据库变更规范

- 所有 DDL/DML 变更放在 `db/migrations/V003__mX_xxx.sql`
- 按模块命名，避免文件名冲突：
  - `V003__m1_user_module.sql`
  - `V003__m2_store_table.sql`
  - `V003__m3_reservation.sql`
  - `V003__m4_order_module.sql`
- 文件开头加 `SET NAMES utf8mb4; SET CHARACTER SET utf8mb4;`
- 使用 `INSERT IGNORE` 保证可重复执行（幂等）
- 表名、列名统一 snake_case

---

## 四、本地开发环境

```bash
# 一键启动（首次或数据库结构变更后）
docker compose down -v   # 清除旧数据
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

## 五、演示账号

固定验证码：**8888**

| 角色 | 手机号 |
|------|--------|
| 顾客 | 13800000001 |
| 店员 | 13800000002 |
| 店长 | 13800000003 |
| 总部运营 | 13800000004 |
| 猫咪管家 | 13800000005 |
| 管理员 | 13800000006 |

---

## 六、CI / CD

每次 push 或 PR 会自动运行 GitHub Actions：

- `backend-springboot`：Maven 构建 + JUnit 测试
- `prototype-backend`：Express 原型语法检查 + 测试
- `frontend`：`npm run build` 构建检查
- `docker-build`：后端 Docker 镜像构建检查

**PR 合并前必须 CI 全绿。**

---

## 七、代码规范

- Java：遵循 Spring Boot 惯例，Controller/Service/Mapper 三层分离
- TypeScript/Vue：使用 Composition API（`<script setup>`），组件用 PascalCase
- 提交信息格式：`feat/fix/refactor/test/docs: 简要描述`
- 一个 commit 只做一件事，避免大杂烩提交
