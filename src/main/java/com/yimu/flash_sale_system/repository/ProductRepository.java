package com.yimu.flash_sale_system.repository;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yimu.flash_sale_system.entity.Product;

public interface ProductRepository extends JpaRepository<Product,Long> {
}
