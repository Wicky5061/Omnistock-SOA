package com.omnistock.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @PutMapping("/inventory/{id}/deduct")
    void deductStock(@PathVariable("id") Long id, @RequestParam("quantity") Integer quantity);
}