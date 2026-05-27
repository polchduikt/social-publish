package com.socialpublish.posts.service;

import com.socialpublish.media.dto.MediaUploadResult;
import com.socialpublish.media.entity.PostMedia;
import com.socialpublish.media.service.CloudinaryMediaService;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.mapper.PostMediaMapper;
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
    private final PostMediaMapper postMediaMapper;

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
                .mapToObj(index -> postMediaMapper.toPostMedia(post, uploaded.get(index), startOrder + index))
                .toList();
        post.getMedia().addAll(newMedia);
    }

    public void deleteByPublicIds(List<String> publicIds) {
        cloudinaryMediaService.deleteByPublicIds(publicIds);
    }

    public void copyMedia(Post source, Post target, UUID ownerId) {
        List<String> sourceUrls = source.getMedia().stream()
                .map(PostMedia::getSecureUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();

        List<MediaUploadResult> copied = cloudinaryMediaService.copyImagesFromUrls(
                sourceUrls,
                ownerId,
                target.getMedia().size()
        );

        int startOrder = target.getMedia().size();
        List<PostMedia> clonedMedia = IntStream.range(0, copied.size())
                .mapToObj(index -> postMediaMapper.toPostMedia(target, copied.get(index), startOrder + index))
                .toList();
        target.getMedia().addAll(clonedMedia);
    }

    private void renumberMediaOrder(List<PostMedia> mediaList) {
        for (int index = 0; index < mediaList.size(); index++) {
            mediaList.get(index).setSortOrder(index);
        }
    }
}