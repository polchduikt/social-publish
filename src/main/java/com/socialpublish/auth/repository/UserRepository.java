package com.socialpublish.auth.repository;

import com.socialpublish.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByGoogleEmailIgnoreCase(String googleEmail);

    boolean existsByGoogleEmailIgnoreCase(String googleEmail);
}
