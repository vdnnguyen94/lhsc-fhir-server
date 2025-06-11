USE LHSCDB;
GO

-- 1. Rename Visits → Encounters and align PK column
EXEC sp_rename 'Visits', 'Encounters';
GO
EXEC sp_rename 'Encounters.visit_id', 'encounter_id', 'COLUMN';
GO

-- 2. Extend Encounters with FHIR fields and JSON storage
ALTER TABLE Encounters
  ADD encounter_class   VARCHAR(100)    NULL,
      encounter_type    VARCHAR(100)    NULL,
      period_start      DATETIME        NULL,    -- maps to visit_date
      period_end        DATETIME        NULL,    -- maps to discharge_date
      resource_json     NVARCHAR(MAX)   NULL;
GO

-- 3. Add JSON storage to Patients
ALTER TABLE Patients
  ADD resource_json NVARCHAR(MAX) NULL;
GO

-- 4. Create Observations table
CREATE TABLE Observations (
    observation_id     INT IDENTITY(1,1) PRIMARY KEY,
    patient_id         INT NOT NULL,
    encounter_id       INT NULL,
    loinc_system       NVARCHAR(255) NOT NULL,
    loinc_code         NVARCHAR(50)  NOT NULL,
    status             VARCHAR(50)   NOT NULL    DEFAULT 'final',
    effective_datetime DATETIME      NOT NULL,
    issued_datetime    DATETIME      NULL,
    value_quantity     DECIMAL(18,4) NULL,
    value_unit         VARCHAR(50)   NULL,
    value_text         NVARCHAR(255) NULL,
    resource_json      NVARCHAR(MAX) NULL,
    created_at         DATETIME      NOT NULL    DEFAULT GETDATE(),
    CONSTRAINT FK_Obs_Patient FOREIGN KEY (patient_id)   REFERENCES Patients(patient_id),
    CONSTRAINT FK_Obs_Encounter FOREIGN KEY (encounter_id) REFERENCES Encounters(encounter_id)
);
GO

-- 5. Create Binaries table for attachments
CREATE TABLE Binaries (
    binary_id    INT IDENTITY(1,1) PRIMARY KEY,
    content_type NVARCHAR(100)   NOT NULL,
    data         VARBINARY(MAX)  NOT NULL,
    created_at   DATETIME        NOT NULL DEFAULT GETDATE()
);
GO

-- 6. Create Compositions table for clinical documents
CREATE TABLE Compositions (
    composition_id INT IDENTITY(1,1) PRIMARY KEY,
    patient_id     INT           NOT NULL,
    encounter_id   INT           NULL,
    title          NVARCHAR(255) NULL,
    status         VARCHAR(50)   NOT NULL DEFAULT 'final',
    type           NVARCHAR(100) NULL,
    date           DATETIME      NOT NULL DEFAULT GETDATE(),
    author         NVARCHAR(255) NULL,
    text_div       NVARCHAR(MAX) NULL,
    resource_json  NVARCHAR(MAX) NULL,
    created_at     DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Comp_Patient   FOREIGN KEY (patient_id)   REFERENCES Patients(patient_id),
    CONSTRAINT FK_Comp_Encounter FOREIGN KEY (encounter_id) REFERENCES Encounters(encounter_id)
);
GO

-- 7. Create DocumentReferences table linking to Binaries and Compositions
CREATE TABLE DocumentReferences (
    docref_id             INT IDENTITY(1,1) PRIMARY KEY,
    composition_id        INT               NULL,
    subject_patient_id    INT               NULL,
    subject_encounter_id  INT               NULL,
    binary_id             INT               NULL,
    content_type          NVARCHAR(100)     NULL,
    title                 NVARCHAR(255)     NULL,
    resource_json         NVARCHAR(MAX)     NULL,
    created_at            DATETIME          NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_DR_Comp       FOREIGN KEY (composition_id)       REFERENCES Compositions(composition_id),
    CONSTRAINT FK_DR_Patient    FOREIGN KEY (subject_patient_id)   REFERENCES Patients(patient_id),
    CONSTRAINT FK_DR_Encounter  FOREIGN KEY (subject_encounter_id) REFERENCES Encounters(encounter_id),
    CONSTRAINT FK_DR_Binary     FOREIGN KEY (binary_id)            REFERENCES Binaries(binary_id)
);
GO
