package com.socialpublish.posts.dto;

import com.socialpublish.common.validation.PlatformList;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostTemplateRequest(
        @NotBlank @Size(max = 150) String templateName,
        @NotBlank @Size(max = 5000) String content,
        @PlatformList
        List<String> platforms
) {}
