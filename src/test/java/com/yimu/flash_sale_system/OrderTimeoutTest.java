package com.yimu.flash_sale_system;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;

import com.yimu.flash_sale_system.config.RabbitConfig;
import com.yimu.flash_sale_system.entity.OrderStatus;
import com.yimu.flash_sale_system.entity.Product;
import com.yimu.flash_sale_system.entity.User;
import com.yimu.flash_sale_system.entity.UserRoles;
import com.yimu.flash_sale_system.exception.DuplicateOrderException;
import com.yimu.flash_sale_system.exception.OrderNotPayableException;
import com.yimu.flash_sale_system.exception.ResourceNotFoundException;
import com.yimu.flash_sale_system.repository.OrderRepository;
import com.yimu.flash_sale_system.repository.ProductRepository;
import com.yimu.flash_sale_system.repository.UserRepository;
import com.yimu.flash_sale_system.service.OrderService;
import com.yimu.flash_sale_system.service.RedisKeys;
import com.yimu.flash_sale_system.service.SeckillService;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/flash_sale_test",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.redis.database=1",
    "seckill.payment-timeout=3s"     // short timeout so the test does not wait 15 minutes
})
// close the context after this class so its RabbitMQ listeners do not consume other tests' messages
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrderTimeoutTest {
    @Autowired SeckillService seckillService;
    @Autowired OrderService orderService;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired StringRedisTemplate redis;
    @Autowired AmqpAdmin amqpAdmin;

    @BeforeEach
    void clean() {
        redis.execute((RedisCallback<Void>) conn -> { conn.serverCommands().flushDb(); return null; });
        amqpAdmin.purgeQueue(RabbitConfig.QUEUE);
        amqpAdmin.purgeQueue(RabbitConfig.DLQ);
        // messages left by another test with a 15 minute TTL would sit at the head of the
        // delay queue and hold back this test's 3 second messages
        amqpAdmin.purgeQueue(RabbitConfig.DELAY_QUEUE);
        amqpAdmin.purgeQueue(RabbitConfig.TIMEOUT_QUEUE);
    }

    @Test
    void unpaidOrderIsCancelledAndPaidOrderIsKept() {
        // 1. one product with stock 2, two users who both win
        Product product = new Product();
        product.setName("test"); product.setPrice(BigDecimal.ONE); product.setStock(2);
        Long productId = productRepository.save(product).getId();
        Long payer = newUser();
        Long idler = newUser();
        seckillService.preheatProduct(productId);

        String paidNo = seckillService.seckill(productId, payer);
        String unpaidNo = seckillService.seckill(productId, idler);

        // 2. wait for the consumer to write both orders; both hold stock while awaiting payment
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertEquals(2, orderRepository.countByProduct_Id(productId)));
        assertEquals(OrderStatus.PENDING, status(paidNo, payer));
        assertEquals(OrderStatus.PENDING, status(unpaidNo, idler));
        assertEquals(0, dbStock(productId));

        // 3. one user pays inside the timeout
        assertEquals(OrderStatus.PAID, orderService.pay(payer, paidNo).getStatus());

        // 4. after the timeout the unpaid order is cancelled and its stock is returned in both stores
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertEquals(OrderStatus.CANCELLED, status(unpaidNo, idler));
            assertEquals(1, dbStock(productId));
            assertEquals("1", redis.opsForValue().get(RedisKeys.stockKey(productId)));
        });

        // 5. the paid order is left alone when its own timeout message arrives
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3))
                .until(() -> status(paidNo, payer) == OrderStatus.PAID);
        assertEquals(1, dbStock(productId));

        // 6. an order can only be paid once, a cancelled order cannot be paid,
        //    and nobody can pay an order that is not theirs
        assertThrows(OrderNotPayableException.class, () -> orderService.pay(payer, paidNo));
        assertThrows(OrderNotPayableException.class, () -> orderService.pay(idler, unpaidNo));
        assertThrows(ResourceNotFoundException.class, () -> orderService.pay(idler, paidNo));

        // 7. the user whose order was cancelled cannot buy the same product again
        assertThrows(DuplicateOrderException.class, () -> seckillService.seckill(productId, idler));
    }

    private Long newUser() {
        User u = new User();
        u.setEmail(UUID.randomUUID() + "@test.com"); u.setPassword("x"); u.setRole(UserRoles.USER);
        return userRepository.save(u).getId();
    }

    private OrderStatus status(String orderNo, Long userId) {
        return orderRepository.findByOrderNoAndUser_Id(orderNo, userId).orElseThrow().getStatus();
    }

    private int dbStock(Long productId) {
        return productRepository.findById(productId).orElseThrow().getStock();
    }
}
