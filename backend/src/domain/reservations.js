const ACTIVE_STATUSES = new Set(["booked", "seated"]);
const VALID_STATUSES = new Set(["booked", "seated", "finished", "cancelled", "no_show"]);

const ALLOWED_TRANSITIONS = {
  booked: new Set(["seated", "cancelled", "no_show"]),
  seated: new Set(["finished"]),
  finished: new Set([]),
  cancelled: new Set([]),
  no_show: new Set([]),
};

function badRequest(message) {
  const error = new Error(message);
  error.status = 400;
  return error;
}

function normalizeMobileNumber(value) {
  return String(value || "").replace(/\D/g, "");
}

function assertReservationPayload(payload, now = new Date()) {
  const required = [
    "customerName",
    "mobileNumber",
    "storeId",
    "reservationDate",
    "reservationTime",
    "partySize",
  ];

  for (const field of required) {
    if (payload[field] === undefined || payload[field] === null || payload[field] === "") {
      throw badRequest(`${field} is required`);
    }
  }

  const mobile = normalizeMobileNumber(payload.mobileNumber);
  if (mobile.length < 8) {
    throw badRequest("mobileNumber must contain at least 8 digits");
  }

  const partySize = Number(payload.partySize);
  if (!Number.isInteger(partySize) || partySize <= 0 || partySize > 12) {
    throw badRequest("partySize must be an integer between 1 and 12");
  }

  if (!/^\d{4}-\d{2}-\d{2}$/.test(payload.reservationDate)) {
    throw badRequest("reservationDate must use YYYY-MM-DD");
  }

  if (!/^\d{2}:\d{2}$/.test(payload.reservationTime)) {
    throw badRequest("reservationTime must use HH:mm");
  }

  const reservationAt = new Date(`${payload.reservationDate}T${payload.reservationTime}:00+08:00`);
  if (Number.isNaN(reservationAt.getTime())) {
    throw badRequest("reservation date/time is invalid");
  }

  if (reservationAt < now) {
    throw badRequest("reservation must be in the future");
  }

  const [hour, minute] = payload.reservationTime.split(":").map(Number);
  const minutes = hour * 60 + minute;
  const opensAt = 10 * 60 + 30;
  const lastReservationAt = 21 * 60 + 30;
  if (minutes < opensAt || minutes > lastReservationAt) {
    throw badRequest("reservationTime must be between 10:30 and 21:30");
  }

  return {
    customerName: String(payload.customerName).trim(),
    mobileNumber: mobile,
    storeId: Number(payload.storeId),
    tableId: payload.tableId ? Number(payload.tableId) : null,
    recommendedCatId: payload.recommendedCatId ? Number(payload.recommendedCatId) : null,
    reservationDate: payload.reservationDate,
    reservationTime: payload.reservationTime,
    partySize,
    note: payload.note ? String(payload.note).trim() : null,
    preferences: Array.isArray(payload.preferences) ? payload.preferences.map(String) : [],
  };
}

function canTransitionReservation(currentStatus, nextStatus) {
  if (!VALID_STATUSES.has(nextStatus)) {
    return false;
  }

  return ALLOWED_TRANSITIONS[currentStatus]?.has(nextStatus) || false;
}

function assertStatusTransition(currentStatus, nextStatus) {
  if (!VALID_STATUSES.has(nextStatus)) {
    throw badRequest(`unknown reservation status: ${nextStatus}`);
  }

  if (!canTransitionReservation(currentStatus, nextStatus)) {
    throw badRequest(`reservation cannot transition from ${currentStatus} to ${nextStatus}`);
  }
}

module.exports = {
  ACTIVE_STATUSES,
  VALID_STATUSES,
  assertReservationPayload,
  assertStatusTransition,
  canTransitionReservation,
  normalizeMobileNumber,
};
