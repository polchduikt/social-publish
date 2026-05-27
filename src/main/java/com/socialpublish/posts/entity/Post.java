package com.socialpublish.posts.entity;

import com.socialpublish.auth.entity.User;
import com.socialpublish.media.entity.PostMedia;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_posts_owner", columnList = "owner_id"),
    @Index(name = "idx_posts_status_scheduled", columnList = "status, scheduled_at"),
    @Index(name = "idx_posts_updated", columnList = "updated_at DESC")
})
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 5000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    private Instant scheduledAt;

    private Instant publishedAt;

    @Column(length = 500)
    private String failedReason;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private int maxRetries = 3;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(length = 200)
    private String platforms = "";

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<PostMedia> media = new ArrayList<>();

    @Column(nullable = false)
    private boolean recurring = false;

    @Column(length = 50)
    private String recurringDays;

    @Column(length = 5)
    private String recurringTime;

    private Instant recurringEndDate;

    @Column(name = "parent_recurring_id")
    private UUID parentRecurringId;

    @Column(nullable = false)
    private boolean silentMode = false;

    @Column(columnDefinition = "TEXT")
    private String inlineButtons;

    @Column(length = 250)
    private String pollQuestion;

    @Column(columnDefinition = "TEXT")
    private String pollOptions;

    @Column(nullable = false)
    private boolean pollMultipleAnswers = false;

    @Column(nullable = false)
    private boolean pollIsQuiz = false;

    private Integer pollCorrectOptionId;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (status == null) {
            status = PostStatus.DRAFT;
        }
    }

    public void addMedia(PostMedia mediaItem) {
        media.add(mediaItem);
        mediaItem.setPost(this);
    }

    public void removeMedia(PostMedia mediaItem) {
        media.remove(mediaItem);
        mediaItem.setPost(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Post post = (Post) o;
        return id != null && id.equals(post.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
