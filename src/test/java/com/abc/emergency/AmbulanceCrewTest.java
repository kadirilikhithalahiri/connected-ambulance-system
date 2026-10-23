package com.abc.emergency;

import com.abc.emergency.dao.AmbulanceCrewDAO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AmbulanceCrewTest {

    @Test
    void testAmbulanceCrewAssignment() {

        AmbulanceCrewDAO crewDAO = new AmbulanceCrewDAO();

        boolean result = crewDAO.addCrewMember(
                1,
                1,
                "PARAMEDIC"
        );

        assertTrue(result);

        List<String> crewMembers =
                crewDAO.getCrewMembersByAmbulance(1);

        assertFalse(crewMembers.isEmpty());

        System.out.println("Ambulance Crew Test: SUCCESS");
        System.out.println("Ambulance ID: 1");
        System.out.println("Assigned User ID: 1");
        System.out.println("Crew Role: PARAMEDIC");
        System.out.println("Crew Members: " + crewMembers.size());
    }
}