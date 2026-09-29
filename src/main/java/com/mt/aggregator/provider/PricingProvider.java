package com.mt.aggregator.provider;

import com.mt.aggregator.dto.PricingDto;
import com.mt.aggregator.dto.CustomerDto;

public interface PricingProvider {
    PricingDto getPricing(String productId, CustomerDto customer);
}
