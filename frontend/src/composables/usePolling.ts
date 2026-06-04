import { onMounted, onBeforeUnmount, ref } from 'vue'

export function usePolling(fn: () => Promise<void>, intervalMs = 10000) {
  const timer = ref<ReturnType<typeof setInterval> | null>(null)
  const lastUpdated = ref<Date | null>(null)

  async function refresh() {
    await fn()
    lastUpdated.value = new Date()
  }

  function start() {
    stop()
    refresh()
    timer.value = setInterval(refresh, intervalMs)
  }

  function stop() {
    if (timer.value) {
      clearInterval(timer.value)
      timer.value = null
    }
  }

  onMounted(start)
  onBeforeUnmount(stop)

  return { refresh, lastUpdated, stop, start }
}
