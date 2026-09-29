package com.yimu.flash_sale_system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.yimu.flash_sale_system.entity.Product;
import com.yimu.flash_sale_system.entity.User;
import com.yimu.flash_sale_system.entity.UserRoles;
import com.yimu.flash_sale_system.exception.*;
import com.yimu.flash_sale_system.repository.OrderRepository;
import com.yimu.flash_sale_system.repository.ProductRepository;
import com.yimu.flash_sale_system.repository.UserRepository;
import com.yimu.flash_sale_system.service.SeckillService;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/flash_sale_test",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.redis.database=1"
})

public class SeckillConcurrencyTest {
    @Autowired SeckillService seckillService;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired StringRedisTemplate redis;

    @Test
    void shouldNotOversell() throws Exception {


        int stock = 10, threads = 100;

        // 1. prepare data 1 product(stock:10) user :100
        Product product = new Product();
        product.setName("test"); product.setPrice(BigDecimal.ONE); product.setStock(stock);
        Long productId = productRepository.save(product).getId();
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            User u = new User();
            u.setEmail(UUID.randomUUID() + "@test.com"); u.setPassword("x"); u.setRole(UserRoles.USER);
            userIds.add(userRepository.save(u).getId());
        }
        seckillService.preheatProduct(productId);
        // 2. let 100 threads compete
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(), duplicate = new AtomicInteger(),
                      soldOut = new AtomicInteger(), other = new AtomicInteger();
        Map<String, AtomicInteger> errors = new ConcurrentHashMap<>();

        for (Long userId : userIds) {
            pool.submit(() -> {
                try {
                    start.await();
                    seckillService.seckill(productId, userId);
                    success.incrementAndGet();
                } catch (OutOfStockException e) {
                    soldOut.incrementAndGet();
                } catch (DuplicateOrderException e) {
                    duplicate.incrementAndGet();
                } catch (Exception e) {
                    other.incrementAndGet();
                    errors.computeIfAbsent(e.getClass().getSimpleName() + ": " + e.getMessage(),
                            k -> new AtomicInteger()).incrementAndGet();
                }
            });
        
        }
        start.countDown();   // let all threads proceed
        done.await();
        pool.shutdown();

        // 3. assert
        int dbStock = productRepository.findById(productId).orElseThrow().getStock();
        int redisStock = Integer.parseInt(redis.opsForValue().get("seckill:stock:" + productId));
        long orders = orderRepository.countByProduct_Id(productId);

        System.out.printf("success=%d soldOut=%d duplicate=%d other=%d dbStock=%d redisStock=%d orders=%d%n",
                success.get(), soldOut.get(), duplicate.get(), other.get(), dbStock, redisStock, orders);
        errors.forEach((k, v) -> System.out.println(v + " x " + k));

        assertEquals(0, other.get());              // no unexpected errors
        assertEquals(stock, success.get());        // stock should be exactly sold out
        assertEquals(0, dbStock);                  // database stock should be 0
        assertEquals(dbStock, redisStock);         // Redis same as database
        assertEquals(success.get(), orders);       // order num = success
    }

    

    @BeforeEach
    void cleanRedis() {
        redis.execute((RedisCallback<Void>) conn -> {
            conn.serverCommands().flushDb();   // empty 1 not 0
            return null;
        });
    }

}