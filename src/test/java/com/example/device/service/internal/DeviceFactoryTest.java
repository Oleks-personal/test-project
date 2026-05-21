package com.example.device.service.internal;

import com.example.device.model.Device;
import com.example.device.model.DeviceState;
import com.example.device.service.dto.DeviceCreateRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceFactoryTest {
    private final DeviceFactory deviceFactory = new DeviceFactory();

    @Test
    void createDevice_IfDeviceValid_ShouldCreateDevice() {
        Device device = deviceFactory.createDevice(new DeviceCreateRequest("Some Name", "Some Brand", DeviceState.AVAILABLE));

        assertNotNull(device.getId());
        assertEquals("Some Name", device.getName());
        assertEquals("Some Brand", device.getBrand());
        assertEquals(DeviceState.AVAILABLE, device.getState());
        assertNotNull(device.getCreationTime());
    }

    @Test
    void createDevice_IfDeviceNoValid_ShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> 
                deviceFactory.createDevice(new DeviceCreateRequest("", "Some Brand", DeviceState.AVAILABLE)));
    }
}