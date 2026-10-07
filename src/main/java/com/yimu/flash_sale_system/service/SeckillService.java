package com.yimu.flash_sale_system.service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.yimu.flash_sale_system.config.RabbitConfig;
import com.yimu.flash_sale_system.dto.SeckillMessage;
import com.yimu.flash_sale_system.entity.Product;
import com.yimu.flash_sale_system.exception.DuplicateOrderException;
import com.yimu.flash_sale_system.exception.OutOfStockException;
import com.yimu.flash_sale_system.exception.ResourceNotFoundException;
import com.yimu.flash_sale_system.exception.SeckillNotStartedException;
import com.yimu.flash_sale_system.repository.OrderRepository;
import com.yimu.flash_sale_system.repository.ProductRepository;

@Service 
public class SeckillService {
    private final ProductRepository productRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> seckillScript;
    private final RabbitTemplate rabbitTemplate;
    private final OrderRepository orderRepository;


    public SeckillService(StringRedisTemplate stringRedisTemplate,
                        DefaultRedisScript<Long> seckillScript,
                        ProductRepository productRepository,
                        RabbitTemplate rabbitTemplate,
                        OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.seckillScript = seckillScript;
        this.rabbitTemplate = rabbitTemplate;
        this.orderRepository = orderRepository;
    }

    public String seckill(Long productId, Long userId) {
        Long result = stringRedisTemplate.execute(seckillScript, List.of(RedisKeys.stockKey(productId), RedisKeys.usersKey(productId)), userId.toString());
        if (result == null || result == -3){
            throw new SeckillNotStartedException("Seckill has not started");
        }

        if (result == -1) {
            throw new OutOfStockException("Product is out of stock");
        }

        if (result == -2) {
            throw new DuplicateOrderException("Duplicate order");
        }        
        String orderNo = UUID.randomUUID().toString();
        stringRedisTemplate.opsForValue().set(RedisKeys.resultKey(orderNo), "PENDING",Duration.ofMinutes(30));
        try {
            rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE,RabbitConfig.ROUTING_KEY,new SeckillMessage(orderNo,userId,productId));
            // return orderService.createSeckillOrder(userId, productId);
        } catch (AmqpException e) {
            // Rollback stock and user set on failure
            stringRedisTemplate.opsForValue().increment(RedisKeys.stockKey(productId));
            stringRedisTemplate.opsForSet().remove(RedisKeys.usersKey(productId), userId.toString());
            stringRedisTemplate.delete(RedisKeys.resultKey(orderNo));
            throw e;
        }
        // record the order
        
        return orderNo;
    }
        public String getSeckillStatus(String orderNo) {
        
        String result = stringRedisTemplate.opsForValue().get(RedisKeys.resultKey(orderNo));
        if (result != null) {
            return result;
        }
        // not in redis, check database
        if (orderRepository.existsByOrderNo(orderNo)) {
            return "SUCCESS";
        }
        throw new ResourceNotFoundException("Order not found");
    }

    public void preheatProduct(Long productId) {
        // Preheat the product for seckill
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        stringRedisTemplate.opsForValue().set(RedisKeys.stockKey(productId), String.valueOf(product.getStock()));
        stringRedisTemplate.delete(RedisKeys.usersKey(productId));
    }
}
