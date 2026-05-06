package com.socialpublish.posts.dto;

public record PostMediaView(
        String publicId,
        String url,
        int sortOrder
) {
    public boolean isVideo() {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalized = url.toLowerCase();
        return normalized.contains("/video/upload/")
                || normalized.endsWith(".mp4")
                || normalized.endsWith(".mov")
                || normalized.endsWith(".webm")
                || normalized.endsWith(".m4v");
    }
}
