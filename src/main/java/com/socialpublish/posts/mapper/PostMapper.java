package com.socialpublish.posts.mapper;

import com.socialpublish.media.entity.PostMedia;
import com.socialpublish.posts.dto.PostMediaView;
import com.socialpublish.posts.dto.PostUpsertRequest;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.Post;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Mapper(componentModel = "spring")
public interface PostMapper {

    int EXCERPT_MAX_LENGTH = 88;
    int EXCERPT_TRIM_LENGTH = 85;

    @Mapping(target = "scheduledAt", source = "scheduledAt", qualifiedByName = "toLocalDateTime")
    @Mapping(target = "publishedAt", source = "publishedAt", qualifiedByName = "toLocalDateTime")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "toLocalDateTime")
    @Mapping(target = "updatedAt", source = "updatedAt", qualifiedByName = "toLocalDateTime")
    @Mapping(target = "platforms", source = "platforms", qualifiedByName = "normalizePlatformsRaw")
    @Mapping(target = "platformList", source = "platforms", qualifiedByName = "toPlatformList")
    @Mapping(target = "excerpt", source = "content", qualifiedByName = "toExcerpt")
    PostView toView(Post post);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "media", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "failedReason", ignore = true)
    @Mapping(target = "retryCount", ignore = true)
    @Mapping(target = "platforms", source = "platforms", qualifiedByName = "platformsToString")
    @Mapping(target = "scheduledAt", source = "scheduledAt", qualifiedByName = "toInstant")
    Post toEntity(PostUpsertRequest request);

    List<PostView> toViews(List<Post> posts);

    @Named("platformsToString")
    default String platformsToString(List<String> platforms) {
        return platforms == null || platforms.isEmpty() ? "" : String.join(",", platforms);
    }

    @Named("toInstant")
    default Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }


    @Mapping(target = "scheduledAt", source = "scheduledAt", qualifiedByName = "toLocalDateTime")
    @Mapping(target = "platforms", source = "platforms", qualifiedByName = "toPlatformList")
    PostUpsertRequest toUpsertRequest(Post post);

    default PostMediaView toPostMediaView(PostMedia media) {
        return new PostMediaView(media.getPublicId(), media.getSecureUrl(), media.getSortOrder());
    }

    @Named("toLocalDateTime")
    default LocalDateTime toLocalDateTime(Instant value) {
        if (value == null) {
            return null;
        }
        return value.atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Named("normalizePlatformsRaw")
    default String normalizePlatformsRaw(String value) {
        return value == null ? "" : value;
    }

    @Named("toPlatformList")
    default List<String> toPlatformList(String platforms) {
        if (platforms == null || platforms.isBlank()) {
            return List.of();
        }
        return Arrays.stream(platforms.split("[,;\\s]+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(token -> token.replaceAll("[^a-zA-Z0-9_:]", ""))
                .map(token -> token.toUpperCase(Locale.ROOT))
                .filter(token -> !token.isBlank())
                .toList();
    }

    @Named("toExcerpt")
    default String toExcerpt(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        if (content.length() <= EXCERPT_MAX_LENGTH) {
            return content;
        }
        return content.substring(0, EXCERPT_TRIM_LENGTH) + "...";
    }
}
