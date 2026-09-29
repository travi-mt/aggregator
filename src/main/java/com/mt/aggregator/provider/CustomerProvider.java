package com.mt.aggregator.provider;

import com.mt.aggregator.dto.CustomerDto;
import com.mt.aggregator.exception.CustomerNotFoundException;

public interface CustomerProvider {
    CustomerDto getCustomer(String customerId) throws CustomerNotFoundException;
}
