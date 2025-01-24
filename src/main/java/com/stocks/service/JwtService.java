package com.stocks.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.stocks.exceptions.UnauthorizedException;


@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String secret;

    public Long extractUserId(String token) {
        return Long.parseLong(Jwts.parserBuilder()
            .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
            .build()
            .parseClaimsJws(token)
            .getBody()
            .getSubject());
    }

    public void validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
                .build()
                .parseClaimsJws(token);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid token");
        }
    }
}