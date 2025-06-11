package com.masterehr.repository;

import com.masterehr.entity.EncounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EncounterRepository extends JpaRepository<EncounterEntity, Integer> {

    // Find all encounters for a specific patient
    List<EncounterEntity> findByPatientId(Integer patientId);
}
