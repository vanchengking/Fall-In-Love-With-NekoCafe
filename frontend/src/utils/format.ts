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
