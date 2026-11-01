package com.abc.emergency.web;

import com.abc.emergency.dto.ApiResponse;
import com.abc.emergency.util.DBConnection;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");

        List<String> tables = new ArrayList<>();
        List<Map<String, Object>> users = new ArrayList<>();
        Map<String, Integer> rowCounts = new LinkedHashMap<>();

        try (Connection conn = DBConnection.getConnection()) {
            data.put("database", "CONNECTED");
            data.put("catalog", conn.getCatalog());

            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            data.put("tables", tables);

            // Check non-sensitive user metadata
            boolean hasUsers = tables.stream().anyMatch(t -> t.equalsIgnoreCase("users"));
            if (hasUsers) {
                String userSql = """
                        SELECT user_id, username, full_name, role, active,
                               CASE WHEN password_hash LIKE '$2%' THEN 'BCRYPT' ELSE 'PLAIN_OR_OTHER' END AS hash_type
                        FROM users ORDER BY user_id
                        """;
                try (PreparedStatement stmt = conn.prepareStatement(userSql);
                     ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> u = new LinkedHashMap<>();
                        u.put("userId", rs.getInt("user_id"));
                        u.put("username", rs.getString("username"));
                        u.put("fullName", rs.getString("full_name"));
                        u.put("role", rs.getString("role"));
                        u.put("active", rs.getBoolean("active"));
                        u.put("hashType", rs.getString("hash_type"));
                        users.add(u);
                    }
                }
            }
            data.put("users", users);

            // Table row counts for key entities
            String[] countTables = {"patients", "ambulances", "emergency_cases", "dispatch_records"};
            for (String tbl : countTables) {
                if (tables.stream().anyMatch(t -> t.equalsIgnoreCase(tbl))) {
                    try (PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + tbl);
                         ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            rowCounts.put(tbl, rs.getInt(1));
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            data.put("rowCounts", rowCounts);

        } catch (Exception e) {
            data.put("database", "ERROR: " + e.getMessage());
            return ResponseEntity.status(503).body(new ApiResponse<>(503, "Database health check failed", data));
        }

        return ResponseEntity.ok(new ApiResponse<>(200, "System healthy", data));
    }
}
