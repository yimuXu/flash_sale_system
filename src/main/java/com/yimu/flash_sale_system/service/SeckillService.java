package com.yimu.flash_sale_system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.yimu.flash_sale_system.entity.Order;
import com.yimu.flash_sale_system.entity.OrderStatus;
import com.yimu.flash_sale_system.entity.Product;
import com.yimu.flash_sale_system.entity.User;
import com.yimu.flash_sale_system.exception.*;
import com.yimu.flash_sale_system.repository.OrderRepository;
import com.yimu.flash_sale_system.repository.ProductRepository;
import com.yimu.flash_sale_system.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;

@Service 
public class SeckillService {
    private final ProductRepository productRepository;
    private final OrderService orderService;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> seckillScript;
    
    private static String stockKey(Long productId) {
        return "seckill:stock:" + productId;
    }
    private static String usersKey(Long productId) {
        return "seckill:users:" + productId;
    }


    public SeckillService(StringRedisTemplate stringRedisTemplate,
                        DefaultRedisScript<Long> seckillScript,
                        ProductRepository productRepository,
                        OrderService orderService) {
        this.productRepository = productRepository;
        this.orderService = orderService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.seckillScript = seckillScript;
    }

    public Order seckill(Long productId, Long userId) {
        Long result = stringRedisTemplate.execute(seckillScript, List.of(stockKey(productId), usersKey(productId)), userId.toString());
        if (result == null || result == -3){
            throw new SeckillNotStartedException("Seckill has not started");
        }

        if (result == -1) {
            throw new OutOfStockException("Product is out of stock");
        }

        if (result == -2) {
            throw new DuplicateOrderException("Duplicate order");
        }        
        try {
            return orderService.createSeckillOrder(userId, productId);
        } catch (RuntimeException e) {
            // Rollback stock and user set on failure
            stringRedisTemplate.opsForValue().increment(stockKey(productId));
            stringRedisTemplate.opsForSet().remove(usersKey(productId), userId.toString());
            throw e;
        }
    }

    public void preheatProduct(Long productId) {
        // Preheat the product for seckill
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        stringRedisTemplate.opsForValue().set(stockKey(productId), String.valueOf(product.getStock()));
        stringRedisTemplate.delete(usersKey(productId));
    }
}
