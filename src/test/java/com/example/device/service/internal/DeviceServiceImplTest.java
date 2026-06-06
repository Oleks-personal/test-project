package com.example.device.service.internal;

import com.example.device.errors.BusinessRuleViolationException;
import com.example.device.errors.DeviceNotFoundException;
import com.example.device.model.Device;
import com.example.device.model.DeviceState;
import com.example.device.repository.DeviceRepository;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DevicePatchRequest;
import com.example.device.service.dto.DeviceResponse;
import com.example.device.service.dto.mapper.DeviceMapper;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.device.model.DeviceState.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceServiceImplTest {
    @Mock
    private DeviceRepository deviceRepository;
    @InjectMocks
    private DeviceServiceImpl deviceService;
    private final DeviceMapper deviceMapper= Mappers.getMapper(DeviceMapper.class);

    @BeforeEach
    void setUp() {
        deviceService = new DeviceServiceImpl(deviceRepository, deviceMapper);
    }

    @Test
    void createsDevice() {
        UUID deviceId = UUID.randomUUID();

        Device device = device(deviceId, "New Name", "New Brand", IN_USE);
        DeviceCreateRequest request = createRequest("New Name", "New Brand", IN_USE);
        DeviceResponse expectedDeviceResponse = response(device);

        when(deviceRepository.saveAndFlush(argThat(new DeviceMatcher(device)))).thenReturn(device);
        when(deviceRepository.findByExternalId(eq(deviceId))).thenReturn(Optional.of(device));

        DeviceResponse deviceResponse = deviceService.createDevice(request);
        assertEquals(expectedDeviceResponse, deviceResponse);

        verify(deviceRepository, times(1)).saveAndFlush(any(Device.class));
        verify(deviceRepository, times(1)).findByExternalId(eq(deviceId));
    }

    @Test
    void rejectsInvalidDevice() {
        DeviceCreateRequest request = createRequest("", "New Brand", INACTIVE);

        assertThrows(BusinessRuleViolationException.class, () -> deviceService.createDevice(request));

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void sanitizesDataIntegrityFailure() {
        DeviceCreateRequest request = createRequest("New Name", "New Brand", IN_USE);
        DataIntegrityViolationException repositoryException = new DataIntegrityViolationException("duplicate key value violates unique constraint");

        when(deviceRepository.saveAndFlush(any(Device.class))).thenThrow(repositoryException);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> deviceService.createDevice(request));

        assertEquals("Device data violates persistence constraints.", exception.getMessage());
        assertSame(repositoryException, exception.getCause());
    }

    @Test
    @DisplayName("When device exists and valid, should update and save.")
    void updatesDevice() {
        UUID deviceId = UUID.randomUUID();
        Long version = 1L;

        Device existingDevice = device(deviceId, "Old Name", "Old Brand", AVAILABLE, version);
        Device updatingDevice = device(deviceId, "New Name", "New Brand", IN_USE, version);
        DevicePatchRequest request = patch("New Name", "New Brand", IN_USE, version);

        givenDeviceExists(deviceId, existingDevice);
        when(deviceRepository.saveAndFlush(argThat(new DeviceMatcher(updatingDevice)))).thenAnswer(invocation -> invocation.getArgument(0));
        givenDeviceExists(deviceId, updatingDevice);

        DeviceResponse result = deviceService.updateDevice(deviceId, request);

        assertNotNull(result);
        assertEquals(deviceId, existingDevice.getExternalId());
        assertEquals("New Name", result.name());
        assertEquals("New Brand", result.brand());
        assertEquals(DeviceState.IN_USE, result.state());

        verify(deviceRepository, times(1)).saveAndFlush(existingDevice);
        verify(deviceRepository, times(2)).findByExternalId(deviceId);
    }

    @Test
    void updatesStateWhenInUse() {
        UUID deviceId = UUID.randomUUID();
        Long version = 1L;

        Device existingDevice = device(deviceId, "Old Name", "Old Brand", IN_USE, version);

        DevicePatchRequest request = patch("Old Name", "Old Brand", INACTIVE, version);

        givenDeviceExists(deviceId, existingDevice);
        saveAndFlushReturnsArgument();

        DeviceResponse result = deviceService.updateDevice(deviceId, request);

        assertNotNull(result);
        assertEquals(deviceId, existingDevice.getExternalId());
        assertEquals("Old Name", result.name());
        assertEquals("Old Brand", result.brand());
        assertEquals(DeviceState.INACTIVE, result.state());

        verify(deviceRepository, times(1)).saveAndFlush(existingDevice);
    }

    @Test
    void rejectsMissingDeviceOnUpdate() {
        UUID deviceId = UUID.randomUUID();
        Long version = 1L;

        DevicePatchRequest request = patch(null, null, null, version);

        givenDeviceDoesNotExist(deviceId);

        assertThrows(DeviceNotFoundException.class, () -> deviceService.updateDevice(deviceId, request));

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void rejectsNameChangeWhenInUse() {
        UUID deviceId = UUID.randomUUID();
        Long version = 1L;

        Device existingDevice = device(deviceId, "Old Name", "Old Brand", IN_USE, version);

        DevicePatchRequest request = patch("Illegal Name Change", "Old Brand", IN_USE, version);

        givenDeviceExists(deviceId, existingDevice);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> deviceService.updateDevice(deviceId, request));

        assertEquals("Device 'name' or 'brand' cannot be updated while the device is in use.", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void rejectsBrandChangeWhenInUse() {
        UUID deviceId = UUID.randomUUID();
        Long version = 1L;

        Device existingDevice = device(deviceId, "Old Name", "Old Brand", IN_USE, version);
        DevicePatchRequest request = patch("Old Name", "Illegal Brand Change", IN_USE, version);

        givenDeviceExists(deviceId, existingDevice);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> deviceService.updateDevice(deviceId, request));

        assertEquals("Device 'name' or 'brand' cannot be updated while the device is in use.", exception.getMessage());
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    @DisplayName("When request version is older than DB version, but request data the same as in DB, should skip update and return current state (Idempotency)")
    void allowsIdenticalStaleRetry() {
        UUID deviceId = UUID.randomUUID();
        long databaseVersion = 5L;
        long staleClientVersion = 4L;

        Device existingDevice = device(deviceId, "Current Name", "Current Brand", AVAILABLE, databaseVersion);

        DevicePatchRequest request = patch("Current Name", "Current Brand", AVAILABLE, staleClientVersion);

        givenDeviceExists(deviceId, existingDevice);

        DeviceResponse result = deviceService.updateDevice(deviceId, request);

        assertNotNull(result);
        assertEquals(databaseVersion, result.version());
        assertEquals("Current Name", result.name()); // Verifies the domain state was untouched

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    @DisplayName("When request version is older than DB version, but request data not the same as in DB, should throw optimistic locking exception")
    void rejectsDifferentStalePayload() {
        UUID deviceId = UUID.randomUUID();
        long databaseVersion = 5L;
        long staleClientVersion = 4L;

        Device existingDevice = device(deviceId, "Current Name", "Current Brand", AVAILABLE, databaseVersion);

        DevicePatchRequest request = patch("Another Name", "Another Brand", AVAILABLE, staleClientVersion);

        givenDeviceExists(deviceId, existingDevice);

        OptimisticLockingFailureException exception = assertThrows(OptimisticLockingFailureException.class, () -> deviceService.updateDevice(deviceId, request));
        assertEquals("Object of class [com.example.device.model.Device] with identifier [" + deviceId + "]: optimistic locking failed", exception.getMessage());

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    @DisplayName("When request version is newer than DB version, should throw optimistic locking exception")
    void rejectsFutureVersion() {
        UUID deviceId = UUID.randomUUID();
        long databaseVersion = 5L;
        long futureClientVersion = 6L;

        Device existingDevice = device(deviceId, "Current Name", "Current Brand", AVAILABLE, databaseVersion);

        DevicePatchRequest request = patch("Another Name", "Current Brand", AVAILABLE, futureClientVersion);

        givenDeviceExists(deviceId, existingDevice);

        OptimisticLockingFailureException exception = assertThrows(OptimisticLockingFailureException.class, () -> deviceService.updateDevice(deviceId, request));
        assertEquals("Object of class [com.example.device.model.Device] with identifier [" + deviceId + "]: optimistic locking failed", exception.getMessage());

        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void deletesDevice() {
        UUID deviceId = UUID.randomUUID();
        Device existingDevice = device(deviceId, "Old Name", "Old Brand", INACTIVE);

        givenDeviceExists(deviceId, existingDevice);

        deviceService.deleteDevice(deviceId);

        verify(deviceRepository, times(1)).delete(existingDevice);
    }

    @Test
    void rejectsMissingDeviceOnDelete() {
        UUID deviceId = UUID.randomUUID();

        givenDeviceDoesNotExist(deviceId);

        DeviceNotFoundException exception = assertThrows(DeviceNotFoundException.class, () -> deviceService.deleteDevice(deviceId));

        assertEquals("Device not found with id: " + deviceId, exception.getMessage());

        verify(deviceRepository, never()).deleteById(anyLong());
    }

    @Test
    void rejectsDeletingInUseDevice() {
        UUID deviceId = UUID.randomUUID();

        Device existingDevice = device(deviceId, "Old Name", "Old Brand", IN_USE);

        givenDeviceExists(deviceId, existingDevice);

        BusinessRuleViolationException exception = assertThrows(BusinessRuleViolationException.class, () -> deviceService.deleteDevice(deviceId));

        assertEquals("Cannot delete device because it is currently in use.", exception.getMessage());

        verify(deviceRepository, never()).deleteById(anyLong());
    }

    @Test
    void returnsDeviceByExternalId() {
        UUID deviceId = UUID.randomUUID();

        Device existingDevice = device(deviceId, "Old Name", "Old Brand", INACTIVE);

        givenDeviceExists(deviceId, existingDevice);

        deviceService.findByExternalId(deviceId);

        verify(deviceRepository, times(1)).findByExternalId(deviceId);
    }

    @Test
    void rejectsMissingDeviceByExternalId() {
        UUID deviceId = UUID.randomUUID();
        givenDeviceDoesNotExist(deviceId);

        DeviceNotFoundException exception = assertThrows(DeviceNotFoundException.class, () -> deviceService.findByExternalId(deviceId));

        assertEquals("Device not found with id: " + deviceId, exception.getMessage());

        verify(deviceRepository, times(1)).findByExternalId(deviceId);
    }

    @Test
    void returnsDevicesByState() {
        UUID deviceId = UUID.randomUUID();

        PageRequest pageable = PageRequest.of(1, 50);
        Device existingDevice = device(deviceId, "Old Name", "Old Brand", IN_USE);
        DeviceResponse deviceResponse = response(existingDevice);
        List<DeviceResponse> expectedDevices = List.of(deviceResponse);

        when(deviceRepository.findByState(IN_USE, pageable)).thenReturn(new PageImpl<>(List.of(existingDevice)));

        Slice<DeviceResponse> devices = deviceService.findByState(IN_USE, pageable);

        assertEquals(new PageImpl<>(expectedDevices), devices);
        verify(deviceRepository, times(1)).findByState(IN_USE, pageable);
    }

    @Test
    void returnsEmptyStateResults() {
        PageRequest pageable = PageRequest.of(1, 50);
        when(deviceRepository.findByState(IN_USE, pageable)).thenReturn(Page.empty());

        Slice<DeviceResponse> devices = deviceService.findByState(IN_USE, pageable);
        assertEquals(Page.empty(), devices);

        verify(deviceRepository, times(1)).findByState(IN_USE, pageable);
    }

    @Test
    void returnsDevicesByBrand() {
        UUID deviceId = UUID.randomUUID();
        PageRequest pageable = PageRequest.of(1, 50);

        Device existingDevice = device(deviceId, "Old Name", "Old Brand", IN_USE);
        DeviceResponse deviceResponse = response(existingDevice);
        List<DeviceResponse> expectedDevices = List.of(deviceResponse);

        when(deviceRepository.findByBrand("Old Brand", pageable)).thenReturn(new PageImpl<>(List.of(existingDevice)));

        Slice<DeviceResponse> devices = deviceService.findByBrand("Old Brand", pageable);

        assertEquals(new PageImpl<>(expectedDevices), devices);
        verify(deviceRepository, times(1)).findByBrand("Old Brand", pageable);
    }

    @Test
    void returnsEmptyBrandResults() {
        PageRequest pageable = PageRequest.of(1, 50);
        when(deviceRepository.findByBrand("Old Brand", pageable)).thenReturn(Page.empty());

        Slice<DeviceResponse> devices = deviceService.findByBrand("Old Brand", pageable);
        assertEquals(Page.empty(), devices);

        verify(deviceRepository, times(1)).findByBrand("Old Brand", pageable);
    }

    @Test
    void returnsAllDevices() {
        UUID deviceId = UUID.randomUUID();

        PageRequest pageable = PageRequest.of(1, 50);
        Device existingDevice = device(deviceId, "Old Name", "Old Brand", IN_USE);
        DeviceResponse deviceResponse = response(existingDevice);
        List<DeviceResponse> expectedDevices = List.of(deviceResponse);

        when(deviceRepository.queryAllBy(pageable)).thenReturn(new PageImpl<>(List.of(existingDevice)));

        Slice<DeviceResponse> devices = deviceService.findAll(pageable);
        assertEquals(new PageImpl<>(expectedDevices), devices);

        verify(deviceRepository, times(1)).queryAllBy(pageable);
    }

    @Test
    void returnsEmptyDevicePage() {
        PageRequest pageable = PageRequest.of(1, 50);
        when(deviceRepository.queryAllBy(pageable)).thenReturn(Page.empty());

        Slice<DeviceResponse> page = deviceService.findAll(pageable);
        assertEquals(Page.empty(), page);

        verify(deviceRepository, times(1)).queryAllBy(pageable);
    }

    private void givenDeviceExists(UUID deviceId, Device device) {
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.of(device));
    }

    private void givenDeviceDoesNotExist(UUID deviceId) {
        when(deviceRepository.findByExternalId(deviceId)).thenReturn(Optional.empty());
    }

    private void saveAndFlushReturnsArgument() {
        when(deviceRepository.saveAndFlush(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static @NonNull Device device(UUID deviceId, final String name, final String brand, final DeviceState deviceState) {
        return device(deviceId, name, brand, deviceState, 1L);
    }

    private static @NonNull Device device(UUID deviceId, final String name, final String brand, final DeviceState deviceState, Long version) {
        Device device = new Device(name, brand, deviceState);
        ReflectionTestUtils.setField(device, "externalId", deviceId);
        if (version != null) {
            ReflectionTestUtils.setField(device, "version", version);
        }
        return device;
    }

    private static @NonNull DeviceResponse response(Device device) {
        return new DeviceResponse(device.getExternalId(), device.getName(), device.getBrand(), device.getState(), device.getVersion(), device.getCreationTime());
    }

    private static @NonNull DevicePatchRequest patch(String name, String brand, DeviceState state, Long version) {
        return new DevicePatchRequest(name, brand, state, version);
    }

    private static @NonNull DeviceCreateRequest createRequest(String name, String brand, DeviceState state) {
        return new DeviceCreateRequest(name, brand, state);
    }

    private record DeviceMatcher(Device left) implements ArgumentMatcher<Device> {

        @Override
            public boolean matches(Device right) {
                return left.getBrand().equals(right.getBrand()) &&
                        left.getName().equals(right.getName()) &&
                        left.getState().equals(right.getState()) &&
                        right.getExternalId() != null;
            }
        }
}
