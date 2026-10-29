package com.omnistock.productservice.controller;

import com.omnistock.productservice.model.Product;
import com.omnistock.productservice.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        return productRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Product not found with ID: " + id)));
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody Product product) {
        String validationError = validateProduct(product);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(createErrorResponse(validationError));
        }

        Product saved = productRepository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        String validationError = validateProduct(product);
        if (validationError != null) {
            return ResponseEntity.badRequest().body(createErrorResponse(validationError));
        }

        return productRepository.findById(id)
                .<ResponseEntity<?>>map(existing -> {
                    existing.setName(product.getName());
                    existing.setSku(product.getSku());
                    existing.setPrice(product.getPrice());
                    existing.setCategory(product.getCategory());
                    existing.setQuantity(product.getQuantity());
                    return ResponseEntity.ok(productRepository.save(existing));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Product not found with ID: " + id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(createErrorResponse("Product not found with ID: " + id));
    }

    private String validateProduct(Product product) {
        if (product == null) {
            return "Product payload is required";
        }
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            return "Product name cannot be empty";
        }
        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            return "Product SKU cannot be empty";
        }
        if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            return "Product price must be a non-negative value";
        }
        if (product.getQuantity() == null || product.getQuantity() < 0) {
            return "Product quantity must be non-negative";
        }
        return null;
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
