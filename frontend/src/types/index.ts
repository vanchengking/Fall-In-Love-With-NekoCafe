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
  score?: number
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
  token: string
  user: AuthUser
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
