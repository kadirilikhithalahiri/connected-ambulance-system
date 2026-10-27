package com.abc.emergency.web;

import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.dto.ApiResponse;
import com.abc.emergency.dto.PatientRequest;
import com.abc.emergency.dto.PatientResponse;
import com.abc.emergency.model.Patient;
import com.abc.emergency.model.User;
import com.abc.emergency.service.PatientService;
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
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final UserDAO userDAO;

    public PatientController(PatientService patientService) {
        this(patientService, new UserDAO());
    }

    @Autowired
    public PatientController(PatientService patientService, @Autowired(required = false) UserDAO userDAO) {
        this.patientService = patientService;
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

    private boolean isMedicalRole(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) ||
                "ROLE_PARAMEDIC".equals(a.getAuthority()) ||
                "ROLE_EMERGENCY_DOCTOR".equals(a.getAuthority())
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PARAMEDIC', 'EMERGENCY_DOCTOR')")
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getAllPatients(Authentication authentication) {
        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        ServiceResult<List<Patient>> result = patientService.getPatientsForCallerResult(userId, role);
        if (result.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An error occurred while retrieving patient records. Please contact administrator.", null));
        }

        if (!result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, result.getMessage(), null));
        }

        boolean medical = isMedicalRole(authentication);
        List<PatientResponse> responses = result.getData().stream()
                .map(p -> medical ? PatientResponse.from(p) : PatientResponse.summary(p))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new ApiResponse<>(200, "Patients retrieved successfully.", responses));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PARAMEDIC', 'EMERGENCY_DOCTOR')")
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(@PathVariable("id") int id,
                                                                       Authentication authentication) {
        if (!ValidationUtil.isValidId(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Invalid patient ID. ID must be greater than 0.", null));
        }

        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        ServiceResult<Patient> result = patientService.getPatientByIdForCaller(id, userId, role);
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
                    .body(new ApiResponse<>(500, "An error occurred while retrieving the patient record. Please contact administrator.", null));
        }

        boolean medical = isMedicalRole(authentication);
        PatientResponse response = medical ? PatientResponse.from(result.getData()) : PatientResponse.summary(result.getData());

        return ResponseEntity.ok(new ApiResponse<>(200, "Patient retrieved successfully.", response));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DISPATCHER', 'PARAMEDIC')")
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(@Valid @RequestBody PatientRequest request) {
        ServiceResult<Patient> result = patientService.createPatientResult(
                request.getPatientName(),
                request.getAge(),
                request.getGender(),
                request.getBloodGroup(),
                request.getMedicalHistory(),
                request.getAllergies(),
                request.getEmergencyContact()
        );

        if (result.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse<>(201, "Patient registered successfully.", PatientResponse.from(result.getData())));
        }

        if (result.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An error occurred while saving the patient record. Please contact administrator.", null));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, result.getMessage(), null));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PARAMEDIC', 'EMERGENCY_DOCTOR')")
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(@PathVariable("id") int id,
                                                                      @Valid @RequestBody PatientRequest request,
                                                                      Authentication authentication) {
        if (!ValidationUtil.isValidId(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(400, "Invalid patient ID. ID must be greater than 0.", null));
        }

        int userId = resolveUserId(authentication);
        String role = resolveRole(authentication);

        Patient patientToUpdate = new Patient(
                id,
                request.getPatientName(),
                request.getAge(),
                request.getGender().toUpperCase(),
                request.getBloodGroup(),
                request.getMedicalHistory(),
                request.getAllergies(),
                request.getEmergencyContact()
        );

        ServiceResult<Patient> result = patientService.updatePatientResultForCaller(patientToUpdate, userId, role);

        if (result.isSuccess()) {
            return ResponseEntity.ok(new ApiResponse<>(200, "Patient updated successfully.", PatientResponse.from(result.getData())));
        }

        if (result.getStatus() == ServiceResult.Status.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse<>(404, result.getMessage(), null));
        }

        if (result.getStatus() == ServiceResult.Status.DATABASE_ERROR) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse<>(500, "An error occurred while updating the patient record. Please contact administrator.", null));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(400, result.getMessage(), null));
    }
}
