import type { Store, DiningTable, Cat, MenuItem, Reservation, DashboardSummary } from '@/types'

export const fallbackStores: Store[] = [
  {
    id: 1,
    name: 'NekoCafe 五道口店',
    city: '北京',
    address: '海淀区成府路 99 号',
    phone: '010-88880001',
    table_count: 4,
    total_seats: 16,
  },
]

export const fallbackTables: DiningTable[] = [
  { id: 1, code: 'A01', store_id: 1, seats: 2, area: 'window', cat_zone: true, available_for_slot: true },
  { id: 2, code: 'A02', store_id: 1, seats: 4, area: 'window', cat_zone: true, available_for_slot: true },
  { id: 3, code: 'B01', store_id: 1, seats: 4, area: 'main', cat_zone: false, available_for_slot: true },
]

export const fallbackCats: Cat[] = [
  { id: 1, name: '团子', breed: '布偶', personality_tags: ['quiet', 'gentle'], health_status: 'healthy', weight_kg: '4.80' },
  { id: 2, name: '拿铁', breed: '英短', personality_tags: ['active', 'photo'], health_status: 'healthy', weight_kg: '5.20' },
]

export const fallbackMenuItems: MenuItem[] = [
  { id: 1, name: '猫爪拿铁', category: 'drink', price_cents: 3200, tags: ['coffee', 'signature'] },
  { id: 2, name: '三文鱼能量碗', category: 'meal', price_cents: 5800, tags: ['healthy', 'salmon'] },
  { id: 3, name: '毛线球芝士蛋糕', category: 'dessert', price_cents: 3600, tags: ['sweet', 'cheese'] },
]

export const fallbackReservations: Reservation[] = []

export const fallbackDashboard: DashboardSummary = {
  today_reservations: 2,
  seated_count: 1,
  finished_count: 3,
  unique_customers: 2,
  revenue_cents: 12600,
  turnover_rate: 1,
  repeat_rate: 0.42,
  alerts: [{ id: 1, level: 'warning', title: '晚高峰桌位紧张', detail: '18:00-20:00 预约集中。' }],
}
