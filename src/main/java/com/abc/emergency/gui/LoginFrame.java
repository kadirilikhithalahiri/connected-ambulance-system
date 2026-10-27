package com.abc.emergency.gui;

import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.model.User;
import com.abc.emergency.service.LoginService;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    private JTextField usernameField;
    private JPasswordField passwordField;
    private final UserDAO userDAO;
    private final LoginService loginService;

    public LoginFrame() {
        this(new LoginService(), new UserDAO());
    }

    public LoginFrame(LoginService loginService, UserDAO userDAO) {
        this.loginService = loginService != null ? loginService : new LoginService();
        this.userDAO = userDAO != null ? userDAO : new UserDAO();

        setTitle("ABC Emergency Medical Services");
        setSize(500, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(
                BorderFactory.createEmptyBorder(30, 40, 30, 40)
        );

        JLabel titleLabel = new JLabel(
                "CONNECTED AMBULANCE SYSTEM",
                SwingConstants.CENTER
        );
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));

        JLabel subtitleLabel = new JLabel(
                "Emergency Response Management System",
                SwingConstants.CENTER
        );
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        JPanel headingPanel = new JPanel(new GridLayout(2, 1));
        headingPanel.add(titleLabel);
        headingPanel.add(subtitleLabel);

        JPanel formPanel = new JPanel(
                new GridLayout(2, 2, 10, 15)
        );

        JLabel usernameLabel = new JLabel("Username:");
        JLabel passwordLabel = new JLabel("Password:");

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        JButton loginButton = new JButton("Login");
        JButton clearButton = new JButton("Clear");

        JPanel buttonPanel = new JPanel(
                new FlowLayout(FlowLayout.CENTER, 15, 5)
        );

        buttonPanel.add(loginButton);
        buttonPanel.add(clearButton);

        mainPanel.add(headingPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        loginButton.addActionListener(e -> login());
        clearButton.addActionListener(e -> clearFields());
        passwordField.addActionListener(e -> login());
    }

    private void login() {

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Username cannot be empty.",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Password cannot be empty.",
                    "Login Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        User user = loginService.login(username, password);

        if (user == null) {
            User existingUser = userDAO.getUserByUsername(username);
            if (existingUser != null && !existingUser.isActive()) {
                JOptionPane.showMessageDialog(
                        this,
                        "This user account is inactive.",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Invalid username or password.",
                        "Login Failed",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            return;
        }

        JOptionPane.showMessageDialog(
                this,
                "Login successful.\nWelcome, " + user.getFullName() + "!",
                "Login Success",
                JOptionPane.INFORMATION_MESSAGE
        );

        dispose();

        new DashboardFrame(user).setVisible(true);
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        usernameField.requestFocus();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
