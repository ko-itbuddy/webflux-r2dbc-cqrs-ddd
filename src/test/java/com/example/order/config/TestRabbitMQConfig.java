package com.example.order.config;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@Configuration
public class TestRabbitMQConfig {

    @Bean
    @Primary
    public RabbitProperties rabbitProperties() {
        RabbitProperties props = new RabbitProperties();
        props.setHost("localhost");
        props.setPort(5672);
        props.setUsername("guest");
        props.setPassword("guest");
        return props;
    }

    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        return mock(ConnectionFactory.class);
    }

    @Bean
    @Primary
    public AmqpTemplate amqpTemplate() {
        return mock(AmqpTemplate.class);
    }
}
