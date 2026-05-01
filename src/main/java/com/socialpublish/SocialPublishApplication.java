package com.socialpublish;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SocialPublishApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialPublishApplication.class, args);
    }

}
