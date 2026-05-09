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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bots")
@RequiredArgsConstructor
@Tag(name = "2. Bots", description = "Bot management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class BotController {

    private final BotRespository botRepository;

    @Operation(
            summary = "Create a bot",
            description = "Creates a new AI bot with a persona",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                    {
                      "name": "Bot Alpha",
                      "personaDescription": "I am a helpful AI bot"
                    }
                """)
                    )
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bot created successfully"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required")
            }
    )
    @PostMapping
    public ResponseEntity<Bot> createBot(@RequestBody Bot bot) {
        return ResponseEntity.ok(botRepository.save(bot));
    }

    @Operation(
            summary = "Get bot by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bot found"),
                    @ApiResponse(responseCode = "404", description = "Bot not found"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized - JWT required")
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<Bot> getBot(
            @Parameter(description = "Bot ID", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(botRepository.findById(id).orElseThrow());
    }
}