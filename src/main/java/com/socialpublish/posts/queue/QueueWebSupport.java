package com.socialpublish.posts.queue;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class QueueWebSupport {

    public void applySelectionError(RedirectAttributes redirectAttributes) {
        redirectAttributes.addAttribute("error", "Select at least one post");
    }

    public void applyBulkResultFeedback(QueueBulkResult result, RedirectAttributes redirectAttributes) {
        if (result.processed() == 0) {
            redirectAttributes.addAttribute("error", "No posts were updated");
            return;
        }

        if (result.skipped() > 0) {
            redirectAttributes.addAttribute(
                    "message",
                    "Updated " + result.processed() + " posts, skipped " + result.skipped()
            );
            return;
        }

        redirectAttributes.addAttribute("message", "Updated " + result.processed() + " posts");
    }

    public String buildRedirectUrl(QueueFilterRequest filters) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/queue");

        if (filters.getStatus() != null) {
            builder.queryParam("status", filters.getStatus().name());
        }
        if (filters.hasSearch()) {
            builder.queryParam("search", filters.normalizedSearch());
        }
        if (filters.hasTag()) {
            builder.queryParam("tag", filters.normalizedTag());
        }
        if (filters.getPlatform() != QueuePlatformFilter.ALL) {
            builder.queryParam("platform", filters.getPlatform().name());
        }
        if (filters.getType() != QueuePostTypeFilter.ALL) {
            builder.queryParam("type", filters.getType().name());
        }
        if (filters.getDateRange() != QueueDateRangeFilter.ALL) {
            builder.queryParam("dateRange", filters.getDateRange().name());
        }
        if (filters.getSort() != QueueSortOption.NEWEST) {
            builder.queryParam("sort", filters.getSort().name());
        }
        builder.queryParam("size", filters.getSize());
        return builder.toUriString();
    }
}
