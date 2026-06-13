const express = require("express");
const { asyncHandler } = require("../middleware/asyncHandler");
const repository = require("./repository");

const router = express.Router();

router.post(
  "/auth/login",
  asyncHandler(async (req, res) => {
    const user = await repository.upsertDemoUser(req.body.data || req.body);
    res.json({
      data: {
        user,
        token: `demo-token-${user.id}-${user.role}`,
      },
    });
  })
);

router.get(
  "/stores",
  asyncHandler(async (req, res) => {
    res.json({ data: await repository.listStores() });
  })
);

router.post(
  "/stores",
  asyncHandler(async (req, res) => {
    const store = await repository.createStore(req.body.data || req.body);
    res.status(201).json({ data: store });
  })
);

router.patch(
  "/stores/:id",
  asyncHandler(async (req, res) => {
    await repository.updateStore(req.params.id, req.body.data || req.body);
    res.json({ data: { success: true } });
  })
);

router.delete(
  "/stores/:id",
  asyncHandler(async (req, res) => {
    await repository.deleteStore(req.params.id);
    res.json({ data: { success: true } });
  })
);

router.get(
  "/tables",
  asyncHandler(async (req, res) => {
    const tables = await repository.listTables({
      storeId: req.query.storeId,
      reservationDate: req.query.date,
      reservationTime: req.query.time,
      partySize: req.query.partySize,
    });
    res.json({ data: tables });
  })
);

router.post(
  "/tables",
  asyncHandler(async (req, res) => {
    const table = await repository.createTable(req.body.data || req.body);
    res.status(201).json({ data: table });
  })
);

router.patch(
  "/tables/:id",
  asyncHandler(async (req, res) => {
    await repository.updateTable(req.params.id, req.body.data || req.body);
    res.json({ data: { success: true } });
  })
);

router.delete(
  "/tables/:id",
  asyncHandler(async (req, res) => {
    await repository.deleteTable(req.params.id);
    res.json({ data: { success: true } });
  })
);

router.get(
  "/menu-items",
  asyncHandler(async (req, res) => {
    res.json({ data: await repository.listMenuItems(req.query.storeId) });
  })
);

router.post(
  "/menu-items",
  asyncHandler(async (req, res) => {
    const item = await repository.createMenuItem(req.body.data || req.body);
    res.status(201).json({ data: item });
  })
);

router.patch(
  "/menu-items/:id",
  asyncHandler(async (req, res) => {
    await repository.updateMenuItem(req.params.id, req.body.data || req.body);
    res.json({ data: { success: true } });
  })
);

router.delete(
  "/menu-items/:id",
  asyncHandler(async (req, res) => {
    await repository.deleteMenuItem(req.params.id);
    res.json({ data: { success: true } });
  })
);

router.patch(
  "/menu-items/:id/photo",
  asyncHandler(async (req, res) => {
    await repository.updateMenuItemPhoto(req.params.id, (req.body.data || req.body).photoUrl);
    res.json({ data: { success: true } });
  })
);

router.get(
  "/cats",
  asyncHandler(async (req, res) => {
    res.json({ data: await repository.listCats(req.query.storeId) });
  })
);

router.post(
  "/cats",
  asyncHandler(async (req, res) => {
    const cat = await repository.createCat(req.body.data || req.body);
    res.status(201).json({ data: cat });
  })
);

router.patch(
  "/cats/:id",
  asyncHandler(async (req, res) => {
    await repository.updateCat(req.params.id, req.body.data || req.body);
    res.json({ data: { success: true } });
  })
);

router.delete(
  "/cats/:id",
  asyncHandler(async (req, res) => {
    await repository.deleteCat(req.params.id);
    res.json({ data: { success: true } });
  })
);

router.patch(
  "/cats/:id/photo",
  asyncHandler(async (req, res) => {
    await repository.updateCatPhoto(req.params.id, (req.body.data || req.body).photoUrl);
    res.json({ data: { success: true } });
  })
);

router.get(
  "/cat-health-records",
  asyncHandler(async (req, res) => {
    res.json({ data: await repository.listCatHealthRecords(req.query.catId) });
  })
);

router.post(
  "/cat-health-records",
  asyncHandler(async (req, res) => {
    const record = await repository.addCatHealthRecord(req.body.data || req.body);
    res.status(201).json({ data: record });
  })
);

router.get(
  "/reservations",
  asyncHandler(async (req, res) => {
    const reservations = await repository.listReservations({
      date: req.query.date,
      mobileNumber: req.query.mobileNumber,
      storeId: req.query.storeId,
      status: req.query.status,
    });
    res.json({ data: reservations });
  })
);

router.post(
  "/reservations",
  asyncHandler(async (req, res) => {
    const reservation = await repository.createReservation(req.body.data || req.body);
    res.status(201).json({ data: reservation });
  })
);

router.patch(
  "/reservations/:id/status",
  asyncHandler(async (req, res) => {
    const status = (req.body.data || req.body).status;
    const reservation = await repository.updateReservationStatus(req.params.id, status);
    res.json({ data: reservation });
  })
);

router.get(
  "/orders",
  asyncHandler(async (req, res) => {
    res.json({ data: await repository.listOrders(req.query.storeId) });
  })
);

router.post(
  "/orders",
  asyncHandler(async (req, res) => {
    const order = await repository.createOrder(req.body.data || req.body);
    res.status(201).json({ data: order });
  })
);

router.get(
  "/recommendations",
  asyncHandler(async (req, res) => {
    const preferences = typeof req.query.preferences === "string"
      ? req.query.preferences.split(",").map((tag) => tag.trim()).filter(Boolean)
      : [];
    const recommendations = await repository.getRecommendations({
      userId: req.query.userId,
      storeId: req.query.storeId,
      preferences,
    });
    res.json({ data: recommendations });
  })
);

router.get(
  "/users",
  asyncHandler(async (req, res) => {
    res.json({ data: await repository.listUsers({ role: req.query.role }) });
  })
);

router.post(
  "/users",
  asyncHandler(async (req, res) => {
    const user = await repository.createUser(req.body.data || req.body);
    res.status(201).json({ data: user });
  })
);

router.patch(
  "/users/:id",
  asyncHandler(async (req, res) => {
    await repository.updateUser(req.params.id, req.body.data || req.body);
    res.json({ data: { success: true } });
  })
);

router.delete(
  "/users/:id",
  asyncHandler(async (req, res) => {
    await repository.deleteUser(req.params.id);
    res.json({ data: { success: true } });
  })
);

router.get(
  "/reviews",
  asyncHandler(async (req, res) => {
    res.json({ data: await repository.listReviews(req.query.storeId) });
  })
);

router.get(
  "/dashboard/summary",
  asyncHandler(async (req, res) => {
    res.json({ data: await repository.getDashboardSummary(req.query.storeId) });
  })
);

router.get(
  "/audit-logs",
  asyncHandler(async (req, res) => {
    const logs = await repository.listAuditLogs({
      action: req.query.action,
      targetTable: req.query.targetTable,
      startDate: req.query.startDate,
      endDate: req.query.endDate,
      limit: req.query.limit,
    });
    res.json({ data: logs });
  })
);

router.post(
  "/audit-logs",
  asyncHandler(async (req, res) => {
    const body = req.body.data || req.body;
    const log = await repository.insertAuditLog({
      userId: body.user_id,
      userName: body.user_name,
      action: body.action,
      targetTable: body.target_table,
      targetId: body.target_id,
      detail: body.detail,
      ipAddress: req.ip,
    });
    res.status(201).json({ data: { success: true } });
  })
);

module.exports = { apiRouter: router };
