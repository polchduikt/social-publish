package com.socialpublish.media.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.socialpublish.media.config.CloudinaryProperties;
import com.socialpublish.media.dto.MediaUploadResult;
import com.socialpublish.posts.exception.PostValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryMediaService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final int MAX_FILES_PER_POST = 10;

    private final Cloudinary cloudinary;
    private final CloudinaryProperties cloudinaryProperties;

    public List<MediaUploadResult> uploadImages(List<MultipartFile> files, UUID ownerId, int existingCount) {
        List<MultipartFile> safeFiles = files == null
                ? List.of()
                : files.stream().filter(file -> file != null && !file.isEmpty()).toList();

        if (safeFiles.isEmpty()) {
            return List.of();
        }
        validateCloudinaryConfigured();

        if (existingCount + safeFiles.size() > MAX_FILES_PER_POST) {
            throw new PostValidationException("You can attach up to " + MAX_FILES_PER_POST + " photos");
        }

        List<MediaUploadResult> uploaded = new ArrayList<>();
        for (MultipartFile file : safeFiles) {
            validateImage(file);
            uploaded.add(uploadSingle(file, ownerId));
        }
        return uploaded;
    }

    public List<MediaUploadResult> copyImagesFromUrls(List<String> urls, UUID ownerId, int existingCount) {
        List<String> safeUrls = urls == null
                ? List.of()
                : urls.stream()
                .filter(url -> url != null && !url.isBlank())
                .toList();

        if (safeUrls.isEmpty()) {
            return List.of();
        }

        validateCloudinaryConfigured();
        if (existingCount + safeUrls.size() > MAX_FILES_PER_POST) {
            throw new PostValidationException("You can attach up to " + MAX_FILES_PER_POST + " photos");
        }

        List<MediaUploadResult> copied = new ArrayList<>();
        for (String url : safeUrls) {
            copied.add(uploadRemoteUrl(url, ownerId));
        }
        return copied;
    }

    public void deleteByPublicIds(List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return;
        }

        for (String publicId : publicIds) {
            if (publicId == null || publicId.isBlank()) {
                continue;
            }
            try {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            } catch (Exception ex) {
                log.warn("Failed to delete Cloudinary asset {}", publicId, ex);
            }
        }
    }

    private void validateCloudinaryConfigured() {
        if (isBlank(cloudinaryProperties.getCloudName())
                || isBlank(cloudinaryProperties.getApiKey())
                || isBlank(cloudinaryProperties.getApiSecret())) {
            throw new PostValidationException("Cloudinary is not configured. Set cloud_name, api_key and api_secret");
        }
    }

    private MediaUploadResult uploadSingle(MultipartFile file, UUID ownerId) {
        try {
            Map<?, ?> response = cloudinary.uploader().upload(file.getBytes(), uploadOptions(ownerId));
            return toUploadResult(response);
        } catch (IOException ex) {
            throw new PostValidationException("Failed to read uploaded file");
        } catch (Exception ex) {
            throw new PostValidationException("Failed to upload image to Cloudinary");
        }
    }

    private MediaUploadResult uploadRemoteUrl(String url, UUID ownerId) {
        try {
            Map<?, ?> response = cloudinary.uploader().upload(url, uploadOptions(ownerId));
            return toUploadResult(response);
        } catch (Exception ex) {
            throw new PostValidationException("Failed to copy image to Cloudinary");
        }
    }

    private Map<String, Object> uploadOptions(UUID ownerId) {
        return ObjectUtils.asMap(
                "folder", "social-publish/posts/" + ownerId,
                "resource_type", "image",
                "overwrite", false
        );
    }

    private MediaUploadResult toUploadResult(Map<?, ?> response) {
        String publicId = readString(response.get("public_id"));
        String secureUrl = readString(response.get("secure_url"));
        String format = readString(response.get("format"));
        int width = readInt(response.get("width"));
        int height = readInt(response.get("height"));
        long bytes = readLong(response.get("bytes"));
        return new MediaUploadResult(publicId, secureUrl, format, width, height, bytes);
    }

    private void validateImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new PostValidationException("Only image files are allowed");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new PostValidationException("Each image must be up to 10 MB");
        }
    }

    private String readString(Object value) {
        return value == null ? "" : value.toString();
    }

    private int readInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return 0;
    }

    private long readLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
