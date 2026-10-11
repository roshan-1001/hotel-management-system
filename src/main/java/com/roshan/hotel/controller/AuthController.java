package com.roshan.hotel.controller;

import com.roshan.hotel.service.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody LoginRequest request) {
        Authentication authentication =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken.unauthenticated(
                                request.username(),
                                request.password()
                        )
                );

        return new TokenResponse(
                jwtService.generateToken(authentication),
                "Bearer",
                3600
        );
    }

    public record LoginRequest(String username, String password) {}

    public record TokenResponse(
            String accessToken,
            String tokenType,
            long expiresIn
    ) {}
}