package com.socialpublish.posts.service;

import org.springframework.stereotype.Component;

@Component
public class PostTitleGenerator {

    private static final int MAX_TITLE_LENGTH = 150;
    private static final String FALLBACK_TITLE = "Untitled post";

    public String fromContent(String content) {
        if (content == null || content.isBlank()) {
            return FALLBACK_TITLE;
        }

        String firstLine = content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .findFirst()
                .orElse(FALLBACK_TITLE);

        String plain = firstLine
                .replaceAll("\\[(.*?)]\\((.*?)\\)", "$1")
                .replaceAll("[*_~`>|\\[\\]()]", "")
                .trim();

        String title = plain.isBlank() ? FALLBACK_TITLE : plain;
        return title.length() > MAX_TITLE_LENGTH ? title.substring(0, MAX_TITLE_LENGTH) : title;
    }
}
