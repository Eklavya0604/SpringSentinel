package com.Vaish.SpringSentinel.config;

import com.Vaish.SpringSentinel.model.Role;
import com.Vaish.SpringSentinel.model.User;
import com.Vaish.SpringSentinel.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedAdmin() {
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword(
                        passwordEncoder.encode("admin123")
                );
                admin.setRole(Role.ADMIN);
                admin.setPremium(true);
                userRepository.save(admin);
                log.info(
                        "✅ Admin seeded — username: admin, " +
                                "password: admin123"
                );
            }
        };
    }
}