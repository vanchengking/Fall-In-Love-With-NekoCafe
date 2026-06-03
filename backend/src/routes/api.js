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

router.get(
  "/menu-items",
  asyncHandler(async (req, res) => {
    res.json({ data: await repository.listMenuItems(req.query.storeId) });
  })
);

router.get(
  "/cats",
  asyncHandler(async (req, res) => {
    res.json({ data: await repository.listCats(req.query.storeId) });
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
  "/dashboard/summary",
  asyncHandler(async (req, res) => {
    res.json({ data: await repository.getDashboardSummary(req.query.storeId) });
  })
);

module.exports = { apiRouter: router };
