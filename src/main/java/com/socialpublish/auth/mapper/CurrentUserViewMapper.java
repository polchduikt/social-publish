package com.socialpublish.auth.mapper;

import com.socialpublish.auth.dto.AuthProviderType;
import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.dto.UserRoleType;
import com.socialpublish.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CurrentUserViewMapper {

    @Mapping(target = "provider", expression = "java(toProviderType(user.getProvider()))")
    @Mapping(target = "role", expression = "java(toRoleType(user.getRole()))")
    @Mapping(target = "googleLinked", expression = "java(user.isGoogleLinked())")
    CurrentUserView toView(User user);

    @Mapping(target = "id", expression = "java(null)")
    @Mapping(target = "googleEmail", source = "email")
    @Mapping(target = "provider", expression = "java(AuthProviderType.GOOGLE)")
    @Mapping(target = "role", expression = "java(UserRoleType.USER)")
    @Mapping(target = "passwordLoginEnabled", constant = "false")
    @Mapping(target = "googleLinked", constant = "true")
    @Mapping(target = "emailNotificationsEnabled", constant = "true")
    @Mapping(target = "aiAssistantEnabled", constant = "true")
    CurrentUserView toOAuth2View(String email, String fullName);

    default AuthProviderType toProviderType(com.socialpublish.auth.entity.AuthProvider provider) {
        if (provider == null) {
            return null;
        }
        return AuthProviderType.valueOf(provider.name());
    }

    default UserRoleType toRoleType(com.socialpublish.auth.entity.Role role) {
        if (role == null) {
            return null;
        }
        return UserRoleType.valueOf(role.name());
    }
}
