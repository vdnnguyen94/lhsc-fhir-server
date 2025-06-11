package com.masterehr.repository;

import com.masterehr.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, Integer> {
    
    /**
     * This custom method tells Spring Data JPA to create a query that finds all patients
     * where the 'lastName' column matches the provided parameter.
     * The method name itself defines the query!
     * @param lastName The family name to search for.
     * @return A list of matching patient entities.
     */
    List<PatientEntity> findByLastName(String lastName);

}
