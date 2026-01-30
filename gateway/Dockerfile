# Build stage
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /build
COPY . .
RUN ./mvnw -DskipTests clean package -q

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]