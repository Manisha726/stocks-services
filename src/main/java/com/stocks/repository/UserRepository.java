package com.stocks.repository;

import com.stocks.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Find user by username and return Optional<User> to avoid null issues
    Optional<User> findByUsername(String username);

    // Check if a username already exists
    boolean existsByUsername(String username);
}
