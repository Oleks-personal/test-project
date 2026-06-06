package com.example.device.service.dto;

import com.example.device.model.DeviceState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeviceCreateRequest (
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name length is more than 255 characters")
        String name,
        @NotBlank(message = "Brand is required")
        @Size(max = 255, message = "Brand length is more than 255 characters")
        String brand,
        DeviceState state
) {
    public DeviceCreateRequest {
        if(state == null) {state=DeviceState.INACTIVE;}
    }
}
