import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  stages: [
    { duration: "30s", target: 20 },
    { duration: "1m", target: 80 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<350"],
  },
};

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export default function () {
  const date = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString().slice(0, 10);
  const payload = JSON.stringify({
    data: {
      customerName: `压测用户${__VU}-${__ITER}`,
      mobileNumber: `139${String(__VU).padStart(4, "0")}${String(__ITER).padStart(4, "0")}`,
      storeId: 1,
      reservationDate: date,
      reservationTime: "20:30",
      partySize: 2,
      preferences: ["quiet", "window"],
    },
  });

  const response = http.post(`${BASE_URL}/api/reservations`, payload, {
    headers: { "Content-Type": "application/json" },
  });

  check(response, {
    "status is 201 or slot conflict": (res) => res.status === 201 || res.status === 409,
    "p95 target sample": (res) => res.timings.duration < 350,
  });

  sleep(1);
}
