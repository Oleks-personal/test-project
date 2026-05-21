package com.example.device.service.dto;

import com.example.device.model.DeviceState;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeviceResponse(UUID id, String name, String brand, DeviceState state, long version, OffsetDateTime creationTime) {
}
