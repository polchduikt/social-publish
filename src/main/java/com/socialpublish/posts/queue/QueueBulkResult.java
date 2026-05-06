package com.socialpublish.posts.queue;

public record QueueBulkResult(
        int processed,
        int skipped
) {
}
