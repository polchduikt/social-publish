package com.socialpublish.scheduling.service;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.scheduling.exception.SchedulingException;
import com.socialpublish.scheduling.jobs.PublishPostJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostSchedulerService {

    private static final String JOB_GROUP = "post-publishing";

    private final Scheduler scheduler;

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

            SimpleTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger-" + post.getId(), JOB_GROUP)
                    .startAt(Date.from(post.getScheduledAt()))
                    .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                            .withMisfireHandlingInstructionFireNow())
                    .build();

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled post {} for {}", post.getId(), post.getScheduledAt());
        } catch (SchedulerException ex) {
            throw new SchedulingException("Failed to schedule post " + post.getId(), ex);
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
            throw new SchedulingException("Failed to cancel scheduled post " + postId, ex);
        }
    }

    private JobKey jobKey(UUID postId) {
        return new JobKey("publish-" + postId, JOB_GROUP);
    }
}
