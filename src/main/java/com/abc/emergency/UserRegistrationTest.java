package com.abc.emergency;

import com.abc.emergency.service.UserService;

public class UserRegistrationTest {

    public static void main(String[] args) {

        UserService userService = new UserService();

        boolean result = userService.registerUser(
                "paramedic01",
                "password123",
                "Rahul Kumar",
                "PARAMEDIC",
                "9876543210"
        );

        System.out.println();
        System.out.println("Registration Result: " + result);
    }
}