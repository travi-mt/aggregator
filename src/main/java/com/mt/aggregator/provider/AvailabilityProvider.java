package com.mt.aggregator.provider;

import com.mt.aggregator.dto.AvailabilityDto;

public interface AvailabilityProvider {
    AvailabilityDto getAvailability(String productId);
}
