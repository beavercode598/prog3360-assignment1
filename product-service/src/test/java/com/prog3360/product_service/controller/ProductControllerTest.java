package com.prog3360.product_service.controller;

import com.prog3360.product_service.flags.FeatureFlagService;
import com.prog3360.product_service.model.Product;
import com.prog3360.product_service.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    ProductRepository repo;

    @MockBean
    FeatureFlagService featureFlagService;

    @Test
    void premiumPricing_off_returnsRegularPrice() throws Exception {
        when(featureFlagService.isEnabled("premium-pricing")).thenReturn(false);

        // IMPORTANT: repo returns Product (not ProductDto)
        Product p = new Product("Test", 100.0, 10);
        when(repo.findAll()).thenReturn(List.of(p));

        mvc.perform(get("/api/products/premium"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test"))
                .andExpect(jsonPath("$[0].price").value(100.0))
                .andExpect(jsonPath("$[0].quantity").value(10));
    }

    @Test
    void premiumPricing_on_returnsDiscountedPrice() throws Exception {
        when(featureFlagService.isEnabled("premium-pricing")).thenReturn(true);

        Product p = new Product("Test", 100.0, 10);
        when(repo.findAll()).thenReturn(List.of(p));

        mvc.perform(get("/api/products/premium"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test"))
                .andExpect(jsonPath("$[0].price").value(90.0))
                .andExpect(jsonPath("$[0].quantity").value(10));
    }
}