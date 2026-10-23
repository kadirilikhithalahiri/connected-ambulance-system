package com.abc.emergency;

import com.abc.emergency.service.UserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class UserServiceTest {

    @Test
    void testInvalidUsername() {

        UserService service = new UserService();

        boolean result = service.registerUser(
                "ab",
                "password123",
                "Rahul Kumar",
                "PARAMEDIC",
                "9876543210"
        );

        assertFalse(result);
    }

    @Test
    void testInvalidPassword() {

        UserService service = new UserService();

        boolean result = service.registerUser(
                "rahul123",
                "123",
                "Rahul Kumar",
                "PARAMEDIC",
                "9876543210"
        );

        assertFalse(result);
    }

    @Test
    void testInvalidPhone() {

        UserService service = new UserService();

        boolean result = service.registerUser(
                "rahul123",
                "password123",
                "Rahul Kumar",
                "PARAMEDIC",
                "98765"
        );

        assertFalse(result);
    }

    @Test
    void testInvalidName() {

        UserService service = new UserService();

        boolean result = service.registerUser(
                "rahul123",
                "password123",
                "Rahul123",
                "PARAMEDIC",
                "9876543210"
        );

        assertFalse(result);
    }
}