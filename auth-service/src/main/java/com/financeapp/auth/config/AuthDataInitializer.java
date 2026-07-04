package com.financeapp.auth.config;

import com.financeapp.auth.entity.AuthUser;
import com.financeapp.auth.repository.AuthUserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthDataInitializer implements CommandLineRunner {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.auth.seed-admin-enabled:true}")
    private boolean seedAdminEnabled;

    @Value("${app.auth.seed-admin-email:admin@finance.com}")
    private String seedAdminEmail;

    @Value("${app.auth.seed-admin-password:password123}")
    private String seedAdminPassword;

    @Value("${app.auth.seed-admin-roles:ADMIN,USER}")
    private String seedAdminRoles;

    @Override
    public void run(String... args) {
        if (!seedAdminEnabled) {
            return;
        }

        String normalizedEmail = seedAdminEmail.trim().toLowerCase(Locale.ROOT);
        if (authUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        AuthUser admin = new AuthUser();
        admin.setEmail(normalizedEmail);
        admin.setPasswordHash(passwordEncoder.encode(seedAdminPassword));
        admin.setRoles(seedAdminRoles.trim().toUpperCase(Locale.ROOT));
        admin.setActive(true);
        authUserRepository.save(admin);
    }
}
