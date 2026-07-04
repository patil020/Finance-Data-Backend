FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY service-discovery ./service-discovery
RUN cd service-discovery && mvn clean package -q -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/service-discovery/target/service-discovery-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
