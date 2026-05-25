package com.socialpublish.auth.service;

import com.socialpublish.AbstractIntegrationTest;
import com.socialpublish.auth.dto.RegisterRequest;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.exception.UserAlreadyExistsException;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.posts.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRegisterNewUserSuccessfully() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("integration@test.com");
        request.setPassword("strongpassword123");
        request.setFullName("Integration Test User");

        authService.register(request);

        Optional<User> foundUser = userRepository.findByEmailIgnoreCase("integration@test.com");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getFullName()).isEqualTo("Integration Test User");
        assertThat(passwordEncoder.matches("strongpassword123", foundUser.get().getPassword())).isTrue();
    }

    @Test
    void shouldThrowExceptionWhenUserAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("duplicate@test.com");
        request.setPassword("password");
        request.setFullName("User One");

        authService.register(request);

        RegisterRequest secondRequest = new RegisterRequest();
        secondRequest.setEmail("duplicate@test.com");
        secondRequest.setPassword("newpassword");
        secondRequest.setFullName("User Two");

        assertThatThrownBy(() -> authService.register(secondRequest))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("User with this email already exists");
    }
}
