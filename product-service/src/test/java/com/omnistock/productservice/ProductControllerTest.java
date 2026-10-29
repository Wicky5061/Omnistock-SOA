package com.omnistock.productservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistock.productservice.model.Product;
import com.omnistock.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void getAllProducts_shouldReturnList() throws Exception {
        productRepository.save(new Product("Item 1", "SKU-1", new BigDecimal("10.00"), "Cat 1", 5));
        productRepository.save(new Product("Item 2", "SKU-2", new BigDecimal("20.00"), "Cat 2", 10));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Item 1")));
    }

    @Test
    void getProductById_whenExists_shouldReturnProduct() throws Exception {
        Product p = productRepository.save(new Product("Item X", "SKU-X", new BigDecimal("50.00"), "Cat X", 15));

        mockMvc.perform(get("/products/" + p.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Item X")))
                .andExpect(jsonPath("$.sku", is("SKU-X")));
    }

    @Test
    void getProductById_whenNotExists_shouldReturn404() throws Exception {
        mockMvc.perform(get("/products/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));
    }

    @Test
    void createProduct_withValidData_shouldReturn201() throws Exception {
        Product newProduct = new Product("New Gadget", "GAD-001", new BigDecimal("99.99"), "Tech", 30);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name", is("New Gadget")));
    }

    @Test
    void createProduct_withEmptyName_shouldReturn400() throws Exception {
        Product invalid = new Product("", "GAD-002", new BigDecimal("10.00"), "Tech", 10);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("name cannot be empty")));
    }

    @Test
    void createProduct_withNegativePrice_shouldReturn400() throws Exception {
        Product invalid = new Product("Bad Price", "BAD-01", new BigDecimal("-5.00"), "Misc", 5);

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("price must be a non-negative value")));
    }

    @Test
    void updateProduct_whenExists_shouldReturnUpdated() throws Exception {
        Product p = productRepository.save(new Product("Old Name", "SKU-OLD", new BigDecimal("10.00"), "Cat", 5));
        Product updatePayload = new Product("Updated Name", "SKU-NEW", new BigDecimal("15.00"), "Updated Cat", 20);

        mockMvc.perform(put("/products/" + p.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Name")))
                .andExpect(jsonPath("$.quantity", is(20)));
    }

    @Test
    void deleteProduct_whenExists_shouldReturn204() throws Exception {
        Product p = productRepository.save(new Product("To Delete", "DEL-01", new BigDecimal("10.00"), "Cat", 1));

        mockMvc.perform(delete("/products/" + p.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProduct_whenNotExists_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/products/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));
    }
}
