package com.socialpublish.posts.repository;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID>, JpaSpecificationExecutor<Post> {
    Optional<Post> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Post> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    List<Post> findTop10ByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    long countByOwnerId(UUID ownerId);

    long countByOwnerIdAndStatus(UUID ownerId, PostStatus status);

    List<Post> findByOwnerIdAndStatusOrderByUpdatedAtDesc(UUID ownerId, PostStatus status);

    Optional<Post> findFirstByOwnerIdAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
            UUID ownerId,
            PostStatus status,
            Instant now
    );

    List<Post> findByOwnerIdAndUpdatedAtAfter(UUID ownerId, Instant after);
}
