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

    @Bean DirectExchange seckillExchange() { return new DirectExchange(EXCHANGE); }
    @Bean DirectExchange deadLetterExchange() { return new DirectExchange(DLX); }

    @Bean Queue seckillQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DLX)          // rejected messages go to dead letter exchange
                .deadLetterRoutingKey(DLQ)
                .build();
    }
    @Bean Queue deadLetterQueue() { return QueueBuilder.durable(DLQ).build(); }

    @Bean Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(ROUTING_KEY);
    }
    @Bean Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange()).with(DLQ);
    }

    @Bean MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();   // message is transmitted in JSON format
    }
}

