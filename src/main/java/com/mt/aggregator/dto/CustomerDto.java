package com.mt.aggregator.dto;

public record CustomerDto(
        String customerId,
        String segment,
        String preferences) {
}