package com.masterehr.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import com.masterehr.provider.EncounterProvider;
import com.masterehr.provider.PatientProvider;
import com.masterehr.provider.ObservationProvider;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;

@WebServlet("/fhir/*")
public class FhirServerConfig extends RestfulServer {

    private final PatientProvider patientProvider;
    private final EncounterProvider encounterProvider;
    private final ObservationProvider observationProvider;
    private final FhirContext fhirContext;

    @Autowired
    public FhirServerConfig(PatientProvider patientProvider, EncounterProvider encounterProvider, 
    ObservationProvider observationProvider, FhirContext fhirContext) {
        this.patientProvider = patientProvider;
        this.encounterProvider = encounterProvider;
        this.observationProvider = observationProvider;
        this.fhirContext = fhirContext;
    }

    @Override
    protected void initialize() throws ServletException {
        setFhirContext(fhirContext);
        setResourceProviders(Arrays.asList(patientProvider, encounterProvider,observationProvider));
        registerInterceptor(new ResponseHighlighterInterceptor());
    }
}