/**
 * NekoCafe 全局演示数据生成器
 *
 * 生成足够丰富的、互相引用的演示数据，使所有角色页面看起来真实可用。
 * 数据包含：3 家门店、每家 10 桌位、11 只猫咪、16 道菜品、25+ 预约、12+ 订单、
 *           健康记录、评价、审计日志、优惠券、用户列表。
 */

import type {
  Store, DiningTable, TableStatus, Cat, MenuItem,
  Reservation, Order, DashboardSummary, OperationAlert,
  CatHealthRecord, Review,
} from '@/types'

// ──────────────────────────────────────────────
//  日期工具
// ──────────────────────────────────────────────
const today = new Date().toISOString().slice(0, 10)
const tomorrow = (() => {
  const d = new Date(); d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
})()
const yesterday = (() => {
  const d = new Date(); d.setDate(d.getDate() - 1)
  return d.toISOString().slice(0, 10)
})()
const twoDaysAgo = (() => {
  const d = new Date(); d.setDate(d.getDate() - 2)
  return d.toISOString().slice(0, 10)
})()
const threeDaysAgo = (() => {
  const d = new Date(); d.setDate(d.getDate() - 3)
  return d.toISOString().slice(0, 10)
})()

// ──────────────────────────────────────────────
//  猫咪档案（11 只，覆盖所有健康状态）
// ──────────────────────────────────────────────
export const mockCats: Cat[] = [
  { id: 1,  store_id: 1, name: '团子',     breed: '布偶猫',       personality_tags: ['quiet', 'gentle'],    health_status: 'healthy', weight_kg: '4.80', last_vaccine_at: `${threeDaysAgo}`, photo_url: 'https://placekitten.com/400/300', score: 4.8 },
  { id: 2,  store_id: 1, name: '拿铁',     breed: '英国短毛猫',   personality_tags: ['active', 'photo'],    health_status: 'healthy', weight_kg: '5.20', last_vaccine_at: `${twoDaysAgo}`,   photo_url: 'https://placekitten.com/401/301', score: 4.5 },
  { id: 3,  store_id: 1, name: '芝麻',     breed: '异国短毛猫',   personality_tags: ['lazy', 'friendly'],   health_status: 'observe', weight_kg: '4.50', last_vaccine_at: `${yesterday}`,    photo_url: 'https://placekitten.com/402/302', score: 4.2 },
  { id: 4,  store_id: 1, name: '抹茶',     breed: '美国短毛猫',   personality_tags: ['sweet', 'gentle'],    health_status: 'healthy', weight_kg: '3.90', last_vaccine_at: `${today}`,       photo_url: 'https://placekitten.com/403/303', score: 4.6 },
  { id: 5,  store_id: 1, name: '布丁',     breed: '苏格兰折耳猫', personality_tags: ['quiet', 'lazy'],      health_status: 'resting', weight_kg: '4.10', last_vaccine_at: `${threeDaysAgo}`, photo_url: 'https://placekitten.com/404/304', score: 3.9 },
  { id: 6,  store_id: 2, name: '摩卡',     breed: '暹罗猫',       personality_tags: ['active', 'curious'],  health_status: 'healthy', weight_kg: '3.60', last_vaccine_at: `${twoDaysAgo}`,   photo_url: 'https://placekitten.com/405/305', score: 4.7 },
  { id: 7,  store_id: 2, name: '提拉米苏', breed: '俄罗斯蓝猫',   personality_tags: ['shy', 'gentle'],      health_status: 'sick',    weight_kg: '4.30', last_vaccine_at: `${yesterday}`,    photo_url: 'https://placekitten.com/406/306', score: 4.3 },
  { id: 8,  store_id: 2, name: '乌龙',     breed: '孟加拉豹猫',   personality_tags: ['active', 'playful'],  health_status: 'healthy', weight_kg: '5.00', last_vaccine_at: `${today}`,       photo_url: 'https://placekitten.com/407/307', score: 4.4 },
  { id: 9,  store_id: 3, name: '芋圆',     breed: '金渐层',       personality_tags: ['gentle', 'photo'],    health_status: 'healthy', weight_kg: '4.60', last_vaccine_at: `${twoDaysAgo}`,   photo_url: 'https://placekitten.com/408/308', score: 4.6 },
  { id: 10, store_id: 3, name: '奶盖',     breed: '银渐层',       personality_tags: ['quiet', 'curious'],   health_status: 'healthy', weight_kg: '3.80', last_vaccine_at: `${yesterday}`,    photo_url: 'https://placekitten.com/409/309', score: 4.1 },
  { id: 11, store_id: 3, name: '豆花',     breed: '英短蓝猫',     personality_tags: ['lazy', 'friendly'],   health_status: 'resting', weight_kg: '5.10', last_vaccine_at: `${threeDaysAgo}`, photo_url: 'https://placekitten.com/410/310', score: 4.5 },
]

const catName = (id: number) => mockCats.find(c => c.id === id)?.name || ''

// ──────────────────────────────────────────────
//  猫咪健康记录（每只 3~5 条，含疫苗+体重+互动）
// ──────────────────────────────────────────────
export const mockCatHealthRecords: CatHealthRecord[] = [
  // 团子
  { id: 1, cat_id: 1, weight_kg: 4.5, vaccine_note: '狂犬疫苗第一针', interaction_note: '今日安静，适合安静桌位', recorded_at: `${threeDaysAgo}T10:00:00` },
  { id: 2, cat_id: 1, weight_kg: 4.6, vaccine_note: '', interaction_note: '主动蹭人，性格温顺', recorded_at: `${twoDaysAgo}T14:00:00` },
  { id: 3, cat_id: 1, weight_kg: 4.8, vaccine_note: '狂犬疫苗第二针', interaction_note: '状态良好，食欲正常', recorded_at: `${yesterday}T11:00:00` },
  { id: 4, cat_id: 1, weight_kg: 4.8, vaccine_note: '', interaction_note: '今日互动 5 次，顾客好评', recorded_at: `${today}T15:00:00` },
  // 拿铁
  { id: 5, cat_id: 2, weight_kg: 5.0, vaccine_note: '猫三联疫苗', interaction_note: '活泼好动，喜欢逗猫棒', recorded_at: `${twoDaysAgo}T10:00:00` },
  { id: 6, cat_id: 2, weight_kg: 5.1, vaccine_note: '', interaction_note: '镜头感十足，配合拍照', recorded_at: `${yesterday}T16:00:00` },
  { id: 7, cat_id: 2, weight_kg: 5.2, vaccine_note: '', interaction_note: '今日互动 8 次，最受欢迎', recorded_at: `${today}T14:00:00` },
  // 芝麻（观察中）
  { id: 8, cat_id: 3, weight_kg: 4.6, vaccine_note: '驱虫已完成', interaction_note: '食欲略有下降', recorded_at: `${twoDaysAgo}T09:00:00` },
  { id: 9, cat_id: 3, weight_kg: 4.5, vaccine_note: '', interaction_note: '精神一般，建议观察', recorded_at: `${yesterday}T10:00:00` },
  { id: 10, cat_id: 3, weight_kg: 4.5, vaccine_note: '', interaction_note: '今日活动量减少', recorded_at: `${today}T11:00:00` },
  // 抹茶
  { id: 11, cat_id: 4, weight_kg: 3.7, vaccine_note: '猫三联疫苗第一针', interaction_note: '性格甜美，喜欢被抱', recorded_at: `${today}T10:00:00` },
  { id: 12, cat_id: 4, weight_kg: 3.9, vaccine_note: '', interaction_note: '今日互动 3 次', recorded_at: `${today}T16:00:00` },
  // 布丁（休息中）
  { id: 13, cat_id: 5, weight_kg: 4.0, vaccine_note: '狂犬疫苗', interaction_note: '大部分时间在睡觉', recorded_at: `${threeDaysAgo}T14:00:00` },
  { id: 14, cat_id: 5, weight_kg: 4.1, vaccine_note: '', interaction_note: '偶尔醒来散步', recorded_at: `${yesterday}T15:00:00` },
  // 摩卡
  { id: 15, cat_id: 6, weight_kg: 3.4, vaccine_note: '猫三联疫苗', interaction_note: '好奇心旺盛，探索欲强', recorded_at: `${twoDaysAgo}T11:00:00` },
  { id: 16, cat_id: 6, weight_kg: 3.6, vaccine_note: '', interaction_note: '今日互动 6 次', recorded_at: `${today}T13:00:00` },
  // 提拉米苏（生病）
  { id: 17, cat_id: 7, weight_kg: 4.4, vaccine_note: '驱虫已完成', interaction_note: '精神不佳，需关注', recorded_at: `${yesterday}T09:00:00` },
  { id: 18, cat_id: 7, weight_kg: 4.3, vaccine_note: '', interaction_note: '已安排兽医检查', recorded_at: `${today}T10:00:00` },
  // 乌龙
  { id: 19, cat_id: 8, weight_kg: 4.8, vaccine_note: '狂犬疫苗', interaction_note: '精力充沛，追逐逗猫棒', recorded_at: `${today}T11:00:00` },
  { id: 20, cat_id: 8, weight_kg: 5.0, vaccine_note: '', interaction_note: '今日互动 7 次', recorded_at: `${today}T15:00:00` },
  // 芋圆
  { id: 21, cat_id: 9, weight_kg: 4.4, vaccine_note: '猫三联疫苗', interaction_note: '温柔亲人，适合拍照', recorded_at: `${twoDaysAgo}T10:00:00` },
  { id: 22, cat_id: 9, weight_kg: 4.6, vaccine_note: '', interaction_note: '今日互动 4 次', recorded_at: `${today}T14:00:00` },
  // 奶盖
  { id: 23, cat_id: 10, weight_kg: 3.6, vaccine_note: '驱虫已完成', interaction_note: '安静陪伴型', recorded_at: `${yesterday}T11:00:00` },
  { id: 24, cat_id: 10, weight_kg: 3.8, vaccine_note: '', interaction_note: '今日互动 2 次', recorded_at: `${today}T15:00:00` },
  // 豆花（休息中）
  { id: 25, cat_id: 11, weight_kg: 5.0, vaccine_note: '狂犬疫苗', interaction_note: '典型沙发土豆', recorded_at: `${threeDaysAgo}T10:00:00` },
  { id: 26, cat_id: 11, weight_kg: 5.1, vaccine_note: '', interaction_note: '偶尔慢悠悠走动', recorded_at: `${yesterday}T14:00:00` },
]

// ──────────────────────────────────────────────
//  菜单（16 道，覆盖 3 家门店 + 全部分类）
// ──────────────────────────────────────────────
export const mockMenuItems: MenuItem[] = [
  // 五道口店
  { id: 1,  store_id: 1, name: '猫爪拿铁',         category: 'drink',   price_cents: 3200, tags: ['coffee', 'signature'],     photo_url: 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=300&h=200&fit=crop', score: 4.8 },
  { id: 2,  store_id: 1, name: '三文鱼能量碗',     category: 'meal',    price_cents: 5800, tags: ['healthy', 'salmon'],       photo_url: 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=300&h=200&fit=crop', score: 4.5 },
  { id: 3,  store_id: 1, name: '毛线球芝士蛋糕',   category: 'dessert', price_cents: 3600, tags: ['sweet', 'cheese'],         photo_url: 'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=300&h=200&fit=crop', score: 4.6 },
  { id: 4,  store_id: 1, name: '猫咪冰淇淋',       category: 'dessert', price_cents: 2800, tags: ['sweet', 'ice-cream'],      photo_url: 'https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=300&h=200&fit=crop', score: 4.3 },
  { id: 5,  store_id: 1, name: '手冲埃塞俄比亚',   category: 'drink',   price_cents: 4200, tags: ['coffee', 'single-origin'], photo_url: 'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=300&h=200&fit=crop', score: 4.7 },
  { id: 6,  store_id: 1, name: '全日早午餐盘',     category: 'meal',    price_cents: 6800, tags: ['healthy', 'brunch'],       photo_url: 'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=300&h=200&fit=crop', score: 4.4 },
  { id: 7,  store_id: 1, name: '抹茶拿铁',         category: 'drink',   price_cents: 3000, tags: ['tea', 'matcha'],           photo_url: 'https://images.unsplash.com/photo-1536256263959-770b48d82b0a?w=300&h=200&fit=crop', score: 4.2 },
  { id: 8,  store_id: 1, name: '提拉米苏',         category: 'dessert', price_cents: 3800, tags: ['sweet', 'coffee'],         photo_url: 'https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=300&h=200&fit=crop', score: 4.5 },
  // 朝阳大悦城店
  { id: 9,  store_id: 2, name: '焦糖玛奇朵',       category: 'drink',   price_cents: 3600, tags: ['coffee', 'sweet'],         photo_url: 'https://images.unsplash.com/photo-1485808191679-5f86510681a2?w=300&h=200&fit=crop', score: 4.4 },
  { id: 10, store_id: 2, name: '烟熏三文鱼贝果',   category: 'meal',    price_cents: 4600, tags: ['healthy', 'salmon'],       photo_url: 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=300&h=200&fit=crop', score: 4.3 },
  { id: 11, store_id: 2, name: '草莓千层蛋糕',     category: 'dessert', price_cents: 4200, tags: ['sweet', 'strawberry'],     photo_url: 'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=300&h=200&fit=crop', score: 4.6 },
  { id: 12, store_id: 2, name: '冰美式',           category: 'drink',   price_cents: 2200, tags: ['coffee', 'iced'],          photo_url: 'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=300&h=200&fit=crop', score: 4.1 },
  // 西单店
  { id: 13, store_id: 3, name: '桂花拿铁',         category: 'drink',   price_cents: 3400, tags: ['coffee', 'floral'],        photo_url: 'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=300&h=200&fit=crop', score: 4.5 },
  { id: 14, store_id: 3, name: '牛油果吐司',       category: 'meal',    price_cents: 3800, tags: ['healthy', 'avocado'],      photo_url: 'https://images.unsplash.com/photo-1541519227354-08fa5d50c44d?w=300&h=200&fit=crop', score: 4.3 },
  { id: 15, store_id: 3, name: '芒果慕斯',         category: 'dessert', price_cents: 3200, tags: ['sweet', 'mango'],          photo_url: 'https://images.unsplash.com/photo-1488477181946-6428a0291777?w=300&h=200&fit=crop', score: 4.4 },
  { id: 16, store_id: 3, name: '燕麦奶拿铁',       category: 'drink',   price_cents: 3000, tags: ['coffee', 'oat-milk'],      photo_url: 'https://images.unsplash.com/photo-1461023058943-07fcbe16d735?w=300&h=200&fit=crop', score: 4.2 },
]

// ──────────────────────────────────────────────
//  门店（3 家）
// ──────────────────────────────────────────────
export const mockStores: (Store & { photo_url: string })[] = [
  { id: 1, name: 'NekoCafe 五道口店',     city: '北京', address: '海淀区成府路 99 号 2F',       phone: '010-82880001', table_count: 10, total_seats: 40, open_time: '10:00', close_time: '22:00', photo_url: 'https://images.unsplash.com/photo-1559925393-8be0ec4767c8?w=600&h=400&fit=crop' },
  { id: 2, name: 'NekoCafe 朝阳大悦城店', city: '北京', address: '朝阳区朝阳北路 101 号 5F',    phone: '010-85880002', table_count: 10, total_seats: 38, open_time: '10:00', close_time: '22:00', photo_url: 'https://images.unsplash.com/photo-1554118811-1e0d58224f24?w=600&h=400&fit=crop' },
  { id: 3, name: 'NekoCafe 西单店',       city: '北京', address: '西城区西单北大街 110 号 B1',   phone: '010-66880003', table_count: 9,  total_seats: 34, open_time: '10:30', close_time: '21:30', photo_url: 'https://images.unsplash.com/photo-1600093463592-8e36ae95ef56?w=600&h=400&fit=crop' },
]

// ──────────────────────────────────────────────
//  桌位（29 张，覆盖全部状态）
// ──────────────────────────────────────────────
export const mockTables: DiningTable[] = [
  // 五道口店（10 桌）
  { id: 101, code: 'A01', store_id: 1, seats: 2, area: 'window', cat_zone: true,  status: 'occupied',   cat_name: '团子',   current_reservation_id: 1002 },
  { id: 102, code: 'A02', store_id: 1, seats: 4, area: 'window', cat_zone: true,  status: 'dining',     cat_name: '拿铁',   current_reservation_id: 1003 },
  { id: 103, code: 'A03', store_id: 1, seats: 2, area: 'window', cat_zone: true,  status: 'reserved',   cat_name: '芝麻',   current_reservation_id: 1001 },
  { id: 104, code: 'B01', store_id: 1, seats: 4, area: 'main',   cat_zone: false, status: 'free' },
  { id: 105, code: 'B02', store_id: 1, seats: 6, area: 'main',   cat_zone: false, status: 'reserved',   cat_name: '抹茶',   current_reservation_id: 1004 },
  { id: 106, code: 'B03', store_id: 1, seats: 4, area: 'main',   cat_zone: true,  status: 'cleaning',   cat_name: '布丁' },
  { id: 107, code: 'B04', store_id: 1, seats: 4, area: 'main',   cat_zone: false, status: 'free' },
  { id: 108, code: 'C01', store_id: 1, seats: 6, area: 'party',  cat_zone: true,  status: 'cleaning',   cat_name: '团子' },
  { id: 109, code: 'C02', store_id: 1, seats: 8, area: 'party',  cat_zone: false, status: 'free' },
  { id: 110, code: 'C03', store_id: 1, seats: 8, area: 'party',  cat_zone: false, status: 'occupied',   cat_name: '拿铁',   current_reservation_id: 1005 },

  // 朝阳大悦城店（10 桌）
  { id: 201, code: 'A01', store_id: 2, seats: 2, area: 'window', cat_zone: true,  status: 'occupied',   cat_name: '摩卡',     current_reservation_id: 2002 },
  { id: 202, code: 'A02', store_id: 2, seats: 2, area: 'window', cat_zone: true,  status: 'dining',     cat_name: '提拉米苏', current_reservation_id: 2003 },
  { id: 203, code: 'A03', store_id: 2, seats: 4, area: 'window', cat_zone: true,  status: 'reserved',   cat_name: '乌龙',     current_reservation_id: 2001 },
  { id: 204, code: 'B01', store_id: 2, seats: 4, area: 'main',   cat_zone: false, status: 'free' },
  { id: 205, code: 'B02', store_id: 2, seats: 4, area: 'main',   cat_zone: false, status: 'free' },
  { id: 206, code: 'B03', store_id: 2, seats: 6, area: 'main',   cat_zone: true,  status: 'reserved',   cat_name: '摩卡',     current_reservation_id: 2004 },
  { id: 207, code: 'B04', store_id: 2, seats: 4, area: 'main',   cat_zone: false, status: 'cleaning',   cat_name: '乌龙' },
  { id: 208, code: 'C01', store_id: 2, seats: 6, area: 'party',  cat_zone: true,  status: 'free' },
  { id: 209, code: 'C02', store_id: 2, seats: 8, area: 'party',  cat_zone: false, status: 'occupied',   cat_name: '提拉米苏', current_reservation_id: 2005 },
  { id: 210, code: 'C03', store_id: 2, seats: 8, area: 'party',  cat_zone: false, status: 'free' },

  // 西单店（9 桌）
  { id: 301, code: 'A01', store_id: 3, seats: 2, area: 'window', cat_zone: true,  status: 'reserved',   cat_name: '芋圆', current_reservation_id: 3001 },
  { id: 302, code: 'A02', store_id: 3, seats: 4, area: 'window', cat_zone: true,  status: 'occupied',   cat_name: '奶盖', current_reservation_id: 3002 },
  { id: 303, code: 'B01', store_id: 3, seats: 4, area: 'main',   cat_zone: false, status: 'dining',     cat_name: '豆花', current_reservation_id: 3003 },
  { id: 304, code: 'B02', store_id: 3, seats: 4, area: 'main',   cat_zone: false, status: 'free' },
  { id: 305, code: 'B03', store_id: 3, seats: 6, area: 'main',   cat_zone: true,  status: 'free',       cat_name: '芋圆' },
  { id: 306, code: 'B04', store_id: 3, seats: 4, area: 'main',   cat_zone: false, status: 'cleaning' },
  { id: 307, code: 'C01', store_id: 3, seats: 6, area: 'party',  cat_zone: true,  status: 'free',       cat_name: '豆花' },
  { id: 308, code: 'C02', store_id: 3, seats: 8, area: 'party',  cat_zone: false, status: 'reserved',   cat_name: '奶盖', current_reservation_id: 3004 },
  { id: 309, code: 'C03', store_id: 3, seats: 8, area: 'party',  cat_zone: false, status: 'free' },
]

// ──────────────────────────────────────────────
//  预约（25 条，覆盖全部状态 + 多时间段）
// ──────────────────────────────────────────────
export const mockReservations: Reservation[] = [
  // ── 五道口店 今天 ──
  { id: 1001, user_id: 101, store_id: 1, table_id: 103, recommended_cat_id: 3,  reservation_date: today, reservation_time: '18:30', party_size: 2, status: 'booked',   note: '希望坐窗边',             customer_name: '林小满', mobile_number: '13800001111', table_code: 'A03', cat_name: '芝麻' },
  { id: 1002, user_id: 102, store_id: 1, table_id: 101, recommended_cat_id: 1,  reservation_date: today, reservation_time: '16:00', party_size: 3, status: 'seated',   note: '',                        customer_name: '张明远', mobile_number: '13800002222', table_code: 'A01', cat_name: '团子' },
  { id: 1003, user_id: 103, store_id: 1, table_id: 102, recommended_cat_id: 2,  reservation_date: today, reservation_time: '17:00', party_size: 2, status: 'dining',   note: '少冰',                    customer_name: '王思琪', mobile_number: '13800003333', table_code: 'A02', cat_name: '拿铁' },
  { id: 1004, user_id: 104, store_id: 1, table_id: 105, recommended_cat_id: 4,  reservation_date: today, reservation_time: '19:30', party_size: 4, status: 'booked',   note: '公司团建',                customer_name: '陈佳怡', mobile_number: '13800004444', table_code: 'B02', cat_name: '抹茶' },
  { id: 1005, user_id: 105, store_id: 1, table_id: 110, recommended_cat_id: 2,  reservation_date: today, reservation_time: '15:00', party_size: 5, status: 'finished', note: '',                        customer_name: '李浩然', mobile_number: '13800005555', table_code: 'C03', cat_name: '拿铁' },
  { id: 1006, user_id: 106, store_id: 1, table_id: 106, recommended_cat_id: 5,  reservation_date: today, reservation_time: '16:30', party_size: 2, status: 'finished', note: '',                        customer_name: '赵雨萱', mobile_number: '13800006666', table_code: 'B03', cat_name: '布丁' },
  { id: 1007, user_id: 107, store_id: 1, table_id: null, recommended_cat_id: 1, reservation_date: today, reservation_time: '11:00', party_size: 2, status: 'cancelled', note: '临时有事',               customer_name: '周子墨', mobile_number: '13800007777', table_code: 'B01', cat_name: '团子' },
  { id: 1008, user_id: 108, store_id: 1, table_id: null, recommended_cat_id: 4, reservation_date: today, reservation_time: '12:00', party_size: 3, status: 'no_show',   note: '',                       customer_name: '吴天宇', mobile_number: '13800008888', table_code: 'B04', cat_name: '抹茶' },
  // 五道口 明天
  { id: 1009, user_id: 109, store_id: 1, table_id: 108, recommended_cat_id: 1,  reservation_date: tomorrow, reservation_time: '19:00', party_size: 6, status: 'booked', note: '生日聚会，需要蛋糕',    customer_name: '郑梦涵', mobile_number: '13800009999', table_code: 'C01', cat_name: '团子' },
  { id: 1010, user_id: 110, store_id: 1, table_id: 109, recommended_cat_id: 2,  reservation_date: tomorrow, reservation_time: '14:00', party_size: 4, status: 'booked', note: '',                       customer_name: '何俊杰', mobile_number: '13800010000', table_code: 'C02', cat_name: '拿铁' },

  // ── 朝阳大悦城店 今天 ──
  { id: 2001, user_id: 201, store_id: 2, table_id: 203, recommended_cat_id: 8,  reservation_date: today, reservation_time: '18:00', party_size: 2, status: 'booked',   note: '',                        customer_name: '孙小雪', mobile_number: '13900001111', table_code: 'A03', cat_name: '乌龙' },
  { id: 2002, user_id: 202, store_id: 2, table_id: 201, recommended_cat_id: 6,  reservation_date: today, reservation_time: '16:30', party_size: 2, status: 'seated',   note: '对猫毛过敏但还是想来',   customer_name: '刘思远', mobile_number: '13900002222', table_code: 'A01', cat_name: '摩卡' },
  { id: 2003, user_id: 203, store_id: 2, table_id: 202, recommended_cat_id: 7,  reservation_date: today, reservation_time: '17:30', party_size: 3, status: 'dining',   note: '',                        customer_name: '黄雅琴', mobile_number: '13900003333', table_code: 'A02', cat_name: '提拉米苏' },
  { id: 2004, user_id: 204, store_id: 2, table_id: 206, recommended_cat_id: 6,  reservation_date: today, reservation_time: '19:00', party_size: 4, status: 'booked',   note: '带小朋友',                customer_name: '吴天宇', mobile_number: '13900004444', table_code: 'B03', cat_name: '摩卡' },
  { id: 2005, user_id: 205, store_id: 2, table_id: 209, recommended_cat_id: 7,  reservation_date: today, reservation_time: '14:00', party_size: 6, status: 'finished', note: '',                        customer_name: '郑梦涵', mobile_number: '13900005555', table_code: 'C02', cat_name: '提拉米苏' },
  { id: 2006, user_id: 206, store_id: 2, table_id: null, recommended_cat_id: 8, reservation_date: today, reservation_time: '10:30', party_size: 2, status: 'no_show',   note: '',                       customer_name: '何俊杰', mobile_number: '13900006666', table_code: 'B01', cat_name: '乌龙' },
  // 朝阳 明天
  { id: 2007, user_id: 207, store_id: 2, table_id: 208, recommended_cat_id: 6,  reservation_date: tomorrow, reservation_time: '18:30', party_size: 5, status: 'booked', note: '朋友聚会',               customer_name: '马晓晨', mobile_number: '13900007777', table_code: 'C01', cat_name: '摩卡' },

  // ── 西单店 今天 ──
  { id: 3001, user_id: 301, store_id: 3, table_id: 301, recommended_cat_id: 9,  reservation_date: today, reservation_time: '17:00', party_size: 2, status: 'booked',   note: '',                        customer_name: '朱雨辰', mobile_number: '13600001111', table_code: 'A01', cat_name: '芋圆' },
  { id: 3002, user_id: 302, store_id: 3, table_id: 302, recommended_cat_id: 10, reservation_date: today, reservation_time: '16:00', party_size: 2, status: 'seated',   note: '第一次来',                customer_name: '许博文', mobile_number: '13600002222', table_code: 'A02', cat_name: '奶盖' },
  { id: 3003, user_id: 303, store_id: 3, table_id: 303, recommended_cat_id: 11, reservation_date: today, reservation_time: '15:30', party_size: 3, status: 'dining',   note: '',                        customer_name: '韩冰洁', mobile_number: '13600003333', table_code: 'B01', cat_name: '豆花' },
  { id: 3004, user_id: 304, store_id: 3, table_id: 308, recommended_cat_id: 10, reservation_date: today, reservation_time: '19:00', party_size: 5, status: 'booked',   note: '商务洽谈',                customer_name: '曹文轩', mobile_number: '13600004444', table_code: 'C02', cat_name: '奶盖' },
  { id: 3005, user_id: 305, store_id: 3, table_id: null, recommended_cat_id: 9, reservation_date: today, reservation_time: '11:30', party_size: 2, status: 'cancelled', note: '改期',                    customer_name: '蔡明辉', mobile_number: '13600005555', table_code: 'A03', cat_name: '芋圆' },
  // 西单 明天
  { id: 3006, user_id: 306, store_id: 3, table_id: 307, recommended_cat_id: 11, reservation_date: tomorrow, reservation_time: '18:00', party_size: 4, status: 'booked', note: '',                      customer_name: '田雨橙', mobile_number: '13600006666', table_code: 'C01', cat_name: '豆花' },
  { id: 3007, user_id: 307, store_id: 3, table_id: 309, recommended_cat_id: 9,  reservation_date: tomorrow, reservation_time: '15:00', party_size: 6, status: 'booked', note: '团建活动',              customer_name: '钱思源', mobile_number: '13600007777', table_code: 'C03', cat_name: '芋圆' },
]

// ──────────────────────────────────────────────
//  订单（12 条，覆盖全部状态）
// ──────────────────────────────────────────────
export const mockOrders: Order[] = [
  // 五道口 - pending
  {
    id: 2001, reservation_id: 1001, user_id: 101, store_id: 1, status: 'pending', payment_status: 'unpaid', total_cents: 6800,
    customer_name: '林小满', table_code: 'A03', party_size: 2, created_at: `${today}T18:35:00`,
    reservation_status: 'booked', note: '希望坐窗边', cat_reminder: '芝麻今日心情不错，可以互动',
    items: [
      { id: 1, menu_item_id: 5, name: '手冲埃塞俄比亚', quantity: 1, price_cents: 4200, subtotal_cents: 4200, served: false },
      { id: 2, menu_item_id: 3, name: '毛线球芝士蛋糕', quantity: 1, price_cents: 3600, subtotal_cents: 3600, served: false },
    ],
  },
  // 五道口 - preparing
  {
    id: 2002, reservation_id: 1002, user_id: 102, store_id: 1, status: 'preparing', payment_status: 'unpaid', total_cents: 12200,
    customer_name: '张明远', table_code: 'A01', party_size: 3, created_at: `${today}T16:10:00`,
    reservation_status: 'seated', note: '', cat_reminder: '团子今日状态良好，适合拍照',
    items: [
      { id: 3, menu_item_id: 1, name: '猫爪拿铁',       quantity: 2, price_cents: 3200, subtotal_cents: 6400, served: false },
      { id: 4, menu_item_id: 2, name: '三文鱼能量碗',   quantity: 1, price_cents: 5800, subtotal_cents: 5800, served: false },
    ],
  },
  // 五道口 - served
  {
    id: 2003, reservation_id: 1003, user_id: 103, store_id: 1, status: 'served', payment_status: 'unpaid', total_cents: 12800,
    customer_name: '王思琪', table_code: 'A02', party_size: 2, created_at: `${today}T17:10:00`, served_at: `${today}T17:25:00`,
    reservation_status: 'dining', note: '少冰', cat_reminder: '拿铁今天很活跃',
    items: [
      { id: 5, menu_item_id: 2, name: '三文鱼能量碗',   quantity: 1, price_cents: 5800, subtotal_cents: 5800, served: true },
      { id: 6, menu_item_id: 5, name: '手冲埃塞俄比亚', quantity: 1, price_cents: 4200, subtotal_cents: 4200, served: true },
      { id: 7, menu_item_id: 4, name: '猫咪冰淇淋',     quantity: 1, price_cents: 2800, subtotal_cents: 2800, served: true },
    ],
  },
  // 五道口 - paid
  {
    id: 2004, reservation_id: 1005, user_id: 105, store_id: 1, status: 'paid', payment_status: 'paid', total_cents: 16600,
    customer_name: '李浩然', table_code: 'C03', party_size: 5, created_at: `${today}T15:05:00`, served_at: `${today}T15:30:00`, paid_at: `${today}T16:20:00`,
    reservation_status: 'finished', payment_method: '微信支付',
    items: [
      { id: 8, menu_item_id: 1, name: '猫爪拿铁',       quantity: 3, price_cents: 3200, subtotal_cents: 9600, served: true },
      { id: 9, menu_item_id: 6, name: '全日早午餐盘',   quantity: 1, price_cents: 6800, subtotal_cents: 6800, served: true },
    ],
  },
  // 五道口 - pending (第二条)
  {
    id: 2005, reservation_id: 1004, user_id: 104, store_id: 1, status: 'pending', payment_status: 'unpaid', total_cents: 18800,
    customer_name: '陈佳怡', table_code: 'B02', party_size: 4, created_at: `${today}T19:35:00`,
    reservation_status: 'booked', note: '公司团建，需要安静区域',
    items: [
      { id: 10, menu_item_id: 2, name: '三文鱼能量碗',   quantity: 2, price_cents: 5800, subtotal_cents: 11600, served: false },
      { id: 11, menu_item_id: 1, name: '猫爪拿铁',       quantity: 1, price_cents: 3200, subtotal_cents: 3200, served: false },
      { id: 12, menu_item_id: 4, name: '猫咪冰淇淋',     quantity: 1, price_cents: 2800, subtotal_cents: 2800, served: false },
    ],
  },
  // 五道口 - exception
  {
    id: 2006, reservation_id: 1006, user_id: 106, store_id: 1, status: 'exception', payment_status: 'unpaid', total_cents: 7200,
    customer_name: '赵雨萱', table_code: 'B03', party_size: 2, created_at: `${today}T16:40:00`,
    reservation_status: 'finished', note: '对花生过敏，已通知厨房',
    items: [
      { id: 13, menu_item_id: 3, name: '毛线球芝士蛋糕', quantity: 1, price_cents: 3600, subtotal_cents: 3600, served: false },
      { id: 14, menu_item_id: 1, name: '猫爪拿铁',       quantity: 1, price_cents: 3200, subtotal_cents: 3200, served: false },
    ],
  },
  // 朝阳 - pending
  {
    id: 3001, reservation_id: 2001, user_id: 201, store_id: 2, status: 'pending', payment_status: 'unpaid', total_cents: 8200,
    customer_name: '孙小雪', table_code: 'A03', party_size: 2, created_at: `${today}T18:05:00`,
    reservation_status: 'booked',
    items: [
      { id: 15, menu_item_id: 9,  name: '焦糖玛奇朵',     quantity: 1, price_cents: 3600, subtotal_cents: 3600, served: false },
      { id: 16, menu_item_id: 11, name: '草莓千层蛋糕',   quantity: 1, price_cents: 4200, subtotal_cents: 4200, served: false },
    ],
  },
  // 朝阳 - preparing
  {
    id: 3002, reservation_id: 2002, user_id: 202, store_id: 2, status: 'preparing', payment_status: 'unpaid', total_cents: 8200,
    customer_name: '刘思远', table_code: 'A01', party_size: 2, created_at: `${today}T16:35:00`,
    reservation_status: 'seated', note: '对猫毛过敏',
    items: [
      { id: 17, menu_item_id: 12, name: '冰美式',         quantity: 2, price_cents: 2200, subtotal_cents: 4400, served: false },
      { id: 18, menu_item_id: 10, name: '烟熏三文鱼贝果', quantity: 1, price_cents: 4600, subtotal_cents: 4600, served: false },
    ],
  },
  // 朝阳 - paid
  {
    id: 3003, reservation_id: 2005, user_id: 205, store_id: 2, status: 'paid', payment_status: 'paid', total_cents: 14600,
    customer_name: '郑梦涵', table_code: 'C02', party_size: 6, created_at: `${today}T14:05:00`, served_at: `${today}T14:30:00`, paid_at: `${today}T15:40:00`,
    reservation_status: 'finished', payment_method: '支付宝',
    items: [
      { id: 19, menu_item_id: 9,  name: '焦糖玛奇朵',     quantity: 3, price_cents: 3600, subtotal_cents: 10800, served: true },
      { id: 20, menu_item_id: 11, name: '草莓千层蛋糕',   quantity: 1, price_cents: 4200, subtotal_cents: 4200, served: true },
    ],
  },
  // 西单 - served
  {
    id: 4001, reservation_id: 3003, user_id: 303, store_id: 3, status: 'served', payment_status: 'unpaid', total_cents: 7200,
    customer_name: '韩冰洁', table_code: 'B01', party_size: 3, created_at: `${today}T15:35:00`, served_at: `${today}T15:55:00`,
    reservation_status: 'dining',
    items: [
      { id: 21, menu_item_id: 13, name: '桂花拿铁',       quantity: 2, price_cents: 3400, subtotal_cents: 6800, served: true },
      { id: 22, menu_item_id: 15, name: '芒果慕斯',       quantity: 1, price_cents: 3200, subtotal_cents: 3200, served: true },
    ],
  },
  // 西单 - pending
  {
    id: 4002, reservation_id: 3001, user_id: 301, store_id: 3, status: 'pending', payment_status: 'unpaid', total_cents: 7200,
    customer_name: '朱雨辰', table_code: 'A01', party_size: 2, created_at: `${today}T17:05:00`,
    reservation_status: 'booked',
    items: [
      { id: 23, menu_item_id: 16, name: '燕麦奶拿铁',     quantity: 1, price_cents: 3000, subtotal_cents: 3000, served: false },
      { id: 24, menu_item_id: 14, name: '牛油果吐司',     quantity: 1, price_cents: 3800, subtotal_cents: 3800, served: false },
    ],
  },
  // 西单 - paid (昨天)
  {
    id: 4003, reservation_id: 3002, user_id: 302, store_id: 3, status: 'paid', payment_status: 'paid', total_cents: 10200,
    customer_name: '许博文', table_code: 'A02', party_size: 2, created_at: `${yesterday}T16:05:00`, served_at: `${yesterday}T16:30:00`, paid_at: `${yesterday}T17:20:00`,
    reservation_status: 'finished', payment_method: '微信支付',
    items: [
      { id: 25, menu_item_id: 13, name: '桂花拿铁',       quantity: 2, price_cents: 3400, subtotal_cents: 6800, served: true },
      { id: 26, menu_item_id: 15, name: '芒果慕斯',       quantity: 1, price_cents: 3200, subtotal_cents: 3200, served: true },
    ],
  },
]

// ──────────────────────────────────────────────
//  评价（10 条）
// ──────────────────────────────────────────────
export const mockReviews: Review[] = [
  { id: 1,  reservation_id: 1005, user_id: 105, store_id: 1, cat_id: 2,  rating: 5, content: '环境非常好，猫咪很亲人，拿铁拉花太可爱了！下次还来。', created_at: `${today}T16:30:00` },
  { id: 2,  reservation_id: 1006, user_id: 106, store_id: 1, cat_id: 5,  rating: 4, content: '团子太萌了，一直在腿上睡觉。甜品也不错，就是等位有点久。', created_at: `${today}T17:00:00` },
  { id: 3,  reservation_id: 2005, user_id: 205, store_id: 2, cat_id: 7,  rating: 5, content: '带小朋友来的，玩得特别开心。猫咪都很健康干净，工作人员也很热情。', created_at: `${yesterday}T15:45:00` },
  { id: 4,  reservation_id: null,  user_id: 109, store_id: 1, cat_id: 1,  rating: 5, content: '抹茶蛋糕绝了！猫咪摩卡特别活泼，拍照超配合。', created_at: `${yesterday}T14:00:00` },
  { id: 5,  reservation_id: null,  user_id: 110, store_id: 1, cat_id: 4,  rating: 4, content: '环境安静适合办公，手冲咖啡水准在线。布丁猫太治愈了。', created_at: `${twoDaysAgo}T16:00:00` },
  { id: 6,  reservation_id: null,  user_id: 207, store_id: 2, cat_id: 6,  rating: 5, content: '第三次来了，每次都有新猫咪。店员很专业，讲解猫咪性格。', created_at: `${twoDaysAgo}T18:00:00` },
  { id: 7,  reservation_id: null,  user_id: 208, store_id: 2, cat_id: 8,  rating: 4, content: '猫爪拿铁必点！拉花是猫爪形状，舍不得喝。', created_at: `${threeDaysAgo}T15:00:00` },
  { id: 8,  reservation_id: null,  user_id: 306, store_id: 3, cat_id: 9,  rating: 5, content: '西单店环境很好，桂花拿铁特别香。芋圆超级乖。', created_at: `${yesterday}T18:00:00` },
  { id: 9,  reservation_id: null,  user_id: 307, store_id: 3, cat_id: 10, rating: 4, content: '牛油果吐司很新鲜，芒果慕斯甜度刚好。奶盖猫很安静。', created_at: `${twoDaysAgo}T17:00:00` },
  { id: 10, reservation_id: null,  user_id: 308, store_id: 3, cat_id: 11, rating: 3, content: '豆花在睡觉没互动到，有点遗憾。饮品不错。', created_at: `${threeDaysAgo}T14:00:00` },
]

// ──────────────────────────────────────────────
//  运营告警（5 条）
// ──────────────────────────────────────────────
export const mockAlerts: OperationAlert[] = [
  { id: 1, store_id: 1, level: 'warning', title: '晚高峰桌位紧张',         detail: '18:00-20:00 预约集中（4 桌），建议提前协调加桌。' },
  { id: 2, store_id: 1, level: 'info',    title: '团子休息提醒',           detail: '团子 18:00 后需休息，建议安排其他猫咪接替靠窗猫区。' },
  { id: 3, store_id: 1, level: 'warning', title: '毛线球芝士蛋糕库存不足', detail: '当前剩余 3 份，预计今日售罄，请通知后厨备货。' },
  { id: 4, store_id: 1, level: 'info',    title: '赵雨萱订单异常跟进',     detail: '花生过敏订单，已协调退菜，待顾客确认。' },
  { id: 5, store_id: 1, level: 'warning', title: 'B03 桌清台超时',         detail: 'B03 已空闲 15 分钟未清理，影响后续预约。' },
]

// ──────────────────────────────────────────────
//  审计日志（10 条）
// ──────────────────────────────────────────────
export interface AuditLog {
  id: number
  created_at: string
  user_name: string
  action: string
  target_table: string
  target_id: number
  detail: { method?: string; path?: string; status?: number; body?: Record<string, unknown> }
  ip_address: string
}

export const mockAuditLogs: AuditLog[] = [
  { id: 1,  created_at: `${today}T19:35:12`, user_name: '高店员',     action: 'CREATE',       target_table: 'orders',       target_id: 2001, detail: { method: 'POST',   path: '/api/orders',            status: 201, body: { store_id: 1, total_cents: 6800 } }, ip_address: '192.168.1.101' },
  { id: 2,  created_at: `${today}T18:30:05`, user_name: '系统',       action: 'STATUS_CHANGE', target_table: 'reservations', target_id: 1001, detail: { method: 'PATCH',  path: '/api/reservations/1001/status', status: 200, body: { status: 'booked' } }, ip_address: '192.168.1.1' },
  { id: 3,  created_at: `${today}T17:25:30`, user_name: '高店员',     action: 'STATUS_CHANGE', target_table: 'orders',       target_id: 2003, detail: { method: 'PATCH',  path: '/api/orders/2003/status', status: 200, body: { status: 'served' } }, ip_address: '192.168.1.101' },
  { id: 4,  created_at: `${today}T16:20:45`, user_name: '系统',       action: 'STATUS_CHANGE', target_table: 'orders',       target_id: 2004, detail: { method: 'PATCH',  path: '/api/orders/2004/status', status: 200, body: { status: 'paid' } }, ip_address: '192.168.1.1' },
  { id: 5,  created_at: `${today}T16:00:12`, user_name: '葛店长',     action: 'UPDATE',       target_table: 'stores',       target_id: 1,    detail: { method: 'PATCH',  path: '/api/stores/1',          status: 200, body: { phone: '010-82880001' } }, ip_address: '192.168.1.102' },
  { id: 6,  created_at: `${today}T15:30:00`, user_name: '卢猫咪管家', action: 'CREATE',       target_table: 'cat_health_records', target_id: 14, detail: { method: 'POST', path: '/api/cat-health-records', status: 201, body: { cat_id: 5, weight_kg: 4.1 } }, ip_address: '192.168.1.105' },
  { id: 7,  created_at: `${today}T14:00:08`, user_name: '系统管理员', action: 'CREATE',       target_table: 'users',        target_id: 7,    detail: { method: 'POST',   path: '/api/users',             status: 201, body: { name: '新店员', role: 'staff' } }, ip_address: '192.168.1.106' },
  { id: 8,  created_at: `${today}T11:30:22`, user_name: '高店员',     action: 'DELETE',       target_table: 'menu_items',   target_id: 99,   detail: { method: 'DELETE', path: '/api/menu-items/99',     status: 200 }, ip_address: '192.168.1.101' },
  { id: 9,  created_at: `${yesterday}T20:00:00`, user_name: '系统',   action: 'STATUS_CHANGE', target_table: 'reservations', target_id: 1005, detail: { method: 'PATCH', path: '/api/reservations/1005/status', status: 200, body: { status: 'finished' } }, ip_address: '192.168.1.1' },
  { id: 10, created_at: `${yesterday}T18:15:33`, user_name: '总部运营', action: 'UPDATE',     target_table: 'stores',       target_id: 2,    detail: { method: 'PATCH',  path: '/api/stores/2',          status: 200, body: { address: '朝阳区朝阳北路 101 号 5F' } }, ip_address: '192.168.1.104' },
]

// ──────────────────────────────────────────────
//  优惠券（4 条）
// ──────────────────────────────────────────────
export interface Coupon {
  id: number; code: string; title: string; discount_cents: number; min_spend_cents: number
  valid_from: string; valid_to: string; status: string; claimed_count: number; used_count: number
}

export const mockCoupons: Coupon[] = [
  { id: 1, code: 'NEKO20',   title: '新客立减 20 元',   discount_cents: 2000, min_spend_cents: 5000, valid_from: today, valid_to: tomorrow, status: 'active', claimed_count: 128, used_count: 56 },
  { id: 2, code: 'CAT50',    title: '猫咪日半价',       discount_cents: 5000, min_spend_cents: 10000, valid_from: today, valid_to: tomorrow, status: 'active', claimed_count: 45, used_count: 12 },
  { id: 3, code: 'WEEKEND',  title: '周末满减',         discount_cents: 3000, min_spend_cents: 8000, valid_from: yesterday, valid_to: tomorrow, status: 'active', claimed_count: 200, used_count: 89 },
  { id: 4, code: 'SPRING',   title: '春日限定优惠',     discount_cents: 1500, min_spend_cents: 3000, valid_from: threeDaysAgo, valid_to: yesterday, status: 'expired', claimed_count: 312, used_count: 198 },
]

// ──────────────────────────────────────────────
//  用户/员工列表（6 条）
// ──────────────────────────────────────────────
export interface StaffMember {
  id: number; name: string; mobile_number: string; role: string; store_id?: number
}

export const mockStaffList: StaffMember[] = [
  { id: 1, name: '林小满',     mobile_number: '13800000001', role: 'customer' },
  { id: 2, name: '高店员',     mobile_number: '13800000002', role: 'staff',      store_id: 1 },
  { id: 3, name: '葛店长',     mobile_number: '13800000003', role: 'manager',    store_id: 1 },
  { id: 4, name: '总部运营',   mobile_number: '13800000004', role: 'operator' },
  { id: 5, name: '卢猫咪管家', mobile_number: '13800000005', role: 'cat_keeper', store_id: 1 },
  { id: 6, name: '系统管理员', mobile_number: '13800000006', role: 'admin' },
]

// ──────────────────────────────────────────────
//  Dashboard 聚合
// ──────────────────────────────────────────────
export const mockDashboard: DashboardSummary = (() => {
  const store1Res = mockReservations.filter(r => r.store_id === 1 && r.reservation_date === today)
  const store1Paid = mockOrders.filter(o => o.store_id === 1 && o.payment_status === 'paid')
  return {
    today_reservations: store1Res.length,
    seated_count:       store1Res.filter(r => r.status === 'seated').length,
    dining_count:       store1Res.filter(r => r.status === 'dining').length,
    finished_count:     store1Res.filter(r => r.status === 'finished').length,
    unique_customers:   new Set(store1Res.map(r => r.user_id)).size,
    revenue_cents:      store1Paid.reduce((s, o) => s + o.total_cents, 0),
    turnover_rate:      2.1,
    repeat_rate:        0.38,
    alerts:             mockAlerts,
  }
})()

// ──────────────────────────────────────────────
//  按 store_id 过滤的便捷函数
// ──────────────────────────────────────────────
export function tablesForStore(storeId: number): DiningTable[] {
  return mockTables.filter(t => t.store_id === storeId)
}

export function reservationsForStore(storeId: number, date?: string): Reservation[] {
  return mockReservations.filter(r =>
    r.store_id === storeId && (!date || r.reservation_date === date)
  )
}

export function ordersForStore(storeId: number): Order[] {
  return mockOrders.filter(o => o.store_id === storeId)
}

export function catsForStore(storeId: number): Cat[] {
  return mockCats.filter(c => c.store_id === storeId)
}

// ──────────────────────────────────────────────
//  Mock API 拦截器
// ──────────────────────────────────────────────
export function handleMockRequest(method: string, url: string, params?: Record<string, unknown>): unknown {
  const storeId = Number(params?.storeId) || 1
  const date = (params?.date as string) || today

  if (method === 'get' && url === '/tables') return tablesForStore(storeId)
  if (method === 'get' && url === '/reservations') return reservationsForStore(storeId, date)
  if (method === 'get' && url === '/orders') return ordersForStore(storeId)
  if (method === 'get' && url === '/cats') return catsForStore(storeId)
  if (method === 'get' && url === '/menu-items') return mockMenuItems.filter(m => m.store_id === storeId)
  if (method === 'get' && url === '/stores') return mockStores
  if (method === 'get' && url === '/dashboard/summary') return mockDashboard
  if (method === 'get' && url === '/reviews') return mockReviews.filter(r => r.store_id === storeId)
  if (method === 'get' && url === '/cat-health-records') return mockCatHealthRecords.filter(r => r.cat_id === Number(params?.catId))
  if (method === 'get' && url === '/audit-logs') return mockAuditLogs
  if (method === 'get' && url === '/coupons') return mockCoupons
  if (method === 'get' && url === '/users') return mockStaffList.filter(u => {
    const roleFilter = params?.role as string
    if (!roleFilter) return true
    return roleFilter.split(',').includes(u.role)
  })
  if (method === 'get' && url === '/recommendations') {
    return {
      cat: mockCats.find(c => c.store_id === storeId) || null,
      tables: tablesForStore(storeId).filter(t => t.status === 'free').slice(0, 3),
      menuItems: mockMenuItems.filter(m => m.store_id === storeId).slice(0, 3),
    }
  }

  // PATCH /reservations/:id/status
  if (method === 'patch' && url.startsWith('/reservations/')) {
    const id = Number(url.split('/')[2])
    const r = mockReservations.find(r => r.id === id)
    if (r && params?.status) {
      r.status = params.status as Reservation['status']
      if (params.status === 'seated') {
        const t = mockTables.find(t => t.id === r.table_id)
        if (t) t.status = 'occupied'
      } else if (params.status === 'dining') {
        const t = mockTables.find(t => t.id === r.table_id)
        if (t) t.status = 'dining'
      } else if (params.status === 'finished' || params.status === 'cancelled') {
        const t = mockTables.find(t => t.id === r.table_id)
        if (t) { t.status = 'cleaning'; t.current_reservation_id = undefined }
      }
    }
    return { success: true }
  }

  // PATCH /orders/:id/status
  if (method === 'patch' && url.startsWith('/orders/')) {
    const id = Number(url.split('/')[2])
    const o = mockOrders.find(o => o.id === id)
    if (o && params?.status) {
      o.status = params.status as Order['status']
      if (params.status === 'served') { o.items?.forEach(i => i.served = true); o.served_at = new Date().toISOString() }
      if (params.status === 'paid') { o.payment_status = 'paid'; o.paid_at = new Date().toISOString() }
    }
    return { success: true }
  }

  // PATCH /tables/:id/status
  if (method === 'patch' && url.startsWith('/tables/')) {
    const id = Number(url.split('/')[2])
    const t = mockTables.find(t => t.id === id)
    if (t && params?.status) {
      t.status = params.status as TableStatus
      if (params.status === 'free') t.current_reservation_id = undefined
    }
    return { success: true }
  }

  // POST /auth/sms/send (验证码发送 - mock 直接成功)
  if (method === 'post' && url === '/auth/sms/send') {
    return { success: true, message: '验证码已发送' }
  }

  // POST /auth/login
  if (method === 'post' && url === '/auth/login') {
    const mobile = String(params?.mobileNumber || params?.data?.mobileNumber || '')
    const mockUsers: Record<string, { id: number; name: string; role: string; mobile_number: string; member_level: string; points: number }> = {
      '13800000001': { id: 1, name: '林小满',     role: 'customer',   mobile_number: '13800000001', member_level: 'gold',   points: 1280 },
      '13800000002': { id: 2, name: '高店员',     role: 'staff',      mobile_number: '13800000002', member_level: 'silver', points: 0 },
      '13800000003': { id: 3, name: '葛店长',     role: 'manager',    mobile_number: '13800000003', member_level: 'silver', points: 0 },
      '13800000004': { id: 4, name: '总部运营',   role: 'operator',   mobile_number: '13800000004', member_level: 'silver', points: 0 },
      '13800000005': { id: 5, name: '卢猫咪管家', role: 'cat_keeper', mobile_number: '13800000005', member_level: 'silver', points: 0 },
      '13800000006': { id: 6, name: '系统管理员', role: 'admin',      mobile_number: '13800000006', member_level: 'silver', points: 0 },
    }
    const user = mockUsers[mobile]
    if (user) return { user, token: `mock-token-${user.id}-${user.role}` }
    return { error: { message: '手机号未注册' } }
  }

  console.warn(`[Mock] 未匹配的请求: ${method.toUpperCase()} ${url}`)
  return []
}
