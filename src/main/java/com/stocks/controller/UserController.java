package com.stocks.controller;

import com.stocks.entity.User;
import com.stocks.repository.UserRepository;
import com.stocks.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Object> registerUser(@RequestBody User user) {
        // Check if the username already exists
        if (userRepository.existsByUsername(user.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"msg\":\"Username already exists\"}");
        }

        // Encrypt the password before saving
        user.setPassword(userService.encodePassword(user.getPassword()));
        userRepository.save(user);

        // Return success message
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("{\"msg\":\"" + " User " + user.getUsername() + " registered successfully\"}");
    }

    @PostMapping("/login")
    public ResponseEntity<Object> loginUser(@RequestBody User user) {
        try {
            // Find the user by username
            Optional<User> existingUserOptional = userRepository.findByUsername(user.getUsername());

            // Check if the user exists and the password matches
            if (existingUserOptional.isEmpty() || !userService.matchPassword(user.getPassword(), existingUserOptional.get().getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"msg\":\"Invalid credentials\"}");
            }

            // Get the user from Optional
            User existingUser = existingUserOptional.get();

            // Generate JWT token with user id
            String token = userService.generateToken(existingUser.getId());

            return ResponseEntity.ok("{\"access_token\":\"" + token + "\"}");

        } catch (Exception e) {
            // Log the error and return internal server error response
            e.printStackTrace(); // For debugging purposes, log the exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"msg\":\"An error occurred while processing the login request\"}");
        }
    }
}
