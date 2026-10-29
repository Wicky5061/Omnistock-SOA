package com.omnistock.inventoryservice.repository;

import com.omnistock.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    boolean existsByProductId(Long productId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Inventory i SET i.stockQuantity = i.stockQuantity - :quantity WHERE i.productId = :productId AND i.stockQuantity >= :quantity")
    int deductStockByProductId(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Inventory i SET i.stockQuantity = i.stockQuantity + :quantity WHERE i.productId = :productId")
    int addStockByProductId(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}