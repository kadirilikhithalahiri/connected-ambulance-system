package com.abc.emergency;

import com.abc.emergency.service.UserService;

public class UserRegistrationTest {

    public static void main(String[] args) {

        if (args.length == 0) {
            System.out.println("Safety Guard: UserRegistrationTest requires explicit CLI arguments to prevent accidental database writes.");
            System.out.println("Usage: java UserRegistrationTest <username> <password> <fullName> <role> <phone>");
            return;
        }

        String testUsername = args[0];
        String testPassword = args.length > 1 ? args[1] : "SyntheticPass@123";
        String testName = args.length > 2 ? args[2] : "Synthetic Paramedic";
        String testRole = args.length > 3 ? args[3] : "PARAMEDIC";
        String testPhone = args.length > 4 ? args[4] : "9876543210";

        UserService userService = new UserService();

        boolean result = userService.registerUser(
                testUsername,
                testPassword,
                testName,
                testRole,
                testPhone
        );

        System.out.println();
        System.out.println("Registration Result: " + result);
    }
}
