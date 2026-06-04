<template>
  <div>
    <div v-if="recommendations.cat" class="recommendation-band">
      <Cat :size="24" />
      <div>
        <strong>{{ recommendations.cat.name }}</strong>
        <span class="desc">{{ recommendations.cat.breed }} · {{ recommendations.cat.personality_tags?.join(' / ') }}</span>
      </div>
    </div>

    <div class="table-grid">
      <article v-for="table in recommendations.tables" :key="table.id" class="item-card">
        <Armchair :size="20" />
        <strong>{{ table.code }}</strong>
        <span class="desc">{{ table.seats }} 人 · {{ table.area }}</span>
      </article>
    </div>

    <div class="menu-list">
      <article v-for="item in menuItems" :key="item.id" class="menu-row">
        <div>
          <strong>{{ item.name }}</strong>
          <span class="desc">{{ item.category }} · {{ item.tags?.join(' / ') }}</span>
        </div>
        <div class="quantity">
          <span>{{ cents(item.price_cents) }}</span>
          <el-input-number
            :model-value="selectedMenu[item.id] || 0"
            @update:model-value="$emit('update:quantity', item.id, $event)"
            :min="0"
            :max="9"
            size="small"
          />
        </div>
      </article>
    </div>

    <el-button
      type="primary"
      plain
      style="width: 100%; margin-top: 12px"
      @click="$emit('createOrder')"
    >
      <ShoppingCart :size="18" style="margin-right: 6px" />
      生成点单 {{ cents(orderTotal) }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { Cat, Armchair, ShoppingCart } from '@lucide/vue'
import { cents } from '@/utils/format'
import type { Recommendations as RecType, MenuItem } from '@/types'

defineProps<{
  recommendations: RecType
  menuItems: MenuItem[]
  selectedMenu: Record<number, number>
  orderTotal: number
}>()

defineEmits<{
  'update:quantity': [menuItemId: number, quantity: number]
  createOrder: []
}>()
</script>
