package com.abc.emergency.util;

public class ValidationUtil {

    public static boolean isValidName(String name) {
        return name != null
                && !name.trim().isEmpty()
                && name.matches("[a-zA-Z ]+");
    }

    public static boolean isValidAge(int age) {
        return age > 0 && age <= 120;
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\d{10,15}");
    }

    public static boolean isValidUsername(String username) {
        return username != null
                && username.matches("[a-zA-Z0-9_]{4,50}");
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidHeartRate(int heartRate) {
        return heartRate >= 20 && heartRate <= 250;
    }

    public static boolean isValidBloodPressure(int systolic, int diastolic) {
        return systolic >= 40 && systolic <= 250
                && diastolic >= 20 && diastolic <= 150;
    }

    public static boolean isValidSpo2(double spo2) {
        return spo2 >= 0 && spo2 <= 100;
    }

    public static boolean isValidTemperature(double temperature) {
        return temperature >= 25 && temperature <= 45;
    }

    public static boolean isValidRespiratoryRate(int respiratoryRate) {
        return respiratoryRate >= 5 && respiratoryRate <= 80;
    }

    public static boolean isValidCoordinates(double latitude, double longitude) {
        return latitude >= -90 && latitude <= 90
                && longitude >= -180 && longitude <= 180;
    }

    public static boolean isValidId(int id) {
        return id > 0;
    }
}