# Stage 1: Build Spring Boot application with Java 22 and Maven
FROM maven:3.9.6-eclipse-temurin-22 AS build
WORKDIR /app

# Configure Maven mirror (Google Maven Central) to prevent HTTP 429 rate limits
COPY backend/cravely-backend/settings.xml /root/.m2/settings.xml
COPY backend/cravely-backend/settings.xml .
COPY backend/cravely-backend/pom.xml .
COPY backend/cravely-backend/src ./src

# Build package using Google Maven Central mirror with HTTP retries
RUN mvn -B -ntp -s settings.xml -Dmaven.wagon.http.retryHandler.count=5 clean package -DskipTests

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
