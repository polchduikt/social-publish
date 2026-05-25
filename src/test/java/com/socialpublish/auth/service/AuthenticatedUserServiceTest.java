package com.socialpublish.auth.service;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.entity.Role;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.mapper.CurrentUserViewMapper;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.auth.security.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserViewMapper currentUserViewMapper;

    @InjectMocks
    private AuthenticatedUserService authenticatedUserService;

    private User testUser;
    private CurrentUserView testUserView;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setRole(Role.USER);
        
        testUserView = new CurrentUserView(
                testUser.getId(), testUser.getEmail(), null, testUser.getFullName(),
                null, null, false, false, false, false
        );
    }

    @Test
    void resolveCurrentUser_WithNullAuthentication_ReturnsEmpty() {
        Optional<CurrentUserView> result = authenticatedUserService.resolveCurrentUser(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveCurrentUser_WithUnauthenticatedToken_ReturnsEmpty() {
        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(false);

        Optional<CurrentUserView> result = authenticatedUserService.resolveCurrentUser(auth);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveCurrentUser_WithAppUserDetails_ReturnsUser() {
        AppUserDetails userDetails = new AppUserDetails(testUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        
        when(currentUserViewMapper.toView(testUser)).thenReturn(testUserView);

        Optional<CurrentUserView> result = authenticatedUserService.resolveCurrentUser(auth);
        
        assertTrue(result.isPresent());
        assertEquals(testUserView, result.get());
    }

    @Test
    void resolveCurrentUser_WithOAuth2User_FoundByGoogleSub_ReturnsUser() {
        testUser.setGoogleSub("google-sub-123");
        testUser.setGoogleEmail("test@example.com");

        OAuth2User oauth2User = new DefaultOAuth2User(
                Set.of(), Map.of("email", "test@example.com", "name", "Test User", "sub", "google-sub-123"), "name"
        );
        Authentication auth = new OAuth2AuthenticationToken(oauth2User, Set.of(), "google");
        
        when(userRepository.findByGoogleSub("google-sub-123")).thenReturn(Optional.of(testUser));
        when(currentUserViewMapper.toView(testUser)).thenReturn(testUserView);

        Optional<CurrentUserView> result = authenticatedUserService.resolveCurrentUser(auth);
        
        assertTrue(result.isPresent());
        assertEquals(testUserView, result.get());
    }

    @Test
    void resolveCurrentUser_WithOAuth2User_FoundByGoogleEmail_ReturnsUser() {
        testUser.setGoogleEmail("test@example.com");

        OAuth2User oauth2User = new DefaultOAuth2User(
                Set.of(), Map.of("email", "test@example.com", "name", "Test User"), "name"
        );
        Authentication auth = new OAuth2AuthenticationToken(oauth2User, Set.of(), "google");
        
        when(userRepository.findByGoogleEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(currentUserViewMapper.toView(testUser)).thenReturn(testUserView);

        Optional<CurrentUserView> result = authenticatedUserService.resolveCurrentUser(auth);
        
        assertTrue(result.isPresent());
        assertEquals(testUserView, result.get());
    }

    @Test
    void resolveCurrentUser_WithOAuth2User_FoundByPrimaryEmail_ReturnsUser() {
        OAuth2User oauth2User = new DefaultOAuth2User(
                Set.of(), Map.of("email", "test@example.com", "name", "Test User"), "name"
        );
        Authentication auth = new OAuth2AuthenticationToken(oauth2User, Set.of(), "google");
        
        when(userRepository.findByGoogleEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(currentUserViewMapper.toView(testUser)).thenReturn(testUserView);

        Optional<CurrentUserView> result = authenticatedUserService.resolveCurrentUser(auth);
        
        assertTrue(result.isPresent());
        assertEquals(testUserView, result.get());
    }

    @Test
    void resolveCurrentUser_WithOAuth2User_NotFoundInDb_ReturnsEmpty() {
        OAuth2User oauth2User = new DefaultOAuth2User(
                Set.of(), Map.of("email", "new@example.com", "name", "New User"), "name"
        );
        Authentication auth = new OAuth2AuthenticationToken(oauth2User, Set.of(), "google");
        
        when(userRepository.findByGoogleEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());

        Optional<CurrentUserView> result = authenticatedUserService.resolveCurrentUser(auth);
        
        assertTrue(result.isEmpty());
    }
}
