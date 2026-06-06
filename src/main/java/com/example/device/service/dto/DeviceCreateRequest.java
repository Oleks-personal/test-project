package com.example.device.service.dto;

import com.example.device.model.DeviceState;
import jakarta.validation.constraints.NotBlank;

public record DeviceCreateRequest (
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Brand is required") String brand,
        DeviceState state
) {
    public DeviceCreateRequest {
        if(state == null) {state=DeviceState.INACTIVE;}
    }
}
