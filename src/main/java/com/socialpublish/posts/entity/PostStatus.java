package com.socialpublish.posts.entity;

import java.util.List;

public enum PostStatus {
    DRAFT,
    SCHEDULED,
    PUBLISHING,
    PUBLISHED,
    RETRYING,
    FAILED,
    CANCELLED;

    private static final List<PostStatus> USER_SETTABLE = List.of(DRAFT, SCHEDULED, CANCELLED);

    public static List<PostStatus> userSettable() {
        return USER_SETTABLE;
    }
}
