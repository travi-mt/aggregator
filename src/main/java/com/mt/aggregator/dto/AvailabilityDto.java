package com.mt.aggregator.dto;

public record AvailabilityDto(
        String id,
        int stockLevel,
        String warehouseLocation,
        String expectedDelivery) {
}
