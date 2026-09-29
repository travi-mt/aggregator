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
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(BaseFixtures.getVipCustomerData());

                when(pricing.getPricing("123", BaseFixtures.getVipCustomerData()))
                                .thenReturn(BaseFixtures.getVipPricingData());

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate("123", "CUST-001");

                assertNotNull(result);
                assertEquals("123", result.id());
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
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(BaseFixtures.getVipCustomerData());

                when(pricing.getPricing(any(), any()))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Pricing upstream unavailable"));

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate("123", "CUST-001");

                assertNotNull(result);
                assertEquals("123", result.id());
                assertEquals("Product 123", result.name());

                assertNull(result.pricing());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());
        }

        @Test
        void shouldReturnProductWithoutPricingWhenPricingRequestIsInterrupted() {
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(BaseFixtures.getVipCustomerData());

                when(pricing.getPricing(any(), any()))
                                .thenThrow(new UpstreamInterruptedException(
                                                "Pricing request was interrupted",
                                                new InterruptedException()));

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate("123", "CUST-001");

                assertNotNull(result);
                assertNull(result.pricing());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());
        }

        @Test
        void shouldReturnProductWithoutAvailabilityWhenAvailabilityIsUnavailable() {
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(BaseFixtures.getVipCustomerData());

                when(pricing.getPricing("123", BaseFixtures.getVipCustomerData()))
                                .thenReturn(BaseFixtures.getVipPricingData());

                when(availability.getAvailability("123"))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Availability upstream unavailable"));

                ProductResponse result = sut.aggregate("123", "CUST-001");

                assertNotNull(result);
                assertEquals("123", result.id());

                assertNull(result.availability());

                assertNotNull(result.pricing());
                assertEquals(80, result.pricing().finalPrice().intValue());

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());
        }

        @Test
        void shouldReturnProductWithoutAvailabilityWhenAvailabilityRequestIsInterrupted() {
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(BaseFixtures.getVipCustomerData());

                when(pricing.getPricing("123", BaseFixtures.getVipCustomerData()))
                                .thenReturn(BaseFixtures.getVipPricingData());

                when(availability.getAvailability("123"))
                                .thenThrow(new UpstreamInterruptedException(
                                                "Availability request was interrupted",
                                                new InterruptedException()));

                ProductResponse result = sut.aggregate("123", "CUST-001");

                assertNotNull(result);
                assertNull(result.availability());

                assertNotNull(result.pricing());
                assertEquals(80, result.pricing().finalPrice().intValue());

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());
        }

        @Test
        void shouldReturnProductWithoutCustomerWhenCustomerIsUnavailable() {
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Customer upstream unavailable"));

                when(pricing.getPricing("123", null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate("123", "CUST-001");

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
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenThrow(new UpstreamInterruptedException(
                                                "Customer request was interrupted",
                                                new InterruptedException()));

                when(pricing.getPricing("123", null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate("123", "CUST-001");

                assertNotNull(result);
                assertNull(result.customer());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().finalPrice().intValue());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());
        }

        @Test
        void shouldReturnStandardPricingWhenCustomerIdIsNotProvided() {
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(pricing.getPricing("123", null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate("123", null);

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
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                CustomerDto vipCustomer = BaseFixtures.getVipCustomerData();

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(vipCustomer);

                when(pricing.getPricing("123", vipCustomer))
                                .thenReturn(BaseFixtures.getVipPricingData());

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate("123", "CUST-001");

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
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Customer upstream unavailable"));

                when(pricing.getPricing("123", null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate("123", "CUST-001");

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
                when(catalog.getCatalog("123"))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Catalog upstream unavailable"));

                CatalogUnavailableException exception = assertThrows(
                                CatalogUnavailableException.class,
                                () -> sut.aggregate("123", "CUST-001"));

                assertEquals(
                                "Catalog unavailable. Try again in a few seconds.",
                                exception.getMessage());

                verifyNoInteractions(pricing);
                verifyNoInteractions(availability);
                verifyNoInteractions(customer);
        }

        @Test
        void shouldFailWholeRequestWhenCatalogRequestIsInterrupted() {
                when(catalog.getCatalog("123"))
                                .thenThrow(new UpstreamInterruptedException(
                                                "Catalog request was interrupted",
                                                new InterruptedException()));

                CatalogUnavailableException exception = assertThrows(
                                CatalogUnavailableException.class,
                                () -> sut.aggregate("123", "CUST-001"));

                assertEquals(
                                "Catalog request was interrupted.",
                                exception.getMessage());

                verifyNoInteractions(pricing);
                verifyNoInteractions(availability);
                verifyNoInteractions(customer);
        }

        @Test
        void shouldUseCustomerForPricingWhenCustomerIdIsProvidedAndCustomerIsAvailable() {
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                CustomerDto customerDto = BaseFixtures.getVipCustomerData();

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(customerDto);

                PricingDto pricingDto = BaseFixtures.getVipPricingData();

                when(pricing.getPricing("123", customerDto))
                                .thenReturn(pricingDto);

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate("123", "CUST-001");

                assertNotNull(result.customer());
                assertEquals("VIP", result.customer().segment());

                assertNotNull(result.pricing());
                assertEquals(80, result.pricing().finalPrice().intValue());

                verify(pricing).getPricing("123", customerDto);
        }

        @Test
        void shouldUseNullCustomerForPricingWhenCustomerLookupFails() {
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenThrow(new UpstreamUnavailableException(
                                                "Customer upstream unavailable"));

                when(pricing.getPricing("123", null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate("123", "CUST-001");

                assertNull(result.customer());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().finalPrice().intValue());

                verify(pricing).getPricing("123", null);
        }

        @Test
        void shouldUseNullCustomerForPricingWhenCustomerDoesNotExist() {
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("CUST-001"))
                                .thenReturn(null); // Customer does not exist

                when(pricing.getPricing("123", null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate("123", "CUST-001");

                assertNull(result.customer());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().finalPrice().intValue());

                verify(pricing).getPricing("123", null);
        }

        @Test
        void shouldReturnStandardPricingWhenCustomerDoesNotExist() {
                when(catalog.getCatalog("123"))
                                .thenReturn(BaseFixtures.getCatalogData());

                when(customer.getCustomer("INVALID-CUSTOMER"))
                                .thenThrow(new CustomerNotFoundException(
                                                "Customer not found: INVALID-CUSTOMER"));

                when(pricing.getPricing("123", null))
                                .thenReturn(BaseFixtures.getStandardPricingData());

                when(availability.getAvailability("123"))
                                .thenReturn(BaseFixtures.getAvailabilityData());

                ProductResponse result = sut.aggregate(
                                "123",
                                "INVALID-CUSTOMER");

                assertNotNull(result);
                assertNull(result.customer());

                assertNotNull(result.pricing());
                assertEquals(100, result.pricing().basePrice().intValue());
                assertEquals(0, result.pricing().customerDiscount().intValue());
                assertEquals(100, result.pricing().finalPrice().intValue());

                assertNotNull(result.availability());
                assertEquals(10, result.availability().stockLevel());

                verify(pricing).getPricing("123", null);
        }
}
