import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '@/utils/http'
import type { MenuItem, SelectedOrderItem, OrderItemRequest } from '@/types'

export const useCartStore = defineStore('cart', () => {
  const selectedMenu = ref<Record<number, number>>({})
  const menuItems = ref<MenuItem[]>([])
  const reservationId = ref<number | null>(null)

  const selectedOrderItems = computed<SelectedOrderItem[]>(() =>
    menuItems.value
      .filter((item) => (selectedMenu.value[item.id] || 0) > 0)
      .map((item) => ({
        menuItemId: item.id,
        quantity: selectedMenu.value[item.id],
        name: item.name,
        subtotal: selectedMenu.value[item.id] * item.price_cents,
      })),
  )

  const orderTotal = computed(() =>
    selectedOrderItems.value.reduce((sum, item) => sum + item.subtotal, 0),
  )

  // 原价合计（未打折，与 orderTotal 相同，供前端价格对比使用）
  const orderOriginalTotal = computed(() =>
    selectedOrderItems.value.reduce((sum, item) => sum + item.subtotal, 0),
  )

  const orderCount = computed(() =>
    selectedOrderItems.value.reduce((sum, item) => sum + item.quantity, 0),
  )

  function setQuantity(menuItemId: number, quantity: number) {
    selectedMenu.value[menuItemId] = Math.max(0, Math.min(9, quantity))
  }

  function clearCart() {
    selectedMenu.value = {}
  }

  function setReservationId(id: number | null) {
    reservationId.value = id
  }

  async function createOrder() {
    if (!reservationId.value || selectedOrderItems.value.length === 0) {
      throw new Error('请选择预约和菜品')
    }
    const items: OrderItemRequest[] = selectedOrderItems.value.map((i) => ({
      menuItemId: i.menuItemId,
      quantity: i.quantity,
    }))
    const order = await api.post('/orders', { reservationId: reservationId.value, items })
    clearCart()
    return order
  }

  return {
    selectedMenu,
    menuItems,
    reservationId,
    selectedOrderItems,
    orderTotal,
    orderCount,
    setQuantity,
    clearCart,
    setReservationId,
    createOrder,
  }
})
