package com.yimu.flash_sale_system.config;

import org.springframework.amqp.core.Binding;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;


@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "seckill.exchange";
    public static final String QUEUE = "seckill.order.queue";
    public static final String ROUTING_KEY = "seckill.order";
    public static final String DLX = "seckill.dlx";
    public static final String DLQ = "seckill.order.dlq";
    public static final String DELAY_QUEUE = "order.delay.queue";
    public static final String TIMEOUT_EXCHANGE = "order.timeout.exchange";
    public static final String TIMEOUT_QUEUE = "order.timeout.queue";
    public static final String TIMEOUT_ROUTING_KEY = "order.timeout";

    @Bean DirectExchange seckillExchange() { return new DirectExchange(EXCHANGE); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange(DLX); }
    @Bean DirectExchange timeoutExchange() { return new DirectExchange(TIMEOUT_EXCHANGE); }

    @Bean Queue seckillQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)          // messages that still fail after retries go to the dead letter queue
                .deadLetterRoutingKey(DLQ)
                .build();
    }
    @Bean Queue deadLetterQueue() { return QueueBuilder.durable(DLQ).build(); }

    // No consumer and no binding: messages are sent here with a TTL and wait until they expire
    @Bean Queue delayQueue() {
        return QueueBuilder.durable(DELAY_QUEUE)
                .deadLetterExchange(TIMEOUT_EXCHANGE)          // expired messages go to the timeout queue
                .deadLetterRoutingKey(TIMEOUT_ROUTING_KEY)
                .build();
    }
    @Bean Queue timeoutQueue() { return QueueBuilder.durable(TIMEOUT_QUEUE).build(); }

    @Bean Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(ROUTING_KEY);
    }
    @Bean Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ);
    }
    @Bean Binding timeoutBinding() {
        return BindingBuilder.bind(timeoutQueue()).to(timeoutExchange()).with(TIMEOUT_ROUTING_KEY);
    }

    @Bean MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();   // message is transmitted in JSON format
    }
}

