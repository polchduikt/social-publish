package com.socialpublish.publishing.worker;

import com.socialpublish.publishing.config.RabbitConfig;
import com.socialpublish.publishing.dto.PublishPostMessage;
import com.socialpublish.publishing.service.PublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PublishingConsumer {

    private static final Logger log = LoggerFactory.getLogger(PublishingConsumer.class);

    private final PublishingService publishingService;

    public PublishingConsumer(PublishingService publishingService) {
        this.publishingService = publishingService;
    }

    @RabbitListener(queues = RabbitConfig.PUBLISH_QUEUE)
    public void onPublishRequest(PublishPostMessage message) {
        log.info("Received publish request for post {} (attempt {})", message.postId(), message.attempt());
        publishingService.attemptPublish(message.postId(), message.attempt());
    }
}
