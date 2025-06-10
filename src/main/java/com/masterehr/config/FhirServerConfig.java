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
 * This class is a Servlet. Servlets are the standard Java way to handle web requests.
 * We are creating a HAPI FHIR RestfulServer and telling it which Resource Providers to use.
 */
@WebServlet("/fhir/*")
public class FhirServerConfig extends RestfulServer {

    @Autowired
    private PatientProvider patientProvider; // Spring will inject our PatientProvider bean

    @Override
    protected void initialize() throws ServletException {
        // Tell the server which version of FHIR to support
        setFhirContext(FhirContext.forR4());

        // Register our PatientProvider. This tells the server how to handle
        // requests for Patient resources. We can add more providers here later.
        setResourceProviders(Arrays.asList(patientProvider));

        // Register an interceptor to add color highlighting to browser responses
        registerInterceptor(new ResponseHighlighterInterceptor());
    }
}
