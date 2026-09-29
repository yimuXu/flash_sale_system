package com.yimu.flash_sale_system.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yimu.flash_sale_system.entity.Product;

public interface ProductRepository extends JpaRepository<Product,Long> {
    @Modifying
    @Query ("UPDATE Product p SET p.stock = p.stock-1 WHERE p.id = :id AND p.stock > 0")
    int decreaseStock(@Param ("id") Long id);
}
