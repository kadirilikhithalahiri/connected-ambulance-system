package com.abc.emergency.web;

import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.dto.ApiResponse;
import com.abc.emergency.dto.CaseStatusUpdateRequest;
import com.abc.emergency.dto.CaseTriageRequest;
import com.abc.emergency.dto.CaseTriageResponse;
import com.abc.emergency.dto.EmergencyCaseRequest;
import com.abc.emergency.dto.EmergencyCaseResponse;
import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.model.User;
import com.abc.emergency.service.EmergencyCaseService;
import com.abc.emergency.service.ServiceResult;
import com.abc.emergency.util.ValidationUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cases")
public class EmergencyCaseController {

    private final EmergencyCaseService emergencyCaseService;
    private final UserDAO userDAO;

    public EmergencyCaseController(EmergencyCaseService emergencyCaseService) {
        this(emergencyCaseService, new UserDAO());
    }

    @Autowired
    public EmergencyCaseController(EmergencyCaseService emergencyCaseService, @Autowired(required = false) UserDAO userDAO) {
        this.emergencyCaseService = emergencyCaseService;
        this.userDAO = userDAO != null ? userDAO : new UserDAO();
    }

    private int resolveUserId(Authentication authentication) {
        if (authentication == null) {
            return -1;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user.getUserId();
        }
        String name = authentication.getName();
        if (name != null && !name.trim().isEmpty()) {
            User user = userDAO.getUserByUsername(name);
            if (user != null) {
                return user.getUserId();
            }
        }
        return -1;
    }

    private String resolveRole(Authentication authentication) {
        if (authentication == null) {
            return "";
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String auth = authority.getAuthority();
            if (auth.startsWith("ROLE_")) {
                return auth.substring(5);
            }
        }
        return "";
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PARAMEDIC', 'EMERGENCY_DOCTOR', 'AMBULANCE_DRIVER')")
    public ResponseEntity<ApiResponse<List<EmergencyCaseResponse>>> getAllCases(Authentication authentication) {
        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        ServiceResult<List<EmergencyCase>> result = emergencyCaseService.getCasesForCallerResult(userId, role);
        if (result.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An error occurred while retrieving emergency cases. Please contact administrator.", null));
        }

        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, result.getMessage(), null));
        }

        List<EmergencyCaseResponse> responses = result.getData().stream()
                .map(EmergencyCaseResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(200, "Emergency cases retrieved successfully.", responses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PARAMEDIC', 'EMERGENCY_DOCTOR', 'AMBULANCE_DRIVER')")
    public ResponseEntity<ApiResponse<EmergencyCaseResponse>> getCaseById(@PathVariable("id") int id,
                                                                         Authentication authentication) {
        if (!ValidationUtil.isValidId(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Invalid case ID. ID must be greater than 0.", null));
        }

        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        ServiceResult<EmergencyCase> result = emergencyCaseService.getCaseByIdForCaller(id, userId, role);

        if (result.getStatus() == ServiceResult.Status.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, result.getMessage(), null));
        }

        if (result.getStatus() == ServiceResult.Status.VALIDATION_ERROR) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, result.getMessage(), null));
        }

        if (result.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An error occurred while retrieving the emergency case. Please contact administrator.", null));
        }

        return ResponseEntity.ok(new ApiResponse<>(200, "Emergency case retrieved successfully.", EmergencyCaseResponse.from(result.getData())));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PARAMEDIC')")
    public ResponseEntity<ApiResponse<EmergencyCaseResponse>> createCase(@Valid @RequestBody EmergencyCaseRequest request) {
        ServiceResult<EmergencyCase> result = emergencyCaseService.createEmergencyCaseResult(
                request.getPatientId(),
                request.getIncidentLocation(),
                request.getEmergencyType(),
                request.getSymptoms(),
                request.getPriorityLevel()
        );

        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(201, "Emergency case registered successfully.", EmergencyCaseResponse.from(result.getData())));
        }

        if (result.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An error occurred while saving the emergency case. Please contact administrator.", null));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, result.getMessage(), null));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PARAMEDIC')")
    public ResponseEntity<ApiResponse<EmergencyCaseResponse>> updateCaseStatus(@PathVariable("id") int id,
                                                                               @Valid @RequestBody CaseStatusUpdateRequest request,
                                                                               Authentication authentication) {
        if (!ValidationUtil.isValidId(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Invalid case ID. ID must be greater than 0.", null));
        }

        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        ServiceResult<EmergencyCase> result = emergencyCaseService.updateCaseStatusAtomic(id, request.getStatus(), userId, role);

        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse<>(200, "Emergency case status updated successfully.", EmergencyCaseResponse.from(result.getData())));
        }

        if (result.getStatus() == ServiceResult.Status.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(409, result.getMessage(), null));
        }

        if (result.getStatus() == ServiceResult.Status.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, result.getMessage(), null));
        }

        if (result.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An error occurred while updating the case status. Please contact administrator.", null));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, result.getMessage(), null));
    }

    @PostMapping("/{id}/triage")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PARAMEDIC', 'EMERGENCY_DOCTOR')")
    public ResponseEntity<ApiResponse<CaseTriageResponse>> triageCase(@PathVariable("id") int id,
                                                                      @Valid @RequestBody CaseTriageRequest request,
                                                                      Authentication authentication) {
        if (!ValidationUtil.isValidId(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Invalid case ID. ID must be greater than 0.", null));
        }

        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        ServiceResult<CaseTriageResponse> result = emergencyCaseService.triageCaseResult(id, request, userId, role);

        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse<>(200, "AI triage evaluation completed successfully.", result.getData()));
        }

        if (result.getStatus() == ServiceResult.Status.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, result.getMessage(), null));
        }

        if (result.getStatus() == ServiceResult.Status.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiResponse<>(409, result.getMessage(), null));
        }

        if (result.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An error occurred during AI triage evaluation. Please contact administrator.", null));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, result.getMessage(), null));
    }
}
