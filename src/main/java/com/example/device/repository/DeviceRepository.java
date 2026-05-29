package com.example.device.repository;

import com.example.device.model.Device;
import com.example.device.model.DeviceState;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceRepository extends JpaRepository<Device, Long> {
    Slice<Device> queryAllBy(Pageable pageable);

    Slice<Device> findByBrand(String brand, Pageable pageable);

    Slice<Device> findByState(DeviceState state, Pageable pageable);

    Optional<Device> findByExternalId(UUID id);
}