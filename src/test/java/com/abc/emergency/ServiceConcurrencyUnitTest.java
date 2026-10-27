package com.abc.emergency;

import com.abc.emergency.dao.EmergencyCaseDAO;
import com.abc.emergency.dao.PatientDAO;
import com.abc.emergency.model.EmergencyCase;
import com.abc.emergency.model.Patient;
import com.abc.emergency.service.EmergencyCaseService;
import com.abc.emergency.service.PatientService;
import com.abc.emergency.service.ServiceResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ServiceConcurrencyUnitTest {

    @Test
    @DisplayName("PatientService: Concurrent parallel requests do not leak error state across threads")
    void testPatientServiceConcurrentErrorIsolation() throws Exception {
        // Stub DAO: offline, thread-safe, no database connections
        PatientDAO stubDao = new PatientDAO() {
            private final AtomicInteger idSeq = new AtomicInteger(100);

            @Override
            public boolean addPatient(Patient patient) {
                patient.setPatientId(idSeq.incrementAndGet());
                return true;
            }

            @Override
            public boolean updatePatient(Patient patient) {
                return true;
            }
        };

        PatientService singletonService = new PatientService(stubDao);

        int threadCount = 40;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                // Block until all threads are ready so they execute concurrently
                startLatch.await();

                int operationType = index % 5;
                switch (operationType) {
                    case 0 -> {
                        // Invalid name
                        ServiceResult<Patient> result = singletonService.createPatientResult(
                                "Invalid123Name", 30, "MALE", "O+", "None", "None", "9876543210");
                        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
                        assertTrue(result.getMessage().contains("letters and spaces"));
                        assertEquals(result.getMessage(), singletonService.getLastErrorMessage());
                    }
                    case 1 -> {
                        // Invalid age
                        ServiceResult<Patient> result = singletonService.createPatientResult(
                                "Valid Name", 150, "MALE", "O+", "None", "None", "9876543210");
                        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
                        assertTrue(result.getMessage().contains("Age must be between 1 and 120"));
                        assertEquals(result.getMessage(), singletonService.getLastErrorMessage());
                    }
                    case 2 -> {
                        // Invalid gender
                        ServiceResult<Patient> result = singletonService.createPatientResult(
                                "Valid Name", 25, "UNKNOWN_GENDER", "O+", "None", "None", "9876543210");
                        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
                        assertTrue(result.getMessage().contains("Allowed values: MALE, FEMALE, OTHER"));
                        assertEquals(result.getMessage(), singletonService.getLastErrorMessage());
                    }
                    case 3 -> {
                        // Invalid phone
                        ServiceResult<Patient> result = singletonService.createPatientResult(
                                "Valid Name", 25, "FEMALE", "A+", "None", "None", "123");
                        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
                        assertTrue(result.getMessage().contains("Must be 10 to 15 digits"));
                        assertEquals(result.getMessage(), singletonService.getLastErrorMessage());
                    }
                    case 4 -> {
                        // Valid patient
                        ServiceResult<Patient> result = singletonService.createPatientResult(
                                "Valid John", 28, "MALE", "B+", "None", "None", "9876543210");
                        assertTrue(result.isSuccess());
                        assertNotNull(result.getData());
                        assertTrue(result.getData().getPatientId() > 0);
                        assertNull(singletonService.getLastErrorMessage(),
                                "Successful operation must clean up thread-local error state");
                    }
                }
                return "OK";
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release all threads simultaneously

        for (Future<String> future : futures) {
            assertEquals("OK", future.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("EmergencyCaseService: Concurrent status updates and validations do not leak state")
    void testEmergencyCaseServiceConcurrentErrorIsolation() throws Exception {
        // Stub DAOs
        EmergencyCaseDAO stubCaseDao = new EmergencyCaseDAO() {
            private final ConcurrentHashMap<Integer, EmergencyCase> caseMap = new ConcurrentHashMap<>();
            private final AtomicInteger idSeq = new AtomicInteger(10);

            {
                // Prepopulate
                caseMap.put(1, new EmergencyCase(1, 1, "MG Road", "CARDIAC", "Chest pain", "CRITICAL", "REGISTERED"));
                caseMap.put(2, new EmergencyCase(2, 1, "Indiranagar", "TRAUMA", "Fracture", "HIGH", "DISPATCHED"));
                caseMap.put(3, new EmergencyCase(3, 1, "Koramangala", "RESPIRATORY", "Asthma", "MODERATE", "IN_TRANSIT"));
            }

            @Override
            public EmergencyCase getEmergencyCaseById(int caseId) {
                return caseMap.get(caseId);
            }

            @Override
            public boolean updateCaseStatusAtomic(int caseId, String expectedStatus, String targetStatus) {
                EmergencyCase ec = caseMap.get(caseId);
                if (ec == null) return false;
                synchronized (ec) {
                    if (ec.getCaseStatus().equalsIgnoreCase(expectedStatus)) {
                        ec.setCaseStatus(targetStatus);
                        return true;
                    }
                    return false;
                }
            }

            @Override
            public boolean addEmergencyCase(EmergencyCase emergencyCase) {
                emergencyCase.setCaseId(idSeq.incrementAndGet());
                caseMap.put(emergencyCase.getCaseId(), emergencyCase);
                return true;
            }
        };

        PatientDAO stubPatientDao = new PatientDAO() {
            @Override
            public Patient getPatientById(int patientId) {
                if (patientId == 999) return null;
                return new Patient(patientId, "Test Patient", 30, "MALE", "O+", "None", "None", "9876543210");
            }
        };

        EmergencyCaseService singletonService = new EmergencyCaseService(stubCaseDao, stubPatientDao);

        int threadCount = 30;
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                int operationType = index % 5;
                switch (operationType) {
                    case 0 -> {
                        // Invalid patient ID
                        ServiceResult<EmergencyCase> result = singletonService.createEmergencyCaseResult(
                                -1, "MG Road", "CARDIAC", "Chest pain");
                        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
                        assertTrue(result.getMessage().contains("Invalid patient ID"));
                        assertEquals(result.getMessage(), singletonService.getLastErrorMessage());
                    }
                    case 1 -> {
                        // Missing location
                        ServiceResult<EmergencyCase> result = singletonService.createEmergencyCaseResult(
                                1, "   ", "CARDIAC", "Chest pain");
                        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
                        assertTrue(result.getMessage().contains("Incident location cannot be empty"));
                        assertEquals(result.getMessage(), singletonService.getLastErrorMessage());
                    }
                    case 2 -> {
                        // Non-existent patient
                        ServiceResult<EmergencyCase> result = singletonService.createEmergencyCaseResult(
                                999, "MG Road", "CARDIAC", "Chest pain");
                        assertEquals(ServiceResult.Status.VALIDATION_ERROR, result.getStatus());
                        assertTrue(result.getMessage().contains("Patient with ID 999 not found"));
                        assertEquals(result.getMessage(), singletonService.getLastErrorMessage());
                    }
                    case 3 -> {
                        // Invalid status transition directly to COMPLETED from REGISTERED
                        ServiceResult<EmergencyCase> result = singletonService.updateCaseStatusAtomic(1, "COMPLETED");
                        // Can be CONFLICT or SUCCESS if another thread already transitioned it, but transition from REGISTERED to COMPLETED is conflict
                        assertEquals(ServiceResult.Status.CONFLICT, result.getStatus());
                        assertTrue(result.getMessage().contains("Invalid status transition")
                                || result.getMessage().contains("Concurrent update conflict"));
                        assertEquals(result.getMessage(), singletonService.getLastErrorMessage());
                    }
                    case 4 -> {
                        // Valid case creation
                        ServiceResult<EmergencyCase> result = singletonService.createEmergencyCaseResult(
                                1, "Whitefield", "TRAUMA", "Severe injury");
                        assertTrue(result.isSuccess());
                        assertNotNull(result.getData());
                        assertTrue(result.getData().getCaseId() > 0);
                        assertNull(singletonService.getLastErrorMessage(),
                                "Successful operation must clean up thread-local error state");
                    }
                }
                return "OK";
            }));
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();

        for (Future<String> future : futures) {
            assertEquals("OK", future.get(10, TimeUnit.SECONDS));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
}
