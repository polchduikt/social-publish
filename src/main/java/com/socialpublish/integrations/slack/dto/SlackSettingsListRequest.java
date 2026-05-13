package com.socialpublish.integrations.slack.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SlackSettingsListRequest {
    @Valid
    private List<SlackSettingsRequest> accounts = new ArrayList<>();
}
