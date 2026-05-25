**MOTHERHOOD JOURNEY**

*Database Design & System Architecture Handout*

Complete guide to schema design, government integration,

Geo-identity RBAC, and scalable architecture decisions

| Organisation | IgireRwanda Organization |
| :---- | :---- |
| **Programme** | SheCanCode Bootcamp  Project 5 of 5 |
| **Location** | Kacyiru, KG 549 St, 36 | Gasabo, Kigali, Rwanda |
| **Domain** | Public Health & Digital Services |
| **Prepared** | April 2026 |
| **Version** | v2.0  Government Integration Edition |

# **1\. Overview & Design Philosophy**

MotherHood Journey is a scalable digital health platform designed to replace paper-based maternal and child health records across Rwanda's health system. This document is the authoritative reference for the database schema, government integration design, Geo-identity model, and access control architecture.

## **1.1 Core Design Principles**

* **Multi-tenancy via facility\_id:** Every patient data table carries a facility\_id FK as the primary tenancy boundary. A single PostgreSQL instance serves an entire network of health centers while keeping data strictly isolated.

* **Geo-identity first:** Every user, mother, facility, and report is pinned to Rwanda's 5-level administrative hierarchy via a Geo\_location\_id FK. This drives both RBAC scoping and CHW assignment automatically.

* **Outbox pattern for resilience:** All government API calls are written to Gov\_sync\_log before execution. A background job retries with exponential backoff. Idempotency keys prevent duplicate submissions critical in low connectivity environments.

* **Consent before data sharing:** Consent records are checked before any patient data is shared with government systems, complying with Rwanda Law No. 058/2021 on Personal Data Protection.

* **Seed tables over duplication:** Reference data (geo\_locations,vaccination\_schedules) is seeded once via Flyway migration and referenced everywhere  never duplicated. This ensures data consistency across the entire system.

* **Polymorphic patient references:** Health visits and appointments use patient\_ref\_id \+ patient\_type to serve both mothers and children from one table, avoiding nullable FK anti-patterns.

## **1.2 Scalability Architecture**

| Scale concern | Design decision |
| ----- | ----- |
| **Multiple facilities** | facility\_id on every patient table full row level multi tenancy with no data leakage |
| **Government roles** | scoped\_geo\_ids UUID on government\_users sector-scoped RBAC without extra join tables |
| **Analytics load** | Dedicated read replica for all analytics and HMIS export queries zero impact on write path |
| **Gov API failures** | Outbox pattern (gov\_sync\_log)  at-least-once delivery with dead-letter alerting after 5 retries |
| **Connectivity gaps** | Offline-first CHW app with local SQLite sync queue; USSD access requires no internet |
| **Audit volume** | audit\_log partitioned by month in production  7-year retention per MoH policy |
| **Future multi-facility** | facility\_groups table \+ SUPER\_ADMIN role ready to add  schema supports it with no migrations |

# **2\. Database Schema Table Reference**

The schema is organized into 10 logical groups across 16 tables. All tables use UUID primary keys, Flyway-managed migrations, and PostgreSQL as the target engine.

## **Group 1 Geo-Identity & Administrative**

### **Table: Geo\_locations**

The single most important reference table in the system. Rwanda's entire administrative hierarchy, 5 provinces, 30 districts, 416 sectors, 2,148 cells, and \~14,000 villages  is seeded once and referenced by every entity. **Never duplicated, always joined.**

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK | Unique identifier |
| **province** | VARCHAR(64) | NOT NULL | One of 5 Rwanda provinces |
| **district** | VARCHAR(64) | NOT NULL | One of 30 districts |
| **sector** | VARCHAR(64) | NOT NULL | One of 416 sectors  primary RBAC boundary |
| **cell** | VARCHAR(64) | NOT NULL | One of 2,148 cells CHW assignment unit |
| **village** | VARCHAR(64) | NOT NULL | \~14,000 villages finest location granularity |
| **postal\_code** | VARCHAR(16) |  | Rwanda postal code |
| **latitude** | FLOAT |  | GPS latitude for mapping |
| **longitude** | FLOAT |  | GPS longitude for mapping |
| **active** | BOOLEAN | DEFAULT true | Soft-delete for boundary changes |
| **created\_at** | TIMESTAMP | DEFAULT now() | Seed timestamp |

| Why geo\_locations is the backbone |
| :---- |
| Every user, mother, facility, appointment, service request, and government report carries a geo\_location\_id FK. |
| A DISTRICT\_OFFICER's scoped\_geo\_ids array is simply a list of sector UUIDs from this table. |
| Spring @PreAuthorize checks: resource.geo\_location.sector IN officer.scoped\_geo\_ids. |
| Analytics can GROUP BY any level: province, district, sector, cell, or village  one JOIN, any granularity. |
| Seeded via Flyway V1\_\_seed\_geo\_locations.sql 14,000+ rows, runs once at deploy. |

## **Group 2 Users & Roles**

### **Table: users**

All six roles share a single users table. Government roles additionally have a government\_users row. This enables one unified JWT authentication pipeline for the entire system.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **facility\_id** | UUID | FK → facilities | Home/registering facility |
| **geo\_location\_id** | UUID | FK → geo\_locations, NOT NULL | Village-level location |
| **national\_id** | VARCHAR(32) | UNIQUE, NOT NULL | Rwanda NID verified via NIDA API |
| **phone\_number** | VARCHAR(20) | UNIQUE, NOT NULL | Primary contact \+ SMS delivery target |
| **password\_hash** | VARCHAR(255) | NOT NULL | bcrypt hashed |
| **role** | VARCHAR(32) | NOT NULL | PATIENT | HEALTH\_WORKER | FACILITY\_ADMIN | DISTRICT\_OFFICER | GOVERNMENT\_ANALYST | MOH\_ADMIN |
| **first\_name** | VARCHAR(64) | NOT NULL |  |
| **last\_name** | VARCHAR(64) | NOT NULL |  |
| **preferred\_language** | VARCHAR(8) | DEFAULT 'rw' | rw=Kinyarwanda, en=English, fr=French |
| **active** | BOOLEAN | DEFAULT true | Soft-disable no hard deletes on users |
| **last\_login** | TIMESTAMP |  | Used for inactivity detection |

### **Table: government\_users**

A 1:1 extension of users for the three government roles. The scoped\_geo\_ids PostgreSQL array is the core of geographic RBAC; it stores the sector or district UUIDs that the officer is authorized to access.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **user\_id** | UUID | FK → users, UNIQUE | 1:1 link to base user record |
| **gov\_role** | VARCHAR(32) | NOT NULL | DISTRICT\_OFFICER | GOVERNMENT\_ANALYST | MOH\_ADMIN |
| **ministry** | VARCHAR(128) | NOT NULL | Employing ministry or agency |
| **employee\_id** | VARCHAR(64) | UNIQUE, NOT NULL | Government employee number |
| **scoped\_geo\_ids** | UUID\[\] |  | PostgreSQL array of authorized geo\_location UUIDs |
| **can\_export** | BOOLEAN | DEFAULT false | Permission to export CSV/Excel reports |
| **can\_push\_hmis** | BOOLEAN | DEFAULT false | Permission to push data to MoH HMIS |
| **last\_audit** | TIMESTAMP |  | Timestamp of last access audit review |

## **Group 3 Facilities**

### **Table: facilities**

Health centers, hospitals, and CHW posts. facility\_id is the multi-tenancy boundary every patient record, visit, and notification belongs to exactly one facility.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **geo\_location\_id** | UUID | FK → geo\_locations, NOT NULL | Physical location of facility |
| **name** | VARCHAR(128) | NOT NULL | Full facility name |
| **facility\_code** | VARCHAR(32) | UNIQUE, NOT NULL | Rwanda MoH official facility code |
| **facility\_type** | VARCHAR(32) | NOT NULL | HEALTH\_CENTER | HOSPITAL | CLINIC | CHW\_POST |
| **district** | VARCHAR(64) | NOT NULL | Denormalized for fast filter queries |
| **phone** | VARCHAR(20) |  | Facility contact number |
| **active** | BOOLEAN | DEFAULT true | Soft-delete |

## **Group 4 Mothers & Pregnancies**

### **Table: mothers**

The primary patient entity. Every mother has exactly one user account (1:1) and is registered at one facility. The NIDA verification flow sets nida\_verified\_status and geo\_location\_id automatically from the national ID lookup response.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **user\_id** | UUID | FK → users, UNIQUE | 1:1 — login account for this mother |
| **facility\_id** | UUID | FK → facilities, NOT NULL | Registering facility |
| **geo\_location\_id** | UUID | FK → geo\_locations, NOT NULL | Village — cross-checked vs NIDA response |
| **health\_id** | VARCHAR(32) | UNIQUE, NOT NULL | Printable digital health ID (e.g. MH-2026-04821) |
| **nida\_verified\_status** | VARCHAR(16) | DEFAULT 'PENDING' | PENDING | VERIFIED | FAILED | MANUAL |
| **date\_of\_birth** | DATE | NOT NULL |  |
| **education\_level** | VARCHAR(32) |  | NONE | PRIMARY | SECONDARY | TERTIARY |
| **registered\_at** | TIMESTAMP | DEFAULT now() |  |

### **Table: pregnancies**

A mother can have multiple pregnancies over time. Each pregnancy has its own assigned CHW, EDD, and outcome. **Never embed pregnancy data in the mothers row this enables full obstetric history.**

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **mother\_id** | UUID | FK → mothers, NOT NULL |  |
| **lmp\_date** | DATE |  | Last menstrual period date |
| **edd** | DATE |  | Estimated due date (LMP \+ 280 days) |
| **status** | VARCHAR(16) | DEFAULT 'ACTIVE' | ACTIVE | DELIVERED | LOST | TRANSFERRED |
| **gravida** | INT |  | Total number of pregnancies (including this one) |
| **para** | INT |  | Number of previous live births |
| **assigned\_chw\_id** | UUID | FK → users | Assigned community health worker |
| **outcome\_notes** | TEXT |  | Free-text delivery notes |

## **Group 5 Children & Vaccination**

### **Table: children**

Registered at birth. The birth\_certificate\_no is the digital replacement for the paper mutuelles card issued at registration, stored securely, and available as a PDF via JasperReports.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **mother\_id** | UUID | FK → mothers, NOT NULL |  |
| **facility\_id** | UUID | FK → facilities, NOT NULL | Birth facility |
| **geo\_location\_id** | UUID | FK → geo\_locations, NOT NULL |  |
| **birth\_certificate\_no** | VARCHAR(64) | UNIQUE | Digital birth certificate number |
| **first\_name** | VARCHAR(64) |  |  |
| **gender** | VARCHAR(8) |  | MALE | FEMALE | UNKNOWN |
| **date\_of\_birth** | DATE | NOT NULL |  |
| **birth\_weight\_kg** | FLOAT |  |  |
| **delivery\_type** | VARCHAR(16) |  | NORMAL | CAESAREAN | ASSISTED |
| **health\_status** | VARCHAR(16) | DEFAULT 'HEALTHY' | HEALTHY | AT\_RISK | CRITICAL — updated by CHW |

### **Table: vaccination\_schedules  (seed/reference table)**

Rwanda's EPI schedule seeded via Flyway. Defines when each vaccine dose is due, in days from birth. Updated via MoH HMIS sync when the schedule changes.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **vaccine\_name** | VARCHAR(64) | NOT NULL | e.g. Pentavalent Vaccine |
| **antigen\_code** | VARCHAR(16) | UNIQUE, NOT NULL | e.g. BCG, OPV0, PENTA1, ROTA1, MMR |
| **dose\_number** | INT | NOT NULL | Dose sequence within the vaccine series |
| **due\_age\_days** | INT | NOT NULL | Days from birth when dose is due |
| **window\_days** | INT | DEFAULT 7 | Acceptable window before/after due date |
| **is\_mandatory** | BOOLEAN | DEFAULT true | False \= deprecated but kept for history |

### **Table: vaccination\_records**

Created automatically when a child is registered one row per EPI schedule entry per child. A Spring @Scheduled cron job scans for PENDING records past due\_date \+ window\_days and flips status to OVERDUE, triggering an SMS reminder.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **child\_id** | UUID | FK → children, NOT NULL |  |
| **schedule\_id** | UUID | FK → vaccination\_schedules, NOT NULL | Which vaccine dose |
| **administered\_by** | UUID | FK → users | Health worker who gave the dose |
| **facility\_id** | UUID | FK → facilities, NOT NULL |  |
| **administered\_date** | DATE |  | NULL until administered |
| **due\_date** | DATE | NOT NULL | date\_of\_birth \+ due\_age\_days — computed at registration |
| **lot\_number** | VARCHAR(32) |  | Vaccine batch for pharmacovigilance |
| **status** | VARCHAR(16) | DEFAULT 'PENDING' | PENDING | ADMINISTERED | MISSED | OVERDUE |

## **Group 6 Clinical Visits**

### **Table: health\_visits**

Supports both mother visits (ANC, PNC) and child visits (growth monitoring, sick child) from one table via a polymorphic patient reference. Includes full vitals set including MUAC for malnutrition screening.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **patient\_ref\_id** | UUID | NOT NULL | Polymorphic  mothers.id or children.id |
| **patient\_type** | VARCHAR(8) | NOT NULL | MOTHER | CHILD |
| **facility\_id** | UUID | FK → facilities, NOT NULL |  |
| **health\_worker\_id** | UUID | FK → users, NOT NULL |  |
| **geo\_location\_id** | UUID | FK → geo\_locations | Location of visit (may differ from home) |
| **visit\_datetime** | TIMESTAMP | NOT NULL |  |
| **visit\_type** | VARCHAR(16) | NOT NULL | ANC | PNC | IMMUNIZATION | SICK\_CHILD | GROWTH\_MONITORING |
| **weight\_kg** | FLOAT |  |  |
| **height\_cm** | FLOAT |  |  |
| **systolic\_bp** | INT |  |  |
| **diastolic\_bp** | INT |  |  |
| **muac\_cm** | FLOAT |  | Mid-upper arm circumference malnutrition screen |
| **chief\_complaint** | TEXT |  |  |
| **notes** | TEXT |  |  |

### **Table: diagnoses**

Multiple ICD-10 diagnoses per visit. The icd10\_code subset is synced from MoH HMIS on schedule updates.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **visit\_id** | UUID | FK → health\_visits, NOT NULL |  |
| **icd10\_code** | VARCHAR(8) | NOT NULL | ICD-10 code from MoH-approved subset |
| **description** | VARCHAR(255) | NOT NULL | Human-readable description |
| **severity** | VARCHAR(16) |  | MILD | MODERATE | SEVERE |
| **is\_primary** | BOOLEAN | DEFAULT false | Primary presenting condition flag |

### **Table: prescriptions**

Medications issued per visit. Forms the basis of medication adherence SMS nudges (stretch goal).

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **visit\_id** | UUID | FK → health\_visits, NOT NULL |  |
| **medication\_name** | VARCHAR(128) | NOT NULL |  |
| **dosage** | VARCHAR(64) | NOT NULL | e.g. 500mg |
| **frequency** | VARCHAR(64) | NOT NULL | e.g. Twice daily with food |
| **duration\_days** | INT | NOT NULL | Drives scheduled SMS adherence reminders |
| **instructions** | TEXT |  | Additional patient instructions |

## **Group 7 Appointments**

### **Table: appointments**

Drives the analytics dashboard no-show rate and capacity planning metrics. The reminder\_sent flag prevents duplicate SMS reminders.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **patient\_ref\_id** | UUID | NOT NULL | Polymorphic mothers.id or children.id |
| **patient\_type** | VARCHAR(8) | NOT NULL | MOTHER | CHILD |
| **facility\_id** | UUID | FK → facilities, NOT NULL |  |
| **health\_worker\_id** | UUID | FK → users | Assigned health worker |
| **geo\_location\_id** | UUID | FK → geo\_locations |  |
| **scheduled\_at** | TIMESTAMP | NOT NULL |  |
| **appointment\_type** | VARCHAR(32) | NOT NULL | ANC | PNC | VACCINATION | GROWTH\_CHECK | FOLLOW\_UP |
| **status** | VARCHAR(16) | DEFAULT 'SCHEDULED' | SCHEDULED | COMPLETED | NO\_SHOW | CANCELLED |
| **reminder\_sent** | BOOLEAN | DEFAULT false | Set by notification cron 24h before scheduled\_at |

## **Group 8 Consent**

### **Table: consent\_records**

Required before any mother's data is shared with government systems. The system checks for an active GOV\_DATA\_SHARE consent record before every HMIS push or DISTRICT\_OFFICER query. Legal basis is logged per Rwanda Law No. 058/2021.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **mother\_id** | UUID | FK → mothers, NOT NULL |  |
| **consent\_type** | VARCHAR(32) | NOT NULL | GOV\_DATA\_SHARE | SMS\_REMINDERS | RESEARCH | FACILITY\_TRANSFER |
| **granted** | BOOLEAN | NOT NULL | true \= consent given, false \= explicitly denied |
| **granted\_by\_role** | VARCHAR(32) |  | Role of user who recorded consent (CHW, PATIENT) |
| **consented\_at** | TIMESTAMP | NOT NULL |  |
| **expires\_at** | TIMESTAMP |  | NULL \= indefinite; else re-consent required |
| **legal\_basis** | VARCHAR(128) |  | Rwanda Law No. 058/2021 on Personal Data Protection |
| **revoked\_at** | TIMESTAMP |  | Timestamp if consent was later withdrawn |

## **Group 9 Government Integration**

### **Table: service\_requests**

Citizens and CHWs submit formal government service requests here. The request lifecycle is: PENDING → UNDER\_REVIEW → APPROVED → IREMBO\_SUBMITTED → COMPLETED. Each status transition triggers an SMS notification to the requester.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **requester\_id** | UUID | FK → users, NOT NULL | Mother or CHW who filed the request |
| **facility\_id** | UUID | FK → facilities, NOT NULL |  |
| **geo\_location\_id** | UUID | FK → geo\_locations, NOT NULL |  |
| **service\_type** | VARCHAR(32) | NOT NULL | BIRTH\_CERT | VACCINATION\_CARD | REFERRAL | HEALTH\_SUMMARY | REPRINT |
| **status** | VARCHAR(24) | DEFAULT 'PENDING' | PENDING | UNDER\_REVIEW | APPROVED | REJECTED | IREMBO\_SUBMITTED | COMPLETED |
| **reference\_no** | VARCHAR(32) | UNIQUE, NOT NULL | Human-readable e.g. SR-2026-00042 |
| **irembo\_ticket\_id** | VARCHAR(64) |  | External Irembo portal reference number |
| **payload** | JSONB |  | Service-specific form data — varies by service\_type |
| **rejection\_reason** | TEXT |  | Set when status \= REJECTED |
| **submitted\_at** | TIMESTAMP | NOT NULL |  |
| **resolved\_at** | TIMESTAMP |  |  |
| **resolved\_by** | UUID | FK → users | FACILITY\_ADMIN or DISTRICT\_OFFICER |

### **Table: service\_request\_docs**

Supporting documents per service request. SHA-256 file\_hash ensures integrity any modification to the stored file is detectable.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **request\_id** | UUID | FK → service\_requests, NOT NULL |  |
| **document\_type** | VARCHAR(32) | NOT NULL | ID\_COPY | BIRTH\_PROOF | FACILITY\_LETTER | OTHER |
| **file\_path** | VARCHAR(512) | NOT NULL | Path in file storage bucket |
| **file\_hash** | VARCHAR(64) | NOT NULL | SHA-256 hash for tamper detection |
| **uploaded\_at** | TIMESTAMP | DEFAULT now() |  |

### **Table: gov\_sync\_log  (outbox pattern)**

Every government API call is written here FIRST, then executed by a background job. This outbox pattern  guarantees at-least-once delivery even when NIDA, HMIS, or Irembo are temporarily unreachable. Status reaches DEAD\_LETTER after 5 retries, triggering an admin alert.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **facility\_id** | UUID | FK → facilities | Originating facility |
| **target\_system** | VARCHAR(16) | NOT NULL | NIDA | HMIS | IREMBO | RURA |
| **sync\_type** | VARCHAR(32) | NOT NULL | IDENTITY\_VERIFY | REPORT\_PUSH | TICKET\_SUBMIT | SCHEDULE\_PULL |
| **status** | VARCHAR(16) | DEFAULT 'PENDING' | PENDING | IN\_FLIGHT | SUCCEEDED | FAILED | DEAD\_LETTER |
| **idempotency\_key** | VARCHAR(128) | UNIQUE, NOT NULL | Prevents duplicate submissions on retry |
| **payload\_hash** | VARCHAR(64) |  | SHA-256 of request payload |
| **retry\_count** | INT | DEFAULT 0 | Incremented on each retry |
| **error\_message** | TEXT |  | Last error from government API |
| **synced\_at** | TIMESTAMP |  | Timestamp of last attempt |
| **next\_retry\_at** | TIMESTAMP |  | Exponential backoff schedule |

### **Table: gov\_reports**

Aggregated statistical reports generated by government analysts and pushed to MoH HMIS. Contains no individual patient records, the aggregated JSONB field holds computed statistics only.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **generated\_by** | UUID | FK → users, NOT NULL | MOH\_ADMIN or GOVERNMENT\_ANALYST |
| **geo\_location\_id** | UUID | FK → geo\_locations, NOT NULL | Geographic scope of the report |
| **report\_type** | VARCHAR(32) | NOT NULL | VACCINATION\_COVERAGE | ANC\_ATTENDANCE | BIRTH\_REGISTRATION | MATERNAL\_HEALTH |
| **period** | VARCHAR(16) | NOT NULL | e.g. 2026-Q1 | 2026-04 | 2026 |
| **scope\_level** | VARCHAR(16) | NOT NULL | NATIONAL | PROVINCE | DISTRICT | SECTOR |
| **aggregates** | JSONB | NOT NULL | Computed stats — no individual patient data |
| **hmis\_push\_status** | VARCHAR(16) | DEFAULT 'NOT\_PUSHED' | NOT\_PUSHED | QUEUED | PUSHED | FAILED |
| **pushed\_at** | TIMESTAMP |  |  |

## **Group 10 Notifications & Audit**

### **Table: sms\_notifications**

Outbound SMS via Africa's Talking API. A cron job scans QUEUED rows where scheduled\_at ≤ now() and calls the AT API. The at\_message\_id enables webhook-based delivery status updates.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **recipient\_user\_id** | UUID | FK → users, NOT NULL |  |
| **phone\_number** | VARCHAR(20) | NOT NULL | Denormalized at send time — number may change later |
| **message\_body** | VARCHAR(320) | NOT NULL | Max 2 SMS segments (160 chars each) |
| **notification\_type** | VARCHAR(32) | NOT NULL | VACCINATION\_REMINDER | APPOINTMENT | HEALTH\_TIP | SERVICE\_STATUS | EMERGENCY |
| **status** | VARCHAR(16) | DEFAULT 'QUEUED' | QUEUED | SENT | DELIVERED | FAILED |
| **at\_message\_id** | VARCHAR(64) |  | Africa's Talking ref for delivery tracking |
| **scheduled\_at** | TIMESTAMP | NOT NULL | When to send |
| **sent\_at** | TIMESTAMP |  | Actual send time |
| **retry\_count** | INT | DEFAULT 0 |  |

### **Table: audit\_log**

PHI access audit log. Required for Rwanda Data Protection Law compliance. **Immutable no UPDATE or DELETE operations are permitted on this table.** Retention: 7 years per MoH policy. Partitioned by month in production.

| Column | Type | Constraints | Description |
| ----- | ----- | ----- | ----- |
| **id** | UUID | PK |  |
| **user\_id** | UUID | FK → users, NOT NULL | Who performed the action |
| **action** | VARCHAR(32) | NOT NULL | READ | CREATE | UPDATE | DELETE | EXPORT | LOGIN | LOGOUT |
| **resource\_type** | VARCHAR(32) | NOT NULL | Table name e.g. mothers, children, gov\_reports |
| **resource\_id** | UUID |  | ID of the accessed record |
| **geo\_location\_id** | UUID | FK → geo\_locations | Location context of the action |
| **ip\_address** | VARCHAR(45) |  | IPv4 or IPv6 |
| **user\_agent** | VARCHAR(255) |  | Browser/client identifier |
| **success** | BOOLEAN | DEFAULT true | false \= failed attempt (important for security monitoring) |
| **fail\_reason** | VARCHAR(128) |  | Reason for failure |
| **created\_at** | TIMESTAMP | NOT NULL | Immutable timestamp |

# **3\. Government Integration Architecture**

MotherHood Journey connects to four Rwandan government systems via a secure API gateway and asynchronous event sync bus. All integrations are consent-gated and fully logged.

## **3.1 Connected Systems**

| System | Authority | Integration purpose |
| ----- | ----- | ----- |
| **NIDA** | National ID Agency | Identity verification at registration  national ID lookup pre-fills mother record and geo\_location\_id. Sets nida\_verified\_status \= VERIFIED. |
| **RURA / Telcos** | Rwanda Utilities Reg. Auth. | USSD gateway for low-end phones; SMS routing via Africa's Talking for vaccination reminders and service status updates. |
| **MoH HMIS** | Ministry of Health | Push aggregated health statistics via gov\_reports. Pull updated vaccination schedules and ICD-10 code subsets. |
| **Irembo** | Government of Rwanda | Submit citizen service requests (birth certificates, referral letters, reprints). irembo\_ticket\_id stored for tracking. |

## **3.2 Outbox Pattern How Government Sync Works**

1. **Write first:** System writes to gov\_sync\_log with status=PENDING and a unique idempotency\_key before making any external call.

2. **Background job:** A Spring @Scheduled job polls gov\_sync\_log for PENDING rows and sets status=IN\_FLIGHT, then calls the government API.

3. **Success:** status → SUCCEEDED, synced\_at set. Done.

4. **Failure:** status → FAILED, error\_message recorded, retry\_count incremented, next\_retry\_at set with exponential backoff (2^retry minutes).

5. **Dead letter:** After 5 retries, status → DEAD\_LETTER. Admin is alerted via dashboard. Manual intervention required.

| Why idempotency\_key is critical |
| :---- |
| If a request times out, we do not know if the government API processed it. |
| On retry, we send the same idempotency\_key  the government API returns the cached result instead of processing twice. |
| This prevents duplicate birth certificate numbers, duplicate HMIS submissions, and duplicate Irembo tickets. |
| Key format: {target\_system}:{sync\_type}:{resource\_id}:{timestamp}  guaranteed globally unique. |

## **3.3 NIDA Verification Flow**

6. CHW enters the national ID number in the registration form.

7. The system creates a gov\_sync\_log entry for NIDA IDENTITY\_VERIFY and queues it.

8. Background job calls NIDA e-indangamuntu API (OAuth2 \+ mTLS).

9. NIDA returns: full name, date of birth, administrative location (village level).

10. The system sets mothers. geo\_location\_id from NIDA village response and nida\_verified\_status \= VERIFIED.

11. Duplicate prevention: unique index on mothers.user\_id and users.national\_id prevents double registration.

# **4\. Geo-Identity & Role-Based Access Control**

Every user, mother, facility, and government report is pinned to Rwanda's administrative hierarchy. RBAC is enforced at the database row level via geo\_location\_id comparisons against a user's authorized scope.

## **4.1 Rwanda Administrative Hierarchy**

| Level | Count | Role & system function |
| ----- | ----- | ----- |
| **Province** | 5 | National-level filter. MOH\_ADMIN reports span all provinces. |
| **District** | 30 | GOVERNMENT\_ANALYST scope. District-level aggregated statistics. |
| **Sector** | 416 | PRIMARY RBAC BOUNDARY. DISTRICT\_OFFICER scoped\_geo\_ids contains sector UUIDs. Data never crosses sectors without authorization. |
| **Cell** | 2,148 | CHW assignment unit. A health worker is assigned to one or more cells. |
| **Village** | \~14,000 | Finest granularity. Pinned to every mothers row via geo\_location\_id FK. Cross-checked against NIDA response. |

## **4.2 Full RBAC Permission Matrix**

| Resource | PATIENT | HEALTH\_WORKER | FAC\_ADMIN | DIST\_OFFICER | GOV\_ANALYST | MOH\_ADMIN |
| ----- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Own profile / children** | **Full** | **Read** | **Full** | **None** | **None** | **None** |
| **Patient records** | **Own only** | **Full** | **Full** | **None** | **None** | **None** |
| **Vaccination records** | **Read own** | **Full** | **Full** | **None** | **None** | **None** |
| **Service requests (file)** | **Own only** | **Submit \+ track** | **Approve / reject** | **Sector scope** | **None** | **Full** |
| **Geo-scoped patient lists** | **None** | **Sector scope** | **Facility scope** | **Sector scope** | **District agg.** | **National** |
| **Analytics & reports** | **None** | **Facility read** | **Facility full** | **Sector scope** | **District scope** | **National** |
| **HMIS push / export** | **None** | **None** | **None** | **None** | **Read only** | **Push \+ export** |
| **Gov sync log** | **None** | **None** | **Read own** | **Sector scope** | **Read all** | **Full** |
| **Consent records** | **Own only** | **Read assigned** | **Facility read** | **None** | **None** | **National read** |
| **User management** | **None** | **None** | **Facility only** | **None** | **None** | **Full** |
| **PDF / birth certificate** | **Own child** | **Assigned patient** | **Full** | **Sector scope** | **None** | **Full** |

# **5\. How to Use the dbdiagram.io Schema**

The companion file motherhood\_journey.dbml contains the complete DBML schema for all 16 tables with field definitions, constraints, indexes, relationships, and notes. To view the interactive diagram:

12. Open your browser and go to https://dbdiagram.io  create a free account if needed.

13. Click **New Diagram** in the top left.

14. Open motherhood\_journey.dbml and copy its entire contents.

15. Paste into the left-hand editor panel on dbdiagram.io the visual diagram renders automatically on the right.

16. Use **Export → Export to PostgreSQL** to generate the full SQL CREATE TABLE script with indexes and constraints ready to run.

| What to look for in the diagram |
| :---- |
| geo\_locations is in the centre and notice how almost every table has a line connecting to it. |
| government\_users has a one-to-one (||) line to users  it is an extension, not a separate login system. |
| vaccination\_records has a unique index on (child\_id, schedule\_id)  each child gets each dose exactly once. |
| gov\_sync\_log has no FK to patients; it is a technical log, not a clinical record. |
| audit\_log has no outgoing FKs to patients; it references only user\_id for accountability. |

## **5.1 Key Relationships Explained**

* **users → government\_users (1:1):** A government user IS a user. The 1:1 join gives them their scoped\_geo\_ids and permission flags without polluting the base users table.

* **mothers → pregnancies (1:many):** One mother, many pregnancies over her lifetime. Each pregnancy has its own CHW assignment and the outcome of full obstetric history is preserved.

* **children → vaccination\_records (1:many):** At birth, one record per EPI schedule entry is created for the child. As doses are administered, the status field updates from PENDING to ADMINISTERED.

* **health\_visits → diagnoses \+ prescriptions (1:many):** A visit can have multiple diagnoses (up to one primary) and multiple prescriptions.

* **service\_requests → gov\_sync\_log (indirect):** When a service request is approved, a gov\_sync\_log entry is created to push it to Irembo. The service request does not directly call the API.

* **mothers → consent\_records (1:many):** A mother can grant different consent types independently. GOV\_DATA\_SHARE is checked before every government API call involving her data.

# **6\. Security & Compliance**

## **6.1 Data Protection**

* **Encryption at rest:** PHI fields encrypted via PostgreSQL pgcrypto. Backups encrypted with AES-256.

* **Transport:** All government API calls use OAuth2 client credentials with mutual TLS (mTLS). JWT access tokens expire in 15 minutes.

* **PHI audit:** Every patient record access is logged to audit\_log. Immutable no UPDATE/DELETE allowed. 7-year retention.

* **Rwanda Law No. 058/2021:** Consent is captured in consent\_records before any data is shared with government systems. Legal basis is documented per field.

* **Idempotency:** All government sync operations use unique idempotency keys to prevent duplicate submissions on network retry.

## **6.2 Production Checklist**

| Requirement | Implementation |
| ----- | ----- |
| PHI never in logs | Structured logging  patient IDs only, never names or medical data |
| Password storage | bcrypt with cost factor ≥ 12 |
| JWT secret rotation | Kubernetes secret with 90-day rotation policy |
| Database backups | Daily encrypted snapshot to off-site S3-compatible storage |
| Rate limiting | Spring gateway: 100 req/min per IP, 1000 req/min per facility |
| SQL injection | Spring Data JPA parameterized queries only no native SQL with interpolation |
| CORS policy | Whitelist only Next.js origin and Irembo callback URLs |
| Dependency scanning | GitHub Actions \+ Dependabot on every PR |

# **7\. Stretch Goals & Scalability Roadmap**

| Feature | Priority | Schema change required |
| ----- | ----- | ----- |
| USSD full access | **High** | None  existing sms\_notifications table handles USSD session state |
| Offline CHW mobile app | **High** | None  sync via existing gov\_sync\_log outbox pattern |
| MoH HMIS FHIR compliance | **Medium** | Add fhir\_resource\_id to mothers, children, health\_visits |
| Medication adherence SMS | **Medium** | prescriptions.duration\_days already enables scheduling |
| Multi-facility admin group | **Medium** | Add facility\_groups table \+ SUPER\_ADMIN role to users.role enum |
| AI pregnancy risk scoring | **Low** | Add risk\_score FLOAT \+ risk\_factors JSONB to pregnancies table |
| e-learning for CHWs | **Low** | New modules: learning\_modules, chw\_progress tables |

Igihe Rwanda Organization  |  Kacyiru, KG 549 St, 36  |  Gasabo, Kigali, Rwanda

info@igirerwanda.org  |  \+250 788 473 533

Database Design Handout v2.0  |  April 2026  |  Confidential  SheCanCode Bootcamp Internal