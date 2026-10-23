package com.abc.emergency;

import com.abc.emergency.util.ValidationUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ValidationUtilTest {

    @Test
    void testValidName() {
        assertTrue(ValidationUtil.isValidName("Rahul Kumar"));
        assertFalse(ValidationUtil.isValidName("Rahul123"));
    }

    @Test
    void testValidAge() {
        assertTrue(ValidationUtil.isValidAge(25));
        assertFalse(ValidationUtil.isValidAge(0));
        assertFalse(ValidationUtil.isValidAge(121));
    }

    @Test
    void testValidPhone() {
        assertTrue(ValidationUtil.isValidPhone("9876543210"));
        assertFalse(ValidationUtil.isValidPhone("98765"));
    }

    @Test
    void testValidHeartRate() {
        assertTrue(ValidationUtil.isValidHeartRate(75));
        assertFalse(ValidationUtil.isValidHeartRate(10));
        assertFalse(ValidationUtil.isValidHeartRate(300));
    }

    @Test
    void testValidBloodPressure() {
        assertTrue(ValidationUtil.isValidBloodPressure(120, 80));
        assertFalse(ValidationUtil.isValidBloodPressure(20, 10));
    }

    @Test
    void testValidSpo2() {
        assertTrue(ValidationUtil.isValidSpo2(98));
        assertFalse(ValidationUtil.isValidSpo2(105));
    }

    @Test
    void testValidTemperature() {
        assertTrue(ValidationUtil.isValidTemperature(37.0));
        assertFalse(ValidationUtil.isValidTemperature(50));
    }

    @Test
    void testValidRespiratoryRate() {
        assertTrue(ValidationUtil.isValidRespiratoryRate(18));
        assertFalse(ValidationUtil.isValidRespiratoryRate(100));
    }

    @Test
    void testValidCoordinates() {
        assertTrue(ValidationUtil.isValidCoordinates(12.9716, 77.5946));
        assertFalse(ValidationUtil.isValidCoordinates(100, 200));
    }

    @Test
    void testValidId() {
        assertTrue(ValidationUtil.isValidId(1));
        assertFalse(ValidationUtil.isValidId(0));
        assertFalse(ValidationUtil.isValidId(-1));
    }
}