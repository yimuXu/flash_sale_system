package com.yimu.flash_sale_system;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

// Same test database and Redis db as the other tests. The context is closed after the class:
// a cached context keeps its RabbitMQ listeners running and would consume other tests' messages.
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/flash_sale_test",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.redis.database=1"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FlashSaleSystemApplicationTests {

	@Test
	void contextLoads() {
	}

}
