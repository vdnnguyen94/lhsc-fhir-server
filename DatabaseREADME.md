public class DatabaseReadme {
# LHSCDB: Database Schema and Architecture

This document provides a comprehensive overview of the `LHSCDB` SQL Server database. It serves as the internal, private "source-of-truth" for the custom `LhscFhirServer` application.

---

## 🎯 Purpose

The `LHSCDB` is designed to simulate a real-world hospital's legacy Electronic Health Record (EHR) database. It stores sensitive Patient Health Information (PHI) in a structured, relational format. The primary architectural pattern of the MasterEHR project is to expose the data within this database via a secure, modern, and standardized **FHIR Facade** (`LhscFhirServer`), rather than allowing direct access.

---

## Diagram: Entity-Relationship Model

The following diagram illustrates the core tables and their relationships within the database.

```mermaid
erDiagram
    Patients {
        int patient_id PK "Primary Key"
        uniqueidentifier patient_uid "De-identified UID"
        char(10) ohip_number "Unique OHIP"
        nvarchar(max) resource_json "FHIR JSON Cache"
    }

    Encounters {
        int encounter_id PK "Primary Key"
        int patient_id FK "Links to Patient"
        datetime visit_date "Start of Encounter"
        nvarchar(max) resource_json "FHIR JSON Cache"
    }

    Observations {
        int observation_id PK "Primary Key"
        int patient_id FK "Links to Patient"
        int encounter_id FK "Links to Encounter"
        nvarchar(50) loinc_code "Lab/Vital Code"
        decimal value_quantity "Numeric Result"
        nvarchar(max) resource_json "FHIR JSON Cache"
    }
    
    Compositions {
        int composition_id PK "Primary Key"
        int patient_id FK "Links to Patient"
        int encounter_id FK "Links to Encounter"
        nvarchar(max) text_div "Narrative Text"
    }

    DocumentReferences {
        int docref_id PK "Primary Key"
        int composition_id FK "Links to Composition"
        int binary_id FK "Links to Binary"
    }

    Binaries {
        int binary_id PK "Primary Key"
        varbinary(max) data "Raw File Data (PDF, etc.)"
    }

    Patients ||--o{ Encounters : "has"
    Patients ||--o{ Observations : "has"
    Encounters ||--o{ Observations : "contains"
    Patients ||--o{ Compositions : "has"
    Encounters ||--o{ Compositions : "contains"
    Compositions ||--|{ DocumentReferences : "is described by"
    Binaries ||--|{ DocumentReferences : "is referenced by"

```

---

## 🗂️ Table Breakdown

### `Patients`
* **Purpose:** Stores the master record for each patient, including demographics and unique identifiers.
* **Key Columns:**
    * `patient_id`: The internal primary key (auto-incrementing integer).
    * `ohip_number`: The unique Ontario Health Insurance Plan number. A `UNIQUE` constraint prevents duplicates.
    * `resource_json`: A cache that stores the complete FHIR `Patient` JSON object whenever a record is created or updated via the FHIR API. This improves read performance.

### `Encounters`
* **Purpose:** Represents a patient's visit or stay at the hospital (e.g., an emergency visit, a routine check-up). Formerly named `Visits`.
* **Key Columns:**
    * `encounter_id`: The primary key for the encounter.
    * `patient_id`: A foreign key linking this encounter directly to a record in the `Patients` table.

### `Observations`
* **Purpose:** Stores specific clinical measurements taken during an encounter, such as vital signs (heart rate, blood pressure) or lab results.
* **Key Columns:**
    * `observation_id`: The primary key for the observation.
    * `patient_id`: A foreign key linking the observation to the patient.
    * `encounter_id`: An optional foreign key linking the observation to a specific encounter.
    * `loinc_code`: Stores the standard LOINC code that identifies the type of observation.

### Document-Related Tables
* **`Compositions`, `DocumentReferences`, `Binaries`:** These tables work together to manage clinical documents. `Binaries` stores the raw file data (like a PDF), `DocumentReferences` holds metadata about the file, and `Composition` provides a clinical narrative or summary for the document.

---

## 🔄 FHIR Mapping & CRUD Operations

The `LhscFhirServer` application uses the HAPI FHIR library and Spring Data JPA to provide a complete set of CRUD operations for these tables, translating them into standard FHIR resources.

* **`Patients` Table ↔ `Patient` Resource:**
    * The `PatientProvider.java` class handles all `GET`, `POST`, `PUT`, and `DELETE` requests for FHIR `Patient` resources.
    * It contains the "translator" logic (`transformToFhirPatient` and `transformToPatientEntity`) to map between the SQL table columns and the standard FHIR `Patient` JSON structure.

* **`Encounters` Table ↔ `Encounter` Resource:**
    * The `EncounterProvider.java` class handles all FHIR operations for encounters.
    * It translates between the `Encounters` table and the FHIR `Encounter` resource.
    * It correctly creates a `subject` reference back to the `Patient` resource (e.g., `"subject": { "reference": "Patient/1" }`).

* **`Observations` Table ↔ `Observation` Resource:**
    * The `ObservationProvider.java` class handles all FHIR operations for observations.
    * It maps fields like `loinc_code`, `value_quantity`, and `effective_datetime` to their corresponding elements in the FHIR `Observation` resource.
    * It correctly links each observation back to both a `Patient` and an `Encounter`.
