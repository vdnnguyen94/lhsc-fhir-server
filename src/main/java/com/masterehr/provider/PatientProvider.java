package com.masterehr.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.masterehr.entity.PatientEntity;
import com.masterehr.repository.PatientRepository;
import org.hl7.fhir.r4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PatientProvider implements IResourceProvider {

    private final PatientRepository patientRepository;
    private final FhirContext fhirContext;

    @Autowired
    public PatientProvider(PatientRepository patientRepository, FhirContext fhirContext) {
        this.patientRepository = patientRepository;
        this.fhirContext = fhirContext;
    }

    private static final String OHIP_SYSTEM_URL = "http://hl7.org/fhir/sid/ca-on-ohip";

    @Read
    public Patient getPatientById(@IdParam IdType theId) {
        return patientRepository.findById(Integer.parseInt(theId.getIdPart()))
                .map(this::transformToFhirPatient)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + theId.getIdPart()));
    }

    @Search
    public List<Patient> searchPatientsByFamilyName(@RequiredParam(name = Patient.SP_FAMILY) String familyName) {
        return patientRepository.findByLastName(familyName).stream()
                .map(this::transformToFhirPatient)
                .collect(Collectors.toList());
    }

    @Create
    @Transactional
    public MethodOutcome createPatient(@ResourceParam Patient thePatient) {
        // Create a new entity and map the fields
        PatientEntity patientEntity = transformToPatientEntity(thePatient, new PatientEntity());
        patientEntity.setPatientUid(UUID.randomUUID());

        // Save once to get the database-assigned ID
        PatientEntity savedPatient = patientRepository.save(patientEntity);

        // Now, update the FHIR resource with the new ID before storing its JSON representation
        thePatient.setId(savedPatient.getPatientId().toString());
        String jsonResource = fhirContext.newJsonParser().setPrettyPrint(false).encodeResourceToString(thePatient);
        savedPatient.setResourceJson(jsonResource);
        
        // Save again to store the JSON representation
        patientRepository.save(savedPatient);

        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(new IdType("Patient", savedPatient.getPatientId().toString()));
        outcome.setCreated(true);
        outcome.setResource(thePatient);
        return outcome;
    }

    @Update
    @Transactional
    public MethodOutcome updatePatient(@IdParam IdType theId, @ResourceParam Patient thePatient) {
        return patientRepository.findById(Integer.parseInt(theId.getIdPart()))
                .map(existingPatient -> {
                    PatientEntity updatedEntity = transformToPatientEntity(thePatient, existingPatient);
                    
                    // Update the FHIR resource with the correct ID before storing the JSON
                    thePatient.setId(theId.getIdPart());
                    String jsonResource = fhirContext.newJsonParser().setPrettyPrint(false).encodeResourceToString(thePatient);
                    updatedEntity.setResourceJson(jsonResource);

                    PatientEntity savedPatient = patientRepository.save(updatedEntity);

                    MethodOutcome outcome = new MethodOutcome();
                    outcome.setId(new IdType("Patient", savedPatient.getPatientId().toString()));
                    outcome.setResource(thePatient);
                    return outcome;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + theId.getIdPart()));
    }

    /**
     * Transforms our internal database entity into the standard FHIR Patient resource.
     * This method is now robust and handles both old and new data.
     */
    private Patient transformToFhirPatient(PatientEntity entity) {
        // If we have a stored JSON representation, use it for efficiency.
        if (entity.getResourceJson() != null && !entity.getResourceJson().isEmpty()) {
            IParser parser = fhirContext.newJsonParser();
            return parser.parseResource(Patient.class, entity.getResourceJson());
        }

        // Fallback for old data: Manually transform the fields if resource_json is null.
        Patient fhirPatient = new Patient();
        fhirPatient.setId(entity.getPatientId().toString());

        if (entity.getOhipNumber() != null) {
            fhirPatient.addIdentifier()
                .setSystem(OHIP_SYSTEM_URL)
                .setValue(entity.getOhipNumber());
        }

        if (entity.getFirstName() != null || entity.getLastName() != null) {
            HumanName name = fhirPatient.addName();
            name.setFamily(entity.getLastName());
            name.addGiven(entity.getFirstName());
        }

        if (entity.getDob() != null) {
            fhirPatient.setBirthDate(Date.valueOf(entity.getDob()));
        }

        if (entity.getGender() != null) {
            fhirPatient.setGender(Enumerations.AdministrativeGender.fromCode(entity.getGender().toLowerCase()));
        }
        
        return fhirPatient;
    }

    /**
     * Transforms an incoming FHIR Patient resource into our internal database entity for saving.
     */
    private PatientEntity transformToPatientEntity(Patient fhirPatient, PatientEntity existingEntity) {
        // Use the existing entity if provided (for updates), otherwise create a new one.
        PatientEntity entity = (existingEntity != null) ? existingEntity : new PatientEntity();

        if (fhirPatient.hasIdentifier()) {
            fhirPatient.getIdentifier().stream()
                    .filter(i -> OHIP_SYSTEM_URL.equals(i.getSystem()))
                    .findFirst()
                    .ifPresent(identifier -> entity.setOhipNumber(identifier.getValue()));
        }

        if (fhirPatient.hasName()) {
            HumanName name = fhirPatient.getNameFirstRep();
            entity.setFirstName(name.getGivenAsSingleString());
            entity.setLastName(name.getFamily());
        }

        if (fhirPatient.hasBirthDate()) {
            entity.setDob(fhirPatient.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }
        
        if (fhirPatient.hasGender()) {
            entity.setGender(fhirPatient.getGender().toCode());
        }

        return entity;
    }

    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }
}
