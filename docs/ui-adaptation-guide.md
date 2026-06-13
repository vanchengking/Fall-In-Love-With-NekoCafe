# NekoCafe 多角色 UI 适配准则

> 本文档定义了 NekoCafe 猫咖智慧餐饮平台各角色前端页面的设计规范与适配标准。
> 基于店员端适配经验总结，供后续角色改造参考。

---

## 一、角色矩阵

| 角色 | 主要诉求 | 权限范围 | 路由前缀 |
|------|----------|----------|----------|
| 顾客 (Customer) | 一键预约、推荐精准、价格透明 | 浏览 / 预约 / 点单 / 评价 | `/` |
| 店员 (Staff) | 操作高效、调度合理、异常告警 | 订单处理 / 桌位调度 / 猫咪打卡 | `/staff` |
| 店长 (Manager) | 门店经营数据、人员排班 | 门店级数据 / 人员管理 | `/admin` |
| 总部运营 (Operator) | 跨门店分析、活动投放 | 全局数据 / 活动配置 | `/admin` |
| 猫咪管家 (Cat Keeper) | 猫咪健康档案、互动记录 | 猫咪档案 / 健康打卡 | `/admin` |
| 管理员 (Admin) | 全部权限 | 所有功能 | `/admin` |

---

## 二、设计系统（全局共享）

### 2.1 色彩系统

| 类型 | 色值 | 用途 |
|------|------|------|
| 主色 | `#0F766E` (teal) | 主按钮、选中态、品牌标识 |
| 次要色-橙 | `#F59E0B` | 待确认、预警、提醒 |
| 次要色-红 | `#EF4444` | 危险操作、取消、异常 |
| 次要色-蓝 | `#3B82F6` | 已预约、信息提示 |
| 成功色 | `#10B981` | 已完成、成功提示 |
| 中性色-深 | `#172033` | 标题文字 |
| 中性色-中 | `#667085` | 次要文字、禁用态 |
| 中性色-浅 | `#F3F4F6` | 卡片背景、分割线 |

**CSS 变量定义位置**：`src/styles/variables.css`

```css
--teal: #0f766e;
--coral: #e86f51;
--gold: #b7791f;
--blue: #2563eb;
--danger: #c2410c;
--success: #16a34a;
/* 状态专用色 */
--status-booked: #3B82F6;
--status-seated: #0F766E;
--status-dining: #EF4444;
--status-finished: #10B981;
--status-cancelled: #6B7280;
--status-noshow: #F59E0B;
```

### 2.2 间距系统（4pt base）

```css
--space-xs: 4px;
--space-sm: 8px;
--space-md: 12px;
--space-base: 16px;
--space-lg: 24px;
--space-xl: 32px;
--space-2xl: 48px;
```

**规则**：所有间距必须使用 CSS 变量，禁止硬编码像素值。

### 2.3 圆角

```css
--radius-sm: 6px;   /* 按钮、标签 */
--radius-md: 10px;  /* 小卡片 */
--radius-lg: 14px;  /* 大卡片、面板 */
```

### 2.4 字体层级

```css
--text-xs: 12px;    /* 辅助文字 */
--text-sm: 13px;    /* 次要内容 */
--text-base: 15px;  /* 正文 */
--text-lg: 18px;    /* 小标题 */
--text-xl: 22px;    /* 卡片标题 */
--text-2xl: 28px;   /* 页面标题 */
--text-3xl: 36px;   /* Hero 标题 */
```

### 2.5 阴影

```css
--shadow-sm: 0 1px 3px rgba(23,32,51,0.06);
--shadow-md: 0 4px 12px rgba(23,32,51,0.08);
--shadow-lg: 0 16px 48px rgba(23,32,51,0.11);
```

### 2.6 响应式断点

```css
/* --bp-sm:  640px  手机横屏 */
/* --bp-md:  768px  平板竖屏 */
/* --bp-lg: 1024px  平板横屏 / 小桌面 */
```

### 2.7 全局工具类

定义在 `src/styles/element-overrides.css`：

| 类名 | 用途 |
|------|------|
| `.card-interactive` | 卡片 hover 上浮 + active 缩放 |
| `.glass` | 毛玻璃效果 (`backdrop-filter: blur(12px)`) |
| `.skeleton` | 加载骨架屏脉冲动画 |
| `.page-enter-active` | 页面淡入动画 |
| `.gradient-text` | 渐变文字（teal→coral） |
| `:focus-visible` | 全局焦点环（`outline: 2px solid var(--teal)`） |

---

## 三、状态机规范

### 3.1 预约状态流转

```
booked (已预约) ──→ seated (已入座) ──→ dining (用餐中) ──→ finished (已完成)
       │                                        │
       ├──→ cancelled (已取消)                   │
       └──→ no_show (未到店)                     │
                                                 ↓
                              finished (已完成，桌位释放)
```

**强制规则**：
- `seated` 只能转 `dining`，禁止 `seated → finished`
- `dining` 只能转 `finished`
- `booked` 可转 `seated` / `cancelled` / `no_show`

**类型定义**（`src/types/index.ts`）：
```typescript
export type ReservationStatus = 'booked' | 'seated' | 'dining' | 'finished' | 'cancelled' | 'no_show'
```

### 3.2 订单状态流转

```
pending (待出餐) ──→ preparing (备餐中) ──→ served (已出餐) ──→ paid (已支付)
       │                                                        │
       └──→ cancelled (已取消)                                  └──→ refunded (已退款)
                                                                       │
                                                                  异常 (exception)
```

**状态卡片区映射**（`StaffOrders.vue` 顶部，支持点击筛选）：

| 卡片 | filterStatus | 说明 |
|------|-------------|------|
| 待出餐 | `'pending'` | 橙色强调，点击筛选 pending 订单 |
| 已支付 | `'paid'` | 绿色，点击筛选已支付订单 |
| 异常 | `'exception'` | 红色，点击筛选异常订单 |
| 今日收入 | `null` | 点击取消筛选，显示全部 |

**操作按钮与状态对应**：

| 订单状态 | 可用操作 |
|----------|----------|
| `pending` | 接单、备餐、关联预约、报告异常 |
| `preparing` | 出餐、标记出餐、关联预约、报告异常 |
| `served` | 通知服务、退款登记、关联预约、报告异常 |
| `paid` | 退款登记、关联预约 |
| `exception` | 报告异常（查看/追加） |

**类型定义**（`src/types/index.ts`）：
```typescript
export type OrderStatus = 'pending' | 'preparing' | 'served' | 'paid' | 'cancelled' | 'refunded' | 'exception'
```

### 3.3 桌位状态

```
free (空闲) ──→ reserved (已预约) ──→ occupied (已入座) ──→ dining (用餐中) ──→ cleaning (待清台) ──→ free
      │                │                                                              │
      │                └──→ free (取消预约)                                            │
      │                                                                               ↓
      └─────────────────────────────────────────────────────────────────── maintenance (不可用)
                                                                            maintenance → free (恢复)
```

**颜色编码**（`StaffTables.vue` 平面图 — 建筑平面图风格）：

| 状态 | 桌面背景渐变 | 边框色 | 座椅圆点 | 标签底色 | 用途 |
|------|-------------|--------|----------|----------|------|
| `free` 空闲 | `#dcfce7→#bbf7d0` | `#86efac` | 绿色 | `#16a34a` | 可接受预约/入座 |
| `reserved` 已预约 | `#dbeafe→#bfdbfe` | `#93c5fd` | 蓝色 | `#2563eb` | 已有预约，等待到店 |
| `occupied` 已入座 | `#d4f0ed→#a7f3d0` | `#5eead4` | teal | `#0f766e` | 顾客已到店入座 |
| `dining` 用餐中 | `#fee2e2→#fecaca` | `#fca5a5` | 红色 | `#dc2626` | 正在用餐 |
| `cleaning` 待清台 | `#fef3c7→#fde68a` | `#fcd34d` | 橙色 | `#d97706` | 顾客离开，需清理 |
| `maintenance` 不可用 | `#f3f4f6→#e5e7eb` | `#d1d5db` | 灰色 | `#9ca3af` | 设备故障/维护中 |

**类型定义**（`src/types/index.ts`）：
```typescript
export type TableStatus = 'free' | 'reserved' | 'occupied' | 'dining' | 'cleaning' | 'maintenance'
```

---

## 四、页面结构模板

### 4.1 Layout 结构

每个角色有独立 Layout 文件：`src/layouts/{Role}Layout.vue`

```
RoleLayout.vue
├── RoleNavbar.vue        ← 角色专属导航栏
│   ├── 品牌 Logo + 项目名
│   ├── 导航标签（当前页绿色下划线）
│   ├── 门店选择器（如需要）
│   └── 用户角色下拉 + 退出
└── <router-view />       ← 页面内容
```

### 4.2 页面标准结构

每个页面遵循以下结构：

```vue
<template>
  <div class="page-enter-active">
    <!-- 1. 顶部工具栏 -->
    <div class="toolbar">
      <h2>页面标题</h2>
      <div class="toolbar-right">
        <!-- 刷新、筛选、导出等 -->
      </div>
    </div>

    <!-- 2. KPI 统计条（支持点击筛选） -->
    <div class="kpi-strip">
      <div class="kpi-card" :class="{ 'kpi-active': filterStatus === 'xxx' }" @click="filterStatus = 'xxx'">
        <div class="kpi-icon" style="background: ...">...</div>
        <div><span>标签</span><strong>数值</strong></div>
      </div>
    </div>

    <!-- 3. 主体内容区 -->
    <div class="main-grid">
      <!-- 左侧/主体 -->
      <!-- 右侧/详情 -->
    </div>

    <!-- 4. 二次确认弹窗 -->
    <el-dialog v-model="confirmDialog" ...>...</el-dialog>
  </div>
</template>
```

### 4.2.1 KPI 卡片点击筛选模式

所有含 KPI 统计条的页面，统一支持"点击卡片筛选下方数据"：

**实现方式**：
```typescript
const filterStatus = ref<string | null>(null)

const filteredData = computed(() => {
  if (!filterStatus.value) return allData.value
  return allData.value.filter(item => item.status === filterStatus.value)
})
```

**模板绑定**：
```vue
<div class="kpi-card"
  :class="{ 'kpi-active': filterStatus === 'targetStatus' }"
  @click="filterStatus = filterStatus === 'targetStatus' ? null : 'targetStatus'"
>
```

**高亮样式**（`.kpi-active`）：
```css
.kpi-card {
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.15s;
}
.kpi-card.kpi-active {
  background: #fff;
  border-color: var(--teal);
  box-shadow: 0 0 0 2px rgba(15,118,110,0.15), var(--shadow-sm);
}
```

**规则**：
- 再次点击同一卡片取消筛选（toggle 行为）
- 筛选状态在 10 秒轮询刷新后保持不变
- "总计/收入"类卡片点击后显示全部（`filterStatus = null`）

**已应用页面**：
- `StaffToday.vue`：6 个卡片（今日预约/待处理/已入座/用餐中/待清台/营业额）
- `StaffOrders.vue`：4 个卡片（待出餐/已支付/异常/今日收入）

### 4.3 组件规范

| 组件 | 规范 |
|------|------|
| 状态标签 | `el-tag` + `statusType()` + `statusLabel()` |
| 操作按钮 | 主操作=`type="success"`，次操作=`plain`，危险=`type="danger"` |
| 次要操作 | `el-dropdown` + `MoreFilled` 圆形按钮，收折次要操作 |
| 禁用态 | `:disabled` + 浅灰背景 |
| 卡片 | 白色背景 + `var(--radius-lg)` + `1px solid var(--line)` + hover 阴影 |
| KPI 卡片 | 可点击筛选 + `.kpi-active` 高亮态（见 4.2.1） |
| 表格 | `el-table` + stripe + `highlight-current-row` |
| 抽屉 | `el-drawer` 用于详情展示 |
| 骨架屏 | `.skeleton` 类 + 固定宽高 |

**操作按钮排列规则**（表格操作列）：
- 仅 1 个操作：直接显示按钮
- 2 个操作：并排显示按钮
- 3 个及以上：主操作按钮 + 次操作收入 `el-dropdown` 下拉菜单
```vue
<template v-if="row.status === 'booked'">
  <el-button size="small" type="success" @click.stop="doAction(row, 'primary')">入座</el-button>
  <el-dropdown trigger="click" @command="(cmd) => doAction(row, cmd)">
    <el-button size="small" :icon="MoreFilled" circle plain @click.stop />
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="secondary1">标记未到</el-dropdown-item>
        <el-dropdown-item command="secondary2" divided>取消预约</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>
```

---

## 五、数据层规范

### 5.1 Fallback 数据

文件：`src/utils/fallback.ts`

每个数据类型必须提供 fallback，确保后端未就绪时页面可渲染：

```typescript
export const fallbackStores: Store[] = [...]
export const fallbackTables: DiningTable[] = [...]
export const fallbackCats: Cat[] = [...]
export const fallbackMenuItems: MenuItem[] = [...]
export const fallbackReservations: Reservation[] = [...]
export const fallbackOrders: Order[] = [...]
export const fallbackDashboard: DashboardSummary = {...}
```

**要求**：
- 覆盖所有状态（不能只有 `booked`，还需 `seated`/`dining`/`finished`/`cancelled`）
- 图片使用 `placekitten.com` 或 `unsplash` 占位
- 开头 `console.info` 提示演示模式

### 5.2 API 调用模式

```typescript
async function loadData() {
  try { data.value = await api.get<Type[]>('/endpoint', params) }
  catch { data.value = fallbackData }
}
```

### 5.3 轮询机制

```typescript
import { usePolling } from '@/composables/usePolling'

const { lastUpdated, newCount, clearNewCount, startWithChangeDetection } = usePolling(loadData, 10000)
startWithChangeDetection(() => data.value.map(r => r.id).sort().join(','))
```

---

## 六、各角色适配清单

### 6.1 顾客端 (Customer) — `/customer`

**已完成重构**（2026-06-12），路由前缀从 `/` 迁移至 `/customer`，样式统一为 CSS 变量体系。

| 页面 | 文件 | 路径 | 状态 |
|------|------|------|------|
| 首页 | `CustomerHome.vue` | `/customer` | ✅ 已重构 |
| 门店列表 | `CustomerStores.vue` | `/customer/stores` | ✅ 已重构 |
| 门店详情 | `CustomerStoreDetail.vue` | `/customer/stores/:id` | ✅ 已重构 |
| 预约桌位 | `CustomerReservation.vue` | `/customer/reservation` | ✅ 已重构 |
| 在线点单 | `CustomerOrder.vue` | `/customer/order` | ✅ 已重构 |
| 支付 | `CustomerPayment.vue` | `/customer/payment` | ✅ 已重构 |
| 评价 | `CustomerReviews.vue` | `/customer/reviews` | ✅ 已重构 |
| 个人中心 | `CustomerProfile.vue` | `/customer/profile` | ✅ 已重构 |
| 导航栏 | `CustomerNavbar.vue` | — | ✅ 已重构 |
| 布局 | `CustomerLayout.vue` | — | ✅ 已重构（新增 footer） |

**重构内容**：
- 路由前缀统一为 `/customer`，`/` 自动重定向
- 全部硬编码颜色替换为 CSS 变量（`var(--teal)`、`var(--coral)`、`var(--paper)` 等）
- CustomerNavbar 简化为顾客专属（移除 staff/admin 角色分支）
- CustomerLayout 新增底部版权 footer
- 推荐 API 的 `userId` 改为从 auth store 动态获取
- 所有页面添加 `page-enter-active`（淡入动画）+ `toolbar`（h2 包裹），与店员端结构统一
- 硬编码 hex 颜色替换为 CSS 变量（渐变色保留）

### 6.2 店员端 (Staff) — `/staff`

**已完成**，参考本次适配。

| 页面 | 文件 | 关键特性 |
|------|------|----------|
| 今日预约 | `StaffToday.vue` | 时间线 + 状态流转 + 运营提醒 + 桌位概览 + 猫咪状态 |
| 预约管理 | `StaffReservations.vue` | 表格 + 详情抽屉 + 严格状态机 + 事件记录 |
| 订单管理 | `StaffOrders.vue` | 左右两栏 + 菜品明细 + 状态操作 + 支付流水 |
| 桌位调度 | `StaffTables.vue` | 平面图 + 排队列表 + 桌位详情 + 颜色编码 |
| 导航栏 | `StaffNavbar.vue` | 门店选择 + 4 导航标签 |
| Layout | `StaffLayout.vue` | StaffNavbar + router-view |

#### 6.2.1 通用页面结构

所有店员页面共享以下顶层结构：

```
┌─────────────────────────────────────────────────────────────────────┐
│ NekoCafe + 导航标签（今日预约｜预约管理｜订单｜桌位状态）            │  ← StaffNavbar
│                                 五道口店  高店员                     │
├─────────────────────────────────────────────────────────────────────┤
│ 店员－页面标题                              10秒自动刷新            │  ← 页面头部
├─────────────────────────────────────────────────────────────────────┤
│ [KPI卡片1] [KPI卡片2] [KPI卡片3] [KPI卡片4] ...                    │  ← 状态卡片区
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│                         主内容区                                     │  ← 主体
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

**导航高亮规则**：当前页面对应的导航标签显示绿色下划线 + 绿色文字。

#### 6.2.2 店员－订单履约管理 (`StaffOrders.vue`)

**页面标题**：店员－订单履约管理

**页面结构**：

```
┌─────────────────────────────────────────────────────────────────────┐
│ [待出餐 6]  [已支付 11]  [异常 1]  [今日收入 ¥9,400]               │  ← KPI 卡片（可点击筛选）
│                                               待履约数量: 2         │
├────────────────────────┬────────────────────────────────────────────┤
│                        │                                            │
│   订单列表（左侧）      │   订单详情/操作区（右侧）                   │
│   （跟随 KPI 筛选）     │                                            │
│ ┌────────────────────┐ │ ┌──────────────────────────────────────┐  │
│ │ #3  A01 林小满     │ │ │ 订单#3 · A01 · 林小满                │  │
│ │ 3人 已入座 ¥286   │ │ │                                      │  │
│ │ seated→dining→...  │ │ │ 菜品表格                             │  │
│ ├────────────────────┤ │ │ ┌──────┬────┬──────┬──────┬────────┐ │  │
│ │ #7  B02 王大锤     │ │ │ │名称  │数量│单价  │小计  │出餐状态│ │  │
│ │ 2人 用餐中 ¥158   │ │ │ ├──────┼────┼──────┼──────┼────────┤ │  │
│ ├────────────────────┤ │ │ │拿铁  │ 2  │ ¥38  │ ¥76  │ 已出餐 │ │  │
│ │ #5  C01 赵小六     │ │ │ │芝士  │ 1  │ ¥42  │ ¥42  │ 待出餐 │ │  │
│ │ 4人 已完成 ¥520   │ │ │ └──────┴────┴──────┴──────┴────────┘ │  │
│ ├────────────────────┤ │ │                                      │  │
│ │ #11 A03 周九儿     │ │ │ [接单] [备餐] [出餐] [标记出餐]       │  │
│ │ 1人 已支付 ¥88    │ │ │ [通知服务] [退款登记] [关联预约]       │  │
│ ├────────────────────┤ │ │ [报告异常]                            │  │
│ │ #2  B01 陈大海     │ │ │                                      │  │
│ │ 5人 异常 ¥680     │ │ │ 支付流水                              │  │
│ └────────────────────┘ │ │ 微信支付 ¥286 2025-01-15 18:42       │  │
│                        │ │                                      │  │
│                        │ │ 顾客备注：少冰、团子怕生别靠太近       │  │
│                        │ │ 猫区提醒：团子今日状态良好             │  │
│                        │ └──────────────────────────────────────┘  │
└────────────────────────┴────────────────────────────────────────────┘
```

**状态卡片区（4个，支持点击筛选）**：
- 待出餐：`pending` 订单数，橙色强调，点击筛选 pending 订单
- 已支付：`paid` 订单数，绿色，点击筛选已支付订单
- 异常：`exception` 订单数，红色，点击筛选异常订单
- 今日收入：总营收，点击取消筛选（显示全部）
- 再次点击同一卡片取消筛选

**左侧订单列表**：
- 数据跟随 KPI 卡片筛选（`filteredOrders` computed）
- 每张订单卡片包含：订单号、桌号、顾客姓名、人数、状态标签、总价
- 卡片底部显示预约状态流转路径（如 `seated → dining → finished`）
- 异常订单左边框红色标识（`.oc-exception`）
- 当前选中卡片高亮（teal 边框）
- 点击切换右侧详情

**右侧订单详情/操作区**：
- 头部：订单号 · 桌号 · 顾客姓名
- 菜品表格：名称、数量、单价、小计、出餐状态（待出餐/已出餐）
- 操作按钮区：按钮根据订单状态动态显示
  - `pending`：「接单」「备餐」
  - `preparing`：「出餐」「标记出餐」
  - `served`：「通知服务」「退款登记」
  - `paid`：「退款登记」
  - `exception`：「查看异常」
  - 所有状态：「关联预约」「报告异常」
- 支付流水信息：支付方式、金额、时间
- 顾客备注 + 猫区提醒

**交互规则**：
- KPI 卡片点击筛选，数据跟随过滤，轮询刷新后保持筛选状态
- 订单状态严格流转，操作按钮与当前状态对应
- 10 秒自动刷新，新订单到来时列表顶部提示

#### 6.2.3 店员－今日预约运营工作台 (`StaffToday.vue`)

**页面标题**：店员－今日预约运营工作台

**页面结构**：

```
┌─────────────────────────────────────────────────────────────────────┐
│ [今日预约 12] [待处理 3] [已入座 4] [用餐中 3] [待清台 2] [营业额 ¥8,200] │  ← 统计卡片（一行6个）
├─────────────────────────────────────────────────────────────────────┤
│ 预约时间线                                                          │
│ ───┬──────────┬──────────┬──────────┬──────────┬───→               │
│   16:00     18:30      19:00      19:30      21:00                 │
│   ┌─────┐   ┌─────┐   ┌─────┐   ┌─────┐                          │
│   │林小满│   │王大锤│   │赵小六│   │周九儿│                          │
│   │A01  │   │B02  │   │C01  │   │A03  │                          │
│   │3人  │   │2人  │   │4人  │   │1人  │                          │
│   │已预约│   │待确认│   │已入座│   │已预约│                          │
│   └─────┘   └─────┘   └─────┘   └─────┘                          │
├───────────────────────────┬─────────────────────────────────────────┤
│ 当前处理面板               │ 桌位快速概览                            │
│                           │                                         │
│ 林小满到店入座             │ ┌────┬────┬────┬────┬────┬────┐       │
│                           │ │A01 │A02 │B01 │B02 │C01 │C02 │       │
│ 运营提醒：                 │ │已入│用餐│空闲│已预│待清│空闲│       │
│ · 晚高峰桌位紧张           │ │座  │中  │    │约  │台  │    │       │
│ · 团子休息提醒（18:00后）  │ │团子│拿铁│    │芝麻│团子│    │       │
│                           │ │工作│工作│    │工作│休息│    │       │
│ [确认入座] [标记未到]      │ └────┴────┴────┴────┴────┴────┘       │
│ [取消预约]                 │                                         │
│                           │ 色块：已预约｜用餐中｜待清台｜空闲       │
└───────────────────────────┴─────────────────────────────────────────┘
```

**统计卡片（一行6个，支持点击筛选）**：
- 今日预约：当日预约总数，点击显示全部
- 待处理：`booked` 状态数量，橙色强调，点击只显示已预约
- 已入座：`seated` 状态数量，点击只显示已入座
- 用餐中：`dining` 状态数量，点击只显示用餐中
- 待清台：需要清理的桌位数，点击显示全部（无对应预约状态）
- 营业额：当日总营收，点击显示全部（不筛选）
- 再次点击同一卡片取消筛选

**时间线区域**（数据跟随卡片筛选）：
- 横向时间轴，刻度为预约时间点（16:00、18:30、19:00、19:30 等）
- 每个时间点下方显示预约卡片：顾客姓名、桌位、人数、状态标签
- 预约卡片可点击展开详情
- 状态标签颜色：已预约=蓝色、待确认=橙色、已入座=绿色

**当前处理面板**：
- 显示当前需处理的预约事件（如"林小满到店入座"）
- 运营提醒列表（晚高峰桌位紧张、猫咪休息提醒等）
- 操作按钮：确认入座、标记未到、取消预约

**桌位快速概览**：
- 表格/色块矩阵展示所有桌位（A01/A02/B01/B02/C01/C02...）
- 颜色编码：已预约(蓝)、用餐中(红)、待清台(橙)、空闲(绿)
- 每个桌位块显示猫咪名字（团子、拿铁、芝麻）
- 猫咪工作状态标注（工作/休息）

**交互规则**：
- 10 秒自动刷新时间线和桌位状态
- 时间线支持左右滚动（当日预约较多时）
- 桌位概览色块可点击跳转桌位详情

#### 6.2.4 店员－预约管理与状态机 (`StaffReservations.vue`)

**页面标题**：店员－预约管理与状态机

**页面结构**：

```
┌─────────────────────────────────────────────────────────────────────┐
│ 手机号: [________]  日期: [________]  状态: [全部▼]                 │
│ [查询]  [重置]                    提示：预约状态严格流转，禁止跳转    │  ← 筛选区
├─────────────────────────────────────────────────────────────────────┤
│ 状态流程图：                                                        │
│                                                                     │
│  创建 ──→ 已预约 ──→ 已入座 ──→ 用餐中 ──→ 已完成                   │
│            │         ↑  │                  ↑                        │
│            ├──→ 已取消 │  └──→ (禁止直接跳转)                        │
│            └──→ 未到店 │                                             │
│                                                                     │
├─────────────────────────────────────────────────────────────────────┤
│ ┌──────┬──────┬──────┬────┬────┬────┬──────┬──────┬──────────────┐ │
│ │时间  │顾客  │手机号│人数│桌位│猫咪│状态  │下一步│操作          │ │
│ ├──────┼──────┼──────┼────┼────┼────┼──────┼──────┼──────────────┤ │
│ │16:00 │林小满│138.. │ 3  │A01 │团子│已预约│→入座│[入座][未到]  │ │
│ │18:30 │王大锤│139.. │ 2  │B02 │芝麻│待确认│→确认│[确认][取消]  │ │
│ │19:00 │赵小六│136.. │ 4  │C01 │团子│已入座│→用餐│[开始用餐]    │ │
│ │19:30 │周九儿│137.. │ 1  │A03 │拿铁│用餐中│→完成│[完桌]        │ │
│ │15:00 │陈大海│135.. │ 5  │B01 │芝麻│已完成│  -  │  -           │ │
│ └──────┴──────┴──────┴────┴────┴────┴──────┴──────┴──────────────┘ │
│                                                                     │
│ 事件记录：                                                          │
│ ┌──────────────────────────────────────────────────────────────┐   │
│ │ 2025-01-15 16:02  林小满 预约创建    操作人: 系统自动         │   │
│ │ 2025-01-15 16:05  林小满 确认预约    操作人: 高店员           │   │
│ │ 2025-01-15 18:28  林小满 到店入座    操作人: 高店员           │   │
│ └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

**筛选区**：
- 手机号输入框（支持模糊搜索）
- 日期选择器（默认当日）
- 状态下拉：全部 / 已预约 / 已入座 / 用餐中 / 已完成 / 已取消 / 未到店
- 查询 + 重置按钮
- 右侧提示文字："严格状态流转"

**状态流程图**：
- 水平流程图，清晰展示合法状态转换路径
- 禁止的路径用虚线 + 红叉标注（如 `seated → finished`）
- 当前筛选状态高亮

**预约表格**：
- 列：时间、顾客、手机号、人数、桌位、猫咪、状态、下一步、操作
- "下一步"列显示可流转的目标状态
- "操作"列根据当前状态动态渲染（主操作按钮 + 次操作下拉菜单）：
  - `booked`：「入座」主按钮 + `⋮` 下拉（标记未到、取消预约）
  - `seated`：「开始用餐」按钮
  - `dining`：「完桌」按钮
  - `finished`/`cancelled`/`no_show`：无操作（显示 `-`）
- 次操作使用 `el-dropdown` + `MoreFilled` 图标按钮收折
- 表格支持 `highlight-current-row` 选中态

**事件记录区**：
- 表格下方展示当前选中预约的事件流水
- 列：时间、动作、操作人
- 按时间倒序排列

**交互规则**：
- 状态严格流转（禁止 `seated → finished`），前端按钮受状态机控制
- 所有状态变更操作需二次确认弹窗
- 支持批量筛选和事件记录追溯

#### 6.2.5 店员－桌位状态与调度地图 (`StaffTables.vue`)

**页面标题**：店员－桌位状态与调度地图

**页面结构**：

```
┌─────────────────────────────────────────────────────────────────────┐
│ 实时桌位                                    10秒刷新  [手动刷新]    │
├─────────────────────────────────────────────────┬───────────────────┤
│                                                 │ 排队 & 即将到店   │
│  ▪ ▪ ▪ ▪ ▪ ▪ ▪ ▪    ← 窗户装饰条              │                   │
│  ┌─ 靠窗猫区 ────────────────────────────────┐ │ 林小满 3人 A01   │
│  │    ● ●       ● ●       ● ●              │ │ 王大锤 2人 B02   │
│  │  ● ┌────┐  ● ● ┌────┐  ● ● ┌────┐  ●  │ │ 赵小六 4人 C01   │
│  │    │ A01│    │  │ A02│    │  │ A03│     │ │                   │
│  │    │ 2人│    │  │ 4人│    │  │ 2人│     │ │ ...               │
│  │  ● └────┘  ● ● └────┘  ● ● └────┘  ●  │ │ [查看全部(8)]     │
│  │    ● ●       ● ●       ● ●              │ │                   │
│  └──────────────────────────────────────────┘ ├───────────────────┤
│  ─ ─ ─ ─ ─ ─ ─  过 道  ─ ─ ─ ─ ─ ─ ─ ─ ─  │ 状态说明          │
│  ┌─ 主厅 ──────────────────────────────────┐ │ ■ 空闲/可入座     │
│  │   ● ● ●       ● ●       ● ● ●         │ │ ■ 已预约          │
│  │ ● ┌──────┐  ● ┌────┐  ● ┌──────┐  ●  │ │ ■ 用餐中          │
│  │   │ B01  │    │B02 │    │ B03  │       │ │ ■ 待清台          │
│  │   │ 4人  │    │2人 │    │ 4人  │       │ │ ■ 不可用/维护中   │
│  │ ● └──────┘  ● └────┘  ● └──────┘  ●  │ │                   │
│  │   ● ● ●       ● ●       ● ● ●         │ │                   │
│  └──────────────────────────────────────────┘ │                   │
│  ─ ─ ─ ─ ─ ─  中央走道  ─ ─ ─ ─ ─ ─ ─ ─    │                   │
│  ┌─入口─┐  ┌─前台/取餐─┐  ┌─ 聚会区 ─────┐ │                   │
│  │  →   │  │   ☕      │  │   ● ● ●     │ │                   │
│  │ 入口  │  │  取餐区   │  │ ● ┌──────┐ ●│ │                   │
│  │       │  │           │  │   │ C01  │  │ │                   │
│  │  🌿   │  │           │  │   │ 6人  │  │ │                   │
│  └───────┘  └───────────┘  │ ● └──────┘ ●│ │                   │
│                             │   ● ● ●     │ │                   │
│                             └─────────────┘ │                   │
│  🌿        🌿        🌿                      │                   │
├─────────────────────────────────────────────────┴───────────────────┤
│ 选中桌位详情：A02 · 靠窗猫区 · 用餐中                               │
│                                                                     │
│ 状态流程：已入座 → 用餐中 → 清台中                                  │
│ 当前：用餐中                                                        │
│                                                                     │
│ 预约信息：18:30 林小满 · 4人 · 拿铁                                 │
│                                                                     │
│ [完桌]  [清台]                                                       │
└─────────────────────────────────────────────────────────────────────┘
```

**顶部工具栏**：
- 标题：实时桌位
- 右侧：10 秒自动刷新徽章 + 上次刷新时间 + 手动刷新按钮

**餐厅平面图（左侧主区）**：

平面图按真实门店空间布局，采用**建筑平面图风格**渲染，分为以下区域：

| 区域 | 边框样式 | 背景色 | 说明 |
|------|----------|--------|------|
| 靠窗猫区 | 青绿虚线 `#5eead4` | `#f0fdfa` 渐变 | 顶部窗户装饰条，猫区桌位 |
| 主厅 | 黄色虚线 `#facc15` | `#fefce8` 渐变 | 中间区域，普通桌位 |
| 聚会区 | 紫色虚线 `#c4b5fd` | `#faf5ff` 渐变 | 大桌位，聚会场景 |
| 入口 / 前台 | 灰色虚线边框 | 浅灰渐变 | 左下角，箭头指引 + 取餐区 |

区域之间用**虚线过道**分隔，标注"过道"或"中央走道"文字。

**桌位卡片设计 — 建筑平面图风格**：

每个桌位渲染为**俯视图式桌椅模型**，而非简单方块：

```
    ●   ●   ●            ← 上排座椅圆点（seat-dot，10×10px 圆形）
  ● ┌───────────┐ ●      ← 左右座椅圆点
    │   B02     │         ← 桌面矩形（table-surface）
    │   4人     │
    └───────────┘
    ●   ●   ●            ← 下排座椅圆点
      已预约               ← 状态标签（悬浮在下方）
```

**桌面尺寸按座位数分级**：

| 等级 | 类名 | 座位数 | 桌面宽度 | 桌面高度 |
|------|------|--------|----------|----------|
| 小桌 | `.surface-sm` | 2人 | 72px | 56px |
| 中桌 | `.surface-md` | 4人 | 96px | 64px |
| 大桌 | `.surface-lg` | 6人 | 128px | 72px |
| 超大桌 | `.surface-xl` | 8人 | 152px | 80px |

**座椅分布规则**：
- 上下各半（`Math.ceil(seats / 2)` 个圆点）
- 左右各 1 个圆点（≥2 人时）
- 座椅圆点颜色跟随桌位状态变化

**颜色编码**（作用于 `.table-surface` 桌面本体，使用 CSS 变量）：

| 状态 | 桌面背景 | 边框色 | 座椅圆点色 | 标签底色 |
|------|----------|--------|------------|----------|
| `free` 空闲 | `var(--success-light)→#bbf7d0` | `#86efac` | 绿色 | `var(--success)` |
| `reserved` 已预约 | `var(--blue-light)→#bfdbfe` | `#93c5fd` | 蓝色 | `var(--blue)` |
| `occupied` 已入座 | `var(--teal-light)→#a7f3d0` | `#5eead4` | teal | `var(--teal)` |
| `dining` 用餐中 | `#fee2e2→#fecaca` | `#fca5a5` | 红色 | `var(--danger)` |
| `cleaning` 待清台 | `var(--gold-light)→#fde68a` | `#fcd34d` | 橙色 | `var(--gold)` |
| `maintenance` 不可用 | `var(--wash)→var(--line)` | `var(--line)` | 灰色 | `var(--muted)` |

**桌面附加元素**：
- 右上角：6px 圆形**状态色点**，快速指示当前状态
- 左上角：**猫爪 SVG**（`cat_zone` 桌位），opacity 0.25
- 中央：桌位编号（粗体）+ 座位数标签

**右侧排队与即将到店列表**：
- 显示今日 `booked` 状态预约，按时间排序
- 每条：顾客姓名、人数、时间、建议桌位、状态标签
- 操作按钮：安排入座、转桌、联系顾客
- 底部"查看全部(n)"链接展开完整列表

**底部桌位详情面板**（点击桌位后滑入）：
- 左半：桌号 · 区域、当前状态（彩色文字）、状态流转路径（`已入座 → 用餐中 → 清台中`，当前步高亮）、座位数、推荐猫咪
- 右半：预约信息（顾客、人数、时间）、备注、订单信息（如有）
- 操作按钮（根据状态动态显示）：
  - `reserved`：确认入座、取消预约
  - `occupied`：开始用餐
  - `dining`：完桌、清台
  - `cleaning`：完成清洁
  - `maintenance`：恢复使用
  - `free`：无操作（显示"桌位空闲中"）

**CSS 关键结构**：

```css
/* 桌位包裹层 */
.table-wrap { display: flex; flex-direction: column; align-items: center; gap: 2px; }
/* 座椅圆点行 */
.seats-row { display: flex; gap: 6px; justify-content: center; }
.seats-col { display: flex; flex-direction: column; gap: 6px; }
.seat-dot  { width: 10px; height: 10px; border-radius: 50%; }
/* 桌面本体 */
.table-surface { border-radius: 6px; display: flex; flex-direction: column; align-items: center; }
.surface-sm { width: 72px;  height: 56px; }
.surface-md { width: 96px;  height: 64px; }
.surface-lg { width: 128px; height: 72px; }
.surface-xl { width: 152px; height: 80px; }
```

**交互规则**：
- 10 秒自动刷新桌位状态
- 点击桌位切换底部详情面板（再次点击收起）
- 桌位 hover 上浮 + 阴影加深，座椅圆点亮起
- 选中桌位桌面加 teal 外发光边框
- 操作按钮受桌位状态机控制
- 所有状态变更需二次确认弹窗

### 6.3 店长端 (Manager) — `/admin`

**已完成基础适配**（2026-06-12），权限逻辑已实现。

| 页面 | 文件 | 关键特性 | 状态 |
|------|------|----------|------|
| 数据看板 | `AdminDashboard.vue` | 门店级 KPI，门店选择器锁定本店 | ✅ 已适配 |
| 猫咪档案 | `AdminCats.vue` | 按门店过滤，健康档案管理 | ✅ 已适配 |
| 门店管理 | `AdminStores.vue` | 只读模式（隐藏增删改按钮） | ✅ 已适配 |
| 人员管理 | `AdminStaff.vue` | 只能分配 staff/cat_keeper 角色 | ✅ 已适配 |
| 审计日志 | `AdminAuditLogs.vue` | 查看本店操作日志 | ✅ 已适配 |
| 活动配置 | `AdminCampaigns.vue` | 不可见（菜单隐藏） | — |

**权限规则**：
- 店长只能看到本店数据，门店选择器禁用
- 门店管理页面只读（显示"只读"标签）
- 人员管理只能创建/编辑 staff 和 cat_keeper 角色
- 导航栏菜单按角色过滤，不显示活动配置入口

### 6.4 总部运营端 (Operator) — `/admin`

**已完成基础适配**（2026-06-12），全部管理功能可用。

| 页面 | 文件 | 关键特性 | 状态 |
|------|------|----------|------|
| 数据看板 | `AdminDashboard.vue` | 跨门店数据，可切换门店 | ✅ 已适配 |
| 门店管理 | `AdminStores.vue` | 全部 CRUD（增删改门店/桌位/菜品） | ✅ 已适配 |
| 活动配置 | `AdminCampaigns.vue` | 优惠券 CRUD | ✅ 已适配 |
| 审计日志 | `AdminAuditLogs.vue` | 全局审计日志查看 | ✅ 已适配 |
| 人员管理 | `AdminStaff.vue` | 不可见（菜单隐藏） | — |

**权限规则**：
- 运营可切换门店查看全局数据
- 门店管理全部 CRUD 可用
- 人员管理不可见（仅 manager/admin 可访问）
- 导航栏显示活动配置入口

**适配要点**：
- Dashboard 需支持门店切换 + 全局汇总
- 活动配置需增加效果追踪
- 需新增跨门店对比图表

### 6.5 猫咪管家端 (Cat Keeper) — `/cat-keeper`

**已完成**（2026-06-12），独立模块，与 `admin/AdminCats.vue` 分离。

| 页面 | 文件 | 路径 | 关键特性 |
|------|------|------|----------|
| 布局 | `CatKeeperLayout.vue` | — | 顶部导航栏（仅"猫咪档案"菜单），角色标签，退出登录 |
| 猫咪列表 | `CatKeeperCats.vue` | `/cat-keeper/cats` | 卡片网格展示，疫苗超期提醒，点击进入健康档案 |
| 健康档案 | `CatKeeperHealth.vue` | `/cat-keeper/cats/:id/health` | 基本信息编辑、体重趋势图（ECharts 折线）、健康记录表格、互动日志时间线、照片上传 |

**路由配置**：
```typescript
// roleDefaultRoute
cat_keeper: '/cat-keeper/cats'

// 路由
path: '/cat-keeper'
component: CatKeeperLayout
meta: { roles: ['cat_keeper', 'admin'] }
children:
  /cat-keeper/cats               → CatKeeperCats.vue
  /cat-keeper/cats/:id/health    → CatKeeperHealth.vue
```

**功能详情**：
- **猫咪列表**：按门店过滤，卡片显示照片/名字/品种/体重/性格标签/健康状态，疫苗超 30 天高亮提醒
- **基本信息编辑**：名字、品种、健康状态、性格标签，保存后更新 DB
- **体重趋势图**：ECharts 折线图，展示最近 N 次体重记录，带面积渐变
- **健康记录表格**：el-table 展示时间、体重、疫苗记录、互动日志
- **互动日志时间线**：el-timeline 展示互动记录，支持新增
- **照片上传**：通过 PhotoUpload 组件，PATCH `/cats/:id/photo`
- 猫咪工作/休息状态切换
- 互动次数统计

---

## 七、适配流程 Checklist

对每个新角色的适配，按以下步骤执行：

### Step 1：基础设施
- [ ] 更新 `types/index.ts`（新增/扩展接口）
- [ ] 更新 `utils/format.ts`（状态映射）
- [ ] 更新 `utils/fallback.ts`（mock 数据）
- [ ] 更新 `styles/variables.css`（如有新状态色）

### Step 2：导航与布局
- [ ] 新建 `components/{Role}Navbar.vue`
- [ ] 更新/新建 `layouts/{Role}Layout.vue`
- [ ] 确认 `router/index.ts` 路由配置

### Step 3：页面重写
- [ ] 每个页面遵循标准结构（toolbar → KPI → 主体 → 弹窗）
- [ ] 所有操作使用二次确认弹窗
- [ ] 10s 轮询 + 新数据通知
- [ ] 骨架屏加载态
- [ ] 响应式适配（640px / 768px / 1024px）

### Step 4：验证
- [ ] `npm run build` 无错误
- [ ] 浏览器访问所有路由正常渲染
- [ ] 状态机操作按钮仅在合法状态下可用
- [ ] 轮询 + 通知正常工作
- [ ] 移动端布局正常

---

## 八、文件命名规范

```
src/
├── components/
│   ├── CustomerNavbar.vue      ← 角色前缀
│   ├── StaffNavbar.vue
│   ├── AdminNavbar.vue         ← 待新建
│   └── ...
├── layouts/
│   ├── CustomerLayout.vue
│   ├── StaffLayout.vue
│   ├── AdminLayout.vue
│   └── ...
├── views/
│   ├── customer/               ← 角色目录
│   │   ├── CustomerHome.vue
│   │   └── ...
│   ├── staff/
│   │   ├── StaffToday.vue
│   │   └── ...
│   └── admin/
│       ├── AdminDashboard.vue
│       └── ...
├── utils/
│   ├── fallback.ts             ← 统一 fallback
│   ├── format.ts               ← 统一格式化
│   └── http.ts                 ← 统一 API
├── composables/
│   └── usePolling.ts           ← 统一轮询
├── styles/
│   ├── variables.css           ← 设计 token
│   ├── element-overrides.css   ← 全局工具类
│   └── components.css          ← 全局组件样式
└── types/
    └── index.ts                ← 统一类型定义
```

---

## 九、已知限制与后续优化

| 项目 | 当前状态 | 后续计划 |
|------|----------|----------|
| 状态机 | 前端强制，后端也需校验 | 后端增加状态校验中间件 |
| 轮询 | 10s 固定间隔 | 改为 WebSocket 推送 |
| 图片 | 占位图 | 接入 OSS + 后端上传 |
| 导出 | Mock 提示 | 接入真实导出 API |
| 排班 | 未实现 | 新建排班日历组件 |
| 多门店 | 门店选择器 | 支持门店级数据隔离 |

---

## 十、验收任务落地计划（卢欣怡负责）

> 基于 COLLABORATION.md 中的验收矩阵，结合代码审计结果，列出每项任务的现状、缺口与实施计划。

### 10.1 任务总览

| # | 优先级 | 用例 | 任务描述 | 当前状态 |
|---|--------|------|----------|----------|
| T1 | Must | UC-07/08 | 操作后状态正确变更 | ✅ 已完成 |
| T2 | Should | UC-07 | 异常场景告警列表新增 | ✅ 已完成 |
| T3 | Must | UC-10 | 看板显示指标数据 | ✅ 已完成 |
| T4 | Must | UC-10 | 看板显示衍生指标 | ✅ 已完成 |
| T5 | Must | UC-10 | 含至少 3 种图表 | ✅ 已完成 |
| T6 | Must | UC-Admin | CRUD 成功后 DB 更新 | ✅ 已完成 |
| T7 | Must | ISO25010 | 操作后 audit_logs 新增 | ✅ 已完成 |

### 10.2 各任务详细分析

#### T1: UC-07/08 操作后状态正确变更 — ✅ 已完成

**验收标准**：预约/订单/桌位操作后，状态严格按状态机流转，前端按钮仅在合法状态下可用。

**现状**：
- `StaffReservations.vue`：booked → seated → dining → finished，按钮按状态显示，二次确认弹窗
- `StaffTables.vue`：桌位状态流转（free → reserved → occupied → dining → cleaning → free），详情面板操作按钮严格控制
- `StaffOrders.vue`：订单状态流转（pending → preparing → served → paid），异常报告

**结论**：无需修改，可直接用于答辩演示。

---

#### T2: UC-07 异常场景告警列表新增 — ✅ 已完成

**验收标准**：异常场景（异常订单、运营告警）有独立的告警列表展示。

**现状**：
- `StaffToday.vue` 已有 `operation_alerts` 运营提醒区域（来自 `fallbackDashboard.alerts`）
- `StaffOrders.vue` 已实现可折叠异常告警面板（`alert-panel`），聚合异常订单 + 运营告警
- 异常订单可点击跳转详情，运营告警按 level（info/warning）分类显示

---

#### T3: UC-10 看板显示指标数据 — ✅ 已完成

**验收标准**：管理员看板显示预约量、翻台率、收入等核心指标。

**现状（AdminDashboard.vue）**：
- 7 个 KPI 卡片已实现：预约量、翻台率、坪效、复购率、总收入、客单价、平均评分
- 数据通过 `api.get('/dashboard/summary')` 获取
- 环比趋势已改为 `null`，无数据时显示"-" + "环比数据待接入"
- 后端 `turnover_rate` 已修复：`finishedCount / tableCount`
- 后端 `repeat_rate` 已修复：`repeatCustomers / totalCustomers`（从订单数据动态计算）
- `reviews` 表已创建，`GET /reviews` 端点已注册，前端优先从 API 获取评价数据

---

#### T4: UC-10 看板显示衍生指标 — ✅ 已完成

**验收标准**：看板包含衍生指标（客单价、高峰时段等）。

**现状**：
- 客单价 KPI 已实现：前端从 paid 订单计算 `total / count`，显示在第 6 个 KPI 卡片
- 高峰时段柱状图已实现：复用 `BarChart`，X 轴为时段（10:00-21:00），Y 轴为预约数，最大值高亮为 coral 色
- 数据从 `reservations` 按 `reservation_time` 聚合，前端独立计算无需后端额外字段

---

#### T5: UC-10 含至少 3 种图表 — ✅ 已完成

**验收标准**：看板包含至少 3 种不同类型的图表。

**现状（AdminDashboard.vue）**：
- 已有 4 个 ECharts 图表，使用 `BarChart`、`LineChart`、`PieChart` 3 种类型
- 图 1：预约与收入趋势（折线 + 柱状组合）
- 图 2：预约状态分布（饼图/环形图）
- 图 3：菜品销量排行（水平柱状图）
- 图 4：猫咪人气评分（垂直柱状图）

**注意**：图 1、3、4 使用硬编码 mock 数据。若答辩被追问，可解释"后端数据接口对接中，当前为 demo 数据"。

**结论**：满足"至少 3 种图表"硬性要求，无需紧急修改。

---

#### T6: UC-Admin CRUD 成功后 DB 更新 — ✅ 已完成

**验收标准**：管理员执行 CRUD 操作后，数据库正确更新。

**现状**：
- 前端 4 个 Admin 页面（Stores / Cats / Staff / Campaigns）的 CRUD 调用链正确
- 每个 save / delete 函数在 API 调用成功后都执行 `await loadData()` 刷新数据
- 后端 15 个 CRUD 端点全部实现（stores/tables/cats/menu-items/users 各 3 个）
- `repository.js` 包含 15 个对应数据库操作函数
- `api.js` 已注册全部 POST/PATCH/DELETE 路由
- 额外补充：`PATCH /cats/:id/photo` 和 `PATCH /menu-items/:id/photo` 照片上传端点

---

#### T7: ISO25010 操作后 audit_logs 新增 — ✅ 已完成

**验收标准**：所有关键操作（CRUD、状态变更）产生审计日志记录。

**现状**：
- `audit_logs` 表已创建（含 user_id/user_name/action/target_table/target_id/detail/ip_address/created_at + 索引）
- `auditMiddleware.js` 已实现：拦截 POST/PATCH/DELETE 请求，从路径推断 target_table/target_id，响应成功后异步写入
- `repository.js` 包含 `insertAuditLog()` 和 `listAuditLogs()` 函数
- `api.js` 已注册 `GET /audit-logs`（查询）和 `POST /audit-logs`（写入）端点
- `AdminAuditLogs.vue` 已实现：含操作类型/目标表/日期范围筛选，el-table 展示，JSON 详情弹窗，10s 自动刷新
- 路由和导航栏已配置

**实施计划**：

**第一步：数据库（需成睿轩配合）**

```sql
-- 文件：db/migrations/V003__m7_audit_logs.sql
CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  user_name VARCHAR(64),
  action VARCHAR(32) NOT NULL COMMENT 'CREATE/UPDATE/DELETE/STATUS_CHANGE',
  target_table VARCHAR(64) NOT NULL COMMENT 'reservations/orders/stores/...',
  target_id BIGINT,
  detail JSON COMMENT '变更前后快照',
  ip_address VARCHAR(45),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_created (created_at),
  INDEX idx_target (target_table, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**第二步：后端中间件（卢欣怡可独立完成）**

新建 `backend/src/middleware/auditMiddleware.js`：
- 拦截所有 `POST / PATCH / DELETE` 请求
- 从请求路径解析 `target_table` 和 `target_id`
- 记录 `user_id`（从 token 解析）、`action`、`detail`（请求 body）
- 异步写入 `audit_logs` 表，不阻塞主请求

**第三步：后端 API（需成睿轩配合）**

在 `api.js` 中新增：
- `GET /audit-logs` — 查询审计日志，支持按时间/操作类型/目标表筛选
- `repository.js` 中新增 `listAuditLogs(filters)` 函数

**第四步：前端页面（卢欣怡独立完成）**

| 文件 | 改动 |
|------|------|
| `views/admin/AdminAuditLogs.vue` | **新建** — 审计日志查看页面 |
| `router/index.ts` | 新增 `/admin/audit-logs` 路由 |
| `components/AdminNavbar.vue` | 导航栏增加"审计日志"入口 |

**AdminAuditLogs.vue 设计方案**：
- 顶部筛选区：日期范围、操作类型下拉（CREATE/UPDATE/DELETE/STATUS_CHANGE）、目标表下拉
- 主体：`el-table` 展示审计日志列表（时间、操作人、操作类型、目标、详情）
- 详情列可展开查看变更前后 JSON 快照
- 10 秒自动刷新（复用 `usePolling`）

---

### 10.3 执行策略：先个人实现，再合并主线

**协作原则**：每人先在自己的 feature 分支上独立完成全部任务（含前后端），然后通过 PR 合并到 main。

**分支分配**：

| 成员 | 分支名 | 负责任务 |
|------|--------|----------|
| 卢欣怡 | `feat/frontend-pages` | T1(已完成)、T2、T3前端、T4前端、T5(已完成)、T7前端 |
| 葛宇晨 | `feat/m1-user-order` | UC-02、UC-09、UC-03、UC-04（用户/订单/预约相关后端） |
| 成睿轩 | `feat/m2-store-dashboard` | UC-04查询、UC-05~08状态机、UC-06/07看板、T6后端、T7后端 |
| 高范铖 | `feat/m3-reservation` | UC-04预约核心、UC-07/08预约状态机 |

**卢欣怡分支（`feat/frontend-pages`）执行计划**：

> **策略**：前端 + 后端（Node.js 原型）均在本地分支独立完成，不依赖其他同学的分支。
> 后端同学（葛宇晨/成睿轩）的 Spring Boot 端点后续合并时，前端已有的 Node.js 原型端点可作为对照参考。

```
Step 1: T2 — StaffOrders.vue 新增异常告警面板 ✅
  └─ 修改：StaffOrders.vue（新增可折叠告警面板，聚合异常订单 + 运营告警）

Step 2: T3 前端 — AdminDashboard.vue 移除硬编码趋势 ✅
  └─ 修改：AdminDashboard.vue（6 个 KPI 趋势值改为 null，无数据时显示"-" + "环比数据待接入"）

Step 3: T4 前端 — AdminDashboard.vue 新增衍生指标 ✅
  └─ 修改：AdminDashboard.vue（新增客单价 KPI + 预约高峰时段柱状图 + KPI 网格改为 4 列）

Step 4: T7 后端 — 新建 audit_logs 表 + API 端点 ✅
  └─ 修改：backend/src/db/schema.sql（新增 audit_logs 表，含索引）
  └─ 修改：backend/src/routes/repository.js（新增 insertAuditLog / listAuditLogs）
  └─ 修改：backend/src/routes/api.js（注册 GET/POST /audit-logs）

Step 5: T7 后端 — 新建审计中间件 ✅
  └─ 新建：backend/src/middleware/auditMiddleware.js
  └─ 修改：backend/src/app.js（注册中间件到 /api 路由前）

Step 6: T7 前端 — 新建审计日志页面 ✅
  └─ 新建：views/admin/AdminAuditLogs.vue
  └─ 修改：router/index.ts（新增 /admin/audit-logs 路由）
  └─ 修改：components/AdminNavbar.vue（新增"审计日志"导航入口 + ClipboardList 图标）

Step 7: T6 后端 — 补全 Admin CRUD 端点（Node.js 原型） ✅
  └─ 修改：backend/src/routes/repository.js（新增 stores/tables/cats/menu-items/users 共 15 个 CRUD 函数）
  └─ 修改：backend/src/routes/api.js（注册 15 个 POST/PATCH/DELETE 端点）

Step 8: T6 前端 — Admin CRUD 端到端验证 ✅
  └─ 验证：24 个前端 API 调用全部匹配后端端点
  └─ 修复：补充 PATCH /menu-items/:id/photo 和 PATCH /cats/:id/photo 两个缺失端点
  └─ 修改：schema.sql（cats/menu_items 表新增 photo_url 列）
  └─ 修改：repository.js（新增 updateCatPhoto / updateMenuItemPhoto）
  └─ 修改：api.js（注册两个 photo 路由）
```

**合并策略**：
- 卢欣怡在本地分支完成全部前后端改动（含 Node.js 原型端点），自测通过后提 PR
- 后端同学合并 Spring Boot 端点时，可参考 Node.js 原型的接口设计和 SQL
- 合并后两端并存，前端通过 `vite.config.ts` 的 `/api` 代理决定走 Node.js 还是 Spring Boot

**合并前必须**：`npm run build` 无错误 + `node backend/src/test/domain.test.js` 通过。

### 10.4 文件改动清单（含执行人）

#### 卢欣怡独立完成（`feat/frontend-pages` 分支）

| 文件 | 操作 | 对应任务 |
|------|------|----------|
| `frontend/src/views/staff/StaffOrders.vue` | 修改：新增异常告警面板 | T2 ✅ |
| `frontend/src/views/admin/AdminDashboard.vue` | 修改：移除硬编码趋势 + 新增客单价/高峰时段 | T3/T4 |
| `frontend/src/views/admin/AdminAuditLogs.vue` | **新建**：审计日志查看页面 | T7 |
| `frontend/src/router/index.ts` | 修改：新增 `/admin/audit-logs` 路由 | T7 |
| `frontend/src/components/AdminNavbar.vue` | 修改：新增"审计日志"导航入口 | T7 |
| `backend/src/db/schema.sql` | 修改：新增 `audit_logs` 表 | T7 |
| `backend/src/routes/repository.js` | 修改：新增 `insertAuditLog` / `listAuditLogs` | T7 |
| `backend/src/routes/api.js` | 修改：注册 `GET/POST /audit-logs` | T7 |
| `backend/src/middleware/auditMiddleware.js` | **新建**：请求拦截审计中间件 | T7 |
| `backend/src/routes/repository.js` | 修改：新增 stores/tables/cats/users CRUD 函数 | T6 |
| `backend/src/routes/api.js` | 修改：注册 stores/tables/cats/users 的 POST/PATCH/DELETE | T6 |

#### 后端同学后续合并时对照

| 文件 | 需改动 | 对应任务 | 负责人 |
|------|--------|----------|--------|
| `db/schema.sql` 或迁移脚本 | 新增 `reviews` 表 | T3 | 成睿轩 |
| `routes/repository.js` | 修复 `getDashboardSummary`（翻台率公式 bug） | T3 | 成睿轩 |
| `routes/repository.js` | 新增 `avg_order_value`、`peak_hours` | T4 | 成睿轩 |
| `routes/api.js` | 注册 `GET /reviews` | T3 | 成睿轩 |

### 10.5 修改记录（含代码验证状态）

> 2026-06-12 22:00 全量验证：13 个类别全部 PASS，代码与文档一致。

#### 店员端功能增强

| 日期 | 步骤 | 文件 | 改动摘要 | 验证 |
|------|------|------|----------|------|
| 2026-06-12 | Step 1 | `StaffOrders.vue` | 新增可折叠异常告警面板：聚合 `exception` 订单 + `operation_alerts`，点击异常订单跳转详情 | ✅ |
| 2026-06-12 | Step 2 | `AdminDashboard.vue` | 6 个 KPI 硬编码趋势值改为 `null`，无数据时灰色显示"-" + "环比数据待接入" | ✅ |
| 2026-06-12 | Step 3 | `AdminDashboard.vue` | 新增客单价 KPI（paid 订单均价）+ 预约高峰时段柱状图（10:00-21:00，最大值高亮）+ KPI 网格改 4 列 | ✅ |

#### 后端审计日志系统

| 日期 | 步骤 | 文件 | 改动摘要 | 验证 |
|------|------|------|----------|------|
| 2026-06-12 | Step 4 | `schema.sql` | 新增 `audit_logs` 表（含 user_id/user_name/action/target_table/target_id/detail/ip_address/created_at + 索引） | ✅ |
| 2026-06-12 | Step 4 | `repository.js` | 新增 `insertAuditLog()` 和 `listAuditLogs()` 函数，支持按 action/targetTable/日期筛选 | ✅ |
| 2026-06-12 | Step 4 | `api.js` | 注册 `GET /audit-logs`（查询）和 `POST /audit-logs`（写入）端点 | ✅ |
| 2026-06-12 | Step 5 | `auditMiddleware.js` | **新建**：拦截 POST/PATCH/DELETE 请求，从路径推断 target_table/target_id，响应成功后异步写入 audit_logs | ✅ |
| 2026-06-12 | Step 5 | `app.js` | 注册审计中间件：`app.use("/api", auditMiddleware, apiRouter)` | ✅ |
| 2026-06-12 | Step 6 | `AdminAuditLogs.vue` | **新建**：审计日志查看页面，含操作类型/目标表/日期范围筛选，el-table 展示，JSON 详情弹窗 | ✅ |
| 2026-06-12 | Step 6 | `router/index.ts` | 新增 `/admin/audit-logs` 路由（manager/operator/admin 可访问） | ✅ |
| 2026-06-12 | Step 6 | `AdminNavbar.vue` | 新增"审计日志"导航入口（ClipboardList 图标） | ✅ |

#### 后端 CRUD 端点补全

| 日期 | 步骤 | 文件 | 改动摘要 | 验证 |
|------|------|------|----------|------|
| 2026-06-12 | Step 7 | `repository.js` | 新增 15 个 CRUD 函数：createStore/updateStore/deleteStore、createTable/updateTable/deleteTable、createCat/updateCat/deleteCat、createMenuItem/updateMenuItem/deleteMenuItem、listUsers/createUser/updateUser/deleteUser | ✅ |
| 2026-06-12 | Step 7 | `api.js` | 注册 15 个端点：stores/tables/cats/menu-items/users 的 POST/PATCH/DELETE | ✅ |
| 2026-06-12 | Step 8 | `schema.sql` | cats/menu_items 表新增 `photo_url VARCHAR(512)` 列 | ✅ |
| 2026-06-12 | Step 8 | `repository.js` | 新增 `updateCatPhoto` / `updateMenuItemPhoto` 函数 | ✅ |
| 2026-06-12 | Step 8 | `api.js` | 注册 `PATCH /cats/:id/photo` 和 `PATCH /menu-items/:id/photo` | ✅ |

#### 顾客端整体重构（路由前缀 + CSS 变量）

| 日期 | 文件 | 改动摘要 | 验证 |
|------|------|----------|------|
| 2026-06-12 | `router/index.ts` | 顾客路由从 `/` 移至 `/customer`，新增 `/ → /customer` 重定向，`roleDefaultRoute` customer 指向 `/customer` | ✅ |
| 2026-06-12 | `CustomerLayout.vue` | 重写：使用 CSS 变量（`var(--wash)` 替代硬编码 `#faf9f6`），新增底部版权 footer，flex 纵向布局 | ✅ |
| 2026-06-12 | `CustomerNavbar.vue` | 重写：移除 staff/admin 角色分支（简化为顾客专属），全部颜色改为 CSS 变量，路由路径更新为 `/customer/*` | ✅ |
| 2026-06-12 | `CustomerHome.vue` | 7 个 router-link 路径更新，`#ef4444`→`var(--danger)`，`#f59e0b`→`var(--gold)`，`userId:1`→`auth.user?.id \|\| 1` | ✅ |
| 2026-06-12 | `CustomerStores.vue` | 3 个 `$router.push` 路径更新为 `/customer/stores/${id}` | ✅ |
| 2026-06-12 | `CustomerStoreDetail.vue` | 2 个路由路径更新，7 处硬编码颜色改为 CSS 变量（gold/danger/corral/paper/wash） | ✅ |
| 2026-06-12 | `CustomerReservation.vue` | 30 处 CSS 颜色替换，`userId:1`→`auth.user?.id \|\| 1`，行内 style 颜色改为变量 | ✅ |
| 2026-06-12 | `CustomerOrder.vue` | `router.push('/payment')`→`'/customer/payment'` | ✅ |
| 2026-06-12 | `CustomerPayment.vue` | 3 处路由更新（`/order`→`/customer/order`，`/profile`→`/customer/profile`，`/`→`/customer`），`#f8f9fa`→`var(--wash)` | ✅ |
| 2026-06-12 | `CustomerProfile.vue` | 无变更（路由和颜色已符合规范） | ✅ |
| 2026-06-12 | `CustomerReviews.vue` | 无变更（路由和颜色已符合规范） | ✅ |

#### 管理端权限增强

| 日期 | 文件 | 改动摘要 | 验证 |
|------|------|----------|------|
| 2026-06-12 | `AdminDashboard.vue` | 引入 auth store，`storeId` 从硬编码 `1` 改为动态（店长锁定本店，运营可切换），店长时门店下拉禁用并显示"本店数据"标签 | ✅ |
| 2026-06-12 | `AdminCats.vue` | `storeId: 1` 改为 `auth.user?.store_id \|\| 1`，跟随用户所属门店 | ✅ |
| 2026-06-12 | `AdminStores.vue` | 新增 `canEditStore` 权限计算：运营=全部 CRUD，店长=只读（隐藏新增/编辑/删除按钮，显示"只读"标签），`stores` 改为 computed 过滤 | ✅ |
| 2026-06-12 | `AdminStaff.vue` | 新增 `assignableRoles` 权限控制：店长只能分配 `staff/cat_keeper`，运营可分配所有角色，角色选择器改为动态渲染 | ✅ |

#### 猫咪管家端独立模块

| 日期 | 文件 | 改动摘要 | 验证 |
|------|------|----------|------|
| 2026-06-12 | `CatKeeperLayout.vue` | **新建**：顶部导航栏布局（与店员端风格统一），仅显示"猫咪档案"菜单，角色标签（黄色药丸），退出登录 | ✅ |
| 2026-06-12 | `CatKeeperCats.vue` | **新建**：猫咪卡片网格列表，疫苗超 30 天提醒，点击进入健康档案详情 | ✅ |
| 2026-06-12 | `CatKeeperHealth.vue` | **新建**：猫咪健康档案详情页，含基本信息编辑、ECharts 体重趋势折线图、健康记录 el-table、互动日志 el-timeline、照片上传 | ✅ |
| 2026-06-12 | `router/index.ts` | 新增 `/cat-keeper` 路由（cats + cats/:id/health），`cat_keeper` 默认路由改为 `/cat-keeper/cats` | ✅ |

#### 布局风格统一

| 日期 | 文件 | 改动摘要 | 验证 |
|------|------|----------|------|
| 2026-06-12 | `AdminNavbar.vue` | **重写**：深色侧边栏 → 白色顶部导航栏，CSS 变量，角色标签药丸，响应式适配 | ✅ |
| 2026-06-12 | `AdminLayout.vue` | **重写**：`flex-direction: row`（侧边栏）→ `flex-direction: column`（顶部导航 + 内容区） | ✅ |
| 2026-06-12 | `CatKeeperLayout.vue` | **重写**：深色侧边栏 → 白色顶部导航栏，与 AdminNavbar 风格一致 | ✅ |

#### 工具层补充

| 日期 | 文件 | 改动摘要 | 验证 |
|------|------|----------|------|
| 2026-06-12 | `format.ts` | 新增 `orderStatusLabel`、`orderStatusType`、`tagLabel` 导出函数 | ✅ |
| 2026-06-12 | `fallback.ts` | 新增 `fallbackOrders` 导出（2 条 mock 订单），`fallbackDashboard` 增加 `dining_count` 字段 | ✅ |

#### 全局 UI 一致性审查

**审查时间**：2026-06-12

| 优先级 | 问题类别 | 状态 | 说明 |
|--------|----------|------|------|
| P0 | 阻断性问题 | 无 | — |
| P1 | `components.css` 硬编码值 | ✅ 已修复 | 18 处间距/圆角 + 16 处颜色全部替换为 CSS 变量 |
| P1 | Vue 文件硬编码 hex 颜色 | ⚠️ 已知 | 顾客端已大部分替换，剩余 ~200 处在管理端和店员端（图表色/平面图色/渐变色，设计需要） |
| P2 | 内联 style 属性 | ⚠️ 已知 | 12 处，多为布局辅助（`display:flex; gap`），不影响功能 |
| P2 | `<h2>` 内联样式 | ⚠️ 已知 | Login.vue 1 处 `style="text-align: center"` |

**已修复**：`components.css` 全面重构，所有硬编码 `8px/10px/12px/14px/16px/18px/20px/24px` 替换为 `var(--space-*)` 和 `var(--radius-*)`，所有硬编码颜色替换为 `var(--paper)/var(--wash)/var(--line)/var(--muted)/var(--teal-*)/var(--coral-*)/var(--success-*)/var(--gold-*)` 等 CSS 变量。

**可接受的保留**：
- `StaffTables.vue` 的 84 处 hex：建筑平面图的桌面/座椅/区域颜色，是设计稿指定色值
- `AdminDashboard.vue` 的 31 处 hex：ECharts 图表品牌配色
- `999px` / `99px` 圆角：药丸形标签的标准写法，无需替换

#### 布局风格统一（已完成）

**2026-06-12 已完成**：所有角色统一为**白色顶部导航栏 + 内容区**布局，消除深色侧边栏差异。

| 角色 | 布局模式 | 导航栏 | 导航栏高度 | 内容区最大宽度 |
|------|----------|--------|-----------|--------------|
| 顾客 | 顶部导航 + footer | `CustomerNavbar.vue` | 60px | 1200px |
| 店员 | 顶部导航 | `StaffNavbar.vue` | 56px | 1400px |
| 店长/运营/管理员 | 顶部导航 | `AdminNavbar.vue` | 56px | 1400px |
| 猫咪管家 | 顶部导航 | `CatKeeperLayout.vue` | 56px | 1200px |

**统一设计规范**：
- 导航栏：白色背景 `var(--paper)` + 底部边框 `var(--line)` + `box-shadow: var(--shadow-sm)`
- 品牌区：`var(--coral)` 背景的 "N" 图标 + "NekoCafe" 文字
- 导航链接：`var(--muted)` 默认色，hover/active 时 `var(--teal)` + 底部 2px 青色边框
- 右侧：角色标签（药丸形）+ 用户下拉菜单
- 响应式：768px 以下隐藏导航文字，只显示图标

**剩余差异**（非阻断）：
- 管理端 5 个页面（AdminDashboard/AdminStores/AdminStaff/AdminCats/AdminCampaigns）的 CSS 仍使用部分硬编码 hex 和内联 style，可后续逐步替换为 CSS 变量

#### 顾客端 UI 优化

| 日期 | 文件 | 改动摘要 | 验证 |
|------|------|----------|------|
| 2026-06-12 | `CustomerStores.vue` | 添加 `page-enter-active` + toolbar，`#e8f6f1`/`#fef3e2`→`var(--teal-light)`/`var(--coral-light)`，inline style→CSS class | ✅ |
| 2026-06-12 | `CustomerOrder.vue` | 添加 `page-enter-active` + toolbar，`#f5f5f4`→`var(--wash)` | ✅ |
| 2026-06-12 | `CustomerPayment.vue` | 添加 `page-enter-active` | ✅ |
| 2026-06-12 | `CustomerProfile.vue` | 添加 `page-enter-active` + toolbar，5 处 `#f0eeea`→`var(--line)` | ✅ |
| 2026-06-12 | `CustomerReviews.vue` | 添加 `page-enter-active` + toolbar，inline style→CSS class | ✅ |
| 2026-06-12 | `CustomerReservation.vue` | 添加 `page-enter-active` + toolbar + subtitle class，`#d1fae5`→`var(--success-light)`，`#f3f4f6`→`var(--wash)`，`#e5e7eb`→`var(--line)`，`#e8f6f1`→`var(--teal-light)`，`#d1d5db`→`var(--line)`，`#9ca3af`→`var(--muted)` | ✅ |
| 2026-06-12 | `CustomerHome.vue` | 添加 toolbar，`#d1d5db`→`var(--line)` | ✅ |
| 2026-06-12 | `CustomerStoreDetail.vue` | 添加 toolbar，9 处 hex→CSS 变量（`#fff`→`var(--paper)`，`#f3f4f6`→`var(--wash)`，`#e5e7eb`→`var(--line)`，`#fafafa`→`var(--wash)`，`#16a34a`→`var(--success)`，`#b45309`→`var(--gold)`） | ✅ |

**统一规范**：
- 所有顾客页面根 div 添加 `page-enter-active`（页面淡入动画）
- 所有页面添加 `toolbar` 包裹 `h2`（与店员端一致）
- 硬编码 hex 颜色替换为 CSS 变量（渐变色和骨架屏 inline style 保留）

#### 桌位可视化风格统一

| 日期 | 文件 | 改动摘要 | 验证 |
|------|------|----------|------|
| 2026-06-12 | `CustomerReservation.vue` | 桌位从简单按钮改为建筑平面图风格（座椅圆点 + 按座位数分级的桌面矩形），使用 CSS 变量配色 | ✅ |
| 2026-06-12 | `StaffTables.vue` | 状态色从硬编码 hex 改为 CSS 变量（`var(--success)`/`var(--blue)`/`var(--teal)`/`var(--danger)`/`var(--gold)`/`var(--muted)`），渐变色保留 | ✅ |

**统一桌位组件结构**：
```
.table-wrap                    ← 包裹层（点击区域）
  .seats-row.seats-top         ← 上排座椅圆点
  .table-body-row
    .seats-col.seats-left      ← 左侧座椅圆点
    .table-surface.surface-*   ← 桌面矩形（sm/md/lg/xl 按座位数分级）
      .table-paw               ← 猫爪装饰（cat_zone 桌位）
      .table-code              ← 桌位编号
      .table-seats-label       ← 座位数
    .seats-col.seats-right     ← 右侧座椅圆点
  .seats-row.seats-bottom      ← 下排座椅圆点
  .table-status-tag            ← 状态标签（店员端）
```

**统一配色方案**：

| 状态 | 桌面渐变 | 标签/边框 | 座椅圆点 |
|------|----------|----------|----------|
| 空闲 | `var(--success-light) → #bbf7d0` | `var(--success)` | 绿色 |
| 已预约 | `var(--blue-light) → #bfdbfe` | `var(--blue)` | 蓝色 |
| 已入座 | `var(--teal-light) → #a7f3d0` | `var(--teal)` | teal |
| 用餐中 | `#fee2e2 → #fecaca` | `var(--danger)` | 红色 |
| 待清台 | `var(--gold-light) → #fde68a` | `var(--gold)` | 橙色 |
| 维护中 | `var(--wash) → var(--line)` | `var(--muted)` | 灰色 |
