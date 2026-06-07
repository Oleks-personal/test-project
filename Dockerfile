FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /device-project

# Create a non-root user and group
RUN addgroup -S device && adduser -S device -G device

# Copy the jar and change ownership to the non-root user
COPY --from=build --chown=device:device /workspace/build/libs/device-project-*.jar ./device-project.jar

# Switch to the non-root user
USER device

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "device-project.jar"]
