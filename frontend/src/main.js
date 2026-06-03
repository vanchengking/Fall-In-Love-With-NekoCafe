import { computed, createApp, onMounted, reactive, ref } from "vue";
import {
  Armchair,
  CalendarDays,
  Cat,
  CheckCircle2,
  ClipboardCheck,
  Coffee,
  HeartPulse,
  LayoutDashboard,
  RefreshCw,
  Search,
  ShoppingCart,
  Sparkles,
  Utensils,
  XCircle,
} from "@lucide/vue";
import "./styles.css";

const API_BASE = import.meta.env.VITE_API_BASE_URL || "/api";

const fallback = {
  stores: [
    {
      id: 1,
      name: "NekoCafe 五道口店",
      city: "北京",
      address: "海淀区成府路 99 号",
      phone: "010-88880001",
      table_count: 4,
      total_seats: 16,
    },
  ],
  tables: [
    { id: 1, code: "A01", store_id: 1, seats: 2, area: "window", cat_zone: true, available_for_slot: true },
    { id: 2, code: "A02", store_id: 1, seats: 4, area: "window", cat_zone: true, available_for_slot: true },
    { id: 3, code: "B01", store_id: 1, seats: 4, area: "main", cat_zone: false, available_for_slot: true },
  ],
  cats: [
    { id: 1, name: "团子", breed: "布偶", personality_tags: ["quiet", "gentle"], health_status: "healthy", weight_kg: "4.80" },
    { id: 2, name: "拿铁", breed: "英短", personality_tags: ["active", "photo"], health_status: "healthy", weight_kg: "5.20" },
  ],
  menuItems: [
    { id: 1, name: "猫爪拿铁", category: "drink", price_cents: 3200, tags: ["coffee", "signature"] },
    { id: 2, name: "三文鱼能量碗", category: "meal", price_cents: 5800, tags: ["healthy", "salmon"] },
    { id: 3, name: "毛线球芝士蛋糕", category: "dessert", price_cents: 3600, tags: ["sweet", "cheese"] },
  ],
  reservations: [],
  dashboard: {
    today_reservations: 2,
    seated_count: 1,
    finished_count: 3,
    unique_customers: 2,
    revenue_cents: 12600,
    turnover_rate: 1,
    repeat_rate: 0.42,
    alerts: [{ id: 1, level: "warning", title: "晚高峰桌位紧张", detail: "18:00-20:00 预约集中。" }],
  },
};

function tomorrow() {
  const date = new Date();
  date.setDate(date.getDate() + 1);
  return date.toISOString().slice(0, 10);
}

async function api(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {}),
    },
    ...options,
  });

  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(body.error?.message || `HTTP ${response.status}`);
  }
  return body.data;
}

function cents(value) {
  return `¥${(Number(value || 0) / 100).toFixed(2)}`;
}

const App = {
  components: {
    Armchair,
    CalendarDays,
    Cat,
    CheckCircle2,
    ClipboardCheck,
    Coffee,
    HeartPulse,
    LayoutDashboard,
    RefreshCw,
    Search,
    ShoppingCart,
    Sparkles,
    Utensils,
    XCircle,
  },
  setup() {
    const tabs = [
      { key: "customer", label: "预约", icon: CalendarDays },
      { key: "staff", label: "店员", icon: ClipboardCheck },
      { key: "cats", label: "猫咪", icon: Cat },
      { key: "ops", label: "看板", icon: LayoutDashboard },
    ];

    const activeTab = ref("customer");
    const loading = ref(false);
    const message = ref("");
    const stores = ref([]);
    const tables = ref([]);
    const cats = ref([]);
    const menuItems = ref([]);
    const reservations = ref([]);
    const orders = ref([]);
    const dashboard = ref(fallback.dashboard);
    const recommendations = ref({ cat: null, tables: [], menuItems: [] });
    const selectedMenu = reactive({});

    const reservationForm = reactive({
      customerName: "林小满",
      mobileNumber: "13800000001",
      storeId: 1,
      reservationDate: tomorrow(),
      reservationTime: "18:30",
      partySize: 2,
      tableId: "",
      recommendedCatId: "",
      preferences: ["quiet", "window"],
      note: "希望靠窗，喜欢安静猫咪",
    });

    const healthForm = reactive({
      catId: 1,
      weightKg: 4.8,
      vaccineNote: "疫苗记录正常",
      interactionNote: "今日互动温和",
    });

    const search = reactive({
      mobileNumber: "",
      date: "",
    });

    const preferenceOptions = ["quiet", "window", "sweet", "coffee", "photo", "healthy"];

    const currentStore = computed(() =>
      stores.value.find((store) => Number(store.id) === Number(reservationForm.storeId))
    );

    const activeReservations = computed(() =>
      reservations.value.filter((reservation) => ["booked", "seated"].includes(reservation.status))
    );

    const latestReservation = computed(() => activeReservations.value[0] || reservations.value[0]);

    const selectedOrderItems = computed(() =>
      menuItems.value
        .filter((item) => Number(selectedMenu[item.id] || 0) > 0)
        .map((item) => ({
          menuItemId: item.id,
          quantity: Number(selectedMenu[item.id]),
          name: item.name,
          subtotal: Number(selectedMenu[item.id]) * Number(item.price_cents),
        }))
    );

    const orderTotal = computed(() =>
      selectedOrderItems.value.reduce((sum, item) => sum + item.subtotal, 0)
    );

    function setFallbackData(error) {
      message.value = `后端未连接，当前显示本地演示数据：${error.message}`;
      if (stores.value.length === 0) stores.value = fallback.stores;
      if (tables.value.length === 0) tables.value = fallback.tables;
      if (cats.value.length === 0) cats.value = fallback.cats;
      if (menuItems.value.length === 0) menuItems.value = fallback.menuItems;
      if (reservations.value.length === 0) reservations.value = fallback.reservations;
      dashboard.value = fallback.dashboard;
    }

    async function loadBaseData() {
      loading.value = true;
      try {
        const [storeData, tableData, catData, menuData, reservationData, dashboardData] = await Promise.all([
          api("/stores"),
          api(`/tables?storeId=${reservationForm.storeId}`),
          api(`/cats?storeId=${reservationForm.storeId}`),
          api(`/menu-items?storeId=${reservationForm.storeId}`),
          api("/reservations"),
          api(`/dashboard/summary?storeId=${reservationForm.storeId}`),
        ]);
        stores.value = storeData;
        tables.value = tableData;
        cats.value = catData;
        menuItems.value = menuData;
        reservations.value = reservationData;
        dashboard.value = dashboardData;
        await refreshRecommendations();
        message.value = "";
      } catch (error) {
        setFallbackData(error);
      } finally {
        loading.value = false;
      }
    }

    async function refreshTables() {
      try {
        tables.value = await api(
          `/tables?storeId=${reservationForm.storeId}&date=${reservationForm.reservationDate}&time=${reservationForm.reservationTime}&partySize=${reservationForm.partySize}`
        );
      } catch (error) {
        setFallbackData(error);
      }
    }

    async function refreshReservations() {
      const params = new URLSearchParams();
      if (search.mobileNumber) params.set("mobileNumber", search.mobileNumber);
      if (search.date) params.set("date", search.date);
      if (reservationForm.storeId) params.set("storeId", reservationForm.storeId);

      try {
        reservations.value = await api(`/reservations?${params.toString()}`);
      } catch (error) {
        setFallbackData(error);
      }
    }

    async function refreshRecommendations() {
      const params = new URLSearchParams({
        userId: "1",
        storeId: String(reservationForm.storeId),
        preferences: reservationForm.preferences.join(","),
      });
      try {
        recommendations.value = await api(`/recommendations?${params.toString()}`);
        if (recommendations.value.cat) {
          reservationForm.recommendedCatId = recommendations.value.cat.id;
        }
      } catch (error) {
        recommendations.value = {
          cat: cats.value[0] || null,
          tables: tables.value.slice(0, 3),
          menuItems: menuItems.value.slice(0, 3),
        };
      }
    }

    async function createReservation() {
      loading.value = true;
      try {
        const created = await api("/reservations", {
          method: "POST",
          body: JSON.stringify({ data: reservationForm }),
        });
        reservations.value = [created, ...reservations.value];
        message.value = `预约已创建：${created.table_code || "自动分配桌位"}`;
        await refreshTables();
      } catch (error) {
        message.value = error.message;
      } finally {
        loading.value = false;
      }
    }

    async function updateReservationStatus(reservation, status) {
      try {
        const updated = await api(`/reservations/${reservation.id}/status`, {
          method: "PATCH",
          body: JSON.stringify({ data: { status } }),
        });
        reservations.value = reservations.value.map((item) => (item.id === updated.id ? updated : item));
        message.value = `预约 #${updated.id} 已更新为 ${updated.status}`;
      } catch (error) {
        message.value = error.message;
      }
    }

    async function createOrder() {
      if (!latestReservation.value || selectedOrderItems.value.length === 0) {
        message.value = "请选择预约和菜品";
        return;
      }

      try {
        const order = await api("/orders", {
          method: "POST",
          body: JSON.stringify({
            data: {
              reservationId: latestReservation.value.id,
              items: selectedOrderItems.value,
            },
          }),
        });
        orders.value = [order, ...orders.value];
        message.value = `订单已生成：${cents(order.total_cents)}`;
      } catch (error) {
        message.value = error.message;
      }
    }

    async function addHealthRecord() {
      try {
        await api("/cat-health-records", {
          method: "POST",
          body: JSON.stringify({ data: healthForm }),
        });
        message.value = "健康记录已保存";
      } catch (error) {
        message.value = error.message;
      }
    }

    function togglePreference(tag) {
      if (reservationForm.preferences.includes(tag)) {
        reservationForm.preferences = reservationForm.preferences.filter((item) => item !== tag);
      } else {
        reservationForm.preferences = [...reservationForm.preferences, tag];
      }
      refreshRecommendations();
    }

    onMounted(loadBaseData);

    return {
      activeReservations,
      activeTab,
      addHealthRecord,
      cents,
      createOrder,
      createReservation,
      currentStore,
      dashboard,
      healthForm,
      latestReservation,
      loading,
      menuItems,
      message,
      orderTotal,
      orders,
      preferenceOptions,
      recommendations,
      refreshReservations,
      refreshTables,
      reservationForm,
      reservations,
      search,
      selectedMenu,
      selectedOrderItems,
      stores,
      tabs,
      tables,
      cats,
      togglePreference,
      updateReservationStatus,
    };
  },
  template: `
    <div class="app-shell">
      <aside class="sidebar" aria-label="主导航">
        <div class="brand">
          <div class="brand-mark">N</div>
          <div>
            <strong>NekoCafe</strong>
            <span>G17 · T-01</span>
          </div>
        </div>
        <nav class="nav-list">
          <button
            v-for="tab in tabs"
            :key="tab.key"
            class="nav-button"
            :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key"
          >
            <component :is="tab.icon" :size="18" />
            <span>{{ tab.label }}</span>
          </button>
        </nav>
      </aside>

      <main class="workspace">
        <section class="banner">
          <img src="/assets/neko-cafe-banner.png" alt="NekoCafe 猫咪主题餐厅" />
          <div class="banner-copy">
            <span class="eyebrow">{{ currentStore?.city || '北京' }}</span>
            <h1>{{ currentStore?.name || 'NekoCafe 智慧餐饮预约平台' }}</h1>
            <div class="banner-metrics">
              <span>{{ dashboard.today_reservations }} 今日预约</span>
              <span>{{ dashboard.seated_count }} 已入座</span>
              <span>{{ cents(dashboard.revenue_cents) }} 沙箱收入</span>
            </div>
          </div>
        </section>

        <div class="status-row">
          <div class="store-select">
            <Coffee :size="18" />
            <select v-model.number="reservationForm.storeId" @change="refreshTables">
              <option v-for="store in stores" :key="store.id" :value="store.id">
                {{ store.name }}
              </option>
            </select>
          </div>
          <button class="icon-text-button" @click="refreshReservations" :disabled="loading">
            <RefreshCw :size="16" />
            <span>刷新</span>
          </button>
          <p v-if="message" class="toast">{{ message }}</p>
        </div>

        <section v-if="activeTab === 'customer'" class="section-grid customer-grid">
          <form class="panel form-panel" @submit.prevent="createReservation">
            <div class="panel-title">
              <CalendarDays :size="18" />
              <h2>预约</h2>
            </div>
            <div class="field-grid">
              <label>
                <span>姓名</span>
                <input v-model.trim="reservationForm.customerName" required />
              </label>
              <label>
                <span>手机号</span>
                <input v-model.trim="reservationForm.mobileNumber" required />
              </label>
              <label>
                <span>日期</span>
                <input type="date" v-model="reservationForm.reservationDate" @change="refreshTables" required />
              </label>
              <label>
                <span>时间</span>
                <input type="time" v-model="reservationForm.reservationTime" @change="refreshTables" required />
              </label>
              <label>
                <span>人数</span>
                <input type="number" min="1" max="12" v-model.number="reservationForm.partySize" @change="refreshTables" required />
              </label>
              <label>
                <span>桌位</span>
                <select v-model="reservationForm.tableId">
                  <option value="">自动分配</option>
                  <option
                    v-for="table in tables"
                    :key="table.id"
                    :value="table.id"
                    :disabled="table.available_for_slot === false"
                  >
                    {{ table.code }} · {{ table.seats }} 人 · {{ table.area }}
                  </option>
                </select>
              </label>
            </div>
            <div class="chips">
              <button
                v-for="tag in preferenceOptions"
                :key="tag"
                type="button"
                class="chip"
                :class="{ selected: reservationForm.preferences.includes(tag) }"
                @click="togglePreference(tag)"
              >
                {{ tag }}
              </button>
            </div>
            <label>
              <span>备注</span>
              <textarea v-model.trim="reservationForm.note" rows="3"></textarea>
            </label>
            <button class="primary-button" type="submit" :disabled="loading">
              <CheckCircle2 :size="18" />
              <span>提交预约</span>
            </button>
          </form>

          <section class="panel">
            <div class="panel-title">
              <Sparkles :size="18" />
              <h2>推荐</h2>
            </div>
            <div v-if="recommendations.cat" class="recommendation-band">
              <Cat :size="24" />
              <div>
                <strong>{{ recommendations.cat.name }}</strong>
                <span>{{ recommendations.cat.breed }} · {{ recommendations.cat.personality_tags?.join(' / ') }}</span>
              </div>
            </div>
            <div class="table-grid">
              <article v-for="table in recommendations.tables" :key="table.id" class="item-card">
                <Armchair :size="20" />
                <strong>{{ table.code }}</strong>
                <span>{{ table.seats }} 人 · {{ table.area }}</span>
              </article>
            </div>
            <div class="menu-list">
              <article v-for="item in menuItems" :key="item.id" class="menu-row">
                <div>
                  <strong>{{ item.name }}</strong>
                  <span>{{ item.category }} · {{ item.tags?.join(' / ') }}</span>
                </div>
                <div class="quantity">
                  <span>{{ cents(item.price_cents) }}</span>
                  <input type="number" min="0" max="9" v-model.number="selectedMenu[item.id]" />
                </div>
              </article>
            </div>
            <button class="secondary-button" @click="createOrder">
              <ShoppingCart :size="18" />
              <span>生成点单 {{ cents(orderTotal) }}</span>
            </button>
          </section>
        </section>

        <section v-if="activeTab === 'staff'" class="section-grid staff-grid">
          <div class="panel">
            <div class="panel-title">
              <Search :size="18" />
              <h2>检索</h2>
            </div>
            <div class="field-grid compact">
              <label>
                <span>手机号</span>
                <input v-model.trim="search.mobileNumber" />
              </label>
              <label>
                <span>日期</span>
                <input type="date" v-model="search.date" />
              </label>
            </div>
            <button class="secondary-button" @click="refreshReservations">
              <Search :size="17" />
              <span>查询</span>
            </button>
          </div>

          <div class="reservation-list">
            <article v-for="reservation in reservations" :key="reservation.id" class="reservation-card">
              <div>
                <span class="badge">{{ reservation.status }}</span>
                <h3>{{ reservation.customer_name || reservation.customerName }}</h3>
                <p>{{ reservation.reservation_date }} {{ reservation.reservation_time }} · {{ reservation.party_size }} 人</p>
                <p>{{ reservation.store_name }} · {{ reservation.table_code || '待分配' }} · {{ reservation.cat_name || '未指定猫咪' }}</p>
              </div>
              <div class="card-actions">
                <button class="icon-button" title="入座" @click="updateReservationStatus(reservation, 'seated')" :disabled="reservation.status !== 'booked'">
                  <CheckCircle2 :size="18" />
                </button>
                <button class="icon-button" title="完桌" @click="updateReservationStatus(reservation, 'finished')" :disabled="reservation.status !== 'seated'">
                  <Utensils :size="18" />
                </button>
                <button class="icon-button danger" title="取消" @click="updateReservationStatus(reservation, 'cancelled')" :disabled="reservation.status !== 'booked'">
                  <XCircle :size="18" />
                </button>
              </div>
            </article>
          </div>
        </section>

        <section v-if="activeTab === 'cats'" class="section-grid cat-grid">
          <div class="cat-list">
            <article v-for="cat in cats" :key="cat.id" class="cat-card">
              <Cat :size="22" />
              <div>
                <h3>{{ cat.name }}</h3>
                <p>{{ cat.breed }} · {{ cat.health_status }}</p>
                <span>{{ cat.personality_tags?.join(' / ') }}</span>
              </div>
            </article>
          </div>
          <form class="panel form-panel" @submit.prevent="addHealthRecord">
            <div class="panel-title">
              <HeartPulse :size="18" />
              <h2>健康记录</h2>
            </div>
            <label>
              <span>猫咪</span>
              <select v-model.number="healthForm.catId">
                <option v-for="cat in cats" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
              </select>
            </label>
            <label>
              <span>体重 kg</span>
              <input type="number" min="1" step="0.1" v-model.number="healthForm.weightKg" />
            </label>
            <label>
              <span>疫苗</span>
              <input v-model.trim="healthForm.vaccineNote" />
            </label>
            <label>
              <span>互动</span>
              <textarea rows="3" v-model.trim="healthForm.interactionNote"></textarea>
            </label>
            <button class="primary-button" type="submit">
              <CheckCircle2 :size="18" />
              <span>保存记录</span>
            </button>
          </form>
        </section>

        <section v-if="activeTab === 'ops'" class="ops-layout">
          <div class="metric-strip">
            <article>
              <span>预约</span>
              <strong>{{ dashboard.today_reservations }}</strong>
            </article>
            <article>
              <span>翻台</span>
              <strong>{{ dashboard.turnover_rate }}</strong>
            </article>
            <article>
              <span>复购</span>
              <strong>{{ Math.round(dashboard.repeat_rate * 100) }}%</strong>
            </article>
            <article>
              <span>收入</span>
              <strong>{{ cents(dashboard.revenue_cents) }}</strong>
            </article>
          </div>
          <div class="panel chart-panel">
            <div class="panel-title">
              <LayoutDashboard :size="18" />
              <h2>经营状态</h2>
            </div>
            <div class="bar-chart" aria-label="运营指标柱状图">
              <span style="height: 65%"></span>
              <span style="height: 42%"></span>
              <span style="height: 78%"></span>
              <span style="height: 55%"></span>
              <span style="height: 88%"></span>
              <span style="height: 70%"></span>
            </div>
          </div>
          <div class="alert-list">
            <article v-for="alert in dashboard.alerts" :key="alert.id" class="alert-card">
              <span class="badge warning">{{ alert.level }}</span>
              <strong>{{ alert.title }}</strong>
              <p>{{ alert.detail }}</p>
            </article>
          </div>
        </section>
      </main>
    </div>
  `,
};

createApp(App).mount("#app");
