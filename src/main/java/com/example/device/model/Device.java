package com.example.device.model;

import com.example.device.errors.BusinessRuleViolationException;
import io.micrometer.common.util.StringUtils;
import jakarta.persistence.*;
import org.jspecify.annotations.NonNull;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "devices", indexes = {
        @Index(name = "idx_devices_brand", columnList = "brand"),
        @Index(name = "idx_devices_state", columnList = "state")
})
public class Device {

    @Id
    @Column(columnDefinition = "UUID")
    private final UUID id = generateUUID();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "device_state default 'AVAILABLE'")
    private DeviceState state = DeviceState.AVAILABLE;

    @Column(
            name = "creation_time",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP"
    )
    private final OffsetDateTime creationTime = OffsetDateTime.now();

    @Version
    private Long version;

    protected Device() {
    }

    public Device(String name, String brand, DeviceState state) {
        validateProperties(name, brand, state);

        this.name = name;
        this.brand = brand;
        this.state = state;
    }

    public void updateDetails(String newName, String newBrand, DeviceState newState) {
        validateProperties(newName, newBrand, newState);

        if (!this.canUpdateProperties(newName, newBrand)) {
            throw new BusinessRuleViolationException("Device 'name' or 'brand' cannot be updated while the device is in use.");
        }

        this.name = newName;
        this.brand = newBrand;
        this.state = newState;
    }

    public boolean canBeDeleted() {
        return this.state != DeviceState.IN_USE;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public DeviceState getState() {
        return state;
    }

    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    public Long getVersion() {
        return version;
    }

    protected @NonNull UUID generateUUID() {
        return UUID.randomUUID();
    }

    private boolean canUpdateProperties(String newName, String newBrand) {
        if (this.state == DeviceState.IN_USE) {
            return Objects.equals(this.name, newName) && Objects.equals(this.brand, newBrand);
        }
        return true;
    }

    private void validateProperties(String newName, String newBrand, DeviceState newState) {
        validateProperty("name", newName);
        validateProperty("brand", newBrand);
        if (newState == null) {
            throw new IllegalArgumentException("Device 'state' is required.");
        }
    }

    private void validateProperty(String fieldName, String value) {
        if (StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Device '" + fieldName + "' cannot be blank or null.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device other)) return false;

        return Objects.equals(this.id, other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}