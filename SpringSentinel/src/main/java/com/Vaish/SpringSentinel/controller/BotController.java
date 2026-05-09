package com.Vaish.SpringSentinel.controller;

import com.Vaish.SpringSentinel.model.Bot;
import com.Vaish.SpringSentinel.repository.BotRespository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
@Tag(name = "2. Bots",
        description = "Bot management — ADMIN only")
@SecurityRequirement(name = "Bearer Authentication")
public class BotController {

    private final BotRespository botRepository;

    // ── Create bot ───────────────────────────────────────────────
    @Operation(
            summary = "Create a bot (ADMIN only)",
            requestBody =
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "name": "Bot Alpha",
                      "personaDescription": "Helpful AI bot"
                    }
                """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Bot created"),
                    @ApiResponse(responseCode = "403",
                            description = "Forbidden - ADMIN only"),
                    @ApiResponse(responseCode = "401",
                            description = "Unauthorized")
            }
    )
    @PostMapping
    public ResponseEntity<Bot> createBot(
            @Valid @RequestBody Bot bot) {
        return ResponseEntity.ok(botRepository.save(bot));
    }

    // ── Get all bots ─────────────────────────────────────────────
    @Operation(
            summary = "Get all bots (ADMIN only)",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "List of bots"),
                    @ApiResponse(responseCode = "403",
                            description = "Forbidden - ADMIN only"),
                    @ApiResponse(responseCode = "401",
                            description = "Unauthorized")
            }
    )
    @GetMapping
    public ResponseEntity<List<Bot>> getAllBots() {
        return ResponseEntity.ok(botRepository.findAll());
    }

    // ── Get bot by ID ────────────────────────────────────────────
    @Operation(
            summary = "Get bot by ID (ADMIN only)",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Bot found"),
                    @ApiResponse(responseCode = "404",
                            description = "Bot not found"),
                    @ApiResponse(responseCode = "401",
                            description = "Unauthorized")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<Bot> getBot(
            @Parameter(description = "Bot ID", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(
                botRepository.findById(id)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Bot not found"))
        );
    }
}