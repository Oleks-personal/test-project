# Device Management Service for 1Global

A Spring Boot service for managing corporate hardware assets, featuring domain-driven business rules, PostgreSQL data integrity, and idempotent API updates

---

## 📋 Overview

Tasked with developing a REST API capable of persisting and managing device resources.

### Device Domain
* **ID:** Unique identifier.
* **Name:** Device name.
* **Brand:** Device manufacturer/brand.
* **State:** `AVAILABLE`, `IN_USE`, `INACTIVE`.
* **Creation time:** Timestamp of when the device was registered.

### Supported Functionalities
* Create a new device.
* Fully and/or partially update an existing device.
* Fetch a single device.
* Fetch all devices (with pagination).
* Fetch devices by brand (with pagination).
* Fetch devices by state (with pagination).
* Delete a single device.

### Domain Validations
* **Creation time** is immutable and cannot be updated.
* **Name** and **Brand** properties cannot be updated if the device state is `IN_USE`.
* **IN_USE** devices cannot be deleted.

## Acceptance Criteria
* The application should compile and run successfully.
* The application must contain a reasonable test coverage.
* The API must be documented.
* The application must be capable of persisting resources on a database of your choice, excluding in-memory.
* The application must be containerized.
* The project must be delivered as a git repository.
* The project includes a README file with all project related/necessary documentation/instructions.

## Requirements
* Java 21+
* Maven 3.9+ or Gradle 8+
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

## 🛠 Prerequisites & Requirements

* **Java 21**
* **Gradle 8+**
* **Docker Desktop**
* **PostgreSQL 16** (If running outside of Docker Compose)

---

## 🚀 Local Development Setup

### 1. Environment Configuration
The project uses environment variables for database configuration. A template file `.env.example` is provided.

1. Create a `.env` file from the template:
   ```bash
   cp .env.example .env
   ```
2. Update the `.env` file with your database credentials. The `.env` file is ignored by Git to keep your secrets secure.

### 2. Build and Test
To build the project and execute unit and integration tests:
```bash
./gradlew clean test bootJar
```

#### Reports:
* **Unit Tests:** `build/reports/tests/test/index.html`
* **JaCoCo Coverage:** `build/reports/jacoco/test/html/index.html`

> **Note:** `DeviceApplicationIT` is an integration test suite that utilizes [Testcontainers](https://docs.spring.io/spring-boot/reference/testing/testcontainers.html) to run a real PostgreSQL instance.

### 3. Run with Docker Compose
The repository includes a pre-configured `docker-compose.yml` that initializes the database and the application.

```bash
docker compose up -d --build
```

---

## 📖 API Documentation

The API is documented using OpenAPI/Swagger. Once the application is running, you can access the UI here:

[http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
