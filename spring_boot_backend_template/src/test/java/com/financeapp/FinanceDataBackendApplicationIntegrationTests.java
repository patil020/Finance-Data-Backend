package com.financeapp;

import com.financeapp.dao.FinancialRecordDao;
import com.financeapp.dao.UserDao;
import com.financeapp.dto.request.*;
import com.financeapp.dto.response.*;
import com.financeapp.entities.FinancialRecord;
import com.financeapp.entities.User;
import com.financeapp.enums.RecordType;
import com.financeapp.enums.Role;
import com.financeapp.enums.UserStatus;
import com.financeapp.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Finance Data Backend API
 * Tests cover: Authentication, User Management, Financial Records, and Dashboard
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Finance Data Backend - Integration Tests")
public class FinanceDataBackendApplicationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserDao userDao;

    @Autowired
    private FinancialRecordDao financialRecordDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String analystToken;
    private String viewerToken;
    private User adminUser;
    private User analystUser;
    private User viewerUser;

    @BeforeEach
    public void setUp() {
        // Create test users with different roles
        adminUser = createTestUser("admin@test.com", "Admin@123", Role.ADMIN, UserStatus.ACTIVE);
        analystUser = createTestUser("analyst@test.com", "Analyst@123", Role.ANALYST, UserStatus.ACTIVE);
        viewerUser = createTestUser("viewer@test.com", "Viewer@123", Role.VIEWER, UserStatus.ACTIVE);

        // Generate JWT tokens
        adminToken = generateToken(adminUser);
        analystToken = generateToken(analystUser);
        viewerToken = generateToken(viewerUser);
    }

    // ==================== AUTH TESTS ====================

    @Test
    @DisplayName("Should register new user successfully")
    public void testRegisterNewUser() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFullName("New User");
        request.setEmail("newuser@test.com");
        request.setPassword("NewPass@123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("newuser@test.com"))
                .andExpect(jsonPath("$.user.role").value("VIEWER"));
    }

    @Test
    @DisplayName("Should not register user with duplicate email")
    public void testRegisterDuplicateEmail() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFullName("Duplicate User");
        request.setEmail("admin@test.com");
        request.setPassword("DupPass@123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already registered."));
    }

    @Test
    @DisplayName("Should reject password with weak complexity")
    public void testRegisterWeakPassword() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setFullName("New User");
        request.setEmail("weakpass@test.com");
        request.setPassword("weakpass123"); // Missing uppercase and special char

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    public void testLoginSuccess() throws Exception {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("admin@test.com");
        request.setPassword("Admin@123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("admin@test.com"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Should fail login with invalid credentials")
    public void testLoginInvalidCredentials() throws Exception {
        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("admin@test.com");
        request.setPassword("WrongPassword@123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials."));
    }

    @Test
    @DisplayName("Should not allow inactive user login")
    public void testLoginInactiveUser() throws Exception {
        // Create inactive user
        User inactiveUser = createTestUser("inactive@test.com", "Inactive@123", Role.VIEWER, UserStatus.INACTIVE);

        LoginRequestDto request = new LoginRequestDto();
        request.setEmail("inactive@test.com");
        request.setPassword("Inactive@123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Inactive user cannot log in."));
    }

    // ==================== USER MANAGEMENT TESTS ====================

    @Test
    @DisplayName("Admin should create new user")
    public void testAdminCreateUser() throws Exception {
        UserRequestDto request = new UserRequestDto();
        request.setFullName("Test User");
        request.setEmail("testuser@example.com");
        request.setPassword("TestPass@123");
        request.setRole(Role.ANALYST);
        request.setStatus(UserStatus.ACTIVE);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("testuser@example.com"))
                .andExpect(jsonPath("$.role").value("ANALYST"));
    }

    @Test
    @DisplayName("Non-admin should not create user")
    public void testAnalystCannotCreateUser() throws Exception {
        UserRequestDto request = new UserRequestDto();
        request.setFullName("Test User");
        request.setEmail("testuser@example.com");
        request.setPassword("TestPass@123");
        request.setRole(Role.ANALYST);
        request.setStatus(UserStatus.ACTIVE);

        mockMvc.perform(post("/api/users")
                .header("Authorization", "Bearer " + analystToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin should view all users with pagination")
    public void testAdminGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Admin should update user role")
    public void testAdminChangeUserRole() throws Exception {
        RoleUpdateDto request = new RoleUpdateDto();
        request.setRole(Role.ADMIN);

        mockMvc.perform(patch("/api/users/" + analystUser.getId() + "/role")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("Admin should update user status")
    public void testAdminChangeUserStatus() throws Exception {
        StatusUpdateDto request = new StatusUpdateDto();
        request.setStatus(UserStatus.INACTIVE);

        mockMvc.perform(patch("/api/users/" + viewerUser.getId() + "/status")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    @DisplayName("Unauthorized request should return 401")
    public void testUnauthorizedRequest() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    // ==================== FINANCIAL RECORDS TESTS ====================

    @Test
    @DisplayName("Admin should create financial record")
    public void testAdminCreateRecord() throws Exception {
        FinancialRecordRequestDto request = new FinancialRecordRequestDto();
        request.setAmount(BigDecimal.valueOf(5000.00));
        request.setType(RecordType.INCOME);
        request.setCategory("Salary");
        request.setRecordDate(LocalDate.now());
        request.setNote("Monthly salary");

        mockMvc.perform(post("/api/records")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(5000.00))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.category").value("Salary"));
    }

    @Test
    @DisplayName("Viewer should not create financial record")
    public void testViewerCannotCreateRecord() throws Exception {
        FinancialRecordRequestDto request = new FinancialRecordRequestDto();
        request.setAmount(BigDecimal.valueOf(5000.00));
        request.setType(RecordType.INCOME);
        request.setCategory("Salary");
        request.setRecordDate(LocalDate.now());
        request.setNote("Monthly salary");

        mockMvc.perform(post("/api/records")
                .header("Authorization", "Bearer " + viewerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("All roles should view financial records")
    public void testAllRolesCanViewRecords() throws Exception {
        // Create a record first
        createTestRecord(adminUser, BigDecimal.valueOf(5000), RecordType.INCOME, "Salary");

        // Test Admin
        mockMvc.perform(get("/api/records")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Test Analyst
        mockMvc.perform(get("/api/records")
                .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());

        // Test Viewer
        mockMvc.perform(get("/api/records")
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Should filter records by type, category, and date")
    public void testFilterRecords() throws Exception {
        // Create multiple records
        createTestRecord(adminUser, BigDecimal.valueOf(5000), RecordType.INCOME, "Salary");
        createTestRecord(adminUser, BigDecimal.valueOf(2000), RecordType.EXPENSE, "Rent");
        createTestRecord(adminUser, BigDecimal.valueOf(1000), RecordType.EXPENSE, "Food");

        mockMvc.perform(get("/api/records/filter")
                .header("Authorization", "Bearer " + analystToken)
                .param("type", "EXPENSE")
                .param("category", "Rent")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].category").value("Rent"));
    }

    @Test
    @DisplayName("Should return error for invalid date range")
    public void testFilterRecordsInvalidDateRange() throws Exception {
        LocalDate startDate = LocalDate.of(2024, 12, 31);
        LocalDate endDate = LocalDate.of(2024, 1, 1);

        mockMvc.perform(get("/api/records/filter")
                .header("Authorization", "Bearer " + analystToken)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Start date cannot be after end date")));
    }

    @Test
    @DisplayName("Admin should update financial record")
    public void testAdminUpdateRecord() throws Exception {
        FinancialRecord record = createTestRecord(adminUser, BigDecimal.valueOf(5000), RecordType.INCOME, "Salary");

        FinancialRecordRequestDto request = new FinancialRecordRequestDto();
        request.setAmount(BigDecimal.valueOf(6000.00));
        request.setType(RecordType.INCOME);
        request.setCategory("Bonus");
        request.setRecordDate(LocalDate.now());
        request.setNote("Updated note");

        mockMvc.perform(put("/api/records/" + record.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(6000.00))
                .andExpect(jsonPath("$.category").value("Bonus"));
    }

    @Test
    @DisplayName("Admin should delete financial record (soft delete)")
    public void testAdminDeleteRecord() throws Exception {
        FinancialRecord record = createTestRecord(adminUser, BigDecimal.valueOf(5000), RecordType.INCOME, "Salary");

        mockMvc.perform(delete("/api/records/" + record.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Verify record is soft-deleted (cannot be retrieved)
        mockMvc.perform(get("/api/records/" + record.getId())
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Pagination should work with edge cases")
    public void testPaginationEdgeCases() throws Exception {
        // Create 15 records
        for (int i = 0; i < 15; i++) {
            createTestRecord(adminUser, BigDecimal.valueOf(1000 * (i + 1)), RecordType.INCOME, "Category" + i);
        }

        // Test page 0 with size 10
        mockMvc.perform(get("/api/records?page=0&size=10")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(15)))
                .andExpect(jsonPath("$.content", hasSize(10)))
                .andExpect(jsonPath("$.totalPages").value(2));

        // Test page 1 with size 10
        mockMvc.perform(get("/api/records?page=1&size=10")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThan(0))));
    }

    // ==================== DASHBOARD TESTS ====================

    @Test
    @DisplayName("All roles should view dashboard summary")
    public void testAllRolesCanViewDashboard() throws Exception {
        // Create test records
        createTestRecord(adminUser, BigDecimal.valueOf(10000), RecordType.INCOME, "Salary");
        createTestRecord(adminUser, BigDecimal.valueOf(5000), RecordType.EXPENSE, "Rent");

        // Test Admin
        mockMvc.perform(get("/api/dashboard/summary")
                .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome").exists())
                .andExpect(jsonPath("$.totalExpense").exists())
                .andExpect(jsonPath("$.netBalance").exists());

        // Test Analyst
        mockMvc.perform(get("/api/dashboard/summary")
                .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk());

        // Test Viewer
        mockMvc.perform(get("/api/dashboard/summary")
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Dashboard should return category-wise totals")
    public void testDashboardCategoryWise() throws Exception {
        createTestRecord(adminUser, BigDecimal.valueOf(5000), RecordType.INCOME, "Salary");
        createTestRecord(adminUser, BigDecimal.valueOf(3000), RecordType.INCOME, "Freelance");
        createTestRecord(adminUser, BigDecimal.valueOf(2000), RecordType.EXPENSE, "Rent");

        mockMvc.perform(get("/api/dashboard/category-wise")
                .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].category").exists())
                .andExpect(jsonPath("$[*].total").exists());
    }

    @Test
    @DisplayName("Dashboard should return recent activity")
    public void testDashboardRecentActivity() throws Exception {
        createTestRecord(adminUser, BigDecimal.valueOf(5000), RecordType.INCOME, "Salary");
        createTestRecord(adminUser, BigDecimal.valueOf(2000), RecordType.EXPENSE, "Rent");

        mockMvc.perform(get("/api/dashboard/recent-activity?limit=5")
                .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Dashboard should return monthly trends")
    public void testDashboardMonthlyTrends() throws Exception {
        createTestRecord(adminUser, BigDecimal.valueOf(10000), RecordType.INCOME, "Salary");
        createTestRecord(adminUser, BigDecimal.valueOf(5000), RecordType.EXPENSE, "Rent");

        mockMvc.perform(get("/api/dashboard/monthly-trends")
                .header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].year").exists())
                .andExpect(jsonPath("$[*].month").exists())
                .andExpect(jsonPath("$[*].totalIncome").exists())
                .andExpect(jsonPath("$[*].totalExpense").exists());
    }

    // ==================== HELPER METHODS ====================

    private User createTestUser(String email, String password, Role role, UserStatus status) {
        User user = User.builder()
                .fullName("Test " + role.name())
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(role)
                .status(status)
                .build();
        return userDao.save(user);
    }

    private FinancialRecord createTestRecord(User user, BigDecimal amount, RecordType type, String category) {
        FinancialRecord record = FinancialRecord.builder()
                .amount(amount)
                .type(type)
                .category(category)
                .recordDate(LocalDate.now())
                .note("Test record")
                .createdBy(user)
                .deleted(false)
                .build();
        return financialRecordDao.save(record);
    }

    private String generateToken(User user) {
        org.springframework.security.core.userdetails.UserDetails userDetails =
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole().name())
                        .build();
        return jwtUtil.generateToken(userDetails);
    }
}
