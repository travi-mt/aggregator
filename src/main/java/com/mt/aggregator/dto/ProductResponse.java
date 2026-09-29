package com.mt.aggregator.dto;

import java.util.List;

public record ProductResponse(
        String id,
        String name,
        String description,
        List<String> specs,
        List<String> images,

        PricingDto pricing,
        AvailabilityDto availability,
        CustomerDto customer) {
}
