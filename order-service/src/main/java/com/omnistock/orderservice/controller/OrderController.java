package com.omnistock.orderservice.controller;

import com.omnistock.orderservice.client.InventoryClient;
import com.omnistock.orderservice.model.Order;
import com.omnistock.orderservice.repository.OrderRepository;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public OrderController(OrderRepository orderRepository, InventoryClient inventoryClient) {
        this.orderRepository = orderRepository;
        this.inventoryClient = inventoryClient;
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Order not found with ID: " + id)));
    }

    @PostMapping
    public ResponseEntity<?> placeOrder(@RequestBody Order order) {
        if (order == null) {
            return ResponseEntity.badRequest().body(createErrorResponse("Order payload is required"));
        }
        if (order.getProductId() == null || order.getProductId() <= 0) {
            return ResponseEntity.badRequest().body(createErrorResponse("Valid product ID is required"));
        }
        if (order.getQuantity() == null || order.getQuantity() <= 0) {
            return ResponseEntity.badRequest().body(createErrorResponse("Order quantity must be greater than zero"));
        }
        if (order.getProductName() == null || order.getProductName().trim().isEmpty()) {
            order.setProductName("Product #" + order.getProductId());
        }

        try {
            inventoryClient.deductStock(order.getProductId(), order.getQuantity());
            order.setStatus("CONFIRMED");
        } catch (FeignException.BadRequest e) {
            order.setStatus("REJECTED - Insufficient stock");
        } catch (FeignException.NotFound e) {
            order.setStatus("REJECTED - Product not found in inventory");
        } catch (FeignException e) {
            order.setStatus("FAILED - Inventory service error (HTTP " + e.status() + ")");
        } catch (Exception e) {
            order.setStatus("FAILED - Unable to connect to Inventory Service");
        }

        Order saved = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}