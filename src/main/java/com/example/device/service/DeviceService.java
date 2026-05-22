package com.example.device.service;

import com.example.device.model.DeviceState;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DeviceResponse;
import com.example.device.service.dto.DeviceUpdateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface DeviceService {
    DeviceResponse updateDevice(UUID id, DeviceUpdateRequest updateRequest);

    DeviceResponse createDevice(DeviceCreateRequest deviceCreateRequest);

    void deleteDevice(UUID id);

    DeviceResponse findById(UUID id);

    Slice<DeviceResponse> findByBrand(String brand, Pageable pageRequest);

    Slice<DeviceResponse> findByState(DeviceState state, Pageable pageRequest);

    Slice<DeviceResponse> findAll(Pageable pageRequest);
}
