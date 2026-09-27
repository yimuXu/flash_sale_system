package com.yimu.flash_sale_system.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.yimu.flash_sale_system.entity.Order;
import com.yimu.flash_sale_system.entity.OrderStatus;
import com.yimu.flash_sale_system.entity.Product;
import com.yimu.flash_sale_system.entity.User;
import com.yimu.flash_sale_system.exception.DuplicateOrderException;
import com.yimu.flash_sale_system.exception.OutOfStockException;
import com.yimu.flash_sale_system.exception.ResourceNotFoundException;
import com.yimu.flash_sale_system.repository.OrderRepository;
import com.yimu.flash_sale_system.repository.ProductRepository;
import com.yimu.flash_sale_system.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;

@Service 
public class SeckillService {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public SeckillService(ProductRepository productRepository, UserRepository userRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order seckill(Long productId, Long userId) {
        // Validate product and user existence
        Product product = productRepository.findById(productId)
        .orElseThrow(()-> new ResourceNotFoundException("Product not found"));
        User user = userRepository.findById(userId).orElseThrow(()-> new ResourceNotFoundException("User not found"));
        // avoid reordering the same product
        if (orderRepository.existsByUser_IdAndProduct_Id(userId, productId)) {
            throw new DuplicateOrderException("User has already ordered this product");
        }
        // Check stock 
        if (product.getStock() <= 0) {
            throw new OutOfStockException("Product is out of stock");
        }
        // Reduce stock and create order
        product.setStock(product.getStock() - 1);
        productRepository.saveAndFlush(product);
        // Create order
        Order order = new Order();
        order.setProduct(product);
        order.setUser(user);
        order.setQuantity(1);

        order.setOrderNo(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.COMPLETED);
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }
}
