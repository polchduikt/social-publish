package com.socialpublish.media.dto;

public record MediaUploadResult(
        String publicId,
        String secureUrl,
        String format,
        int width,
        int height,
        long bytes
) {
}
