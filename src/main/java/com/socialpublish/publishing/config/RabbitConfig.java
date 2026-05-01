package com.socialpublish.publishing.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "post.exchange";
    public static final String PUBLISH_QUEUE = "post.publish";
    public static final String RETRY_QUEUE = "post.publish.retry";
    public static final String PUBLISH_KEY = "publish";
    public static final String RETRY_KEY = "retry";

    @Value("${app.publishing.retry-delay-ms:30000}")
    private long retryDelayMs;

    @Bean
    public DirectExchange postExchange() {
        return new DirectExchange(EXCHANGE);
    }

    @Bean
    public Queue publishQueue() {
        return QueueBuilder.durable(PUBLISH_QUEUE).build();
    }

    @Bean
    public Queue retryQueue() {
        return QueueBuilder.durable(RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", PUBLISH_KEY)
                .withArgument("x-message-ttl", retryDelayMs)
                .build();
    }

    @Bean
    public Binding publishBinding(Queue publishQueue, DirectExchange postExchange) {
        return BindingBuilder.bind(publishQueue).to(postExchange).with(PUBLISH_KEY);
    }

    @Bean
    public Binding retryBinding(Queue retryQueue, DirectExchange postExchange) {
        return BindingBuilder.bind(retryQueue).to(postExchange).with(RETRY_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
