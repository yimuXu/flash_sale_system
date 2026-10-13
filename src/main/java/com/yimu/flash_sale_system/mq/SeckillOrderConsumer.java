package com.yimu.flash_sale_system.mq;

import java.time.Duration;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
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
    private final RabbitTemplate rabbitTemplate;
    @Value("${seckill.payment-timeout:15m}")
    private Duration paymentTimeout;


    public SeckillOrderConsumer(OrderService orderService, StringRedisTemplate stringRedisTemplate, RabbitTemplate rabbitTemplate) {
        this.orderService = orderService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener (queues = RabbitConfig.QUEUE)
    public void handle(SeckillMessage msg) {
        String resultKey = RedisKeys.resultKey(msg.userId(), msg.orderNo());
        try {
            orderService.createSeckillOrder(msg.userId(), msg.productId(), msg.orderNo());
        } catch (DataIntegrityViolationException e) {
            // The order already exists = duplicate delivery, treated as success; otherwise, it is really failed, throw it out to go through retry and dead letter queue
            if (!orderService.existsByOrderNoAndUser_Id(msg.orderNo(), msg.userId())) throw e;
        } catch (OutOfStockException e) {
            // The database is out of stock, indicating that the Redis inventory is more than the database: only mark it as failed, and cannot add the inventory back
            stringRedisTemplate.opsForValue().set(resultKey, "FAILED", Duration.ofMinutes(30));
            return;
        }
        rabbitTemplate.convertAndSend("", RabbitConfig.DELAY_QUEUE, msg, m -> {
            m.getMessageProperties().setExpiration(String.valueOf(paymentTimeout.toMillis()));
            return m;
        });
        stringRedisTemplate.opsForValue().set(resultKey, "SUCCESS", Duration.ofMinutes(30));
    }

    @RabbitListener (queues = RabbitConfig.TIMEOUT_QUEUE)
    public void handleTimeout(SeckillMessage msg) {
        if (orderService.cancelIfUnpaid(msg.orderNo(),msg.productId())){
            stringRedisTemplate.opsForValue().increment(RedisKeys.stockKey(msg.productId()));
        }
        
    }

    @RabbitListener (queues = RabbitConfig.DLQ)
    public void handleDeadLetter(SeckillMessage msg) {
        stringRedisTemplate.opsForValue().increment(RedisKeys.stockKey(msg.productId()));
        stringRedisTemplate.opsForSet().remove(RedisKeys.usersKey(msg.productId()), msg.userId().toString());
        stringRedisTemplate.opsForValue().set(RedisKeys.resultKey(msg.userId(), msg.orderNo()), "FAILED", Duration.ofMinutes(30));
    }
    
}
