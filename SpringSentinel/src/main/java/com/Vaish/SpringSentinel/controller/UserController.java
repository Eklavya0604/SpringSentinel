package com.Vaish.SpringSentinel.controller;

import com.Vaish.SpringSentinel.model.User;
import com.Vaish.SpringSentinel.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "4. Users", description = "User management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserRepository userRepository;

    @Operation(
            summary = "Create a user",
            description = "Creates a new human user (different from auth register — no JWT returned)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "username": "humanuser",
                      "password": "pass123",
                      "isPremium": true
                    }
                """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "User created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation failed"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required")
            }
    )
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        return ResponseEntity.ok(userRepository.save(user));
    }

    @Operation(
            summary = "Get user by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User found"),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<User> getUser(
            @Parameter(description = "User ID", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(userRepository.findById(id).orElseThrow());
    }
}