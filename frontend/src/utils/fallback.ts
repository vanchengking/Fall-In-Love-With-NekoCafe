import type { Store, DiningTable, Cat, MenuItem, Reservation, DashboardSummary } from '@/types'

// 离线降级数据：字段口径与 /api/stores、/api/tables 一致（V008 种子为 12 城市 38 家门店，
// 此处仅保留代表性门店，完整数据以接口为准）
export const fallbackStores: Store[] = [
  {
    id: 1,
    name: 'NekoCafe 五道口店',
    city: '北京',
    address: '海淀区成府路 99 号',
    phone: '010-88880001',
    open_time: '10:30',
    close_time: '22:30',
    business_hours_text: '周一至周日 10:30-22:30',
    table_count: 4,
    total_seats: 16,
  },
  {
    id: 6,
    name: 'NekoCafe 静安寺店',
    city: '上海',
    address: '静安区南京西路 1601 号',
    phone: '021-88880006',
    open_time: '10:00',
    close_time: '22:30',
    business_hours_text: '周一至周日 10:00-22:30',
    table_count: 4,
    total_seats: 16,
  },
  {
    id: 22,
    name: 'NekoCafe 春熙路店',
    city: '成都',
    address: '锦江区红星路三段 99 号',
    phone: '028-88880022',
    open_time: '10:00',
    close_time: '23:00',
    business_hours_text: '周一至周日 10:00-23:00',
    table_count: 4,
    total_seats: 16,
  },
]

export const fallbackTables: DiningTable[] = [
  { id: 1, code: 'A01', store_id: 1, seats: 2, area: 'window', cat_zone: true, status: 'available', available_for_slot: true },
  { id: 2, code: 'A02', store_id: 1, seats: 4, area: 'window', cat_zone: true, status: 'available', available_for_slot: true },
  { id: 3, code: 'B01', store_id: 1, seats: 4, area: 'main', cat_zone: false, status: 'available', available_for_slot: true },
  { id: 4, code: 'C01', store_id: 1, seats: 6, area: 'party', cat_zone: false, status: 'available', available_for_slot: true },
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
