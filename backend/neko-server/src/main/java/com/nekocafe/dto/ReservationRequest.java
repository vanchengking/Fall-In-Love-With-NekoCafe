package com.nekocafe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationRequest(
        String customerName,
        String mobileNumber,
        Long storeId,
        Long tableId,
        Long recommendedCatId,
        String reservationDate,
        String reservationTime,
        Integer partySize,
        String note,
        List<String> preferences) {
}
