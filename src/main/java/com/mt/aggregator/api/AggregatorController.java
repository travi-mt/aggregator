package com.mt.aggregator.api;

import com.mt.aggregator.dto.ProductResponse;
import com.mt.aggregator.service.AggregatorService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/aggregate")
public class AggregatorController {

    private final AggregatorService service;

    public AggregatorController(AggregatorService service) {
        this.service = service;
    }

    @GetMapping("/product-info/{productId}")
    public ProductResponse getProductInfo(
            @PathVariable("productId") String productId,
            @RequestParam(value = "customerId", required = false) String customerId) {
        return service.aggregate(productId, customerId);
    }
}
