# Stage 1: Build Spring Boot application with Java 22 and Maven
FROM maven:3.9.6-eclipse-temurin-22 AS build
WORKDIR /app

# Pre-fetch Maven dependencies for layer caching
COPY backend/cravely-backend/pom.xml .
RUN mvn dependency:go-offline -B

# Compile and package application
COPY backend/cravely-backend/src ./src
RUN mvn clean package -DskipTests

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

