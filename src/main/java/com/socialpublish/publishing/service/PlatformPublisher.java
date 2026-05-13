package com.socialpublish.publishing.service;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.publishing.entity.Platform;
import java.util.UUID;

public interface PlatformPublisher {
    void publish(Post post, UUID targetId);
    Platform getPlatform();
}
