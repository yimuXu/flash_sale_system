package com.yimu.flash_sale_system.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yimu.flash_sale_system.dto.OrderResponse;
import com.yimu.flash_sale_system.entity.Order;
import com.yimu.flash_sale_system.service.OrderService;

@RestController 
public class OrderController {
    // Inject OrderService
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    //get order by USER id
    @GetMapping("/orders/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByUserId(@PathVariable Long userId) {
        List<OrderResponse> orders = OrderResponse.fromOrders(orderService.getOrdersByUserId(userId));
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/orders/{orderId}/{userId}")
    public ResponseEntity<OrderResponse> getOrderByIdForUser(@PathVariable Long orderId, @PathVariable Long userId) {
        OrderResponse order = OrderResponse.fromOrder(orderService.getOrderForUser(userId, orderId));
        return ResponseEntity.ok(order);
    }

    @GetMapping ("admin/orders")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        List<OrderResponse> orders = OrderResponse.fromOrders(orderService.getAllOrders());
        return ResponseEntity.ok(orders);
    }

    @GetMapping ("admin/orders/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        OrderResponse order = OrderResponse.fromOrder(orderService.getOrderById(orderId));
        return ResponseEntity.ok(order);
    }
}