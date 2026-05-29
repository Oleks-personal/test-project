package com.example.device.service.dto;

import com.example.device.model.DeviceState;
import jakarta.validation.constraints.NotNull;

public record DevicePatchRequest(
        String name,
        String brand,
        DeviceState state,
        @NotNull(message = "Version is required") Long version
) {
}
