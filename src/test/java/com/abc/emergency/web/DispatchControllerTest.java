package com.abc.emergency.web;

import com.abc.emergency.config.WebSecurityConfig;
import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.dto.DispatchRequest;
import com.abc.emergency.dto.DispatchStatusUpdateRequest;
import com.abc.emergency.model.DispatchRecord;
import com.abc.emergency.model.User;
import com.abc.emergency.service.DispatchService;
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
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {DispatchController.class, GlobalExceptionHandler.class})
@Import({WebSecurityConfig.class, DispatchControllerTest.TestConfig.class})
public class DispatchControllerTest {

    static class FakeDispatchService extends DispatchService {
        final Map<Integer, DispatchRecord> store = new LinkedHashMap<>();
        private boolean simulateConflict = false;
        private boolean simulateDbError = false;

        public FakeDispatchService() {
            reset();
        }

        public void reset() {
            store.clear();
            DispatchRecord d1 = new DispatchRecord(1, 1, 1, 10, "DISPATCHED");
            DispatchRecord d2 = new DispatchRecord(2, 2, 2, 15, "EN_ROUTE");
            DispatchRecord d3 = new DispatchRecord(3, 3, 3, 20, "ARRIVED");
            DispatchRecord d4 = new DispatchRecord(4, 4, 1, 5, "COMPLETED");
            store.put(1, d1);
            store.put(2, d2);
            store.put(3, d3);
            store.put(4, d4);
            simulateConflict = false;
            simulateDbError = false;
            simulateAllDispatchesDbError = false;
        }

        public boolean simulateAllDispatchesDbError = false;

        public void setSimulateConflict(boolean simulateConflict) {
            this.simulateConflict = simulateConflict;
        }

        public void setSimulateDbError(boolean simulateDbError) {
            this.simulateDbError = simulateDbError;
        }

        @Override
        public ServiceResult<DispatchRecord> dispatchAmbulanceResult(int caseId, int ambulanceId, int estimatedArrivalMinutes) {
            if (!ValidationUtil.isValidId(caseId) || !ValidationUtil.isValidId(ambulanceId)) {
                return ServiceResult.validationError("Invalid Case ID or Ambulance ID. Both must be positive integers.");
            }
            if (caseId == 999) {
                return ServiceResult.notFound("Emergency case not found with ID: " + caseId);
            }
            if (ambulanceId == 999) {
                return ServiceResult.notFound("Ambulance not found with ID: " + ambulanceId);
            }
            if (caseId == 1) {
                return ServiceResult.conflict("Active dispatch already exists for case ID: " + caseId);
            }
            if (ambulanceId == 2) {
                return ServiceResult.conflict("Ambulance ID " + ambulanceId + " is currently not available for dispatch.");
            }
            if (caseId == 888) {
                return ServiceResult.databaseError("DATABASE_ERROR: Connection failed.");
            }
            int newId = store.size() + 1;
            DispatchRecord rec = new DispatchRecord(newId, caseId, ambulanceId, estimatedArrivalMinutes, "DISPATCHED");
            store.put(newId, rec);
            return ServiceResult.success("Ambulance dispatched successfully.", rec);
        }

        @Override
        public ServiceResult<List<DispatchRecord>> getAllDispatchesForCallerResult(String role) {
            if (simulateAllDispatchesDbError || simulateDbError) {
                return ServiceResult.databaseError("DATABASE_ERROR: Connection failed.");
            }
            if ("ADMIN".equalsIgnoreCase(role) || "DISPATCHER".equalsIgnoreCase(role)) {
                return ServiceResult.success("Dispatch records retrieved successfully.", new ArrayList<>(store.values()));
            }
            return ServiceResult.success("Dispatch records retrieved successfully.", Collections.emptyList());
        }

        @Override
        public List<DispatchRecord> getAllDispatchesForCaller(String role) {
            ServiceResult<List<DispatchRecord>> res = getAllDispatchesForCallerResult(role);
            return res.isSuccess() ? res.getData() : Collections.emptyList();
        }

        @Override
        public ServiceResult<DispatchRecord> getDispatchByIdForCaller(int dispatchId, int callerUserId, String callerRole) {
            if (!ValidationUtil.isValidId(dispatchId)) {
                return ServiceResult.validationError("Invalid dispatch ID. ID must be greater than 0.");
            }
            if (dispatchId == 888 || simulateDbError) {
                return ServiceResult.databaseError("DATABASE_ERROR: Connection failed.");
            }
            DispatchRecord rec = store.get(dispatchId);
            if (rec == null) {
                return ServiceResult.notFound("Dispatch record not found with ID: " + dispatchId);
            }
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(callerRole)) {
                if (callerUserId == 101 && rec.getAmbulanceId() != 1) {
                    return ServiceResult.notFound("Dispatch record not found with ID: " + dispatchId);
                }
                if (callerUserId == 102 && rec.getAmbulanceId() != 2) {
                    return ServiceResult.notFound("Dispatch record not found with ID: " + dispatchId);
                }
            }
            return ServiceResult.success("Dispatch record retrieved successfully.", rec);
        }

        @Override
        public ServiceResult<DispatchRecord> updateDispatchStatusResult(int dispatchId, String targetStatus, int callerUserId, String callerRole) {
            if (!ValidationUtil.isValidId(dispatchId)) {
                return ServiceResult.validationError("Invalid Dispatch ID. Must be a positive integer.");
            }
            if (simulateConflict) {
                return ServiceResult.conflict("Concurrent update conflict on dispatch record ID: " + dispatchId);
            }
            if (simulateDbError) {
                return ServiceResult.databaseError("DATABASE_ERROR: Connection lost.");
            }
            DispatchRecord rec = store.get(dispatchId);
            if (rec == null) {
                return ServiceResult.notFound("Dispatch record not found with ID: " + dispatchId);
            }
            if ("AMBULANCE_DRIVER".equalsIgnoreCase(callerRole)) {
                if (callerUserId == 101 && rec.getAmbulanceId() != 1) {
                    return ServiceResult.notFound("Dispatch record not found with ID: " + dispatchId);
                }
                if (callerUserId == 102 && rec.getAmbulanceId() != 2) {
                    return ServiceResult.notFound("Dispatch record not found with ID: " + dispatchId);
                }
            }
            String curr = rec.getDispatchStatus().toUpperCase();
            String target = targetStatus.toUpperCase();

            if (!isAllowedTransition(curr, target)) {
                return ServiceResult.conflict("Invalid dispatch status transition from " + curr + " to " + target + ".");
            }
            rec.setDispatchStatus(target);
            return ServiceResult.success("Dispatch status updated successfully to " + target + ".", rec);
        }

        @Override
        public ServiceResult<DispatchRecord> getActiveDispatchForDriver(int driverUserId) {
            if (driverUserId == 888 || simulateDbError) {
                return ServiceResult.databaseError("DATABASE_ERROR: Connection lost.");
            }
            if (driverUserId == 101) {
                return ServiceResult.success("Active dispatch retrieved.", store.get(1));
            }
            if (driverUserId == 102) {
                return ServiceResult.success("Active dispatch retrieved.", store.get(2));
            }
            if (driverUserId == 103) {
                return ServiceResult.conflict("Ambiguous driver assignment: multiple ambulances assigned to user ID " + driverUserId);
            }
            return ServiceResult.notFound("No active dispatch found for assigned ambulance.");
        }

        @Override
        public List<DispatchRecord> getDispatchesByCaseId(int caseId) {
            if (!ValidationUtil.isValidId(caseId)) {
                return Collections.emptyList();
            }
            return store.values().stream()
                    .filter(d -> d.getCaseId() == caseId)
                    .collect(Collectors.toList());
        }

        @Override
        public ServiceResult<List<DispatchRecord>> getDispatchesByCaseIdForCaller(int caseId, int userId, String role) {
            if (!ValidationUtil.isValidId(caseId)) {
                return ServiceResult.validationError("Invalid case ID. ID must be greater than 0.");
            }
            if (caseId == 888) {
                return ServiceResult.databaseError("Database connection failed.");
            }
            if (caseId == 999) {
                return ServiceResult.notFound("Emergency case not found with ID: " + caseId);
            }
            if ("PARAMEDIC".equalsIgnoreCase(role)) {
                // Paramedic 201 (paramedic01) only authorized for Case 1
                if (userId != 201 || caseId != 1) {
                    return ServiceResult.notFound("Emergency case not found with ID: " + caseId);
                }
            } else if ("AMBULANCE_DRIVER".equalsIgnoreCase(role) || "HOSPITAL_ADMIN".equalsIgnoreCase(role)) {
                return ServiceResult.notFound("Emergency case not found with ID: " + caseId);
            }
            List<DispatchRecord> list = store.values().stream()
                    .filter(d -> d.getCaseId() == caseId)
                    .collect(Collectors.toList());
            return ServiceResult.success("Dispatches retrieved successfully.", list);
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public DispatchService dispatchService() {
            return new FakeDispatchService();
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
                    if ("driver03".equalsIgnoreCase(username)) {
                        User u = new User();
                        u.setUserId(103);
                        u.setUsername("driver03");
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
    private DispatchService dispatchService;

    @BeforeEach
    void setUp() {
        ((FakeDispatchService) dispatchService).reset();
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void createDispatch_asDispatcher_success201() throws Exception {
        DispatchRequest req = new DispatchRequest(5, 3, 12);

        mockMvc.perform(post("/api/dispatches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.caseId").value(5))
                .andExpect(jsonPath("$.data.ambulanceId").value(3))
                .andExpect(jsonPath("$.data.dispatchStatus").value("DISPATCHED"));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void createDispatch_asAdmin_success201() throws Exception {
        DispatchRequest req = new DispatchRequest(6, 3, 10);

        mockMvc.perform(post("/api/dispatches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void createDispatch_asDriver_forbidden403() throws Exception {
        DispatchRequest req = new DispatchRequest(5, 3, 12);

        mockMvc.perform(post("/api/dispatches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "paramedicUser", roles = {"PARAMEDIC"})
    void createDispatch_asParamedic_forbidden403() throws Exception {
        DispatchRequest req = new DispatchRequest(5, 3, 12);

        mockMvc.perform(post("/api/dispatches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void createDispatch_caseNotFound_returns404() throws Exception {
        DispatchRequest req = new DispatchRequest(999, 3, 10);

        mockMvc.perform(post("/api/dispatches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void createDispatch_duplicateCaseDispatch_returns409Conflict() throws Exception {
        DispatchRequest req = new DispatchRequest(1, 3, 10);

        mockMvc.perform(post("/api/dispatches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void createDispatch_ambulanceAlreadyDispatched_returns409Conflict() throws Exception {
        DispatchRequest req = new DispatchRequest(7, 2, 10);

        mockMvc.perform(post("/api/dispatches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void createDispatch_invalidNegativeId_returns400() throws Exception {
        DispatchRequest req = new DispatchRequest(-1, 2, 10);

        mockMvc.perform(post("/api/dispatches")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void getAllDispatches_asDispatcher_returns200() throws Exception {
        mockMvc.perform(get("/api/dispatches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(4)));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void getAllDispatches_asDriver_forbidden403() throws Exception {
        mockMvc.perform(get("/api/dispatches"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void getDispatchById_asDispatcher_returns200() throws Exception {
        mockMvc.perform(get("/api/dispatches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dispatchId").value(1));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void getDispatchById_asAssignedDriver_returns200() throws Exception {
        mockMvc.perform(get("/api/dispatches/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dispatchId").value(1));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void getDispatchById_asDriverForOtherAmbulance_returns404() throws Exception {
        mockMvc.perform(get("/api/dispatches/2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void getDispatchById_invalidId_returns400() throws Exception {
        mockMvc.perform(get("/api/dispatches/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void updateStatus_asDriver_validTransitionToEnRoute_returns200() throws Exception {
        DispatchStatusUpdateRequest req = new DispatchStatusUpdateRequest("EN_ROUTE");

        mockMvc.perform(put("/api/dispatches/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dispatchStatus").value("EN_ROUTE"));
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void updateStatus_asDispatcher_validTransitionToArrived_returns200() throws Exception {
        DispatchStatusUpdateRequest req = new DispatchStatusUpdateRequest("ARRIVED");

        mockMvc.perform(put("/api/dispatches/2/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dispatchStatus").value("ARRIVED"));
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void updateStatus_preDepartureCancellation_returns200() throws Exception {
        // Policy 1: Pre-departure cancellation DISPATCHED -> CANCELLED
        DispatchStatusUpdateRequest req = new DispatchStatusUpdateRequest("CANCELLED");

        mockMvc.perform(put("/api/dispatches/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dispatchStatus").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void updateStatus_postDepartureCancellation_returns200() throws Exception {
        // Policy 1: Post-departure cancellation EN_ROUTE -> CANCELLED
        DispatchStatusUpdateRequest req = new DispatchStatusUpdateRequest("CANCELLED");

        mockMvc.perform(put("/api/dispatches/2/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dispatchStatus").value("CANCELLED"));
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void updateStatus_invalidTransition_returns409Conflict() throws Exception {
        DispatchStatusUpdateRequest req = new DispatchStatusUpdateRequest("COMPLETED");

        // Dispatch 1 is currently DISPATCHED; transitioning directly to COMPLETED is invalid
        mockMvc.perform(put("/api/dispatches/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void updateStatus_invalidStatusString_returns400() throws Exception {
        DispatchStatusUpdateRequest req = new DispatchStatusUpdateRequest("FLYING");

        mockMvc.perform(put("/api/dispatches/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void updateStatus_concurrentConflict_returns409() throws Exception {
        ((FakeDispatchService) dispatchService).setSimulateConflict(true);
        DispatchStatusUpdateRequest req = new DispatchStatusUpdateRequest("EN_ROUTE");

        mockMvc.perform(put("/api/dispatches/1/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void updateStatus_asDriverForOtherAmbulance_returns404() throws Exception {
        DispatchStatusUpdateRequest req = new DispatchStatusUpdateRequest("ARRIVED");

        // Driver 101 attempting to update dispatch 2 (assigned to ambulance 2)
        mockMvc.perform(put("/api/dispatches/2/status")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void getMyActiveDispatch_asDriver101_returns200() throws Exception {
        mockMvc.perform(get("/api/dispatches/my-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dispatchId").value(1))
                .andExpect(jsonPath("$.data.ambulanceId").value(1));
    }

    @Test
    @WithMockUser(username = "driver02", roles = {"AMBULANCE_DRIVER"})
    void getMyActiveDispatch_asDriver102_returns200() throws Exception {
        mockMvc.perform(get("/api/dispatches/my-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dispatchId").value(2))
                .andExpect(jsonPath("$.data.ambulanceId").value(2));
    }

    @Test
    @WithMockUser(username = "unknownDriver", roles = {"AMBULANCE_DRIVER"})
    void getMyActiveDispatch_asDriverWithoutDispatch_returns404() throws Exception {
        mockMvc.perform(get("/api/dispatches/my-active"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void getMyActiveDispatch_asDispatcher_forbidden403() throws Exception {
        mockMvc.perform(get("/api/dispatches/my-active"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void getDispatchesByCaseId_asDispatcher_returns200() throws Exception {
        mockMvc.perform(get("/api/cases/1/dispatches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].caseId").value(1));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void getDispatchesByCaseId_asParamedic_assignedCase_returns200() throws Exception {
        mockMvc.perform(get("/api/cases/1/dispatches"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].caseId").value(1));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void getDispatchesByCaseId_asParamedic_unassignedCase_returns404() throws Exception {
        mockMvc.perform(get("/api/cases/2/dispatches"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(username = "paramedic01", roles = {"PARAMEDIC"})
    void getDispatchesByCaseId_databaseError_returns500() throws Exception {
        mockMvc.perform(get("/api/cases/888/dispatches"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void getDispatchesByCaseId_asDriver_forbidden403() throws Exception {
        mockMvc.perform(get("/api/cases/1/dispatches"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void getDispatchesByCaseId_invalidCaseId_returns400() throws Exception {
        mockMvc.perform(get("/api/cases/0/dispatches"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @WithMockUser(username = "adminUser", roles = {"ADMIN"})
    void getAllDispatches_databaseError_returns500() throws Exception {
        ((FakeDispatchService) dispatchService).simulateAllDispatchesDbError = true;
        mockMvc.perform(get("/api/dispatches"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("An error occurred while retrieving dispatch records")));
    }

    @Test
    @WithMockUser(username = "dispUser", roles = {"DISPATCHER"})
    void getDispatchById_databaseError_returns500() throws Exception {
        mockMvc.perform(get("/api/dispatches/888"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("An error occurred while retrieving the dispatch record")));
    }

    @Test
    @WithMockUser(username = "driver01", roles = {"AMBULANCE_DRIVER"})
    void getMyActiveDispatch_databaseError_returns500() throws Exception {
        ((FakeDispatchService) dispatchService).simulateDbError = true;
        mockMvc.perform(get("/api/dispatches/my-active"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value(containsString("An error occurred while retrieving active dispatch")));
    }

    @Test
    @WithMockUser(username = "unresolvableDriver", roles = {"AMBULANCE_DRIVER"})
    void getMyActiveDispatch_unresolvableUser_failsClosedReturns404() throws Exception {
        mockMvc.perform(get("/api/dispatches/my-active"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getMyActiveDispatch_domainUserPrincipal_success() throws Exception {
        User driverUser = new User();
        driverUser.setUserId(101);
        driverUser.setUsername("driverDirect");
        driverUser.setRole("AMBULANCE_DRIVER");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                driverUser, null, List.of(new SimpleGrantedAuthority("ROLE_AMBULANCE_DRIVER"))
        );

        mockMvc.perform(get("/api/dispatches/my-active").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.dispatchId").value(1));
    }
}
