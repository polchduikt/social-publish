package com.socialpublish.posts.queue;

public record QueueStatsView(
        long scheduled,
        long published,
        long failed,
        long drafts
) {
}
