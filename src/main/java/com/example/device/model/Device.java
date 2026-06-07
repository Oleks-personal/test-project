package com.example.device.model;

import com.example.device.errors.BusinessRuleViolationException;
import jakarta.persistence.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "devices",
        indexes = {
                @Index(name = "idx_devices_brand_creation_time", columnList = "brand,creation_time"),
                @Index(name = "uq_devices_external_id", columnList = "external_id"),
                @Index(name = "idx_devices_state_creation_time", columnList = "state,creation_time"),
                @Index(name = "idx_devices_creation_time", columnList = "creation_time")
        }
)
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "external_id",
            nullable = false,
            unique = true,
            updatable = false,
            columnDefinition = "UUID"
    )
    private UUID externalId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "brand", nullable = false)
    private String brand;

    @JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "device_state")
    private DeviceState state = DeviceState.INACTIVE;

    @Column(
            name = "creation_time",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP"
    )
    private OffsetDateTime creationTime;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Device() {
    }

    public Device(String name, String brand, DeviceState state) {
        validateProperties(name, brand, state);
        this.externalId = UUID.randomUUID();
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
    public Long getId() {
        return id;
    }

    public UUID getExternalId() {
        return externalId;
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
            throw new BusinessRuleViolationException("Device 'state' is required.");
        }
    }

    private void validateProperty(String fieldName, String value) {
        if (StringUtils.isBlank(value)) {
            throw new BusinessRuleViolationException("Device '" + fieldName + "' cannot be blank or null.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Device other)) return false;

        return Objects.equals(this.externalId, other.getExternalId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(externalId);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("externalId", externalId)
                .append("name", name)
                .append("brand", brand)
                .append("state", state)
                .append("creationTime", creationTime)
                .append("version", version)
                .toString();
    }
}