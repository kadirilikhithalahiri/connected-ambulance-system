package com.abc.emergency.web;

import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.dto.ApiResponse;
import com.abc.emergency.dto.AmbulanceLocationUpdateRequest;
import com.abc.emergency.dto.AmbulanceResponse;
import com.abc.emergency.model.Ambulance;
import com.abc.emergency.model.User;
import com.abc.emergency.service.AmbulanceService;
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
@RequestMapping("/api/ambulances")
public class AmbulanceController {

    private final AmbulanceService ambulanceService;
    private final UserDAO userDAO;

    public AmbulanceController(AmbulanceService ambulanceService) {
        this(ambulanceService, new UserDAO());
    }

    @Autowired
    public AmbulanceController(AmbulanceService ambulanceService, @Autowired(required = false) UserDAO userDAO) {
        this.ambulanceService = ambulanceService;
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
    public ResponseEntity<ApiResponse<List<AmbulanceResponse>>> getAllAmbulances(Authentication authentication) {
        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        List<Ambulance> ambulances = ambulanceService.getAllAmbulancesForCaller(userId, role);
        List<AmbulanceResponse> responses = ambulances.stream()
                .map(AmbulanceResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(200, "Ambulances retrieved successfully.", responses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PARAMEDIC', 'EMERGENCY_DOCTOR', 'AMBULANCE_DRIVER')")
    public ResponseEntity<ApiResponse<AmbulanceResponse>> getAmbulanceById(@PathVariable("id") int id,
                                                                           Authentication authentication) {
        if (!ValidationUtil.isValidId(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Invalid ambulance ID. ID must be greater than 0.", null));
        }

        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        ServiceResult<Ambulance> result = ambulanceService.getAmbulanceByIdForCaller(id, userId, role);

        if (result.getStatus() == ServiceResult.Status.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, result.getMessage(), null));
        }

        if (result.getStatus() == ServiceResult.Status.VALIDATION_ERROR) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, result.getMessage(), null));
        }

        return ResponseEntity.ok(new ApiResponse<>(200, "Ambulance retrieved successfully.", AmbulanceResponse.from(result.getData())));
    }

    @PutMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'AMBULANCE_DRIVER')")
    public ResponseEntity<ApiResponse<AmbulanceResponse>> updateLocation(@PathVariable("id") int id,
                                                                         @Valid @RequestBody AmbulanceLocationUpdateRequest request,
                                                                         Authentication authentication) {
        if (!ValidationUtil.isValidId(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Invalid ambulance ID. ID must be greater than 0.", null));
        }

        if (request.getLatitude() == null || !Double.isFinite(request.getLatitude())
                || request.getLongitude() == null || !Double.isFinite(request.getLongitude())
                || (request.getSpeed() != null && !Double.isFinite(request.getSpeed()))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Latitude, longitude, and speed must be finite numbers.", null));
        }

        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        ServiceResult<Ambulance> result = ambulanceService.updateAmbulanceLocationResult(
                id,
                request.getLatitude(),
                request.getLongitude(),
                request.getSpeed(),
                userId,
                role
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse<>(200, "Ambulance location updated successfully.", AmbulanceResponse.from(result.getData())));
        }

        if (result.getStatus() == ServiceResult.Status.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, result.getMessage(), null));
        }

        if (result.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An error occurred while updating ambulance location. Please contact administrator.", null));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, result.getMessage(), null));
    }
}
