package com.socialpublish.auth.service;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.mapper.CurrentUserViewMapper;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.auth.security.AppUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.any;
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
        
        testUserView = new CurrentUserView(testUser.getId(), testUser.getEmail(), testUser.getFullName(), null, null, false, false, false);
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
    void resolveCurrentUser_WithOAuth2UserFoundInDb_ReturnsUser() {
        OAuth2User oauth2User = new DefaultOAuth2User(Set.of(), Map.of("email", "test@example.com", "name", "Test User"), "name");
        Authentication auth = new OAuth2AuthenticationToken(oauth2User, Set.of(), "google");
        
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(currentUserViewMapper.toView(testUser)).thenReturn(testUserView);

        Optional<CurrentUserView> result = authenticatedUserService.resolveCurrentUser(auth);
        
        assertTrue(result.isPresent());
        assertEquals(testUserView, result.get());
        verify(userRepository, never()).save(any());
    }

    @Test
    void resolveCurrentUser_WithOAuth2UserNotFoundInDb_CreatesAndReturnsUser() {
        OAuth2User oauth2User = new DefaultOAuth2User(Set.of(), Map.of("email", "new@example.com", "name", "New User"), "name");
        Authentication auth = new OAuth2AuthenticationToken(oauth2User, Set.of(), "google");
        
        User savedUser = new User();
        savedUser.setId(UUID.randomUUID());
        savedUser.setEmail("new@example.com");
        savedUser.setFullName("New User");
        
        CurrentUserView newView = new CurrentUserView(savedUser.getId(), savedUser.getEmail(), savedUser.getFullName(), null, null, false, false, false);
        
        when(userRepository.findByEmailIgnoreCase("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(currentUserViewMapper.toView(savedUser)).thenReturn(newView);

        Optional<CurrentUserView> result = authenticatedUserService.resolveCurrentUser(auth);
        
        assertTrue(result.isPresent());
        assertEquals(newView, result.get());
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        
        User capturedUser = userCaptor.getValue();
        assertEquals("new@example.com", capturedUser.getEmail());
        assertEquals("New User", capturedUser.getFullName());
        assertEquals(AuthProvider.GOOGLE, capturedUser.getProvider());
        assertEquals(Role.USER, capturedUser.getRole());
        assertFalse(capturedUser.isPasswordLoginEnabled());
    }
}
