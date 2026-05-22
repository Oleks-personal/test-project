# Devices API
Task is to develop a REST API capable of persis and managing device resources.

## Device Domain
* Id
* Name
* Brand
* State (available, in-use, inactive)
* Creation time

## Supported Functionalities
* Create a new device.
* Fully and/or partially update an existing device.
* Fetch a single device.
* Fetch all devices.
* Fetch devices by brand.
* Fetch devices by state.
* Delete a single device.

## Domain Valida-ons
* Crea:on :me cannot be updated.
* Name and brand proper:es cannot be updated if the device is in use.
* In use devices cannot be deleted.

## Acceptance Criteria
* The applica:on should compile and run successfully.
* The applica:on must contain a reasonable test coverage.
* The API must be documented.
* The application must be capable of persis:ng resources on a database of your choice, excluding in-memory.
* The application must be containerized.
* The project must be delivered as a git repository.
* The project includes a README file with all project related/necessary documentation/instructions.

## Requirements
* Java 21+
* Maven 3.9+ or Gradle 8+

## Build project
To build project and execute unit and integration tests you can use following command:
```bash
./gradlew clean test bootJar
```

Unit test and jacoco reports is available here `build/reports`

`DeviceApplicationIT` this is integration test which use [Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)

## Run application
After successful build you can run application using docker compose with following command:
 ```bash
docker-compose up 
```

## Documentation

Swagger documentation for API, available using following URL: http://localhost:8080/swagger-ui/index.html 