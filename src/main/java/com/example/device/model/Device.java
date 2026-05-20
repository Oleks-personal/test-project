package com.example.device.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "devices")
public class Device {

    @Id
    @Column(columnDefinition = "UUID")
    private final UUID id = UUID.randomUUID();

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "device_state default 'available'")
    private DeviceState state = DeviceState.AVAILABLE;

    @Column(
            name = "creation_time",
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP"
    )
    private final OffsetDateTime creationTime = OffsetDateTime.now();

    protected Device() {
    }

    public Device(String name, String brand, DeviceState state) {
        this.name = name;
        this.brand = brand;
        this.state = state;
    }

    public boolean canBeDeleted() {
        return this.state != DeviceState.IN_USE;
    }

    public boolean canUpdateProperties(String newName, String newBrand) {
        if (this.state == DeviceState.IN_USE) {
            return Objects.equals(this.name, newName) && Objects.equals(this.brand, newBrand);
        }
        return true;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public DeviceState getState() {
        return state;
    }

    public void setState(DeviceState state) {
        this.state = state;
    }

    public OffsetDateTime getCreationTime() {
        return creationTime;
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