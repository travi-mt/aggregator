package com.mt.aggregator.provider.mock;

import com.mt.aggregator.exception.ProductNotFoundException;
import com.mt.aggregator.provider.CatalogProvider;
import com.mt.aggregator.dto.CatalogDto;
import com.mt.aggregator.provider.BaseDataProvider;

import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class MockCatalogProvider extends BaseDataProvider implements CatalogProvider {

    private final static String VALID_PRODUCT_ID = "123";

    @Override
    public CatalogDto getCatalog(String productId) {
        // Typical latency: 50 ms.
        // Timeout assumption: 2x typical latency = 100 ms.
        // 1% of requests are simulated as unusually slow.
        jitter(50, 100, 0.01);

        reliability(0.001, "Catalog upstream failure"); // 99.9%

        if (!productExists(productId)) {
            throw new ProductNotFoundException(
                    "Product not found: " + productId);
        }

        return new CatalogDto(
                productId,
                "Product " + productId,
                "Sample description",
                List.of("Spec A", "Spec B", "Spec C"),
                List.of("img1.jpg", "img2.jpg"));
    }

    private boolean productExists(String productId) {
        return VALID_PRODUCT_ID.equals(productId);
    }

}
