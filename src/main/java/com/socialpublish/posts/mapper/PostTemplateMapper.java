package com.socialpublish.posts.mapper;

import com.socialpublish.posts.dto.CreatePostTemplateRequest;
import com.socialpublish.posts.dto.PostTemplateDto;
import com.socialpublish.posts.dto.PostUpsertRequest;
import com.socialpublish.posts.entity.PostTemplate;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring")
public interface PostTemplateMapper {

    @Mapping(target = "formattedPlatforms", expression = "java(toFormattedPlatforms(template, labels))")
    PostTemplateDto toDto(PostTemplate template, @Context Map<String, String> labels);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "platforms", expression = "java(request.platforms() != null ? String.join(\",\", request.platforms()) : \"\")")
    PostTemplate toEntity(CreatePostTemplateRequest request);

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "scheduledAt", ignore = true)
    @Mapping(target = "recurring", ignore = true)
    @Mapping(target = "recurringDays", ignore = true)
    @Mapping(target = "recurringTime", ignore = true)
    @Mapping(target = "recurringEndDate", ignore = true)
    @Mapping(target = "platforms", source = "platforms", qualifiedByName = "platformsToList")
    PostUpsertRequest toUpsertRequest(PostTemplate template);

    @Named("platformsToList")
    default List<String> platformsToList(String platforms) {
        if (platforms == null || platforms.isBlank()) {
            return List.of();
        }
        return Arrays.asList(platforms.split(","));
    }

    default List<String> toFormattedPlatforms(PostTemplate template, Map<String, String> labels) {
        List<String> formatted = new ArrayList<>();
        if (template.getPlatforms() != null && !template.getPlatforms().isBlank()) {
            for (String p : template.getPlatforms().split(",")) {
                formatted.add(labels.getOrDefault(p, p));
            }
        }
        return formatted;
    }
}
