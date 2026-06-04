package com.nekocafe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CatHealthRequest(
        Long catId,
        BigDecimal weightKg,
        String vaccineNote,
        String interactionNote) {
}
