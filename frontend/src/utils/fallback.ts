/**
 * fallback.ts — 统一 fallback 入口
 *
 * 所有演示数据来自 mockData.ts，此处仅做重导出，
 * 保持原有 import 路径不变（@/utils/fallback）。
 */

export {
  mockStores    as fallbackStores,
  mockTables    as fallbackTables,
  mockCats      as fallbackCats,
  mockMenuItems as fallbackMenuItems,
  mockReservations as fallbackReservations,
  mockOrders    as fallbackOrders,
  mockDashboard as fallbackDashboard,
  mockAlerts    as fallbackAlerts,
  mockCatHealthRecords as fallbackCatHealthRecords,
  mockReviews   as fallbackReviews,
  mockAuditLogs as fallbackAuditLogs,
  mockCoupons   as fallbackCoupons,
  mockStaffList as fallbackStaffList,
} from '@/utils/mockData'

import type { Store, DiningTable, Cat, MenuItem, Reservation, Order, DashboardSummary } from '@/types'
import {
  mockStores, mockTables, mockCats, mockMenuItems,
  mockReservations, mockOrders, mockDashboard,
  mockCatHealthRecords, mockReviews, mockAuditLogs, mockCoupons, mockStaffList,
} from '@/utils/mockData'

console.info('%c🐱 NekoCafe 演示模式', 'color: #0f766e; font-weight: bold; font-size: 14px')
console.info('%c当前加载的是界面演示数据，实际数据将从后端 API 获取。', 'color: #667085')
console.info(
  `%c门店 ${mockStores.length} 家 · 桌位 ${mockTables.length} 张 · 猫咪 ${mockCats.length} 只 · 预约 ${mockReservations.length} 条 · 订单 ${mockOrders.length} 条 · 评价 ${mockReviews.length} 条 · 健康记录 ${mockCatHealthRecords.length} 条 · 审计日志 ${mockAuditLogs.length} 条`,
  'color: #667085',
)
