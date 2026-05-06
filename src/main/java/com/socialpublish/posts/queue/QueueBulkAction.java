package com.socialpublish.posts.queue;

public enum QueueBulkAction {
    PUBLISH_NOW,
    DELETE,
    RESCHEDULE,
    DUPLICATE,
    MOVE_TO_DRAFT,
    RETRY_FAILED
}
