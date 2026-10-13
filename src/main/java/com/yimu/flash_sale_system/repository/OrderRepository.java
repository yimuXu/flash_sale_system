package com.yimu.flash_sale_system.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.yimu.flash_sale_system.entity.Order;
import com.yimu.flash_sale_system.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order,Long> {
    List<Order> findByUser_Id(Long userId);
    boolean existsByUser_IdAndProduct_Id(Long userId, Long productId);
    Long countByProduct_Id(Long productId);
    boolean existsByOrderNoAndUser_Id(String orderNo, Long userId);

    @Modifying(clearAutomatically = true)
    @Query ("UPDATE Order o SET o.status = :to, o.updatedAt = :now" +" WHERE o.orderNo = :orderNo AND o.status = :from")
    int changeStatus(@Param("orderNo") String orderNo, @Param("from") OrderStatus from, @Param("to") OrderStatus to, @Param("now") LocalDateTime now);

    Optional<Order> findByOrderNoAndUser_Id(String orderNo, Long userId);
}
