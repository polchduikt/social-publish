package com.socialpublish.auth.service;

import com.socialpublish.auth.dto.RegisterRequest;
import com.socialpublish.auth.entity.AuthProvider;
import com.socialpublish.auth.entity.Role;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.exception.UserAlreadyExistsException;
import com.socialpublish.auth.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)
                || userRepository.existsByGoogleEmailIgnoreCase(normalizedEmail)) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName().trim());
        user.setProvider(AuthProvider.LOCAL);
        user.setRole(Role.USER);
        user.setPasswordLoginEnabled(true);
        userRepository.save(user);
    }
}
