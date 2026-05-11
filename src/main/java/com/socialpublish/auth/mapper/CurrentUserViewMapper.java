package com.socialpublish.auth.mapper;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CurrentUserViewMapper {

    CurrentUserView toView(User user);

    @Mapping(target = "id", expression = "java(null)")
    @Mapping(target = "provider", expression = "java(AuthProvider.GOOGLE)")
    @Mapping(target = "role", expression = "java(Role.USER)")
    @Mapping(target = "passwordLoginEnabled", constant = "false")
    CurrentUserView toOAuth2View(String email, String fullName);
}
