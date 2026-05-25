package com.socialpublish.auth.security;

import com.socialpublish.AbstractIntegrationTest;
import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.posts.repository.PostRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuth2UserSyncServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OAuth2UserSyncService oAuth2UserSyncService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldCreateNewUserOnFirstGoogleLogin() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "newgoogle@example.com");
        attributes.put("email_verified", true);
        attributes.put("sub", "google-sub-12345");
        attributes.put("name", "New Google User");

        oAuth2UserSyncService.syncUser(attributes);

        Optional<User> found = userRepository.findByEmailIgnoreCase("newgoogle@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("New Google User");
        assertThat(found.get().getGoogleEmail()).isEqualTo("newgoogle@example.com");
        assertThat(found.get().getGoogleSub()).isEqualTo("google-sub-12345");
        assertThat(found.get().getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(found.get().isPasswordLoginEnabled()).isFalse();
    }

    @Test
    void shouldLinkGoogleToExistingLocalUserByEmailMatch() {
        User localUser = new User();
        localUser.setEmail("common@example.com");
        localUser.setFullName("Local User");
        localUser.setProvider(AuthProvider.LOCAL);
        localUser.setRole(Role.USER);
        localUser.setPassword("password");
        localUser.setPasswordLoginEnabled(true);
        localUser = userRepository.save(localUser);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "common@example.com");
        attributes.put("email_verified", true);
        attributes.put("sub", "google-sub-common");
        attributes.put("name", "Google Match Name");

        oAuth2UserSyncService.syncUser(attributes);

        Optional<User> found = userRepository.findByEmailIgnoreCase("common@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(localUser.getId());
        assertThat(found.get().getGoogleEmail()).isEqualTo("common@example.com");
        assertThat(found.get().getGoogleSub()).isEqualTo("google-sub-common");
        assertThat(found.get().getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(found.get().isPasswordLoginEnabled()).isTrue();
    }

    @Test
    void shouldLinkGoogleExplicitlyWhenSessionFlagIsPresent() {
        User localUser = new User();
        localUser.setEmail("logged-in@example.com");
        localUser.setFullName("LoggedIn User");
        localUser.setProvider(AuthProvider.LOCAL);
        localUser.setRole(Role.USER);
        localUser.setPassword("password");
        localUser.setPasswordLoginEnabled(true);
        localUser = userRepository.save(localUser);

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.getSession(true).setAttribute(OAuth2UserSyncService.LINK_GOOGLE_USER_ID, localUser.getId());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "different-google@example.com");
        attributes.put("email_verified", true);
        attributes.put("sub", "different-sub-999");
        attributes.put("name", "Different Name");

        oAuth2UserSyncService.syncUser(attributes);

        Optional<User> found = userRepository.findById(localUser.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("logged-in@example.com");
        assertThat(found.get().getGoogleEmail()).isEqualTo("different-google@example.com");
        assertThat(found.get().getGoogleSub()).isEqualTo("different-sub-999");
        
        assertThat(mockRequest.getSession().getAttribute(OAuth2UserSyncService.LINK_GOOGLE_USER_ID)).isNull();
    }

    @Test
    void shouldThrowExceptionWhenLinkingGoogleAccountAlreadyLinkedToAnotherUser() {
        User userA = new User();
        userA.setEmail("usera@example.com");
        userA.setFullName("User A");
        userA.setProvider(AuthProvider.LOCAL);
        userA.setRole(Role.USER);
        userA.setGoogleEmail("shared-google@example.com");
        userA.setGoogleSub("shared-sub");
        userRepository.save(userA);

        User userB = new User();
        userB.setEmail("userb@example.com");
        userB.setFullName("User B");
        userB.setProvider(AuthProvider.LOCAL);
        userB.setRole(Role.USER);
        userB = userRepository.save(userB);

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.getSession(true).setAttribute(OAuth2UserSyncService.LINK_GOOGLE_USER_ID, userB.getId());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", "shared-google@example.com");
        attributes.put("email_verified", true);
        attributes.put("sub", "shared-sub");
        attributes.put("name", "Shared Name");

        assertThatThrownBy(() -> oAuth2UserSyncService.syncUser(attributes))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("already linked to another user");
    }
}
