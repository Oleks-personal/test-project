FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /device-project
COPY --from=build /workspace/build/libs/device-project-*.jar ./device-project.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "device-project.jar"]
