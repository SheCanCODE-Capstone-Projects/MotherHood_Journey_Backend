# Consent Module

## What this module does

This module manages and enforces user permissions for handling sensitive
data within the MotherHood Journey system. It ensures that no data is
shared or processed without valid and legally compliant consent from the
mother.

It is the security gate of the entire system — before any sensitive data
is shared with external government systems like HMIS, NIDA, or Irembo,
this module is called first to verify that valid consent exists.

## Legal basis

This module is built to comply with Rwanda Law No. 058/2021 on Personal
Data Protection. Every consent action is permanently recorded and cannot
be deleted — only revoked. This ensures full accountability and audit
compliance.

## The four consent types

| Type | What it means |
|------|---------------|
| `GOV_DATA_SHARE` | Mother allows her data to be sent to government systems like HMIS |
| `SMS_REMINDERS` | Mother allows the system to send her SMS vaccination reminders |
| `RESEARCH` | Mother allows her data to be used for health research purposes |
| `FACILITY_TRANSFER` | Mother allows her records to be transferred to another facility |

## How hasActiveConsent works

This is the most important method in the entire module. It is called by
other services before sharing any sensitive data.

## hasActiveConsent(motherId, GOV_DATA_SHARE)

Step 1: Find all consent records for this mother
where consent_type = GOV_DATA_SHARE

Step 2: Check that at least one record passes all four conditions:
Condition 1 — granted = true (mother said YES)
Condition 2 — revoked_at is null (never revoked)
Condition 3 — expires_at is null OR in the future (not expired)

Step 3: If all conditions pass → return true → data can flow
If any condition fails → return false → data is blocked

## Important rules

Rule 1 — Consent records are NEVER deleted from the database.
When a mother revokes consent, the system sets revoked_at to the current
time. The record stays in the database forever as an audit trail.

Rule 2 — This module is a permanent audit log.
Every consent action — granting, denying, revoking — is stored with
full timestamps for compliance with Rwanda Law No. 058/2021.

Rule 3 — hasActiveConsent is a gate for other services.
Any service that shares data with external systems must call this method
first. If it returns false, the operation is blocked immediately.

## API Endpoints

| Method | Endpoint | Description | Auth required |
|--------|----------|-------------|---------------|
| POST | `/api/v1/consent` | Record a new consent | Yes |
| DELETE | `/api/v1/consent/{id}` | Revoke an existing consent | Yes |
| GET | `/api/v1/consent/mother/{motherId}` | Get full consent history for a mother | Yes |

## Example usage

### Record a new consent

POST /api/v1/consent

Request body:
{
"motherId": "550e8400-e29b-41d4-a716-446655440000",
"consentType": "GOV_DATA_SHARE",
"granted": true,
"grantedByRole": "HEALTH_WORKER",
"legalBasis": "Rwanda Law No. 058/2021 on Personal Data Protection",
"expiresAt": null
}

Response 201 Created:



"id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
"motherId": "550e8400-e29b-41d4-a716-446655440000",
"consentType": "GOV_DATA_SHARE",
"granted": true,
"grantedByRole": "HEALTH_WORKER",
"legalBasis": "Rwanda Law No. 058/2021 on Personal Data Protection",
"consentedAt": "2026-04-27T10:30:00",
"expiresAt": null,
"revokedAt": null,
"active": true

}

### Revoke a consent

DELETE /api/v1/consent/7c9e6679-7425-40de-944b-e07fc1f90ae7

Response 200 OK:
{
"id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
"motherId": "550e8400-e29b-41d4-a716-446655440000",
"consentType": "GOV_DATA_SHARE",
"granted": true,
"grantedByRole": "HEALTH_WORKER",
"legalBasis": "Rwanda Law No. 058/2021 on Personal Data Protection",
"consentedAt": "2026-04-27T10:30:00",
"expiresAt": null,
"revokedAt": "2026-04-27T14:00:00",
"active": false
}

### Get all consents for a mother
GET /api/v1/consent/mother/550e8400-e29b-41d4-a716-446655440000
Response 200 OK:
[
{
"id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
"consentType": "GOV_DATA_SHARE",
"granted": true,
"active": false,
"revokedAt": "2026-04-27T14:00:00"

},
{
"id": "8d0f7780-8536-51ef-a55c-f18gd2g01bf8",
"consentType": "SMS_REMINDERS",
"granted": true,
"active": true,
"revokedAt": null

}
]

### How other services use the gate

// Inside GovernmentSyncService — before pushing data to HMIS
if (!consentService.hasActiveConsent(motherId, ConsentType.GOV_DATA_SHARE)) {

throw new RuntimeException(
"Cannot share data — mother has not given GOV_DATA_SHARE consent"
);
}
//only reaches here if consent is valid and active 
sendDataTo HMIS(motherId);

## Module structure
modules/consent/
└── src/main/java/com/motherhood/consent/
|
├── domain/
│   ├── model/
│   │   ├── ConsentRecord.java        — database entity
│   │   └── ConsentType.java          — enum of four consent types
│   │   └── ConsentType.java          — enum of four consent types
│   ├── repository/
│   │   └── ConsentRepository.java    — repository contract
│   └── service/
│       └── ConsentDomainService.java — pure business rules
|
├── application/
│   ├── dto/
│   │   ├── ConsentRequest.java       — incoming request data
│   │   └── ConsentResponse.java      — outgoing response data
│   └── service/
│       └── ConsentService.java       — main logic and gate
|
├── infrastructure/
│   ├── mapper/
│   │   └── ConsentMapper.java        — MapStruct DTO converter
│   └── persistence/
│       ├── JpaConsentRepository.java — repository implementation
│       └── SpringDataConsentRepository.java — Spring Data JPA
|
└── presentation/
└── ConsentController.java    — REST endpoints

## How the layers connect

HTTP Request
↓
ConsentController (presentation)
↓ calls
ConsentService (application)
↓ calls
ConsentRepository (domain interface)
↓ implemented by
JpaConsentRepository (infrastructure)
↓ uses
SpringDataConsentRepository (Spring Data JPA)
↓ queries
PostgreSQL — consent_records table
ConsentService also calls
↓
ConsentDomainService (domain)
— checks the four active consent conditions
— never touches the database directly

## Lifecycle of a consent record
Mother visits health center
↓
Health worker explains data usage
↓
Mother says YES or NO
↓
POST /api/v1/consent is called
↓
ConsentRecord saved with:
granted = true or false
revoked_at = null
consented_at = now()
↓
Later — system tries to share data with government
↓
hasActiveConsent(motherId, GOV_DATA_SHARE) is called
↓
Returns true → data flows
Returns false → data is blocked
↓
Mother later decides to withdraw consent
DELETE /api/v1/consent/{id} is called
Record is NOT deleted
revoked_at = now() is set
↓
hasActiveConsent now returns false
Data is no longer shared


## Developer notes

- ConsentRecord uses PostgreSQL native uuid type for the id column
- Records are immutable once created — only revoked_at can be updated
- The active field in ConsentResponse is computed — not stored in database
- expiresAt = null means the consent never expires
- revokedAt = null means the consent has not been revoked
- The hasActiveConsent method must be called before every government
  data push, HMIS sync, and NIDA verification
- ConsentMapper uses MapStruct with active field ignored during mapping
  because it is computed manually in the service layer

## Technologies used

- Java 21
- Spring Boot 3.2.5
- Spring Data JPA
- PostgreSQL (native UUID type)
- MapStruct (DTO conversion)
- Lombok (boilerplate reduction)
- Flyway (database migrations)