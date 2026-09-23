package com.omnistock.orderservice.controller;

import com.omnistock.orderservice.client.InventoryClient;
import com.omnistock.orderservice.model.Order;
import com.omnistock.orderservice.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> placeOrder(@RequestBody Order order) {
        try {
            inventoryClient.deductStock(order.getProductId(), order.getQuantity());
            order.setStatus("CONFIRMED");
        } catch (Exception e) {
            order.setStatus("REJECTED - insufficient stock");
        }
        Order saved = orderRepository.save(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}