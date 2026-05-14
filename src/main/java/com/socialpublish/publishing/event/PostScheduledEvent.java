package com.socialpublish.publishing.event;

import java.util.UUID;

public record PostScheduledEvent(UUID postId, boolean scheduled) {
}
