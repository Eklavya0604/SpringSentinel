package com.Vaish.SpringSentinel.controller;

import com.Vaish.SpringSentinel.model.Role;
import com.Vaish.SpringSentinel.model.User;
import com.Vaish.SpringSentinel.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "4. Users", description = "User management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserRepository userRepository;

    // ── Get user by ID ───────────────────────────────────────────
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
        return ResponseEntity.ok(
                userRepository.findById(id).orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "User not found")));
    }

    // ── Promote user to admin ────────────────────────────────────
    @Operation(
            summary = "Promote user to admin",
            description = "Only an existing ADMIN can call this endpoint. Promotes any user to ADMIN role.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User promoted to ADMIN"),
                    @ApiResponse(responseCode = "403", description = "Forbidden - ADMIN role required"),
                    @ApiResponse(responseCode = "404", description = "User not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required")
            }
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/promote")
    public ResponseEntity<String> promoteToAdmin(
            @Parameter(description = "User ID to promote", example = "2")
            @PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        return ResponseEntity.ok("User " + user.getUsername() + " promoted to ADMIN");
    }
}