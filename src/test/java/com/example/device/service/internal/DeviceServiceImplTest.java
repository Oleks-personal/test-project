package com.example.device.service.internal;

import com.example.device.errors.BusinessRuleViolationException;
import com.example.device.errors.DeviceNotFoundException;
import com.example.device.model.Device;
import com.example.device.model.DeviceState;
import com.example.device.repository.DeviceRepository;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DeviceUpdateRequest;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {
    @Mock
    private DeviceRepository deviceRepository;
    @InjectMocks
    private DeviceServiceImpl deviceService;

    @Test
    void createDevice_ValidDevice_ShouldSave() {
        DeviceCreateRequest request = createDeviceCreateRequest("New Name", "New Brand", DeviceState.IN_USE);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        deviceService.createDevice(request);
        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    void createDevice_DeviceWithEmptyName_ShouldThrowException() {
        DeviceCreateRequest request = createDeviceCreateRequest("", "New Brand", DeviceState.INACTIVE);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> deviceService.createDevice(request));

        assertEquals("Device 'name' cannot be blank or null.", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void createDevice_DeviceWithEmptyBrand_ShouldThrowException() {
        DeviceCreateRequest request = createDeviceCreateRequest("New Name", "", DeviceState.INACTIVE);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> deviceService.createDevice(request));

        assertEquals("Device 'brand' cannot be blank or null.", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void createDevice_DeviceWithNullState_ShouldThrowException() {
        DeviceCreateRequest request = createDeviceCreateRequest("New Name", "New Brand", null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> deviceService.createDevice(request));

        assertEquals("Device 'state' is required.", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    @DisplayName("When device exists and valid, should update and save.")
    void updateDevice_WhenDeviceExistsAndValid_ShouldUpdateAndSave() {
        UUID deviceId = UUID.randomUUID();
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.AVAILABLE);

        DeviceUpdateRequest request = createDeviceUpdateRequest(Optional.of("New Name"), Optional.of("New Brand"), Optional.of(DeviceState.IN_USE));

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingDevice));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Device result = deviceService.updateDevice(deviceId, request);

        assertNotNull(result);
        assertEquals(deviceId, existingDevice.getId());
        assertEquals("New Name", result.getName());
        assertEquals("New Brand", result.getBrand());
        assertEquals(DeviceState.IN_USE, result.getState());

        verify(deviceRepository, times(1)).save(existingDevice);
    }

    @Test
    void updateDevice_WhenDeviceInUseAndStatusChanged_ShouldUpdateAndSave() {
        UUID deviceId = UUID.randomUUID();
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE);

        DeviceUpdateRequest request = createDeviceUpdateRequest(Optional.of("Old Name"), Optional.of("Old Brand"), Optional.of(DeviceState.INACTIVE));

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingDevice));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Device result = deviceService.updateDevice(deviceId, request);

        assertNotNull(result);
        assertEquals(deviceId, existingDevice.getId());
        assertEquals("Old Name", result.getName());
        assertEquals("Old Brand", result.getBrand());
        assertEquals(DeviceState.INACTIVE, result.getState());

        verify(deviceRepository, times(1)).save(existingDevice);
    }

    @Test
    void updateDevice_WhenDeviceDoesNotExist_ShouldThrowException() {
        UUID deviceId = UUID.randomUUID();
        DeviceUpdateRequest request = createDeviceUpdateRequest(Optional.empty(), Optional.empty(), Optional.empty());

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        assertThrows(DeviceNotFoundException.class, () -> deviceService.updateDevice(deviceId, request));

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void updateDevice_WhenDeviceInUseAndNameChanges_ShouldThrowBusinessRuleViolationException() {
        UUID deviceId = UUID.randomUUID();
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE);

        DeviceUpdateRequest request = createDeviceUpdateRequest(Optional.of("Illegal Name Change"), Optional.of("Old Brand"), Optional.of(DeviceState.IN_USE));

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingDevice));

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> deviceService.updateDevice(deviceId, request));

        assertEquals("Device 'name' or 'brand' cannot be updated while the device is in use.", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void updateDevice_WhenDeviceInUseAndBrandChanges_ShouldThrowBusinessRuleViolationException() {
        UUID deviceId = UUID.randomUUID();
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE);

        DeviceUpdateRequest request = createDeviceUpdateRequest(Optional.of("Old Name"), Optional.of("Illegal Brand Change"), Optional.of(DeviceState.IN_USE));

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingDevice));

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> deviceService.updateDevice(deviceId, request));

        assertEquals("Device 'name' or 'brand' cannot be updated while the device is in use.", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void deleteDevice_IfDeviceExistsAndValid_ShouldDelete() {
        UUID deviceId = UUID.randomUUID();
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.INACTIVE);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingDevice));

        deviceService.deleteDevice(deviceId);

        verify(deviceRepository, times(1)).deleteById(deviceId);
    }

    @Test
    void deleteDevice_IfDeviceNotExists_ShouldThrowException() {
        UUID deviceId = UUID.randomUUID();

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        DeviceNotFoundException exception = assertThrows(DeviceNotFoundException.class, () -> deviceService.deleteDevice(deviceId));

        assertEquals("Device not found with id: " + deviceId, exception.getMessage());

        verify(deviceRepository, never()).deleteById(deviceId);
    }

    @Test
    void deleteDevice_IfDeviceInUse_ShouldThrowException() {
        UUID deviceId = UUID.randomUUID();
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingDevice));

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> deviceService.deleteDevice(deviceId));

        assertEquals("Cannot delete device because it is currently in use.", exception.getMessage());

        verify(deviceRepository, never()).deleteById(deviceId);
    }

    @Test
    void findById_IfDeviceExists_ShouldReturn() {
        UUID deviceId = UUID.randomUUID();
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.INACTIVE);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingDevice));

        deviceService.findById(deviceId);

        verify(deviceRepository, times(1)).findById(deviceId);
    }

    @Test
    void findById_IfDeviceNotExists_ShouldTrowException() {
        UUID deviceId = UUID.randomUUID();
        when(deviceRepository.findById(deviceId)).thenReturn(Optional.empty());

        DeviceNotFoundException exception = assertThrows(DeviceNotFoundException.class, () -> deviceService.findById(deviceId));

        assertEquals("Device not found with id: " + deviceId, exception.getMessage());

        verify(deviceRepository, times(1)).findById(deviceId);
    }

    @Test
    void findByState_IfDevicesExists_ShouldReturn() {
        UUID deviceId = UUID.randomUUID();
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE);

        when(deviceRepository.findByState(DeviceState.IN_USE)).thenReturn(List.of(existingDevice));

        deviceService.findByState(DeviceState.IN_USE);

        verify(deviceRepository, times(1)).findByState(DeviceState.IN_USE);
    }

    @Test
    void findByState_IfDeviceNotExists_ShouldReturnEmptyList() {
        when(deviceRepository.findByState(DeviceState.IN_USE)).thenReturn(List.of());

        List<Device> devices = deviceService.findByState(DeviceState.IN_USE);
        assertEquals(List.of(), devices);

        verify(deviceRepository, times(1)).findByState(DeviceState.IN_USE);
    }

    @Test
    void findByBrand_IfDevicesExists_ShouldReturn() {
        UUID deviceId = UUID.randomUUID();
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE);

        when(deviceRepository.findByBrand("Old Brand")).thenReturn(List.of(existingDevice));

        deviceService.findByBrand("Old Brand");

        verify(deviceRepository, times(1)).findByBrand("Old Brand");
    }

    @Test
    void findByBrand_IfDeviceNotExists_ShouldReturnEmptyList() {
        when(deviceRepository.findByBrand("Old Brand")).thenReturn(List.of());

        List<Device> devices = deviceService.findByBrand("Old Brand");
        assertEquals(List.of(), devices);

        verify(deviceRepository, times(1)).findByBrand("Old Brand");
    }

    @Test
    void findAll_IfDevicesExists_ShouldReturn() {
        UUID deviceId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(1, 50);

        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE);

        when(deviceRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(existingDevice)));

        deviceService.findAll(pageable);

        verify(deviceRepository, times(1)).findAll(pageable);
    }

    @Test
    void findAll_IfDeviceNotExists_ShouldReturnEmptyPage() {
        PageRequest pageable = PageRequest.of(1, 50);
        when(deviceRepository.findAll(pageable)).thenReturn(Page.empty());

        Page<Device> page = deviceService.findAll(pageable);
        assertEquals(Page.empty(), page);

        verify(deviceRepository, times(1)).findAll(pageable);
    }

    private static @NonNull Device createTestDevice(UUID deviceId, final String name, final String brand, final DeviceState deviceState) {
        return new Device(name, brand, deviceState) {
            @Override
            protected @NonNull UUID generateUUID() {
                return deviceId;
            }
        };
    }

    private static @NonNull DeviceUpdateRequest createDeviceUpdateRequest(Optional<String> newName, Optional<String> newBrand, Optional<DeviceState> newDeviceState) {
        return new DeviceUpdateRequest(
                newName,
                newBrand,
                newDeviceState
        );
    }

    private static @NonNull DeviceCreateRequest createDeviceCreateRequest(String name, String brand, DeviceState deviceState) {
        return new DeviceCreateRequest(
                name,
                brand,
                deviceState
        );
    }
}
