package com.mt.aggregator.service;

import com.mt.aggregator.dto.ProductResponse;
import com.mt.aggregator.exception.CatalogUnavailableException;
import com.mt.aggregator.exception.ProductNotFoundException;
import com.mt.aggregator.exception.CustomerNotFoundException;
import com.mt.aggregator.exception.UpstreamInterruptedException;
import com.mt.aggregator.exception.UpstreamUnavailableException;

import com.mt.aggregator.dto.AvailabilityDto;
import com.mt.aggregator.dto.CatalogDto;
import com.mt.aggregator.dto.CustomerDto;
import com.mt.aggregator.dto.PricingDto;
import com.mt.aggregator.provider.AvailabilityProvider;
import com.mt.aggregator.provider.CatalogProvider;
import com.mt.aggregator.provider.CustomerProvider;
import com.mt.aggregator.provider.PricingProvider;

import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@Service
public class AggregatorService {

        private final CatalogProvider catalog;
        private final PricingProvider pricing;
        private final AvailabilityProvider availability;
        private final CustomerProvider customer;

        public AggregatorService(
                        CatalogProvider catalog,
                        PricingProvider pricing,
                        AvailabilityProvider availability,
                        CustomerProvider customer) {
                this.catalog = catalog;
                this.pricing = pricing;
                this.availability = availability;
                this.customer = customer;
        }

        public ProductResponse aggregate(String productId, String customerId) {

                final CatalogDto catalogDto;

                // Catalog is required.
                // Upstream failure means the whole aggregation must fail.
                try {
                        catalogDto = catalog.getCatalog(productId);
                } catch (ProductNotFoundException ex) {
                        throw ex;
                } catch (UpstreamUnavailableException ex) {
                        throw new CatalogUnavailableException(
                                        "Catalog unavailable. Try again in a few seconds.",
                                        ex);
                } catch (UpstreamInterruptedException ex) {
                        throw new CatalogUnavailableException(
                                        "Catalog request was interrupted.",
                                        ex);
                }

                try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                        // No customerId -> no customer call.
                        CompletableFuture<CustomerDto> customerFuture = customerId == null
                                        ? CompletableFuture.completedFuture(null)
                                        : callUpstreamOrNull(
                                                        () -> customer.getCustomer(customerId),
                                                        executor);

                        // Optional and independent of customer
                        CompletableFuture<AvailabilityDto> availabilityFuture = callUpstreamOrNull(
                                        () -> availability.getAvailability(productId),
                                        executor);

                        // Pricing is ALWAYS called.
                        //
                        // No customerId:
                        // pricing(productId, null) starts immediately.
                        //
                        // CustomerId provided:
                        // pricing waits for customer and uses it if available.
                        CompletableFuture<PricingDto> pricingFuture = customerId == null
                                        ? callUpstreamOrNull(
                                                        () -> pricing.getPricing(productId, null),
                                                        executor)
                                        : customerFuture.thenCompose(
                                                        customer -> callUpstreamOrNull(
                                                                        () -> pricing.getPricing(productId, customer),
                                                                        executor));

                        return new ProductResponse(
                                        catalogDto.id(),
                                        catalogDto.name(),
                                        catalogDto.description(),
                                        catalogDto.specs(),
                                        catalogDto.images(),
                                        pricingFuture.join(),
                                        availabilityFuture.join(),
                                        customerFuture.join());
                }
        }

        /*
         * Optional upstream failures are intentionally mapped to null.
         * The aggregator cares about the outcome, not the underlying cause,
         * and must return a consistent response when optional data is unavailable.
         */
        private <T> CompletableFuture<T> callUpstreamOrNull(
                        Supplier<T> supplier,
                        Executor executor) {

                return CompletableFuture
                                .supplyAsync(supplier, executor)
                                .exceptionally(ex -> {
                                        Throwable cause = unwrapCompletionException(ex);

                                        if (cause instanceof CustomerNotFoundException
                                                        || cause instanceof UpstreamUnavailableException
                                                        || cause instanceof UpstreamInterruptedException) {
                                                return null;
                                        }

                                        throw new CompletionException(cause);
                                });
        }

        private Throwable unwrapCompletionException(Throwable ex) {
                while (ex instanceof CompletionException
                                || ex instanceof ExecutionException) {
                        ex = ex.getCause();
                }
                return ex;
        }
}
