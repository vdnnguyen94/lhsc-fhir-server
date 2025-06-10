package com.masterehr;

import com.masterehr.entity.PatientEntity;
import com.masterehr.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// To disable this test runner, simply comment out or remove the @Component annotation below.
// Spring will no longer create this bean, and therefore its run() method will not be executed.
// @Component
public class DatabaseConnectionTester implements CommandLineRunner {

    @Autowired
    private PatientRepository patientRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("\n\n--- DATABASE CONNECTION TEST ---");
        try {
            List<PatientEntity> patients = patientRepository.findAll();
            if (patients.isEmpty()) {
                System.out.println(">>> Connection SUCCESSFUL, but no patients found in the database.");
            } else {
                System.out.println(">>> Connection SUCCESSFUL! Found " + patients.size() + " patients:");
                patients.forEach(patient -> System.out.println("  - " + patient.toString()));
            }
        } catch (Exception e) {
            System.err.println(">>> DATABASE CONNECTION FAILED: " + e.getMessage());
        }
        System.out.println("--- TEST COMPLETE ---\n\n");
    }
}
