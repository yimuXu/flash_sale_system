package com.yimu.flash_sale_system.mq;

import java.time.Duration;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.yimu.flash_sale_system.config.RabbitConfig;
import com.yimu.flash_sale_system.dto.SeckillMessage;
import com.yimu.flash_sale_system.exception.OutOfStockException;
import com.yimu.flash_sale_system.service.OrderService;
import com.yimu.flash_sale_system.service.RedisKeys;

@Component 
public class SeckillOrderConsumer {
    private  final OrderService orderService;

    private final StringRedisTemplate stringRedisTemplate;

    public SeckillOrderConsumer(OrderService orderService, StringRedisTemplate stringRedisTemplate) {
        this.orderService = orderService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @RabbitListener (queues = RabbitConfig.QUEUE)
    public void handle(SeckillMessage msg) {
        String resultKey = RedisKeys.resultKey(msg.orderNo());
        try {
            orderService.createSeckillOrder(msg.userId(), msg.productId(), msg.orderNo());
        } catch (DataIntegrityViolationException e) {
            // The order already exists = duplicate delivery, treated as success; otherwise, it is really failed, throw it out to go through retry and dead letter queue
            if (!orderService.existsByOrderNo(msg.orderNo())) throw e;
        } catch (OutOfStockException e) {
            // The database is out of stock, indicating that the Redis inventory is more than the database: only mark it as failed, and cannot add the inventory back
            stringRedisTemplate.opsForValue().set(resultKey, "FAILED", Duration.ofMinutes(30));
            return;
        }
        stringRedisTemplate.opsForValue().set(resultKey, "SUCCESS", Duration.ofMinutes(30));
    }

    @RabbitListener (queues = RabbitConfig.DLQ)
    public void handleDeadLetter(SeckillMessage msg) {
        stringRedisTemplate.opsForValue().increment(RedisKeys.stockKey(msg.productId()));
        stringRedisTemplate.opsForSet().remove(RedisKeys.usersKey(msg.productId()), msg.userId().toString());
        stringRedisTemplate.opsForValue().set(RedisKeys.resultKey(msg.orderNo()), "FAILED", Duration.ofMinutes(30));
    }

}
