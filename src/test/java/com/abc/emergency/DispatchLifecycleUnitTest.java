package com.abc.emergency;

import com.abc.emergency.gui.DispatchFrame;
import com.abc.emergency.model.DispatchRecord;
import com.abc.emergency.service.DispatchService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DispatchLifecycleUnitTest {

    @Test
    void testAllowedStatusTransitions() {
        DispatchService service = new DispatchService();

        // From DISPATCHED
        assertTrue(service.isAllowedTransition("DISPATCHED", "EN_ROUTE"));
        assertTrue(service.isAllowedTransition("DISPATCHED", "CANCELLED"));
        assertFalse(service.isAllowedTransition("DISPATCHED", "ARRIVED"));
        assertFalse(service.isAllowedTransition("DISPATCHED", "COMPLETED"));
        assertFalse(service.isAllowedTransition("DISPATCHED", "DISPATCHED"));

        // From EN_ROUTE
        assertTrue(service.isAllowedTransition("EN_ROUTE", "ARRIVED"));
        assertTrue(service.isAllowedTransition("EN_ROUTE", "CANCELLED"));
        assertFalse(service.isAllowedTransition("EN_ROUTE", "DISPATCHED"));
        assertFalse(service.isAllowedTransition("EN_ROUTE", "COMPLETED"));
        assertFalse(service.isAllowedTransition("EN_ROUTE", "EN_ROUTE"));

        // From ARRIVED
        assertTrue(service.isAllowedTransition("ARRIVED", "COMPLETED"));
        assertTrue(service.isAllowedTransition("ARRIVED", "CANCELLED"));
        assertFalse(service.isAllowedTransition("ARRIVED", "DISPATCHED"));
        assertFalse(service.isAllowedTransition("ARRIVED", "EN_ROUTE"));
        assertFalse(service.isAllowedTransition("ARRIVED", "ARRIVED"));

        // From Terminal States (COMPLETED and CANCELLED)
        assertFalse(service.isAllowedTransition("COMPLETED", "DISPATCHED"));
        assertFalse(service.isAllowedTransition("COMPLETED", "EN_ROUTE"));
        assertFalse(service.isAllowedTransition("COMPLETED", "ARRIVED"));
        assertFalse(service.isAllowedTransition("COMPLETED", "CANCELLED"));

        assertFalse(service.isAllowedTransition("CANCELLED", "DISPATCHED"));
        assertFalse(service.isAllowedTransition("CANCELLED", "EN_ROUTE"));
        assertFalse(service.isAllowedTransition("CANCELLED", "ARRIVED"));
        assertFalse(service.isAllowedTransition("CANCELLED", "COMPLETED"));

        // Null and invalid inputs
        assertFalse(service.isAllowedTransition(null, "EN_ROUTE"));
        assertFalse(service.isAllowedTransition("DISPATCHED", null));
        assertFalse(service.isAllowedTransition("UNKNOWN", "ARRIVED"));
    }

    @Test
    void testStatusValidationWhitelist() {
        DispatchService service = new DispatchService();

        assertTrue(service.isValidStatus("DISPATCHED"));
        assertTrue(service.isValidStatus("EN_ROUTE"));
        assertTrue(service.isValidStatus("ARRIVED"));
        assertTrue(service.isValidStatus("COMPLETED"));
        assertTrue(service.isValidStatus("CANCELLED"));

        assertFalse(service.isValidStatus("UNKNOWN"));
        assertFalse(service.isValidStatus(""));
        assertFalse(service.isValidStatus(null));
    }

    @Test
    void testDispatchInputValidation() {
        DispatchService service = new DispatchService();

        // Invalid case ID
        assertFalse(service.dispatchAmbulance(-1, 1, 10));
        assertTrue(service.getLastErrorMessage().contains("Invalid Case ID"));

        assertFalse(service.dispatchAmbulance(0, 1, 10));
        assertTrue(service.getLastErrorMessage().contains("Invalid Case ID"));

        // Invalid ambulance ID
        assertFalse(service.dispatchAmbulance(1, -1, 10));
        assertTrue(service.getLastErrorMessage().contains("Invalid Case ID or Ambulance ID"));

        assertFalse(service.dispatchAmbulance(1, 0, 10));
        assertTrue(service.getLastErrorMessage().contains("Invalid Case ID or Ambulance ID"));

        // Negative ETA
        assertFalse(service.dispatchAmbulance(1, 1, -5));
        assertTrue(service.getLastErrorMessage().contains("ETA")
                || service.getLastErrorMessage().contains("estimated arrival time"));

        // Nonexistent Case ID
        assertFalse(service.dispatchAmbulance(999999, 1, 10));
        assertTrue(service.getLastErrorMessage().contains("does not exist")
                || service.getLastErrorMessage().toLowerCase().contains("database"));

        // Nonexistent Ambulance ID
        assertFalse(service.dispatchAmbulance(1, 999999, 10));
        assertTrue(service.getLastErrorMessage().contains("does not exist")
                || service.getLastErrorMessage().contains("already completed")
                || service.getLastErrorMessage().contains("active dispatch")
                || service.getLastErrorMessage().toLowerCase().contains("database"));
    }

    @Test
    void testUpdateStatusInputValidation() {
        DispatchService service = new DispatchService();

        // Invalid dispatch ID
        assertFalse(service.updateDispatchStatus(-1, "EN_ROUTE"));
        assertTrue(service.getLastErrorMessage().contains("Invalid Dispatch ID"));

        assertFalse(service.updateDispatchStatus(0, "EN_ROUTE"));
        assertTrue(service.getLastErrorMessage().contains("Invalid Dispatch ID"));

        // Invalid status
        assertFalse(service.updateDispatchStatus(1, "FLYING"));
        assertTrue(service.getLastErrorMessage().contains("Invalid dispatch status"));

        // Nonexistent dispatch ID
        assertFalse(service.updateDispatchStatus(999999, "EN_ROUTE"));
        assertTrue(service.getLastErrorMessage().contains("not found")
                || service.getLastErrorMessage().contains("does not exist")
                || service.getLastErrorMessage().toLowerCase().contains("database"));
    }

    @Test
    void testCompleteDispatchValidation() {
        DispatchService service = new DispatchService();

        // Nonexistent dispatch ID
        assertFalse(service.completeDispatch(999999, 1));
        assertTrue(service.getLastErrorMessage().contains("does not exist"));

        // Check against existing terminal dispatch ID 2 (which is in COMPLETED state in test seed data)
        DispatchRecord record = service.getDispatchById(2);
        if (record != null) {
            // Mismatched ambulance ID
            boolean mismatchResult = service.completeDispatch(2, 9999);
            assertFalse(mismatchResult);
            assertTrue(service.getLastErrorMessage().contains("does not match dispatched Ambulance ID"));

            // Terminal state protection: even with correct ambulance, cannot re-complete an already completed dispatch
            boolean terminalResult = service.completeDispatch(2, record.getAmbulanceId());
            assertFalse(terminalResult);
            assertTrue(service.getLastErrorMessage().contains("Invalid")
                    && service.getLastErrorMessage().contains("transition"));
        }
    }

    @Test
    void testDispatchFrameInitialization() {
        DispatchFrame frame = new DispatchFrame();
        assertNotNull(frame);
        assertEquals("Emergency Dispatch Management", frame.getTitle());
        frame.dispose();
    }
}
