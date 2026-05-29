package com.example.device.web.rest;

import com.example.device.errors.BusinessRuleViolationException;
import com.example.device.errors.DeviceNotFoundException;
import com.example.device.model.DeviceState;
import com.example.device.service.DeviceService;
import com.example.device.service.dto.DeviceCreateRequest;
import com.example.device.service.dto.DevicePatchRequest;
import com.example.device.service.dto.DeviceResponse;
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
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.example.device.model.DeviceState.AVAILABLE;
import static com.example.device.model.DeviceState.IN_USE;
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
        void createsDevice() throws Exception {
            var request = createRequest("Scanner X", "HP", AVAILABLE);
            var responseDto = response("Scanner X", "HP", AVAILABLE);

            when(deviceService.createDevice(any(DeviceCreateRequest.class))).thenReturn(responseDto);

            postDevice(request)
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(responseDto.id().toString()))
                    .andExpect(jsonPath("$.name").value("Scanner X"))
                    .andExpect(jsonPath("$.brand").value("HP"))
                    .andExpect(jsonPath("$.version").value(0));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when Jakarta constraints fail")
        void rejectsInvalidCreateRequest() throws Exception {
            var invalidRequest = createRequest("", "HP", AVAILABLE);

            postDevice(invalidRequest)
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).createDevice(any());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/devices/{id}")
    class UpdateDeviceTests {

        @Test
        @DisplayName("Should return 200 OK and updated payload when update succeeds")
        void updatesDevice() throws Exception {
            UUID deviceId = UUID.randomUUID();
            var updateRequest = patchRequest("New Name", "HP", IN_USE, 1L);
            var responseDto = response(deviceId, "New Name", "HP", IN_USE, 2L);

            when(deviceService.updateDevice(eq(deviceId), any(DevicePatchRequest.class))).thenReturn(responseDto);

            patchDevice(deviceId, updateRequest)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("New Name"))
                    .andExpect(jsonPath("$.state").value("IN_USE"))
                    .andExpect(jsonPath("$.version").value(2));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when version is omitted")
        void rejectsMissingVersion() throws Exception {
            UUID deviceId = UUID.randomUUID();
            String requestWithoutVersion = """
                    {
                      "name": "New Name"
                    }
                    """;

            patchDevice(deviceId, requestWithoutVersion)
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).updateDevice(any(), any());
        }

        @Test
        @DisplayName("Should return 404 Not Found when device does not exist")
        void returnsNotFoundForMissingDeviceOnUpdate() throws Exception {
            UUID missingId = UUID.randomUUID();
            var updateRequest = patchRequest("Name", null, null, 0L);

            when(deviceService.updateDevice(eq(missingId), any(DevicePatchRequest.class)))
                    .thenThrow(new DeviceNotFoundException("Device not found with id: " + missingId));

            patchDevice(missingId, updateRequest)
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/devices/{id}")
    class DeleteDeviceTests {

        @Test
        @DisplayName("Should return 244 No Content on successful removal")
        void deletesDevice() throws Exception {
            UUID deviceId = UUID.randomUUID();
            doNothing().when(deviceService).deleteDevice(deviceId);

            deleteDevice(deviceId)
                    .andExpect(status().isNoContent());

            verify(deviceService, times(1)).deleteDevice(deviceId);
        }

        @Test
        @DisplayName("Should return 409 Conflict when business rules block deletion")
        void rejectsDeletingInUseDevice() throws Exception {
            UUID activeDeviceId = UUID.randomUUID();
            doThrow(new BusinessRuleViolationException("Cannot delete device because it is currently in use."))
                    .when(deviceService).deleteDevice(activeDeviceId);

            deleteDevice(activeDeviceId)
                    .andExpect(status().isUnprocessableContent());
        }
    }

    @Nested
    @DisplayName("GET Paging and Slicing Operations")
    class PagingAndSlicingTests {

        @Test
        @DisplayName("GET /api/v1/devices - Should safely return a slice layout with customized sorting")
        void returnsAllDevicesSlice() throws Exception {
            var pageRequest = PageRequest.of(0, 50, Sort.by("creationTime").descending());
            var item = response("Plotter 9", "Canon", AVAILABLE);
            var simulatedSlice = sliceOf(pageRequest, item);

            when(deviceService.findAll(eq(pageRequest))).thenReturn(simulatedSlice);

            getDevices(0, 50)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].name").value("Plotter 9"))
                    .andExpect(jsonPath("$.content[0].brand").value("Canon"))
                    .andExpect(jsonPath("$.last").value(true))
                    .andExpect(jsonPath("$.numberOfElements").value(1));
        }

        @Test
        @DisplayName("GET /api/v1/devices - Should return 400 Bad Request when page is negative")
        void rejectsNegativePage() throws Exception {
            getDevices(-1, 50)
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).findAll(any());
        }

        @Test
        @DisplayName("GET /api/v1/devices - Should return 400 Bad Request when size is less than one")
        void rejectsSizeLessThanOne() throws Exception {
            getDevices(0, 0)
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).findAll(any());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices/{id}")
    class GetDeviceByIdTests {

        @Test
        @DisplayName("Should return 200 OK and payload when device is found")
        void returnsDeviceById() throws Exception {
            UUID deviceId = UUID.randomUUID();
            var responseDto = response(deviceId, "ThinkPad", "Lenovo", AVAILABLE, 0L);

            when(deviceService.findByExternalId(deviceId)).thenReturn(responseDto);

            getDevice(deviceId)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(deviceId.toString()))
                    .andExpect(jsonPath("$.name").value("ThinkPad"))
                    .andExpect(jsonPath("$.brand").value("Lenovo"))
                    .andExpect(jsonPath("$.state").value("AVAILABLE"));
        }

        @Test
        @DisplayName("Should return 404 Not Found when device does not exist")
        void returnsNotFoundForMissingDevice() throws Exception {
            UUID missingId = UUID.randomUUID();
            when(deviceService.findByExternalId(missingId))
                    .thenThrow(new DeviceNotFoundException("Device not found with id: " + missingId));

            getDevice(missingId)
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices/brand/{brand}")
    class GetDevicesByBrandTests {

        @Test
        @DisplayName("Should return 200 OK and sliced data using default paging parameters")
        void returnsDevicesByBrandWithDefaultPaging() throws Exception {
            String brand = "Apple";
            var pageRequest = PageRequest.of(0, 50, Sort.by("creationTime").descending());
            var device = response("MacBook Pro", brand, AVAILABLE, 1L);
            var simulatedSlice = sliceOf(pageRequest, device);

            when(deviceService.findByBrand(brand, pageRequest)).thenReturn(simulatedSlice);

            getDevicesByBrand(brand)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content[0].brand").value(brand))
                    .andExpect(jsonPath("$.content[0].name").value("MacBook Pro"))
                    .andExpect(jsonPath("$.size").value(50))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.numberOfElements").value(1));
        }

        @Test
        @DisplayName("Should accept custom pagination query parameters")
        void passesCustomPagingForBrand() throws Exception {
            String brand = "Apple";
            var customPageRequest = PageRequest.of(2, 20, Sort.by("creationTime").descending());
            var simulatedSlice = new SliceImpl<DeviceResponse>(List.of(), customPageRequest, false);

            when(deviceService.findByBrand(brand, customPageRequest)).thenReturn(simulatedSlice);

            getDevicesByBrand(brand, 2, 20)
                    .andExpect(status().isOk());

            verify(deviceService, times(1)).findByBrand(brand, customPageRequest);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/devices/state/{state}")
    class GetDevicesByStateTests {

        @Test
        @DisplayName("Should return 200 OK and filtered payload for valid enum state")
        void returnsDevicesByState() throws Exception {
            DeviceState state = IN_USE;
            var pageRequest = PageRequest.of(0, 50, Sort.by("creationTime").descending());
            var device = response("iPhone 15", "Apple", state, 3L);
            var simulatedSlice = sliceOf(pageRequest, device);

            when(deviceService.findByState(state, pageRequest)).thenReturn(simulatedSlice);

            getDevicesByState(state)
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content[0].state").value("IN_USE"))
                    .andExpect(jsonPath("$.content[0].name").value("iPhone 15"));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when path string cannot be converted to DeviceState enum")
        void rejectsInvalidState() throws Exception {
            getDevicesByState("NOT_A_VALID_STATE")
                    .andExpect(status().isBadRequest());

            verify(deviceService, never()).findByState(any(), any());
        }
    }

    private ResultActions postDevice(DeviceCreateRequest request) throws Exception {
        return mockMvc.perform(post("/api/v1/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private ResultActions patchDevice(UUID id, DevicePatchRequest request) throws Exception {
        return patchDevice(id, objectMapper.writeValueAsString(request));
    }

    private ResultActions patchDevice(UUID id, String jsonRequest) throws Exception {
        return mockMvc.perform(patch("/api/v1/devices/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest));
    }

    private ResultActions deleteDevice(UUID id) throws Exception {
        return mockMvc.perform(delete("/api/v1/devices/{id}", id));
    }

    private ResultActions getDevice(UUID id) throws Exception {
        return mockMvc.perform(get("/api/v1/devices/{id}", id));
    }

    private ResultActions getDevices(int page, int size) throws Exception {
        return mockMvc.perform(get("/api/v1/devices")
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)));
    }

    private ResultActions getDevicesByBrand(String brand) throws Exception {
        return mockMvc.perform(get("/api/v1/devices/brand/{brand}", brand));
    }

    private ResultActions getDevicesByBrand(String brand, int page, int size) throws Exception {
        return mockMvc.perform(get("/api/v1/devices/brand/{brand}", brand)
                .param("page", String.valueOf(page))
                .param("size", String.valueOf(size)));
    }

    private ResultActions getDevicesByState(DeviceState state) throws Exception {
        return getDevicesByState(state.name());
    }

    private ResultActions getDevicesByState(String state) throws Exception {
        return mockMvc.perform(get("/api/v1/devices/state/{state}", state));
    }

    private DeviceCreateRequest createRequest(String name, String brand, DeviceState state) {
        return new DeviceCreateRequest(name, brand, state);
    }

    private DevicePatchRequest patchRequest(String name, String brand, DeviceState state, long version) {
        return new DevicePatchRequest(name, brand, state, version);
    }

    private DeviceResponse response(String name, String brand, DeviceState state) {
        return response(UUID.randomUUID(), name, brand, state, 0L);
    }

    private DeviceResponse response(String name, String brand, DeviceState state, long version) {
        return response(UUID.randomUUID(), name, brand, state, version);
    }

    private DeviceResponse response(UUID id, String name, String brand, DeviceState state, long version) {
        return new DeviceResponse(id, name, brand, state, version, OffsetDateTime.now());
    }

    private SliceImpl<DeviceResponse> sliceOf(PageRequest pageRequest, DeviceResponse item) {
        return new SliceImpl<>(List.of(item), pageRequest, false);
    }
}
