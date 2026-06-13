export function cents(value: number | null | undefined): string {
  return `¥${(Number(value || 0) / 100).toFixed(2)}`
}

export function tomorrow(): string {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
}

/** 桌位区域 canonical 值（window/main/party/quiet）对应的中文展示标签；内部值保持英文以兼容选桌逻辑 */
export function areaLabel(area: string | undefined): string {
  const map: Record<string, string> = {
    window: '靠窗区',
    main: '主厅',
    party: '派对区',
    quiet: '安静区',
  }
  return map[area || ''] || area || '主厅'
}

/** 门店营业时间展示：优先用营业时间文本，否则拼接 open/close */
export function businessHours(store: { business_hours_text?: string; open_time?: string; close_time?: string }): string {
  if (store.business_hours_text) return store.business_hours_text
  if (store.open_time && store.close_time) return `${store.open_time} - ${store.close_time}`
  return '营业时间以门店公告为准'
}

export function statusLabel(status: string): string {
  const map: Record<string, string> = {
    created: '待确认',
    booked: '已预约',
    seated: '已入座',
    dining: '用餐中',
    finished: '已完成',
    cancelled: '已取消',
    no_show: '未到店',
  }
  return map[status] || status
}

export function statusType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    created: 'warning',
    booked: 'info',
    seated: '',
    dining: 'warning',
    finished: 'success',
    cancelled: 'danger',
    no_show: 'danger',
  }
  return map[status] || 'info'
}

export function orderStatusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: '待出餐',
    preparing: '备餐中',
    served: '已出餐',
    paid: '已支付',
    cancelled: '已取消',
    refunded: '已退款',
    exception: '异常',
  }
  return map[status] || status
}

export function orderStatusType(status: string): '' | 'success' | 'warning' | 'info' | 'danger' {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    pending: 'warning',
    preparing: '',
    served: 'info',
    paid: 'success',
    cancelled: 'danger',
    refunded: 'danger',
    exception: 'danger',
  }
  return map[status] || 'info'
}

const tagMap: Record<string, string> = {
  quiet: '安静', gentle: '温柔', active: '活泼', playful: '爱玩',
  curious: '好奇', interactive: '亲人', friendly: '友好', photo: '爱拍照',
  ragdoll: '布偶', shy: '怕生', lazy: '慵懒',
  coffee: '咖啡', signature: '招牌', salmon: '三文鱼',
  sweet: '甜品', cheese: '芝士', vegan: '纯素', oat: '燕麦',
  blueberry: '蓝莓',
  drink: '饮品', meal: '主食', dessert: '甜品', snack: '小食',
  window: '窗边', main: '大厅', party: '包厢', corner: '角落',
  healthy: '健康', observe: '观察中', sick: '生病', resting: '休息中',
  rest: '静养',
}

export function tagLabel(tag: string): string {
  return tagMap[tag] || tag
}

export function tagLabels(tags: string[] | undefined): string {
  if (!tags || tags.length === 0) return ''
  return tags.map(t => tagLabel(t)).join(' / ')
}

const avatarColors = ['#e8f6f1', '#fef3e2', '#ede9fe', '#e0f2fe', '#fce7f3']

export function avatarBg(name: string | undefined): string {
  let h = 0
  for (const c of (name || '')) h = c.charCodeAt(0) + ((h << 5) - h)
  return avatarColors[Math.abs(h) % avatarColors.length]
}
