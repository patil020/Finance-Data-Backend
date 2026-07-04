FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY config-server ./config-server
RUN cd config-server && mvn clean package -q -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/config-server/target/config-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "app.jar"]
