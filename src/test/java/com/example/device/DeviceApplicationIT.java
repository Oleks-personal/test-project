package com.example.device;

import com.example.device.model.DeviceState;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DevicePatchRequest;
import com.example.device.service.dto.DeviceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static com.example.device.model.DeviceState.*;
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
    void completesDeviceLifecycle() {
        DeviceResponse createdDevice = createDevice("LaserJet 500", "HP", AVAILABLE);
        DeviceResponse fetchedDevice = getDevice(createdDevice.id());
        DeviceResponse updatedDevice = patchDevice(createdDevice.id(), patchName("LaserJet 500 Enterprise", createdDevice.version()));

        assertThat(createdDevice.name()).isEqualTo("LaserJet 500");
        assertThat(fetchedDevice.name()).isEqualTo("LaserJet 500");
        assertThat(updatedDevice.name()).isEqualTo("LaserJet 500 Enterprise");
        assertThat(updatedDevice.version()).isEqualTo(1L);
    }

    @Test
    @DisplayName("VALIDATION PATH: Sending invalid request should reject at web layer with 400 Bad Request")
    void rejectsInvalidCreatePayload() {
        var invalidRequest = new DeviceCreateRequest("", "HP", AVAILABLE);

        assertThatThrownBy(() -> postDevice(invalidRequest))
                .isInstanceOf(HttpClientErrorException.BadRequest.class)
                .hasMessageContaining("""
                        "status":400,"error":"Bad Request","path":"/api/v1/devices"}" """);
    }

    @Test
    @DisplayName("DATABASE TRIGGER PATH: Modifying name/brand of an IN_USE device via REST should fail at database layer")
    void rejectsNameChangeWhenInUse() {
        DeviceResponse device = createDevice("iPhone 15", "Apple", AVAILABLE);
        DeviceResponse inUseDevice = patchDevice(device.id(), patchState(IN_USE, device.version()));

        assertThatThrownBy(() -> patchDevice(device.id(), patchName("Hacked Name", inUseDevice.version())))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageStartingWith("""
                                422 : "{"detail":"Device 'name' or 'brand' cannot be updated while the device is in use.","instance":"/api/v1/devices/%s","status":422,"title":"Business rule violation occurred" """,
                        device.id());
    }

    @Test
    @DisplayName("IDEMPOTENCY PATH: Stale retry with identical payload should return 200 OK and match current state")
    void allowsIdenticalStaleRetry() {
        DeviceResponse initialDevice = createDevice("ThinkPad X1", "Lenovo", AVAILABLE);
        DevicePatchRequest updateRequest = patchState(INACTIVE, initialDevice.version());

        assertThat(initialDevice.version()).isEqualTo(0L);

        DeviceResponse firstResponse = patchDevice(initialDevice.id(), updateRequest);
        DeviceResponse retryResponse = patchDevice(initialDevice.id(), updateRequest);

        assertThat(firstResponse.version()).isEqualTo(1L);
        assertThat(firstResponse.state()).isEqualTo(INACTIVE);
        assertThat(retryResponse.version()).isEqualTo(1L);
        assertThat(retryResponse.state()).isEqualTo(INACTIVE);
    }

    @Test
    @DisplayName("CONFLICT PATH: Stale retry with DIFFERENT payload should trigger true Optimistic Locking Failure")
    void rejectsDifferentStalePayload() {
        DeviceResponse initialDevice = createDevice("ProBook 450", "HP", AVAILABLE);

        patchDevice(initialDevice.id(), patchName("ProBook 450 G10", initialDevice.version()));

        assertThatThrownBy(() -> patchDevice(initialDevice.id(), patchState(INACTIVE, initialDevice.version())))
                .isInstanceOf(HttpClientErrorException.class)
                .hasMessageStartingWith("""
                                409 : "{"detail":"The device resource you are trying to update has been modified by another concurrent transaction or retry context.","instance":"/api/v1/devices/%s","status":409,"title":"Concurrent Modification Conflict" """,
                        initialDevice.id());
    }

    @Test
    @DisplayName("CONFLICT PATH: Future version should trigger optimistic locking failure")
    void rejectsFutureVersion() {
        DeviceResponse initialDevice = createDevice("EliteBook 840", "HP", AVAILABLE);

        assertThatThrownBy(() -> patchDevice(initialDevice.id(), patchName("EliteBook 840 G11", initialDevice.version() + 1)))
                .isInstanceOf(HttpClientErrorException.Conflict.class);
    }

    private DeviceResponse createDevice(String name, String brand, DeviceState state) {
        var response = postDevice(new DeviceCreateRequest(name, brand, state));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }

    private ResponseEntity<DeviceResponse> postDevice(DeviceCreateRequest request) {
        return restClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(DeviceResponse.class);
    }

    private DeviceResponse getDevice(UUID id) {
        var response = restClient.get()
                .uri("/{id}", id)
                .retrieve()
                .toEntity(DeviceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }

    private DeviceResponse patchDevice(UUID id, DevicePatchRequest request) {
        var response = restClient.patch()
                .uri("/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toEntity(DeviceResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }

    private DevicePatchRequest patchName(String name, long version) {
        return new DevicePatchRequest(name, null, null, version);
    }

    private DevicePatchRequest patchState(DeviceState state, long version) {
        return new DevicePatchRequest(null, null, state, version);
    }
}
