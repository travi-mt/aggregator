package com.mt.aggregator.provider.mock;

import com.mt.aggregator.dto.AvailabilityDto;
import com.mt.aggregator.provider.AvailabilityProvider;
import com.mt.aggregator.provider.BaseDataProvider;

import org.springframework.stereotype.Component;

@Component
public class MockAvailabilityProvider extends BaseDataProvider implements AvailabilityProvider {

    @Override
    public AvailabilityDto getAvailability(String productId) {
        // Typical latency: 100 ms.
        // Timeout assumption: 2x typical latency = 200 ms.
        // 1% of requests are simulated as unusually slow.
        jitter(100, 200, 0.01);
        reliability(0.005, "Availability upstream failure"); // 99.5%

        return new AvailabilityDto(
                productId,
                12,
                "WH-01",
                "2026-10-01");
    }
}
