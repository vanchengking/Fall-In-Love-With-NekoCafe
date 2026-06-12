package com.nekocafe.domain;

import com.nekocafe.common.ApiException;
import com.nekocafe.dto.ReservationRequest;
import com.nekocafe.dto.ReservationRescheduleRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 预约请求校验与归一化。营业时段 10:30-21:30，预约必须在未来，人数 1-12。
 */
public final class ReservationValidator {

    private static final Pattern DATE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern TIME = Pattern.compile("^\\d{2}:\\d{2}$");
    private static final int OPENS_AT = 10 * 60 + 30;
    private static final int LAST_AT = 21 * 60 + 30;

    private ReservationValidator() {
    }

    public record Normalized(
            String customerName,
            String mobileNumber,
            Long storeId,
            Long tableId,
            Long recommendedCatId,
            String reservationDate,
            String reservationTime,
            int partySize,
            String note,
            List<String> preferences) {
    }

    public record RescheduleNormalized(
            Long storeId,
            Long tableId,
            Long recommendedCatId,
            String reservationDate,
            String reservationTime,
            int partySize,
            String note) {
    }

    public static String normalizeMobile(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    public static Normalized validate(ReservationRequest req, LocalDateTime now) {
        if (req == null) {
            throw ApiException.badRequest("request body is required");
        }
        requireText(req.customerName(), "customerName");
        requireText(req.mobileNumber(), "mobileNumber");
        require(req.storeId() != null, "storeId");
        requireText(req.reservationDate(), "reservationDate");
        requireText(req.reservationTime(), "reservationTime");
        require(req.partySize() != null, "partySize");

        String mobile = normalizeMobile(req.mobileNumber());
        if (mobile.length() < 8) {
            throw ApiException.badRequest("mobileNumber must contain at least 8 digits");
        }

        int partySize = req.partySize();
        if (partySize <= 0 || partySize > 12) {
            throw ApiException.badRequest("partySize must be an integer between 1 and 12");
        }

        if (!DATE.matcher(req.reservationDate()).matches()) {
            throw ApiException.badRequest("reservationDate must use YYYY-MM-DD");
        }
        if (!TIME.matcher(req.reservationTime()).matches()) {
            throw ApiException.badRequest("reservationTime must use HH:mm");
        }

        LocalDate date;
        LocalTime time;
        try {
            date = LocalDate.parse(req.reservationDate());
            String[] hm = req.reservationTime().split(":");
            time = LocalTime.of(Integer.parseInt(hm[0]), Integer.parseInt(hm[1]));
        } catch (Exception ex) {
            throw ApiException.badRequest("reservation date/time is invalid");
        }

        if (LocalDateTime.of(date, time).isBefore(now)) {
            throw ApiException.badRequest("reservation must be in the future");
        }

        int minutes = time.getHour() * 60 + time.getMinute();
        if (minutes < OPENS_AT || minutes > LAST_AT) {
            throw ApiException.badRequest("reservationTime must be between 10:30 and 21:30");
        }

        List<String> prefs = new ArrayList<>();
        if (req.preferences() != null) {
            for (String p : req.preferences()) {
                if (p != null) {
                    prefs.add(p);
                }
            }
        }

        return new Normalized(
                req.customerName().trim(),
                mobile,
                req.storeId(),
                req.tableId(),
                req.recommendedCatId(),
                req.reservationDate(),
                req.reservationTime(),
                partySize,
                req.note() == null ? null : req.note().trim(),
                prefs);
    }

    public static RescheduleNormalized validateReschedule(ReservationRescheduleRequest req, LocalDateTime now) {
        if (req == null) {
            throw ApiException.badRequest("request body is required");
        }
        require(req.storeId() != null, "storeId");
        requireText(req.reservationDate(), "reservationDate");
        requireText(req.reservationTime(), "reservationTime");
        require(req.partySize() != null, "partySize");

        int partySize = req.partySize();
        if (partySize <= 0 || partySize > 12) {
            throw ApiException.badRequest("partySize must be an integer between 1 and 12");
        }

        if (!DATE.matcher(req.reservationDate()).matches()) {
            throw ApiException.badRequest("reservationDate must use YYYY-MM-DD");
        }
        if (!TIME.matcher(req.reservationTime()).matches()) {
            throw ApiException.badRequest("reservationTime must use HH:mm");
        }

        LocalDate date;
        LocalTime time;
        try {
            date = LocalDate.parse(req.reservationDate());
            String[] hm = req.reservationTime().split(":");
            time = LocalTime.of(Integer.parseInt(hm[0]), Integer.parseInt(hm[1]));
        } catch (Exception ex) {
            throw ApiException.badRequest("reservation date/time is invalid");
        }

        if (LocalDateTime.of(date, time).isBefore(now)) {
            throw ApiException.badRequest("reservation must be in the future");
        }

        int minutes = time.getHour() * 60 + time.getMinute();
        if (minutes < OPENS_AT || minutes > LAST_AT) {
            throw ApiException.badRequest("reservationTime must be between 10:30 and 21:30");
        }

        return new RescheduleNormalized(
                req.storeId(),
                req.tableId(),
                req.recommendedCatId(),
                req.reservationDate(),
                req.reservationTime(),
                partySize,
                req.note() == null ? null : req.note().trim());
    }

    private static void require(boolean ok, String field) {
        if (!ok) {
            throw ApiException.badRequest(field + " is required");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(field + " is required");
        }
    }
}
