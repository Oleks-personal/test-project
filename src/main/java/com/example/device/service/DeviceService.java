package com.example.device.service;

import com.example.device.model.Device;
import com.example.device.model.DeviceState;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DeviceUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

public interface DeviceService {
    Device updateDevice(UUID id, DeviceUpdateRequest updateRequest);

    Device createDevice(DeviceCreateRequest deviceCreateRequest);

    void deleteDevice(UUID id);

    Device findById(UUID id);

    List<Device> findByBrand(String brand);

    List<Device> findByState(DeviceState state);

    Page<Device> findAll(PageRequest pageRequest);
}
