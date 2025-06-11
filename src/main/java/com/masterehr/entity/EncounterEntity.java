package com.masterehr.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "Encounters")
public class EncounterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "encounter_id")
    private Integer encounterId;

    @Column(name = "patient_id", nullable = false)
    private Integer patientId;

    @Column(name = "visit_date", nullable = false)
    private LocalDateTime visitDate;

    @Column(name = "clinic", length = 100)
    private String clinic;

    @Column(name = "reason_for_visit")
    private String reasonForVisit;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "discharge_date")
    private LocalDateTime dischargeDate;

    @Lob
    @Column(name = "resource_json", columnDefinition = "NVARCHAR(MAX)")
    private String resourceJson;

    // Getters and Setters for all fields...
    // (You can use your IDE to generate these quickly)

    public Integer getEncounterId() { return encounterId; }
    public void setEncounterId(Integer encounterId) { this.encounterId = encounterId; }
    public Integer getPatientId() { return patientId; }
    public void setPatientId(Integer patientId) { this.patientId = patientId; }
    public LocalDateTime getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDateTime visitDate) { this.visitDate = visitDate; }
    public String getClinic() { return clinic; }
    public void setClinic(String clinic) { this.clinic = clinic; }
    public String getReasonForVisit() { return reasonForVisit; }
    public void setReasonForVisit(String reasonForVisit) { this.reasonForVisit = reasonForVisit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getDischargeDate() { return dischargeDate; }
    public void setDischargeDate(LocalDateTime dischargeDate) { this.dischargeDate = dischargeDate; }
    public String getResourceJson() { return resourceJson; }
    public void setResourceJson(String resourceJson) { this.resourceJson = resourceJson; }
}