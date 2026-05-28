package com.socialpublish.integrations.notion.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotionClientService {

    private static final int MAX_NOTION_TITLE_LENGTH = 100;
    private static final int TRUNCATION_SUFFIX_LENGTH = 3;

    private final RestClient restClient;

    public void createDatabaseEntry(String token, String databaseId, String content, List<String> imageUrls) {
        Map<String, Object> titleProperty = Map.of(
            "title", List.of(Map.of("text", Map.of("content", truncate(content, MAX_NOTION_TITLE_LENGTH))))
        );

        List<Map<String, Object>> children = new ArrayList<>();
        children.add(Map.of(
            "object", "block",
            "type", "paragraph",
            "paragraph", Map.of("rich_text", List.of(Map.of("text", Map.of("content", content))))
        ));

        if (imageUrls != null) {
            for (String url : imageUrls) {
                children.add(Map.of(
                    "object", "block",
                    "type", "image",
                    "image", Map.of("type", "external", "external", Map.of("url", url))
                ));
            }
        }

        Map<String, Object> body = Map.of(
            "parent", Map.of("database_id", databaseId),
            "properties", Map.of("Name", titleProperty),
            "children", children
        );

        restClient.post()
            .uri("https://api.notion.com/v1/pages")
            .header("Authorization", "Bearer " + token)
            .header("Notion-Version", "2022-06-28")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();
            
        log.info("Page created in Notion database: {}", databaseId);
    }

    private String truncate(String s, int n) {
        if (s == null) return "Untitled Post";
        if (s.length() <= n) return s;
        return s.substring(0, n - TRUNCATION_SUFFIX_LENGTH) + "...";
    }
}
