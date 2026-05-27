package com.socialpublish.integrations.linkedin.mapper;

import com.socialpublish.integrations.linkedin.dto.LinkedInSettingsView;
import com.socialpublish.integrations.linkedin.entity.LinkedInSettingsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface LinkedInSettingsMapper {

    @Mapping(target = "configured", constant = "true")
    @Mapping(target = "maskedAccessToken", source = "accessToken", qualifiedByName = "maskSecret")
    LinkedInSettingsView toSettingsView(LinkedInSettingsEntity entity);

    @Named("maskSecret")
    default String maskSecret(String rawSecret) {
        if (rawSecret == null || rawSecret.isBlank()) {
            return "";
        }
        String normalized = rawSecret.trim();
        if (normalized.contains("...")) {
            return normalized;
        }
        if (normalized.length() <= 8) {
            return "...";
        }
        return normalized.substring(0, 4) + "..." + normalized.substring(normalized.length() - 4);
    }
}
