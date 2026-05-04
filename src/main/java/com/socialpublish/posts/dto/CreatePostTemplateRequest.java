package com.socialpublish.posts.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostTemplateRequest(
        @NotBlank @Size(max = 150) String templateName,
        @NotBlank @Size(max = 5000) String content,
        @Size(max = 200) String platforms
) {}
