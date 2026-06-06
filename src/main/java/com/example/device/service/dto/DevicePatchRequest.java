package com.example.device.service.dto;

import com.example.device.model.DeviceState;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DevicePatchRequest(
        @Size(max = 255, message = "Name length is more than 255 characters") String name,
        @Size(max = 255, message = "Brand length is more than 255 characters")
        String brand,
        DeviceState state,
        @NotNull(message = "Version is required") Long version
) {
}
