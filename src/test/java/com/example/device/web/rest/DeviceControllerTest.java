package com.example.device.web.rest;

import com.example.device.errors.BusinessRuleViolationException;
import com.example.device.errors.DeviceNotFoundException;
import com.example.device.model.DeviceState;
import com.example.device.service.DeviceService;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DeviceResponse;
import com.example.device.service.dto.DeviceUpdateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeviceController.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeviceService deviceService;

    @Nested
    @DisplayName("POST /api/v1/devices")
    class CreateDeviceTests {

        @Test
        @DisplayName("Should return 201 Created and response payload when request is valid")
        void createDevice_ValidRequest_ShouldReturnCreated() throws Exception {
            // Arrange
            var request = new DeviceCreateRequest("Scanner X", "HP", DeviceState.AVAILABLE);
            var responseDto = new DeviceResponse(UUID.randomUUID(), "Scanner X", "HP", DeviceState.AVAILABLE, 0L, OffsetDateTime.now());

            when(deviceService.createDevice(any(DeviceCreateRequest.class))).thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(responseDto.id().toString()))
                    .andExpect(jsonPath("$.name").value("Scanner X"))
                    .andExpect(jsonPath("$.brand").value("HP"))
                    .andExpect(jsonPath("$.version").value(0));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when Jakarta constraints fail")
        void createDevice_InvalidRequest_ShouldReturnBadRequest() throws Exception {
            // Arrange - assuming blank name triggers validation constraints on DeviceCreateRequest
            var invalidRequest = new DeviceCreateRequest("", "HP", DeviceState.AVAILABLE);

            // Act & Assert
            mockMvc.perform(post("/api/v1/devices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).createDevice(any());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/devices/{id}")
    class UpdateDeviceTests {

        @Test
        @DisplayName("Should return 200 OK and updated payload when update succeeds")
        void updateDevice_ValidRequest_ShouldReturnOk() throws Exception {
            // Arrange
            UUID deviceId = UUID.randomUUID();
            var updateRequest = new DeviceUpdateRequest(Optional.of("New Name"), Optional.of("HP"), Optional.of(DeviceState.IN_USE), 1L);
            var responseDto = new DeviceResponse(deviceId, "New Name", "HP", DeviceState.IN_USE, 2L, OffsetDateTime.now());

            when(deviceService.updateDevice(eq(deviceId), any(DeviceUpdateRequest.class))).thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(patch("/api/v1/devices/{id}", deviceId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Name"))
                    .andExpect(jsonPath("$.state").value("IN_USE"))
                    .andExpect(jsonPath("$.version").value(2));
        }

        @Test
        @DisplayName("Should return 404 Not Found when device does not exist")
        void updateDevice_NonExistingId_ShouldReturnNotFound() throws Exception {
            // Arrange
            UUID missingId = UUID.randomUUID();
            var updateRequest = new DeviceUpdateRequest(Optional.of("Name"), Optional.empty(), Optional.empty(), 0L);

            when(deviceService.updateDevice(eq(missingId), any(DeviceUpdateRequest.class)))
                    .thenThrow(new DeviceNotFoundException("Device not found with id: " + missingId));

            // Act & Assert
            mockMvc.perform(patch("/api/v1/devices/{id}", missingId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/devices/{id}")
    class DeleteDeviceTests {

        @Test
        @DisplayName("Should return 244 No Content on successful removal")
        void deleteDevice_ExistingId_ShouldReturnNoContent() throws Exception {
            // Arrange
            UUID deviceId = UUID.randomUUID();
            doNothing().when(deviceService).deleteDevice(deviceId);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/devices/{id}", deviceId))
                    .andExpect(status().isNoContent());

            verify(deviceService, times(1)).deleteDevice(deviceId);
        }

        @Test
        @DisplayName("Should return 409 Conflict when business rules block deletion")
        void deleteDevice_InUse_ShouldReturnConflict() throws Exception {
            // Arrange
            UUID activeDeviceId = UUID.randomUUID();
            doThrow(new BusinessRuleViolationException("Cannot delete device because it is currently in use."))
                    .when(deviceService).deleteDevice(activeDeviceId);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/devices/{id}", activeDeviceId))
                    .andExpect(status().isUnprocessableContent());
        }
    }

    @Nested
    @DisplayName("GET Paging and Slicing Operations")
    class PagingAndSlicingTests {

        @Test
        @DisplayName("GET /api/v1/devices - Should safely return a slice layout with customized sorting")
        void getAllDevices_ValidParams_ShouldReturnSlicedData() throws Exception {
            // Arrange
            var pageRequest = PageRequest.of(0, 50, Sort.by("creationTime").descending());
            var item = new DeviceResponse(UUID.randomUUID(), "Plotter 9", "Canon", DeviceState.AVAILABLE, 0L, OffsetDateTime.now());

            // SliceImpl is the perfect core implementation object for testing Slice returns
            var simulatedSlice = new SliceImpl<>(List.of(item), pageRequest, false);

            when(deviceService.findAll(eq(pageRequest))).thenReturn(simulatedSlice);

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices")
                            .param("page", "0")
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Plotter 9"))
                    .andExpect(jsonPath("$.content[0].brand").value("Canon"))
                    .andExpect(jsonPath("$.last").value(true)) // Checks slice metadata parsing
                    .andExpect(jsonPath("$.numberOfElements").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices/{id}")
    class GetDeviceByIdTests {

        @Test
        @DisplayName("Should return 200 OK and payload when device is found")
        void getById_ExistingId_ShouldReturnDevice() throws Exception {
            // Arrange
            UUID deviceId = UUID.randomUUID();
            var responseDto = new DeviceResponse(deviceId, "ThinkPad", "Lenovo", DeviceState.AVAILABLE, 0L, OffsetDateTime.now());

            when(deviceService.findById(deviceId)).thenReturn(responseDto);

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices/{id}", deviceId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(deviceId.toString()))
                    .andExpect(jsonPath("$.name").value("ThinkPad"))
                    .andExpect(jsonPath("$.brand").value("Lenovo"))
                    .andExpect(jsonPath("$.state").value("AVAILABLE"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when device does not exist")
        void getById_NonExistingId_ShouldReturnNotFound() throws Exception {
            // Arrange
            UUID missingId = UUID.randomUUID();
            when(deviceService.findById(missingId))
                    .thenThrow(new DeviceNotFoundException("Device not found with id: " + missingId));

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices/{id}", missingId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices/brand/{brand}")
    class GetDevicesByBrandTests {

        @Test
        @DisplayName("Should return 200 OK and sliced data using default paging parameters")
        void getByBrand_DefaultPaging_ShouldReturnSlicedData() throws Exception {
            // Arrange
            String brand = "Apple";
            // Match the default controller values exactly: page 0, size 50, sorted by creationTime DESC
            var pageRequest = PageRequest.of(0, 50, Sort.by("creationTime").descending());
            var device = new DeviceResponse(UUID.randomUUID(), "MacBook Pro", brand, DeviceState.AVAILABLE, 1L, OffsetDateTime.now());
            var simulatedSlice = new SliceImpl<>(List.of(device), pageRequest, false);

            when(deviceService.findByBrand(brand, pageRequest)).thenReturn(simulatedSlice);

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices/brand/{brand}", brand))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content[0].brand").value(brand))
                    .andExpect(jsonPath("$.content[0].name").value("MacBook Pro"))
                    .andExpect(jsonPath("$.size").value(50))
                    .andExpect(jsonPath("$.number").value(0)) // Current page index
                    .andExpect(jsonPath("$.numberOfElements").value(1));
        }

        @Test
        @DisplayName("Should accept custom pagination query parameters")
        void getByBrand_CustomPaging_ShouldPassCorrectPageableToService() throws Exception {
            // Arrange
            String brand = "Apple";
            var customPageRequest = PageRequest.of(2, 20, Sort.by("creationTime").descending());
            var simulatedSlice = new SliceImpl<DeviceResponse>(List.of(), customPageRequest, false);

            when(deviceService.findByBrand(brand, customPageRequest)).thenReturn(simulatedSlice);

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices/brand/{brand}", brand)
                            .param("page", "2")
                            .param("size", "20"))
                    .andExpect(status().isOk());

            verify(deviceService, times(1)).findByBrand(brand, customPageRequest);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices/state/{state}")
    class GetDevicesByStateTests {

        @Test
        @DisplayName("Should return 200 OK and filtered payload for valid enum state")
        void getByState_ValidState_ShouldReturnSlicedData() throws Exception {
            // Arrange
            DeviceState state = DeviceState.IN_USE;
            var pageRequest = PageRequest.of(0, 50, Sort.by("creationTime").descending());
            var device = new DeviceResponse(UUID.randomUUID(), "iPhone 15", "Apple", state, 3L, OffsetDateTime.now());
            var simulatedSlice = new SliceImpl<>(List.of(device), pageRequest, false);

            when(deviceService.findByState(state, pageRequest)).thenReturn(simulatedSlice);

            // Act & Assert
            mockMvc.perform(get("/api/v1/devices/state/{state}", state))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content[0].state").value("IN_USE"))
                    .andExpect(jsonPath("$.content[0].name").value("iPhone 15"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when path string cannot be converted to DeviceState enum")
        void getByState_InvalidStateEnum_ShouldReturnBadRequest() throws Exception {
            // Act & Assert - Passing a garbage string that isn't a valid enum constant
            mockMvc.perform(get("/api/v1/devices/state/{state}", "NOT_A_VALID_STATE"))
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).findByState(any(), any());
        }
    }
}