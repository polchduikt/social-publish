package com.socialpublish.media.entity;

import com.socialpublish.posts.entity.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "post_media")
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class PostMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(nullable = false, length = 255)
    private String publicId;

    @Column(nullable = false, length = 1000)
    private String secureUrl;

    @Column(length = 50)
    private String format;

    private int width;
    private int height;
    private long bytes;

    @Column(nullable = false)
    private int sortOrder;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
