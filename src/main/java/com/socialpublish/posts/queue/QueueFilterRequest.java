package com.socialpublish.posts.queue;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QueueFilterRequest {

    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 100;

    private String status;
    private String search;
    private String tag;
    private List<QueuePlatformFilter> platform = new ArrayList<>();
    private List<QueuePostTypeFilter> type = new ArrayList<>();
    private QueueDateRangeFilter dateRange = QueueDateRangeFilter.ALL;
    private QueueSortOption sort = QueueSortOption.NEWEST;
    private Integer size = DEFAULT_SIZE;

    public String getStatus() {
        return status == null ? "" : status;
    }

    public List<QueuePlatformFilter> getPlatform() {
        return platform == null ? new ArrayList<>() : platform;
    }

    public List<QueuePostTypeFilter> getType() {
        return type == null ? new ArrayList<>() : type;
    }

    public QueueDateRangeFilter getDateRange() {
        return dateRange == null ? QueueDateRangeFilter.ALL : dateRange;
    }

    public QueueSortOption getSort() {
        return sort == null ? QueueSortOption.NEWEST : sort;
    }

    public int getSize() {
        int value = size == null ? DEFAULT_SIZE : size;
        if (value < DEFAULT_SIZE) {
            return DEFAULT_SIZE;
        }
        return Math.min(value, MAX_SIZE);
    }

    public String normalizedSearch() {
        return search == null ? "" : search.trim();
    }

    public String normalizedTag() {
        if (tag == null) {
            return "";
        }
        String normalized = tag.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    public boolean hasSearch() {
        return !normalizedSearch().isBlank();
    }

    public boolean hasTag() {
        return !normalizedTag().isBlank();
    }
}
