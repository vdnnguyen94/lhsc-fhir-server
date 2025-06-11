package com.masterehr.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "Observations")
public class ObservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "observation_id")
    private Integer observationId;

    @Column(name = "patient_id", nullable = false)
    private Integer patientId;

    @Column(name = "encounter_id")
    private Integer encounterId;

    @Column(name = "loinc_system", nullable = false)
    private String loincSystem;

    @Column(name = "loinc_code", nullable = false, length = 50)
    private String loincCode;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "effective_datetime", nullable = false)
    private LocalDateTime effectiveDatetime;

    @Column(name = "value_quantity", precision = 18, scale = 4)
    private BigDecimal valueQuantity;

    @Column(name = "value_unit", length = 50)
    private String valueUnit;

    @Lob
    @Column(name = "resource_json", columnDefinition = "NVARCHAR(MAX)")
    private String resourceJson;

    // Getters and Setters...

    public Integer getObservationId() { return observationId; }
    public void setObservationId(Integer observationId) { this.observationId = observationId; }
    public Integer getPatientId() { return patientId; }
    public void setPatientId(Integer patientId) { this.patientId = patientId; }
    public Integer getEncounterId() { return encounterId; }
    public void setEncounterId(Integer encounterId) { this.encounterId = encounterId; }
    public String getLoincSystem() { return loincSystem; }
    public void setLoincSystem(String loincSystem) { this.loincSystem = loincSystem; }
    public String getLoincCode() { return loincCode; }
    public void setLoincCode(String loincCode) { this.loincCode = loincCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getEffectiveDatetime() { return effectiveDatetime; }
    public void setEffectiveDatetime(LocalDateTime effectiveDatetime) { this.effectiveDatetime = effectiveDatetime; }
    public BigDecimal getValueQuantity() { return valueQuantity; }
    public void setValueQuantity(BigDecimal valueQuantity) { this.valueQuantity = valueQuantity; }
    public String getValueUnit() { return valueUnit; }
    public void setValueUnit(String valueUnit) { this.valueUnit = valueUnit; }
    public String getResourceJson() { return resourceJson; }
    public void setResourceJson(String resourceJson) { this.resourceJson = resourceJson; }
}