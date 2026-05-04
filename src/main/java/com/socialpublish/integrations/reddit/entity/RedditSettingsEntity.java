package com.socialpublish.integrations.reddit.entity;

import com.socialpublish.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "integration_reddit_settings")
@Getter
@Setter
public class RedditSettingsEntity {
    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "access_token", length = 1000)
    private String accessToken;

    @Column(name = "refresh_token", length = 1000)
    private String refreshToken;
    
    @Column(name = "default_subreddit")
    private String defaultSubreddit;

    private boolean enabled = true;
}
