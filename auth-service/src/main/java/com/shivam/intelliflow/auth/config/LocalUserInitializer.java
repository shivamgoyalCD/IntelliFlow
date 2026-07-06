package com.shivam.intelliflow.auth.config;

import com.shivam.intelliflow.auth.entity.AuthUser;
import com.shivam.intelliflow.auth.repository.AuthUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class LocalUserInitializer {
    @Bean
    ApplicationRunner seedLocalUser(
            AuthUserRepository authUserRepository,
            PasswordEncoder passwordEncoder,
            @Value("${intelliflow.auth.local-user.username:admin}") String username,
            @Value("${intelliflow.auth.local-user.password:admin123}") String password
    ) {
        return args -> authUserRepository.findByUsername(username)
                .orElseGet(() -> authUserRepository.save(new AuthUser(
                        username,
                        passwordEncoder.encode(password),
                        "ROLE_ADMIN",
                        true
                )));
    }
}
