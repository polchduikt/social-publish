package com.socialpublish.scheduling.jobs;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.posts.service.PostStatusMachine;
import com.socialpublish.publishing.service.PublishingProducer;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class PublishPostJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(PublishPostJob.class);

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PostStatusMachine statusMachine;

    @Autowired
    private PublishingProducer publishingProducer;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String postIdStr = context.getJobDetail().getJobDataMap().getString("postId");
        UUID postId = UUID.fromString(postIdStr);

        log.info("Quartz job triggered for post {}", postId);

        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            log.warn("Post {} not found, skipping", postId);
            return;
        }

        if (post.getStatus() != PostStatus.SCHEDULED) {
            log.info("Post {} is {}, not SCHEDULED — skipping", postId, post.getStatus());
            return;
        }

        statusMachine.transition(post, PostStatus.PUBLISHING);
        post.setRetryCount(0);
        postRepository.save(post);

        publishingProducer.sendPublishRequest(postId);
        log.info("Post {} transitioned to PUBLISHING and sent to RabbitMQ", postId);
    }
}
