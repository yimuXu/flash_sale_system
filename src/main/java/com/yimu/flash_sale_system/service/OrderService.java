package com.yimu.flash_sale_system.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yimu.flash_sale_system.entity.Order;
import com.yimu.flash_sale_system.exception.ResourceNotFoundException;
import com.yimu.flash_sale_system.exception.OrderNotPayableException;
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
    private final StringRedisTemplate stringRedisTemplate;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        UserRepository userRepository,
                        StringRedisTemplate stringRedisTemplate) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.stringRedisTemplate = stringRedisTemplate; 
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
    public Order createSeckillOrder(Long userId, Long productId,String orderNo) {
        
        int updated = productRepository.decreaseStock(productId);
        if(updated==0){
            throw new OutOfStockException("Product is out of stock");
        }
        // Create seckill order
        Order order = new Order();
        order.setUser(userRepository.getReferenceById(userId));
        order.setProduct(productRepository.getReferenceById(productId));
        order.setQuantity(1);
        order.setOrderNo(orderNo);
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    @Transactional
    public Order pay(Long userId, String orderNo) {
        orderRepository.findByOrderNoAndUser_Id(orderNo, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        int updated = orderRepository.changeStatus(orderNo, OrderStatus.PENDING, OrderStatus.PAID, LocalDateTime.now());
        if (updated == 0) {
            throw new OrderNotPayableException("Order is not awaiting payment");
        }
        return orderRepository.findByOrderNoAndUser_Id(orderNo, userId)
                .orElseThrow();
    }

    @Transactional
    public boolean cancelIfUnpaid(String orderNo, Long productId) {
        int updated = orderRepository.changeStatus(orderNo, OrderStatus.PENDING, OrderStatus.CANCELLED, LocalDateTime.now());
        if (updated == 0) {
            return false;
        }
        productRepository.increaseStock(productId);
        return true;
    }

    public boolean existsByOrderNoAndUser_Id(String orderNo, Long userId) {
        return orderRepository.existsByOrderNoAndUser_Id(orderNo, userId);
    }

    // -------------admin-----------------//
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
}
