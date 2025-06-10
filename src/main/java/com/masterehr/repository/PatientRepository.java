package com.masterehr.repository;

import com.masterehr.entity.PatientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PatientRepository extends JpaRepository<PatientEntity, Integer> {
    // Spring Data JPA will automatically implement all standard CRUD methods.
    // We can add custom query methods here later if needed.
}
