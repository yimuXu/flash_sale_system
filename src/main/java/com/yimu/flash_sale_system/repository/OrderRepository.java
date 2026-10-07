package com.yimu.flash_sale_system.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yimu.flash_sale_system.entity.Order;

public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order> findByUser_Id(Long userId);
    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);
    Long countByProduct_Id(Long productId);
    boolean existsByOrderNo(String orderNo);
}
