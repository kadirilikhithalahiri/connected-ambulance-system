package com.abc.emergency.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "DB_URL=jdbc:mysql://localhost:3306/connected_ambulance_test",
        "DB_USER=amb_test_app",
        "DB_PASSWORD=OfflineTestPassword123!"
})
@AutoConfigureMockMvc
public class WebStaticResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET / routes to index.html welcome page")
    void getRootReturnsIndexHtml() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    @DisplayName("GET /index.html directly returns HTML application shell with 200 OK")
    void getIndexHtmlReturnsOk() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("<title>Connected Ambulance System</title>")))
                .andExpect(content().string(containsString("id=\"appShell\"")))
                .andExpect(content().string(containsString("id=\"viewPatients\"")))
                .andExpect(content().string(containsString("id=\"viewCases\"")))
                .andExpect(content().string(containsString("id=\"viewAmbulances\"")))
                .andExpect(content().string(containsString("id=\"viewDispatches\"")))
                .andExpect(content().string(containsString("id=\"viewReports\"")))
                .andExpect(content().string(containsString("id=\"reportDriverScopeNotice\"")))
                .andExpect(content().string(containsString("Metrics reflect your assigned cases and vehicle, not the entire fleet.")))
                .andExpect(content().string(containsString("Mission Outcomes &amp; Planned Dispatch ETAs")))
                .andExpect(content().string(containsString("id=\"caseStatusModal\"")))
                .andExpect(content().string(containsString("id=\"ambulanceDetailsModal\"")))
                .andExpect(content().string(containsString("id=\"ambulanceLocationModal\"")))
                .andExpect(content().string(containsString("id=\"dispatchCreateModal\"")))
                .andExpect(content().string(containsString("id=\"dispatchDetailsModal\"")))
                .andExpect(content().string(containsString("id=\"dispatchStatusModal\"")))
                .andExpect(content().string(containsString("id=\"statusModalDispatchNotice\"")))
                .andExpect(content().string(containsString("id=\"statusModalGoDispatchesBtn\"")))
                .andExpect(content().string(containsString("id=\"loginView\"")));
    }

    @Test
    @DisplayName("GET /css/styles.css returns CSS stylesheet with 200 OK")
    void getStylesCssReturnsOk() throws Exception {
        mockMvc.perform(get("/css/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(containsString("--navy-700")))
                .andExpect(content().string(containsString("--red-500")));
    }

    @Test
    @DisplayName("GET /js/api.js returns JavaScript API wrapper with 200 OK and clearCsrf function")
    void getApiJsReturnsOk() throws Exception {
        mockMvc.perform(get("/js/api.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"))
                .andExpect(content().string(containsString("EmergencyApi")))
                .andExpect(content().string(containsString("clearCsrf")));
    }

    @Test
    @DisplayName("GET /js/auth.js returns JavaScript Auth manager with 200 OK and clearCsrf invocation")
    void getAuthJsReturnsOk() throws Exception {
        mockMvc.perform(get("/js/auth.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"))
                .andExpect(content().string(containsString("EmergencyAuth")))
                .andExpect(content().string(containsString("clearCsrf")));
    }

    @Test
    @DisplayName("GET /js/patients.js returns Patient module with 200 OK")
    void getPatientsJsReturnsOk() throws Exception {
        mockMvc.perform(get("/js/patients.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"))
                .andExpect(content().string(containsString("EmergencyPatients")))
                .andExpect(content().string(containsString("loadPatients")));
    }

    @Test
    @DisplayName("GET /js/cases.js returns Emergency Cases module with 200 OK and accessibility focus trapping without dead references or deadlock transition")
    void getCasesJsReturnsOk() throws Exception {
        mockMvc.perform(get("/js/cases.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"))
                .andExpect(content().string(containsString("EmergencyCases")))
                .andExpect(content().string(containsString("loadCases")))
                .andExpect(content().string(containsString("trapFocus")))
                .andExpect(content().string(containsString("releaseFocusTrap")))
                .andExpect(content().string(containsString("statusModalDispatchNotice")))
                .andExpect(content().string(containsString("statusModalGoDispatchesBtn")))
                .andExpect(content().string(not(containsString("detailTransitionSection"))))
                .andExpect(content().string(not(containsString("Dispatch Ambulance (DISPATCHED)"))));
    }

    @Test
    @DisplayName("GET /js/ambulances.js returns Ambulance Fleet module with 200 OK and accessibility focus trapping")
    void getAmbulancesJsReturnsOk() throws Exception {
        mockMvc.perform(get("/js/ambulances.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"))
                .andExpect(content().string(containsString("EmergencyAmbulances")))
                .andExpect(content().string(containsString("loadAmbulances")))
                .andExpect(content().string(containsString("trapFocus")))
                .andExpect(content().string(containsString("releaseFocusTrap")))
                .andExpect(content().string(containsString("validateLocationPayload")));
    }

    @Test
    @DisplayName("GET /js/dispatches.js returns Dispatches module with 200 OK and accessibility focus trapping")
    void getDispatchesJsReturnsOk() throws Exception {
        mockMvc.perform(get("/js/dispatches.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"))
                .andExpect(content().string(containsString("EmergencyDispatches")))
                .andExpect(content().string(containsString("loadDispatches")))
                .andExpect(content().string(containsString("trapFocus")))
                .andExpect(content().string(containsString("releaseFocusTrap")))
                .andExpect(content().string(containsString("validateDispatchPayload")));
    }

    @Test
    @DisplayName("GET /js/reports.js returns Reports & Analytics module with 200 OK and accurate terminology")
    void getReportsJsReturnsOk() throws Exception {
        mockMvc.perform(get("/js/reports.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"))
                .andExpect(content().string(containsString("EmergencyReports")))
                .andExpect(content().string(containsString("loadReports")))
                .andExpect(content().string(containsString("renderReportKpis")))
                .andExpect(content().string(containsString("renderCasesAnalytics")))
                .andExpect(content().string(containsString("renderAmbulanceAnalytics")))
                .andExpect(content().string(containsString("renderDispatchAnalytics")))
                .andExpect(content().string(containsString("Case Completion Rate")))
                .andExpect(content().string(containsString("Avg Planned Dispatch ETA")))
                .andExpect(content().string(containsString("Assigned Incidents")))
                .andExpect(content().string(containsString("Assigned Vehicle Status")))
                .andExpect(content().string(not(containsString("Resolution Rate"))));
    }

    @Test
    @DisplayName("GET /js/reports.js enforces rigorous error propagation without silent failure suppression")
    void getReportsJsErrorHandlingReturnsOk() throws Exception {
        mockMvc.perform(get("/js/reports.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"))
                .andExpect(content().string(containsString("err && err.status === 403")))
                .andExpect(content().string(containsString("throw err;")))
                .andExpect(content().string(containsString("Malformed emergency cases response received from server.")))
                .andExpect(content().string(containsString("Malformed ambulance fleet response received from server.")))
                .andExpect(content().string(not(containsString("return { data: [] };"))));
    }

    @Test
    @DisplayName("GET /css/styles.css includes narrow-screen overflow protection for analytics grid")
    void getStylesCssNarrowViewportReturnsOk() throws Exception {
        mockMvc.perform(get("/css/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(containsString(".reports-analytics-grid")))
                .andExpect(content().string(containsString("minmax(min(100%, 300px), 1fr)")));
    }

    @Test
    @DisplayName("GET /js/app.js returns UI coordinator with 200 OK without dead references")
    void getAppJsReturnsOk() throws Exception {
        mockMvc.perform(get("/js/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/javascript"))
                .andExpect(content().string(containsString("EmergencyApp")))
                .andExpect(content().string(not(containsString("welcomeSessionPill"))));
    }

    @Test
    @DisplayName("GET /api/auth/csrf returns CSRF token with 200 OK without authentication")
    void getCsrfTokenReturnsOk() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.token").isString())
                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"));
    }

    @Test
    @DisplayName("Unauthenticated GET /api/auth/me returns 401 Unauthorized")
    void unauthenticatedMeReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required. Please sign in."));
    }
}
