package com.nekocafe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderRequest(Long reservationId, List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(Long menuItemId, Integer quantity) {
    }
}
