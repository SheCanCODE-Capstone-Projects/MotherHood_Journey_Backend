# MotherHood Journey Database Documentation

## Overview
This database schema is designed for the **MotherHood Journey** platform by IgireRwanda Organization. It provides a robust foundation for tracking maternal health, child development, vaccinations, and government service requests in Rwanda.

## Core Design Principles
- **Geographic Granularity**: All entities are linked to the Rwanda administrative hierarchy (Province -> District -> Sector -> Cell -> Village).
- **Multi-tenancy**: Facilities serve as the primary boundary for data access (RBAC).
- **Polymorphism**: Health visits and appointments use polymorphic references to support both mothers and children.
- **Auditability**: Every action involving PHI (Protected Health Information) is logged in an immutable audit table.
- **Government Integration**: Outbox pattern (`gov_sync_log`) ensures reliable communication with NIDA, HMIS, and Irembo.

## Table Groups

### 1. Geo-Identity & Administrative
- `geo_locations`: The master list of all administrative units in Rwanda. Every other table references this for location context.

### 2. Users & Roles
- `users`: Unified table for all platform users (Patients, Health Workers, Admins, etc.).
- `government_users`: Extension table for government officials with specialized RBAC scopes.

### 3. Facilities
- `facilities`: Health centers, hospitals, and clinics. Critical for multi-tenant data isolation.

### 4. Mothers & Pregnancies
- `mothers`: Core patient data, linked to a user account and verified via NIDA.
- `pregnancies`: Tracks obstetric history. A mother can have multiple pregnancy records over time.

### 5. Children & Vaccination
- `children`: Registered at birth, linked to their mother.
- `vaccination_schedules`: The master EPI (Expanded Program on Immunization) schedule for Rwanda.
- `vaccination_records`: Tracking administered and upcoming doses for each child.

### 6. Clinical Visits
- `health_visits`: Clinical encounter data for both mothers and children.
- `diagnoses`: Linked to visits using ICD-10 coding.
- `prescriptions`: Medication history per visit.

### 7. Appointments
- `appointments`: Scheduler for follow-ups, ANC/PNC visits, and vaccinations.

### 8. Consent
- `consent_records`: Legal tracking of data sharing permissions as per Rwanda Law No. 058/2021.

### 9. Government Integration
- `service_requests`: Citizen requests for birth certificates, referrals, etc.
- `service_request_docs`: Uploaded supporting documents.
- `gov_sync_log`: The outbox for all external API sync tasks.
- `gov_reports`: Aggregated, non-identifiable statistics for MoH reporting.

### 10. Notifications & Audit
- `sms_notifications`: Outbound SMS queue (integrated with Africa's Talking).
- `audit_log`: Immutable log of all system actions for security compliance.

## Key Enumerations

| Field | Enum Class | Values |
|-------|------------|--------|
| `users.role` | `UserRole` | PATIENT, HEALTH_WORKER, FACILITY_ADMIN, ... |
| `pregnancies.status` | `PregnancyStatus` | ACTIVE, DELIVERED, LOST, TRANSFERRED |
| `children.gender` | `Gender` | MALE, FEMALE, UNKNOWN |
| `health_visits.visit_type` | `VisitType` | ANC, PNC, IMMUNIZATION, ... |
| `appointments.status` | `AppointmentStatus` | SCHEDULED, COMPLETED, NO_SHOW, CANCELLED |
| `gov_sync_log.status` | `SyncStatus` | PENDING, IN_FLIGHT, SUCCEEDED, FAILED, DEAD_LETTER |

## Setup & Migration
1. Ensure a PostgreSQL database is available.
2. The schema is managed via Flyway.
3. Place the `V1__Initial_Schema.sql` in `src/main/resources/db/migration/`.
4. Run `./mvnw spring-boot:run` to apply the migration.

## Compliance
This design adheres to **Rwanda Law No. 058/2021 on Personal Data Protection and Privacy**. The `audit_log` and `consent_records` tables are mandatory for legal compliance.
