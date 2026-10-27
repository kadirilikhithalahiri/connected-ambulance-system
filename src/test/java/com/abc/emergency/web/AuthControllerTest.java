package com.abc.emergency.web;

import com.abc.emergency.config.WebSecurityConfig;
import com.abc.emergency.dto.LoginRequest;
import com.abc.emergency.model.User;
import com.abc.emergency.service.LoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {AuthController.class, GlobalExceptionHandler.class})
@Import({WebSecurityConfig.class, AuthControllerTest.TestConfig.class})
public class AuthControllerTest {

    @RestController
    static class TestRoleController {
        @GetMapping("/api/admin/test")
        public String adminEndpoint() {
            return "ADMIN_OK";
        }

        @GetMapping("/api/dispatcher/test")
        public String dispatcherEndpoint() {
            return "DISPATCHER_OK";
        }
    }

    static class FakeLoginService extends LoginService {
        int lastLoggedOutUserId = -1;

        @Override
        public User login(String username, String password) {
            if ("paramedic01".equals(username) && "SecurePass#123".equals(password)) {
                return new User(1, "paramedic01", "$2a$12$someSecretHashInDB1234567890", "Rahul Kumar", "PARAMEDIC", "9876543210", true);
            }
            if ("doctor01".equals(username) && "DocPass#123".equals(password)) {
                return new User(2, "doctor01", "$2a$12$hash", "Dr. Priya", "EMERGENCY_DOCTOR", "9123456780", true);
            }
            // Inactive user simulation
            if ("inactiveUser".equals(username)) {
                return null;
            }
            return null;
        }

        @Override
        public boolean logout(int userId) {
            this.lastLoggedOutUserId = userId;
            return true;
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public LoginService loginService() {
            return new FakeLoginService();
        }

        @Bean
        public TestRoleController testRoleController() {
            return new TestRoleController();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoginService loginService;

    private FakeLoginService fakeLoginService;

    @BeforeEach
    void setUp() {
        fakeLoginService = (FakeLoginService) loginService;
        fakeLoginService.lastLoggedOutUserId = -1;
    }

    @Test
    void testUnauthenticatedAccessToMeReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required. Please sign in."));
    }

    @Test
    void testUnknownApiRouteDeniesUnauthenticatedAccessByDefaultForSafety() throws Exception {
        mockMvc.perform(get("/api/unknown/route"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required. Please sign in."));
    }

    @Test
    void testSuccessfulLoginCreatesSecurityContextAndDoesNotExposePasswordHash() throws Exception {
        LoginRequest request = new LoginRequest("paramedic01", "SecurePass#123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.username").value("paramedic01"))
                .andExpect(jsonPath("$.data.fullName").value("Rahul Kumar"))
                .andExpect(jsonPath("$.data.role").value("PARAMEDIC"))
                .andExpect(jsonPath("$.data.phone").value("9876543210"))
                .andExpect(jsonPath("$.data.active").value(true))
                // CRITICAL SECURITY ASSERTION: Password hash must NEVER be present in response
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andReturn();

        // Verify genuine Spring Security context created in session
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession();
        assertNotNull(session);
        SecurityContext secContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertNotNull(secContext);
        assertNotNull(secContext.getAuthentication());
        assertTrue(secContext.getAuthentication().isAuthenticated());
        assertEquals("ROLE_PARAMEDIC", secContext.getAuthentication().getAuthorities().iterator().next().getAuthority());
        assertTrue(secContext.getAuthentication().getPrincipal() instanceof User);
    }

    @Test
    void testFailedLoginLeavesRequestUnauthenticated() throws Exception {
        LoginRequest request = new LoginRequest("unknownUser", "wrongPassword");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password, or account inactive."))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn();

        // Must not establish an authenticated security context
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        if (session != null) {
            SecurityContext secContext = (SecurityContext) session.getAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
            );
            assertNull(secContext);
        }
    }

    @Test
    void testInactiveAccountRejectedAtLogin() throws Exception {
        LoginRequest request = new LoginRequest("inactiveUser", "anyPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password, or account inactive."));
    }

    @Test
    void testBlankCredentialsReturns400ValidationError() throws Exception {
        LoginRequest blankRequest = new LoginRequest("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed."));
    }

    @Test
    void testSessionPreservedAndMeReturnsUserFromSecurityContext() throws Exception {
        LoginRequest request = new LoginRequest("doctor01", "DocPass#123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession();

        // Query /api/auth/me using the established session
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.username").value("doctor01"))
                .andExpect(jsonPath("$.data.role").value("EMERGENCY_DOCTOR"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void testCsrfProtectionEnforcedOnLogout() throws Exception {
        // State-changing POST /api/auth/logout without CSRF must fail with 403 Forbidden
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "dispatcher01", roles = "DISPATCHER")
    void testLogoutWithCsrfInvalidatesSessionAndLogsAudit() throws Exception {
        User mockUser = new User(3, "dispatcher01", "$2a$12$hash", "Suresh", "DISPATCHER", "9988776655", true);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", mockUser);

        mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Logout successful. Session invalidated."));

        // Verify logout timestamp logging was called with the trusted user ID
        assertEquals(3, fakeLoginService.lastLoggedOutUserId);
        assertTrue(session.isInvalid());
    }

    @Test
    @WithMockUser(roles = "PARAMEDIC")
    void testRoleBasedAccessDeniedForInsufficientRole() throws Exception {
        // PARAMEDIC cannot access /api/admin/**
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied. Insufficient role permissions."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testRoleBasedAccessAllowedForAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("ADMIN_OK"));
    }

    @Test
    @WithMockUser(roles = "DISPATCHER")
    void testRoleBasedAccessAllowedForDispatcher() throws Exception {
        mockMvc.perform(get("/api/dispatcher/test"))
                .andExpect(status().isOk())
                .andExpect(content().string("DISPATCHER_OK"));
    }

    @Test
    void testCsrfEndpointAccessiblePublicly() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"));
    }
}
