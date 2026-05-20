package com.example.device.service.dto;

import com.example.device.model.DeviceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeviceCreateRequest (
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Brand is required") String brand,
        @NotNull(message = "Initial state is required") DeviceState state
) { }
