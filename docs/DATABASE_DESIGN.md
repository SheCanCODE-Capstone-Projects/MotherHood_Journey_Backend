# MotherHood Journey — Database Design

**IgireRwanda Organization · SheCanCode Bootcamp · Kigali, Rwanda**  
Schema version: v2.0 (Government Integration Edition) · April 2026

---

## Contents

1. [Design Philosophy](#1-design-philosophy)
2. [Schema Overview — 16 Tables in 10 Groups](#2-schema-overview)
3. [Group-by-Group Reference](#3-group-by-group-reference)
4. [Key Relationships Explained](#4-key-relationships)
5. [Enum Reference](#5-enum-reference)
6. [Index Strategy](#6-index-strategy)
7. [Key Data Flows](#7-key-data-flows)
8. [RBAC Permission Matrix](#8-rbac-permission-matrix)
9. [Security & Compliance](#9-security--compliance)
10. [Flyway Migration Map](#10-flyway-migration-map)
11. [DBML Schema (dbdiagram.io)](#11-dbml-schema)

---

## 1. Design Philosophy

Five principles drive every schema decision:

| Principle | Implementation |
|-----------|---------------|
| **Multi-tenancy via `facility_id`** | Every patient data table carries a `facility_id` FK as the primary tenancy boundary. A single PostgreSQL instance serves the entire facility network with zero data leakage between facilities. |
| **Geo-identity first** | Every user, mother, facility, and report is pinned to Rwanda's 5-level administrative hierarchy via `geo_location_id`. This single FK drives both RBAC scoping and CHW assignment. |
| **Outbox pattern for resilience** | All government API calls are written to `gov_sync_log` before execution. A background job retries with exponential backoff. Idempotency keys prevent duplicate submissions in low-connectivity environments. |
| **Consent before data sharing** | `consent_records` is checked before any patient data is shared with government systems, per Rwanda Law No. 058/2021 on Personal Data Protection. |
| **Seed tables over duplication** | Reference data (`geo_locations`, `vaccination_schedules`) is seeded once via Flyway and referenced everywhere — never duplicated. This guarantees data consistency and enables analytics `GROUP BY` at any geographic level. |

---

## 2. Schema Overview

The schema is organized into 10 logical groups across **16 tables**. All tables use UUID primary keys, Flyway-managed migrations, and PostgreSQL 16 as the target engine.

```
┌──────────────────────────────────────────────────────────────────┐
│  GROUP 1   geo_locations                                         │
│  GROUP 2   users  ·  government_users                            │
│  GROUP 3   facilities                                            │
│  GROUP 4   mothers  ·  pregnancies                               │
│  GROUP 5   children  ·  vaccination_schedules  ·  vaccination_records │
│  GROUP 6   health_visits  ·  diagnoses  ·  prescriptions         │
│  GROUP 7   appointments                                          │
│  GROUP 8   consent_records                                       │
│  GROUP 9   service_requests  ·  service_request_docs             │
│            gov_sync_log  ·  gov_reports                          │
│  GROUP 10  sms_notifications  ·  audit_log                       │
└──────────────────────────────────────────────────────────────────┘
```

**Why `geo_locations` is the backbone:**
Almost every table has a `geo_location_id` FK pointing to this single reference table. Rwanda's full administrative hierarchy — 5 provinces, 30 districts, 416 sectors, 2,148 cells, and ~14,000 villages — is seeded once and referenced everywhere. Analytics can `GROUP BY` any level with a single join.

---

## 3. Group-by-Group Reference

### Group 1 — Geo-Identity & Administrative

#### `geo_locations`

The most important reference table. Seeded once from MoH data. Never mutated.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | Unique identifier |
| `province` | VARCHAR(64) | NOT NULL | One of Rwanda's 5 provinces |
| `district` | VARCHAR(64) | NOT NULL | One of 30 districts |
| `sector` | VARCHAR(64) | NOT NULL | One of 416 sectors — **primary RBAC boundary** |
| `cell` | VARCHAR(64) | NOT NULL | One of 2,148 cells — CHW assignment unit |
| `village` | VARCHAR(64) | NOT NULL | ~14,000 villages — finest location granularity |
| `postal_code` | VARCHAR(16) | | Rwanda postal code |
| `latitude` | FLOAT | | GPS latitude for mapping |
| `longitude` | FLOAT | | GPS longitude for mapping |
| `active` | BOOLEAN | DEFAULT true | Soft-delete for boundary changes |
| `created_at` | TIMESTAMP | DEFAULT now() | Seed timestamp |

**Key indexes:** Composite `(province, district, sector)` for cascade dropdown queries; `sector` for DISTRICT_OFFICER RBAC checks.

---

### Group 2 — Users & Roles

#### `users`

All six roles share a single table. This enables one unified JWT authentication pipeline.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `facility_id` | UUID | FK → facilities | Home or registering facility (nullable for government roles) |
| `geo_location_id` | UUID | FK → geo_locations, NOT NULL | Village-level location — pinned at registration |
| `national_id` | VARCHAR(32) | UNIQUE, NOT NULL | Rwanda NID — verified async via NIDA API |
| `phone_number` | VARCHAR(20) | UNIQUE, NOT NULL | Primary contact and SMS delivery target |
| `password_hash` | VARCHAR(255) | NOT NULL | bcrypt with cost factor ≥ 12 |
| `role` | VARCHAR(32) | NOT NULL | `PATIENT \| HEALTH_WORKER \| FACILITY_ADMIN \| DISTRICT_OFFICER \| GOVERNMENT_ANALYST \| MOH_ADMIN` |
| `first_name` | VARCHAR(64) | NOT NULL | |
| `last_name` | VARCHAR(64) | NOT NULL | |
| `preferred_language` | VARCHAR(8) | DEFAULT 'rw' | `rw` = Kinyarwanda, `en` = English, `fr` = French |
| `active` | BOOLEAN | DEFAULT true | Soft-disable — no hard deletes on users |
| `created_at` | TIMESTAMP | DEFAULT now() | |
| `last_login` | TIMESTAMP | | Used for inactivity detection |

**Key indexes:** Unique on `national_id`, unique on `phone_number`, non-unique on `role` and `facility_id`.

#### `government_users`

A 1:1 extension of `users` for the three government roles. The `scoped_geo_ids` PostgreSQL array is the core of geographic RBAC.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `user_id` | UUID | FK → users, UNIQUE, NOT NULL | 1:1 link to base user record |
| `gov_role` | VARCHAR(32) | NOT NULL | `DISTRICT_OFFICER \| GOVERNMENT_ANALYST \| MOH_ADMIN` |
| `ministry` | VARCHAR(128) | NOT NULL | Employing ministry or agency |
| `employee_id` | VARCHAR(64) | UNIQUE, NOT NULL | Government employee number |
| `scoped_geo_ids` | UUID[] | | PostgreSQL array of authorized `geo_location` UUIDs |
| `can_export` | BOOLEAN | DEFAULT false | Permission to export CSV/Excel reports |
| `can_push_hmis` | BOOLEAN | DEFAULT false | Permission to push data to MoH HMIS |
| `last_audit` | TIMESTAMP | | Timestamp of last access audit review |
| `created_at` | TIMESTAMP | DEFAULT now() | |

> **How `scoped_geo_ids` works:** A DISTRICT_OFFICER's array contains the sector UUIDs they are authorized to access. Spring `@PreAuthorize` checks: `resource.geoLocation.sector IN officer.scopedGeoIds`. MOH_ADMIN and GOVERNMENT_ANALYST have unrestricted access (null-equivalent scope).

---

### Group 3 — Facilities

#### `facilities`

Health centers, hospitals, and CHW posts. `facility_id` is the **multi-tenancy boundary**.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `geo_location_id` | UUID | FK → geo_locations, NOT NULL | Physical location of facility |
| `name` | VARCHAR(128) | NOT NULL | Full facility name |
| `facility_code` | VARCHAR(32) | UNIQUE, NOT NULL | Rwanda MoH official facility code |
| `facility_type` | VARCHAR(32) | NOT NULL | `HEALTH_CENTER \| HOSPITAL \| CLINIC \| CHW_POST` |
| `district` | VARCHAR(64) | NOT NULL | Denormalized for fast filter queries |
| `phone` | VARCHAR(20) | | Facility contact number |
| `active` | BOOLEAN | DEFAULT true | Soft-delete |
| `created_at` | TIMESTAMP | DEFAULT now() | |

> **Why `district` is denormalized:** Facility search by district is a very common query. Storing `district` directly avoids a join to `geo_locations` on every facility list request — the value is stable and updated only if an administrative boundary changes.

---

### Group 4 — Mothers & Pregnancies

#### `mothers`

The primary patient entity. Every mother has exactly one user account (1:1).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `user_id` | UUID | FK → users, UNIQUE, NOT NULL | 1:1 — mother's login account |
| `facility_id` | UUID | FK → facilities, NOT NULL | Registering facility — **tenancy boundary** |
| `geo_location_id` | UUID | FK → geo_locations, NOT NULL | Village — cross-checked against NIDA response |
| `health_id` | VARCHAR(32) | UNIQUE, NOT NULL | Digital health ID e.g. `MH-2026-004821` — generated from `seq_mother_health_id` sequence |
| `nida_verified_status` | VARCHAR(16) | DEFAULT 'PENDING' | `PENDING \| VERIFIED \| FAILED \| MANUAL` |
| `date_of_birth` | DATE | NOT NULL | |
| `education_level` | VARCHAR(32) | | `NONE \| PRIMARY \| SECONDARY \| TERTIARY` |
| `registered_at` | TIMESTAMP | DEFAULT now() | |

> **`health_id` generation:** `MotherService.generateHealthId()` calls `SELECT nextval('seq_mother_health_id')` and formats the result as `MH-{YEAR}-{NNNNNN}`. The sequence `seq_mother_health_id` is created by Flyway migration V12. This ID is the printable, sharable identifier for mothers without smartphones.

#### `pregnancies`

A mother can have multiple pregnancies over time. Never embed pregnancy data in the `mothers` row.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `mother_id` | UUID | FK → mothers, NOT NULL | |
| `lmp_date` | DATE | | Last menstrual period date |
| `edd` | DATE | | Estimated due date — computed as `lmp_date + 280 days` |
| `status` | VARCHAR(16) | DEFAULT 'ACTIVE' | `ACTIVE \| DELIVERED \| LOST \| TRANSFERRED` |
| `gravida` | INT | | Total number of pregnancies (including this one) |
| `para` | INT | | Number of previous live births |
| `assigned_chw_id` | UUID | FK → users | Assigned community health worker |
| `outcome_notes` | TEXT | | Free-text delivery outcome notes |
| `created_at` | TIMESTAMP | DEFAULT now() | |
| `updated_at` | TIMESTAMP | | |

---

### Group 5 — Children & Vaccination

#### `children`

Registered at birth. `birth_certificate_no` is the digital twin of the paper mutuelles card.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `mother_id` | UUID | FK → mothers, NOT NULL | |
| `facility_id` | UUID | FK → facilities, NOT NULL | Birth facility |
| `geo_location_id` | UUID | FK → geo_locations, NOT NULL | |
| `birth_certificate_no` | VARCHAR(64) | UNIQUE | Digital birth certificate number |
| `first_name` | VARCHAR(64) | | |
| `gender` | VARCHAR(8) | | `MALE \| FEMALE \| UNKNOWN` |
| `date_of_birth` | DATE | NOT NULL | |
| `birth_weight_kg` | FLOAT | | |
| `delivery_type` | VARCHAR(16) | NOT NULL | `NORMAL \| CAESAREAN \| ASSISTED` |
| `health_status` | VARCHAR(16) | DEFAULT 'HEALTHY' | `HEALTHY \| AT_RISK \| CRITICAL` — updated by CHW visits |
| `registered_at` | TIMESTAMP | DEFAULT now() | |

#### `vaccination_schedules` (seed / reference table)

Rwanda's EPI schedule — seeded via Flyway, never deleted. Deprecated doses use `is_mandatory=false`.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `vaccine_name` | VARCHAR(64) | NOT NULL | e.g. `Pentavalent Vaccine` |
| `antigen_code` | VARCHAR(16) | UNIQUE, NOT NULL | e.g. `BCG`, `OPV0`, `PENTA1`, `ROTA1`, `MMR` |
| `dose_number` | INT | NOT NULL | Dose sequence within the vaccine series |
| `due_age_days` | INT | NOT NULL | Days from birth when dose is due |
| `window_days` | INT | DEFAULT 7 | Acceptable window before/after due date |
| `is_mandatory` | BOOLEAN | DEFAULT true | `false` = deprecated but kept for history |
| `description` | TEXT | | |
| `updated_at` | TIMESTAMP | | Set when MoH pushes a schedule update |

#### `vaccination_records`

Created automatically when a child is registered — one row per EPI schedule entry.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `child_id` | UUID | FK → children, NOT NULL | |
| `schedule_id` | UUID | FK → vaccination_schedules, NOT NULL | Which vaccine dose |
| `administered_by` | UUID | FK → users | Health worker who gave the dose |
| `facility_id` | UUID | FK → facilities, NOT NULL | |
| `administered_date` | DATE | | NULL until administered |
| `due_date` | DATE | NOT NULL | `date_of_birth + due_age_days` — computed at registration |
| `lot_number` | VARCHAR(32) | | Vaccine batch for pharmacovigilance |
| `status` | VARCHAR(16) | DEFAULT 'PENDING' | `PENDING \| ADMINISTERED \| MISSED \| OVERDUE` |
| `notes` | TEXT | | |
| `created_at` | TIMESTAMP | DEFAULT now() | |

**Critical constraint:** Composite unique index on `(child_id, schedule_id)` — each child receives each dose exactly once.

> **Automation:** A `@Scheduled` cron job runs nightly at 01:00 Rwanda time. It scans `PENDING` records where `due_date + window_days < today` and flips them to `OVERDUE`, enqueuing an SMS reminder per record.

---

### Group 6 — Clinical Visits

#### `health_visits`

Supports both mother visits (ANC, PNC) and child visits from one table via a polymorphic patient reference.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `patient_ref_id` | UUID | NOT NULL | Polymorphic — `mothers.id` or `children.id` |
| `patient_type` | VARCHAR(8) | NOT NULL | `MOTHER \| CHILD` |
| `facility_id` | UUID | FK → facilities, NOT NULL | |
| `health_worker_id` | UUID | FK → users, NOT NULL | |
| `geo_location_id` | UUID | FK → geo_locations | Location of visit — may differ from patient's home |
| `visit_datetime` | TIMESTAMP | NOT NULL | |
| `visit_type` | VARCHAR(16) | NOT NULL | `ANC \| PNC \| IMMUNIZATION \| SICK_CHILD \| GROWTH_MONITORING` |
| `chief_complaint` | TEXT | | |
| `weight_kg` | FLOAT | | |
| `height_cm` | FLOAT | | |
| `systolic_bp` | INT | | |
| `diastolic_bp` | INT | | |
| `muac_cm` | FLOAT | | Mid-upper arm circumference — malnutrition screening |
| `notes` | TEXT | | |
| `created_at` | TIMESTAMP | DEFAULT now() | |

> **Why no strict FK on `patient_ref_id`?** A strict FK would require either two nullable columns or separate tables. The polymorphic pattern (`patient_ref_id` + `patient_type`) avoids nullable FK anti-patterns while serving both mothers and children from a single table. Application-layer validation enforces referential integrity.

#### `diagnoses`

Multiple ICD-10 diagnoses per visit. ICD-10 code subset is synced from MoH HMIS.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `visit_id` | UUID | FK → health_visits, NOT NULL | |
| `icd10_code` | VARCHAR(8) | NOT NULL | ICD-10 code from MoH-approved subset (seeded in V9) |
| `description` | VARCHAR(255) | NOT NULL | Human-readable description |
| `severity` | VARCHAR(16) | | `MILD \| MODERATE \| SEVERE` |
| `is_primary` | BOOLEAN | DEFAULT false | Primary presenting condition flag — used for analytics aggregation |
| `created_at` | TIMESTAMP | DEFAULT now() | |

#### `prescriptions`

Medications issued per visit. `duration_days` enables scheduled medication adherence SMS reminders.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `visit_id` | UUID | FK → health_visits, NOT NULL | |
| `medication_name` | VARCHAR(128) | NOT NULL | |
| `dosage` | VARCHAR(64) | NOT NULL | e.g. `500mg` |
| `frequency` | VARCHAR(64) | NOT NULL | e.g. `Twice daily with food` |
| `duration_days` | INT | NOT NULL | Drives scheduled adherence reminder SMS |
| `instructions` | TEXT | | Additional patient instructions |
| `created_at` | TIMESTAMP | DEFAULT now() | |

---

### Group 7 — Appointments

#### `appointments`

Drives the no-show rate analytics and capacity planning metrics. Uses the same polymorphic patient reference pattern as `health_visits`.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `patient_ref_id` | UUID | NOT NULL | Polymorphic — `mothers.id` or `children.id` |
| `patient_type` | VARCHAR(8) | NOT NULL | `MOTHER \| CHILD` |
| `facility_id` | UUID | FK → facilities, NOT NULL | |
| `health_worker_id` | UUID | FK → users | Assigned health worker |
| `geo_location_id` | UUID | FK → geo_locations | |
| `scheduled_at` | TIMESTAMP | NOT NULL | |
| `appointment_type` | VARCHAR(32) | NOT NULL | `ANC \| PNC \| VACCINATION \| GROWTH_CHECK \| FOLLOW_UP` |
| `status` | VARCHAR(16) | DEFAULT 'SCHEDULED' | `SCHEDULED \| COMPLETED \| NO_SHOW \| CANCELLED` |
| `reminder_sent` | BOOLEAN | DEFAULT false | Set by notification cron 24h before `scheduled_at` |
| `notes` | TEXT | | |
| `cancellation_reason` | TEXT | | Populated on status → CANCELLED |
| `created_at` | TIMESTAMP | DEFAULT now() | |

> **`reminder_sent` flag:** A `@Scheduled` cron runs hourly. It queries appointments where `scheduled_at` is between `now+23h` and `now+25h` and `reminder_sent=false`. After enqueuing the SMS, it sets `reminder_sent=true` — preventing duplicate reminders even if the cron runs twice.

---

### Group 8 — Consent

#### `consent_records`

Required before any mother's data is shared with government systems.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `mother_id` | UUID | FK → mothers, NOT NULL | |
| `consent_type` | VARCHAR(32) | NOT NULL | `GOV_DATA_SHARE \| SMS_REMINDERS \| RESEARCH \| FACILITY_TRANSFER` |
| `granted` | BOOLEAN | NOT NULL | `true` = consent given; `false` = explicitly denied |
| `granted_by_role` | VARCHAR(32) | | Role of user who recorded consent (CHW, PATIENT) |
| `consented_at` | TIMESTAMP | NOT NULL | |
| `expires_at` | TIMESTAMP | | NULL = indefinite; non-null = re-consent required after this date |
| `legal_basis` | VARCHAR(128) | | `Rwanda Law No. 058/2021 on Personal Data Protection` |
| `revoked_at` | TIMESTAMP | | Set when consent is later withdrawn |

> **Consent gate:** `ConsentService.hasActiveConsent(motherId, GOV_DATA_SHARE)` is called before every HMIS push or DISTRICT_OFFICER data query. It checks for a consent record where `granted=true`, `revoked_at IS NULL`, and `(expires_at IS NULL OR expires_at > now())`.

---

### Group 9 — Government Integration

#### `service_requests`

Citizens and CHWs submit formal government service requests. Lifecycle: `PENDING → UNDER_REVIEW → APPROVED → IREMBO_SUBMITTED → COMPLETED`.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `requester_id` | UUID | FK → users, NOT NULL | Mother or CHW who filed the request |
| `facility_id` | UUID | FK → facilities, NOT NULL | |
| `geo_location_id` | UUID | FK → geo_locations, NOT NULL | |
| `service_type` | VARCHAR(32) | NOT NULL | `BIRTH_CERT \| VACCINATION_CARD \| REFERRAL \| HEALTH_SUMMARY \| REPRINT` |
| `status` | VARCHAR(24) | DEFAULT 'PENDING' | `PENDING \| UNDER_REVIEW \| APPROVED \| REJECTED \| IREMBO_SUBMITTED \| COMPLETED` |
| `reference_no` | VARCHAR(32) | UNIQUE, NOT NULL | Human-readable e.g. `SR-2026-00042` |
| `irembo_ticket_id` | VARCHAR(64) | | External Irembo portal reference (set on IREMBO_SUBMITTED) |
| `payload` | JSONB | | Service-specific form data — shape varies by `service_type` |
| `rejection_reason` | TEXT | | Set when status = REJECTED |
| `submitted_at` | TIMESTAMP | NOT NULL | |
| `resolved_at` | TIMESTAMP | | |
| `resolved_by` | UUID | FK → users | FACILITY_ADMIN or DISTRICT_OFFICER who resolved |

**Note:** The requester must be a registered mother with an active `GOV_DATA_SHARE` consent record before the system accepts the request.

#### `service_request_docs`

Supporting documents per service request. `file_hash` (SHA-256) ensures tamper detection.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `request_id` | UUID | FK → service_requests, NOT NULL | |
| `document_type` | VARCHAR(32) | NOT NULL | `ID_COPY \| BIRTH_PROOF \| FACILITY_LETTER \| OTHER` |
| `file_path` | VARCHAR(512) | NOT NULL | Path in file storage bucket |
| `file_hash` | VARCHAR(64) | NOT NULL | SHA-256 hash — recomputed on retrieval to detect tampering |
| `uploaded_at` | TIMESTAMP | DEFAULT now() | |

#### `gov_sync_log` — Outbox Pattern

Every government API call is written here **first**, then executed by a background job.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `facility_id` | UUID | FK → facilities | Originating facility |
| `target_system` | VARCHAR(16) | NOT NULL | `NIDA \| HMIS \| IREMBO \| RURA` |
| `sync_type` | VARCHAR(32) | NOT NULL | `IDENTITY_VERIFY \| REPORT_PUSH \| TICKET_SUBMIT \| SCHEDULE_PULL` |
| `status` | VARCHAR(16) | DEFAULT 'PENDING' | `PENDING \| IN_FLIGHT \| SUCCEEDED \| FAILED \| DEAD_LETTER` |
| `idempotency_key` | VARCHAR(128) | UNIQUE, NOT NULL | Prevents duplicate submissions on retry |
| `payload_hash` | VARCHAR(64) | | SHA-256 of request payload — change detection |
| `retry_count` | INT | DEFAULT 0 | Incremented on each retry |
| `error_message` | TEXT | | Last error from government API |
| `synced_at` | TIMESTAMP | | Timestamp of last attempt |
| `next_retry_at` | TIMESTAMP | | Exponential backoff — `2^retry_count` minutes |
| `created_at` | TIMESTAMP | DEFAULT now() | |

**`DEAD_LETTER` after 5 retries** — triggers an alert SMS to all MOH_ADMIN users. Manual retry available via `POST /api/v1/gov-sync-log/{id}/retry`.

> **Why `idempotency_key` is critical:** If a request times out, it is unknown whether the government API processed it. On retry, the same key is sent — the API returns the cached result instead of processing twice. Key format: `{target_system}:{sync_type}:{resource_id}:{timestamp}`.

#### `gov_reports`

Aggregated statistical reports — contains **no individual patient records**.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `generated_by` | UUID | FK → users, NOT NULL | `MOH_ADMIN` or `GOVERNMENT_ANALYST` |
| `geo_location_id` | UUID | FK → geo_locations, NOT NULL | Geographic scope of the report |
| `report_type` | VARCHAR(32) | NOT NULL | `VACCINATION_COVERAGE \| ANC_ATTENDANCE \| BIRTH_REGISTRATION \| MATERNAL_HEALTH` |
| `period` | VARCHAR(16) | NOT NULL | e.g. `2026-Q1`, `2026-04`, `2026` |
| `scope_level` | VARCHAR(16) | NOT NULL | `NATIONAL \| PROVINCE \| DISTRICT \| SECTOR` |
| `aggregates` | JSONB | NOT NULL | Computed statistics — `vaccination_coverage_pct`, `anc_visits_total`, etc. |
| `hmis_push_status` | VARCHAR(16) | DEFAULT 'NOT_PUSHED' | `NOT_PUSHED \| QUEUED \| PUSHED \| FAILED` |
| `generated_at` | TIMESTAMP | DEFAULT now() | |
| `pushed_at` | TIMESTAMP | | Set when HMIS push succeeds |

---

### Group 10 — Notifications & Audit

#### `sms_notifications`

Outbound SMS via Africa's Talking API. A cron runs every 5 minutes processing `QUEUED` rows.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `recipient_user_id` | UUID | FK → users, NOT NULL | |
| `phone_number` | VARCHAR(20) | NOT NULL | Denormalized at send time — number may change later |
| `message_body` | VARCHAR(320) | NOT NULL | Max 2 SMS segments (160 chars each) |
| `notification_type` | VARCHAR(32) | NOT NULL | `VACCINATION_REMINDER \| APPOINTMENT \| HEALTH_TIP \| SERVICE_STATUS \| EMERGENCY` |
| `status` | VARCHAR(16) | DEFAULT 'QUEUED' | `QUEUED \| SENT \| DELIVERED \| FAILED` |
| `at_message_id` | VARCHAR(64) | | Africa's Talking reference — enables delivery webhook tracking |
| `scheduled_at` | TIMESTAMP | NOT NULL | When to send |
| `sent_at` | TIMESTAMP | | Actual send time |
| `retry_count` | INT | DEFAULT 0 | Max 3 retries |
| `created_at` | TIMESTAMP | DEFAULT now() | |

> **Delivery loop:** After sending, Africa's Talking calls `POST /webhooks/at/delivery` with the `at_message_id`. The webhook handler updates `status` to `DELIVERED` or `FAILED`. The endpoint validates an HMAC-SHA256 signature to prevent spoofing.

#### `audit_log`

PHI access audit log. **Immutable** — no UPDATE or DELETE operations permitted.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK | |
| `user_id` | UUID | FK → users, NOT NULL | Who performed the action |
| `action` | VARCHAR(32) | NOT NULL | `READ \| CREATE \| UPDATE \| DELETE \| EXPORT \| LOGIN \| LOGOUT` |
| `resource_type` | VARCHAR(32) | NOT NULL | Table name e.g. `mothers`, `children`, `gov_reports` |
| `resource_id` | UUID | | ID of the accessed record |
| `geo_location_id` | UUID | FK → geo_locations | Location context of the action |
| `ip_address` | VARCHAR(45) | | IPv4 or IPv6 |
| `user_agent` | VARCHAR(255) | | Browser/client identifier |
| `success` | BOOLEAN | DEFAULT true | `false` = failed attempt — important for security monitoring |
| `fail_reason` | VARCHAR(128) | | Reason for failure |
| `created_at` | TIMESTAMP | NOT NULL | Immutable timestamp |

**Partitioned by month in production** — V3 migration creates the parent table and 2026 monthly partitions. 7-year retention per MoH policy.

---

## 4. Key Relationships

```
geo_locations ←──────── users ──────────────── government_users (1:1)
      │                  │
      │              mothers (1:1 via user_id)
      │                  │
      ├──── facilities   ├──── pregnancies (1:many)
      │         │         │
      │         └──── vaccination_records ←── vaccination_schedules
      │                  │
      │             children (1:many)
      │                  │
      │     health_visits (polymorphic ↑ mothers OR children)
      │             │
      │        ├── diagnoses (1:many)
      │        └── prescriptions (1:many)
      │
      └──── appointments (polymorphic ↑ mothers OR children)
                   │
          consent_records ←── service_requests ──→ gov_sync_log
                                     │
                          service_request_docs
                                          
gov_reports ──→ gov_sync_log
sms_notifications ─→ users
audit_log ─────────→ users
```

| Relationship | Cardinality | Note |
|-------------|-------------|------|
| `users` → `government_users` | 1:1 | Government user IS a user; 1:1 join gives geo scope without polluting base table |
| `users` → `mothers` | 1:1 | Mother has exactly one login account |
| `mothers` → `pregnancies` | 1:many | Full obstetric history preserved — never embed pregnancy in mother row |
| `children` → `vaccination_records` | 1:many | Auto-created at birth — one row per EPI schedule entry |
| `health_visits` → `diagnoses` | 1:many | Multiple ICD-10 diagnoses per visit; `is_primary` marks presenting condition |
| `service_requests` → `gov_sync_log` | indirect | Approval creates a sync log entry — no direct FK (intentional) |

---

## 5. Enum Reference

All enums are stored as `VARCHAR` in the database (`@Enumerated(EnumType.STRING)` in JPA). This keeps DB entries human-readable without a separate lookup table.

| Enum | Values |
|------|--------|
| **UserRole** | `PATIENT` · `HEALTH_WORKER` · `FACILITY_ADMIN` · `DISTRICT_OFFICER` · `GOVERNMENT_ANALYST` · `MOH_ADMIN` |
| **FacilityType** | `HEALTH_CENTER` · `HOSPITAL` · `CLINIC` · `CHW_POST` |
| **NidaVerifiedStatus** | `PENDING` · `VERIFIED` · `FAILED` · `MANUAL` |
| **PregnancyStatus** | `ACTIVE` · `DELIVERED` · `LOST` · `TRANSFERRED` |
| **Gender** | `MALE` · `FEMALE` · `UNKNOWN` |
| **DeliveryType** | `NORMAL` · `CAESAREAN` · `ASSISTED` |
| **ChildHealthStatus** | `HEALTHY` · `AT_RISK` · `CRITICAL` |
| **VaccinationStatus** | `PENDING` · `ADMINISTERED` · `MISSED` · `OVERDUE` |
| **VisitType** | `ANC` · `PNC` · `IMMUNIZATION` · `SICK_CHILD` · `GROWTH_MONITORING` |
| **DiagnosisSeverity** | `MILD` · `MODERATE` · `SEVERE` |
| **AppointmentType** | `ANC` · `PNC` · `VACCINATION` · `GROWTH_CHECK` · `FOLLOW_UP` |
| **AppointmentStatus** | `SCHEDULED` · `COMPLETED` · `NO_SHOW` · `CANCELLED` |
| **ConsentType** | `GOV_DATA_SHARE` · `SMS_REMINDERS` · `RESEARCH` · `FACILITY_TRANSFER` |
| **ServiceType** | `BIRTH_CERT` · `VACCINATION_CARD` · `REFERRAL` · `HEALTH_SUMMARY` · `REPRINT` |
| **ServiceRequestStatus** | `PENDING` · `UNDER_REVIEW` · `APPROVED` · `REJECTED` · `IREMBO_SUBMITTED` · `COMPLETED` |
| **DocumentType** | `ID_COPY` · `BIRTH_PROOF` · `FACILITY_LETTER` · `OTHER` |
| **GovTargetSystem** | `NIDA` · `HMIS` · `IREMBO` · `RURA` |
| **GovSyncType** | `IDENTITY_VERIFY` · `REPORT_PUSH` · `TICKET_SUBMIT` · `SCHEDULE_PULL` |
| **GovSyncStatus** | `PENDING` · `IN_FLIGHT` · `SUCCEEDED` · `FAILED` · `DEAD_LETTER` |
| **ReportType** | `VACCINATION_COVERAGE` · `ANC_ATTENDANCE` · `BIRTH_REGISTRATION` · `MATERNAL_HEALTH` |
| **ScopeLevel** | `NATIONAL` · `PROVINCE` · `DISTRICT` · `SECTOR` |
| **HmisPushStatus** | `NOT_PUSHED` · `QUEUED` · `PUSHED` · `FAILED` |
| **NotificationType** | `VACCINATION_REMINDER` · `APPOINTMENT` · `HEALTH_TIP` · `SERVICE_STATUS` · `EMERGENCY` |
| **SmsStatus** | `QUEUED` · `SENT` · `DELIVERED` · `FAILED` |
| **AuditAction** | `READ` · `CREATE` · `UPDATE` · `DELETE` · `EXPORT` · `LOGIN` · `LOGOUT` |
| **EducationLevel** | `NONE` · `PRIMARY` · `SECONDARY` · `TERTIARY` |
| **Language** | `rw` (Kinyarwanda) · `en` (English) · `fr` (French) |

---

## 6. Index Strategy

Indexes are defined in two places: JPA entity `@Table(indexes = {...})` annotations and Flyway migration scripts. The strategy follows these rules:

| Rule | Rationale |
|------|-----------|
| Every FK column has an index | Avoid sequential scans on join columns |
| Every `status` column has an index | Status filters are the most common WHERE clause |
| Composite unique index on `(child_id, schedule_id)` in `vaccination_records` | Enforce one-dose-per-child-per-schedule at the database level |
| Composite index on `(province, district, sector)` in `geo_locations` | Fast cascade dropdown queries |
| Index on `next_retry_at` in `gov_sync_log` | Background job polling for PENDING rows ready for retry |
| Index on `scheduled_at` in `appointments` and `sms_notifications` | Cron job range queries |
| Unique indexes on `national_id` and `phone_number` in `users` | Prevent duplicate registration at DB level |

---

## 7. Key Data Flows

### 7.1 Mother Registration

```
Client → POST /api/v1/mothers
         │
         ├── Validate facilityId scope (caller.facilityId == request.facilityId)
         ├── motherRepository.existsByUserId() — duplicate check
         ├── entityManager.getReference() — lazy proxy for User, Facility, GeoLocation
         ├── Generate healthId via SELECT nextval('seq_mother_health_id')
         ├── motherRepository.save(mother)  [nida_verified_status = PENDING]
         └── nidaVerificationService.verify(motherId, nationalId)  [async]
                     │
                     └── gov_sync_log INSERT  [status=PENDING, target=NIDA]
                                 │
                                 └── GovSyncService @Scheduled
                                             │
                                             ├── NIDA API call
                                             ├── Update mother.nida_verified_status = VERIFIED
                                             └── gov_sync_log UPDATE  [status=SUCCEEDED]
```

### 7.2 Child Registration & Vaccination Schedule

```
Client → POST /api/v1/children
         │
         ├── childRepository.save(child)
         └── For each row in vaccination_schedules WHERE is_mandatory=true:
                 vaccination_records INSERT {
                     child_id = child.id,
                     schedule_id = schedule.id,
                     due_date = child.dateOfBirth + schedule.due_age_days,
                     status = 'PENDING'
                 }

Nightly cron (01:00 Rwanda time):
         ├── SELECT * FROM vaccination_records
         │   WHERE status='PENDING' AND due_date + window_days < today
         ├── UPDATE status = 'OVERDUE'
         └── For each OVERDUE: sms_notifications INSERT [VACCINATION_REMINDER]
```

### 7.3 Government Sync (Outbox Pattern)

```
Any service that calls a government API:
         │
         ├── gov_sync_log INSERT [status=PENDING, idempotency_key=unique]
         └── Return immediately (no blocking HTTP call)

GovSyncService @Scheduled (every 2 min):
         ├── SELECT * FROM gov_sync_log
         │   WHERE status='PENDING' AND next_retry_at <= now()
         ├── UPDATE status = 'IN_FLIGHT'
         ├── Call external API (NIDA / HMIS / Irembo)
         │
         ├── On success:
         │       UPDATE status = 'SUCCEEDED', synced_at = now()
         │
         └── On failure:
                 ├── UPDATE retry_count++, status='FAILED'
                 ├── next_retry_at = now() + (2^retry_count minutes)
                 └── If retry_count >= 5:
                         UPDATE status = 'DEAD_LETTER'
                         sms_notifications INSERT [EMERGENCY → all MOH_ADMIN users]
```

### 7.4 SMS Outbox

```
SmsService.enqueue(userId, phoneNumber, message, type, scheduledAt):
         └── sms_notifications INSERT [status=QUEUED]

SMS Cron @Scheduled (every 5 min):
         ├── SELECT * FROM sms_notifications
         │   WHERE status='QUEUED' AND scheduled_at <= now()
         ├── Africa's Talking API call
         ├── UPDATE status='SENT', at_message_id=..., sent_at=now()
         └── On failure: retry_count++ (max 3), reschedule

Delivery Webhook POST /webhooks/at/delivery:
         ├── Validate HMAC-SHA256 signature
         ├── Lookup by at_message_id
         └── UPDATE status = 'DELIVERED' or 'FAILED'
```

---

## 8. RBAC Permission Matrix

| Resource | PATIENT | HEALTH_WORKER | FACILITY_ADMIN | DISTRICT_OFFICER | GOV_ANALYST | MOH_ADMIN |
|----------|:-------:|:-------------:|:--------------:|:----------------:|:-----------:|:---------:|
| Own profile / children | Full | Read | Read | None | None | None |
| Patient records (mothers/children) | Own only | Facility scope | Facility full | Sector scope | None | National |
| Vaccination records | Read own | Facility full | Facility full | None | None | National |
| Service requests (file) | Own only | Submit + track | Approve/reject | Sector scope | None | Full |
| Geo-scoped patient lists | None | Sector scope | Facility scope | Sector scope | District agg | National |
| Analytics & reports | None | Facility read | Facility full | Sector scope | District scope | National |
| HMIS push / export | None | None | None | None | Read only | Push + export |
| Gov sync log | None | None | Read own | Sector scope | Read all | Full |
| Consent records | Own only | Read assigned | Facility read | None | None | National read |
| User management | None | None | Facility only | None | None | Full |
| Admin dashboard | None | None | None | None | None | Full |
| Audit log | None | None | None | None | Read all | Full |

**Enforcement:** Two layers — URL-level `requestMatchers` in `SecurityConfig` (coarse) + `@PreAuthorize` on service methods (fine-grained). The `scoped_geo_ids` UUID array on `government_users` enforces the DISTRICT_OFFICER's sector boundary.

---

## 9. Security & Compliance

### Data Protection

| Requirement | Implementation |
|-------------|---------------|
| **PHI never in logs** | Structured logging — patient IDs only, never names or medical data |
| **Password storage** | bcrypt with cost factor ≥ 12 |
| **Transport security** | All government API calls use OAuth2 + mTLS |
| **Audit immutability** | `audit_log` has no UPDATE/DELETE permissions; partitioned append-only |
| **Consent gate** | `ConsentService.hasActiveConsent()` checked before every government data share |
| **Idempotency** | All gov sync operations use unique idempotency keys — prevents duplicate submissions |
| **SQL injection** | Spring Data JPA parameterized queries only — no native SQL with string interpolation |
| **CORS** | Whitelist-only: Next.js origin + Irembo callback URL |
| **JWT secrets** | 90-day rotation policy via Kubernetes/Railway secret management |

### Rwanda Law No. 058/2021 Compliance

- `consent_records.legal_basis` documents the legal basis per consent record
- `consent_records.expires_at` enforces re-consent requirements
- `audit_log` provides the 7-year immutable access history required by MoH policy
- `gov_reports.aggregates` contains **only computed statistics** — no individual patient data reaches government systems without consent

---

## 10. Flyway Migration Map

Never modify an applied migration — add a new numbered script instead.

| Migration | What it does |
|-----------|-------------|
| `V1__Initial_Schema.sql` | Creates all core tables: geo_locations, users, government_users, facilities, mothers, pregnancies, children, vaccination_schedules, vaccination_records, health_visits, diagnoses, prescriptions, appointments, consent_records, service_requests, service_request_docs, gov_sync_log, gov_reports, sms_notifications, audit_log |
| `V2__Schema_Fixes.sql` | Schema corrections from initial review |
| `V3__Audit_Log_Partitioning.sql` | Converts `audit_log` to a partitioned table; creates 2026 monthly partitions |
| `V4__Constraints_And_Partitions.sql` | Additional constraints and partition adjustments |
| `V5__Appointments_And_Notifications.sql` | Refinements to appointments and sms_notifications tables |
| `V6__SR_Sequence.sql` | Creates `SR-YYYY-NNNNN` reference number sequence for service_requests |
| `V7__Performance_Indexes.sql` | Adds missing performance indexes across all tables |
| `V8__HealthVisit_Constraints.sql` | Adds NOT NULL constraints to health_visits required fields |
| `V9__Seed_Icd10_Hmis_Subset.sql` | Seeds ICD-10 code subset approved by MoH HMIS |
| `V10__add_cancellation_reason_to_appointments.sql` | `ALTER TABLE appointments ADD COLUMN cancellation_reason TEXT` |
| `V11__GovSyncLog_Missing_Columns.sql` | Adds missing columns to gov_sync_log (`next_retry_at`, etc.) |
| `V12__add_mother_health_id_sequence.sql` | `CREATE SEQUENCE seq_mother_health_id` — required by `MotherService.generateHealthId()` |

> **Naming convention:** `V{number}__{description}.sql`. The number must increase monotonically. Flyway will refuse to start if a previously-applied script is modified (checksum validation).

---

## 11. DBML Schema

Paste the block below at [dbdiagram.io](https://dbdiagram.io) to render the interactive entity relationship diagram. Use **Export → Export to PostgreSQL** to generate full `CREATE TABLE` SQL with indexes and constraints.

```dbml
// ============================================================
// MotherHood Journey — Database Schema
// IgireRwanda Organization | SheCanCode Bootcamp | Kigali, Rwanda
// Paste this entire block at https://dbdiagram.io
// ============================================================

// ─────────────────────────────────────────────
// GROUP 1: GEO-IDENTITY & ADMINISTRATIVE
// ─────────────────────────────────────────────

Table geo_locations {
  id              uuid        [pk, note: 'Primary key']
  province        varchar(64) [not null, note: 'One of 5 provinces']
  district        varchar(64) [not null, note: 'One of 30 districts']
  sector          varchar(64) [not null, note: 'One of 416 sectors — RBAC scope boundary']
  cell            varchar(64) [not null, note: 'One of 2,148 cells — CHW assignment unit']
  village         varchar(64) [not null, note: '~14,000 villages — finest granularity']
  postal_code     varchar(16)
  latitude        float
  longitude       float
  active          boolean     [default: true]
  created_at      timestamp   [default: `now()`]

  indexes {
    (province, district, sector) [name: 'idx_geo_pds']
    (sector) [name: 'idx_geo_sector']
  }

  note: 'Seeded once via Flyway migration with full Rwanda admin hierarchy. Every entity table references this via geo_location_id FK. Never duplicated — always referenced.'
}

// ─────────────────────────────────────────────
// GROUP 2: USERS & ROLES
// ─────────────────────────────────────────────

Table users {
  id                  uuid        [pk]
  facility_id         uuid        [ref: > facilities.id, note: 'Home facility']
  geo_location_id     uuid        [ref: > geo_locations.id, not null, note: 'Village-level location']
  national_id         varchar(32) [unique, not null, note: 'Rwanda NID — verified by NIDA']
  phone_number        varchar(20) [unique, not null]
  password_hash       varchar(255) [not null]
  role                varchar(32) [not null, note: 'PATIENT | HEALTH_WORKER | FACILITY_ADMIN | DISTRICT_OFFICER | GOVERNMENT_ANALYST | MOH_ADMIN']
  first_name          varchar(64) [not null]
  last_name           varchar(64) [not null]
  preferred_language  varchar(8)  [default: 'rw', note: 'rw=Kinyarwanda, en=English, fr=French']
  active              boolean     [default: true]
  created_at          timestamp   [default: `now()`]
  last_login          timestamp

  indexes {
    national_id [name: 'idx_users_nid', unique]
    phone_number [name: 'idx_users_phone', unique]
    role [name: 'idx_users_role']
    facility_id [name: 'idx_users_facility']
  }

  note: 'All six roles share this single table. Government roles additionally have a government_users row (1:1). Enables unified JWT auth pipeline.'
}

Table government_users {
  id              uuid        [pk]
  user_id         uuid        [ref: - users.id, unique, not null, note: '1:1 extension of users table']
  gov_role        varchar(32) [not null, note: 'DISTRICT_OFFICER | GOVERNMENT_ANALYST | MOH_ADMIN']
  ministry        varchar(128) [not null, note: 'Employing ministry or agency']
  employee_id     varchar(64) [unique, not null, note: 'Government employee identifier']
  scoped_geo_ids  uuid[]      [note: 'PostgreSQL array — authorized sector/district geo_location UUIDs for DISTRICT_OFFICER']
  can_export      boolean     [default: false, note: 'Permission to export CSV/reports']
  can_push_hmis   boolean     [default: false, note: 'Permission to push data to MoH HMIS']
  last_audit      timestamp   [note: 'Last access-audit review timestamp']
  created_at      timestamp   [default: `now()`]

  note: '1:1 extension of users. Government staff log in via the same JWT pipeline. scoped_geo_ids drives sector-level RBAC.'
}

// ─────────────────────────────────────────────
// GROUP 3: FACILITIES
// ─────────────────────────────────────────────

Table facilities {
  id              uuid        [pk]
  geo_location_id uuid        [ref: > geo_locations.id, not null]
  name            varchar(128) [not null]
  facility_code   varchar(32) [unique, not null, note: 'Rwanda MoH facility code']
  facility_type   varchar(32) [not null, note: 'HEALTH_CENTER | HOSPITAL | CLINIC | CHW_POST']
  district        varchar(64) [not null, note: 'Denormalized for fast filter queries']
  phone           varchar(20)
  active          boolean     [default: true]
  created_at      timestamp   [default: `now()`]

  indexes {
    facility_code [unique, name: 'idx_facility_code']
    geo_location_id [name: 'idx_facility_geo']
  }

  note: 'Every patient row, visit row, and service request carries a facility_id. Primary multi-tenancy boundary.'
}

// ─────────────────────────────────────────────
// GROUP 4: MOTHERS & PREGNANCIES
// ─────────────────────────────────────────────

Table mothers {
  id                    uuid        [pk]
  user_id               uuid        [ref: - users.id, unique, not null, note: '1:1 — mother has a user account']
  facility_id           uuid        [ref: > facilities.id, not null, note: 'Registering facility']
  geo_location_id       uuid        [ref: > geo_locations.id, not null, note: 'Village-level — cross-checked against NIDA']
  health_id             varchar(32) [unique, not null, note: 'MH-YYYY-NNNNNN from seq_mother_health_id']
  nida_verified_status  varchar(16) [default: 'PENDING', note: 'PENDING | VERIFIED | FAILED | MANUAL']
  date_of_birth         date        [not null]
  education_level       varchar(32) [note: 'NONE | PRIMARY | SECONDARY | TERTIARY']
  registered_at         timestamp   [default: `now()`]

  indexes {
    health_id [unique, name: 'idx_mother_health_id']
    facility_id [name: 'idx_mother_facility']
    geo_location_id [name: 'idx_mother_geo']
    nida_verified_status [name: 'idx_mother_nida_status']
  }

  note: 'Core patient entity. NIDA cross-check sets geo_location_id automatically. health_id is the printable identifier for mothers without smartphones.'
}

Table pregnancies {
  id                uuid        [pk]
  mother_id         uuid        [ref: > mothers.id, not null]
  lmp_date          date        [note: 'Last menstrual period date']
  edd               date        [note: 'Estimated due date = lmp_date + 280 days']
  status            varchar(16) [not null, default: 'ACTIVE', note: 'ACTIVE | DELIVERED | LOST | TRANSFERRED']
  gravida           int         [note: 'Total number of pregnancies']
  para              int         [note: 'Number of live births']
  assigned_chw_id   uuid        [ref: > users.id, note: 'Assigned community health worker']
  outcome_notes     text
  created_at        timestamp   [default: `now()`]
  updated_at        timestamp

  indexes {
    mother_id [name: 'idx_pregnancy_mother']
    assigned_chw_id [name: 'idx_pregnancy_chw']
    status [name: 'idx_pregnancy_status']
  }

  note: 'Separate entity — a mother can have multiple pregnancies. Never embed pregnancy data in mothers row. Enables full obstetric history.'
}

// ─────────────────────────────────────────────
// GROUP 5: CHILDREN & VACCINATION
// ─────────────────────────────────────────────

Table children {
  id                    uuid        [pk]
  mother_id             uuid        [ref: > mothers.id, not null]
  facility_id           uuid        [ref: > facilities.id, not null, note: 'Birth facility']
  geo_location_id       uuid        [ref: > geo_locations.id, not null]
  birth_certificate_no  varchar(64) [unique, note: 'Digital birth cert number — issued at birth']
  first_name            varchar(64)
  gender                varchar(8)  [note: 'MALE | FEMALE | UNKNOWN']
  date_of_birth         date        [not null]
  birth_weight_kg       float
  delivery_type         varchar(16) [not null, note: 'NORMAL | CAESAREAN | ASSISTED']
  health_status         varchar(16) [default: 'HEALTHY', note: 'HEALTHY | AT_RISK | CRITICAL']
  registered_at         timestamp   [default: `now()`]

  indexes {
    mother_id [name: 'idx_child_mother']
    facility_id [name: 'idx_child_facility']
    birth_certificate_no [unique, name: 'idx_child_birth_cert']
    health_status [name: 'idx_child_status']
  }

  note: 'Registered at birth. birth_certificate_no is the digital twin of the paper mutuelles card. health_status updated by CHW visits.'
}

Table vaccination_schedules {
  id              uuid        [pk]
  vaccine_name    varchar(64) [not null]
  antigen_code    varchar(16) [unique, not null, note: 'e.g. BCG, OPV0, PENTA1, ROTA1, MMR']
  dose_number     int         [not null]
  due_age_days    int         [not null, note: 'Days from birth when dose is due']
  window_days     int         [default: 7, note: 'Acceptable window before/after due date']
  is_mandatory    boolean     [default: true]
  description     text
  updated_at      timestamp

  indexes {
    antigen_code [unique, name: 'idx_vacc_sched_code']
  }

  note: 'Seeded via Flyway with full Rwanda EPI schedule. Never deleted — deprecated doses use is_mandatory=false.'
}

Table vaccination_records {
  id                  uuid        [pk]
  child_id            uuid        [ref: > children.id, not null]
  schedule_id         uuid        [ref: > vaccination_schedules.id, not null]
  administered_by     uuid        [ref: > users.id, note: 'Health worker who gave the dose']
  facility_id         uuid        [ref: > facilities.id, not null]
  administered_date   date
  due_date            date        [not null, note: 'Computed: date_of_birth + due_age_days']
  lot_number          varchar(32) [note: 'Vaccine batch for pharmacovigilance']
  status              varchar(16) [not null, default: 'PENDING', note: 'PENDING | ADMINISTERED | MISSED | OVERDUE']
  notes               text
  created_at          timestamp   [default: `now()`]

  indexes {
    child_id [name: 'idx_vacc_rec_child']
    status [name: 'idx_vacc_rec_status']
    due_date [name: 'idx_vacc_rec_due']
    (child_id, schedule_id) [unique, name: 'idx_vacc_rec_child_sched']
  }

  note: 'Created automatically at child registration — one row per EPI schedule entry. Nightly cron flips PENDING past-due records to OVERDUE and triggers SMS reminders.'
}

// ─────────────────────────────────────────────
// GROUP 6: CLINICAL VISITS
// ─────────────────────────────────────────────

Table health_visits {
  id                  uuid        [pk]
  patient_ref_id      uuid        [not null, note: 'Polymorphic — points to mothers.id or children.id']
  patient_type        varchar(8)  [not null, note: 'MOTHER | CHILD']
  facility_id         uuid        [ref: > facilities.id, not null]
  health_worker_id    uuid        [ref: > users.id, not null]
  geo_location_id     uuid        [ref: > geo_locations.id, note: 'Location of visit — may differ from home']
  visit_datetime      timestamp   [not null]
  visit_type          varchar(16) [not null, note: 'ANC | PNC | IMMUNIZATION | SICK_CHILD | GROWTH_MONITORING']
  chief_complaint     text
  weight_kg           float
  height_cm           float
  systolic_bp         int
  diastolic_bp        int
  muac_cm             float       [note: 'Mid-upper arm circumference — malnutrition screening']
  notes               text
  created_at          timestamp   [default: `now()`]

  indexes {
    patient_ref_id [name: 'idx_visit_patient']
    facility_id [name: 'idx_visit_facility']
    visit_datetime [name: 'idx_visit_datetime']
    health_worker_id [name: 'idx_visit_worker']
    (patient_ref_id, patient_type) [name: 'idx_visit_patient_poly']
  }

  note: 'Polymorphic patient reference supports both mother and child visits from one table.'
}

Table diagnoses {
  id          uuid        [pk]
  visit_id    uuid        [ref: > health_visits.id, not null]
  icd10_code  varchar(8)  [not null, note: 'ICD-10 code — subset seeded from MoH HMIS']
  description varchar(255) [not null]
  severity    varchar(16) [note: 'MILD | MODERATE | SEVERE']
  is_primary  boolean     [default: false, note: 'Primary diagnosis flag for analytics']
  created_at  timestamp   [default: `now()`]

  indexes {
    visit_id [name: 'idx_diag_visit']
    icd10_code [name: 'idx_diag_icd10']
  }

  note: 'Multiple diagnoses per visit. is_primary marks the presenting condition for analytics aggregation.'
}

Table prescriptions {
  id              uuid        [pk]
  visit_id        uuid        [ref: > health_visits.id, not null]
  medication_name varchar(128) [not null]
  dosage          varchar(64) [not null]
  frequency       varchar(64) [not null]
  duration_days   int         [not null, note: 'Drives scheduled adherence SMS reminders']
  instructions    text
  created_at      timestamp   [default: `now()`]

  indexes {
    visit_id [name: 'idx_rx_visit']
  }

  note: 'Medications per visit. duration_days enables scheduling adherence reminder SMS via sms_notifications.'
}

// ─────────────────────────────────────────────
// GROUP 7: APPOINTMENTS
// ─────────────────────────────────────────────

Table appointments {
  id                    uuid        [pk]
  patient_ref_id        uuid        [not null, note: 'Polymorphic — mothers.id or children.id']
  patient_type          varchar(8)  [not null, note: 'MOTHER | CHILD']
  facility_id           uuid        [ref: > facilities.id, not null]
  health_worker_id      uuid        [ref: > users.id]
  geo_location_id       uuid        [ref: > geo_locations.id]
  scheduled_at          timestamp   [not null]
  appointment_type      varchar(32) [not null, note: 'ANC | PNC | VACCINATION | GROWTH_CHECK | FOLLOW_UP']
  status                varchar(16) [not null, default: 'SCHEDULED', note: 'SCHEDULED | COMPLETED | NO_SHOW | CANCELLED']
  reminder_sent         boolean     [default: false, note: 'Set by notification cron 24h before scheduled_at']
  notes                 text
  cancellation_reason   text
  created_at            timestamp   [default: `now()`]

  indexes {
    patient_ref_id [name: 'idx_appt_patient']
    facility_id [name: 'idx_appt_facility']
    scheduled_at [name: 'idx_appt_datetime']
    status [name: 'idx_appt_status']
  }

  note: 'Drives the no-show rate analytics and capacity planning. reminder_sent prevents duplicate SMS reminders.'
}

// ─────────────────────────────────────────────
// GROUP 8: CONSENT
// ─────────────────────────────────────────────

Table consent_records {
  id                uuid        [pk]
  mother_id         uuid        [ref: > mothers.id, not null]
  consent_type      varchar(32) [not null, note: 'GOV_DATA_SHARE | SMS_REMINDERS | RESEARCH | FACILITY_TRANSFER']
  granted           boolean     [not null, note: 'true=consent given, false=explicitly denied']
  granted_by_role   varchar(32) [note: 'Role of user who recorded consent']
  consented_at      timestamp   [not null, default: `now()`]
  expires_at        timestamp   [note: 'NULL=indefinite; else re-consent required']
  legal_basis       varchar(128) [note: 'Rwanda Law No. 058/2021 on Personal Data Protection']
  revoked_at        timestamp

  indexes {
    mother_id [name: 'idx_consent_mother']
    (mother_id, consent_type) [name: 'idx_consent_type']
    expires_at [name: 'idx_consent_expiry']
  }

  note: 'REQUIRED before any mother data is shared with government systems. GOV_DATA_SHARE consent checked before every HMIS push or DISTRICT_OFFICER data query.'
}

// ─────────────────────────────────────────────
// GROUP 9: GOVERNMENT INTEGRATION
// ─────────────────────────────────────────────

Table service_requests {
  id                uuid        [pk]
  requester_id      uuid        [ref: > users.id, not null, note: 'Mother or CHW who filed']
  facility_id       uuid        [ref: > facilities.id, not null]
  geo_location_id   uuid        [ref: > geo_locations.id, not null]
  service_type      varchar(32) [not null, note: 'BIRTH_CERT | VACCINATION_CARD | REFERRAL | HEALTH_SUMMARY | REPRINT']
  status            varchar(24) [not null, default: 'PENDING', note: 'PENDING | UNDER_REVIEW | APPROVED | REJECTED | IREMBO_SUBMITTED | COMPLETED']
  reference_no      varchar(32) [unique, not null, note: 'Human-readable ref e.g. SR-2026-00042']
  irembo_ticket_id  varchar(64) [note: 'External Irembo portal reference number']
  payload           jsonb       [note: 'Service-specific form data — shape varies by service_type']
  rejection_reason  text
  submitted_at      timestamp   [not null, default: `now()`]
  resolved_at       timestamp
  resolved_by       uuid        [ref: > users.id, note: 'FACILITY_ADMIN or DISTRICT_OFFICER']

  indexes {
    reference_no [unique, name: 'idx_sr_ref']
    requester_id [name: 'idx_sr_requester']
    facility_id [name: 'idx_sr_facility']
    status [name: 'idx_sr_status']
    geo_location_id [name: 'idx_sr_geo']
    irembo_ticket_id [name: 'idx_sr_irembo']
  }

  note: 'Citizen and CHW government service requests. Routed to FACILITY_ADMIN, optionally escalated to DISTRICT_OFFICER, then pushed async to Irembo via gov_sync_log outbox.'
}

Table service_request_docs {
  id              uuid        [pk]
  request_id      uuid        [ref: > service_requests.id, not null]
  document_type   varchar(32) [not null, note: 'ID_COPY | BIRTH_PROOF | FACILITY_LETTER | OTHER']
  file_path       varchar(512) [not null, note: 'Path in file storage bucket']
  file_hash       varchar(64) [not null, note: 'SHA-256 hash for tamper detection']
  uploaded_at     timestamp   [default: `now()`]

  indexes {
    request_id [name: 'idx_srd_request']
  }

  note: 'SHA-256 file_hash ensures document integrity — any modification to the stored file is detectable.'
}

Table gov_sync_log {
  id                uuid        [pk]
  facility_id       uuid        [ref: > facilities.id]
  target_system     varchar(16) [not null, note: 'NIDA | HMIS | IREMBO | RURA']
  sync_type         varchar(32) [not null, note: 'IDENTITY_VERIFY | REPORT_PUSH | TICKET_SUBMIT | SCHEDULE_PULL']
  status            varchar(16) [not null, default: 'PENDING', note: 'PENDING | IN_FLIGHT | SUCCEEDED | FAILED | DEAD_LETTER']
  idempotency_key   varchar(128) [unique, not null, note: 'Prevents duplicate submissions on retry']
  payload_hash      varchar(64) [note: 'SHA-256 of request payload']
  retry_count       int         [default: 0]
  error_message     text
  synced_at         timestamp
  next_retry_at     timestamp   [note: 'Exponential backoff: 2^retry_count minutes']
  created_at        timestamp   [default: `now()`]

  indexes {
    idempotency_key [unique, name: 'idx_gsync_idempotency']
    status [name: 'idx_gsync_status']
    target_system [name: 'idx_gsync_target']
    next_retry_at [name: 'idx_gsync_retry']
  }

  note: 'Outbox pattern. Every government API call is written here FIRST. DEAD_LETTER after 5 retries triggers admin alert.'
}

Table gov_reports {
  id                uuid        [pk]
  generated_by      uuid        [ref: > users.id, not null, note: 'MOH_ADMIN or GOVERNMENT_ANALYST']
  geo_location_id   uuid        [ref: > geo_locations.id, not null, note: 'Geographic scope of report']
  report_type       varchar(32) [not null, note: 'VACCINATION_COVERAGE | ANC_ATTENDANCE | BIRTH_REGISTRATION | MATERNAL_HEALTH']
  period            varchar(16) [not null, note: 'e.g. 2026-Q1 | 2026-04 | 2026']
  scope_level       varchar(16) [not null, note: 'NATIONAL | PROVINCE | DISTRICT | SECTOR']
  aggregates        jsonb       [not null, note: 'Computed statistics only — no individual patient records']
  hmis_push_status  varchar(16) [default: 'NOT_PUSHED', note: 'NOT_PUSHED | QUEUED | PUSHED | FAILED']
  generated_at      timestamp   [default: `now()`]
  pushed_at         timestamp

  indexes {
    generated_by [name: 'idx_greport_user']
    geo_location_id [name: 'idx_greport_geo']
    (report_type, period, scope_level) [name: 'idx_greport_type_period']
    hmis_push_status [name: 'idx_greport_hmis']
  }

  note: 'Aggregated statistics only — zero individual patient records. Pushed to MoH HMIS via gov_sync_log outbox.'
}

// ─────────────────────────────────────────────
// GROUP 10: NOTIFICATIONS & AUDIT
// ─────────────────────────────────────────────

Table sms_notifications {
  id                  uuid        [pk]
  recipient_user_id   uuid        [ref: > users.id, not null]
  phone_number        varchar(20) [not null, note: 'Denormalized — number at time of send']
  message_body        varchar(320) [not null, note: 'Max 2 SMS segments (160 chars each)']
  notification_type   varchar(32) [not null, note: 'VACCINATION_REMINDER | APPOINTMENT | HEALTH_TIP | SERVICE_STATUS | EMERGENCY']
  status              varchar(16) [not null, default: 'QUEUED', note: 'QUEUED | SENT | DELIVERED | FAILED']
  at_message_id       varchar(64) [note: "Africa's Talking message ID for delivery webhook tracking"]
  scheduled_at        timestamp   [not null]
  sent_at             timestamp
  retry_count         int         [default: 0, note: 'Max 3 retries']
  created_at          timestamp   [default: `now()`]

  indexes {
    recipient_user_id [name: 'idx_sms_user']
    status [name: 'idx_sms_status']
    scheduled_at [name: 'idx_sms_scheduled']
    notification_type [name: 'idx_sms_type']
  }

  note: "Outbound SMS via Africa's Talking API. at_message_id enables webhook-based delivery status updates."
}

Table audit_log {
  id              uuid        [pk]
  user_id         uuid        [ref: > users.id, not null]
  action          varchar(32) [not null, note: 'READ | CREATE | UPDATE | DELETE | EXPORT | LOGIN | LOGOUT']
  resource_type   varchar(32) [not null, note: 'Table name e.g. mothers, children, gov_reports']
  resource_id     uuid        [note: 'ID of the accessed record']
  geo_location_id uuid        [ref: > geo_locations.id, note: 'Location context of action']
  ip_address      varchar(45) [note: 'IPv4 or IPv6']
  user_agent      varchar(255)
  success         boolean     [default: true, note: 'false = failed attempt — security monitoring']
  fail_reason     varchar(128)
  created_at      timestamp   [not null, default: `now()`]

  indexes {
    user_id [name: 'idx_audit_user']
    resource_type [name: 'idx_audit_resource']
    (resource_type, resource_id) [name: 'idx_audit_resource_id']
    created_at [name: 'idx_audit_ts']
  }

  note: 'PHI access audit log. Immutable — no UPDATE or DELETE permitted. 7-year retention per MoH policy. Partitioned by month in production.'
}
```

---

*IgireRwanda Organization · SheCanCode Bootcamp · Kacyiru, KG 549 St, 36 · Gasabo, Kigali, Rwanda*