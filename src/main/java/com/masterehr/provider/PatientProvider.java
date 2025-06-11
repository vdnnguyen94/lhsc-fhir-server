package com.masterehr.provider;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.RequiredParam; // New Import
import ca.uhn.fhir.rest.annotation.Search;       // New Import
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

import java.sql.Date;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PatientProvider implements IResourceProvider {

    @Autowired
    private PatientRepository patientRepository;

    /**
     * This is the "read" operation, which handles requests like GET /Patient/1
     */
    @Read
    public Patient getPatientById(@IdParam IdType theId) {
        // ... (this existing method is unchanged)
        Optional<PatientEntity> patientEntityOptional = patientRepository.findById(Integer.parseInt(theId.getIdPart()));
        if (patientEntityOptional.isPresent()) {
            return transformToFhirPatient(patientEntityOptional.get());
        } else {
            throw new ResourceNotFoundException("Patient not found with ID: " + theId.getIdPart());
        }
    }

    /**
     * This is the "search" operation, which handles requests like GET /Patient?family=Smith
     * The @Search annotation tells HAPI FHIR this method handles search queries.
     */
    @Search
    public List<Patient> searchPatientsByFamilyName(
            @RequiredParam(name = Patient.SP_FAMILY) String familyName) {
        
        // Step 1: Use our new repository method to find patients in the database
        List<PatientEntity> dbPatients = patientRepository.findByLastName(familyName);

        // Step 2: Transform the list of database entities into a list of FHIR resources
        List<Patient> fhirPatients = dbPatients.stream()
                .map(this::transformToFhirPatient) // Reuse our existing transformation logic
                .collect(Collectors.toList());
        
        return fhirPatients;
    }


    /**
     * This private helper method transforms our internal database entity into the standard FHIR Patient resource.
     */
    private Patient transformToFhirPatient(PatientEntity entity) {
        // ... (this existing method is unchanged)
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
                case "male":
                    fhirPatient.setGender(Enumerations.AdministrativeGender.MALE);
                    break;
                case "female":
                    fhirPatient.setGender(Enumerations.AdministrativeGender.FEMALE);
                    break;
                default:
                    fhirPatient.setGender(Enumerations.AdministrativeGender.UNKNOWN);
                    break;
            }
        }
        return fhirPatient;
    }

    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }
}
