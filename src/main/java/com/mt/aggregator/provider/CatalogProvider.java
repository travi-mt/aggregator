package com.mt.aggregator.provider;

import com.mt.aggregator.dto.CatalogDto;
import com.mt.aggregator.exception.ProductNotFoundException;

public interface CatalogProvider {
    CatalogDto getCatalog(String productId) throws ProductNotFoundException;;
}
