package com.masterehr.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.masterehr.entity.EncounterEntity;
import com.masterehr.repository.EncounterRepository;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

    @Search
    public List<Encounter> searchEncountersByPatient(
        @RequiredParam(name = Encounter.SP_PATIENT) ReferenceParam thePatient) {
        String patientId = thePatient.getIdPart();
        List<EncounterEntity> dbEncounters = encounterRepository.findByPatientId(Integer.parseInt(patientId));
        return dbEncounters.stream()
                .map(this::transformToFhirEncounter)
                .collect(Collectors.toList());
    }

    /**
     * Handles POST /Encounter to create a new encounter.
     */
    @Create
    @Transactional
    public MethodOutcome createEncounter(@ResourceParam Encounter theEncounter) {
        EncounterEntity encounterEntity = transformToEncounterEntity(theEncounter, new EncounterEntity());
        
        EncounterEntity savedEncounter = encounterRepository.save(encounterEntity);
        
        theEncounter.setId(savedEncounter.getEncounterId().toString());
        String jsonResource = fhirContext.newJsonParser().setPrettyPrint(false).encodeResourceToString(theEncounter);
        savedEncounter.setResourceJson(jsonResource);
        
        encounterRepository.save(savedEncounter);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(new IdType("Encounter", savedEncounter.getEncounterId().toString()));
        outcome.setCreated(true);
        outcome.setResource(theEncounter);
        return outcome;
    }

    /**
     * Handles PUT /Encounter/[id] to update an existing encounter.
     */
    @Update
    @Transactional
    public MethodOutcome updateEncounter(@IdParam IdType theId, @ResourceParam Encounter theEncounter) {
        return encounterRepository.findById(Integer.parseInt(theId.getIdPart()))
            .map(existingEncounter -> {
                EncounterEntity updatedEntity = transformToEncounterEntity(theEncounter, existingEncounter);
                
                theEncounter.setId(theId.getIdPart());
                String jsonResource = fhirContext.newJsonParser().setPrettyPrint(false).encodeResourceToString(theEncounter);
                updatedEntity.setResourceJson(jsonResource);

                EncounterEntity savedEncounter = encounterRepository.save(updatedEntity);

                MethodOutcome outcome = new MethodOutcome();
                outcome.setId(new IdType("Encounter", savedEncounter.getEncounterId().toString()));
                outcome.setResource(theEncounter);
                return outcome;
            })
            .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + theId.getIdPart()));
    }

    private Encounter transformToFhirEncounter(EncounterEntity entity) {
        if (entity.getResourceJson() != null && !entity.getResourceJson().isEmpty()) {
            return fhirContext.newJsonParser().parseResource(Encounter.class, entity.getResourceJson());
        }

        Encounter encounter = new Encounter();
        encounter.setId(entity.getEncounterId().toString());
        
        if (entity.getStatus() != null) {
            encounter.setStatus(Encounter.EncounterStatus.fromCode(entity.getStatus().toLowerCase()));
        }
        
        encounter.setSubject(new Reference("Patient/" + entity.getPatientId()));
        
        // --- NEW: Map the Reason for Visit ---
        if (entity.getReasonForVisit() != null && !entity.getReasonForVisit().isEmpty()) {
            CodeableConcept reasonCode = new CodeableConcept();
            reasonCode.setText(entity.getReasonForVisit());
            encounter.addReasonCode(reasonCode);
        }
        
        Period period = new Period();
        if (entity.getVisitDate() != null) {
            period.setStart(Date.from(entity.getVisitDate().atZone(ZoneId.systemDefault()).toInstant()));
        }
        if (entity.getDischargeDate() != null) {
            period.setEnd(Date.from(entity.getDischargeDate().atZone(ZoneId.systemDefault()).toInstant()));
        }
        if (period.hasStart() || period.hasEnd()) {
            encounter.setPeriod(period);
        }

        return encounter;
    }
    
    /**
     * Transforms an incoming FHIR Encounter into our internal database entity.
     */
    private EncounterEntity transformToEncounterEntity(Encounter fhirEncounter, EncounterEntity existingEntity) {
        EncounterEntity entity = (existingEntity != null) ? existingEntity : new EncounterEntity();

        if (fhirEncounter.hasSubject() && fhirEncounter.getSubject().hasReference()) {
            IdType subjectId = new IdType(fhirEncounter.getSubject().getReference());
            entity.setPatientId(Integer.parseInt(subjectId.getIdPart()));
        }
        
        if (fhirEncounter.hasStatus()) {
            entity.setStatus(fhirEncounter.getStatus().toCode());
        }

        // --- NEW: Map the Reason for Visit ---
        if (fhirEncounter.hasReasonCode()) {
            // Get the text from the first reasonCode if it exists
            String reasonText = fhirEncounter.getReasonCodeFirstRep().getText();
            entity.setReasonForVisit(reasonText);
        }
        
        if (fhirEncounter.hasPeriod()) {
            Period period = fhirEncounter.getPeriod();
            if (period.hasStart()) {
                entity.setVisitDate(period.getStart().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            }
            if (period.hasEnd()) {
                entity.setDischargeDate(period.getEnd().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
            }
        }
        
        return entity;
    }
}
