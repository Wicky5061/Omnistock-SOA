package com.omnistock.productservice;

import com.omnistock.productservice.model.Product;
import com.omnistock.productservice.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataLoader implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataLoader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        productRepository.save(new Product("Laptop Pro 15", "LAP-001", new BigDecimal("1299.99"), "Electronics", 50));
        productRepository.save(new Product("Wireless Mouse", "MOU-001", new BigDecimal("29.99"), "Accessories", 500));
        productRepository.save(new Product("Mechanical Keyboard", "KEY-001", new BigDecimal("89.99"), "Accessories", 200));
        productRepository.save(new Product("4K Monitor 27\"", "MON-001", new BigDecimal("449.99"), "Electronics", 75));
        System.out.println(">>> Sample products loaded successfully!");
    }
}
