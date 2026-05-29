package com.example.device.service.internal;

import com.example.device.errors.BusinessRuleViolationException;
import com.example.device.errors.DeviceNotFoundException;
import com.example.device.model.Device;
import com.example.device.model.DeviceState;
import com.example.device.repository.DeviceRepository;
import com.example.device.service.DeviceService;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DevicePatchRequest;
import com.example.device.service.dto.DeviceResponse;
import com.example.device.service.dto.mapper.DeviceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.Optional;
import java.util.UUID;

@Transactional
@Service
class DeviceServiceImpl implements DeviceService {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceServiceImpl.class);

    private static final String DEVICE_DATA_INTEGRITY_MESSAGE = "Device data violates persistence constraints.";

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;

    DeviceServiceImpl(DeviceRepository deviceRepository, DeviceMapper deviceMapper) {
        this.deviceRepository = deviceRepository;
        this.deviceMapper = deviceMapper;
    }

    @Override
    public DeviceResponse createDevice(DeviceCreateRequest deviceCreateRequest) {
        Device newDevice = deviceMapper.deviceCreateRequestToDevice(deviceCreateRequest);
        Device device;
        try {
            device = deviceRepository.save(newDevice);
        } catch (DataIntegrityViolationException e) {
            LOG.error("Failed to create device: {}", DEVICE_DATA_INTEGRITY_MESSAGE, e);
            throw new BusinessRuleViolationException(DEVICE_DATA_INTEGRITY_MESSAGE, e);
        }
        return transformToDto(device);
    }

    @Override
    public DeviceResponse updateDevice(UUID id, DevicePatchRequest updateRequest) {
        Device existingDevice = deviceRepository.findByExternalId(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

        if (!ObjectUtils.nullSafeEquals(existingDevice.getVersion(), updateRequest.version())) {
            if (isStaleIdempotentRetry(existingDevice, updateRequest)) {
                return transformToDto(existingDevice);
            }
            LOG.warn("Optimistic lock failure: Device {} has version {} in database, but update request specified version {}", id, existingDevice.getVersion(), updateRequest.version());
            throw new ObjectOptimisticLockingFailureException(Device.class, id);
        }

        String targetName = Optional.ofNullable(updateRequest.name()).orElse(existingDevice.getName());
        String targetBrand = Optional.ofNullable(updateRequest.brand()).orElse(existingDevice.getBrand());
        DeviceState targetState = Optional.ofNullable(updateRequest.state()).orElse(existingDevice.getState());

        existingDevice.updateDetails(targetName, targetBrand, targetState);

        Device device;
        try {
            device = deviceRepository.saveAndFlush(existingDevice);
        } catch (DataIntegrityViolationException e) {
            LOG.error("Failed to update device {}: {}", id, DEVICE_DATA_INTEGRITY_MESSAGE, e);
            throw new BusinessRuleViolationException(DEVICE_DATA_INTEGRITY_MESSAGE, e);
        }
        return transformToDto(device);
    }

    @Override
    public void deleteDevice(UUID id) {
        Device device = deviceRepository.findByExternalId(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

        if (!device.canBeDeleted()) {
            LOG.warn("Cannot delete device {} because it is in an invalid state for deletion: {}", id, device.getState());
            throw new BusinessRuleViolationException("Cannot delete device because it is currently in use.");
        }

        deviceRepository.delete(device);
    }

    @Transactional(readOnly = true)
    @Override
    public DeviceResponse findByExternalId(UUID id) {
        Device device = deviceRepository.findByExternalId(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));
        return transformToDto(device);
    }

    @Transactional(readOnly = true)
    @Override
    public Slice<DeviceResponse> findByBrand(String brand, Pageable pageRequest) {
        Slice<Device> devices = deviceRepository.findByBrand(brand, pageRequest);

        return devices.map(this::transformToDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Slice<DeviceResponse> findByState(DeviceState state, Pageable pageRequest) {
        Slice<Device> devices = deviceRepository.findByState(state, pageRequest);
        return devices.map(this::transformToDto);
    }

    @Transactional(readOnly = true)
    @Override
    public Slice<DeviceResponse> findAll(Pageable pageRequest) {
        Slice<Device> devices = deviceRepository.queryAllBy(pageRequest);
        return devices.map(this::transformToDto);
    }

    private DeviceResponse transformToDto(Device device) {
        return deviceMapper.deviceToDeviceResponse(device);
    }

    private boolean isStaleIdempotentRetry(Device existingDevice, DevicePatchRequest updateRequest) {
        return existingDevice.getVersion() != null
                && existingDevice.getVersion() > updateRequest.version()
                && (updateRequest.name() == null || ObjectUtils.nullSafeEquals(updateRequest.name(), existingDevice.getName()))
                && (updateRequest.brand() == null || ObjectUtils.nullSafeEquals(updateRequest.brand(), existingDevice.getBrand()))
                && (updateRequest.state() == null || ObjectUtils.nullSafeEquals(updateRequest.state(), existingDevice.getState()));
    }
}
