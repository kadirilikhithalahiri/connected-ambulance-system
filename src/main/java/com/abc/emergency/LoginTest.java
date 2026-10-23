package com.abc.emergency;

import com.abc.emergency.model.User;
import com.abc.emergency.service.LoginService;

public class LoginTest {

    public static void main(String[] args) {

        LoginService loginService = new LoginService();

        User user = loginService.login(
                "paramedic01",
                "password123"
        );

        System.out.println();

        if (user != null) {
            System.out.println("Login Test: SUCCESS");
            System.out.println("User ID: " + user.getUserId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("Name: " + user.getFullName());
            System.out.println("Role: " + user.getRole());

            System.out.println();
            System.out.println(
                    "Role Validation: "
                    + loginService.validateRole(user, "PARAMEDIC")
            );
        } else {
            System.out.println("Login Test: FAILED");
        }
    }
}