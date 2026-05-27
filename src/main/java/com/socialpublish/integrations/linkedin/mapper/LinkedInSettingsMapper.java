package com.socialpublish.integrations.linkedin.mapper;

import com.socialpublish.integrations.linkedin.dto.LinkedInSettingsView;
import com.socialpublish.integrations.linkedin.entity.LinkedInSettingsEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface LinkedInSettingsMapper {

    @Mapping(target = "configured", constant = "true")
    LinkedInSettingsView toSettingsView(LinkedInSettingsEntity entity);
}
