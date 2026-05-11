package com.socialpublish.media.repository;

import com.socialpublish.media.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PostMediaRepository extends JpaRepository<PostMedia, UUID> {

    List<PostMedia> findByPostIdOrderBySortOrderAsc(UUID postId);
}
