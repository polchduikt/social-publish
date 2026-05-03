package com.socialpublish.scheduling.jobs;

import com.socialpublish.publishing.service.PublishingService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

public class PublishPostJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(PublishPostJob.class);

    @Autowired
    private PublishingService publishingService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String postIdStr = context.getJobDetail().getJobDataMap().getString("postId");
        UUID postId = UUID.fromString(postIdStr);

        log.info("Quartz job triggered for post {}", postId);
        publishingService.startScheduledPublish(postId);
    }
}
