package io.ten1010.aipub.projectcontroller.configuration;

import io.ten1010.aipub.projectcontroller.service.MockRegistryRobotService;
import io.ten1010.aipub.projectcontroller.service.RegistryRobotService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfiguration {

    @Bean
    public RegistryRobotService registryRobotService() {
        return new MockRegistryRobotService();
    }

}
