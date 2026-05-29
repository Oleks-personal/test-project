package com.example.device.service.dto.mapper;

import com.example.device.model.Device;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DeviceResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceMapper {
    @Mapping(source = "externalId", target = "id")
    DeviceResponse deviceToDeviceResponse(Device device);
    Device deviceCreateRequestToDevice(DeviceCreateRequest deviceCreateRequest);
}
