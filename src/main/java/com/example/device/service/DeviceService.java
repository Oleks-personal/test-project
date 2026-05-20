package com.example.device.service;

import com.example.device.model.Device;
import com.example.device.service.dto.DeviceUpdateRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public interface DeviceService {
    @Transactional
    Device updateDevice(UUID id, DeviceUpdateRequest updateRequest);
}
