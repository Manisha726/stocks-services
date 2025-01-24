package com.stocks.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Disables CSRF protection (if you don't need it)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll()  // Allow all requests without authentication
            )
            .httpBasic().disable() // Disable HTTP basic authentication
            .formLogin().disable() // Disable form login
            .logout().disable(); // Disable logout functionality

        return http.build();
    }
}
