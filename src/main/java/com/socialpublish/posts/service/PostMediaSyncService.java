package com.socialpublish.posts.service;

import com.socialpublish.media.dto.MediaUploadResult;
import com.socialpublish.media.entity.PostMedia;
import com.socialpublish.media.service.CloudinaryMediaService;
import com.socialpublish.posts.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class PostMediaSyncService {

    private final CloudinaryMediaService cloudinaryMediaService;

    public void syncMedia(
            Post post,
            UUID ownerId,
            List<MultipartFile> mediaFiles,
            List<String> removeMediaPublicIds
    ) {
        List<String> removeIds = removeMediaPublicIds == null ? List.of() : removeMediaPublicIds;
        if (!removeIds.isEmpty()) {
            List<String> toDelete = post.getMedia().stream()
                    .filter(media -> removeIds.contains(media.getPublicId()))
                    .map(PostMedia::getPublicId)
                    .toList();

            if (!toDelete.isEmpty()) {
                post.getMedia().removeIf(media -> toDelete.contains(media.getPublicId()));
                renumberMediaOrder(post.getMedia());
                cloudinaryMediaService.deleteByPublicIds(toDelete);
            }
        }

        List<MediaUploadResult> uploaded = cloudinaryMediaService.uploadImages(
                mediaFiles,
                ownerId,
                post.getMedia().size()
        );

        int startOrder = post.getMedia().size();
        List<PostMedia> newMedia = IntStream.range(0, uploaded.size())
                .mapToObj(index -> mapToPostMedia(post, uploaded.get(index), startOrder + index))
                .toList();
        post.getMedia().addAll(newMedia);
    }

    public void deleteByPublicIds(List<String> publicIds) {
        cloudinaryMediaService.deleteByPublicIds(publicIds);
    }

    private PostMedia mapToPostMedia(Post post, MediaUploadResult upload, int sortOrder) {
        PostMedia media = new PostMedia();
        media.setPost(post);
        media.setPublicId(upload.publicId());
        media.setSecureUrl(upload.secureUrl());
        media.setFormat(upload.format());
        media.setWidth(upload.width());
        media.setHeight(upload.height());
        media.setBytes(upload.bytes());
        media.setSortOrder(sortOrder);
        return media;
    }

    private void renumberMediaOrder(List<PostMedia> mediaList) {
        for (int index = 0; index < mediaList.size(); index++) {
            mediaList.get(index).setSortOrder(index);
        }
    }
}
