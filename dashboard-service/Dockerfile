FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY finance-shared-library ./finance-shared-library
COPY dashboard-service ./dashboard-service
RUN cd finance-shared-library && mvn clean install -q
RUN cd dashboard-service && mvn clean package -q -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/dashboard-service/target/dashboard-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
