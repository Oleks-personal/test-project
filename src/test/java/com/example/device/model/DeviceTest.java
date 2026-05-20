package com.example.device.model;

import com.example.device.errors.BusinessRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    @Test
    @DisplayName("Test that IN_USE devices cannot be deleted.")
    void deviceCannotBeDeletedIfDeviceInUse() {
        Device device = new Device("device name", "Super Brand", DeviceState.IN_USE);
        boolean result = device.canBeDeleted();
        assertFalse(result, "IN_USE devices cannot be deleted");
    }

    @Test
    @DisplayName("Test that devices can be deleted if it not in IN_USE state.")
    void deviceCanBeDeletedIfDeviceNotInUse() {
        Device device = new Device("device name", "Super Brand", DeviceState.INACTIVE);
        boolean result = device.canBeDeleted();
        assertTrue(result, "Device can be deleted if it not in IN_USE state");
    }

    @Test
    @DisplayName("Test that name and brand properties cannot be updated if the device is IN_USE.")
    void deviceDetailsCannotBeUpdatedIfDeviceInUse() {
        Device device = new Device("device name", "Super Brand", DeviceState.IN_USE);
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> device.updateDetails("new device name", "Other Super Brand", DeviceState.AVAILABLE));
        assertEquals("Device 'name' or 'brand' cannot be updated while the device is in use.", exception.getMessage());
        assertEquals("device name", device.getName());
        assertEquals("Super Brand", device.getBrand());
        assertEquals(DeviceState.IN_USE, device.getState());
    }

    @Test
    @DisplayName("Test that device status can be updated if the device is IN_USE.")
    void deviceStatusCanBeUpdatedIfDeviceInUse() {
        Device device = new Device("device name", "Super Brand", DeviceState.IN_USE);
        device.updateDetails("device name", "Super Brand", DeviceState.INACTIVE);
        assertEquals("device name", device.getName());
        assertEquals("Super Brand", device.getBrand());
        assertEquals(DeviceState.INACTIVE, device.getState());
    }

    @Test
    @DisplayName("Test that name and brand properties can be updated if the device is not IN_USE.")
    void deviceDetailsCannotBeUpdatedIfDeviceNotInUse() {
        Device device = new Device("Device name", "Super Brand", DeviceState.AVAILABLE);
        device.updateDetails("new device name", "Other Super Brand", DeviceState.IN_USE);
        assertEquals("new device name", device.getName());
        assertEquals("Other Super Brand", device.getBrand());
        assertEquals(DeviceState.IN_USE, device.getState());
    }

    @Test
    @DisplayName("Test that device cannot be updated if new device name is blank")
    void deviceNameCannotBeUpdatedIfNewDeviceNameIsBlanc() {
        Device device = new Device("device name", "Super Brand", DeviceState.IN_USE);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> device.updateDetails("", "Other Super Brand", DeviceState.AVAILABLE));
        assertEquals("Device 'name' cannot be blank or null.", exception.getMessage());
    }

    @Test
    @DisplayName("Test that device cannot be updated if new device name is blank")
    void deviceNameCannotBeUpdatedIfNewDeviceNameIsNull() {
        Device device = new Device("device name", "Super Brand", DeviceState.IN_USE);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> device.updateDetails(null, "Other Super Brand", DeviceState.AVAILABLE));
        assertEquals("Device 'name' cannot be blank or null.", exception.getMessage());
    }

    @Test
    @DisplayName("Test that device cannot be updated if new device brand is blank")
    void deviceBrandCannotBeUpdatedIfNewDeviceNameIsBlanc() {
        Device device = new Device("device name", "Super Brand", DeviceState.IN_USE);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> device.updateDetails("new device name", "", DeviceState.AVAILABLE));
        assertEquals("Device 'brand' cannot be blank or null.", exception.getMessage());
    }

    @Test
    @DisplayName("Test that device cannot be updated if new device brand is null")
    void deviceBrandCannotBeUpdatedIfNewDeviceBrandIsNull() {
        Device device = new Device("device name", "Super Brand", DeviceState.IN_USE);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> device.updateDetails("new device name", null, DeviceState.AVAILABLE));
        assertEquals("Device 'brand' cannot be blank or null.", exception.getMessage());
    }


    @Test
    @DisplayName("Test that device cannot be updated if new device state is null")
    void deviceStateCannotBeUpdatedIfNewDeviceStateIsNull() {
        Device device = new Device("device name", "Super Brand", DeviceState.IN_USE);
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> device.updateDetails("new device name", "Other Super Brand", null));
        assertEquals("Device 'state' is required.", exception.getMessage());
    }
}