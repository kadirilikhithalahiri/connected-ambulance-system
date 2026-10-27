package com.abc.emergency.web;

import com.abc.emergency.config.WebSecurityConfig;
import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.dto.AmbulanceLocationUpdateRequest;
import com.abc.emergency.model.Ambulance;
import com.abc.emergency.model.User;
import com.abc.emergency.service.AmbulanceService;
import com.abc.emergency.service.ServiceResult;
import com.abc.emergency.util.ValidationUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AmbulanceController.class, GlobalExceptionHandler.class})
@Import({WebSecurityConfig.class, AmbulanceControllerTest.TestConfig.class})
public class AmbulanceControllerTest {

    static class FakeAmbulanceService extends AmbulanceService {
        final Map<Integer, Ambulance> store = new LinkedHashMap<>();
        private boolean simulateDbError = false;

        public FakeAmbulanceService() {
            reset();
        }

        public void reset() {
            store.clear();
            Ambulance amb1 = new Ambulance(1, "KA-01-AB-1234", 101, 201, 12.9716, 77.5946, "Defibrillator, Oxygen", "AVAILABLE");
            Ambulance amb2 = new Ambulance(2, "KA-02-CD-5678", 102, 202, 12.9352, 77.6245, "Ventilator, ECG", "DISPATCHED");
            Ambulance amb3 = new Ambulance(3, "KA-03-EF-9012", 0, 203, 12.9279, 77.6271, "Basic Life Support", "AVAILABLE");
            store.put(1, amb1);
            store.put(2, amb2);
            store.put(3, amb3);
            simulateDbError = false;
        }

        public void setSimulateDbError(boolean simulate) {
            this.simulateDbError = simulate;
        }

        @Override
        public List<Ambulance> getAllAmbulances() {
            return new ArrayList<>(store.values());
        }

        @Override
        public Ambulance getAmbulanceById(int ambulanceId) {
            return store.get(ambulanceId);
        }

        @Override
        public ServiceResult<Ambulance> getAmbulanceForDriver(int userId) {
            if (userId == 101) {
                return ServiceResult.success("Assigned ambulance retrieved.", store.get(1));
            }
            if (userId == 102) {
                return ServiceResult.success("Assigned ambulance retrieved.", store.get(2));
            }
            return ServiceResult.notFound("No ambulance assigned to driver ID " + userId);
        }

        @Override
        public List<Ambulance> getAllAmbulancesForCaller(int userId, String role) {
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(role)) {
                if (userId == 101 && store.containsKey(1)) {
                    return List.of(store.get(1));
                }
                if (userId == 102 && store.containsKey(2)) {
                    return List.of(store.get(2));
                }
                return Collections.emptyList();
            }
            if ("HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
                return Collections.emptyList();
            }
            return new ArrayList<>(store.values());
        }

        @Override
        public ServiceResult<Ambulance> getAmbulanceByIdForCaller(int ambulanceId, int userId, String role) {
            if (!ValidationUtil.isValidId(ambulanceId)) {
                return ServiceResult.validationError("Invalid ambulance ID. ID must be greater than 0.");
            }
            Ambulance amb = store.get(ambulanceId);
            if (amb == null) {
                return ServiceResult.notFound("Ambulance not found with ID: " + ambulanceId);
            }
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(role)) {
                if (userId == 101 && ambulanceId != 1) {
                    return ServiceResult.notFound("Ambulance not found with ID: " + ambulanceId);
                }
                if (userId == 102 && ambulanceId != 2) {
                    return ServiceResult.notFound("Ambulance not found with ID: " + ambulanceId);
                }
            }
            if ("HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
                return ServiceResult.notFound("Ambulance not found with ID: " + ambulanceId);
            }
            return ServiceResult.success("Ambulance retrieved successfully.", amb);
        }

        @Override
        public ServiceResult<Ambulance> updateAmbulanceLocationResult(int ambulanceId, Double latitude, Double longitude, Double speed, int callerUserId, String callerRole) {
            if (!ValidationUtil.isValidId(ambulanceId)) {
                return ServiceResult.validationError("Invalid ambulance ID. ID must be greater than 0.");
            }
            if (latitude == null || !Double.isFinite(latitude) || latitude < -90.0 || latitude > 90.0) {
                return ServiceResult.validationError("Invalid latitude. Must be a finite number between -90.0 and 90.0.");
            }
            if (longitude == null || !Double.isFinite(longitude) || longitude < -180.0 || longitude > 180.0) {
                return ServiceResult.validationError("Invalid longitude. Must be a finite number between -180.0 and 180.0.");
            }
            if (speed != null && (!Double.isFinite(speed) || speed < 0.0 || speed > 250.0)) {
                return ServiceResult.validationError("Invalid speed. Must be a finite number between 0.0 and 250.0 km/h.");
            }
            if (simulateDbError) {
                return ServiceResult.databaseError("DATABASE_ERROR: Connection failed.");
            }
            Ambulance amb = store.get(ambulanceId);
            if (amb == null) {
                return ServiceResult.notFound("Ambulance not found with ID: " + ambulanceId);
            }
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(callerRole)) {
                if (callerUserId == 101 && ambulanceId != 1) {
                    return ServiceResult.notFound("Ambulance not found with ID: " + ambulanceId);
                }
                if (callerUserId == 102 && ambulanceId != 2) {
                    return ServiceResult.notFound("Ambulance not found with ID: " + ambulanceId);
                }
            }
            amb.setGpsLatitude(latitude);
            amb.setGpsLongitude(longitude);
            return ServiceResult.success("Ambulance location updated successfully.", amb);
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public AmbulanceService ambulanceService() {
            return new FakeAmbulanceService();
        }

        @Bean
        public UserDAO userDAO() {
            return new UserDAO() {
                @Override
                public User getUserByUsername(String username) {
                    if ("driver01".equalsIgnoreCase(username)) {
                        User u = new User();
                        u.setUserId(101);
                        u.setUsername("driver01");
                        u.setRole("AMBULANCE_DRIVER");
                        return u;
                    }
                    if ("driver02".equalsIgnoreCase(username)) {
                        User u = new User();
                        u.setUserId(102);
                        u.setUsername("driver02");
                        u.setRole("AMBULANCE_DRIVER");
                        return u;
                    }
                    if ("paramedic01".equalsIgnoreCase(username)) {
                        User u = new User();
                        u.setUserId(201);
                        u.setUsername("paramedic01");
                        u.setRole("PARAMEDIC");
                        return u;
                    }
                    if ("paramedic02".equalsIgnoreCase(username)) {
                        User u = new User();
                        u.setUserId(202);
                        u.setUsername("paramedic02");
                        u.setRole("PARAMEDIC");
                        return u;
                    }
                    return null;
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AmbulanceService ambulanceService;

    @BeforeEach
    void setUp() {
        ((FakeAmbulanceService) ambulanceService).reset();
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void getAllAmbulances_asAdmin_returns200AndAllAmbulances() throws Exception {
        mockMvc.perform(get("/api/ambulances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(3)))
                .andExpect(jsonPath("$.data[0].vehicleNumber").value("KA-01-AB-1234"));
    }

    @Test
    @WithMockUser(username = "dispatcherUser", roles = {"DISPATCHER"})
    void getAllAmbulances_asDispatcher_returns200AndAllAmbulances() throws Exception {
        mockMvc.perform(get("/api/ambulances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    @WithMockUser(username = "paramedicUser", roles = {"PARAMEDIC"})
    void getAllAmbulances_asParamedic_returns200AndAllAmbulances() throws Exception {
        mockMvc.perform(get("/api/ambulances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    @WithMockUser(username = "doctorUser", roles = {"EMERGENCY_DOCTOR"})
    void getAllAmbulances_asDoctor_returns200AndAllAmbulances() throws Exception {
        mockMvc.perform(get("/api/ambulances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(3)));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void getAllAmbulances_asDriver101_returnsOnlyAssignedAmbulance() throws Exception {
        mockMvc.perform(get("/api/ambulances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].ambulanceId").value(1))
                .andExpect(jsonPath("$.data[0].vehicleNumber").value("KA-01-AB-1234"));
    }

    @Test
    @WithMockUser(username = "unknownDriver", roles = {"AMBULANCE_DRIVER"})
    void getAllAmbulances_asUnassignedDriver_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/ambulances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "hospAdmin", roles = {"HOSPITAL_ADMIN"})
    void getAllAmbulances_asHospitalAdmin_returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/ambulances"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllAmbulances_unauthenticated_returns401Or403() throws Exception {
        mockMvc.perform(get("/api/ambulances"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 401 || status == 403);
                });
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void getAmbulanceById_asAdmin_returns200() throws Exception {
        mockMvc.perform(get("/api/ambulances/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.ambulanceId").value(1))
                .andExpect(jsonPath("$.data.vehicleNumber").value("KA-01-AB-1234"));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void getAmbulanceById_invalidId_returns400() throws Exception {
        mockMvc.perform(get("/api/ambulances/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void getAmbulanceById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/ambulances/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void getAmbulanceById_asDriver101_assignedAmbulance_returns200() throws Exception {
        mockMvc.perform(get("/api/ambulances/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.ambulanceId").value(1));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void getAmbulanceById_asDriver101_otherAmbulance_returns404() throws Exception {
        mockMvc.perform(get("/api/ambulances/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "hospAdmin", roles = {"HOSPITAL_ADMIN"})
    void getAmbulanceById_asHospitalAdmin_returns403Forbidden() throws Exception {
        mockMvc.perform(get("/api/ambulances/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispatcherUser", roles = {"DISPATCHER"})
    void updateLocation_asDispatcher_success() throws Exception {
        AmbulanceLocationUpdateRequest req = new AmbulanceLocationUpdateRequest(12.9800, 77.6000, 45.5);

        mockMvc.perform(put("/api/ambulances/1/location")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.gpsLatitude").value(12.9800))
                .andExpect(jsonPath("$.data.gpsLongitude").value(77.6000));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void updateLocation_asAssignedDriver_success() throws Exception {
        AmbulanceLocationUpdateRequest req = new AmbulanceLocationUpdateRequest(12.9850, 77.6050, 30.0);

        mockMvc.perform(put("/api/ambulances/1/location")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.gpsLatitude").value(12.9850))
                .andExpect(jsonPath("$.data.gpsLongitude").value(77.6050));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void updateLocation_asDriverForOtherAmbulance_returns404() throws Exception {
        AmbulanceLocationUpdateRequest req = new AmbulanceLocationUpdateRequest(12.9850, 77.6050, 30.0);

        mockMvc.perform(put("/api/ambulances/2/location")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "paramedicUser", roles = {"PARAMEDIC"})
    void updateLocation_asParamedic_returns403Forbidden() throws Exception {
        // Policy 2: Paramedics may not update location in this phase
        AmbulanceLocationUpdateRequest req = new AmbulanceLocationUpdateRequest(12.9850, 77.6050, 30.0);

        mockMvc.perform(put("/api/ambulances/1/location")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispatcherUser", roles = {"DISPATCHER"})
    void updateLocation_invalidId_returns400() throws Exception {
        AmbulanceLocationUpdateRequest req = new AmbulanceLocationUpdateRequest(12.9850, 77.6050, 30.0);

        mockMvc.perform(put("/api/ambulances/0/location")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "dispatcherUser", roles = {"DISPATCHER"})
    void updateLocation_missingLatitude_returns400() throws Exception {
        String invalidJson = "{\"longitude\": 77.6050, \"speed\": 30.0}";

        mockMvc.perform(put("/api/ambulances/1/location")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "dispatcherUser", roles = {"DISPATCHER"})
    void updateLocation_latitudeOutOfRange_returns400() throws Exception {
        AmbulanceLocationUpdateRequest req = new AmbulanceLocationUpdateRequest(95.0, 77.6050, 30.0);

        mockMvc.perform(put("/api/ambulances/1/location")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "dispatcherUser", roles = {"DISPATCHER"})
    void updateLocation_longitudeOutOfRange_returns400() throws Exception {
        AmbulanceLocationUpdateRequest req = new AmbulanceLocationUpdateRequest(12.9850, 190.0, 30.0);

        mockMvc.perform(put("/api/ambulances/1/location")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "dispatcherUser", roles = {"DISPATCHER"})
    void updateLocation_speedOutOfRange_returns400() throws Exception {
        AmbulanceLocationUpdateRequest req = new AmbulanceLocationUpdateRequest(12.9850, 77.6050, 300.0);

        mockMvc.perform(put("/api/ambulances/1/location")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "dispatcherUser", roles = {"DISPATCHER"})
    void updateLocation_databaseError_returns500() throws Exception {
        ((FakeAmbulanceService) ambulanceService).setSimulateDbError(true);
        AmbulanceLocationUpdateRequest req = new AmbulanceLocationUpdateRequest(12.9850, 77.6050, 30.0);

        mockMvc.perform(put("/api/ambulances/1/location")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    @WithMockUser(username = "unresolvableDriver", roles = {"AMBULANCE_DRIVER"})
    void getAllAmbulances_unresolvableDriver_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/ambulances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void getAllAmbulances_domainUserPrincipal_success() throws Exception {
        User driverUser = new User();
        driverUser.setUserId(101);
        driverUser.setUsername("driverDirect");
        driverUser.setRole("AMBULANCE_DRIVER");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                driverUser, null, List.of(new SimpleGrantedAuthority("ROLE_AMBULANCE_DRIVER"))
        );

        mockMvc.perform(get("/api/ambulances").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }
}
