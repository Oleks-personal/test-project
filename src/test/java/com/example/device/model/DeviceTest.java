package com.example.device.model;

import com.example.device.errors.BusinessRuleViolationException;
import org.hamcrest.CoreMatchers;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DeviceTest {

    @Test
    @DisplayName("Test that IN_USE devices cannot be deleted.")
    void rejectsDeleteWhenInUse() {
        Device device = createDevice("device name", "Super Brand", DeviceState.IN_USE);
        boolean result = device.canBeDeleted();
        assertFalse(result, "IN_USE devices cannot be deleted");
    }

    @Test
    @DisplayName("Test that devices can be deleted if it not in IN_USE state.")
    void allowsDeleteWhenNotInUse() {
        Device device = createDevice("device name", "Super Brand", DeviceState.INACTIVE);
        boolean result = device.canBeDeleted();
        assertTrue(result, "Device can be deleted if it not in IN_USE state");
    }

    @Test
    @DisplayName("Test that name and brand properties cannot be updated if the device is IN_USE.")
    void rejectsNameOrBrandChangeWhenInUse() {
        Device device = createDevice("device name", "Super Brand", DeviceState.IN_USE);
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> device.updateDetails("new device name", "Other Super Brand", DeviceState.AVAILABLE));
        assertEquals("Device 'name' or 'brand' cannot be updated while the device is in use.", exception.getMessage());
        assertEquals("device name", device.getName());
        assertEquals("Super Brand", device.getBrand());
        assertEquals(DeviceState.IN_USE, device.getState());
    }

    @Test
    @DisplayName("Test that device status can be updated if the device is IN_USE.")
    void allowsStateChangeWhenInUse() {
        Device device = createDevice("device name", "Super Brand", DeviceState.IN_USE);
        device.updateDetails("device name", "Super Brand", DeviceState.INACTIVE);
        assertEquals("device name", device.getName());
        assertEquals("Super Brand", device.getBrand());
        assertEquals(DeviceState.INACTIVE, device.getState());
    }

    @Test
    @DisplayName("Test that name and brand properties can be updated if the device is not IN_USE.")
    void allowsDetailsChangeWhenNotInUse() {
        Device device = createDevice("Device name", "Super Brand", DeviceState.AVAILABLE);
        device.updateDetails("new device name", "Other Super Brand", DeviceState.IN_USE);
        assertEquals("new device name", device.getName());
        assertEquals("Other Super Brand", device.getBrand());
        assertEquals(DeviceState.IN_USE, device.getState());
    }

    @Test
    @DisplayName("Test that device cannot be updated if new device name is blank")
    void rejectsBlankName() {
        Device device = createDevice("device name", "Super Brand", DeviceState.IN_USE);
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> device.updateDetails("", "Other Super Brand", DeviceState.AVAILABLE));
        assertEquals("Device 'name' cannot be blank or null.", exception.getMessage());
    }

    @Test
    @DisplayName("Test that device cannot be updated if new device name is blank")
    void rejectsNullName() {
        Device device = createDevice("device name", "Super Brand", DeviceState.IN_USE);
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> device.updateDetails(null, "Other Super Brand", DeviceState.AVAILABLE));
        assertEquals("Device 'name' cannot be blank or null.", exception.getMessage());
    }

    @Test
    @DisplayName("Test that device cannot be updated if new device brand is blank")
    void rejectsBlankBrand() {
        Device device = createDevice("device name", "Super Brand", DeviceState.IN_USE);
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> device.updateDetails("new device name", "", DeviceState.AVAILABLE));
        assertEquals("Device 'brand' cannot be blank or null.", exception.getMessage());
    }

    @Test
    @DisplayName("Test that device cannot be updated if new device brand is null")
    void rejectsNullBrand() {
        Device device = createDevice("device name", "Super Brand", DeviceState.IN_USE);
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> device.updateDetails("new device name", null, DeviceState.AVAILABLE));
        assertEquals("Device 'brand' cannot be blank or null.", exception.getMessage());
    }


    @Test
    @DisplayName("Test that device cannot be updated if new device state is null")
    void rejectsNullState() {
        Device device = createDevice("device name", "Super Brand", DeviceState.IN_USE);
        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> device.updateDetails("new device name", "Other Super Brand", null));
        assertEquals("Device 'state' is required.", exception.getMessage());
    }

    @Test
    @DisplayName("Test that device with equal externalId is equal even if other fields not equal")
    void deviceEqualsIfExternalIdEquals() {
        UUID externalId = UUID.randomUUID();
        Device device1 = createDevice("device name", "Super Brand", DeviceState.IN_USE, externalId);
        Device device2 = createDevice("device name other", "Super Brand Other", DeviceState.INACTIVE, externalId);
        assertEquals(device1, device2);
    }

    @Test
    @DisplayName("Test that device toString return a String in expected format")
    void deviceToStringShouldReturnStringInExpectedFormat() {
        UUID externalId = UUID.randomUUID();
        Device device = createDevice("device name", "Super Brand", DeviceState.IN_USE, externalId);
        assertThat(device.toString(), CoreMatchers.containsString("[id=<null>,externalId=" + externalId + ",name=device name,brand=Super Brand,state=IN_USE,creationTime=<null>,version=<null>]"));
    }

    private static @NonNull Device createDevice(String deviceName, String deviceBrand, DeviceState deviceState) {
        return new Device(deviceName, deviceBrand, deviceState);
    }

    private static @NonNull Device createDevice(String deviceName, String deviceBrand, DeviceState deviceState, UUID externalId) {
        Device device = new Device(deviceName, deviceBrand, deviceState);
        ReflectionTestUtils.setField(device, "externalId", externalId);
        return device;
    }
}