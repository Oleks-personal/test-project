package com.example.device.service.internal;

import com.example.device.model.Device;
import com.example.device.service.dto.DeviceCreateRequest;
import org.springframework.stereotype.Component;

@Component
public class DeviceFactory {
    public Device createDevice(DeviceCreateRequest deviceCreateRequest) {
        return new Device(deviceCreateRequest.name(), deviceCreateRequest.brand(), deviceCreateRequest.state());
    }
}
