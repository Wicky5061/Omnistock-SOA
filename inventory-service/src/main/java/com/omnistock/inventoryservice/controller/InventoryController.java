package com.omnistock.inventoryservice.controller;

import com.omnistock.inventoryservice.model.Inventory;
import com.omnistock.inventoryservice.repository.InventoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/{id}")
    public ResponseEntity<Inventory> getInventoryById(@PathVariable Long id) {
        return inventoryRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/low-stock")
    public List<Inventory> getLowStockItems() {
        return inventoryRepository.findAll().stream()
                .filter(Inventory::isLowStock)
                .toList();
    }

    @PostMapping
    public ResponseEntity<Inventory> createInventory(@RequestBody Inventory inventory) {
        Inventory saved = inventoryRepository.save(inventory);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}/deduct")
    public ResponseEntity<Inventory> deductStock(@PathVariable Long id, @RequestParam Integer quantity) {
        return inventoryRepository.findById(id)
                .map(inv -> {
                    if (inv.getStockQuantity() < quantity) {
                        return ResponseEntity.badRequest().<Inventory>build();
                    }
                    inv.setStockQuantity(inv.getStockQuantity() - quantity);
                    return ResponseEntity.ok(inventoryRepository.save(inv));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}