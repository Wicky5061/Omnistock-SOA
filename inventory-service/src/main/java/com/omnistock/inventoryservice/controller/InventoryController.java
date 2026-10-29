package com.omnistock.inventoryservice.controller;

import com.omnistock.inventoryservice.model.Inventory;
import com.omnistock.inventoryservice.repository.InventoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @GetMapping
    public List<Inventory> getAllInventory() {
        return inventoryRepository.findAll();
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getInventoryByProductId(@PathVariable Long productId) {
        return inventoryRepository.findByProductId(productId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Inventory record not found for product ID: " + productId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInventoryById(@PathVariable Long id) {
        // Try by productId first, then by record id
        Optional<Inventory> inv = inventoryRepository.findByProductId(id);
        if (inv.isPresent()) {
            return ResponseEntity.ok(inv.get());
        }
        return inventoryRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Inventory record not found for ID: " + id)));
    }

    @GetMapping("/low-stock")
    public List<Inventory> getLowStockItems() {
        return inventoryRepository.findAll().stream()
                .filter(Inventory::isLowStock)
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> createOrUpdateInventory(@RequestBody Inventory inventory) {
        if (inventory.getProductId() == null || inventory.getProductId() <= 0) {
            return ResponseEntity.badRequest().body(createErrorResponse("Valid product ID is required"));
        }
        if (inventory.getStockQuantity() == null || inventory.getStockQuantity() < 0) {
            return ResponseEntity.badRequest().body(createErrorResponse("Stock quantity cannot be negative"));
        }
        if (inventory.getLowStockThreshold() == null || inventory.getLowStockThreshold() < 0) {
            inventory.setLowStockThreshold(10);
        }

        Optional<Inventory> existing = inventoryRepository.findByProductId(inventory.getProductId());
        if (existing.isPresent()) {
            Inventory inv = existing.get();
            inv.setProductName(inventory.getProductName());
            inv.setStockQuantity(inventory.getStockQuantity());
            inv.setLowStockThreshold(inventory.getLowStockThreshold());
            return ResponseEntity.ok(inventoryRepository.save(inv));
        }

        Inventory saved = inventoryRepository.save(inventory);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping({"/product/{productId}/deduct", "/{productId}/deduct"})
    public ResponseEntity<?> deductStock(@PathVariable("productId") Long productId, @RequestParam Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest().body(createErrorResponse("Quantity must be greater than zero"));
        }

        // Locate inventory by productId first, then fallback to record id
        Optional<Inventory> opt = inventoryRepository.findByProductId(productId);
        if (opt.isEmpty()) {
            opt = inventoryRepository.findById(productId);
        }

        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Inventory not found for product ID: " + productId));
        }

        Inventory inv = opt.get();
        if (inv.getStockQuantity() < quantity) {
            return ResponseEntity.badRequest().body(createErrorResponse(
                    "Insufficient stock for product ID: " + inv.getProductId() +
                    ". Available: " + inv.getStockQuantity() + ", Requested: " + quantity));
        }

        int updatedRows = inventoryRepository.deductStockByProductId(inv.getProductId(), quantity);
        if (updatedRows == 0) {
            // Concurrent deduction occurred
            return ResponseEntity.badRequest().body(createErrorResponse(
                    "Insufficient stock due to concurrent order. Please retry."));
        }

        Inventory updated = inventoryRepository.findByProductId(inv.getProductId()).orElse(inv);
        return ResponseEntity.ok(updated);
    }

    @PutMapping({"/product/{productId}/restock", "/{productId}/restock"})
    public ResponseEntity<?> restock(@PathVariable("productId") Long productId, @RequestParam Integer quantity) {
        if (quantity == null || quantity <= 0) {
            return ResponseEntity.badRequest().body(createErrorResponse("Restock quantity must be greater than zero"));
        }

        Optional<Inventory> opt = inventoryRepository.findByProductId(productId);
        if (opt.isEmpty()) {
            opt = inventoryRepository.findById(productId);
        }

        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("Inventory not found for product ID: " + productId));
        }

        Inventory inv = opt.get();
        inventoryRepository.addStockByProductId(inv.getProductId(), quantity);
        Inventory updated = inventoryRepository.findByProductId(inv.getProductId()).orElse(inv);
        return ResponseEntity.ok(updated);
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}