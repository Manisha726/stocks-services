package com.stocks.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

import java.util.Date;

@Service
public class UserService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // Method to encode password before saving to DB
    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    // Method to check if the raw password matches the encoded password
    public boolean matchPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // Method to generate JWT token with user id
    public String generateToken(Long userId) {
        return Jwts.builder()
            .setSubject(String.valueOf(userId))
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 86400000))
            .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes()))
            .compact();
    }
}