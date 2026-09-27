package com.yimu.flash_sale_system.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.yimu.flash_sale_system.entity.Order;
import com.yimu.flash_sale_system.entity.Product;

public class OrderResponse {
    private Long orderId;
    private Long productId;
    private Long userId;
    private Integer quantity;
    private String orderStatus;
    private String orderNo;
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static OrderResponse fromOrder(Order order) {
        OrderResponse response = new OrderResponse();
        response.setOrderId(order.getId());
        response.setProductId(order.getProduct().getId());
        response.setUserId(order.getUser().getId());
        response.setOrderStatus(order.getStatus().name());
        response.setQuantity(order.getQuantity());
        response.setOrderNo(order.getOrderNo());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }
    public static List<OrderResponse> fromOrders(List<Order> orders) {
        return orders.stream()
                .map(OrderResponse::fromOrder)
                .collect(Collectors.toList());
    }
}
