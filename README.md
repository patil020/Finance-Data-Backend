# Finance Data Processing and Access Control Backend

## Project Overview
This project is a clean CDAC-style Spring Boot backend for a finance dashboard.  
It supports secure JWT authentication, role-based access control, user management, financial record management, and dashboard summary APIs.

## Features
- JWT-based stateless authentication
- Role-based authorization with `ADMIN`, `ANALYST`, `VIEWER`
- User management with role and status updates
- Inactive users cannot log in
- Financial records CRUD with soft delete
- Filter records by type, category, and date range
- Pagination and sorting support
- Dashboard APIs:
  - total income
  - total expense
  - net balance
  - category-wise totals
  - recent activity
  - monthly trends
- DTO-based API layer (entities are not exposed directly)
- Global exception handling with structured JSON error responses
- Startup sample data initializer for quick testing
- Swagger/OpenAPI documentation

## Tech Stack
- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA (Hibernate)
- Spring Security
- JWT (`io.jsonwebtoken`)
- MySQL
- Lombok
- Bean Validation
- ModelMapper
- Swagger/OpenAPI (`springdoc-openapi`)
- Maven

## Package Structure
```text
src/main/java/com/financeapp
|-- config
|-- controller
|-- dao
|   `-- specification
|-- dto
|   |-- request
|   `-- response
|-- entities
|-- enums
|-- exception
|-- security
|-- service
|   `-- impl
`-- FinanceDataBackendApplication.java
```

## API List

### Auth APIs
- `POST /api/auth/register`
- `POST /api/auth/login`

### User APIs (ADMIN only)
- `POST /api/users`
- `GET /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`
- `PATCH /api/users/{id}/role`
- `PATCH /api/users/{id}/status`

### Financial Record APIs
- `POST /api/records` (ADMIN)
- `GET /api/records` (ADMIN, ANALYST, VIEWER)
- `GET /api/records/{id}` (ADMIN, ANALYST, VIEWER)
- `PUT /api/records/{id}` (ADMIN)
- `DELETE /api/records/{id}` (ADMIN)
- `GET /api/records/filter` (ADMIN, ANALYST, VIEWER)

### Dashboard APIs
- `GET /api/dashboard/summary`
- `GET /api/dashboard/category-wise`
- `GET /api/dashboard/recent-activity`
- `GET /api/dashboard/monthly-trends`

## Role Access Rules
- `ADMIN`
  - Full access to users, records, and dashboard APIs
- `ANALYST`
  - Read-only access to records and dashboard APIs
- `VIEWER`
  - Read-only access to records and dashboard APIs

## Database Setup
1. Install and run MySQL.
2. Create DB (optional, auto-create is enabled):
   ```sql
   CREATE DATABASE finance_backend;
   ```
3. Default DB config in `application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/finance_backend?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
   spring.datasource.username=KD3_89348_Ajinkya
   spring.datasource.password=manager
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   ```

## Application Run Steps
1. Open project in IntelliJ IDEA.
2. Ensure Java 17 is configured.
3. Run:
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```
4. Swagger UI:
   - `http://localhost:8080/swagger-ui.html`

## Sample Credentials
- `admin@example.com / Admin@123` (ADMIN)
- `analyst@example.com / Analyst@123` (ANALYST)
- `viewer@example.com / Viewer@123` (VIEWER)

## Assumptions Made
- Public registration (`/api/auth/register`) creates only `VIEWER` users.
- ADMIN can create users directly with any role using `/api/users`.
- Financial record delete is soft delete (`deleted=true`).
- Dashboard calculations ignore deleted records.
- Email is normalized to lowercase before persistence and login.

## Tradeoffs
- Used simple manual service logic and minimal abstraction to keep interview readability high.
- Used ModelMapper only for straightforward DTO mapping; custom fields are set manually where required.
- Kept API design focused on assignment scope (no OAuth, no external systems, no microservices).

## Future Improvements
- Add refresh token flow for JWT lifecycle management.
- Add audit trail table for user and record changes.
- Add unit tests and controller integration tests for each module.
- Add request/response logging with correlation IDs.
- Add rate limiting and brute-force protection on login API.
