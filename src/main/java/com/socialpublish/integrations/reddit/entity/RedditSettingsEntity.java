package com.socialpublish.integrations.reddit.entity;

import com.socialpublish.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.UUID;
import com.socialpublish.integrations.entity.BaseIntegrationSettings;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "reddit_settings")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class RedditSettingsEntity implements BaseIntegrationSettings {
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
