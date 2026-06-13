<template>
  <div class="page-enter-active">
    <!-- ═══ 顶部 Hero：模糊实景背景 + 个性化欢迎语 ═══ -->
    <section class="hero">
      <div class="hero-bg">
        <img src="https://images.unsplash.com/photo-1559925393-8be0ec4767c8?w=1200&h=500&fit=crop" alt="" class="hero-bg-img" />
        <div class="hero-overlay"></div>
      </div>
      <div class="hero-content">
        <span class="hero-greeting">{{ greeting }}，{{ userName }}</span>
        <h1>欢迎来到 NekoCafe</h1>
        <p class="hero-sub">与猫咪共度美好时光，预约你的专属座位</p>
        <div class="hero-stats">
          <div class="hero-stat">
            <span class="hero-stat-num">3</span>
            <span class="hero-stat-label">家门店</span>
          </div>
          <span class="hero-divider"></span>
          <div class="hero-stat">
            <span class="hero-stat-num">12</span>
            <span class="hero-stat-label">只猫咪</span>
          </div>
          <span class="hero-divider"></span>
          <div class="hero-stat">
            <span class="hero-stat-num">4.8</span>
            <span class="hero-stat-label">用户评分</span>
          </div>
        </div>
      </div>
      <!-- 猫爪装饰 -->
      <span class="paw p1">🐾</span>
      <span class="paw p2">🐾</span>
      <span class="paw p3">🐾</span>
    </section>

    <!-- ═══ 快捷入口 ═══ -->
    <section class="section">
      <div class="section-header">
        <h2>快捷入口</h2>
        <span class="section-line"></span>
      </div>
      <div class="quick-grid">
        <router-link to="/customer/reservation" class="quick-card" role="button" aria-label="预约桌位">
          <div class="quick-icon qi-reservation">
            <CalendarDays :size="28" />
          </div>
          <div class="quick-info">
            <h3>预约桌位</h3>
            <p>选择日期时段和空闲桌位</p>
          </div>
          <span class="quick-arrow">→</span>
          <span class="quick-badge hot">HOT</span>
        </router-link>
        <router-link to="/customer/stores" class="quick-card" role="button" aria-label="浏览门店">
          <div class="quick-icon qi-stores">
            <Cat :size="28" />
          </div>
          <div class="quick-info">
            <h3>浏览门店</h3>
            <p>查看猫咪档案和性格</p>
          </div>
          <span class="quick-arrow">→</span>
        </router-link>
        <router-link to="/customer/order" class="quick-card" role="button" aria-label="在线点单">
          <div class="quick-icon qi-order">
            <ShoppingCart :size="28" />
          </div>
          <div class="quick-info">
            <h3>在线点单</h3>
            <p>饮品甜品一键下单</p>
          </div>
          <span class="quick-arrow">→</span>
        </router-link>
        <router-link to="/customer/profile" class="quick-card" role="button" aria-label="个人中心">
          <div class="quick-icon qi-profile">
            <UserCircle :size="28" />
          </div>
          <div class="quick-info">
            <h3>个人中心</h3>
            <p>预约记录和会员信息</p>
          </div>
          <span class="quick-arrow">→</span>
        </router-link>
      </div>
    </section>

    <!-- ═══ 推荐猫咪：横向滚动 ═══ -->
    <section class="section">
      <div class="section-header">
        <h2>猫咪档案</h2>
        <router-link to="/customer/stores" class="section-more">查看全部 →</router-link>
      </div>
      <!-- 骨架屏 -->
      <div v-if="loading" class="cat-scroll">
        <div v-for="i in 4" :key="i" class="cat-scroll-card skeleton-card">
          <div class="skeleton" style="height: 160px; border-radius: var(--radius-lg) var(--radius-lg) 0 0"></div>
          <div style="padding: var(--space-base)">
            <div class="skeleton" style="height: 18px; width: 50%; margin-bottom: var(--space-xs)"></div>
            <div class="skeleton" style="height: 13px; width: 70%; margin-bottom: var(--space-sm)"></div>
            <div class="skeleton" style="height: 20px; width: 60%"></div>
          </div>
        </div>
      </div>
      <!-- 真实内容 -->
      <div v-else class="cat-scroll">
        <div v-for="cat in allCats" :key="cat.id" class="cat-scroll-card" role="button" tabindex="0" :aria-label="`查看猫咪 ${cat.name}`">
          <div class="cat-photo-wrap">
            <img :src="cat.photo_url || `https://placekitten.com/300/200`" :alt="cat.name" class="cat-scroll-photo" />
            <span class="cat-likes">
              <Heart :size="12" /> {{ 28 + cat.id * 13 }}
            </span>
          </div>
          <div class="cat-scroll-body">
            <h4>{{ cat.name }}</h4>
            <p class="cat-breed">{{ cat.breed }}</p>
            <div class="cat-tags">
              <span v-for="tag in cat.personality_tags" :key="tag" class="cat-tag">{{ tagLabel(tag) }}</span>
            </div>
            <div class="cat-meta">
              <span class="cat-bookings">
                <CalendarDays :size="12" /> 已被预约 {{ 15 + cat.id * 8 }} 次
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ 推荐菜品：两列网格 ═══ -->
    <section class="section">
      <div class="section-header">
        <h2>人气菜品</h2>
        <router-link to="/customer/order" class="section-more">查看菜单 →</router-link>
      </div>
      <!-- 骨架屏 -->
      <div v-if="loading" class="menu-grid">
        <div v-for="i in 4" :key="i" class="menu-card skeleton-card">
          <div class="skeleton" style="height: 140px"></div>
          <div style="padding: var(--space-base)">
            <div class="skeleton" style="height: 18px; width: 60%; margin-bottom: var(--space-xs)"></div>
            <div class="skeleton" style="height: 13px; width: 40%; margin-bottom: var(--space-sm)"></div>
            <div class="skeleton" style="height: 20px; width: 30%"></div>
          </div>
        </div>
      </div>
      <div v-else-if="!menuItems.length">
        <el-empty description="暂无菜品数据" />
      </div>
      <div v-else class="menu-grid">
        <div v-for="(item, idx) in menuItems.slice(0, 6)" :key="item.id" class="menu-card" role="button" tabindex="0" :aria-label="`查看菜品 ${item.name}`">
          <div class="menu-photo-wrap">
            <img :src="item.photo_url || MENU_PHOTOS[idx % MENU_PHOTOS.length]" :alt="item.name" class="menu-photo" />
            <span v-if="idx === 0" class="menu-badge hot">TOP1</span>
            <span v-else-if="idx === 1" class="menu-badge new">新品</span>
          </div>
          <div class="menu-body">
            <h4>{{ item.name }}</h4>
            <p class="menu-desc">{{ filteredMenuTags(item) }}</p>
            <div class="menu-footer">
              <span class="menu-price">{{ cents(item.price_cents) }}</span>
              <span class="menu-likes">
                <span class="like-icon">♥</span> {{ 86 + idx * 23 }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- ═══ 底部：顾客评价滚动条 ═══ -->
    <section class="section">
      <div class="section-header">
        <h2>顾客好评</h2>
        <router-link to="/customer/reviews" class="section-more">全部评价 →</router-link>
      </div>
      <div class="review-ticker-wrap">
        <div class="review-ticker">
          <div v-for="(r, idx) in reviewLoop" :key="idx" class="review-tick-card">
            <div class="tick-header">
              <div class="tick-avatar" :style="{ background: avatarBg(idx) }">{{ r.name[0] }}</div>
              <div>
                <span class="tick-name">{{ r.name }}</span>
                <div class="tick-stars">
                  <span v-for="s in 5" :key="s" :class="s <= r.rating ? 'star-on' : 'star-off'">★</span>
                </div>
              </div>
            </div>
            <p class="tick-text">{{ r.text }}</p>
            <span class="tick-date">{{ r.date }}</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { CalendarDays, Cat, ShoppingCart, UserCircle, Heart } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { api } from '@/utils/http'
import { cents, tagLabel } from '@/utils/format'
import { fallbackMenuItems, fallbackCats } from '@/utils/fallback'
import type { MenuItem, Recommendations as RecType, Cat as CatType } from '@/types'

const MENU_PHOTOS = [
  'https://images.unsplash.com/photo-1572442388796-11668a67e53d?w=400&h=300&fit=crop',
  'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=400&h=300&fit=crop',
  'https://images.unsplash.com/photo-1565958011703-44f9829ba187?w=400&h=300&fit=crop',
  'https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=400&h=300&fit=crop',
  'https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=400&h=300&fit=crop',
  'https://images.unsplash.com/photo-1504674900247-0877df9cc836?w=400&h=300&fit=crop',
]

const auth = useAuthStore()
const menuItems = ref<MenuItem[]>([])
const allCats = ref<CatType[]>([])
const loading = ref(true)

// ── 个性化欢迎语 ──
const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 11) return '早上好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})
const userName = computed(() => auth.user?.name || auth.user?.mobileNumber || '猫友')

// ── 顾客评价 mock ──
const reviews = [
  { name: '林小雨', rating: 5, text: '环境非常好，猫咪很亲人，拿铁拉花太可爱了！下次还来。', date: '06-10' },
  { name: '张明远', rating: 4, text: '团子太萌了，一直在腿上睡觉。甜品也不错，就是等位有点久。', date: '06-09' },
  { name: '王思琪', rating: 5, text: '带小朋友来的，玩得特别开心。猫咪都很健康干净。', date: '06-08' },
  { name: '陈佳怡', rating: 5, text: '抹茶蛋糕绝了！猫咪摩卡特别活泼，拍照超配合。', date: '06-07' },
  { name: '李浩然', rating: 4, text: '环境安静适合办公，手冲咖啡水准在线。布丁猫太治愈了。', date: '06-06' },
  { name: '赵雨萱', rating: 5, text: '第三次来了，每次都有新猫咪。店员很专业，讲解猫咪性格。', date: '06-05' },
  { name: '周子墨', rating: 5, text: '猫爪拿铁必点！拉花是猫爪形状，舍不得喝。', date: '06-04' },
]
// 循环两次实现无缝滚动
const reviewLoop = computed(() => [...reviews, ...reviews])

function avatarBg(idx: number): string {
  const colors = ['#e8f6f1', '#fef3e2', '#ede9fe', '#e0f2fe', '#fce7f3', '#fef9c3', '#dbeafe']
  return colors[idx % colors.length]
}

function filteredMenuTags(item: MenuItem): string {
  if (!item.tags?.length) return ''
  const catLabel = tagLabel(item.category || '')
  const seen = new Set<string>()
  const result: string[] = []
  for (const tag of item.tags) {
    const label = tagLabel(tag)
    if (label !== catLabel && !seen.has(label)) { seen.add(label); result.push(label) }
  }
  return result.join(' / ')
}

onMounted(async () => {
  try {
    const [m, rec] = await Promise.all([
      api.get<MenuItem[]>('/menu-items', { storeId: 1 }),
      api.get<RecType>('/recommendations', { userId: auth.user?.id || 1, storeId: 1, preferences: 'quiet,coffee' }),
    ])
    menuItems.value = m
    allCats.value = rec.cat ? [rec.cat, ...fallbackCats.slice(1)] : fallbackCats
  } catch {
    menuItems.value = fallbackMenuItems
    allCats.value = fallbackCats
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
/* ══════════════════════════════════════════
   Hero 区域
   ══════════════════════════════════════════ */
.hero {
  position: relative;
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-xl);
  overflow: hidden;
  min-height: 260px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero-bg {
  position: absolute;
  inset: 0;
}
.hero-bg-img {
  width: 100%; height: 100%;
  object-fit: cover;
  filter: blur(4px) brightness(0.7);
  transform: scale(1.05);
}
.hero-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.55), rgba(232, 111, 81, 0.3));
}
.hero-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: var(--space-2xl) var(--space-lg);
  color: #fff;
}
.hero-greeting {
  display: inline-block;
  padding: var(--space-xs) var(--space-base);
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(8px);
  border-radius: 999px;
  font-size: var(--text-sm);
  margin-bottom: var(--space-sm);
  letter-spacing: 0.5px;
}
.hero-content h1 {
  font-size: var(--text-2xl);
  color: #fff;
  margin-bottom: var(--space-xs);
  text-shadow: 0 2px 8px rgba(0,0,0,0.2);
}
.hero-sub {
  font-size: var(--text-base);
  opacity: 0.9;
  margin-bottom: var(--space-lg);
}
.hero-stats {
  display: inline-flex;
  align-items: center;
  gap: var(--space-base);
  padding: var(--space-sm) var(--space-lg);
  background: rgba(255, 255, 255, 0.15);
  backdrop-filter: blur(12px);
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.2);
}
.hero-stat { text-align: center; }
.hero-stat-num { display: block; font-size: var(--text-xl); font-weight: 700; }
.hero-stat-label { font-size: var(--text-xs); opacity: 0.85; }
.hero-divider { width: 1px; height: 24px; background: rgba(255,255,255,0.3); }
.paw { position: absolute; z-index: 1; opacity: 0.1; pointer-events: none; }
.p1 { top: 16px; left: 24px; font-size: 48px; transform: rotate(-20deg); }
.p2 { bottom: 16px; right: 32px; font-size: 36px; transform: rotate(15deg); }
.p3 { top: 40%; right: 15%; font-size: 28px; transform: rotate(-10deg); }

/* ══════════════════════════════════════════
   Section 通用
   ══════════════════════════════════════════ */
.section { margin-bottom: var(--space-xl); }
.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-base);
  margin-bottom: var(--space-lg);
}
.section-header h2 { font-size: var(--text-xl); margin-bottom: 0; }
.section-line { flex: 1; height: 1px; background: linear-gradient(90deg, var(--line), transparent); }
.section-more {
  font-size: var(--text-sm);
  color: var(--teal);
  text-decoration: none;
  white-space: nowrap;
  transition: color var(--transition-fast);
}
.section-more:hover { color: var(--teal-dark); }

/* ══════════════════════════════════════════
   快捷入口
   ══════════════════════════════════════════ */
.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-base);
}
.quick-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--space-base);
  padding: var(--space-lg);
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: var(--shadow-md);
  text-decoration: none;
  color: var(--ink);
  transition: all var(--transition-fast);
  overflow: hidden;
}
.quick-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-3px);
  border-color: var(--teal-light);
}
.quick-card:hover .quick-icon { animation: icon-bounce 0.4s ease; }
.quick-icon {
  width: 56px; height: 56px;
  border-radius: var(--radius-md);
  display: grid; place-items: center;
  flex-shrink: 0;
  transition: transform var(--transition-fast);
}
.qi-reservation { background: linear-gradient(135deg, #e8f6f1, var(--teal-light)); color: var(--teal); }
.qi-stores { background: linear-gradient(135deg, #fef3e2, var(--coral-light)); color: var(--coral); }
.qi-order { background: linear-gradient(135deg, var(--purple-light), #ddd6fe); color: var(--purple); }
.qi-profile { background: linear-gradient(135deg, #e0f2fe, var(--blue-light)); color: var(--blue); }
.quick-info { flex: 1; min-width: 0; }
.quick-info h3 { font-size: var(--text-base); font-weight: 700; margin-bottom: 2px; }
.quick-info p { font-size: var(--text-sm); color: var(--muted); }
.quick-arrow {
  font-size: 18px;
  color: var(--muted);
  opacity: 0;
  transform: translateX(-4px);
  transition: all var(--transition-fast);
}
.quick-card:hover .quick-arrow { opacity: 1; transform: translateX(0); }
.quick-badge {
  position: absolute;
  top: var(--space-sm);
  right: var(--space-sm);
  padding: 2px var(--space-sm);
  font-size: 10px;
  font-weight: 700;
  border-radius: var(--radius-sm);
  color: #fff;
  letter-spacing: 0.5px;
}
.quick-badge.hot { background: linear-gradient(135deg, #ef4444, #f97316); }

@keyframes icon-bounce {
  0%   { transform: scale(1); }
  40%  { transform: scale(1.2); }
  70%  { transform: scale(0.95); }
  100% { transform: scale(1); }
}

/* ══════════════════════════════════════════
   猫咪横向滚动
   ══════════════════════════════════════════ */
.cat-scroll {
  display: flex;
  gap: var(--space-base);
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  -webkit-overflow-scrolling: touch;
  padding-bottom: var(--space-sm);
  scrollbar-width: thin;
  scrollbar-color: var(--line) transparent;
}
.cat-scroll::-webkit-scrollbar { height: 6px; }
.cat-scroll::-webkit-scrollbar-track { background: transparent; }
.cat-scroll::-webkit-scrollbar-thumb { background: var(--line); border-radius: 3px; }

.cat-scroll-card {
  flex: 0 0 220px;
  scroll-snap-align: start;
  background: var(--paper);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  overflow: hidden;
  transition: all var(--transition-fast);
}
.cat-scroll-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-3px);
  border-color: var(--teal-light);
}
.cat-photo-wrap { position: relative; }
.cat-scroll-photo {
  width: 100%;
  height: 160px;
  object-fit: cover;
  display: block;
}
.cat-likes {
  position: absolute;
  bottom: var(--space-sm);
  right: var(--space-sm);
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px var(--space-sm);
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(4px);
  border-radius: 999px;
  color: #fff;
  font-size: 11px;
}
.cat-scroll-body { padding: var(--space-base); }
.cat-scroll-body h4 { font-size: var(--text-base); margin-bottom: 2px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cat-breed { font-size: var(--text-xs); color: var(--muted); margin-bottom: var(--space-sm); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cat-tags { display: flex; flex-wrap: wrap; gap: var(--space-xs); margin-bottom: var(--space-sm); }
.cat-tag {
  display: inline-block;
  padding: 2px var(--space-sm);
  background: var(--teal-light);
  color: var(--teal-dark);
  font-size: 10px;
  font-weight: 600;
  border-radius: var(--radius-sm);
}
.cat-meta { font-size: var(--text-xs); color: var(--muted); display: flex; align-items: center; gap: 3px; }

/* ══════════════════════════════════════════
   菜品两列网格
   ══════════════════════════════════════════ */
.menu-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-base);
}
.menu-card {
  background: var(--paper);
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  overflow: hidden;
  transition: all var(--transition-fast);
}
.menu-card:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-3px);
  border-color: var(--teal-light);
}
.menu-photo-wrap { position: relative; }
.menu-photo { width: 100%; height: 140px; object-fit: cover; display: block; }
.menu-badge {
  position: absolute;
  top: var(--space-sm);
  left: var(--space-sm);
  padding: 2px var(--space-sm);
  border-radius: var(--radius-sm);
  font-size: 10px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.5px;
}
.menu-badge.hot { background: linear-gradient(135deg, #ef4444, #f97316); }
.menu-badge.new { background: linear-gradient(135deg, var(--purple), #a78bfa); }
.menu-body { padding: var(--space-base); }
.menu-body h4 { font-size: var(--text-base); margin-bottom: var(--space-xs); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.menu-desc { font-size: var(--text-sm); color: var(--muted); margin-bottom: var(--space-sm); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.menu-footer { display: flex; justify-content: space-between; align-items: baseline; }
.menu-price { font-size: var(--text-lg); font-weight: 700; color: var(--coral); }
.menu-likes { font-size: var(--text-xs); color: var(--muted); display: flex; align-items: center; gap: 3px; }
.like-icon { color: var(--danger); }

/* ══════════════════════════════════════════
   评价滚动条
   ══════════════════════════════════════════ */
.review-ticker-wrap {
  overflow: hidden;
  border-radius: var(--radius-lg);
  background: linear-gradient(135deg, var(--wash), #f0fdf4);
  padding: var(--space-base) 0;
}
.review-ticker {
  display: flex;
  gap: var(--space-base);
  animation: ticker-scroll 40s linear infinite;
  width: max-content;
}
.review-ticker:hover { animation-play-state: paused; }
.review-tick-card {
  flex: 0 0 280px;
  background: var(--paper);
  border-radius: var(--radius-md);
  padding: var(--space-base);
  box-shadow: var(--shadow-sm);
  border: 1px solid var(--line);
}
.tick-header { display: flex; align-items: center; gap: var(--space-sm); margin-bottom: var(--space-sm); }
.tick-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  display: grid; place-items: center;
  font-size: 14px; font-weight: 700;
  color: var(--ink);
  flex-shrink: 0;
}
.tick-name { font-size: var(--text-sm); font-weight: 600; }
.tick-stars { display: flex; gap: 1px; }
.star-on { color: var(--gold); font-size: 12px; }
.star-off { color: var(--line); font-size: 12px; }
.tick-text { font-size: var(--text-sm); color: var(--ink); line-height: 1.5; margin-bottom: var(--space-xs); }
.tick-date { font-size: var(--text-xs); color: var(--muted); }

@keyframes ticker-scroll {
  0%   { transform: translateX(0); }
  100% { transform: translateX(-50%); }
}

/* ── 骨架屏 ── */
.skeleton-card { pointer-events: none; }

/* ══════════════════════════════════════════
   响应式
   ══════════════════════════════════════════ */
@media (max-width: 768px) {
  .quick-grid { grid-template-columns: 1fr; }
  .menu-grid { grid-template-columns: 1fr; }
  .hero-stats { flex-wrap: wrap; justify-content: center; gap: var(--space-sm); }
  .hero-divider { display: none; }
}
@media (max-width: 640px) {
  .quick-grid { grid-template-columns: repeat(2, 1fr); }
  .quick-card { flex-direction: column; text-align: center; gap: var(--space-sm); padding: var(--space-base); }
  .quick-arrow { display: none; }
  .cat-scroll-card { flex: 0 0 180px; }
  .cat-scroll-photo { height: 130px; }
}
</style>
