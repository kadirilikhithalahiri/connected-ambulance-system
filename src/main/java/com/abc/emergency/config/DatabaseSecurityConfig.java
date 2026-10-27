package com.abc.emergency.config;

import com.abc.emergency.util.DatabaseSecurityValidator;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

/**
 * Validates database configuration upon Spring Boot application startup,
 * enforcing fail-closed security before any application services can access
 * a database.
 */
@Configuration
public class DatabaseSecurityConfig {

    @Value("${spring.datasource.url:#{null}}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:#{null}}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:#{null}}")
    private String datasourcePassword;

    private final Function<String, String> envLookup;

    public DatabaseSecurityConfig() {
        this(System::getenv);
    }

    public DatabaseSecurityConfig(Function<String, String> envLookup) {
        this.envLookup = (envLookup != null) ? envLookup : System::getenv;
    }

    public DatabaseSecurityConfig(String datasourceUrl, String datasourceUsername, String datasourcePassword) {
        this(datasourceUrl, datasourceUsername, datasourcePassword, System::getenv);
    }

    public DatabaseSecurityConfig(String datasourceUrl, String datasourceUsername, String datasourcePassword,
                                  Function<String, String> envLookup) {
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.envLookup = (envLookup != null) ? envLookup : System::getenv;
    }

    @PostConstruct
    public void validateDatabaseConfiguration() {
        // Resolve either from Spring properties or environment variables via envLookup seam
        String envUrl = (envLookup != null) ? envLookup.apply("DB_URL") : System.getenv("DB_URL");
        String envUser = (envLookup != null) ? envLookup.apply("DB_USER") : System.getenv("DB_USER");
        String envPassword = (envLookup != null) ? envLookup.apply("DB_PASSWORD") : System.getenv("DB_PASSWORD");

        String url = (datasourceUrl != null && !datasourceUrl.trim().isEmpty()) ? datasourceUrl : envUrl;
        String user = (datasourceUsername != null && !datasourceUsername.trim().isEmpty()) ? datasourceUsername : envUser;
        String password = (datasourcePassword != null && !datasourcePassword.trim().isEmpty()) ? datasourcePassword : envPassword;

        // Fail-closed validation: halts Spring context if database parameters are missing, invalid, or target production/root
        DatabaseSecurityValidator.validate(url, user, password);
    }
}
