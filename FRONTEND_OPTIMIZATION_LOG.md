# 前端优化工作记录

## 优化周期

2026-06-05，基于 Impeccable 设计准则（7 大维度）+ T-01 任务书功能完整性检查。

---

## 一、设计系统建立（基础层）

### 1.1 字体优化（/typeset）

| 文件 | 修改 |
|------|------|
| `frontend/index.html` | 添加 Google Fonts 加载 Nunito（400/600/700） |
| `frontend/src/styles/variables.css` | `Inter` → `Nunito` |
| `frontend/src/styles/element-overrides.css` | `Inter` → `Nunito` |

**原因**：Inter 是 AI 生成 UI 中最常见的字体，缺乏辨识度。Nunito 圆润友好，匹配猫咪咖啡厅调性。

### 1.2 间距系统（/layout）

**文件**：`frontend/src/styles/variables.css`

新增 4pt base 间距 token：

```css
--space-xs: 4px;
--space-sm: 8px;
--space-md: 12px;
--space-base: 16px;
--space-lg: 24px;
--space-xl: 32px;
--space-2xl: 48px;
```

**原因**：原有代码到处散落任意值（18px、14px、20px），无统一节奏。

### 1.3 阴影/圆角层级

**文件**：`frontend/src/styles/variables.css`

```css
--shadow-sm: 0 1px 3px rgba(23,32,51,0.06);
--shadow-md: 0 4px 12px rgba(23,32,51,0.08);
--shadow-lg: 0 16px 48px rgba(23,32,51,0.11);

--radius-sm: 6px;
--radius-md: 10px;
--radius-lg: 14px;
```

**原因**：原只有 1 个 `--shadow`（过重），卡片各用各的 shadow 值。

### 1.4 字体层级

**文件**：`frontend/src/styles/variables.css`

```css
--text-xs: 12px;  --text-sm: 13px;  --text-base: 15px;
--text-lg: 18px;  --text-xl: 22px;  --text-2xl: 28px;  --text-3xl: 36px;
```

新增 `h1/h2/h3/h4` 全局层级定义（原来只有 h1）。

### 1.5 过渡动画 token

```css
--transition-fast: 0.15s ease;
--transition-base: 0.25s ease;
```

---

## 二、交互状态增强（/polish）

### 2.1 全局卡片交互类

**文件**：`frontend/src/styles/element-overrides.css`

```css
.card-interactive {
  transition: box-shadow var(--transition-fast), transform var(--transition-fast), border-color var(--transition-fast);
  cursor: pointer;
}
.card-interactive:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
  border-color: var(--teal-light);
}
.card-interactive:active {
  transform: translateY(0);
  box-shadow: var(--shadow-sm);
}
```

### 2.2 全局骨架屏动画

```css
@keyframes skeleton-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}
.skeleton {
  background: var(--line);
  border-radius: var(--radius-sm);
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}
```

### 2.3 按钮 active 反馈

```css
.el-button:active { transform: scale(0.97); }
```

---

## 三、标签中文化（功能修复）

### 3.1 翻译工具函数

**文件**：`frontend/src/utils/format.ts`

新增 `tagLabel(tag)` 和 `tagLabels(tags[])` 函数，内置翻译表覆盖：

- 猫咪性格：quiet→安静、gentle→温柔、active→活泼、playful→爱玩…
- 健康状态：healthy→健康、observe→观察中、rest→静养…
- 菜品分类：drink→饮品、meal→主食、dessert→甜品…
- 菜品标签：coffee→咖啡、signature→招牌、sweet→甜品、cheese→芝士…
- 桌位区域：window→窗边、main→大厅、party→包厢…

### 3.2 应用翻译的页面

| 页面 | 修改内容 |
|------|---------|
| `CustomerStoreDetail.vue` | 猫咪标签/健康状态/菜品分类/桌位区域全部翻译 |
| `CustomerHome.vue` | 推荐猫咪标签、人气菜品标签翻译 |
| `CustomerOrder.vue` | 菜品分类/标签翻译 |
| `CustomerReservation.vue` | 猫咪标签、桌位区域、菜品标签翻译 |

### 3.3 菜品图片占位符

| 页面 | 修改 |
|------|------|
| `CustomerStoreDetail.vue` | 菜品图片加 `menu-photo-placeholder`（🍽️） |
| `CustomerHome.vue` | 同上 |
| `CustomerOrder.vue` | 同上 |

### 3.4 分类去重

**文件**：`CustomerHome.vue`

修改前：`{{ tagLabels([item.category]) }} · {{ tagLabels(item.tags) }}` → "甜品 · 甜品/芝士"
修改后：`{{ tagLabels(item.tags) }}` → "甜品/芝士"

---

## 四、店员端通知增强（M5 功能补全）

### 4.1 轮询变更检测

**文件**：`frontend/src/composables/usePolling.ts`

新增 `startWithChangeDetection(getSignature)` 方法：
- 每次轮询对比数据签名（id 列表拼接）
- 检测到新增 id 时 `newCount++`
- 提供 `clearNewCount()` 重置计数

### 4.2 StaffToday.vue 改造

- 使用变更检测轮询（10 秒间隔）
- 新预约到达：**红色徽章 "新预约"** + 卡片高亮动画（2 秒渐隐）
- "待处理" 卡片用 `--coral` 强调色背景
- 响应式断点 `@media(640px)`

### 4.3 StaffOrders.vue 改造

- 同样使用变更检测轮询
- 新订单到达：**绿色徽章 "新订单"** + 卡片高亮
- 使用设计 token 替换硬编码值

### 4.4 通知延迟分析

```
顾客下单 → 后端写入 DB → 最多 10 秒 → 店员轮询发现 → 徽章+高亮
```

最大延迟 10 秒，课程演示完全够用。无需 WebSocket。

---

## 五、页面级优化

### 5.1 CustomerStores.vue

- 新增骨架屏 loading 状态
- 新增 `el-empty` 空状态
- 城市徽章（`store-badge`）
- 使用 `.card-interactive` 全局交互类
- 响应式 `@media(640px)`
- 全部替换为设计 token

### 5.2 AdminStaff.vue（API 对接）

- 移除硬编码数据
- 调用 `GET /api/users?role=staff,manager,operator,cat_keeper,admin`
- 新增角色筛选 `el-select`
- 角色图标/颜色/中文标签映射

### 5.3 AdminCampaigns.vue（API 对接）

- 移除硬编码数据
- 调用 `GET /api/coupons` 获取真实优惠券
- 新增创建优惠券弹窗（el-dialog + el-form）
- 金额显示使用 `cents()` 格式化

---

## 六、孤儿代码清理

| 操作 | 文件 | 原因 |
|------|------|------|
| 删除 | `components/ReservationForm.vue` | 未被任何页面引用，`CustomerReservation.vue` 自带内联表单 |

---

## 七、设计质量评分（Impeccable /critique）

| 维度 | 优化前 | 优化后 | 说明 |
|------|--------|--------|------|
| 间距系统 | 4/10 | 7/10 | 7 级 token 已建立，待全页面替换 |
| 配色 | 7/10 | 8/10 | 新增 light 色值，语义色完整 |
| 字体层次 | 6/10 | 8/10 | h1-h4 层级 + text-xs~3xl token |
| 交互状态 | 3/10 | 6/10 | 全局交互类+骨架屏，待更多页面应用 |
| 响应式 | 5/10 | 6/10 | CustomerStores/StaffToday 已加断点 |
| 空状态 | 4/10 | 6/10 | 关键页面已加 el-empty |
| 阴影/圆角 | 5/10 | 8/10 | 3 级阴影 + 3 级圆角 token |

**综合：6.5 → 7.5**

---

## 八、第二轮优化（2026-06-05 续）

### 8.1 阶段 1：顾客端骨架屏（P1）

| 页面 | 修改 |
|------|------|
| `CustomerHome.vue` | 推荐猫咪骨架屏 + 人气菜品 4 卡片骨架屏 + el-empty |
| `CustomerOrder.vue` | 菜品列表 5 行骨架屏 + 购物车骨架屏 + el-empty |
| `CustomerPayment.vue` | 支付卡片骨架屏 + 无订单 el-empty |
| `CustomerProfile.vue` | 预约/订单 Tab 各 3 行骨架屏 |
| `CustomerReviews.vue` | 评价列表 3 卡片骨架屏 |

全部页面同步应用设计 token（`--space-*`、`--radius-*`、`--text-*`）。

### 8.2 阶段 2：店员端+管理端响应式断点（P1）

| 页面 | 断点 | 修改 |
|------|------|------|
| `StaffToday.vue` | 640px | stat-cards 4→2 列，res-card 改纵向 |
| `StaffReservations.vue` | 640px | res-card 改纵向，按钮满宽 |
| `StaffOrders.vue` | 640px | 已有断点（上轮完成） |
| `StaffTables.vue` | 640px | table-grid 2 列，卡片缩小 |
| `AdminDashboard.vue` | 1100px/600px | 已有断点（原有） |
| `AdminStores.vue` | 640px | menu-grid 改单列，卡片改纵向 |
| `AdminCats.vue` | 640px | cat-grid 2 列，record-item 改纵向 |
| `AdminStaff.vue` | 640px | staff-grid 改单列 |
| `AdminCampaigns.vue` | 640px | campaign-meta 改纵向 |

### 8.3 阶段 4：布局差异化（P2）

**CustomerHome.vue** — 人气菜品"一大两小"布局：
- 第一个菜品卡片横跨 2 行（`grid-row: 1/3`），图片高度 200px，字号放大
- 其余 3 个菜品卡片为普通小卡片
- 3 列 Grid 布局

**CustomerStores.vue** — 门店交替换行布局：
- 全宽卡片替代原 Grid 网格
- 奇数行：左图右文（`store-card--left`）
- 偶数行：右图左文（`store-card--right`）
- 图标区域使用渐变背景（teal/coral 交替）
- 移动端 640px 回归纵向居中

---

## 九、更新后设计质量评分

| 维度 | 第一轮后 | 第二轮后 | 说明 |
|------|----------|----------|------|
| 间距系统 | 7/10 | 8/10 | 全部顾客端+管理端页面已应用 token |
| 配色 | 8/10 | 8/10 | 无变化 |
| 字体层次 | 8/10 | 8/10 | 无变化 |
| 交互状态 | 6/10 | 7/10 | 全部顾客端页面有骨架屏 |
| 响应式 | 6/10 | 8/10 | 9 个页面新增断点 |
| 空状态 | 6/10 | 8/10 | 全部顾客端+管理端有 el-empty |
| 阴影/圆角 | 8/10 | 8/10 | 无变化 |
| 布局多样性 | 5/10 | 7/10 | CustomerHome/Stores 差异化 |

**综合：7.5 → 8.0**

---

## 十、待后续优化

| 项目 | 优先级 | 说明 |
|------|--------|------|
| 暗色模式支持 | P2 | 需要 dark mode token 层 |
| ElNotification 弹窗通知 | P2 | 当前仅徽章+高亮，可加桌面通知 |
| CustomerStoreDetail 响应式 | P2 | 猫咪/桌位/菜单三 Tab 需断点 |
| LBS 附近门店 | P2 | 选做功能，后端需地理位置 API |
