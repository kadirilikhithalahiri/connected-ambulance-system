package com.abc.emergency;

import com.abc.emergency.dao.UserDAO;
import com.abc.emergency.model.User;
import com.abc.emergency.service.LoginService;
import com.abc.emergency.service.UserService;
import com.abc.emergency.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityAuthenticationUnitTest {

    private MockUserDAO mockUserDAO;
    private UserService userService;
    private TestableLoginService loginService;

    // In-memory mock DAO to isolate unit tests completely from live MySQL
    static class MockUserDAO extends UserDAO {
        final Map<String, User> userMap = new HashMap<>();
        boolean simulateUpdateFailure = false;
        boolean conditionalUpdateInvoked = false;

        @Override
        public boolean addUser(User user) {
            if (userMap.containsKey(user.getUsername())) {
                return false;
            }
            userMap.put(user.getUsername(), user);
            return true;
        }

        @Override
        public User getUserByUsername(String username) {
            User existing = userMap.get(username);
            if (existing == null) {
                return null;
            }
            // Return copy
            return new User(
                    existing.getUserId(),
                    existing.getUsername(),
                    existing.getPasswordHash(),
                    existing.getFullName(),
                    existing.getRole(),
                    existing.getPhone(),
                    existing.isActive()
            );
        }

        @Override
        public User getUserById(int userId) {
            for (User u : userMap.values()) {
                if (u.getUserId() == userId) {
                    return new User(
                            u.getUserId(),
                            u.getUsername(),
                            u.getPasswordHash(),
                            u.getFullName(),
                            u.getRole(),
                            u.getPhone(),
                            u.isActive()
                    );
                }
            }
            return null;
        }

        @Override
        public boolean updatePasswordHashConditional(int userId, String expectedOldHashOrPlain, String newHash) {
            conditionalUpdateInvoked = true;
            if (simulateUpdateFailure) {
                return false;
            }
            for (User u : userMap.values()) {
                if (u.getUserId() == userId && u.getPasswordHash().equals(expectedOldHashOrPlain)) {
                    u.setPasswordHash(newHash);
                    return true;
                }
            }
            return false;
        }
    }

    // Subclass to avoid hitting live Login_Log table
    static class TestableLoginService extends LoginService {
        int lastLoggedUserId = -1;
        String lastLoggedStatus = null;
        int logCallCount = 0;

        TestableLoginService(UserDAO userDAO) {
            super(userDAO);
        }

        @Override
        protected void logLoginAttempt(int userId, String status) {
            this.lastLoggedUserId = userId;
            this.lastLoggedStatus = status;
            this.logCallCount++;
        }
    }

    @BeforeEach
    void setUp() {
        mockUserDAO = new MockUserDAO();
        userService = new UserService(mockUserDAO);
        loginService = new TestableLoginService(mockUserDAO);
    }

    @Test
    void testRegistrationStoresBCryptHashNotPlaintext() {
        String rawPassword = "SecurePassword@123";
        boolean registered = userService.registerUser(
                "medic_test",
                rawPassword,
                "Medic Tester",
                "PARAMEDIC",
                "9876543210"
        );

        assertTrue(registered);

        User savedUser = mockUserDAO.getUserByUsername("medic_test");
        assertNotNull(savedUser);

        // Stored value must be a valid 60-character BCrypt hash
        assertTrue(PasswordUtil.isBCryptHash(savedUser.getPasswordHash()));

        // Stored value must NOT be the raw plaintext
        assertNotEquals(rawPassword, savedUser.getPasswordHash());

        // Password verification must succeed with raw password against stored hash
        assertTrue(PasswordUtil.checkPassword(rawPassword, savedUser.getPasswordHash()));
    }

    @Test
    void testSuccessfulLegacyLoginMigratesPasswordToBCrypt() {
        String legacyPlaintext = "paramedic123";
        User legacyUser = new User(1, "legacy_user", legacyPlaintext, "Legacy Paramedic", "PARAMEDIC", "9876543210", true);
        mockUserDAO.userMap.put("legacy_user", legacyUser);

        // Verify initial state is legacy plaintext
        assertFalse(PasswordUtil.isBCryptHash(legacyUser.getPasswordHash()));

        // Perform login with correct legacy password
        User loggedInUser = loginService.login("legacy_user", legacyPlaintext);

        // Login must succeed
        assertNotNull(loggedInUser);
        assertEquals("SUCCESS", loginService.lastLoggedStatus);
        assertEquals(1, loginService.lastLoggedUserId);
        assertEquals(1, loginService.logCallCount);

        // Must have invoked conditional update
        assertTrue(mockUserDAO.conditionalUpdateInvoked);

        // Database copy must now be migrated to BCrypt hash
        User updatedInDB = mockUserDAO.getUserByUsername("legacy_user");
        assertTrue(PasswordUtil.isBCryptHash(updatedInDB.getPasswordHash()));
        assertTrue(PasswordUtil.checkPassword(legacyPlaintext, updatedInDB.getPasswordHash()));

        // The returned in-memory user must also reflect the new BCrypt hash
        assertTrue(PasswordUtil.isBCryptHash(loggedInUser.getPasswordHash()));
    }

    @Test
    void testFailedLoginLeavesStoredValueUnchanged() {
        String legacyPlaintext = "paramedic123";
        User legacyUser = new User(2, "legacy_user_2", legacyPlaintext, "Legacy Medic 2", "PARAMEDIC", "9876543210", true);
        mockUserDAO.userMap.put("legacy_user_2", legacyUser);

        // Login with wrong password
        User loggedInUser = loginService.login("legacy_user_2", "wrong_password");

        // Login must fail
        assertNull(loggedInUser);
        assertEquals("FAILED", loginService.lastLoggedStatus);
        assertEquals(2, loginService.lastLoggedUserId);
        assertEquals(1, loginService.logCallCount);

        // Conditional update must NOT be invoked
        assertFalse(mockUserDAO.conditionalUpdateInvoked);

        // Stored password in DB must remain legacy plaintext unchanged
        User userInDB = mockUserDAO.getUserByUsername("legacy_user_2");
        assertEquals(legacyPlaintext, userInDB.getPasswordHash());
    }

    @Test
    void testDatabaseMigrationFailureAbortsLoginSafely() {
        String legacyPlaintext = "paramedic123";
        User legacyUser = new User(3, "legacy_user_3", legacyPlaintext, "Legacy Medic 3", "PARAMEDIC", "9876543210", true);
        mockUserDAO.userMap.put("legacy_user_3", legacyUser);

        // Simulate database update failure (e.g., DB disconnected during update)
        mockUserDAO.simulateUpdateFailure = true;

        User loggedInUser = loginService.login("legacy_user_3", legacyPlaintext);

        // Login must abort safely rather than reporting a false success
        assertNull(loggedInUser);
        assertEquals("FAILED", loginService.lastLoggedStatus);
        assertEquals(1, loginService.logCallCount);
    }

    @Test
    void testStaleCredentialsRejectedWhenConditionalUpdateFails() {
        String legacyPlaintext = "paramedic123";
        User legacyUser = new User(10, "concurrent_user", legacyPlaintext, "Concurrent Paramedic", "PARAMEDIC", "9876543210", true);
        mockUserDAO.userMap.put("concurrent_user", legacyUser);

        // Simulate that before the update executed, another admin changed the password in the DB
        mockUserDAO.simulateUpdateFailure = true;
        mockUserDAO.userMap.get("concurrent_user").setPasswordHash(PasswordUtil.hashPassword("BrandNewPassword#999"));

        User loggedInUser = loginService.login("concurrent_user", legacyPlaintext);

        // Login with old stale credentials must fail
        assertNull(loggedInUser);
        assertEquals("FAILED", loginService.lastLoggedStatus);
        assertEquals(1, loginService.logCallCount);
    }

    @Test
    void testConcurrentLoginPermittedIfOtherSessionMigratedSamePassword() {
        String legacyPlaintext = "paramedic123";
        User legacyUser = new User(11, "concurrent_user_2", legacyPlaintext, "Concurrent Paramedic 2", "PARAMEDIC", "9876543210", true);
        mockUserDAO.userMap.put("concurrent_user_2", legacyUser);

        // Simulate that during login, another concurrent request already successfully migrated this exact password to BCrypt
        String alreadyMigratedHash = PasswordUtil.hashPassword(legacyPlaintext);
        mockUserDAO.simulateUpdateFailure = true;
        mockUserDAO.userMap.get("concurrent_user_2").setPasswordHash(alreadyMigratedHash);

        User loggedInUser = loginService.login("concurrent_user_2", legacyPlaintext);

        // Login succeeds because the reloaded DB hash matches the entered password
        assertNotNull(loggedInUser);
        assertEquals("SUCCESS", loginService.lastLoggedStatus);
        assertEquals(alreadyMigratedHash, loggedInUser.getPasswordHash());
        assertEquals(1, loginService.logCallCount);
    }

    @Test
    void testInactiveAccountRejected() {
        User inactiveUser = new User(4, "inactive_user", "password123", "Inactive Paramedic", "PARAMEDIC", "9876543210", false);
        mockUserDAO.userMap.put("inactive_user", inactiveUser);

        User loggedInUser = loginService.login("inactive_user", "password123");

        assertNull(loggedInUser);
        assertEquals("FAILED", loginService.lastLoggedStatus);
        assertEquals(4, loginService.lastLoggedUserId);
        assertEquals(1, loginService.logCallCount);
    }

    @Test
    void testAlreadyHashedUserDoesNotRehashOnLogin() {
        String password = "KnownPassword#2026";
        String existingHash = PasswordUtil.hashPassword(password);
        User hashedUser = new User(5, "hashed_user", existingHash, "Hashed Medic", "PARAMEDIC", "9876543210", true);
        mockUserDAO.userMap.put("hashed_user", hashedUser);

        User loggedInUser = loginService.login("hashed_user", password);

        assertNotNull(loggedInUser);
        assertEquals("SUCCESS", loginService.lastLoggedStatus);
        assertEquals(1, loginService.logCallCount);

        // Should NOT trigger any migration or DB update
        assertFalse(mockUserDAO.conditionalUpdateInvoked);
        assertEquals(existingHash, mockUserDAO.getUserByUsername("hashed_user").getPasswordHash());
    }

    @Test
    void testRoleValidation() {
        User user = new User(6, "doc_user", "hash", "Doctor Smith", "EMERGENCY_DOCTOR", "9876543210", true);

        assertTrue(loginService.validateRole(user, "EMERGENCY_DOCTOR"));
        assertTrue(loginService.validateRole(user, "emergency_doctor")); // Case insensitive
        assertFalse(loginService.validateRole(user, "PARAMEDIC"));
        assertFalse(loginService.validateRole(null, "EMERGENCY_DOCTOR"));
        assertFalse(loginService.validateRole(user, null));
    }
}
