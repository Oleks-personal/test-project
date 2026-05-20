package com.example.device.repository;

import com.example.device.model.Device;
import com.example.device.model.DeviceState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findByBrand(String brand);

    List<Device> findByState(DeviceState state);
}