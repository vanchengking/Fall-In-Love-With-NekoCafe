export function cents(value: number | null | undefined): string {
  return `¥${(Number(value || 0) / 100).toFixed(2)}`
}

export function tomorrow(): string {
  const date = new Date()
  date.setDate(date.getDate() + 1)
  return date.toISOString().slice(0, 10)
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
