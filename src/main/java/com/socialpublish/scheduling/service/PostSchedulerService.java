package com.socialpublish.scheduling.service;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.scheduling.jobs.PublishPostJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class PostSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(PostSchedulerService.class);
    private static final String JOB_GROUP = "post-publishing";

    private final Scheduler scheduler;

    public PostSchedulerService(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void schedulePost(Post post) {
        try {
            JobKey jobKey = jobKey(post.getId());
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
            }

            JobDetail jobDetail = JobBuilder.newJob(PublishPostJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("postId", post.getId().toString())
                    .build();

            SimpleTrigger trigger = (SimpleTrigger) TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + post.getId(), JOB_GROUP)
                    .startAt(Date.from(post.getScheduledAt()))
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled post {} for {}", post.getId(), post.getScheduledAt());
        } catch (SchedulerException ex) {
            throw new RuntimeException("Failed to schedule post " + post.getId(), ex);
        }
    }

    public void cancelScheduledPost(UUID postId) {
        try {
            JobKey jobKey = jobKey(postId);
            if (scheduler.checkExists(jobKey)) {
                scheduler.deleteJob(jobKey);
                log.info("Cancelled scheduled job for post {}", postId);
            }
        } catch (SchedulerException ex) {
            throw new RuntimeException("Failed to cancel scheduled post " + postId, ex);
        }
    }

    private JobKey jobKey(UUID postId) {
        return new JobKey("publish-" + postId, JOB_GROUP);
    }
}
