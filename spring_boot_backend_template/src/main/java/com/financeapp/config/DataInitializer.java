package com.financeapp.config;

import com.financeapp.dao.FinancialRecordDao;
import com.financeapp.dao.UserDao;
import com.financeapp.entities.FinancialRecord;
import com.financeapp.entities.User;
import com.financeapp.enums.RecordType;
import com.financeapp.enums.Role;
import com.financeapp.enums.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer {

    @Bean
    public CommandLineRunner initializeData(
            UserDao userDao,
            FinancialRecordDao financialRecordDao,
            PasswordEncoder passwordEncoder) {
        return args -> {
            User admin = createUserIfMissing(
                    userDao, passwordEncoder, "Admin User", "admin@example.com", "Admin@123", Role.ADMIN, UserStatus.ACTIVE);
            User analyst = createUserIfMissing(
                    userDao, passwordEncoder, "Analyst User", "analyst@example.com", "Analyst@123", Role.ANALYST, UserStatus.ACTIVE);
            createUserIfMissing(
                    userDao, passwordEncoder, "Viewer User", "viewer@example.com", "Viewer@123", Role.VIEWER, UserStatus.ACTIVE);

            if (financialRecordDao.count() == 0) {
                financialRecordDao.save(FinancialRecord.builder()
                        .amount(BigDecimal.valueOf(120000))
                        .type(RecordType.INCOME)
                        .category("Salary")
                        .recordDate(LocalDate.now().minusDays(21))
                        .note("Monthly salary credited")
                        .createdBy(admin)
                        .deleted(false)
                        .build());

                financialRecordDao.save(FinancialRecord.builder()
                        .amount(BigDecimal.valueOf(20000))
                        .type(RecordType.EXPENSE)
                        .category("Rent")
                        .recordDate(LocalDate.now().minusDays(18))
                        .note("House rent paid")
                        .createdBy(admin)
                        .deleted(false)
                        .build());

                financialRecordDao.save(FinancialRecord.builder()
                        .amount(BigDecimal.valueOf(9000))
                        .type(RecordType.EXPENSE)
                        .category("Groceries")
                        .recordDate(LocalDate.now().minusDays(10))
                        .note("Monthly groceries")
                        .createdBy(analyst)
                        .deleted(false)
                        .build());

                financialRecordDao.save(FinancialRecord.builder()
                        .amount(BigDecimal.valueOf(45000))
                        .type(RecordType.INCOME)
                        .category("Freelance")
                        .recordDate(LocalDate.now().minusDays(7))
                        .note("Project consulting payment")
                        .createdBy(analyst)
                        .deleted(false)
                        .build());

                financialRecordDao.save(FinancialRecord.builder()
                        .amount(BigDecimal.valueOf(6000))
                        .type(RecordType.EXPENSE)
                        .category("Utilities")
                        .recordDate(LocalDate.now().minusDays(3))
                        .note("Electricity and internet bill")
                        .createdBy(admin)
                        .deleted(false)
                        .build());
            }
        };
    }

    private User createUserIfMissing(
            UserDao userDao,
            PasswordEncoder passwordEncoder,
            String fullName,
            String email,
            String rawPassword,
            Role role,
            UserStatus status) {
        String normalizedEmail = email.trim().toLowerCase();
        return userDao.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> userDao.save(User.builder()
                        .fullName(fullName.trim())
                        .email(normalizedEmail)
                        .password(passwordEncoder.encode(rawPassword))
                        .role(role)
                        .status(status)
                        .build()));
    }
}
