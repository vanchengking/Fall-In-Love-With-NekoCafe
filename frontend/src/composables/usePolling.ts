import { onMounted, onBeforeUnmount, ref } from 'vue'

export function usePolling(fn: () => Promise<void>, intervalMs = 10000) {
  const timer = ref<ReturnType<typeof setInterval> | null>(null)
  const lastUpdated = ref<Date | null>(null)
  const newCount = ref(0)
  const prevSignature = ref('')

  async function refresh() {
    await fn()
    lastUpdated.value = new Date()
  }

  function start() {
    stop()
    refresh()
    timer.value = setInterval(refresh, intervalMs)
  }

  /** 带新数据检测的轮询：getSignature 返回数据签名（如 id 列表） */
  function startWithChangeDetection(getSignature: () => string) {
    stop()
    const tick = async () => {
      const before = prevSignature.value
      await fn()
      lastUpdated.value = new Date()
      const after = getSignature()
      if (before && after !== before) {
        const beforeIds = new Set(before.split(','))
        const added = after.split(',').filter(id => id && !beforeIds.has(id))
        if (added.length > 0) newCount.value += added.length
      }
      prevSignature.value = after
    }
    tick()
    timer.value = setInterval(tick, intervalMs)
  }

  function stop() {
    if (timer.value) {
      clearInterval(timer.value)
      timer.value = null
    }
  }

  function clearNewCount() { newCount.value = 0 }

  onMounted(start)
  onBeforeUnmount(stop)

  return { refresh, lastUpdated, stop, start, startWithChangeDetection, newCount, clearNewCount }
}
