package com.Vaish.SpringSentinel.controller;

import com.Vaish.SpringSentinel.model.Role;
import com.Vaish.SpringSentinel.model.User;
import com.Vaish.SpringSentinel.repository.UserRepository;
import com.Vaish.SpringSentinel.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "1. Authentication",
        description = "Register and Login APIs")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    // ── Register ─────────────────────────────────────────────────
    @Operation(
            summary = "Register User",
            description = """
            Creates a new account.
            premium = true  → ADMIN role
            premium = false → USER role
            """,
            requestBody =
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "username": "testuser",
                      "password": "test123",
                      "isPremium": false
                    }
                """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Registration successful"),
                    @ApiResponse(responseCode = "409",
                            description = "Username already exists")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(
            @RequestBody User user) {

        if (userRepository.findByUsername(
                user.getUsername()).isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        user.setPassword(
                passwordEncoder.encode(user.getPassword())
        );

        user.setRole(user.isPremium()
                ? Role.ADMIN : Role.USER
        );

        userRepository.save(user);

        String token = jwtService.generateToken(
                user.getUsername()
        );

        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "token", token,
                "role", user.getRole().name()
        ));
    }

    // ── Login ─────────────────────────────────────────────────────
    @Operation(
            summary = "Login User",
            description = "Login using username and password",
            requestBody =
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "username": "admin",
                      "password": "admin123"
                    }
                """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Login successful"),
                    @ApiResponse(responseCode = "401",
                            description = "Invalid credentials")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(
            @RequestBody User user) {

        User found = userRepository
                .findByUsername(user.getUsername())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "User not found"
                ));

        if (!passwordEncoder.matches(
                user.getPassword(), found.getPassword())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Invalid password"
            );
        }

        String token = jwtService.generateToken(
                found.getUsername()
        );

        return ResponseEntity.ok(Map.of(
                "message", "Login successful",
                "token", token,
                "role", found.getRole().name()
        ));
    }
}