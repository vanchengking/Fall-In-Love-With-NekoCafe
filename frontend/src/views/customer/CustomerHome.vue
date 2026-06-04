<template>
  <div>
    <section class="hero">
      <h1>欢迎来到 NekoCafe</h1>
      <p>与猫咪共度美好时光，预约你的专属座位</p>
    </section>

    <div class="quick-grid">
      <router-link to="/reservation" class="quick-card">
        <div class="icon-circle" style="background: #e8f6f1; color: #0f766e"><CalendarDays :size="28" /></div>
        <h3>预约桌位</h3>
        <p>选择日期时段和空闲桌位</p>
      </router-link>
      <router-link to="/stores" class="quick-card">
        <div class="icon-circle" style="background: #fef3e2; color: #e86f51"><Cat :size="28" /></div>
        <h3>浏览猫咪</h3>
        <p>查看店内猫咪档案和性格</p>
      </router-link>
      <router-link to="/order" class="quick-card">
        <div class="icon-circle" style="background: #ede9fe; color: #7c3aed"><ShoppingCart :size="28" /></div>
        <h3>在线点单</h3>
        <p>饮品甜品一键下单</p>
      </router-link>
      <router-link to="/profile" class="quick-card">
        <div class="icon-circle" style="background: #e0f2fe; color: #2563eb"><UserCircle :size="28" /></div>
        <h3>我的预约</h3>
        <p>查看预约记录和个人信息</p>
      </router-link>
      <router-link to="/reviews" class="quick-card">
        <div class="icon-circle" style="background: #fef9c3; color: #ca8a04"><Star :size="28" /></div>
        <h3>评价</h3>
        <p>查看和提交用餐评价</p>
      </router-link>
    </div>

    <section v-if="recommendations.cat" class="rec-section">
      <h2>为你推荐</h2>
      <div class="rec-cat-card">
        <img v-if="recommendations.cat.photo_url" :src="recommendations.cat.photo_url" class="rec-cat-photo" />
        <div v-else class="rec-cat-icon"><Cat :size="32" /></div>
        <div>
          <h3>{{ recommendations.cat.name }}</h3>
          <p>{{ recommendations.cat.breed }} · {{ recommendations.cat.personality_tags?.join(' / ') }}</p>
        </div>
      </div>
    </section>

    <section v-if="menuItems.length" class="rec-section">
      <h2>人气菜品</h2>
      <div class="menu-grid">
        <div v-for="item in menuItems.slice(0, 4)" :key="item.id" class="menu-card">
          <img v-if="item.photo_url" :src="item.photo_url" class="menu-photo" />
          <h4>{{ item.name }}</h4>
          <p class="menu-meta">{{ item.category }} · {{ item.tags?.join(' / ') }}</p>
          <span class="menu-price">{{ cents(item.price_cents) }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { CalendarDays, Cat, ShoppingCart, UserCircle, Star } from '@lucide/vue'
import { api } from '@/utils/http'
import { cents } from '@/utils/format'
import { fallbackMenuItems, fallbackCats } from '@/utils/fallback'
import type { MenuItem, Recommendations as RecType } from '@/types'

const menuItems = ref<MenuItem[]>([])
const recommendations = ref<RecType>({ cat: null, tables: [], menuItems: [] })

onMounted(async () => {
  try {
    const [m, rec] = await Promise.all([
      api.get<MenuItem[]>('/menu-items', { storeId: 1 }),
      api.get<RecType>('/recommendations', { userId: 1, storeId: 1, preferences: 'quiet,coffee' }),
    ])
    menuItems.value = m
    recommendations.value = rec
  } catch {
    menuItems.value = fallbackMenuItems
    recommendations.value = { cat: fallbackCats[0] || null, tables: [], menuItems: fallbackMenuItems }
  }
})
</script>

<style scoped>
.hero {
  text-align: center;
  padding: 48px 20px 32px;
  background: linear-gradient(135deg, #e8f6f1, #fef3e2);
  border-radius: 16px;
  margin-bottom: 32px;
}
.hero h1 { font-size: 28px; color: #172033; margin-bottom: 8px; }
.hero p { color: #667085; font-size: 16px; }
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 32px;
}
.quick-card {
  padding: 24px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e8e5df;
  text-decoration: none;
  color: #172033;
  transition: box-shadow 0.2s, transform 0.2s;
}
.quick-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,0.08); transform: translateY(-2px); }
.icon-circle {
  width: 52px; height: 52px; border-radius: 12px;
  display: grid; place-items: center; margin-bottom: 12px;
}
.quick-card h3 { font-size: 16px; margin-bottom: 4px; }
.quick-card p { font-size: 13px; color: #667085; }
.rec-section { margin-bottom: 32px; }
.rec-section h2 { font-size: 20px; margin-bottom: 16px; }
.rec-cat-card {
  display: flex; align-items: center; gap: 16px;
  padding: 20px; background: #e8f6f1; border-radius: 12px;
}
.rec-cat-photo { width: 64px; height: 64px; object-fit: cover; border-radius: 12px; flex-shrink: 0; }
.rec-cat-icon { color: #0f766e; }
.rec-cat-card h3 { font-size: 18px; margin-bottom: 4px; }
.rec-cat-card p { color: #667085; font-size: 14px; }
.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
}
.menu-card {
  padding: 20px; background: #fff; border-radius: 12px;
  border: 1px solid #e8e5df; overflow: hidden;
}
.menu-photo { width: 100%; height: 120px; object-fit: cover; border-radius: 8px; margin-bottom: 10px; }
.menu-card h4 { font-size: 16px; margin-bottom: 6px; }
.menu-meta { font-size: 13px; color: #667085; margin-bottom: 8px; }
.menu-price { font-size: 18px; font-weight: 700; color: #e86f51; }
</style>
