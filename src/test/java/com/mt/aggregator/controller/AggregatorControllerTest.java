package com.mt.aggregator.controller;

import com.mt.aggregator.api.AggregatorController;
import com.mt.aggregator.service.AggregatorService;
import com.mt.aggregator.fixtures.BaseFixtures;
import com.mt.aggregator.dto.ProductResponse;
import com.mt.aggregator.exception.CatalogUnavailableException;
import com.mt.aggregator.exception.ProductNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@WebMvcTest(AggregatorController.class)
class AggregatorControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private AggregatorService aggregatorService;

        private final String username = "test-user";
        private final String password = "test-password";

        @Test
        void shouldReturnProductInfo() throws Exception {
                when(aggregatorService.aggregate("123", "CUST-001"))
                                .thenReturn(new ProductResponse(
                                                "123",
                                                "Product 123",
                                                "Sample description",
                                                List.of("Spec A", "Spec B"),
                                                List.of("img1.jpg"),
                                                BaseFixtures.getVipPricingData(),
                                                BaseFixtures.getAvailabilityData(),
                                                BaseFixtures.getVipCustomerData()));

                mockMvc.perform(get("/aggregate/product-info/123")
                                .param("customerId", "CUST-001")
                                .with(httpBasic(username, password)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").value("123"))
                                .andExpect(jsonPath("$.pricing.finalPrice").value(80))
                                .andExpect(jsonPath("$.availability.stockLevel").value(10))
                                .andExpect(jsonPath("$.customer.segment").value("VIP"));

                verify(aggregatorService).aggregate("123", "CUST-001");
        }

        @Test
        void shouldFailWhenCatalogFails() throws Exception {
                when(aggregatorService.aggregate("123", "CUST-001"))
                                .thenThrow(new CatalogUnavailableException(
                                                "Catalog unavailable. Try again in a few seconds."));

                mockMvc.perform(get("/aggregate/product-info/123")
                                .param("customerId", "CUST-001")
                                .with(httpBasic(username, password)))
                                .andExpect(status().isServiceUnavailable())
                                .andExpect(status().isServiceUnavailable())
                                .andExpect(content().contentTypeCompatibleWith(
                                                MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(503))
                                .andExpect(jsonPath("$.title").value("Catalog unavailable"))
                                .andExpect(jsonPath("$.detail")
                                                .value("Catalog unavailable. Try again in a few seconds."));
        }

        @Test
        void shouldReturnNullPricingWhenPricingFails() throws Exception {
                ProductResponse response = new ProductResponse(
                                "123",
                                "Product 123",
                                "Sample description",
                                List.of("Spec A"),
                                List.of("img1.jpg"),
                                null, // pricing failed
                                BaseFixtures.getAvailabilityData(),
                                BaseFixtures.getVipCustomerData());

                when(aggregatorService.aggregate("123", "CUST-001"))
                                .thenReturn(response);

                mockMvc.perform(get("/aggregate/product-info/123")
                                .param("customerId", "CUST-001")
                                .with(httpBasic(username, password)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pricing").doesNotExist())
                                .andExpect(jsonPath("$.id").value("123"))
                                .andExpect(jsonPath("$.name").value("Product 123"))
                                .andExpect(jsonPath("$.availability.stockLevel").value(10))
                                .andExpect(jsonPath("$.customer.segment").value("VIP"));
        }

        @Test
        void shouldReturnProductWithNullAvailabilityWhenAvailabilityFails() throws Exception {
                ProductResponse response = new ProductResponse(
                                "123",
                                "Product 123",
                                "Sample description",
                                List.of("Spec A", "Spec B"),
                                List.of("img1.jpg"),
                                BaseFixtures.getVipPricingData(),
                                null, // availability failed
                                BaseFixtures.getVipCustomerData());

                when(aggregatorService.aggregate("123", "CUST-001"))
                                .thenReturn(response);

                mockMvc.perform(get("/aggregate/product-info/123")
                                .param("customerId", "CUST-001")
                                .with(httpBasic(username, password)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.availability").doesNotExist())
                                .andExpect(jsonPath("$.pricing.finalPrice").value(80))
                                .andExpect(jsonPath("$.customer.segment").value("VIP"));
        }

        @Test
        void shouldReturnProductWithNullCustomerWhenCustomerFails() throws Exception {
                ProductResponse response = new ProductResponse(
                                "123",
                                "Product 123",
                                "Sample description",
                                List.of("Spec A", "Spec B"),
                                List.of("img1.jpg"),
                                BaseFixtures.getVipPricingData(),
                                BaseFixtures.getAvailabilityData(),
                                null // customer failed
                );

                when(aggregatorService.aggregate("123", "CUST-001"))
                                .thenReturn(response);

                mockMvc.perform(get("/aggregate/product-info/123")
                                .param("customerId", "CUST-001")
                                .with(httpBasic(username, password)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.customer").doesNotExist())
                                .andExpect(jsonPath("$.pricing.finalPrice").value(80))
                                .andExpect(jsonPath("$.availability.stockLevel").value(10));
        }

        @Test
        void shouldReturnNullCustomerAndPricingWhenCustomerIdMissing() throws Exception {
                ProductResponse response = new ProductResponse(
                                "123",
                                "Product 123",
                                "Sample description",
                                List.of("Spec A", "Spec B"),
                                List.of("img1.jpg"),
                                null, // pricing null
                                BaseFixtures.getAvailabilityData(),
                                null // customer null
                );

                when(aggregatorService.aggregate("123", null))
                                .thenReturn(response);

                mockMvc.perform(get("/aggregate/product-info/123")
                                .with(httpBasic(username, password)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.pricing").doesNotExist())
                                .andExpect(jsonPath("$.customer").doesNotExist())
                                .andExpect(jsonPath("$.availability.stockLevel").value(10));
        }

        @Test
        void shouldReturnProductWithoutCustomerWhenCustomerIdDoesNotExist() throws Exception {
                when(aggregatorService.aggregate("123", "INVALID-CUSTOMER"))
                                .thenReturn(new ProductResponse(
                                                "123",
                                                "Product 123",
                                                "Sample description",
                                                List.of("Spec A", "Spec B"),
                                                List.of("img1.jpg"),
                                                BaseFixtures.getStandardPricingData(),
                                                BaseFixtures.getAvailabilityData(),
                                                null));

                mockMvc.perform(get("/aggregate/product-info/123")
                                .param("customerId", "INVALID-CUSTOMER")
                                .with(httpBasic(username, password)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("123"))
                                .andExpect(jsonPath("$.pricing.finalPrice").value(100))
                                .andExpect(jsonPath("$.availability.stockLevel").value(10))
                                .andExpect(jsonPath("$.customer").doesNotExist());

                verify(aggregatorService).aggregate("123", "INVALID-CUSTOMER");
        }

        @Test
        void shouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
                when(aggregatorService.aggregate("INVALID-PRODUCT", "CUST-001"))
                                .thenThrow(new ProductNotFoundException(
                                                "Product not found: INVALID-PRODUCT"));

                mockMvc.perform(get("/aggregate/product-info/INVALID-PRODUCT")
                                .param("customerId", "CUST-001")
                                .with(httpBasic(username, password)))
                                .andExpect(status().isNotFound())
                                .andExpect(status().isNotFound())
                                .andExpect(content().contentTypeCompatibleWith(
                                                MediaType.APPLICATION_PROBLEM_JSON))
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.title").value("Product not found"))
                                .andExpect(jsonPath("$.detail")
                                                .value("Product not found: INVALID-PRODUCT"));

                verify(aggregatorService).aggregate("INVALID-PRODUCT", "CUST-001");
        }

        @Test
        void shouldReturnNotFoundWhenProductIdIsMissing() throws Exception {
                mockMvc.perform(get("/aggregate/product-info")
                                .param("customerId", "CUST-001")
                                .with(httpBasic(username, password)))
                                .andExpect(status().isNotFound());

                verifyNoInteractions(aggregatorService);
        }

}
