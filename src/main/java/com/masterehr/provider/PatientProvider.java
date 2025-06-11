package com.masterehr.provider;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.masterehr.entity.PatientEntity;
import com.masterehr.repository.PatientRepository;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class PatientProvider implements IResourceProvider {

    @Autowired
    private PatientRepository patientRepository;

    /**
     * Handles GET /Patient/[id]
     */
    @Read
    public Patient getPatientById(@IdParam IdType theId) {
        Optional<PatientEntity> patientEntityOptional = patientRepository.findById(Integer.parseInt(theId.getIdPart()));
        if (patientEntityOptional.isPresent()) {
            return transformToFhirPatient(patientEntityOptional.get());
        } else {
            throw new ResourceNotFoundException("Patient not found with ID: " + theId.getIdPart());
        }
    }

    /**
     * Handles GET /Patient?family=[name]
     */
    @Search
    public List<Patient> searchPatientsByFamilyName(
            @RequiredParam(name = Patient.SP_FAMILY) String familyName) {
        List<PatientEntity> dbPatients = patientRepository.findByLastName(familyName);
        return dbPatients.stream()
                .map(this::transformToFhirPatient)
                .collect(Collectors.toList());
    }

    /**
     * Handles POST /Patient to create a new patient.
     * The @Transactional annotation ensures this operation is atomic.
     */
    @Create
    @Transactional
    public MethodOutcome createPatient(@ResourceParam Patient thePatient) {
        // Transform the incoming FHIR resource into our database entity
        PatientEntity patientEntity = transformToPatientEntity(thePatient);
        
        // Set a new UID for the new patient
        patientEntity.setPatientUid(UUID.randomUUID());

        // Save the new entity to the database
        PatientEntity savedPatient = patientRepository.save(patientEntity);

        // Return a MethodOutcome which includes the new ID of the created resource
        MethodOutcome outcome = new MethodOutcome();
        outcome.setId(new IdType("Patient", savedPatient.getPatientId().toString()));
        outcome.setCreated(true);
        outcome.setResource(transformToFhirPatient(savedPatient)); // Return the created resource
        return outcome;
    }

    /**
     * Handles PUT /Patient/[id] to update an existing patient.
     */
    @Update
    @Transactional
    public MethodOutcome updatePatient(@IdParam IdType theId, @ResourceParam Patient thePatient) {
        // First, check if a patient with this ID already exists
        return patientRepository.findById(Integer.parseInt(theId.getIdPart()))
            .map(existingPatient -> {
                // Patient exists, so update it
                PatientEntity updatedEntity = transformToPatientEntity(thePatient);
                // Important: Set the ID from the URL to ensure we update the correct record
                updatedEntity.setPatientId(existingPatient.getPatientId());
                updatedEntity.setPatientUid(existingPatient.getPatientUid()); // Preserve existing UID

                PatientEntity savedPatient = patientRepository.save(updatedEntity);

                MethodOutcome outcome = new MethodOutcome();
                outcome.setId(new IdType("Patient", savedPatient.getPatientId().toString()));
                outcome.setResource(transformToFhirPatient(savedPatient));
                return outcome;
            })
            .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + theId.getIdPart()));
    }

    /**
     * Transforms our internal database entity into the standard FHIR Patient resource.
     */
    private Patient transformToFhirPatient(PatientEntity entity) {
        Patient fhirPatient = new Patient();
        fhirPatient.setId(entity.getPatientId().toString());
        if (entity.getFirstName() != null || entity.getLastName() != null) {
            HumanName name = new HumanName();
            name.setFamily(entity.getLastName());
            name.setGiven(Collections.singletonList(new org.hl7.fhir.r4.model.StringType(entity.getFirstName())));
            fhirPatient.setName(Collections.singletonList(name));
        }
        if (entity.getDob() != null) {
            fhirPatient.setBirthDate(Date.valueOf(entity.getDob()));
        }
        if (entity.getGender() != null) {
            switch (entity.getGender().toLowerCase()) {
                case "male": fhirPatient.setGender(Enumerations.AdministrativeGender.MALE); break;
                case "female": fhirPatient.setGender(Enumerations.AdministrativeGender.FEMALE); break;
                default: fhirPatient.setGender(Enumerations.AdministrativeGender.UNKNOWN); break;
            }
        }
        return fhirPatient;
    }

    /**
     * Transforms an incoming FHIR Patient resource into our internal database entity for saving.
     */
    private PatientEntity transformToPatientEntity(Patient fhirPatient) {
        PatientEntity entity = new PatientEntity();

        if (fhirPatient.hasName()) {
            HumanName name = fhirPatient.getNameFirstRep();
            entity.setFirstName(name.getGivenAsSingleString());
            entity.setLastName(name.getFamily());
        }

        if (fhirPatient.hasBirthDate()) {
            // Convert java.util.Date to java.time.LocalDate
            entity.setDob(fhirPatient.getBirthDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }

        if (fhirPatient.hasGender()) {
            entity.setGender(fhirPatient.getGender().toCode());
        }
        
        // In a real application, you would map other fields like OHIP number, address, etc.
        // For now, we'll set a placeholder for the required ohipNumber field.
        entity.setOhipNumber("0000000000");

        return entity;
    }

    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }
}
