package com.omnistock.inventoryservice;

import com.omnistock.inventoryservice.model.Inventory;
import com.omnistock.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataLoader implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    public DataLoader(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args) {
        inventoryRepository.save(new Inventory(1L, "Laptop Pro 15", 50, 10));
        inventoryRepository.save(new Inventory(2L, "Wireless Mouse", 500, 50));
        inventoryRepository.save(new Inventory(3L, "Mechanical Keyboard", 8, 10));
        inventoryRepository.save(new Inventory(4L, "4K Monitor 27\"", 75, 15));
        System.out.println(">>> Sample inventory loaded successfully!");
    }
}