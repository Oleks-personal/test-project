package com.example.device.service.dto;

import com.example.device.model.Device;
import com.example.device.model.DeviceState;

import java.util.Optional;

public record DeviceUpdateRequest(
        Optional<String> name,
        Optional<String> brand,
        Optional<DeviceState> state,
        long version
) {
    public String resolveName(Device existing) {
        return name != null && name.isPresent() ? name.get() : existing.getName();
    }

    public String resolveBrand(Device existing) {
        return brand != null && brand.isPresent() ? brand.get() : existing.getBrand();
    }

    public DeviceState resolveState(Device existing) {
        return state != null && state.isPresent() ? state.get() : existing.getState();
    }
}