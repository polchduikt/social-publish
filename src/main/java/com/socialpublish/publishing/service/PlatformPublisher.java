package com.socialpublish.publishing.service;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.publishing.entity.Platform;

public interface PlatformPublisher {
    void publish(Post post);
    Platform getPlatform();
}
