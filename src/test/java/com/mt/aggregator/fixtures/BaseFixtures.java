package com.mt.aggregator.fixtures;

import com.mt.aggregator.dto.AvailabilityDto;
import com.mt.aggregator.dto.CatalogDto;
import com.mt.aggregator.dto.CustomerDto;
import com.mt.aggregator.dto.PricingDto;

import java.math.BigDecimal;
import java.util.List;

public final class BaseFixtures {

    private BaseFixtures() {
    }

    public static CatalogDto getCatalogData() {
        return new CatalogDto(
                "123",
                "Product 123",
                "Sample description",
                List.of("A", "B"),
                List.of("img1", "img2"));
    }

    public static CustomerDto getVipCustomerData() {
        return new CustomerDto(
                "CUST-001",
                "VIP",
                "Prefers OEM");
    }

    public static CustomerDto getStandardCustomerData() {
        return new CustomerDto(
                "CUST-004",
                "STANDARD",
                "Prefers OEM");
    }

    public static PricingDto getVipPricingData() {
        return new PricingDto(
                "123",
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(80));
    }

    public static PricingDto getStandardPricingData() {
        return new PricingDto(
                "123",
                BigDecimal.valueOf(100),
                BigDecimal.ZERO,
                BigDecimal.valueOf(100));
    }

    public static AvailabilityDto getAvailabilityData() {
        return new AvailabilityDto(
                "123",
                10,
                "WH-01",
                "2026-10-01");
    }
}
