package com.socialpublish.posts.mapper;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.time.Instant;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {PostStatus.class})
public interface RecurringPostMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "owner", source = "source.owner")
    @Mapping(target = "title", source = "source.title")
    @Mapping(target = "content", source = "source.content")
    @Mapping(target = "platforms", source = "source.platforms")
    @Mapping(target = "status", expression = "java(PostStatus.SCHEDULED)")
    @Mapping(target = "scheduledAt", source = "nextAt")
    @Mapping(target = "recurring", constant = "true")
    @Mapping(target = "recurringDays", source = "source.recurringDays")
    @Mapping(target = "recurringTime", source = "source.recurringTime")
    @Mapping(target = "recurringEndDate", source = "source.recurringEndDate")
    @Mapping(target = "maxRetries", source = "source.maxRetries")
    @Mapping(target = "parentRecurringId", source = "parentId")
    @Mapping(target = "media", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "failedReason", ignore = true)
    @Mapping(target = "retryCount", constant = "0")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "silentMode", source = "source.silentMode")
    @Mapping(target = "inlineButtons", source = "source.inlineButtons")
    @Mapping(target = "pollQuestion", source = "source.pollQuestion")
    @Mapping(target = "pollOptions", source = "source.pollOptions")
    @Mapping(target = "pollMultipleAnswers", source = "source.pollMultipleAnswers")
    @Mapping(target = "pollIsQuiz", source = "source.pollIsQuiz")
    @Mapping(target = "pollCorrectOptionId", source = "source.pollCorrectOptionId")
    Post toNextOccurrence(Post source, Instant nextAt, UUID parentId);
}
