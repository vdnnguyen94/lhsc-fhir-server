package com.masterehr.repository;

import com.masterehr.entity.ObservationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationRepository extends JpaRepository<ObservationEntity, Integer> {

    // Find all observations for a specific patient
    List<ObservationEntity> findByPatientId(Integer patientId);
}