package com.socialpublish.integrations.discord.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DiscordSettingsListRequest {
    @Valid
    private List<DiscordSettingsRequest> accounts = new ArrayList<>();
}
