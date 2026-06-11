import { createRouter, createWebHistory } from 'vue-router'

const roleDefaultRoute: Record<string, string> = {
  customer: '/',
  staff: '/staff',
  manager: '/admin/dashboard',
  operator: '/admin/dashboard',
  cat_keeper: '/admin/cats',
  admin: '/admin/dashboard',
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/Login.vue'), meta: { public: true } },
    { path: '/register', name: 'register', component: () => import('@/views/Register.vue'), meta: { public: true } },

    // 顾客路由
    {
      path: '/',
      component: () => import('@/layouts/CustomerLayout.vue'),
      children: [
        { path: '', name: 'customer-home', component: () => import('@/views/customer/CustomerHome.vue') },
        { path: 'stores', name: 'customer-stores', component: () => import('@/views/customer/CustomerStores.vue') },
        { path: 'stores/:id', name: 'customer-store-detail', component: () => import('@/views/customer/CustomerStoreDetail.vue') },
        { path: 'stores/:id/reviews', name: 'customer-store-reviews', component: () => import('@/views/customer/StoreReviews.vue'), meta: { public: true } },
        { path: 'reservation', name: 'customer-reservation', component: () => import('@/views/customer/CustomerReservation.vue') },
        { path: 'order', name: 'customer-order', component: () => import('@/views/customer/CustomerOrder.vue') },
        { path: 'payment', name: 'customer-payment', component: () => import('@/views/customer/CustomerPayment.vue') },
        { path: 'reviews', name: 'customer-reviews', component: () => import('@/views/customer/CustomerReviews.vue') },
        { path: 'profile', name: 'customer-profile', component: () => import('@/views/customer/CustomerProfile.vue') },
      ],
    },

    // 店员路由
    {
      path: '/staff',
      component: () => import('@/layouts/StaffLayout.vue'),
      meta: { roles: ['staff', 'manager', 'operator', 'admin'] },
      children: [
        { path: '', name: 'staff-today', component: () => import('@/views/staff/StaffToday.vue') },
        { path: 'reservations', name: 'staff-reservations', component: () => import('@/views/staff/StaffReservations.vue') },
        { path: 'orders', name: 'staff-orders', component: () => import('@/views/staff/StaffOrders.vue') },
        { path: 'tables', name: 'staff-tables', component: () => import('@/views/staff/StaffTables.vue') },
      ],
    },

    // 管理路由
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { roles: ['manager', 'operator', 'admin', 'cat_keeper'] },
      children: [
        { path: 'dashboard', name: 'admin-dashboard', component: () => import('@/views/admin/AdminDashboard.vue'), meta: { roles: ['manager', 'operator', 'admin'] } },
        { path: 'cats', name: 'admin-cats', component: () => import('@/views/admin/AdminCats.vue') },
        { path: 'stores', name: 'admin-stores', component: () => import('@/views/admin/AdminStores.vue'), meta: { roles: ['manager', 'operator', 'admin'] } },
        { path: 'staff', name: 'admin-staff', component: () => import('@/views/admin/AdminStaff.vue'), meta: { roles: ['manager', 'admin'] } },
        { path: 'campaigns', name: 'admin-campaigns', component: () => import('@/views/admin/AdminCampaigns.vue'), meta: { roles: ['operator', 'admin'] } },
      ],
    },

    { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('@/views/NotFound.vue') },
  ],
})

function getAuth(): { token: string; role: string } | null {
  try {
    const raw = localStorage.getItem('neko-auth')
    if (!raw) return null
    const parsed = JSON.parse(raw)
    if (!parsed.token || !parsed.user?.role) return null
    return { token: parsed.token, role: parsed.user.role }
  } catch {
    return null
  }
}

router.beforeEach((to) => {
  if (to.path === '/login') return
  if (to.meta?.public) return

  const auth = getAuth()
  if (!auth) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 检查父路由的 roles 元信息
  const matched = to.matched
  for (const record of matched) {
    if (record.meta?.roles) {
      const allowed = record.meta.roles as string[]
      if (!allowed.includes(auth.role)) {
        return roleDefaultRoute[auth.role] || '/'
      }
    }
  }
})

export default router
export { roleDefaultRoute }
