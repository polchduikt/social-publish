package com.socialpublish.integrations.notion.entity;

import com.socialpublish.auth.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.UUID;

import com.socialpublish.integrations.entity.BaseIntegrationSettings;

@Entity
@Table(name = "notion_settings")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class NotionSettingsEntity implements BaseIntegrationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 500)
    private String apiToken;

    @Column(nullable = false, length = 200)
    private String databaseId;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
}
