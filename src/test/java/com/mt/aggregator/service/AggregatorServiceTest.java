package com.mt.aggregator.service;

import com.mt.aggregator.dto.ProductResponse;
import com.mt.aggregator.dto.CustomerDto;
import com.mt.aggregator.dto.PricingDto;
import com.mt.aggregator.exception.CatalogUnavailableException;
import com.mt.aggregator.exception.UpstreamInterruptedException;
import com.mt.aggregator.exception.UpstreamUnavailableException;
import com.mt.aggregator.exception.CustomerNotFoundException;
import com.mt.aggregator.fixtures.BaseFixtures;
import com.mt.aggregator.provider.AvailabilityProvider;
import com.mt.aggregator.provider.CatalogProvider;
import com.mt.aggregator.provider.CustomerProvider;
import com.mt.aggregator.provider.PricingProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.mt.aggregator.fixtures.BaseFixtures.SAMPLE_PRODUCT_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AggregatorServiceTest {

        @Mock
        private CatalogProvider catalog;

        @Mock
        private PricingProvider pricing;

        @Mock
        private AvailabilityProvider availability;

        @Mock
        private CustomerProvider customer;

        @InjectMocks
        private AggregatorService sut;

        @Test
        void shouldReturnFullyAggregatedProductWhenAllUpstreamsSucceed() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(BaseFixtures.getVipCustomerData());

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, BaseFixtures.getVipCustomerData()))
                                .thenReturn(BaseFixtures.getVipPricingData());

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNotNull(result);
                assertEquals(SAMPLE_PRODUCT_ID, result.id());
                assertEquals("Product 123", result.name());
                assertEquals("Sample description", result.description());
                assertEquals(
                                BaseFixtures.getCatalogData().specs(),
                                result.specs());
                assertEquals(
                                BaseFixtures.getCatalogData().images(),
                                result.images());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().basePrice().intValue());
                assertEquals(20, result.pricing().customerDiscount().intValue());
                assertEquals(80, result.pricing().finalPrice().intValue());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());
        }

        @Test
        void shouldReturnProductWithoutPricingWhenPricingIsUnavailable() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(BaseFixtures.getVipCustomerData());

                when(pricing.getPricing(any(), any()))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Pricing upstream unavailable"));

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNotNull(result);
                assertEquals(SAMPLE_PRODUCT_ID, result.id());
                assertEquals("Product 123", result.name());

                assertNull(result.pricing());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());
        }

        @Test
        void shouldReturnProductWithoutPricingWhenPricingRequestIsInterrupted() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(BaseFixtures.getVipCustomerData());

                when(pricing.getPricing(any(), any()))
                                .thenThrow(new UpstreamInterruptedException(
                                                "Pricing request was interrupted",
                                                new InterruptedException()));

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNotNull(result);
                assertNull(result.pricing());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());
        }

        @Test
        void shouldReturnProductWithoutAvailabilityWhenAvailabilityIsUnavailable() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(BaseFixtures.getVipCustomerData());

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, BaseFixtures.getVipCustomerData()))
                                .thenReturn(BaseFixtures.getVipPricingData());

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Availability upstream unavailable"));

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNotNull(result);
                assertEquals(SAMPLE_PRODUCT_ID, result.id());

                assertNull(result.availability());

                assertNotNull(result.pricing());
                assertEquals(80, result.pricing().finalPrice().intValue());

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());
        }

        @Test
        void shouldReturnProductWithoutAvailabilityWhenAvailabilityRequestIsInterrupted() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(BaseFixtures.getVipCustomerData());

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, BaseFixtures.getVipCustomerData()))
                                .thenReturn(BaseFixtures.getVipPricingData());

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenThrow(new UpstreamInterruptedException(
                                                "Availability request was interrupted",
                                                new InterruptedException()));

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNotNull(result);
                assertNull(result.availability());

                assertNotNull(result.pricing());
                assertEquals(80, result.pricing().finalPrice().intValue());

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());
        }

        @Test
        void shouldReturnProductWithoutCustomerWhenCustomerIsUnavailable() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Customer upstream unavailable"));

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNotNull(result);

                assertNull(result.customer());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().basePrice().intValue());
                assertEquals(0, result.pricing().customerDiscount().intValue());
                assertEquals(100, result.pricing().finalPrice().intValue());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());
        }

        @Test
        void shouldReturnProductWithoutCustomerWhenCustomerRequestIsInterrupted() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenThrow(new UpstreamInterruptedException(
                                                "Customer request was interrupted",
                                                new InterruptedException()));

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNotNull(result);
                assertNull(result.customer());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().finalPrice().intValue());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());
        }

        @Test
        void shouldReturnStandardPricingWhenCustomerIdIsNotProvided() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, null);

                assertNotNull(result);

                assertNull(result.customer());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().basePrice().intValue());
                assertEquals(0, result.pricing().customerDiscount().intValue());
                assertEquals(100, result.pricing().finalPrice().intValue());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());

                verifyNoInteractions(customer);
        }

        @Test
        void shouldReturnPersonalizedPricingWhenCustomerIsAvailable() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                CustomerDto vipCustomer = BaseFixtures.getVipCustomerData();

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(vipCustomer);

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, vipCustomer))
                                .thenReturn(BaseFixtures.getVipPricingData());

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNotNull(result);

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().basePrice().intValue());
                assertEquals(20, result.pricing().customerDiscount().intValue());
                assertEquals(80, result.pricing().finalPrice().intValue());
        }

        @Test
        void shouldReturnProductWithStandardPricingWhenCustomerLookupFails() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Customer upstream unavailable"));

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNotNull(result);

                assertNull(result.customer());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().basePrice().intValue());
                assertEquals(0, result.pricing().customerDiscount().intValue());
                assertEquals(100, result.pricing().finalPrice().intValue());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());
        }

        @Test
        void shouldFailWholeRequestWhenCatalogIsUnavailable() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Catalog upstream unavailable"));

                CatalogUnavailableException exception = assertThrows(
                                CatalogUnavailableException.class,
                                () -> sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001"));

                assertEquals(
                                "Catalog unavailable. Try again in a few seconds.",
                                exception.getMessage());

                verifyNoInteractions(pricing);
                verifyNoInteractions(availability);
                verifyNoInteractions(customer);
        }

        @Test
        void shouldFailWholeRequestWhenCatalogRequestIsInterrupted() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenThrow(new UpstreamInterruptedException(
                                                "Catalog request was interrupted",
                                                new InterruptedException()));

                CatalogUnavailableException exception = assertThrows(
                                CatalogUnavailableException.class,
                                () -> sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001"));

                assertEquals(
                                "Catalog request was interrupted.",
                                exception.getMessage());

                verifyNoInteractions(pricing);
                verifyNoInteractions(availability);
                verifyNoInteractions(customer);
        }

        @Test
        void shouldUseCustomerForPricingWhenCustomerIdIsProvidedAndCustomerIsAvailable() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                CustomerDto customerDto = BaseFixtures.getVipCustomerData();

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(customerDto);

                PricingDto pricingDto = BaseFixtures.getVipPricingData();

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, customerDto))
                                .thenReturn(pricingDto);

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());

                assertNotNull(result.pricing());
                assertEquals(80, result.pricing().finalPrice().intValue());

                verify(pricing).getPricing(SAMPLE_PRODUCT_ID, customerDto);
        }

        @Test
        void shouldUseNullCustomerForPricingWhenCustomerLookupFails() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Customer upstream unavailable"));

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNull(result.customer());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().finalPrice().intValue());

                verify(pricing).getPricing(SAMPLE_PRODUCT_ID, null);
        }

        @Test
        void shouldUseNullCustomerForPricingWhenCustomerDoesNotExist() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(null); // Customer does not exist

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(SAMPLE_PRODUCT_ID, "CUST-001");

                assertNull(result.customer());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().finalPrice().intValue());

                verify(pricing).getPricing(SAMPLE_PRODUCT_ID, null);
        }

        @Test
        void shouldReturnStandardPricingWhenCustomerDoesNotExist() {
                when(catalog.getCatalog(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("INVALID-CUSTOMER"))
                                .thenThrow(new CustomerNotFoundException(
                                                "Customer not found: INVALID-CUSTOMER"));

                when(pricing.getPricing(SAMPLE_PRODUCT_ID, null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability(SAMPLE_PRODUCT_ID))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(
                                SAMPLE_PRODUCT_ID,
                                "INVALID-CUSTOMER");

                assertNotNull(result);
                assertNull(result.customer());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().basePrice().intValue());
                assertEquals(0, result.pricing().customerDiscount().intValue());
                assertEquals(100, result.pricing().finalPrice().intValue());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());

                verify(pricing).getPricing(SAMPLE_PRODUCT_ID, null);
        }
}
