package com.abc.emergency;

import com.abc.emergency.service.LoginService;

public class LogoutTest {

    public static void main(String[] args) {

        LoginService loginService = new LoginService();

        boolean result = loginService.logout(1);

        System.out.println();
        System.out.println("Logout Test: " + (result ? "SUCCESS" : "FAILED"));
    }
}