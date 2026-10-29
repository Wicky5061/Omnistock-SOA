package com.omnistock.orderservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.omnistock.orderservice.client.InventoryClient;
import com.omnistock.orderservice.model.Order;
import com.omnistock.orderservice.repository.OrderRepository;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private InventoryClient inventoryClient;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void placeOrder_whenStockAvailable_shouldConfirmOrder() throws Exception {
        doNothing().when(inventoryClient).deductStock(1L, 2);

        Order order = new Order(1L, "Laptop Pro", 2, null);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.productId", is(1)))
                .andExpect(jsonPath("$.quantity", is(2)));
    }

    @Test
    void placeOrder_whenStockInsufficient_shouldRejectOrder() throws Exception {
        Request req = Request.create(Request.HttpMethod.PUT, "/inventory/product/1/deduct",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        FeignException badRequest = new FeignException.BadRequest("Insufficient stock", req, null, null);

        doThrow(badRequest).when(inventoryClient).deductStock(1L, 500);

        Order order = new Order(1L, "Laptop Pro", 500, null);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("REJECTED - Insufficient stock")));
    }

    @Test
    void placeOrder_whenProductNotFoundInInventory_shouldRejectOrder() throws Exception {
        Request req = Request.create(Request.HttpMethod.PUT, "/inventory/product/999/deduct",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
        FeignException notFound = new FeignException.NotFound("Not found", req, null, null);

        doThrow(notFound).when(inventoryClient).deductStock(999L, 1);

        Order order = new Order(999L, "Unknown Item", 1, null);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("REJECTED - Product not found in inventory")));
    }

    @Test
    void placeOrder_whenInventoryServiceDown_shouldRecordFailure() throws Exception {
        doThrow(new RuntimeException("Connection refused")).when(inventoryClient).deductStock(anyLong(), anyInt());

        Order order = new Order(1L, "Laptop Pro", 1, null);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", containsString("Unable to connect to Inventory Service")));
    }

    @Test
    void placeOrder_withZeroOrNegativeQuantity_shouldReturn400() throws Exception {
        Order invalidZero = new Order(1L, "Laptop", 0, null);
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidZero)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("greater than zero")));

        Order invalidNegative = new Order(1L, "Laptop", -3, null);
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidNegative)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("greater than zero")));
    }

    @Test
    void placeOrder_withInvalidProductId_shouldReturn400() throws Exception {
        Order invalid = new Order(-1L, "Invalid ID", 1, null);
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Valid product ID is required")));
    }

    @Test
    void getAllOrders_shouldReturnOrderList() throws Exception {
        orderRepository.save(new Order(1L, "Product A", 2, "CONFIRMED"));
        orderRepository.save(new Order(2L, "Product B", 1, "CONFIRMED"));

        mockMvc.perform(get("/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void getOrderById_whenExists_shouldReturnOrder() throws Exception {
        Order saved = orderRepository.save(new Order(1L, "Product A", 2, "CONFIRMED"));

        mockMvc.perform(get("/orders/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(saved.getId().intValue())))
                .andExpect(jsonPath("$.productName", is("Product A")));
    }
}
