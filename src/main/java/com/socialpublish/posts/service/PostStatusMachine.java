package com.socialpublish.posts.service;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.exception.PostValidationException;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;

import static com.socialpublish.posts.entity.PostStatus.*;

@Component
public class PostStatusMachine {

    private static final Map<PostStatus, Set<PostStatus>> TRANSITIONS = Map.of(
            DRAFT, Set.of(SCHEDULED, CANCELLED),
            SCHEDULED, Set.of(PUBLISHING, DRAFT, CANCELLED),
            PUBLISHING, Set.of(PUBLISHED, RETRYING, FAILED),
            RETRYING, Set.of(PUBLISHING, FAILED, CANCELLED),
            FAILED, Set.of(DRAFT),
            CANCELLED, Set.of(DRAFT),
            PUBLISHED, Set.of()
    );

    public void validateTransition(PostStatus from, PostStatus to) {
        Set<PostStatus> allowed = TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new PostValidationException(
                    "Cannot transition from " + from + " to " + to
            );
        }
    }

    public void transition(Post post, PostStatus target) {
        validateTransition(post.getStatus(), target);
        post.setStatus(target);
    }
}
