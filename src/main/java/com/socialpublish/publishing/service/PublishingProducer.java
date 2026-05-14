package com.socialpublish.publishing.service;

import com.socialpublish.publishing.config.RabbitConfig;
import com.socialpublish.publishing.dto.PublishPostMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class PublishingProducer {

    private final RabbitTemplate rabbitTemplate;

    public PublishingProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendPublishRequest(UUID postId, boolean scheduled) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.PUBLISH_KEY,
                new PublishPostMessage(postId, 1, scheduled)
        );
    }

    public void sendRetryRequest(UUID postId, int attempt, boolean scheduled) {
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.RETRY_KEY,
                new PublishPostMessage(postId, attempt, scheduled)
        );
    }
}
