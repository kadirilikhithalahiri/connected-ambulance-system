
package com.abc.emergency.config;

import com.abc.emergency.util.DatabaseSecurityValidator;
import com.abc.emergency.util.ProductionDatabaseSecurityValidator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseSecurityConfig {

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @PostConstruct
    public void validateDatabaseConfiguration() {
        String url = datasourceUrl;
        String user = datasourceUsername;
        String password = datasourcePassword;

        if (url == null || url.isBlank()) {
            url = System.getenv("DB_URL");
        }
        if (user == null || user.isBlank()) {
            user = System.getenv("DB_USER");
        }
        if (password == null || password.isBlank()) {
            password = System.getenv("DB_PASSWORD");
        }

        String profile = System.getenv("SPRING_PROFILES_ACTIVE");

        if ("production".equalsIgnoreCase(profile)) {
            ProductionDatabaseSecurityValidator.validate(url, user, password);
        } else {
            DatabaseSecurityValidator.validate(url, user, password);
        }
    }
}
