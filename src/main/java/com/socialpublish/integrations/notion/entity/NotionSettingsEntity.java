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
@Table(name = "notion_settings", indexes = {
    @Index(name = "idx_notion_user", columnList = "user_id")
})
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class NotionSettingsEntity implements BaseIntegrationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 500)
    private String apiToken;

    @Column(nullable = false, length = 200)
    private String databaseId;

    @Column(nullable = true)
    private String label;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotionSettingsEntity that = (NotionSettingsEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
