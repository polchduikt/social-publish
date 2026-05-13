package com.socialpublish.integrations.telegram.dto;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TelegramSettingsListRequest {
    @Valid
    private List<TelegramSettingsRequest> accounts = new ArrayList<>();
}
