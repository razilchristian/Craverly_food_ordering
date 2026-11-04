# Stage 1: Build Spring Boot application with Java 22 and Maven
FROM maven:3.9.6-eclipse-temurin-22 AS build
WORKDIR /app

# Copy pom.xml and source code directly
COPY backend/cravely-backend/pom.xml .
COPY backend/cravely-backend/src ./src

# Build package in a single pass with HTTP retry support and non-interactive output
# Prevents duplicate dependency resolution and mitigates Maven Central HTTP 429 rate limits
RUN mvn -B -ntp -Dmaven.wagon.http.retryHandler.count=5 clean package -DskipTests

# Stage 2: Production JRE 22 runtime
FROM eclipse-temurin:22-jre-jammy
WORKDIR /app

# Run securely as unprivileged user
RUN groupadd -r cravely && useradd -r -g cravely cravely
COPY --from=build /app/target/cravely-backend-*.jar app.jar
RUN chown -R cravely:cravely /app
USER cravely

ENV PORT=8081
EXPOSE ${PORT}

# Launch Spring Boot with dynamic Render port binding
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dserver.port=${PORT}", "-jar", "app.jar"]
