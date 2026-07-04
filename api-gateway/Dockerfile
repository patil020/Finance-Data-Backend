FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY finance-shared-library ./finance-shared-library
COPY api-gateway ./api-gateway
RUN cd finance-shared-library && mvn clean install -q
RUN cd api-gateway && mvn clean package -q -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/api-gateway/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
