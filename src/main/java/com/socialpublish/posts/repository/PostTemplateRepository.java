package com.socialpublish.posts.repository;

import com.socialpublish.posts.entity.PostTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PostTemplateRepository extends JpaRepository<PostTemplate, UUID> {
    List<PostTemplate> findAllByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);
}
