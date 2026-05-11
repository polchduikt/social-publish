package com.socialpublish.publishing.worker;

import com.socialpublish.publishing.config.RabbitConfig;
import com.socialpublish.publishing.dto.PublishPostMessage;
import com.socialpublish.publishing.service.PublishingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublishingConsumer {

    private final PublishingService publishingService;

    @RabbitListener(queues = RabbitConfig.PUBLISH_QUEUE)
    public void onPublishRequest(PublishPostMessage message) {
        log.info("Received publish request for post {} (attempt {})", message.postId(), message.attempt());
        publishingService.attemptPublish(message.postId(), message.attempt());
    }
}
