package com.yimu.flash_sale_system.entity;

import java.time.LocalDateTime;

import jakarta.annotation.Generated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "orders",uniqueConstraints = @UniqueConstraint(columnNames = {"user_id","product_id"}))
public class Order {
    @Id
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    // for idempotency
    @Column (unique = true)
    private String orderNo;
    @ManyToOne 
    private User user;
    @ManyToOne 
    private Product product;
    private Integer quantity;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }
    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }
    public String getOrderNo() {
        return orderNo;
    }

    
    public void setUser(User user) {
        this.user = user;
    }
    public User getUser() {
        return user;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
    public Product getProduct() {
        return product;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
    public OrderStatus getStatus() {
        return status;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }   
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

}
