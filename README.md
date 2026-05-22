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


# Device Management Service

A high-performance Spring Boot production service designed to track and manage corporate hardware assets (Devices). The system enforces domain-driven encapsulation rules, data-layer safety via PostgreSQL custom types/triggers, and client-side resiliency with an idempotent retry mechanism.

---

## 🏛 Architecture & Core Concepts

### 1. Rich Domain Model
Unlike classic anemic models, the `Device` entity owns its integrity. State changes and mutations are strictly validated inside the domain boundary. For example, modifying the name or brand of an asset while its state is `IN_USE` is structurally forbidden and throws a `BusinessRuleViolationException`.

### 2. Concurrency & Network Idempotency
To maximize database throughput under distributed architectures, this service utilizes **Pure Optimistic Locking** (`@Version`).

Additionally, the `PATCH` endpoint features an **Idempotent Retry Defense Filter**. If a network failure prevents a client from receiving a successful `200 OK` modification confirmation, subsequent client retries utilizing a stale version token are checked against the active payload data:
* **Identical State Retry:** If the data matches the current database state perfectly, the service short-circuits gracefully and returns `200 OK`.
* **True Mid-Air Collision:** If the payload differs, an `ObjectOptimisticLockingFailureException` is triggered, mapping directly to an **HTTP 409 Conflict**.

### 3. Native Database Safety
Schema evolution is driven by **Flyway**. The database engine enforces data structure rules directly on the bare metal via:
* Custom PostgreSQL Enumerated Types (`device_state`).
* Native PL/pgSQL database triggers to prevent rogue out-of-application table updates or illegal asset purges.

---

## Prerequisites

Before running or developing on this application, ensure you have the following installed:
* **Java 21** or higher
* **Docker Desktop**
* **PostgreSQL 16** (If running outside of Docker Compose)

---

## Local Development Setup

### 1. Build project
To build project and execute unit and integration tests you can use following command:
```bash
./gradlew clean test bootJar
```

Unit test and jacoco reports is available here `build/reports`

`DeviceApplicationIT` this is integration test which use [Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html)

### 2. Spin up Infrastructure Dependencies
The repository includes a pre-configured `docker-compose.yml` that initializes your local target database instance. Application deployment can be disabled, if you prefer to run application from your IDE.

```bash
docker compose up -d
```

## Documentation

Swagger documentation for API, available using following URL: http://localhost:8080/swagger-ui/index.html 