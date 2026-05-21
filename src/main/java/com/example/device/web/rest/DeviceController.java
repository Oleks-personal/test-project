package com.example.device.web.rest;

import com.example.device.model.DeviceState;
import com.example.device.service.DeviceService;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DeviceResponse;
import com.example.device.service.dto.DeviceUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Device Management", description = "Endpoints for managing the lifecycle, states, and allocation of devices")
public class DeviceController {
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    @Operation(
            summary = "Create a new device",
            description = "Registers a brand new device entity into the catalog. Initial state defaults to AVAILABLE if not specified.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Device created successfully",
                            content = @Content(schema = @Schema(implementation = DeviceResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Invalid request payload or failed property validation constraints")
            }
    )
    public ResponseEntity<DeviceResponse> createDevice(
            @RequestBody @Valid DeviceCreateRequest createRequest) {

        DeviceResponse createdDevice = deviceService.createDevice(createRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDevice);
    }

    //version is required
    @PatchMapping("/{id}")
    @Operation(
            summary = "Partially update an existing device",
            description = "Updates specific properties of an existing device. Throws a validation block if attempting to change name or brand while state is IN_USE.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Device updated successfully"),
                    @ApiResponse(responseCode = "404", description = "Device not found with the provided UUID"),
                    @ApiResponse(responseCode = "409", description = "Business rule violation error triggered (e.g., modifying active asset properties)")
            }
    )
    public ResponseEntity<DeviceResponse> updateDevice(
            @PathVariable @Parameter(description = "Unique identifier of the target device") UUID id,
            @RequestBody DeviceUpdateRequest updateRequest) {

        DeviceResponse updatedDevice = deviceService.updateDevice(id, updateRequest);
        return ResponseEntity.ok(updatedDevice);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a device entry",
            description = "Removes a device permanently from storage. Only devices that are NOT in an 'IN_USE' status state can be safely purged.",
            responses = {
                    @ApiResponse(responseCode = "244", description = "Device removed successfully"),
                    @ApiResponse(responseCode = "409", description = "Cannot delete a device that is currently marked as IN_USE")
            }
    )
    public ResponseEntity<Void> deleteDevice(@PathVariable UUID id) {
        deviceService.deleteDevice(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find device by ID", description = "Retrieves structural payload details for a single target device.")
    public ResponseEntity<DeviceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(deviceService.findById(id));
    }


    @GetMapping("/brand/{brand}")
    @Operation(summary = "Get devices filtered by Brand", description = "Returns a continuous fast scroll data Slice of devices belonging to a target brand string matching creation timestamps descending.")
    public ResponseEntity<Slice<DeviceResponse>> getByBrand(
            @PathVariable String brand,
            @RequestParam(defaultValue = "0") @Parameter(description = "Zero-indexed page destination") int page,
            @RequestParam(defaultValue = "50") @Parameter(description = "Total element layout capacity bounds per slice request slice limit") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("creationTime").descending());
        return ResponseEntity.ok(deviceService.findByBrand(brand, pageRequest));
    }

    @GetMapping("/state/{state}")
    @Operation(summary = "Get devices filtered by State", description = "Returns a high-speed slice view layout containing all active elements matching the defined enum state criteria.")
    public ResponseEntity<Slice<DeviceResponse>> getByState(
            @PathVariable DeviceState state,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("creationTime").descending());
        return ResponseEntity.ok(deviceService.findByState(state, pageRequest));
    }

    @GetMapping
    @Operation(summary = "Fetch all system devices", description = "Returns a master global slice lookup of all logged system devices sorted chronologically by default.")
    public ResponseEntity<Slice<DeviceResponse>> getAllDevices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("creationTime").descending());
        Slice<DeviceResponse> devicesPage = deviceService.findAll(pageRequest);

        return ResponseEntity.ok(devicesPage);
    }
}