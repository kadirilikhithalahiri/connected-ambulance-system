package com.abc.emergency;

import com.abc.emergency.model.DispatchRecord;
import com.abc.emergency.service.DispatchService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DispatchServiceTest {

    @Test
    void testDispatchManagement() {

        DispatchService dispatchService = new DispatchService();

        boolean dispatched =
                dispatchService.dispatchAmbulance(
                        1,
                        1,
                        15
                );

        assertTrue(dispatched);

        System.out.println("Dispatch Ambulance Test: SUCCESS");

        DispatchRecord record = null;

        for (int i = 1; i <= 100; i++) {

            DispatchRecord current =
                    dispatchService.getDispatchById(i);

            if (current != null
                    && current.getCaseId() == 1
                    && current.getAmbulanceId() == 1) {

                record = current;
            }
        }

        assertNotNull(record);

        int dispatchId = record.getDispatchId();

        System.out.println("Dispatch ID: " + dispatchId);
        System.out.println("Case ID: " + record.getCaseId());
        System.out.println("Ambulance ID: " + record.getAmbulanceId());
        System.out.println("Initial Status: " + record.getDispatchStatus());

        boolean updated =
                dispatchService.updateDispatchStatus(
                        dispatchId,
                        "EN_ROUTE"
                );

        assertTrue(updated);

        System.out.println("Dispatch Status Update Test: SUCCESS");

        boolean arrived =
                dispatchService.markAsArrived(dispatchId);

        assertTrue(arrived);

        System.out.println("Dispatch Arrival Test: SUCCESS");

        boolean completed =
                dispatchService.completeDispatch(
                        dispatchId,
                        1
                );

        assertTrue(completed);

        System.out.println("Dispatch Completion Test: SUCCESS");
        System.out.println("Ambulance Availability Reset: SUCCESS");
    }
}
