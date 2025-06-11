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
import com.masterehr.entity.ObservationEntity;
import com.masterehr.repository.ObservationRepository;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ObservationProvider implements IResourceProvider {

    private final ObservationRepository observationRepository;
    private final FhirContext fhirContext;

    @Autowired
    public ObservationProvider(ObservationRepository observationRepository, FhirContext fhirContext) {
        this.observationRepository = observationRepository;
        this.fhirContext = fhirContext;
    }

    @Override
    public Class<Observation> getResourceType() {
        return Observation.class;
    }

    @Read
    public Observation getObservationById(@IdParam IdType theId) {
        return observationRepository.findById(Integer.parseInt(theId.getIdPart()))
                .map(this::transformToFhirObservation)
                .orElseThrow(() -> new ResourceNotFoundException("Observation not found with ID: " + theId.getIdPart()));
    }

    @Search
    public List<Observation> searchObservationsByPatient(
        @RequiredParam(name = Observation.SP_PATIENT) ReferenceParam thePatient) {

        String patientId = thePatient.getIdPart();
        List<ObservationEntity> dbObservations = observationRepository.findByPatientId(Integer.parseInt(patientId));

        return dbObservations.stream()
                .map(this::transformToFhirObservation)
                .collect(Collectors.toList());
    }

    private Observation transformToFhirObservation(ObservationEntity entity) {
        if (entity.getResourceJson() != null && !entity.getResourceJson().isEmpty()) {
            return fhirContext.newJsonParser().parseResource(Observation.class, entity.getResourceJson());
        }

        Observation observation = new Observation();
        observation.setId(entity.getObservationId().toString());

        // Set status
        if (entity.getStatus() != null) {
            observation.setStatus(Observation.ObservationStatus.fromCode(entity.getStatus().toLowerCase()));
        }

        // Set code (e.g., LOINC code for the observation type)
        observation.getCode().addCoding()
            .setSystem(entity.getLoincSystem())
            .setCode(entity.getLoincCode());

        // Set subject (the patient)
        observation.setSubject(new Reference("Patient/" + entity.getPatientId()));

        // Set encounter (the visit it's associated with), if it exists
        if (entity.getEncounterId() != null) {
            observation.setEncounter(new Reference("Encounter/" + entity.getEncounterId()));
        }

        // Set effectiveDateTime
        if (entity.getEffectiveDatetime() != null) {
            observation.setEffective(new DateTimeType(Date.from(entity.getEffectiveDatetime().atZone(ZoneId.systemDefault()).toInstant())));
        }

        // Set valueQuantity
        if (entity.getValueQuantity() != null) {
            Quantity value = new Quantity();
            value.setValue(entity.getValueQuantity());
            value.setUnit(entity.getValueUnit());
            observation.setValue(value);
        }

        return observation;
    }
}