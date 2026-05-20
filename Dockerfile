FROM eclipse-temurin:21-jdk-alpine
COPY build/libs/device-project-*.jar /device-project/device-project.jar
WORKDIR /device-project
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "device-project.jar"]