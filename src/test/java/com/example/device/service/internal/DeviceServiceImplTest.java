package com.example.device.service.internal;

import com.example.device.errors.BusinessRuleViolationException;
import com.example.device.errors.DeviceNotFoundException;
import com.example.device.model.Device;
import com.example.device.model.DeviceState;
import com.example.device.repository.DeviceRepository;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DeviceResponse;
import com.example.device.service.dto.DeviceUpdateRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
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
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.example.device.service.internal.DeviceServiceImpl.JAKARTA_PERSISTENCE_LOCK_TIMEOUT;
import static com.example.device.service.internal.DeviceServiceImpl.PESSIMISTIC_LOCK_TIMEOUT_MS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {
    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private DeviceFactory deviceFactory;
    @Mock
    private EntityManager entityManager;
    @InjectMocks
    private DeviceServiceImpl deviceService;

    @Test
    void createDevice_ValidDevice_ShouldSave() {
        UUID deviceId = UUID.randomUUID();

        Device device = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE);
        DeviceCreateRequest request = createDeviceCreateRequest("New Name", "New Brand", DeviceState.IN_USE);
        DeviceResponse expectedDeviceResponse = createTestDeviceResponse(device);

        when(deviceFactory.createDevice(request)).thenReturn(device);
        when(deviceRepository.save(eq(device))).thenReturn(device);

        DeviceResponse deviceResponse = deviceService.createDevice(request);
        assertEquals(expectedDeviceResponse, deviceResponse);

        verify(deviceRepository, times(1)).save(any(Device.class));
    }

    @Test
    void createDevice_IfNotValidDevice_ShouldThrowException() {
        DeviceCreateRequest request = createDeviceCreateRequest("", "New Brand", DeviceState.INACTIVE);

        when(deviceFactory.createDevice(request)).thenThrow(new IllegalArgumentException());

        assertThrows(IllegalArgumentException.class, () -> deviceService.createDevice(request));

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    @DisplayName("When device exists and valid, should update and save.")
    void updateDevice_WhenDeviceExistsAndValid_ShouldUpdateAndSave() {
        UUID deviceId = UUID.randomUUID();
        Long version = 1L;

        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.AVAILABLE, version);
        DeviceUpdateRequest request = createDeviceUpdateRequest(Optional.of("New Name"), Optional.of("New Brand"), Optional.of(DeviceState.IN_USE), version);

        when(entityManager.find(
                eq(Device.class),
                eq(deviceId),
                eq(LockModeType.PESSIMISTIC_WRITE),
                eq(Map.of(JAKARTA_PERSISTENCE_LOCK_TIMEOUT, PESSIMISTIC_LOCK_TIMEOUT_MS))
        )).thenReturn(existingDevice);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceResponse result = deviceService.updateDevice(deviceId, request);

        assertNotNull(result);
        assertEquals(deviceId, existingDevice.getId());
        assertEquals("New Name", result.name());
        assertEquals("New Brand", result.brand());
        assertEquals(DeviceState.IN_USE, result.state());

        verify(deviceRepository, times(1)).save(existingDevice);
    }

    @Test
    void updateDevice_WhenDeviceInUseAndStatusChanged_ShouldUpdateAndSave() {
        UUID deviceId = UUID.randomUUID();
        Long version = 1L;

        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE, version);

        DeviceUpdateRequest request = createDeviceUpdateRequest(Optional.of("Old Name"), Optional.of("Old Brand"), Optional.of(DeviceState.INACTIVE), version);

        when(entityManager.find(
                eq(Device.class),
                eq(deviceId),
                eq(LockModeType.PESSIMISTIC_WRITE),
                eq(Map.of(JAKARTA_PERSISTENCE_LOCK_TIMEOUT, PESSIMISTIC_LOCK_TIMEOUT_MS))
        )).thenReturn(existingDevice);
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceResponse result = deviceService.updateDevice(deviceId, request);

        assertNotNull(result);
        assertEquals(deviceId, existingDevice.getId());
        assertEquals("Old Name", result.name());
        assertEquals("Old Brand", result.brand());
        assertEquals(DeviceState.INACTIVE, result.state());

        verify(deviceRepository, times(1)).save(existingDevice);
    }

    @Test
    void updateDevice_WhenDeviceDoesNotExist_ShouldThrowException() {
        UUID deviceId = UUID.randomUUID();
        Long version = 1L;

        DeviceUpdateRequest request = createDeviceUpdateRequest(Optional.empty(), Optional.empty(), Optional.empty(), version);

        when(entityManager.find(
                eq(Device.class),
                eq(deviceId),
                eq(LockModeType.PESSIMISTIC_WRITE),
                eq(Map.of(JAKARTA_PERSISTENCE_LOCK_TIMEOUT, PESSIMISTIC_LOCK_TIMEOUT_MS))
        )).thenReturn(null);

        assertThrows(DeviceNotFoundException.class, () -> deviceService.updateDevice(deviceId, request));

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void updateDevice_WhenDeviceInUseAndNameChanges_ShouldThrowBusinessRuleViolationException() {
        UUID deviceId = UUID.randomUUID();
        Long version = 1L;

        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE, version);

        DeviceUpdateRequest request = createDeviceUpdateRequest(Optional.of("Illegal Name Change"), Optional.of("Old Brand"), Optional.of(DeviceState.IN_USE), version);

        when(entityManager.find(
                eq(Device.class),
                eq(deviceId),
                eq(LockModeType.PESSIMISTIC_WRITE),
                eq(Map.of(JAKARTA_PERSISTENCE_LOCK_TIMEOUT, PESSIMISTIC_LOCK_TIMEOUT_MS))
        )).thenReturn(existingDevice);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> deviceService.updateDevice(deviceId, request));

        assertEquals("Device 'name' or 'brand' cannot be updated while the device is in use.", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void updateDevice_WhenDeviceInUseAndBrandChanges_ShouldThrowBusinessRuleViolationException() {
        UUID deviceId = UUID.randomUUID();
        Long version = 1L;

        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE, version);
        DeviceUpdateRequest request = createDeviceUpdateRequest(Optional.of("Old Name"), Optional.of("Illegal Brand Change"), Optional.of(DeviceState.IN_USE), version);

        when(entityManager.find(
                eq(Device.class),
                eq(deviceId),
                eq(LockModeType.PESSIMISTIC_WRITE),
                eq(Map.of(JAKARTA_PERSISTENCE_LOCK_TIMEOUT, PESSIMISTIC_LOCK_TIMEOUT_MS))
        )).thenReturn(existingDevice);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> deviceService.updateDevice(deviceId, request));

        assertEquals("Device 'name' or 'brand' cannot be updated while the device is in use.", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    @DisplayName("When request version is older than DB version, should skip update and return current state (Idempotency)")
    void updateDevice_WhenVersionIsStale_ShouldReturnExistingDeviceWithoutSaving() {
        UUID deviceId = UUID.randomUUID();
        long databaseVersion = 5L;
        long staleClientVersion = 4L;

        Device existingDevice = createTestDevice(deviceId, "Current Name", "Current Brand", DeviceState.AVAILABLE, databaseVersion);

        DeviceUpdateRequest request = createDeviceUpdateRequest(
                Optional.of("New Name"),
                Optional.of("New Brand"),
                Optional.of(DeviceState.IN_USE),
                staleClientVersion
        );

        // FIX: Mock the EntityManager instead of the Repository
        when(entityManager.find(
                eq(Device.class),
                eq(deviceId),
                eq(LockModeType.PESSIMISTIC_WRITE),
                anyMap() // Matches the Map.of(...) configuration parameter smoothly
        )).thenReturn(existingDevice);

        // Act
        DeviceResponse result = deviceService.updateDevice(deviceId, request);

        // Assert
        assertNotNull(result);
        assertEquals(databaseVersion, result.version());
        assertEquals("Current Name", result.name()); // Verifies the domain state was untouched

        // Verify that the save method was never called
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void deleteDevice_IfDeviceExistsAndValid_ShouldDelete() {
        UUID deviceId = UUID.randomUUID();
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.INACTIVE);

        when(deviceRepository.findById(deviceId)).thenReturn(Optional.of(existingDevice));

        deviceService.deleteDevice(deviceId);

        verify(deviceRepository, times(1)).delete(existingDevice);
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

        PageRequest pageable = PageRequest.of(1, 50);
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE);
        DeviceResponse deviceResponse = createTestDeviceResponse(existingDevice);
        List<DeviceResponse> expectedDevices = List.of(deviceResponse);

        when(deviceRepository.findByState(DeviceState.IN_USE, pageable)).thenReturn(new PageImpl<>(List.of(existingDevice)));

        Slice<DeviceResponse> devices = deviceService.findByState(DeviceState.IN_USE, pageable);

        assertEquals(new PageImpl<>(expectedDevices), devices);
        verify(deviceRepository, times(1)).findByState(DeviceState.IN_USE, pageable);
    }

    @Test
    void findByState_IfDeviceNotExists_ShouldReturnEmptyList() {
        PageRequest pageable = PageRequest.of(1, 50);
        when(deviceRepository.findByState(DeviceState.IN_USE, pageable)).thenReturn(Page.empty());

        Slice<DeviceResponse> devices = deviceService.findByState(DeviceState.IN_USE, pageable);
        assertEquals(Page.empty(), devices);

        verify(deviceRepository, times(1)).findByState(DeviceState.IN_USE, pageable);
    }

    @Test
    void findByBrand_IfDevicesExists_ShouldReturn() {
        UUID deviceId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(1, 50);

        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE);
        DeviceResponse deviceResponse = createTestDeviceResponse(existingDevice);
        List<DeviceResponse> expectedDevices = List.of(deviceResponse);

        when(deviceRepository.findByBrand("Old Brand", pageable)).thenReturn(new PageImpl<>(List.of(existingDevice)));

        Slice<DeviceResponse> devices = deviceService.findByBrand("Old Brand", pageable);

        assertEquals(new PageImpl<>(expectedDevices), devices);
        verify(deviceRepository, times(1)).findByBrand("Old Brand", pageable);
    }

    @Test
    void findByBrand_IfDeviceNotExists_ShouldReturnEmptyList() {
        PageRequest pageable = PageRequest.of(1, 50);
        when(deviceRepository.findByBrand("Old Brand", pageable)).thenReturn(Page.empty());

        Slice<DeviceResponse> devices = deviceService.findByBrand("Old Brand", pageable);
        assertEquals(Page.empty(), devices);

        verify(deviceRepository, times(1)).findByBrand("Old Brand", pageable);
    }

    @Test
    void findAll_IfDevicesExists_ShouldReturn() {
        UUID deviceId = UUID.randomUUID();

        PageRequest pageable = PageRequest.of(1, 50);
        Device existingDevice = createTestDevice(deviceId, "Old Name", "Old Brand", DeviceState.IN_USE);
        DeviceResponse deviceResponse = createTestDeviceResponse(existingDevice);
        List<DeviceResponse> expectedDevices = List.of(deviceResponse);

        when(deviceRepository.queryAllBy(pageable)).thenReturn(new PageImpl<>(List.of(existingDevice)));

        Slice<DeviceResponse> devices = deviceService.findAll(pageable);
        assertEquals(new PageImpl<>(expectedDevices), devices);

        verify(deviceRepository, times(1)).queryAllBy(pageable);
    }

    @Test
    void findAll_IfDeviceNotExists_ShouldReturnEmptyPage() {
        PageRequest pageable = PageRequest.of(1, 50);
        when(deviceRepository.queryAllBy(pageable)).thenReturn(Page.empty());

        Slice<DeviceResponse> page = deviceService.findAll(pageable);
        assertEquals(Page.empty(), page);

        verify(deviceRepository, times(1)).queryAllBy(pageable);
    }

    private static @NonNull Device createTestDevice(UUID deviceId, final String name, final String brand, final DeviceState deviceState) {
        return createTestDevice(deviceId, name, brand, deviceState, 1L);
    }

    private static @NonNull Device createTestDevice(UUID deviceId, final String name, final String brand, final DeviceState deviceState, Long version) {
        Device device = new Device(name, brand, deviceState) {
            @Override
            protected @NonNull UUID generateUUID() {
                return deviceId;
            }
        };
        if (version != null) {
            ReflectionTestUtils.setField(device, "version", version);
        }
        return device;
    }

    private static @NonNull DeviceResponse createTestDeviceResponse(Device device) {
        return new DeviceResponse(device.getId(), device.getName(), device.getBrand(), device.getState(), device.getVersion(), device.getCreationTime());
    }

    private static @NonNull DeviceUpdateRequest createDeviceUpdateRequest(Optional<String> newName, Optional<String> newBrand, Optional<DeviceState> newDeviceState, Long version) {
        return new DeviceUpdateRequest(
                newName,
                newBrand,
                newDeviceState,
                version
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
