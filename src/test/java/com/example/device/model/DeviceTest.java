package com.example.device.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    @Test
    @DisplayName("Test that IN_USE devices cannot be deleted.")
    void DeviceCannotBeDeletedIfDeviceInUse() {
        Device device = new Device("device name", "Super Brand", DeviceState.IN_USE);
        boolean result = device.canBeDeleted();
        assertFalse(result, "IN_USE devices cannot be deleted");
    }

    @Test
    @DisplayName("Test that devices can be deleted if it not in IN_USE state.")
    void DeviceCanBeDeletedIfDeviceNotInUse() {
        Device device = new Device("device name", "Super Brand", DeviceState.INACTIVE);
        boolean result = device.canBeDeleted();
        assertTrue(result, "Device can be deleted if it not in IN_USE state");
    }

    @Test
    @DisplayName("Test that name and brand properties cannot be updated if the device is IN_USE.")
    void canUpdatePropertiesIfDeviceInUse() {
        Device device = new Device("device name", "Super Brand", DeviceState.IN_USE);
        boolean result = device.canUpdateProperties("new device name", "Other Super Brand");
        assertFalse(result, "Name and brand properties cannot be updated if the device is in use");
    }

    @Test
    @DisplayName("Test that name and brand properties can be updated if the device is not IN_USE.")
    void canUpdatePropertiesIfDeviceIsNotInUse() {
        Device device = new Device("Device name", "Super Brand", DeviceState.AVAILABLE);
        boolean result = device.canUpdateProperties("New device name", "Other Super Brand");
        assertTrue(result, "Name and brand properties can be updated if the device is in use");
    }
}