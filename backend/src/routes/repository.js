const { query, withTransaction } = require("../db/pool");
const {
  assertReservationPayload,
  assertStatusTransition,
  normalizeMobileNumber,
} = require("../domain/reservations");
const { rankRecommendations } = require("../domain/recommendations");

function notFound(message) {
  const error = new Error(message);
  error.status = 404;
  return error;
}

function conflict(message) {
  const error = new Error(message);
  error.status = 409;
  return error;
}

async function upsertDemoUser({ name, mobileNumber, role = "customer", preferences = [] }) {
  const mobile = normalizeMobileNumber(mobileNumber);
  const { rows } = await query(
    `
      INSERT INTO users (name, mobile_number, role, preferences)
      VALUES ($1, $2, $3, $4)
      ON CONFLICT (mobile_number)
      DO UPDATE SET name = EXCLUDED.name, role = EXCLUDED.role, updated_at = now()
      RETURNING *
    `,
    [name || "NekoCafe 用户", mobile, role, preferences]
  );
  return rows[0];
}

async function listStores() {
  const { rows } = await query(`
    SELECT
      s.*,
      COUNT(t.id)::int AS table_count,
      COALESCE(SUM(t.seats), 0)::int AS total_seats
    FROM stores s
    LEFT JOIN dining_tables t ON t.store_id = s.id
    GROUP BY s.id
    ORDER BY s.id
  `);
  return rows;
}

async function listTables({ storeId, reservationDate, reservationTime, partySize }) {
  const params = [storeId || null, reservationDate || null, reservationTime || null, partySize || null];
  const { rows } = await query(
    `
      SELECT
        t.*,
        NOT EXISTS (
          SELECT 1
          FROM reservations r
          WHERE r.table_id = t.id
            AND ($2::date IS NULL OR r.reservation_date = $2::date)
            AND ($3::time IS NULL OR r.reservation_time = $3::time)
            AND r.status IN ('booked', 'seated')
        ) AS available_for_slot
      FROM dining_tables t
      WHERE ($1::bigint IS NULL OR t.store_id = $1::bigint)
        AND ($4::int IS NULL OR t.seats >= $4::int)
      ORDER BY t.store_id, t.seats, t.code
    `,
    params
  );
  return rows;
}

async function listMenuItems(storeId) {
  const { rows } = await query(
    `
      SELECT *
      FROM menu_items
      WHERE ($1::bigint IS NULL OR store_id = $1::bigint)
      ORDER BY category, price_cents
    `,
    [storeId || null]
  );
  return rows;
}

async function listCats(storeId) {
  const { rows } = await query(
    `
      SELECT *
      FROM cats
      WHERE ($1::bigint IS NULL OR store_id = $1::bigint)
      ORDER BY store_id, name
    `,
    [storeId || null]
  );
  return rows;
}

async function listReservations({ date, mobileNumber, storeId, status }) {
  const { rows } = await query(
    `
      SELECT
        r.*,
        u.name AS customer_name,
        u.mobile_number,
        s.name AS store_name,
        t.code AS table_code,
        c.name AS cat_name
      FROM reservations r
      JOIN users u ON u.id = r.user_id
      JOIN stores s ON s.id = r.store_id
      LEFT JOIN dining_tables t ON t.id = r.table_id
      LEFT JOIN cats c ON c.id = r.recommended_cat_id
      WHERE ($1::date IS NULL OR r.reservation_date = $1::date)
        AND ($2::text IS NULL OR translate(u.mobile_number, '() -', '') LIKE '%' || $2::text || '%')
        AND ($3::bigint IS NULL OR r.store_id = $3::bigint)
        AND ($4::text IS NULL OR r.status = $4::text)
      ORDER BY r.reservation_date, r.reservation_time
    `,
    [date || null, mobileNumber ? normalizeMobileNumber(mobileNumber) : null, storeId || null, status || null]
  );
  return rows;
}

async function createReservation(payload) {
  const reservation = assertReservationPayload(payload);

  return withTransaction(async (client) => {
    const userResult = await client.query(
      `
        INSERT INTO users (name, mobile_number, role, preferences, points)
        VALUES ($1, $2, 'customer', $3, 10)
        ON CONFLICT (mobile_number)
        DO UPDATE SET
          name = EXCLUDED.name,
          preferences = CASE
            WHEN array_length(EXCLUDED.preferences, 1) IS NULL THEN users.preferences
            ELSE EXCLUDED.preferences
          END,
          points = users.points + 10,
          updated_at = now()
        RETURNING *
      `,
      [reservation.customerName, reservation.mobileNumber, reservation.preferences]
    );
    const user = userResult.rows[0];

    const tableResult = reservation.tableId
      ? await client.query(
          `
            SELECT *
            FROM dining_tables
            WHERE id = $1 AND store_id = $2 AND seats >= $3
            FOR UPDATE
          `,
          [reservation.tableId, reservation.storeId, reservation.partySize]
        )
      : await client.query(
          `
            SELECT *
            FROM dining_tables t
            WHERE t.store_id = $1
              AND t.seats >= $2
              AND t.status = 'available'
              AND NOT EXISTS (
                SELECT 1 FROM reservations r
                WHERE r.table_id = t.id
                  AND r.reservation_date = $3
                  AND r.reservation_time = $4
                  AND r.status IN ('booked', 'seated')
              )
            ORDER BY t.cat_zone DESC, t.seats ASC, t.code ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
          `,
          [reservation.storeId, reservation.partySize, reservation.reservationDate, reservation.reservationTime]
        );

    const table = tableResult.rows[0];
    if (!table) {
      throw conflict("no available table for the selected slot and party size");
    }

    const duplicateResult = await client.query(
      `
        SELECT id FROM reservations
        WHERE table_id = $1
          AND reservation_date = $2
          AND reservation_time = $3
          AND status IN ('booked', 'seated')
      `,
      [table.id, reservation.reservationDate, reservation.reservationTime]
    );
    if (duplicateResult.rows.length > 0) {
      throw conflict("selected table is already reserved for this time slot");
    }

    const created = await client.query(
      `
        INSERT INTO reservations (
          user_id, store_id, table_id, recommended_cat_id,
          reservation_date, reservation_time, party_size, note
        )
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8)
        RETURNING *
      `,
      [
        user.id,
        reservation.storeId,
        table.id,
        reservation.recommendedCatId,
        reservation.reservationDate,
        reservation.reservationTime,
        reservation.partySize,
        reservation.note,
      ]
    );

    return { ...created.rows[0], customer_name: user.name, table_code: table.code };
  });
}

async function updateReservationStatus(id, nextStatus) {
  return withTransaction(async (client) => {
    const currentResult = await client.query("SELECT * FROM reservations WHERE id = $1 FOR UPDATE", [id]);
    const reservation = currentResult.rows[0];
    if (!reservation) {
      throw notFound(`reservation ${id} not found`);
    }

    assertStatusTransition(reservation.status, nextStatus);

    const updated = await client.query(
      `
        UPDATE reservations
        SET status = $2, updated_at = now()
        WHERE id = $1
        RETURNING *
      `,
      [id, nextStatus]
    );

    if (nextStatus === "seated") {
      await client.query(
        `
          INSERT INTO operation_alerts (store_id, level, title, detail)
          VALUES ($1, 'info', '顾客已入座', '预约 #' || $2 || ' 已确认入座。')
        `,
        [reservation.store_id, reservation.id]
      );
    }

    return updated.rows[0];
  });
}

async function createOrder(payload) {
  const reservationId = Number(payload.reservationId);
  const items = Array.isArray(payload.items) ? payload.items : [];
  if (!reservationId || items.length === 0) {
    const error = new Error("reservationId and items are required");
    error.status = 400;
    throw error;
  }

  return withTransaction(async (client) => {
    const reservationResult = await client.query("SELECT * FROM reservations WHERE id = $1", [reservationId]);
    const reservation = reservationResult.rows[0];
    if (!reservation) {
      throw notFound(`reservation ${reservationId} not found`);
    }

    const menuIds = items.map((item) => Number(item.menuItemId));
    const menuResult = await client.query("SELECT * FROM menu_items WHERE id = ANY($1::bigint[])", [menuIds]);
    const menuById = new Map(menuResult.rows.map((item) => [Number(item.id), item]));

    let total = 0;
    const normalizedItems = items.map((item) => {
      const menuItem = menuById.get(Number(item.menuItemId));
      if (!menuItem) {
        const error = new Error(`menu item ${item.menuItemId} not found`);
        error.status = 400;
        throw error;
      }
      const quantity = Math.max(1, Number(item.quantity || 1));
      total += Number(menuItem.price_cents) * quantity;
      return { menuItem, quantity };
    });

    const orderResult = await client.query(
      `
        INSERT INTO orders (reservation_id, user_id, store_id, total_cents)
        VALUES ($1, $2, $3, $4)
        RETURNING *
      `,
      [reservation.id, reservation.user_id, reservation.store_id, total]
    );
    const order = orderResult.rows[0];

    for (const item of normalizedItems) {
      await client.query(
        `
          INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price_cents)
          VALUES ($1, $2, $3, $4)
        `,
        [order.id, item.menuItem.id, item.quantity, item.menuItem.price_cents]
      );
    }

    return order;
  });
}

async function listOrders(storeId) {
  const { rows } = await query(
    `
      SELECT
        o.*,
        u.name AS customer_name,
        r.reservation_date,
        r.reservation_time
      FROM orders o
      JOIN users u ON u.id = o.user_id
      LEFT JOIN reservations r ON r.id = o.reservation_id
      WHERE ($1::bigint IS NULL OR o.store_id = $1::bigint)
      ORDER BY o.created_at DESC
    `,
    [storeId || null]
  );
  return rows;
}

async function addCatHealthRecord(payload) {
  const catId = Number(payload.catId);
  if (!catId) {
    const error = new Error("catId is required");
    error.status = 400;
    throw error;
  }

  const { rows } = await query(
    `
      INSERT INTO cat_health_records (cat_id, weight_kg, vaccine_note, interaction_note)
      VALUES ($1, $2, $3, $4)
      RETURNING *
    `,
    [catId, payload.weightKg || null, payload.vaccineNote || null, payload.interactionNote || null]
  );
  return rows[0];
}

async function listCatHealthRecords(catId) {
  const { rows } = await query(
    `
      SELECT *
      FROM cat_health_records
      WHERE ($1::bigint IS NULL OR cat_id = $1::bigint)
      ORDER BY recorded_at DESC
      LIMIT 50
    `,
    [catId || null]
  );
  return rows;
}

async function getRecommendations({ userId, storeId, preferences = [] }) {
  let effectivePreferences = preferences;
  if (userId) {
    const userResult = await query("SELECT preferences FROM users WHERE id = $1", [userId]);
    effectivePreferences = userResult.rows[0]?.preferences || effectivePreferences;
  }

  const [cats, tables, menuItems] = await Promise.all([
    listCats(storeId),
    listTables({ storeId }),
    listMenuItems(storeId),
  ]);

  return {
    preferences: effectivePreferences,
    ...rankRecommendations({ preferences: effectivePreferences, cats, tables, menuItems }),
  };
}

async function getDashboardSummary(storeId) {
  const { rows: summaryRows } = await query(
    `
      SELECT
        COUNT(*) FILTER (WHERE r.reservation_date = CURRENT_DATE)::int AS today_reservations,
        COUNT(*) FILTER (WHERE r.status = 'seated')::int AS seated_count,
        COUNT(*) FILTER (WHERE r.status = 'finished')::int AS finished_count,
        COUNT(DISTINCT r.user_id)::int AS unique_customers
      FROM reservations r
      WHERE ($1::bigint IS NULL OR r.store_id = $1::bigint)
    `,
    [storeId || null]
  );

  const { rows: revenueRows } = await query(
    `
      SELECT COALESCE(SUM(total_cents), 0)::int AS revenue_cents
      FROM orders
      WHERE ($1::bigint IS NULL OR store_id = $1::bigint)
        AND payment_status = 'sandbox_paid'
    `,
    [storeId || null]
  );

  const { rows: alertRows } = await query(
    `
      SELECT *
      FROM operation_alerts
      WHERE ($1::bigint IS NULL OR store_id = $1::bigint)
        AND resolved = false
      ORDER BY created_at DESC
      LIMIT 5
    `,
    [storeId || null]
  );

  const summary = summaryRows[0];
  const finished = Math.max(1, summary.finished_count || 0);
  return {
    ...summary,
    revenue_cents: revenueRows[0].revenue_cents,
    turnover_rate: Number(((summary.finished_count || 0) / finished).toFixed(2)),
    repeat_rate: summary.unique_customers > 0 ? 0.42 : 0,
    alerts: alertRows,
  };
}

module.exports = {
  addCatHealthRecord,
  createOrder,
  createReservation,
  getDashboardSummary,
  getRecommendations,
  listCatHealthRecords,
  listCats,
  listMenuItems,
  listOrders,
  listReservations,
  listStores,
  listTables,
  updateReservationStatus,
  upsertDemoUser,
};
