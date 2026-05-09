package com.Vaish.SpringSentinel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SpringSentinel API")
                        .version("1.0")
                        .description("""
                    ## 🛡️ SpringSentinel
                    
                    High-performance API gateway with Redis guardrails.
                    
                    ### How to authenticate:
                    1. Call **/api/auth/register** or **/api/auth/login**
                    2. Copy the token from response
                    3. Click **Authorize 🔒** above
                    4. Paste token and click Authorize
                    
                    ### Redis Guardrails:
                    - **Horizontal Cap** — Max 100 bot replies per post
                    - **Vertical Cap** — Max depth level 20
                    - **Cooldown Cap** — 10 min between bot-human interactions
                    - **Rate Limit** — 100 requests/min per IP
                    """)
                        .contact(new Contact()
                                .name("SpringSentinel")
                                .email("kumareklavya744@email.com")
                                .url("https://github.com/Eklavya0604/SpringSentinel")))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Paste your JWT token here")));
    }
}