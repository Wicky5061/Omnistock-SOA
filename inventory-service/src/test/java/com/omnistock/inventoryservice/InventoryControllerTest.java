package com.omnistock.inventoryservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistock.inventoryservice.model.Inventory;
import com.omnistock.inventoryservice.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        inventoryRepository.deleteAll();
    }

    @Test
    void getInventoryByProductId_whenExists_shouldReturnInventory() throws Exception {
        inventoryRepository.save(new Inventory(101L, "Widget", 50, 10));

        mockMvc.perform(get("/inventory/product/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is(101)))
                .andExpect(jsonPath("$.productName", is("Widget")))
                .andExpect(jsonPath("$.stockQuantity", is(50)));
    }

    @Test
    void getInventoryByProductId_whenNotExists_shouldReturn404() throws Exception {
        mockMvc.perform(get("/inventory/product/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));
    }

    @Test
    void deductStock_whenSufficientStock_shouldDeductAndReturn200() throws Exception {
        inventoryRepository.save(new Inventory(102L, "Keyboard", 30, 5));

        mockMvc.perform(put("/inventory/product/102/deduct")
                        .param("quantity", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stockQuantity", is(20)));

        Inventory updated = inventoryRepository.findByProductId(102L).orElseThrow();
        assertEquals(20, updated.getStockQuantity());
    }

    @Test
    void deductStock_whenInsufficientStock_shouldRejectWith400() throws Exception {
        inventoryRepository.save(new Inventory(103L, "Mouse", 5, 2));

        mockMvc.perform(put("/inventory/product/103/deduct")
                        .param("quantity", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Insufficient stock")));

        // Verify stock quantity did not change (prevents negative stock)
        Inventory unchanged = inventoryRepository.findByProductId(103L).orElseThrow();
        assertEquals(5, unchanged.getStockQuantity());
    }

    @Test
    void deductStock_withInvalidQuantity_shouldReturn400() throws Exception {
        mockMvc.perform(put("/inventory/product/104/deduct")
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("greater than zero")));

        mockMvc.perform(put("/inventory/product/104/deduct")
                        .param("quantity", "-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("greater than zero")));
    }

    @Test
    void deductStock_whenMissingInventory_shouldReturn404() throws Exception {
        mockMvc.perform(put("/inventory/product/888/deduct")
                        .param("quantity", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));
    }

    @Test
    void getLowStock_shouldReturnOnlyItemsBelowThreshold() throws Exception {
        inventoryRepository.save(new Inventory(201L, "Low Item", 3, 5));     // Low stock
        inventoryRepository.save(new Inventory(202L, "Ample Item", 100, 10)); // Normal stock

        mockMvc.perform(get("/inventory/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId", is(201)));
    }

    @Test
    void createOrUpdateInventory_withValidData_shouldReturn201() throws Exception {
        Inventory inv = new Inventory(301L, "New Product", 75, 15);

        mockMvc.perform(post("/inventory")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(inv)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId", is(301)))
                .andExpect(jsonPath("$.stockQuantity", is(75)));
    }
}
