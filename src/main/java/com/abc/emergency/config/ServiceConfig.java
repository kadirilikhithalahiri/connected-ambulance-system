package com.abc.emergency.config;

import com.abc.emergency.dao.AmbulanceDAO;
import com.abc.emergency.dao.DispatchRecordDAO;
import com.abc.emergency.dao.EmergencyCaseDAO;
import com.abc.emergency.service.AmbulanceService;
import com.abc.emergency.service.DispatchService;
import com.abc.emergency.service.EmergencyCaseService;
import com.abc.emergency.service.LoginService;
import com.abc.emergency.service.PatientService;
import com.abc.emergency.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServiceConfig {

    @Bean
    public LoginService loginService() {
        return new LoginService();
    }

    @Bean
    public UserService userService() {
        return new UserService();
    }

    @Bean
    public PatientService patientService() {
        return new PatientService();
    }

    @Bean
    public EmergencyCaseService emergencyCaseService() {
        return new EmergencyCaseService();
    }

    @Bean
    public AmbulanceService ambulanceService() {
        return new AmbulanceService();
    }

    @Bean
    public DispatchService dispatchService(AmbulanceService ambulanceService) {
        return new DispatchService(new AmbulanceDAO(), new DispatchRecordDAO(), new EmergencyCaseDAO(), ambulanceService);
    }
}
