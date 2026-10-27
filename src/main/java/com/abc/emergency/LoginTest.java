package com.abc.emergency;

import com.abc.emergency.model.User;
import com.abc.emergency.service.LoginService;

public class LoginTest {

    public static void main(String[] args) {

        String testUsername = args.length > 0 ? args[0] : "synthetic_test_user";
        String testPassword = args.length > 1 ? args[1] : "synthetic_test_password";

        LoginService loginService = new LoginService();

        User user = loginService.login(testUsername, testPassword);

        System.out.println();

        if (user != null) {
            System.out.println("Login Test: SUCCESS");
            System.out.println("User ID: " + user.getUserId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("Role: " + user.getRole());

            System.out.println();
            System.out.println(
                    "Role Validation: "
                    + loginService.validateRole(user, user.getRole())
            );
        } else {
            System.out.println("Login Test: FAILED (Expected if synthetic user is not in database)");
        }
    }
}
