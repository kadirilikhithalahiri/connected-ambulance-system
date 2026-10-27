package com.abc.emergency.web;

import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.dto.ApiResponse;
import com.abc.emergency.dto.DispatchRequest;
import com.abc.emergency.dto.DispatchResponse;
import com.abc.emergency.dto.DispatchStatusUpdateRequest;
import com.abc.emergency.model.DispatchRecord;
import com.abc.emergency.model.User;
import com.abc.emergency.service.DispatchService;
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
public class DispatchController {

    private final DispatchService dispatchService;
    private final UserDAO userDAO;

    public DispatchController(DispatchService dispatchService) {
        this(dispatchService, new UserDAO());
    }

    @Autowired
    public DispatchController(DispatchService dispatchService, @Autowired(required = false) UserDAO userDAO) {
        this.dispatchService = dispatchService;
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

    @PostMapping("/api/dispatches")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<DispatchResponse>> createDispatch(@Valid @RequestBody DispatchRequest request) {
        ServiceResult<DispatchRecord> result = dispatchService.dispatchAmbulanceResult(
                request.getCaseId(),
                request.getAmbulanceId(),
                request.getEstimatedArrivalMinutes()
        );

        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(201, "Ambulance dispatched successfully.", DispatchResponse.from(result.getData())));
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
                    .body(new ApiResponse<>(500, "An error occurred while creating the dispatch record. Please contact administrator.", null));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, result.getMessage(), null));
    }

    @GetMapping("/api/dispatches")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER')")
    public ResponseEntity<ApiResponse<List<DispatchResponse>>> getAllDispatches(Authentication authentication) {
        String role = resolveRole(authentication);
        ServiceResult<List<DispatchRecord>> result = dispatchService.getAllDispatchesForCallerResult(role);

        if (result.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An error occurred while retrieving dispatch records. Please contact administrator.", null));
        }

        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, result.getMessage(), null));
        }

        List<DispatchResponse> responses = result.getData().stream()
                .map(DispatchResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(200, "Dispatch records retrieved successfully.", responses));
    }

    @GetMapping("/api/dispatches/my-active")
    @PreAuthorize("hasRole('AMBULANCE_DRIVER')")
    public ResponseEntity<ApiResponse<DispatchResponse>> getMyActiveDispatch(Authentication authentication) {
        int userId = resolveUserId(authentication);
        ServiceResult<DispatchRecord> result = dispatchService.getActiveDispatchForDriver(userId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse<>(200, "Active dispatch retrieved successfully.", DispatchResponse.from(result.getData())));
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
                    .body(new ApiResponse<>(500, "An error occurred while retrieving active dispatch. Please contact administrator.", null));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, result.getMessage(), null));
    }

    @GetMapping("/api/dispatches/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'AMBULANCE_DRIVER')")
    public ResponseEntity<ApiResponse<DispatchResponse>> getDispatchById(@PathVariable("id") int id,
                                                                         Authentication authentication) {
        if (!ValidationUtil.isValidId(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Invalid dispatch ID. ID must be greater than 0.", null));
        }

        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        ServiceResult<DispatchRecord> result = dispatchService.getDispatchByIdForCaller(id, userId, role);

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
                    .body(new ApiResponse<>(500, "An error occurred while retrieving the dispatch record. Please contact administrator.", null));
        }

        return ResponseEntity.ok(new ApiResponse<>(200, "Dispatch record retrieved successfully.", DispatchResponse.from(result.getData())));
    }

    @PutMapping("/api/dispatches/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'AMBULANCE_DRIVER')")
    public ResponseEntity<ApiResponse<DispatchResponse>> updateDispatchStatus(@PathVariable("id") int id,
                                                                              @Valid @RequestBody DispatchStatusUpdateRequest request,
                                                                              Authentication authentication) {
        if (!ValidationUtil.isValidId(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Invalid dispatch ID. ID must be greater than 0.", null));
        }

        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        ServiceResult<DispatchRecord> result = dispatchService.updateDispatchStatusResult(id, request.getStatus(), userId, role);

        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse<>(200, result.getMessage(), DispatchResponse.from(result.getData())));
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
                    .body(new ApiResponse<>(500, "An error occurred while updating dispatch status. Please contact administrator.", null));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, result.getMessage(), null));
    }

    @GetMapping("/api/cases/{caseId}/dispatches")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PARAMEDIC')")
    public ResponseEntity<ApiResponse<List<DispatchResponse>>> getDispatchesByCaseId(@PathVariable("caseId") int caseId,
                                                                                     Authentication authentication) {
        if (!ValidationUtil.isValidId(caseId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Invalid case ID. ID must be greater than 0.", null));
        }

        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        ServiceResult<List<DispatchRecord>> result = dispatchService.getDispatchesByCaseIdForCaller(caseId, userId, role);

        if (result.getStatus() == ServiceResult.Status.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, result.getMessage(), null));
        }

        if (result.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An error occurred while retrieving dispatch records. Please contact administrator.", null));
        }

        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, result.getMessage(), null));
        }

        List<DispatchResponse> responses = result.getData().stream()
                .map(DispatchResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(200, "Case dispatch history retrieved successfully.", responses));
    }
}
