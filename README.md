# Finance Data Processing and Access Control Backend

## Project Overview
This is a Spring Boot backend for a finance dashboard assignment.
It provides JWT authentication, role-based authorization, user management, financial record management, and dashboard analytics APIs.

Repository root contains docs, while the runnable Spring Boot app is inside:

```text
spring_boot_backend_template/
```

## Core Features
- JWT-based stateless authentication
- Role-based authorization: `ADMIN`, `ANALYST`, `VIEWER`
- User management (`ADMIN`):
  - create user
  - list users
  - get user by ID
  - update user
  - change role
  - change status
- Login security:
  - inactive user login blocked
  - account locked for 15 minutes after 5 failed attempts
  - invalid login response includes remaining attempts
- Financial records:
  - create/read/update/delete
  - soft delete (`deleted=true`)
  - filtering by type/category/date range
  - pagination and sorting
- Dashboard APIs:
  - total income
  - total expense
  - net balance
  - category-wise totals
  - recent activity
  - monthly trends
- DTO-based API layer (entities are not exposed directly)
- Global exception handling with consistent JSON error format
- Startup sample data initializer (non-test profile)
- Swagger/OpenAPI documentation

## Tech Stack
- Java 17
- Spring Boot 3
- Spring Web
- Spring Data JPA (Hibernate)
- Spring Security
- JWT (`io.jsonwebtoken`)
- MySQL
- Bean Validation
- ModelMapper
- Lombok
- Swagger/OpenAPI (`springdoc-openapi`)
- Maven
- AWS EC2
- AWS RDS MySQL
- Nginx

## Package Structure
```text
spring_boot_backend_template/src/main/java/com/financeapp
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

## API Summary

### Auth
- `POST /api/auth/register`
- `POST /api/auth/login`

### User Management (`ADMIN`)
- `POST /api/users`
- `GET /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`
- `PATCH /api/users/{id}/role`
- `PATCH /api/users/{id}/status`

### Financial Records
- `POST /api/records` (`ADMIN`)
- `GET /api/records` (`ADMIN`, `ANALYST`, `VIEWER`)
- `GET /api/records/{id}` (`ADMIN`, `ANALYST`, `VIEWER`)
- `PUT /api/records/{id}` (`ADMIN`)
- `DELETE /api/records/{id}` (`ADMIN`)
- `GET /api/records/filter` (`ADMIN`, `ANALYST`, `VIEWER`)

### Dashboard
- `GET /api/dashboard/summary`
- `GET /api/dashboard/category-wise`
- `GET /api/dashboard/recent-activity`
- `GET /api/dashboard/monthly-trends`

## Role Access Rules
- `ADMIN`: full access to user, record, and dashboard APIs
- `ANALYST`: read-only access to record and dashboard APIs
- `VIEWER`: read-only access to record and dashboard APIs

## Configuration Strategy
This project is configured in a production-ready style using environment variables.

### `application.properties`
```properties
spring.application.name=finance-data-backend

server.port=${PORT:8080}

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3306/finance_backend?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:root}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:root}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false
spring.jpa.open-in-view=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

app.jwt.secret=${APP_JWT_SECRET:change-this-in-production}
app.jwt.expiration-ms=${APP_JWT_EXPIRATION_MS:86400000}

springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

### Why this style
- local development works with fallback defaults
- production secrets are not hardcoded in source code
- EC2/RDS deployment can use environment variables cleanly

## Local Setup

### Prerequisites
- Java 17
- Maven
- MySQL

### Optional local DB creation
```sql
CREATE DATABASE finance_backend;
```

### Local run
From repository root:

```powershell
cd spring_boot_backend_template
.\mvnw.cmd spring-boot:run
```

### Local Swagger UI
```text
http://localhost:8080/swagger-ui.html
http://localhost:8080/swagger-ui/index.html
```

## Build JAR
```powershell
cd spring_boot_backend_template
.\mvnw.cmd clean package -DskipTests
```

Runnable JAR:

```text
spring_boot_backend_template/target/finance-data-backend-0.0.1-SNAPSHOT.jar
```

Run locally:

```powershell
java -jar target\finance-data-backend-0.0.1-SNAPSHOT.jar
```

## Testing
```powershell
cd spring_boot_backend_template
.\mvnw.cmd test
```

## Sample Credentials
Created by `DataInitializer` in non-test profile:

- `admin@example.com / Admin@123` (`ADMIN`)
- `analyst@example.com / Analyst@123` (`ANALYST`)
- `viewer@example.com / Viewer@123` (`VIEWER`)

## Authentication & Lockout Behavior
- Registration (`/api/auth/register`) always creates a `VIEWER` user
- Emails are normalized to lowercase
- On login failure, response returns `401` with `remainingAttempts`
- After 5 failed attempts, login is blocked for 15 minutes
- Successful login resets failed-attempt counter

## Production Deployment (EC2 + RDS + systemd + Nginx)

### Architecture
```text
Browser -> Nginx -> Spring Boot App (EC2) -> RDS MySQL
```

### Production Components
- EC2: hosts the Spring Boot application
- RDS MySQL: production database
- `systemd` service: runs app reliably in background
- Nginx: reverse proxy from port `80` to `8080`

### AWS Deployment Notes
#### EC2
Spring Boot app runs on EC2 and listens internally on:

```text
8080
```

#### RDS
MySQL database is hosted in AWS RDS.

The application should use the RDS endpoint through environment variables:

```bash
SPRING_DATASOURCE_URL=jdbc:mysql://<RDS-ENDPOINT>:3306/finance_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=<RDS_PASSWORD>
```

### Production Environment File
On EC2, create:

```text
/opt/finance-app/finance-app.env
```

Example:

```bash
PORT=8080
SPRING_DATASOURCE_URL=jdbc:mysql://<RDS-ENDPOINT>:3306/finance_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=admin
SPRING_DATASOURCE_PASSWORD=<RDS_PASSWORD>
APP_JWT_SECRET=<LONG_RANDOM_SECRET>
APP_JWT_EXPIRATION_MS=86400000
```

### systemd Service Setup
Service file:

```text
/etc/systemd/system/finance-app.service
```

Content:

```ini
[Unit]
Description=Finance Data Backend Spring Boot App
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/opt/finance-app
EnvironmentFile=/opt/finance-app/finance-app.env
ExecStart=/usr/bin/java -jar /opt/finance-app/finance-data-backend.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Service commands:

```bash
sudo systemctl daemon-reload
sudo systemctl enable finance-app
sudo systemctl start finance-app
sudo systemctl status finance-app
sudo journalctl -u finance-app -f
sudo systemctl restart finance-app
sudo systemctl stop finance-app
```

### Nginx Reverse Proxy Setup
Install Nginx:

```bash
sudo dnf install nginx -y
sudo systemctl enable nginx
sudo systemctl start nginx
```

Create config:

```text
/etc/nginx/conf.d/finance-app.conf
```

Content:

```nginx
server {
    listen 80;
    server_name _;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Validate and restart Nginx:

```bash
sudo nginx -t
sudo systemctl restart nginx
```

### Security Group Recommendations
#### EC2 Inbound Rules
- `22` -> My IP
- `80` -> Anywhere
- `443` -> Anywhere
- `8080` -> only temporary for direct testing

#### RDS Security
- database should not be publicly accessible
- allow port `3306` only from the EC2 security group

### Deployment URLs
#### Direct Spring Boot testing
```text
http://<EC2-PUBLIC-IP>:8080/swagger-ui.html
http://<EC2-PUBLIC-IP>:8080/swagger-ui/index.html
```

#### Production-style via Nginx
```text
http://<EC2-PUBLIC-IP>/swagger-ui.html
http://<EC2-PUBLIC-IP>/swagger-ui/index.html
```

## Important Security Notes
- do not commit production passwords to Git
- do not hardcode JWT secrets in source code
- rotate DB passwords and JWT secrets if exposed
- use HTTPS with a domain name in real production environments

## Related Docs
- `MANUAL_TEST_SCENARIOS.md`
- `INTERVIEW_PREP.md`

## Future Improvements
- HTTPS with domain + SSL certificate
- refresh-token flow for JWT lifecycle management
- audit trail for user and record changes
- request/response logging with correlation IDs
- external/distributed attempt tracking for multi-instance deployments
- containerized deployment using Docker/ECS
