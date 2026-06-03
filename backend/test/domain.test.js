const test = require("node:test");
const assert = require("node:assert/strict");
const {
  assertReservationPayload,
  canTransitionReservation,
  normalizeMobileNumber,
} = require("../src/domain/reservations");
const { rankRecommendations, scoreByTags } = require("../src/domain/recommendations");

test("normalizes mobile numbers", () => {
  assert.equal(normalizeMobileNumber("(138) 0000-0001"), "13800000001");
});

test("validates and normalizes reservation payload", () => {
  const payload = assertReservationPayload(
    {
      customerName: " 林小满 ",
      mobileNumber: "138-0000-0001",
      storeId: "1",
      reservationDate: "2026-06-04",
      reservationTime: "18:30",
      partySize: "2",
      preferences: ["quiet"],
    },
    new Date("2026-06-03T09:00:00+08:00")
  );

  assert.equal(payload.customerName, "林小满");
  assert.equal(payload.mobileNumber, "13800000001");
  assert.equal(payload.partySize, 2);
});

test("rejects invalid status transitions", () => {
  assert.equal(canTransitionReservation("booked", "seated"), true);
  assert.equal(canTransitionReservation("seated", "booked"), false);
  assert.equal(canTransitionReservation("finished", "cancelled"), false);
});

test("scores recommendations by matching tags", () => {
  assert.equal(scoreByTags(["quiet", "sweet"], ["quiet", "coffee"]), 10);

  const result = rankRecommendations({
    preferences: ["quiet", "window"],
    cats: [
      { id: 1, name: "拿铁", personality_tags: ["active"], health_status: "healthy" },
      { id: 2, name: "团子", personality_tags: ["quiet"], health_status: "healthy" },
    ],
    tables: [
      { id: 1, code: "B01", area: "main", cat_zone: false, seats: 4 },
      { id: 2, code: "A01", area: "window", cat_zone: true, seats: 2 },
    ],
    menuItems: [],
  });

  assert.equal(result.cat.name, "团子");
  assert.equal(result.tables[0].code, "A01");
});
