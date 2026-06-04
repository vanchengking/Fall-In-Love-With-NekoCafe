package com.nekocafe.domain;

import com.nekocafe.common.ApiException;
import com.nekocafe.dto.ReservationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationValidatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 4, 12, 0);

    private ReservationRequest req(String date, String time, Integer party, String mobile) {
        return new ReservationRequest("林小满", mobile, 1L, null, null, date, time, party, "靠窗", List.of("quiet"));
    }

    @Test
    @DisplayName("合法预约通过并归一化手机号")
    void validReservation() {
        ReservationValidator.Normalized n = ReservationValidator.validate(
                req("2026-06-05", "18:30", 2, "138-0000-0001"), NOW);
        assertEquals("13800000001", n.mobileNumber());
        assertEquals(2, n.partySize());
        assertEquals("2026-06-05", n.reservationDate());
    }

    @Test
    @DisplayName("缺少必填字段抛 400")
    void missingField() {
        ReservationRequest bad = new ReservationRequest(
                null, "13800000001", 1L, null, null, "2026-06-05", "18:30", 2, null, null);
        assertThrows(ApiException.class, () -> ReservationValidator.validate(bad, NOW));
    }

    @Test
    @DisplayName("过去时间被拒绝")
    void pastReservationRejected() {
        assertThrows(ApiException.class,
                () -> ReservationValidator.validate(req("2020-01-01", "18:30", 2, "13800000001"), NOW));
    }

    @Test
    @DisplayName("营业时段外被拒绝")
    void outsideBusinessHours() {
        assertThrows(ApiException.class,
                () -> ReservationValidator.validate(req("2026-06-05", "09:00", 2, "13800000001"), NOW));
        assertThrows(ApiException.class,
                () -> ReservationValidator.validate(req("2026-06-05", "22:00", 2, "13800000001"), NOW));
    }

    @Test
    @DisplayName("人数与手机号边界校验")
    void boundaryChecks() {
        assertThrows(ApiException.class,
                () -> ReservationValidator.validate(req("2026-06-05", "18:30", 0, "13800000001"), NOW));
        assertThrows(ApiException.class,
                () -> ReservationValidator.validate(req("2026-06-05", "18:30", 13, "13800000001"), NOW));
        assertThrows(ApiException.class,
                () -> ReservationValidator.validate(req("2026-06-05", "18:30", 2, "123"), NOW));
    }
}
