# Manual Test Scenarios for Finance Data Backend

## Table of Contents
1. [Authentication Tests](#authentication-tests)
2. [Role-Based Access Control Tests](#role-based-access-control-tests)
3. [User Management Tests](#user-management-tests)
4. [Financial Record Tests](#financial-record-tests)
5. [Dashboard Tests](#dashboard-tests)
6. [Edge Cases & Error Handling](#edge-cases--error-handling)
7. [Performance Tests](#performance-tests)

---

## Authentication Tests

### Test 1.1: Register with Valid Credentials
**Objective:** Verify successful user registration with strong password

**Steps:**
1. Call `POST /api/auth/register`
2. Provide:
   - Full Name: "John Doe"
   - Email: "john@example.com"
   - Password: "JohnDoe@123"

**Expected Result:**
- Status: 201 Created
- Response contains: token, tokenType "Bearer", user object with email and role "VIEWER"
- User can login with registered credentials

---

### Test 1.2: Register with Weak Password
**Objective:** Reject password without uppercase, digit, or special character

**Steps:**
1. Call `POST /api/auth/register`
2. Provide:
   - Full Name: "Jane Doe"
   - Email: "jane@example.com"
   - Password: "weakpassword123" (no uppercase, no special char)

**Expected Result:**
- Status: 400 Bad Request
- Error message mentions password complexity requirements

---

### Test 1.3: Register with Duplicate Email
**Objective:** Prevent duplicate email registrations

**Steps:**
1. Register user with email "duplicate@example.com"
2. Attempt to register again with same email

**Expected Result:**
- Status: 400 Bad Request
- Error message: "Email is already registered."

---

### Test 1.4: Successful Login
**Objective:** Verify JWT token generation on successful login

**Steps:**
1. Call `POST /api/auth/login`
2. Use credentials: admin@example.com / Admin@123

**Expected Result:**
- Status: 200 OK
- Response contains valid JWT token
- Token can be used for authenticated requests

---

### Test 1.5: Login with Invalid Credentials
**Objective:** Reject invalid email/password combination

**Steps:**
1. Call `POST /api/auth/login`
2. Use credentials: admin@example.com / WrongPassword123

**Expected Result:**
- Status: 401 Unauthorized
- Error message: "Invalid credentials."
- Remaining attempts count is shown

---

### Test 1.6: Rate Limiting - Lock After 5 Failed Attempts
**Objective:** Verify account lockout after multiple failed login attempts

**Steps:**
1. Attempt login with correct email but wrong password 5 times
2. On 6th attempt, use correct credentials

**Expected Result:**
- Attempts 1-5: Receive "Invalid credentials" with remaining attempts
- Attempt 5: Remaining attempts shown as 0
- Attempt 6: Status 400 with message "Account is locked due to too many failed login attempts"

---

### Test 1.7: Inactive User Cannot Login
**Objective:** Prevent inactive users from accessing the system

**Steps:**
1. Set user status to INACTIVE via admin
2. Attempt to login with that user's credentials

**Expected Result:**
- Status: 403 Forbidden
- Error message: "Inactive user cannot log in."

---

## Role-Based Access Control Tests

### Test 2.1: ADMIN Can Access All Resources
**Objective:** Verify admin has full access permissions

**Steps:**
1. Login as admin@example.com
2. Attempt:
   - POST /api/users (create user)
   - GET /api/records (view records)
   - POST /api/records (create record)
   - PUT /api/records/{id} (update record)
   - DELETE /api/records/{id} (delete record)
   - GET /api/dashboard/* (access dashboard)

**Expected Result:** All requests return 200/201 OK

---

### Test 2.2: ANALYST Can View But Not Create Records
**Objective:** Verify analyst has read-only access

**Steps:**
1. Login as analyst@example.com
2. Attempt:
   - GET /api/records → Should succeed (200)
   - POST /api/records → Should fail
   - PUT /api/records/{id} → Should fail
   - DELETE /api/records/{id} → Should fail
   - GET /api/dashboard/* → Should succeed (200)

**Expected Result:**
- Read operations: 200 OK
- Write operations: 403 Forbidden

---

### Test 2.3: VIEWER Can Only View Records & Dashboard
**Objective:** Verify viewer has most restricted access

**Steps:**
1. Login as viewer@example.com
2. Attempt:
   - GET /api/records → Should succeed
   - GET /api/dashboard/* → Should succeed
   - POST /api/records → Should fail
   - GET /api/users → Should fail (even list users)
   - PATCH /api/users/{id}/role → Should fail

**Expected Result:**
- Read-only operations on records/dashboard: 200 OK
- All other operations: 403 Forbidden

---

### Test 2.4: Non-Admin Cannot Create Users
**Objective:** Only ADMIN can create/manage users

**Steps:**
1. Login as analyst@example.com
2. Call `POST /api/users` with valid user data

**Expected Result:**
- Status: 403 Forbidden

---

### Test 2.5: Missing Token Rejection
**Objective:** Unauthenticated requests are rejected

**Steps:**
1. Call any protected endpoint without Authorization header
2. Call with invalid/expired token

**Expected Result:**
- Status: 401 Unauthorized
- Error message indicates authentication required

---

## User Management Tests

### Test 3.1: Admin Creates User with Role
**Objective:** Admin can create users with specific roles

**Steps:**
1. Login as admin
2. Call `POST /api/users`
3. Provide: Full Name, Email, Password (complex), Role (ANALYST), Status (ACTIVE)

**Expected Result:**
- Status: 201 Created
- User created with specified role
- Password is hashed (never returned in response)

---

### Test 3.2: Admin Updates User Status
**Objective:** Admin can toggle user status between ACTIVE/INACTIVE

**Steps:**
1. Login as admin
2. Call `PATCH /api/users/{userId}/status`
3. Set status to INACTIVE

**Expected Result:**
- Status: 200 OK
- User status updated
- That user can no longer login

---

### Test 3.3: Admin Changes User Role
**Objective:** Admin can promote/demote user roles

**Steps:**
1. Login as admin
2. Create analyst user
3. Call `PATCH /api/users/{userId}/role`
4. Change role to ADMIN

**Expected Result:**
- Status: 200 OK
- User now has ADMIN permissions
- User's permissions update immediately

---

### Test 3.4: Retrieve User by ID
**Objective:** Admin can fetch single user details

**Steps:**
1. Login as admin
2. Call `GET /api/users/{userId}`

**Expected Result:**
- Status: 200 OK
- Returns user data (email, role, status, timestamps)

---

### Test 3.5: List Users with Pagination
**Objective:** Admin can fetch paginated user list

**Steps:**
1. Login as admin
2. Call `GET /api/users?page=0&size=5&sortBy=email&sortDir=asc`

**Expected Result:**
- Status: 200 OK
- Response includes: content array, totalElements, totalPages, currentPage

---

## Financial Record Tests

### Test 4.1: Admin Creates Income Record
**Objective:** Admin can create financial records

**Steps:**
1. Login as admin
2. Call `POST /api/records`
3. Provide:
   - Amount: 5000.00
   - Type: INCOME
   - Category: "Salary"
   - Date: 2024-04-01
   - Note: "Monthly salary"

**Expected Result:**
- Status: 201 Created
- Record created with createdBy set to logged-in admin

---

### Test 4.2: Admin Creates Expense Record
**Objective:** Admin can create expense type records

**Steps:**
1. Login as admin
2. Create record with Type: EXPENSE, Category: "Rent", Amount: 2000.00

**Expected Result:**
- Status: 201 Created
- Record stored correctly with EXPENSE type

---

### Test 4.3: Invalid Amount Rejected
**Objective:** Reject zero or negative amounts

**Steps:**
1. Login as admin
2. Call `POST /api/records` with Amount: -100 or 0

**Expected Result:**
- Status: 400 Bad Request
- Error: "Amount must be greater than 0"

---

### Test 4.4: Get Record by ID
**Objective:** All roles can retrieve individual record

**Steps:**
1. Create record as admin
2. Login as analyst
3. Call `GET /api/records/{recordId}`

**Expected Result:**
- Status: 200 OK
- Returns full record details with creator info

---

### Test 4.5: Update Record (Admin Only)
**Objective:** Only admin can modify records

**Steps:**
1. Create record as admin
2. Login as analyst
3. Call `PUT /api/records/{recordId}` with updated data

**Expected Result:**
- Status: 403 Forbidden (analyst access denied)

---

### Test 4.6: Delete Record - Soft Delete
**Objective:** Records are soft-deleted (deleted flag set)

**Steps:**
1. Create record and note {recordId}
2. Login as admin
3. Call `DELETE /api/records/{recordId}`
4. Attempt `GET /api/records/{recordId}`

**Expected Result:**
- Delete: Status 204 No Content
- Retrieve: Status 404 Not Found (record marked as deleted)

---

### Test 4.7: Filter by Type
**Objective:** Filter records by INCOME/EXPENSE type

**Steps:**
1. Create mixed INCOME and EXPENSE records
2. Call `GET /api/records/filter?type=EXPENSE`

**Expected Result:**
- Status: 200 OK
- Returns only EXPENSE type records

---

### Test 4.8: Filter by Category (Partial Match)
**Objective:** Filter by category name with case-insensitive search

**Steps:**
1. Create records with categories: "Salary", "salary", "SALARY"
2. Call `GET /api/records/filter?category=sala`

**Expected Result:**
- Returns all records matching "sala" (case-insensitive)

---

### Test 4.9: Filter by Date Range
**Objective:** Filter records between two dates

**Steps:**
1. Create records on different dates
2. Call `GET /api/records/filter?startDate=2024-04-01&endDate=2024-04-30`

**Expected Result:**
- Status: 200 OK
- Returns only records within date range

---

### Test 4.10: Invalid Date Range Error
**Objective:** Reject startDate > endDate

**Steps:**
1. Call `GET /api/records/filter?startDate=2024-04-30&endDate=2024-04-01`

**Expected Result:**
- Status: 400 Bad Request
- Error: "Start date cannot be after end date."

---

## Dashboard Tests

### Test 5.1: Dashboard Summary
**Objective:** Calculate and display total income, expense, net balance

**Steps:**
1. Create: Income 10000, Expense 3000
2. Login as analyst
3. Call `GET /api/dashboard/summary`

**Expected Result:**
- totalIncome: 10000.00
- totalExpense: 3000.00
- netBalance: 7000.00

---

### Test 5.2: Category-Wise Totals
**Objective:** Breakdown expenses by category

**Steps:**
1. Create:
   - Salary: 10000 (INCOME)
   - Rent: 2000 (EXPENSE)
   - Food: 500 (EXPENSE)
   - Freelance: 5000 (INCOME)
2. Call `GET /api/dashboard/category-wise`

**Expected Result:**
- Returns array with categories and totals
- Example: [{category: "Salary", total: 10000}, {category: "Salary", total: 5000},...]

---

### Test 5.3: Recent Activity
**Objective:** Get most recent transactions

**Steps:**
1. Create 3 records with different timestamps
2. Call `GET /api/dashboard/recent-activity?limit=2`

**Expected Result:**
- Returns 2 most recent records ordered by createdAt DESC

---

### Test 5.4: Monthly Trends
**Objective:** View income/expense by month

**Steps:**
1. Create transactions in multiple months
2. Call `GET /api/dashboard/monthly-trends`

**Expected Result:**
- Returns array: [{ year: 2024, month: 4, totalIncome: X, totalExpense: Y, netBalance: Z }, ...]

---

## Edge Cases & Error Handling

### Test 6.1: Null/Empty Required Fields
**Objective:** Validate required fields

**Steps:**
1. Register with missing fullName, email, or password

**Expected Result:**
- Status: 400 Bad Request
- Specific field validation errors returned

---

### Test 6.2: Invalid Email Format
**Objective:** Email validation on registration/login

**Steps:**
1. Register with email: "notanemail" or "user@"

**Expected Result:**
- Status: 400 Bad Request
- Error: "Email format is invalid"

---

### Test 6.3: String Too Long
**Objective:** Field length validation

**Steps:**
1. Create record with category > 100 characters

**Expected Result:**
- Status: 400 Bad Request
- Error: "Category must not exceed 100 characters"

---

### Test 6.4: Deleted Records Not Returned
**Objective:** Soft-deleted records excluded from queries

**Steps:**
1. Create and delete record
2. Call all list endpoints

**Expected Result:**
- Deleted records never appear in any results

---

### Test 6.5: Not Found Error
**Objective:** Request non-existent resource

**Steps:**
1. Call `GET /api/records/99999`

**Expected Result:**
- Status: 404 Not Found
- Error: "Record not found with id: 99999"

---

## Pagination Tests

### Test 7.1: Default Pagination
**Objective:** Default page size works correctly

**Steps:**
1. Create 25 records
2. Call `GET /api/records` (no pagination params)

**Expected Result:**
- Status: 200 OK
- Returns first 10 records (default size)
- totalElements: 25, totalPages: 3

---

### Test 7.2: Custom Page Size
**Objective:** Custom page size parameter works

**Steps:**
1. Call `GET /api/records?page=0&size=5`

**Expected Result:**
- Returns exactly 5 records per page

---

### Test 7.3: Sorting Ascending
**Objective:** Sort records ascending

**Steps:**
1. Create records with amounts: 100, 500, 200
2. Call `GET /api/records?sortBy=amount&sortDir=asc`

**Expected Result:**
- Records ordered: 100, 200, 500

---

### Test 7.4: Sorting Descending
**Objective:** Sort records descending

**Steps:**
1. Call `GET /api/records?sortBy=amount&sortDir=desc`

**Expected Result:**
- Records ordered: 500, 200, 100

---

### Test 7.5: Out of Range Page
**Objective:** Handle page number beyond available pages

**Steps:**
1. Create 5 records (1 page with size 10)
2. Call `GET /api/records?page=5&size=10`

**Expected Result:**
- Returns empty content array
- totalPages: 1

---

## Performance Tests (Manual)

### Test 8.1: Large Dataset Performance
**Objective:** Verify system handles 1000+ records efficiently

**Steps:**
1. Create 1000 financial records
2. Call `GET /api/records?page=0&size=50`
3. Measure response time

**Expected Result:**
- Response time < 500ms
- All records accessible via pagination

---

### Test 8.2: Complex Filter Performance
**Objective:** Multi-field filter performance

**Steps:**
1. Create 500 records
2. Call filter with type + category + date range
3. Measure response time

**Expected Result:**
- Response time < 300ms

---

## Test Execution Checklist

- [ ] All authentication flows tested
- [ ] All role-based restrictions verified
- [ ] CRUD operations working for users and records
- [ ] Dashboard calculations correct
- [ ] Pagination and sorting working
- [ ] All validations triggered correctly
- [ ] Rate limiting working (account locks after 5 attempts)
- [ ] Soft delete functioning properly
- [ ] Token-based authorization working
- [ ] Password complexity enforced
- [ ] Error messages are clear and helpful
- [ ] No sensitive data in error responses
- [ ] Database transactions working correctly
- [ ] Response times acceptable

---

## Postman Collection Endpoints

For convenience, here are all endpoints to test:

**Auth:**
- POST /api/auth/register
- POST /api/auth/login

**Users (Admin only):**
- POST /api/users
- GET /api/users
- GET /api/users/{id}
- PUT /api/users/{id}
- PATCH /api/users/{id}/role
- PATCH /api/users/{id}/status

**Financial Records:**
- POST /api/records (Admin)
- GET /api/records (All)
- GET /api/records/{id} (All)
- PUT /api/records/{id} (Admin)
- DELETE /api/records/{id} (Admin)
- GET /api/records/filter (All)

**Dashboard:**
- GET /api/dashboard/summary (All)
- GET /api/dashboard/category-wise (All)
- GET /api/dashboard/recent-activity (All)
- GET /api/dashboard/monthly-trends (All)

---

## Notes
- Use provided sample credentials for initial testing:
  - admin@example.com / Admin@123
  - analyst@example.com / Analyst@123
  - viewer@example.com / Viewer@123
- All timestamps (createdAt, updatedAt) are set automatically by the system
- Passwords are never returned in API responses
- JWT tokens expire based on JwtUtil configuration
- Rate limiting resets after 15 minutes or successful login
