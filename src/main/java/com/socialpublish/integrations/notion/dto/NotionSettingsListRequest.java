package com.socialpublish.integrations.notion.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class NotionSettingsListRequest {
    @Valid
    private List<NotionSettingsRequest> accounts = new ArrayList<>();
}
