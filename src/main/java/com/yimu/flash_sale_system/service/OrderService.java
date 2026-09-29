package com.yimu.flash_sale_system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yimu.flash_sale_system.entity.Order;
import com.yimu.flash_sale_system.exception.ResourceNotFoundException;
import com.yimu.flash_sale_system.repository.OrderRepository;
import com.yimu.flash_sale_system.repository.ProductRepository;
import com.yimu.flash_sale_system.repository.UserRepository;
import com.yimu.flash_sale_system.exception.OutOfStockException;
import com.yimu.flash_sale_system.entity.OrderStatus;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }
    // --------------buyer-----------------//
    public List<Order> getOrdersByUserId(Long userId) {
        return orderRepository.findByUser_Id(userId);
    }

    public Order getOrderForUser(Long userId, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(order -> order.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
    //------------seckill------------//

    @Transactional
    public Order createSeckillOrder(Long userId, Long productId) {
        
        int updated = productRepository.decreaseStock(productId);
        if(updated==0){
            throw new OutOfStockException("Product is out of stock");
        }
        // Create seckill order
        Order order = new Order();
        order.setUser(userRepository.getReferenceById(userId));
        order.setProduct(productRepository.getReferenceById(productId));
        order.setQuantity(1);
        order.setOrderNo(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.COMPLETED);
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    // -------------admin-----------------//
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
}
