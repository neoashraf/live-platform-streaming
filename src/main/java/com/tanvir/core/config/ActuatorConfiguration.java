package com.tanvir.core.config;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
//@EnableAutoConfiguration(exclude = {TenantConfig.class})
@EnableAutoConfiguration()
@AutoConfigureOrder(Integer.MAX_VALUE) // Set a high order value
public class ActuatorConfiguration {

    // Configuration for actuator endpoints
}

