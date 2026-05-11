package com.socialpublish.scheduling.jobs;

import com.socialpublish.publishing.service.PublishingService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;

@Slf4j
public class PublishPostJob implements Job {

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
