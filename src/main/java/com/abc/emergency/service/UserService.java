package com.abc.emergency.service;

import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.model.User;
import com.abc.emergency.util.PasswordUtil;
import com.abc.emergency.util.ValidationUtil;

public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this(new UserDAO());
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    public boolean registerUser(String username, String password,
                                String fullName, String role, String phone) {

        if (!ValidationUtil.isValidUsername(username)) {
            System.out.println("Invalid username.");
            return false;
        }

        if (!ValidationUtil.isValidPassword(password)) {
            System.out.println("Password must contain at least 6 characters.");
            return false;
        }

        if (!ValidationUtil.isValidName(fullName)) {
            System.out.println("Invalid full name.");
            return false;
        }

        if (!ValidationUtil.isValidPhone(phone)) {
            System.out.println("Invalid phone number.");
            return false;
        }

        if (role == null || role.trim().isEmpty()) {
            System.out.println("Role cannot be empty.");
            return false;
        }

        if (userDAO.getUserByUsername(username) != null) {
            System.out.println("Username already exists.");
            return false;
        }

        User user = new User();

        user.setUsername(username);
        user.setPasswordHash(PasswordUtil.hashPassword(password));
        user.setFullName(fullName);
        user.setRole(role.toUpperCase());
        user.setPhone(phone);
        user.setActive(true);

        boolean result = userDAO.addUser(user);

        if (result) {
            System.out.println("User registered successfully.");
        } else {
            System.out.println("User registration failed.");
        }

        return result;
    }
}
