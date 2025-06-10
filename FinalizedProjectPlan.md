# MasterEHR Project: Finalized Plan & Work Breakdown Structure (WBS)

This document outlines the complete, finalized plan for the MasterEHR project, incorporating all our architectural decisions. This is the master guide for building the custom FHIR servers that will simulate the LHSC and SJHC systems.

---

### ✅ **Phase 1: Core Infrastructure (Complete)**

* **1.1 `BackendFHIR` Service:** The intelligent gateway/bridge has been built and deployed. It can successfully transfer patient data between two FHIR endpoints.
* **1.2 `FrontendFHIR` Application:** A user interface has been built and deployed. It can successfully call the backend to trigger the transfer and display system status.
* **1.3 Environment Setup:** A live deployment environment (Render) has been established for all components.

---

### 🚀 **Phase 2: Build the LHSC FHIR Facade (`LhscFhirServer`)**

**Goal:** Replace the generic "sender" FHIR server with a custom, enterprise-grade FHIR server that serves data directly from your `LHSCDB` SQL Server.

#### **1.0 Project Setup & Initialization**

* **1.1** Create a new, empty repository on GitHub named `lhsc-fhir-server`.
* **1.2** Generate a new Spring Boot project named `LhscFhirServer` using the Spring Initializr.
* **1.3** **Add Dependencies:** In the `pom.xml`, include the following essential libraries:
    * `spring-boot-starter-web`
    * `hapi-fhir-server` (Note: This is the **server** library, not the client).
    * `spring-boot-starter-data-jpa` (For connecting to SQL databases).
    * `mssql-jdbc` (The specific driver for Microsoft SQL Server).
* **1.4** Push the initial project structure to the new GitHub repository.

#### **2.0 SQL Database Integration**

* **2.1** **Configure `application.properties`:**
    * **2.1.1** Add the database connection properties: `spring.datasource.url`, `spring.datasource.username`, and `spring.datasource.password` for your `LHSCDB` SQL Server.
    * **2.1.2** Configure the server to run on a specific port (e.g., `server.port=8090`).
* **2.2** **Create JPA Entity (`PatientEntity.java`):**
    * **2.2.1** Create a Java class that maps directly to your `Patients` table in `LHSCDB`.
    * **2.2.2** Use `@Entity` and `@Table` annotations. Add fields for `patient_id`, `first_name`, `last_name`, `dob`, etc., and map them to the database columns using `@Id` and `@Column`.
* **2.3** **Create JPA Repository (`PatientRepository.java`):**
    * **2.3.1** Create a Java interface that extends `JpaRepository<PatientEntity, Integer>`.
    * **2.3.2** Spring Data JPA will automatically provide all the necessary CRUD methods (`findById`, `save`, `delete`, etc.).

#### **3.0 FHIR Resource Provider (The "Translator")**

* **3.1** **Create `PatientProvider.java`:**
    * **3.1.1** This class is the core of the FHIR server. It will implement the `IResourceProvider` interface from the HAPI FHIR library.
    * **3.1.2** It will define how the server responds to FHIR requests for the `Patient` resource.
* **3.2** **Implement the `@Read` Method (Get Patient by ID):**
    * **3.2.1** This method will handle requests like `GET /Patient/123`.
    * **3.2.2** It will use the `PatientRepository` to query the SQL database for the patient with the matching ID.
    * **3.2.3** It will then **transform** the data from the `PatientEntity` (from the DB) into a standard FHIR `Patient` resource object (from the HAPI library).
    * **3.2.4** It will return the FHIR `Patient` object. HAPI handles the rest.
* **3.3** **Implement the `@Search` Method (Find Patients):**
    * **3.3.1** This method will handle requests like `GET /Patient?family=Smith`.
    * **3.3.2** You will write custom queries in your `PatientRepository` to search the database by last name, and then transform the list of results into a list of FHIR `Patient` objects.
* **3.4** **Implement the `@Create` and `@Update` Methods:**
    * **3.4.1** These methods will handle `POST` and `PUT` requests.
    * **3.4.2** They will do the reverse transformation: take an incoming FHIR `Patient` resource, convert it into a `PatientEntity`, and save it to the SQL database using the repository.

#### **4.0 Deployment**

* **4.1** Create a `Dockerfile` for the new `LhscFhirServer` project.
* **4.2** Deploy the service to Render, configuring the production database credentials as environment variables.

---

### 🚀 **Phase 3: Complete the Ecosystem**

**Goal:** Finalize the full, realistic interoperability workflow between the two simulated healthcare providers.

* **1.0 Build the SJHC FHIR Server:**
    * **1.1** Create a new `sihc-fhir-server` project.
    * **1.2** This server will be simpler. For now, it might use an in-memory database or connect to your second SQL Server VM. Its main purpose is to be the receiving endpoint.
* **2.0 Integrate the System:**
    * **2.1** Update the environment variables in your deployed `BackendFHIR` service.
    * **2.2** Change `FHIR_SERVER_SENDER_URL` to point to your new `LhscFhirServer` on Render.
    * **2.3** Change `FHIR_SERVER_RECEIVER_URL` to point to your new `SjhcFhirServer` on Render.
* **3.0 Build the SJHC Frontend:**
    * **3.1** Create a new React application, `sihc-frontend`.
    * **3.2** This UI will be for SJHC staff. It will list incoming patients and could have forms for manually correcting or enriching patient data that was transferred.


