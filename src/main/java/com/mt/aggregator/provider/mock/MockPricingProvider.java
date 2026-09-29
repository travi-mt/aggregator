package com.mt.aggregator.provider.mock;

import org.springframework.stereotype.Component;

import com.mt.aggregator.provider.BaseDataProvider;
import com.mt.aggregator.provider.PricingProvider;
import com.mt.aggregator.dto.PricingDto;
import com.mt.aggregator.dto.CustomerDto;

import java.math.BigDecimal;

@Component
public class MockPricingProvider extends BaseDataProvider implements PricingProvider {

    @Override
    public PricingDto getPricing(String productId, CustomerDto customer) {
        // Typical latency: 80 ms.
        // Timeout assumption: 2x typical latency = 160 ms.
        // 1% of requests are simulated as unusually slow.
        jitter(80, 160, 0.01);
        reliability(0.001, "Pricing upstream failure"); // 99.9%

        BigDecimal base = BigDecimal.valueOf(100);

        BigDecimal discount = BigDecimal.ZERO;

        if (customer != null) {
            discount = switch (customer.segment()) {
                case "VIP" -> BigDecimal.valueOf(20);
                case "PROFESSIONAL_WORKSHOP" -> BigDecimal.valueOf(10);
                default -> BigDecimal.ZERO;
            };
        }

        BigDecimal finalPrice = base.subtract(discount);

        return new PricingDto(productId, base, discount, finalPrice);
    }

}
