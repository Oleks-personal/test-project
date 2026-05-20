package com.example.device.service.internal;

import com.example.device.errors.BusinessRuleViolationException;
import com.example.device.errors.DeviceNotFoundException;
import com.example.device.model.Device;
import com.example.device.model.DeviceState;
import com.example.device.repository.DeviceRepository;
import com.example.device.service.DeviceService;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DeviceUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Transactional
@Service
class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    DeviceServiceImpl(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public Device createDevice(DeviceCreateRequest deviceCreateRequest) {
        Device newDevice = new Device(deviceCreateRequest.name(), deviceCreateRequest.brand(), deviceCreateRequest.state());
        return deviceRepository.save(newDevice);
    }

    @Override
    public Device updateDevice(UUID id, DeviceUpdateRequest updateRequest) {
        Device existingDevice = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

        String targetName = updateRequest.resolveName(existingDevice);
        String targetBrand = updateRequest.resolveBrand(existingDevice);
        DeviceState targetState = updateRequest.resolveState(existingDevice);

        existingDevice.updateDetails(targetName, targetBrand, targetState);

        return deviceRepository.save(existingDevice);
    }

    @Override
    public void deleteDevice(UUID id) {
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));

        if (!device.canBeDeleted()) {
            throw new BusinessRuleViolationException("Cannot delete device because it is currently in use.");
        }

        deviceRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    @Override
    public Device findById(UUID id) {
        return deviceRepository.findById(id).orElseThrow(() -> new DeviceNotFoundException("Device not found with id: " + id));
    }

    @Transactional(readOnly = true)
    @Override
    public List<Device> findByBrand(String brand) {
        return deviceRepository.findByBrand(brand);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Device> findByState(DeviceState state) {
        return deviceRepository.findByState(state);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<Device> findAll(PageRequest pageRequest) {
        return deviceRepository.findAll(pageRequest);
    }
}