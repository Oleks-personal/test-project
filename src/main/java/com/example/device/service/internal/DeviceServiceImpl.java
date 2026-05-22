package com.example.device.service.internal;

import com.example.device.errors.BusinessRuleViolationException;
import com.example.device.errors.DeviceNotFoundException;
import com.example.device.model.Device;
import com.example.device.model.DeviceState;
import com.example.device.repository.DeviceRepository;
import com.example.device.service.DeviceService;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DeviceResponse;
import com.example.device.service.dto.DeviceUpdateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.UUID;

@Transactional
@Service
class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceFactory deviceFactory;

    DeviceServiceImpl(DeviceRepository deviceRepository, DeviceFactory deviceFactory) {
        this.deviceRepository = deviceRepository;
        this.deviceFactory = deviceFactory;
    }

    @Override
    public DeviceResponse createDevice(DeviceCreateRequest deviceCreateRequest) {
        Device newDevice = deviceFactory.createDevice(deviceCreateRequest);
        Device device = deviceRepository.save(newDevice);
        return transformToDto(device);
    }

    @Override
    public DeviceResponse updateDevice(UUID id, DeviceUpdateRequest updateRequest) {
        Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

        if (existingDevice.getVersion() != null && existingDevice.getVersion() > updateRequest.version()) {
            boolean isIdempotentRetry = updateRequest.name().map(name -> ObjectUtils.nullSafeEquals(existingDevice.getName(), name)).orElse(true)
                    && updateRequest.brand().map(brand -> ObjectUtils.nullSafeEquals(existingDevice.getBrand(), brand)).orElse(true)
                    && updateRequest.state().map(state -> ObjectUtils.nullSafeEquals(existingDevice.getState(), state)).orElse(true);

            if (isIdempotentRetry) {
                return transformToDto(existingDevice);
            }

            throw new ObjectOptimisticLockingFailureException(Device.class, id);
        }

        String targetName = updateRequest.resolveName(existingDevice);
        String targetBrand = updateRequest.resolveBrand(existingDevice);
        DeviceState targetState = updateRequest.resolveState(existingDevice);
        existingDevice.updateDetails(targetName, targetBrand, targetState);

        Device device = deviceRepository.saveAndFlush(existingDevice);
        return transformToDto(device);
    }

    @Override
    public void deleteDevice(UUID id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

        if (!device.canBeDeleted()) {
            throw new BusinessRuleViolationException("Cannot delete device because it is currently in use.");
        }

        deviceRepository.delete(device);
    }

    @Transactional(readOnly = true)
    @Override
    public DeviceResponse findById(UUID id) {
        Device device = deviceRepository.findById(id)
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
        return new DeviceResponse(
                device.getId(),
                device.getName(),
                device.getBrand(),
                device.getState(),
                device.getVersion(),
                device.getCreationTime());
    }
}