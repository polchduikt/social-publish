package com.socialpublish.scheduling.config;

import com.socialpublish.scheduling.jobs.WeeklyStatsJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WeeklyStatsConfig {

    @Bean
    public JobDetail weeklyStatsJobDetail() {
        return JobBuilder.newJob(WeeklyStatsJob.class)
                .withIdentity("weeklyStatsJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger weeklyStatsJobTrigger() {
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule("0 0 9 ? * MON");
        return TriggerBuilder.newTrigger()
                .forJob(weeklyStatsJobDetail())
                .withIdentity("weeklyStatsTrigger")
                .withSchedule(scheduleBuilder)
                .build();
    }
}
