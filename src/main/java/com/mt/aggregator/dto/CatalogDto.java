package com.mt.aggregator.dto;

import java.util.List;

public record CatalogDto(
        String id,
        String name,
        String description,
        List<String> specs,
        List<String> images) {
}