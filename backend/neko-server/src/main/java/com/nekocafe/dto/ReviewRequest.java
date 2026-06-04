package com.nekocafe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewRequest(
        Long reservationId,
        Long storeId,
        Long catId,
        Integer rating,
        String content) {
}
