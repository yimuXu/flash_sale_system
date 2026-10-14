package com.yimu.flash_sale_system;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.yimu.flash_sale_system.config.RabbitConfig;
import com.yimu.flash_sale_system.entity.Product;
import com.yimu.flash_sale_system.exception.ServiceUnavailableException;
import com.yimu.flash_sale_system.repository.ProductRepository;
import com.yimu.flash_sale_system.service.RedisKeys;
import com.yimu.flash_sale_system.service.SeckillService;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/flash_sale_test",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.redis.database=1"
})
// close the context after this class so its RabbitMQ listeners do not consume other tests' messages
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CircuitBreakerTest {
    @Autowired SeckillService seckillService;
    @Autowired ProductRepository productRepository;
    @Autowired StringRedisTemplate redis;

    // replaces the real RabbitTemplate so the test decides whether publishing works
    @MockitoBean RabbitTemplate rabbitTemplate;

    private long nextUserId = 1;

    @BeforeEach
    void clean() {
        redis.execute((RedisCallback<Void>) conn -> { conn.serverCommands().flushDb(); return null; });
    }

    @Test
    void opensWhenPublishingFailsAndClosesWhenItWorksAgain() {
        Product product = new Product();
        product.setName("test"); product.setPrice(BigDecimal.ONE); product.setStock(100);
        Long productId = productRepository.save(product).getId();
        seckillService.preheatProduct(productId);
        String stockKey = RedisKeys.stockKey(productId);
        String usersKey = RedisKeys.usersKey(productId);

        // 1. the broker is down: every publish fails, and after enough failures the breaker opens
        doThrow(new AmqpConnectException(new ConnectException("broker down")))
                .when(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY), any(Object.class));

        int brokerFailures = 0;
        boolean opened = false;
        for (int i = 0; i < 80 && !opened; i++) {
            try {
                seckillService.seckill(productId, nextUserId++);
                fail("publishing is failing, the request must not succeed");
            } catch (AmqpException e) {
                brokerFailures++;                 // reached the broker and failed
            } catch (ServiceUnavailableException e) {
                opened = true;                    // rejected by the open breaker
            }
        }
        System.out.printf("breaker opened after %d failed publishes%n", brokerFailures);
        assertTrue(opened, "the breaker should open while publishing keeps failing");
        assertTrue(brokerFailures >= 20, "it should not open before the minimum number of requests");
        // every failed request gave its stock back
        assertEquals("100", redis.opsForValue().get(stockKey));
        assertEquals(0, redis.opsForSet().size(usersKey));

        // 2. while it is open, requests are rejected before Redis or the broker is touched
        clearInvocations(rabbitTemplate);
        for (int i = 0; i < 5; i++) {
            assertThrows(ServiceUnavailableException.class, () -> seckillService.seckill(productId, nextUserId++));
        }
        verifyNoInteractions(rabbitTemplate);
        assertEquals("100", redis.opsForValue().get(stockKey));
        assertEquals(0, redis.opsForSet().size(usersKey));

        // 3. the broker is back: once the open period is over, one probe request gets through,
        //    succeeds, and the breaker closes again
        doNothing().when(rabbitTemplate).convertAndSend(eq(RabbitConfig.EXCHANGE), eq(RabbitConfig.ROUTING_KEY), any(Object.class));
        await().atMost(Duration.ofSeconds(20)).pollInterval(Duration.ofMillis(500)).untilAsserted(() ->
                assertDoesNotThrow(() -> seckillService.seckill(productId, nextUserId++)));
        assertDoesNotThrow(() -> seckillService.seckill(productId, nextUserId++));
        // only the two successful requests took stock
        assertEquals("98", redis.opsForValue().get(stockKey));
        assertEquals(2, redis.opsForSet().size(usersKey));
    }
}
