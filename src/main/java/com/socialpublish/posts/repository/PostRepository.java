package com.socialpublish.posts.repository;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
    Optional<Post> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<Post> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    List<Post> findTop10ByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    long countByOwnerId(UUID ownerId);

    long countByOwnerIdAndStatus(UUID ownerId, PostStatus status);
}
