package com.yimu.flash_sale_system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.yimu.flash_sale_system.entity.Product;
import com.yimu.flash_sale_system.entity.User;
import com.yimu.flash_sale_system.entity.UserRoles;
import com.yimu.flash_sale_system.exception.OutOfStockException;
import com.yimu.flash_sale_system.repository.OrderRepository;
import com.yimu.flash_sale_system.repository.ProductRepository;
import com.yimu.flash_sale_system.repository.UserRepository;
import com.yimu.flash_sale_system.service.SeckillService;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/flash_sale_test",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})

public class SeckillConcurrencyTest {
    @Autowired SeckillService seckillService;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;

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

        // 2. let 100 threads compete
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(), lockFail = new AtomicInteger(),
                      soldOut = new AtomicInteger(), other = new AtomicInteger();

        for (Long userId : userIds) {
            pool.submit(() -> {
                try {
                    start.await();
                    seckillService.seckill(productId, userId);
                    success.incrementAndGet();
                } catch (ObjectOptimisticLockingFailureException e) {
                    lockFail.incrementAndGet();
                } catch (OutOfStockException e) {
                    soldOut.incrementAndGet();
                } catch (Exception e) {
                    other.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();   // let all threads proceed
        done.await();
        pool.shutdown();

        // 3. assert
        int remaining = productRepository.findById(productId).orElseThrow().getStock();
        System.out.printf("success=%d lockFail=%d soldOut=%d other=%d remaining=%d%n",
                success.get(), lockFail.get(), soldOut.get(), other.get(), remaining);

        assertTrue(remaining >= 0);                          // stock should not be negative
        assertEquals(stock - remaining, success.get());      // deducted stock = success count
        assertEquals(success.get(),                          // order count = success count
                orderRepository.countByProduct_Id(productId));
    }
}