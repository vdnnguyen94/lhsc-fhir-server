package com.masterehr.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * This class holds central application configuration and defines Spring Beans.
 */
@Configuration
public class AppConfig {

    /**
     * This method creates a single, shared instance of a FhirContext for the
     * entire application. The @Bean annotation tells Spring to manage this object.
     * Any other component that needs a FhirContext can now have it injected.
     * @return A configured FhirContext for FHIR R4.
     */
    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }
}
