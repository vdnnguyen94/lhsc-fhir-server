package com.masterehr.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.masterehr.entity.EncounterEntity;
import com.masterehr.repository.EncounterRepository;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class EncounterProvider implements IResourceProvider {

    private final EncounterRepository encounterRepository;
    private final FhirContext fhirContext;

    @Autowired
    public EncounterProvider(EncounterRepository encounterRepository, FhirContext fhirContext) {
        this.encounterRepository = encounterRepository;
        this.fhirContext = fhirContext;
    }

    @Override
    public Class<Encounter> getResourceType() {
        return Encounter.class;
    }

    @Read
    public Encounter getEncounterById(@IdParam IdType theId) {
        return encounterRepository.findById(Integer.parseInt(theId.getIdPart()))
                .map(this::transformToFhirEncounter)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + theId.getIdPart()));
    }

    /**
     * Search for encounters by the patient they are associated with.
     * Handles requests like GET /Encounter?patient=1
     */
    @Search
    public List<Encounter> searchEncountersByPatient(
        @RequiredParam(name = Encounter.SP_PATIENT) ReferenceParam thePatient) {

        // Get the patient ID from the search parameter
        String patientId = thePatient.getIdPart();

        // Use the repository to find encounters by that patient's ID
        List<EncounterEntity> dbEncounters = encounterRepository.findByPatientId(Integer.parseInt(patientId));

        // Transform the list of database entities into a list of FHIR resources
        return dbEncounters.stream()
                .map(this::transformToFhirEncounter)
                .collect(Collectors.toList());
    }

    private Encounter transformToFhirEncounter(EncounterEntity entity) {
        if (entity.getResourceJson() != null && !entity.getResourceJson().isEmpty()) {
            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Encounter.class, entity.getResourceJson());
        }

        // Manual fallback transformation
        Encounter encounter = new Encounter();
        encounter.setId(entity.getEncounterId().toString());

        // Set status
        if ("Admitted".equalsIgnoreCase(entity.getStatus())) {
            encounter.setStatus(Encounter.EncounterStatus.INPROGRESS);
        } else if ("Discharged".equalsIgnoreCase(entity.getStatus())) {
            encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        } else {
            encounter.setStatus(Encounter.EncounterStatus.UNKNOWN);
        }

        // Set patient reference
        encounter.setSubject(new Reference("Patient/" + entity.getPatientId()));

        // Set period
        Period period = new Period();
        if (entity.getVisitDate() != null) {
            period.setStart(Date.from(entity.getVisitDate().atZone(ZoneId.systemDefault()).toInstant()));
        }
        if (entity.getDischargeDate() != null) {
            period.setEnd(Date.from(entity.getDischargeDate().atZone(ZoneId.systemDefault()).toInstant()));
        }
        encounter.setPeriod(period);

        return encounter;
    }
}