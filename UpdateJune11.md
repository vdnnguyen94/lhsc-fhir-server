# FHIR Project Implementation Plan

## Overview
This plan walks through building a full-featured FHIR API gateway that auto-creates or updates EHR data between sender and receiver systems, showcases core R4 resources, document exchange, terminology, workflows, and advanced operations.

## 1. Metadata & Discovery
- **CapabilityStatement**  
  - Define supported resources, search parameters, formats, security.  
- **ImplementationGuide**  
  - Package custom profiles, examples, and extension definitions.

## 2. Core CRUD Endpoints
For each of these resources, implement `POST` (conditional-create), `PUT` (idempotent-update), `GET`, and `DELETE`:
- **Patient** (demographics, identifiers)  COMPLETED
- **Practitioner** / **Organization** (care providers)  
- **Encounter** (visits)  DOABLE
- **Condition**, **AllergyIntolerance**, **Immunization** (clinical statuses)  SKIP
- **Observation** / **DiagnosticReport** (measurements & reports)   DOABLE
- **MedicationRequest** / **MedicationAdministration** (orders & administrations)  SKIP
- **Procedure** (procedures performed) DOABLE

## 3. Document Exchange
- **Composition + DocumentReference + Binary**  
  1. Create a `Composition` as the narrative container.  
  2. Upload note files via `Binary` (empty TXT/CSV or real PDF).  
  3. Reference binaries in `DocumentReference.content.attachment.url`.  
  4. Bundle as type=`document` for atomic import.

## 4. Terminology Services
- **CodeSystem**: host your local lab codes or subset of LOINC.  
- **ValueSet**: define sets for Observation.code, Condition.code.  
- **ConceptMap**: map local codes → standard codes.

## 5. Workflow & Alerts
- **Task**: represent asynchronous work (e.g. “review abnormal lab”).  
- **Subscription**: push notifications to front-end when resources change (e.g. new Observation).

## 6. Custom Operations & Bulk Data
- **OperationDefinition** + `$upsertPatient`  
  - Encapsulate conditional-create/update logic in a single FHIR operation.  
- **Batch/Transaction Bundles**  
  - Support atomic multi-resource imports.  
- **Bulk Data Export** (`$export`)  
  - Allow clients to pull all or delta data as NDJSON.

## 7. Provenance & Auditing
- **Provenance**: record who/when/where for each resource change.  
- **AuditEvent**: capture system-level events (authn/authz, failures).

## 8. Advanced Query & GraphQL
- **Custom SearchParameter**: e.g. `Observation?patient=123&loinc=789-8`.  
- **GraphQL Endpoint**: allow nested data fetches (Patient→Encounter→Observations).

## 9. Storage & SQL Server 2022
- Store resource JSON in `NVARCHAR(MAX)` with JSON indexing.  
- Store attachments (`Binary.data`) in `VARBINARY(MAX)` or FILESTREAM.  
- Use computed columns or JSON_VALUE queries for speedy lookups.

## 10. Demo & Validation
- Populate sample data for 2 patients, labs, notes.  
- Demonstrate:  
  1. Automated `POST /Patient` with `If-None-Exist`.  
  2. `PUT /Observation/{id}` idempotent update.  
  3. Document Bundle import with Composition + Binary.  
  4. Subscription push to a test client on new critical lab value.  
- Validate with HAPI-FHIR built-in validator.

---
*End of Plan*
