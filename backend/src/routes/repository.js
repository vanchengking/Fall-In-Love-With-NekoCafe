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

function badRequest(message) {
  const error = new Error(message);
  error.status = 400;
  return error;
}

function parseJsonArray(value) {
  if (Array.isArray(value)) {
    return value;
  }
  if (!value) {
    return [];
  }
  if (typeof value === "string") {
    try {
      const parsed = JSON.parse(value);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }
  return [];
}

function bool(value) {
  return value === true || value === 1;
}

function normalizeUser(row) {
  return row ? { ...row, preferences: parseJsonArray(row.preferences) } : row;
}

function normalizeCat(row) {
  return row ? { ...row, personality_tags: parseJsonArray(row.personality_tags) } : row;
}

function normalizeMenuItem(row) {
  return row ? { ...row, tags: parseJsonArray(row.tags) } : row;
}

function normalizeTable(row) {
  return row
    ? {
        ...row,
        cat_zone: bool(row.cat_zone),
        available_for_slot: row.available_for_slot === undefined ? undefined : bool(row.available_for_slot),
      }
    : row;
}

function normalizeReservation(row) {
  if (!row) {
    return row;
  }
  return {
    ...row,
    reservation_time: String(row.reservation_time || "").slice(0, 5),
  };
}

function normalizeAlert(row) {
  return row ? { ...row, resolved: bool(row.resolved) } : row;
}

async function readUserByMobile(mobileNumber, client = { query }) {
  const { rows } = await client.query("SELECT * FROM users WHERE mobile_number = ? LIMIT 1", [mobileNumber]);
  return normalizeUser(rows[0]);
}

async function upsertDemoUser({ name, mobileNumber, role = "customer", preferences = [] }) {
  const mobile = normalizeMobileNumber(mobileNumber);
  await query(
    `
      INSERT INTO users (name, mobile_number, role, preferences)
      VALUES (?, ?, ?, ?)
      ON DUPLICATE KEY UPDATE
        name = VALUES(name),
        role = VALUES(role),
        preferences = VALUES(preferences),
        updated_at = CURRENT_TIMESTAMP
    `,
    [name || "NekoCafe 用户", mobile, role, JSON.stringify(preferences)]
  );
  return readUserByMobile(mobile);
}

async function listStores() {
  const { rows } = await query(`
    SELECT
      s.*,
      COUNT(t.id) AS table_count,
      COALESCE(SUM(t.seats), 0) AS total_seats
    FROM stores s
    LEFT JOIN dining_tables t ON t.store_id = s.id
    GROUP BY s.id
    ORDER BY s.id
  `);
  return rows.map((row) => ({
    ...row,
    table_count: Number(row.table_count),
    total_seats: Number(row.total_seats),
  }));
}

async function listTables({ storeId, reservationDate, reservationTime, partySize }) {
  const { rows } = await query(
    `
      SELECT
        t.*,
        NOT EXISTS (
          SELECT 1
          FROM reservations r
          WHERE r.table_id = t.id
            AND (? IS NULL OR r.reservation_date = ?)
            AND (? IS NULL OR r.reservation_time = ?)
            AND r.status IN ('booked', 'seated')
        ) AS available_for_slot
      FROM dining_tables t
      WHERE (? IS NULL OR t.store_id = ?)
        AND (? IS NULL OR t.seats >= ?)
      ORDER BY t.store_id, t.seats, t.code
    `,
    [
      reservationDate || null,
      reservationDate || null,
      reservationTime || null,
      reservationTime || null,
      storeId || null,
      storeId || null,
      partySize || null,
      partySize || null,
    ]
  );
  return rows.map(normalizeTable);
}

async function listMenuItems(storeId) {
  const { rows } = await query(
    `
      SELECT *
      FROM menu_items
      WHERE (? IS NULL OR store_id = ?)
      ORDER BY category, price_cents
    `,
    [storeId || null, storeId || null]
  );
  return rows.map(normalizeMenuItem);
}

async function listCats(storeId) {
  const { rows } = await query(
    `
      SELECT *
      FROM cats
      WHERE (? IS NULL OR store_id = ?)
      ORDER BY store_id, name
    `,
    [storeId || null, storeId || null]
  );
  return rows.map(normalizeCat);
}

async function listReservations({ date, mobileNumber, storeId, status }) {
  const cleanedMobile = mobileNumber ? normalizeMobileNumber(mobileNumber) : null;
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
      WHERE (? IS NULL OR r.reservation_date = ?)
        AND (? IS NULL OR REPLACE(REPLACE(REPLACE(REPLACE(u.mobile_number, '(', ''), ')', ''), ' ', ''), '-', '') LIKE CONCAT('%', ?, '%'))
        AND (? IS NULL OR r.store_id = ?)
        AND (? IS NULL OR r.status = ?)
      ORDER BY r.reservation_date, r.reservation_time
    `,
    [
      date || null,
      date || null,
      cleanedMobile,
      cleanedMobile,
      storeId || null,
      storeId || null,
      status || null,
      status || null,
    ]
  );
  return rows.map(normalizeReservation);
}

async function createReservation(payload) {
  const reservation = assertReservationPayload(payload);

  return withTransaction(async (client) => {
    await client.query(
      `
        INSERT INTO users (name, mobile_number, role, preferences, points)
        VALUES (?, ?, 'customer', ?, 10)
        ON DUPLICATE KEY UPDATE
          name = VALUES(name),
          preferences = IF(JSON_LENGTH(VALUES(preferences)) = 0, preferences, VALUES(preferences)),
          points = points + 10,
          updated_at = CURRENT_TIMESTAMP
      `,
      [reservation.customerName, reservation.mobileNumber, JSON.stringify(reservation.preferences)]
    );
    const user = await readUserByMobile(reservation.mobileNumber, client);

    const tableResult = reservation.tableId
      ? await client.query(
          `
            SELECT *
            FROM dining_tables
            WHERE id = ? AND store_id = ? AND seats >= ?
            FOR UPDATE
          `,
          [reservation.tableId, reservation.storeId, reservation.partySize]
        )
      : await client.query(
          `
            SELECT *
            FROM dining_tables t
            WHERE t.store_id = ?
              AND t.seats >= ?
              AND t.status = 'available'
              AND NOT EXISTS (
                SELECT 1 FROM reservations r
                WHERE r.table_id = t.id
                  AND r.reservation_date = ?
                  AND r.reservation_time = ?
                  AND r.status IN ('booked', 'seated')
              )
            ORDER BY t.cat_zone DESC, t.seats ASC, t.code ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
          `,
          [reservation.storeId, reservation.partySize, reservation.reservationDate, reservation.reservationTime]
        );

    const table = normalizeTable(tableResult.rows[0]);
    if (!table) {
      throw conflict("no available table for the selected slot and party size");
    }

    const duplicateResult = await client.query(
      `
        SELECT id FROM reservations
        WHERE table_id = ?
          AND reservation_date = ?
          AND reservation_time = ?
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
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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

    const createdId = created.result.insertId;
    const { rows } = await client.query("SELECT * FROM reservations WHERE id = ?", [createdId]);
    return normalizeReservation({ ...rows[0], customer_name: user.name, table_code: table.code });
  });
}

async function updateReservationStatus(id, nextStatus) {
  return withTransaction(async (client) => {
    const currentResult = await client.query("SELECT * FROM reservations WHERE id = ? FOR UPDATE", [id]);
    const reservation = normalizeReservation(currentResult.rows[0]);
    if (!reservation) {
      throw notFound(`reservation ${id} not found`);
    }

    assertStatusTransition(reservation.status, nextStatus);

    await client.query(
      `
        UPDATE reservations
        SET status = ?, updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
      `,
      [nextStatus, id]
    );

    if (nextStatus === "seated") {
      await client.query(
        `
          INSERT INTO operation_alerts (store_id, level, title, detail)
          VALUES (?, 'info', '顾客已入座', CONCAT('预约 #', ?, ' 已确认入座。'))
        `,
        [reservation.store_id, reservation.id]
      );
    }

    const { rows } = await client.query("SELECT * FROM reservations WHERE id = ?", [id]);
    return normalizeReservation(rows[0]);
  });
}

async function createOrder(payload) {
  const reservationId = Number(payload.reservationId);
  const items = Array.isArray(payload.items) ? payload.items : [];
  if (!reservationId || items.length === 0) {
    throw badRequest("reservationId and items are required");
  }

  return withTransaction(async (client) => {
    const reservationResult = await client.query("SELECT * FROM reservations WHERE id = ?", [reservationId]);
    const reservation = normalizeReservation(reservationResult.rows[0]);
    if (!reservation) {
      throw notFound(`reservation ${reservationId} not found`);
    }

    const menuIds = items.map((item) => Number(item.menuItemId)).filter(Boolean);
    if (menuIds.length === 0) {
      throw badRequest("at least one valid menuItemId is required");
    }

    const menuResult = await client.query("SELECT * FROM menu_items WHERE id IN (?)", [menuIds]);
    const menuById = new Map(menuResult.rows.map((item) => [Number(item.id), normalizeMenuItem(item)]));

    let total = 0;
    const normalizedItems = items.map((item) => {
      const menuItem = menuById.get(Number(item.menuItemId));
      if (!menuItem) {
        throw badRequest(`menu item ${item.menuItemId} not found`);
      }
      const quantity = Math.max(1, Number(item.quantity || 1));
      total += Number(menuItem.price_cents) * quantity;
      return { menuItem, quantity };
    });

    const orderResult = await client.query(
      `
        INSERT INTO orders (reservation_id, user_id, store_id, total_cents)
        VALUES (?, ?, ?, ?)
      `,
      [reservation.id, reservation.user_id, reservation.store_id, total]
    );
    const orderId = orderResult.result.insertId;

    for (const item of normalizedItems) {
      await client.query(
        `
          INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price_cents)
          VALUES (?, ?, ?, ?)
        `,
        [orderId, item.menuItem.id, item.quantity, item.menuItem.price_cents]
      );
    }

    const { rows } = await client.query("SELECT * FROM orders WHERE id = ?", [orderId]);
    return rows[0];
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
      WHERE (? IS NULL OR o.store_id = ?)
      ORDER BY o.created_at DESC
    `,
    [storeId || null, storeId || null]
  );
  return rows.map((row) => ({ ...row, reservation_time: row.reservation_time ? String(row.reservation_time).slice(0, 5) : null }));
}

async function addCatHealthRecord(payload) {
  const catId = Number(payload.catId);
  if (!catId) {
    throw badRequest("catId is required");
  }

  const result = await query(
    `
      INSERT INTO cat_health_records (cat_id, weight_kg, vaccine_note, interaction_note)
      VALUES (?, ?, ?, ?)
    `,
    [catId, payload.weightKg || null, payload.vaccineNote || null, payload.interactionNote || null]
  );
  const { rows } = await query("SELECT * FROM cat_health_records WHERE id = ?", [result.result.insertId]);
  return rows[0];
}

async function listCatHealthRecords(catId) {
  const { rows } = await query(
    `
      SELECT *
      FROM cat_health_records
      WHERE (? IS NULL OR cat_id = ?)
      ORDER BY recorded_at DESC
      LIMIT 50
    `,
    [catId || null, catId || null]
  );
  return rows;
}

async function getRecommendations({ userId, storeId, preferences = [] }) {
  let effectivePreferences = preferences;
  if (userId) {
    const userResult = await query("SELECT preferences FROM users WHERE id = ?", [userId]);
    effectivePreferences = normalizeUser(userResult.rows[0])?.preferences || effectivePreferences;
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
        SUM(CASE WHEN r.reservation_date = CURRENT_DATE THEN 1 ELSE 0 END) AS today_reservations,
        SUM(CASE WHEN r.status = 'seated' THEN 1 ELSE 0 END) AS seated_count,
        SUM(CASE WHEN r.status = 'dining' THEN 1 ELSE 0 END) AS dining_count,
        SUM(CASE WHEN r.status = 'finished' THEN 1 ELSE 0 END) AS finished_count,
        COUNT(DISTINCT r.user_id) AS unique_customers
      FROM reservations r
      WHERE (? IS NULL OR r.store_id = ?)
    `,
    [storeId || null, storeId || null]
  );

  const { rows: revenueRows } = await query(
    `
      SELECT COALESCE(SUM(total_cents), 0) AS revenue_cents
      FROM orders
      WHERE (? IS NULL OR store_id = ?)
        AND payment_status = 'sandbox_paid'
    `,
    [storeId || null, storeId || null]
  );

  const { rows: tableCountRows } = await query(
    `SELECT COUNT(*) AS table_count FROM dining_tables WHERE (? IS NULL OR store_id = ?)`,
    [storeId || null, storeId || null]
  );

  const { rows: repeatRows } = await query(
    `
      SELECT
        COUNT(*) AS total_customers,
        SUM(CASE WHEN order_count > 1 THEN 1 ELSE 0 END) AS repeat_customers
      FROM (
        SELECT user_id, COUNT(*) AS order_count
        FROM orders
        WHERE (? IS NULL OR store_id = ?)
        GROUP BY user_id
      ) t
    `,
    [storeId || null, storeId || null]
  );

  const { rows: alertRows } = await query(
    `
      SELECT *
      FROM operation_alerts
      WHERE (? IS NULL OR store_id = ?)
        AND resolved = false
      ORDER BY created_at DESC
      LIMIT 5
    `,
    [storeId || null, storeId || null]
  );

  const summary = summaryRows[0] || {};
  const finishedCount = Number(summary.finished_count || 0);
  const tableCount = Number(tableCountRows[0]?.table_count || 1);
  const totalCustomers = Number(repeatRows[0]?.total_customers || 0);
  const repeatCustomers = Number(repeatRows[0]?.repeat_customers || 0);

  return {
    today_reservations: Number(summary.today_reservations || 0),
    seated_count: Number(summary.seated_count || 0),
    dining_count: Number(summary.dining_count || 0),
    finished_count: finishedCount,
    unique_customers: Number(summary.unique_customers || 0),
    revenue_cents: Number(revenueRows[0]?.revenue_cents || 0),
    turnover_rate: Number((finishedCount / tableCount).toFixed(2)),
    repeat_rate: totalCustomers > 0 ? Number((repeatCustomers / totalCustomers).toFixed(3)) : 0,
    alerts: alertRows.map(normalizeAlert),
  };
}

// ── 门店 CRUD ──
async function createStore({ name, city, address, phone, open_time, close_time }) {
  const result = await query(
    `INSERT INTO stores (name, city, address, phone, open_time, close_time) VALUES (?, ?, ?, ?, ?, ?)`,
    [name, city, address, phone, open_time || '10:30:00', close_time || '22:30:00']
  );
  return { id: result.insertId, name, city, address, phone };
}

async function updateStore(id, fields) {
  const allowed = ['name', 'city', 'address', 'phone', 'open_time', 'close_time'];
  const sets = []; const vals = [];
  for (const [k, v] of Object.entries(fields)) {
    if (allowed.includes(k) && v !== undefined) { sets.push(`${k} = ?`); vals.push(v); }
  }
  if (!sets.length) return;
  vals.push(id);
  await query(`UPDATE stores SET ${sets.join(', ')} WHERE id = ?`, vals);
}

async function deleteStore(id) {
  await query('DELETE FROM stores WHERE id = ?', [id]);
}

// ── 桌位 CRUD ──
async function createTable({ store_id, code, seats, area, cat_zone }) {
  const result = await query(
    `INSERT INTO dining_tables (store_id, code, seats, area, cat_zone) VALUES (?, ?, ?, ?, ?)`,
    [store_id, code, seats || 4, area || 'main', cat_zone || false]
  );
  return { id: result.insertId, store_id, code, seats, area, cat_zone };
}

async function updateTable(id, fields) {
  const allowed = ['code', 'seats', 'area', 'cat_zone', 'status'];
  const sets = []; const vals = [];
  for (const [k, v] of Object.entries(fields)) {
    if (allowed.includes(k) && v !== undefined) { sets.push(`${k} = ?`); vals.push(v); }
  }
  if (!sets.length) return;
  vals.push(id);
  await query(`UPDATE dining_tables SET ${sets.join(', ')} WHERE id = ?`, vals);
}

async function deleteTable(id) {
  await query('DELETE FROM dining_tables WHERE id = ?', [id]);
}

// ── 猫咪 CRUD ──
async function createCat({ store_id, name, breed, personality_tags, health_status, weight_kg }) {
  const result = await query(
    `INSERT INTO cats (store_id, name, breed, personality_tags, health_status, weight_kg) VALUES (?, ?, ?, ?, ?, ?)`,
    [store_id, name, breed, JSON.stringify(personality_tags || []), health_status || 'healthy', weight_kg || null]
  );
  return { id: result.insertId, store_id, name, breed };
}

async function updateCat(id, fields) {
  const allowed = ['name', 'breed', 'personality_tags', 'health_status', 'weight_kg', 'last_vaccine_at'];
  const sets = []; const vals = [];
  for (const [k, v] of Object.entries(fields)) {
    if (allowed.includes(k) && v !== undefined) {
      sets.push(`${k} = ?`);
      vals.push(k === 'personality_tags' ? JSON.stringify(v) : v);
    }
  }
  if (!sets.length) return;
  vals.push(id);
  await query(`UPDATE cats SET ${sets.join(', ')} WHERE id = ?`, vals);
}

async function deleteCat(id) {
  await query('DELETE FROM cats WHERE id = ?', [id]);
}

async function updateCatPhoto(id, photoUrl) {
  await query('UPDATE cats SET photo_url = ? WHERE id = ?', [photoUrl, id]);
}

// ── 菜品 CRUD ──
async function createMenuItem({ store_id, name, category, price_cents, tags }) {
  const result = await query(
    `INSERT INTO menu_items (store_id, name, category, price_cents, tags) VALUES (?, ?, ?, ?, ?)`,
    [store_id, name, category, price_cents, JSON.stringify(tags || [])]
  );
  return { id: result.insertId, store_id, name, category, price_cents };
}

async function updateMenuItem(id, fields) {
  const allowed = ['name', 'category', 'price_cents', 'tags', 'status'];
  const sets = []; const vals = [];
  for (const [k, v] of Object.entries(fields)) {
    if (allowed.includes(k) && v !== undefined) {
      sets.push(`${k} = ?`);
      vals.push(k === 'tags' ? JSON.stringify(v) : v);
    }
  }
  if (!sets.length) return;
  vals.push(id);
  await query(`UPDATE menu_items SET ${sets.join(', ')} WHERE id = ?`, vals);
}

async function deleteMenuItem(id) {
  await query('DELETE FROM menu_items WHERE id = ?', [id]);
}

async function updateMenuItemPhoto(id, photoUrl) {
  await query('UPDATE menu_items SET photo_url = ? WHERE id = ?', [photoUrl, id]);
}

// ── 用户 CRUD ──
async function listUsers({ role } = {}) {
  let sql = 'SELECT * FROM users';
  const params = [];
  if (role) {
    const roles = role.split(',').map(r => r.trim()).filter(Boolean);
    if (roles.length === 1) {
      sql += ' WHERE role = ?';
      params.push(roles[0]);
    } else if (roles.length > 1) {
      sql += ` WHERE role IN (${roles.map(() => '?').join(',')})`;
      params.push(...roles);
    }
  }
  sql += ' ORDER BY id';
  return query(sql, params);
}

async function createUser({ name, mobile_number, role, member_level, points, preferences }) {
  const result = await query(
    `INSERT INTO users (name, mobile_number, role, member_level, points, preferences) VALUES (?, ?, ?, ?, ?, ?)`,
    [name, mobile_number, role || 'customer', member_level || 'silver', points || 0, JSON.stringify(preferences || [])]
  );
  return { id: result.insertId, name, mobile_number, role };
}

async function updateUser(id, fields) {
  const allowed = ['name', 'mobile_number', 'role', 'member_level', 'points', 'preferences'];
  const sets = []; const vals = [];
  for (const [k, v] of Object.entries(fields)) {
    if (allowed.includes(k) && v !== undefined) {
      sets.push(`${k} = ?`);
      vals.push(k === 'preferences' ? JSON.stringify(v) : v);
    }
  }
  if (!sets.length) return;
  vals.push(id);
  await query(`UPDATE users SET ${sets.join(', ')} WHERE id = ?`, vals);
}

async function deleteUser(id) {
  await query('DELETE FROM users WHERE id = ?', [id]);
}

// ── 评价 ──
async function listReviews(storeId) {
  const { rows } = await query(
    `
      SELECT r.*, u.name AS user_name
      FROM reviews r
      LEFT JOIN users u ON u.id = r.user_id
      WHERE (? IS NULL OR r.store_id = ?)
      ORDER BY r.created_at DESC
      LIMIT 50
    `,
    [storeId || null, storeId || null]
  );
  return rows;
}

// ── 审计日志 ──
async function insertAuditLog({ userId, userName, action, targetTable, targetId, detail, ipAddress }) {
  await query(
    `INSERT INTO audit_logs (user_id, user_name, action, target_table, target_id, detail, ip_address)
     VALUES (?, ?, ?, ?, ?, ?, ?)`,
    [userId || null, userName || null, action, targetTable, targetId || null, detail ? JSON.stringify(detail) : null, ipAddress || null]
  );
}

async function listAuditLogs({ action, targetTable, startDate, endDate, limit = 100 } = {}) {
  let sql = "SELECT * FROM audit_logs WHERE 1=1";
  const params = [];
  if (action) { sql += " AND action = ?"; params.push(action); }
  if (targetTable) { sql += " AND target_table = ?"; params.push(targetTable); }
  if (startDate) { sql += " AND created_at >= ?"; params.push(startDate); }
  if (endDate) { sql += " AND created_at <= ?"; params.push(endDate); }
  sql += " ORDER BY created_at DESC LIMIT ?";
  params.push(Number(limit) || 100);
  const rows = await query(sql, params);
  return rows.map(r => ({
    ...r,
    detail: typeof r.detail === 'string' ? JSON.parse(r.detail) : r.detail,
  }));
}

module.exports = {
  addCatHealthRecord,
  createCat,
  createMenuItem,
  createOrder,
  createReservation,
  createStore,
  createTable,
  createUser,
  deleteCat,
  deleteMenuItem,
  updateCatPhoto,
  updateMenuItemPhoto,
  deleteStore,
  deleteTable,
  deleteUser,
  getDashboardSummary,
  getRecommendations,
  insertAuditLog,
  listAuditLogs,
  listCatHealthRecords,
  listCats,
  listMenuItems,
  listOrders,
  listReservations,
  listReviews,
  listStores,
  listTables,
  listUsers,
  updateCat,
  updateMenuItem,
  updateReservationStatus,
  updateStore,
  updateTable,
  updateUser,
  upsertDemoUser,
};
