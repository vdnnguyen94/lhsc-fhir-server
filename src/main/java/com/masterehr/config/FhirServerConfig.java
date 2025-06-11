package com.masterehr.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import com.masterehr.provider.PatientProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

/**
 * This class is a Servlet that sets up the HAPI FHIR server.
 * It now receives its configuration (like the FhirContext) via dependency injection.
 */
@WebServlet("/fhir/*")
public class FhirServerConfig extends RestfulServer {

    private final PatientProvider patientProvider;
    private final FhirContext fhirContext;

    /**
     * Use constructor injection to receive the beans we need from Spring.
     * This is a robust way to ensure our servlet has access to the shared FhirContext.
     */
    @Autowired
    public FhirServerConfig(PatientProvider patientProvider, FhirContext fhirContext) {
        this.patientProvider = patientProvider;
        this.fhirContext = fhirContext;
    }

    @Override
    protected void initialize() throws ServletException {
        // Use the centrally-defined FhirContext bean
        setFhirContext(fhirContext);

        // Register our PatientProvider.
        setResourceProviders(Arrays.asList(patientProvider));

        // Register an interceptor to add color highlighting to browser responses
        registerInterceptor(new ResponseHighlighterInterceptor());
    }
}
