package com.nekocafe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationRescheduleRequest(
        Long storeId,
        Long tableId,
        Long recommendedCatId,
        String reservationDate,
        String reservationTime,
        Integer partySize,
        String note) {
}
