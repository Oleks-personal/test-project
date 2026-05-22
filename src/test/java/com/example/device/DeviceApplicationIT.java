package com.example.device;

import com.example.device.model.DeviceState;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DeviceResponse;
import com.example.device.service.dto.DeviceUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DeviceApplicationIT {

    @LocalServerPort
    private int port;

    private RestClient restClient;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.14-alpine")
            .withDatabaseName("device_integration_db")
            .withUsername("prod_user")
            .withPassword("prod_pass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @BeforeEach
    void setUp() {
        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port + "/api/v1/devices")
                .build();
    }

    @Test
    @DisplayName("HAPPY PATH: Create, Fetch, and Update a Device over REST API with database verification")
    void fullDeviceLifecycle_ViaRestApi_ShouldSucceed() {
        // --- 1. POST: Create Device ---
        var createRequest = new DeviceCreateRequest("LaserJet 500", "HP", DeviceState.AVAILABLE);

        var createResponse = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .toEntity(DeviceResponse.class);

        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        DeviceResponse createdDevice = createResponse.getBody();
        assertThat(createdDevice).isNotNull();
        UUID generatedId = createdDevice.id();
        assertThat(createdDevice.name()).isEqualTo("LaserJet 500");

        // --- 2. GET: Verify it is retrievable by ID ---
        var getResponse = restClient.get()
                .uri("/{id}", generatedId)
                .retrieve()
                .toEntity(DeviceResponse.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().name()).isEqualTo("LaserJet 500");

        // --- 3. PATCH: Partially update properties ---
        var updateRequest = new DeviceUpdateRequest(
                Optional.of("LaserJet 500 Enterprise"),
                Optional.empty(),
                Optional.empty(),
                createdDevice.version()
        );

        var patchResponse = restClient.patch()
                .uri("/{id}", generatedId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest)
                .retrieve()
                .toEntity(DeviceResponse.class);

        assertThat(patchResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(patchResponse.getBody().name()).isEqualTo("LaserJet 500 Enterprise");
        assertThat(patchResponse.getBody().version()).isEqualTo(1L); // Optimistic lock increments
    }

    @Test
    @DisplayName("VALIDATION PATH: Sending invalid request should reject at web layer with 400 Bad Request")
    void createDevice_InvalidPayload_ShouldReturn400BadRequest() {
        // Blank name violates @NotBlank constraint on the DTO
        var invalidRequest = new DeviceCreateRequest("", "HP", DeviceState.AVAILABLE);

        assertThatThrownBy(() -> restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequest)
                .retrieve()
                .toBodilessEntity()
        )
                .isInstanceOf(HttpClientErrorException.BadRequest.class)
                .hasMessageContaining("""
                        "status":400,"error":"Bad Request","path":"/api/v1/devices"}" """);
    }

    @Test
    @DisplayName("DATABASE TRIGGER PATH: Modifying name/brand of an IN_USE device via REST should fail at database layer")
    void updateDevice_ModifyNameWhileInUse_ShouldFailViaDatabaseTrigger() {
        // Create a device initially
        var createRequest = new DeviceCreateRequest("iPhone 15", "Apple", DeviceState.AVAILABLE);
        DeviceResponse device = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(DeviceResponse.class);

        // Change its status to IN_USE
        var moveUsageRequest = new DeviceUpdateRequest(Optional.empty(), Optional.empty(), Optional.of(DeviceState.IN_USE), device.version());
        DeviceResponse inUseDevice = restClient.patch()
                .uri("/{id}", device.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(moveUsageRequest)
                .retrieve()
                .body(DeviceResponse.class);

        // Attempt to change the name while it is active (This violates our PostgreSQL update trigger rule)
        var invalidUpdateRequest = new DeviceUpdateRequest(Optional.of("Hacked Name"), Optional.empty(), Optional.empty(), inUseDevice.version());

        // Assert that the database trigger throws an error, causing a Transaction rollback and an HTTP failure
        assertThatThrownBy(() -> restClient.patch()
                .uri("/{id}", device.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidUpdateRequest)
                .retrieve()
                .toBodilessEntity()
        )
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageStartingWith("""
                                422 : "{"detail":"Device 'name' or 'brand' cannot be updated while the device is in use.","instance":"/api/v1/devices/%s","status":422,"title":"Business rule violation occurred" """,
                        device.id());
    }

    @Test
    @DisplayName("IDEMPOTENCY PATH: Stale retry with identical payload should return 200 OK and match current state")
    void updateDevice_IdenticalStaleRetry_ShouldReturn200OkAndBypassConflict() {
        // Create a baseline device (Database will be at Version 0)
        var createRequest = new DeviceCreateRequest("ThinkPad X1", "Lenovo", DeviceState.AVAILABLE);
        DeviceResponse initialDevice = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(DeviceResponse.class);

        assertThat(initialDevice.version()).isEqualTo(0L);

        // Prepare a PATCH request payload to modify the state
        var updateRequest = new DeviceUpdateRequest(
                Optional.empty(),
                Optional.empty(),
                Optional.of(DeviceState.INACTIVE),
                initialDevice.version() // Sending Version 0
        );

        // First attempt: This succeeds, database updates state and increments version to 1
        var firstResponse = restClient.patch()
                .uri("/{id}", initialDevice.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest)
                .retrieve()
                .body(DeviceResponse.class);

        assertThat(firstResponse.version()).isEqualTo(1L);
        assertThat(firstResponse.state()).isEqualTo(DeviceState.INACTIVE);

        // Second attempt (The Stale Retry): Simulate the network dropping the response.
        // The client retries using the EXACT same payload still containing Version 0,
        // even though the DB is now at Version 1.
        var retryResponse = restClient.patch()
                .uri("/{id}", initialDevice.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest)
                .retrieve()
                .toEntity(DeviceResponse.class);

        assertThat(retryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retryResponse.getBody()).isNotNull();
        assertThat(retryResponse.getBody().version()).isEqualTo(1L);
        assertThat(retryResponse.getBody().state()).isEqualTo(DeviceState.INACTIVE);
    }

    @Test
    @DisplayName("CONFLICT PATH: Stale retry with DIFFERENT payload should trigger true Optimistic Locking Failure")
    void updateDevice_StaleVersionWithDifferentPayload_ShouldThrowConflict() {
        // Create a baseline device (Version 0)
        var createRequest = new DeviceCreateRequest("ProBook 450", "HP", DeviceState.AVAILABLE);
        DeviceResponse initialDevice = restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(createRequest)
                .retrieve()
                .body(DeviceResponse.class);

        // Successful update changes name (Database moves to Version 1)
        var updateRequest1 = new DeviceUpdateRequest(
                Optional.of("ProBook 450 G10"),
                Optional.empty(),
                Optional.empty(),
                initialDevice.version()
        );
        restClient.patch()
                .uri("/{id}", initialDevice.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(updateRequest1)
                .retrieve()
                .toBodilessEntity();

        // A completely different modification arrives using the stale Version 0
        // trying to change the state instead of the name. This is a legitimate concurrent conflict!
        var conflictingRequest = new DeviceUpdateRequest(
                Optional.empty(),
                Optional.empty(),
                Optional.of(DeviceState.INACTIVE),
                initialDevice.version()
        );


        // Because the payloads do not match, it must throw an Optimistic Locking error
        assertThatThrownBy(() -> restClient.patch()
                .uri("/{id}", initialDevice.id())
                .contentType(MediaType.APPLICATION_JSON)
                .body(conflictingRequest)
                .retrieve()
                .toBodilessEntity()
        )
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageStartingWith("""
                                409 : "{"detail":"The device resource you are trying to update has been modified by another concurrent transaction or retry context.","instance":"/api/v1/devices/%s","status":409,"title":"Concurrent Modification Conflict" """,
                        initialDevice.id());
    }
}