package com.mt.aggregator.dto;

import java.math.BigDecimal;

public record PricingDto(
                String id,
                BigDecimal basePrice,
                BigDecimal customerDiscount,
                BigDecimal finalPrice) {
}
