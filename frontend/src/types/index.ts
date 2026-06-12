export interface Store {
  id: number
  name: string
  city: string
  address: string
  phone: string
  table_count?: number
  total_seats?: number
  open_time?: string
  close_time?: string
  business_hours_text?: string
  latitude?: number | string
  longitude?: number | string
  photo_url?: string | null
  equipment_desc?: string | null
  area_detail?: string | null
}

export interface DiningTable {
  id: number
  code: string
  store_id: number
  seats: number
  area: string
  cat_zone: boolean
  status?: string
  available_for_slot?: boolean
  photo_url?: string | null
  area_detail?: string | null
  device_note?: string | null
  score?: number
}

/** FR-TABLE-003 桌位实时状态：后端按查询的门店/日期/时间派生，空闲/已预约/使用中/停用 */
export type TableRuntimeStatus = 'free' | 'reserved' | 'in_use' | 'disabled'

/** /api/admin/tables 返回的店员桌位状态行（DiningTable + 实时状态与当前预约只读字段） */
export interface StaffTableStatus extends DiningTable {
  runtime_status?: TableRuntimeStatus
  runtime_status_label?: string
  current_reservation_id?: number | null
  current_reservation_status?: ReservationStatus | null
  current_reservation_status_label?: string | null
  customer_name?: string | null
  mobile_number?: string | null
  reservation_time?: string | null
  party_size?: number | null
}

export interface Cat {
  id: number
  store_id?: number
  name: string
  breed: string
  personality_tags: string[]
  health_status: string
  weight_kg: string
  last_vaccine_at?: string
  photo_url?: string
  score?: number
}

export interface MenuItem {
  id: number
  store_id?: number
  name: string
  category: string
  price_cents: number
  tags: string[]
  status?: string
  photo_url?: string
  score?: number
}

export type ReservationStatus = 'created' | 'booked' | 'seated' | 'dining' | 'finished' | 'cancelled' | 'no_show'

export interface Reservation {
  id: number
  user_id: number
  store_id: number
  table_id: number | null
  recommended_cat_id: number | null
  reservation_date: string
  reservation_time: string
  party_size: number
  status: ReservationStatus
  status_label?: string
  note: string
  customer_name?: string
  customerName?: string
  table_code?: string
  store_name?: string
  cat_name?: string
  created_at?: string
}

export interface Order {
  id: number
  reservation_id: number
  user_id: number
  store_id: number
  status: string
  payment_status: string
  total_cents: number
  reservation_date?: string
  reservation_time?: string
  customer_name?: string
  created_at?: string
}

export interface CatHealthRecord {
  id: number
  cat_id: number
  weight_kg: number
  vaccine_note: string
  interaction_note: string
  recorded_at?: string
}

export interface OperationAlert {
  id: number
  store_id?: number
  level: string
  title: string
  detail: string
  resolved?: boolean
}

export interface DashboardSummary {
  today_reservations: number
  seated_count: number
  dining_count?: number
  finished_count: number
  unique_customers: number
  revenue_cents: number
  turnover_rate: number
  repeat_rate: number
  alerts: OperationAlert[]
}

export interface Recommendations {
  preferences?: string[]
  cat: Cat | null
  tables: DiningTable[]
  menuItems: MenuItem[]
}

export interface AuthUser {
  id: number
  name: string
  mobile_number?: string
  mobileNumber?: string
  role: string
  member_level?: string
  memberLevel?: string
  points?: number
  preferences?: string[]
}

export interface AuthResult {
  access_token: string
  token_type?: string
  expires_in?: number
  /** 兼容旧响应：等同 access_token，优先读取 access_token */
  token?: string
  user: AuthUser
}

export interface PointTransaction {
  id: number
  user_id: number
  delta: number
  balance_after: number
  source_type: string
  source_id?: number | null
  reason?: string | null
  created_at?: string
}

export interface ReservationForm {
  customerName: string
  mobileNumber: string
  storeId: number
  reservationDate: string
  reservationTime: string
  partySize: number
  tableId: string | number
  recommendedCatId: string | number
  preferences: string[]
  note: string
}

export interface HealthForm {
  catId: number
  weightKg: number
  vaccineNote: string
  interactionNote: string
}

export interface OrderItemRequest {
  menuItemId: number
  quantity: number
}

export interface SelectedOrderItem extends OrderItemRequest {
  name: string
  subtotal: number
}

export interface Review {
  id: number
  user_id: number
  store_id: number
  order_id?: number | null
  reservation_id?: number | null
  cat_id?: number | null
  rating: number
  food_rating?: number | null
  service_rating?: number | null
  environment_rating?: number | null
  cat_rating?: number | null
  content: string | null
  is_anonymous?: boolean
  reply?: string | null
  replied_at?: string | null
  created_at?: string
}
