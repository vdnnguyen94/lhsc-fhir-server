package com.masterehr.provider;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
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
import java.util.Optional;

@Component
public class PatientProvider implements IResourceProvider {

    @Autowired
    private PatientRepository patientRepository;

    /**
     * This method handles the "read" operation (e.g., GET /Patient/1).
     * The @Read annotation tells HAPI FHIR that this method handles fetching a single resource by its ID.
     */
    @Read
    public Patient getPatientById(@IdParam IdType theId) {
        // Step 1: Query the SQL database using our repository.
        Optional<PatientEntity> patientEntityOptional = patientRepository.findById(Integer.parseInt(theId.getIdPart()));

        if (patientEntityOptional.isPresent()) {
            // Step 2: If the patient is found, transform the database entity into a FHIR resource.
            PatientEntity dbPatient = patientEntityOptional.get();
            return transformToFhirPatient(dbPatient);
        } else {
            // Step 3: If not found, throw a specific FHIR exception that results in a 404 Not Found response.
            throw new ResourceNotFoundException("Patient not found with ID: " + theId.getIdPart());
        }
    }

    /**
     * This private helper method transforms our internal database entity into the standard FHIR Patient resource.
     * This is the core "translation" logic.
     */
    private Patient transformToFhirPatient(PatientEntity entity) {
        Patient fhirPatient = new Patient();

        // Map the database ID to the FHIR resource ID
        fhirPatient.setId(entity.getPatientId().toString());

        // Map the name fields
        if (entity.getFirstName() != null || entity.getLastName() != null) {
            HumanName name = new HumanName();
            name.setFamily(entity.getLastName());
            name.setGiven(Collections.singletonList(new org.hl7.fhir.r4.model.StringType(entity.getFirstName())));
            fhirPatient.setName(Collections.singletonList(name));
        }

        // Map the date of birth
        if (entity.getDob() != null) {
            fhirPatient.setBirthDate(Date.valueOf(entity.getDob()));
        }

        // Map the gender
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
        
        // We can map other fields here later (e.g., address, phone number).

        return fhirPatient;
    }

    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }
}
