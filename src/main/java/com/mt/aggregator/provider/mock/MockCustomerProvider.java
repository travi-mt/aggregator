package com.mt.aggregator.provider.mock;

import org.springframework.stereotype.Component;

import com.mt.aggregator.provider.BaseDataProvider;
import com.mt.aggregator.dto.CustomerDto;
import com.mt.aggregator.provider.CustomerProvider;
import com.mt.aggregator.exception.CustomerNotFoundException;

@Component
public class MockCustomerProvider extends BaseDataProvider implements CustomerProvider {

    @Override
    public CustomerDto getCustomer(String customerId) {
        // Typical latency: 60 ms.
        // Timeout assumption: 2x typical latency = 120 ms.
        // 1% of requests are simulated as unusually slow.
        jitter(60, 120, 0.01);

        reliability(0.02, "Customer upstream failure"); // 98%

        return switch (customerId) {
            case "CUST-001" -> new CustomerDto(
                    "CUST-001",
                    "VIP",
                    "Prefers OEM parts");

            case "CUST-002" -> new CustomerDto(
                    "CUST-002",
                    "PROFESSIONAL_WORKSHOP",
                    "Prefers OEM parts");

            default -> throw new CustomerNotFoundException(
                    "Customer not found: " + customerId);
        };
    }
}
