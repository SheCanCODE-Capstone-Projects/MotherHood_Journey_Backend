# MotherHood Journey — API Reference

**Base URL:** `http://localhost:8080/api/v1`  
**Exception:** Webhook endpoints at `/webhooks/at` (no `/api/v1` prefix)  
**Auth:** All endpoints except `auth/*`, `geo/*`, and `/webhooks/at/*` require a JWT Bearer token in `Authorization: Bearer <token>`.

---

## Response Envelope

Every response (except 204 No Content, file uploads, and webhooks) is wrapped:

```json
{
  "success": true,
  "message": "Human-readable status",
  "data": { ... }
}
```

```**HTTP Status codes used:**
| Code | Meaning |
|------|---------|
| 200 | Successful GET / PATCH / PUT |
| 201 | Resource created (POST) |
| 202 | Async operation accepted |
| 204 | No content (DELETE, revoke, logout) |
| 400 | Validation error |
| 401 | Missing/invalid token or HMAC signature |
| 403 | Insufficient role |
| 404 | Resource not found |
| 415 | Unsupported file type |
| 500 | Unexpected server error (correlation ID in body) |

---

## 1. Authentication (`/auth`)

Public endpoints — no token required.

### POST `/auth/register`
Register a new user.

**Request body:**
```json
{
  "phoneNumber": "+250788XXXXXX",
  "nationalId": "1199XXXXXXXXXX",
  "password": "Min8chars",
  "firstName": "Jane",
  "lastName": "Doe",
  "role": "HEALTH_WORKER",
  "geoLocationId": "uuid",
  "facilityId": "uuid"
}
```
`role` enum: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`, `GOVERNMENT_ANALYST`, `PATIENT`  
`facilityId` required for `HEALTH_WORKER` / `FACILITY_ADMIN`.

**Response 201:** UserResponse (see §17)

---

### POST `/auth/login`
Authenticate and receive tokens.

**Request body:**
```json
{ "phoneNumber": "+250788XXXXXX", "password": "yourPassword" }
```

**Response 200:**
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "role": "HEALTH_WORKER"
}
```

Lockout: 5 consecutive failures lock the account for 15 minutes.

---

### POST `/auth/refresh`
Rotate tokens using a refresh token.

**Request body:**
```json
{ "refreshToken": "eyJ..." }
```

**Response 200:** Same as login (new token pair). Reusing a rotated token revokes the entire token family.

---

### POST `/auth/logout`
Revoke a refresh token.

**Request body:**
```json
{ "refreshToken": "eyJ..." }
```

**Response 204**

---

## 2. Admin Dashboard (`/admin`)

Required role: `MOH_ADMIN` (all endpoints).

### GET `/admin/dashboard`
Platform-wide counters.

**Response 200:**
```json
{
  "totalFacilities": 12,
  "totalMothers": 1400,
  "totalChildren": 980,
  "totalUsers": 85,
  "pendingAppointments": 23,
  "pendingVaccinations": 57,
  "activePregnancies": 210
}
```

---

### GET `/admin/users?page=0&size=20`
Paginated list of all users.

**Response 200:** Page of UserResponse

---

### PATCH `/admin/users/{id}/deactivate`
Deactivate a user account.

**Response 200:** UserResponse

---

### PATCH `/admin/users/{id}/activate`
Activate a user account.

**Response 200:** UserResponse

---

## 3. Geographic Data (`/geo`)

Public — no token required for cascade hierarchy lists. Token required for `/resolve` and `/{id}/summary`.

### GET `/geo/provinces`
List all provinces.

**Response 200:** `["Kigali City", "Northern", "Southern", "Eastern", "Western"]`

---

### GET `/geo/districts?province={name}`
List districts in a province.

**Response 200:** `["Gasabo", "Kicukiro", "Nyarugenge"]`

---

### GET `/geo/sectors?province={name}&district={name}`
**Response 200:** Array of sector names

---

### GET `/geo/cells?province={name}&district={name}&sector={name}`
**Response 200:** Array of cell names

---

### GET `/geo/villages?province={name}&district={name}&sector={name}&cell={name}`
**Response 200:** Array of village names

---

### GET `/geo/resolve?province={}&district={}&sector={}&cell={}&village={}`
Resolve a full address path to a location UUID. All five parameters required.

**Response 200:**
```json
{
  "id": "uuid",
  "province": "Kigali City",
  "district": "Gasabo",
  "sector": "Bumbogo",
  "cell": "Kinyaga",
  "village": "Akakaza",
  "active": true
}
```

---

### GET `/geo/{id}/summary`
Lightweight summary of a location by UUID.

**Response 200:**
```json
{ "id": "uuid", "sector": "Bumbogo", "cell": "Kinyaga", "village": "Akakaza" }
```

---

## 4. Facilities (`/facilities`)

### POST `/facilities`
Required role: `MOH_ADMIN`. Create a facility.

**Request body:**
```json
{
  "name": "Gasabo District Hospital",
  "facilityCode": "KG-GAS-001",
  "facilityType": "HEALTH_CENTER",
  "district": "Gasabo",
  "phone": "+250788000000",
  "geoLocationId": "uuid"
}
```

**Response 201:** FacilityResponse

---

### GET `/facilities?district={}&facilityType={}&page=0&size=20`
Required role: Authenticated. List active facilities (pagination supported).

**Response 200:** Page of FacilityResponse

---

### GET `/facilities/{id}`
Required role: Authenticated.

**Response 200:** FacilityResponse

---

### PUT `/facilities/{id}`
Required role: `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:**
```json
{
  "name": "Updated HC",
  "facilityType": "HEALTH_CENTER",
  "district": "Gasabo",
  "phone": "+250788000001",
  "active": true
}
```

**Response 200:** FacilityResponse

---

### DELETE `/facilities/{id}`
Required role: `MOH_ADMIN`.

**Response 204**

---

### GET `/facilities/{id}/analytics`
Required role: `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:**
```json
{
  "facilityId": "uuid",
  "activeMothers": 340,
  "ancVisitsLast30Days": 48,
  "ancAttendanceRate": 72.5,
  "vaccinationsDue": 120,
  "vaccinationsAdministered": 98,
  "vaccinationCoverageRate": 44.95,
  "appointmentsScheduled": 55,
  "appointmentsNoShow": 7,
  "noShowRate": 12.73,
  "serviceRequestsPending": 3,
  "serviceRequestsOverdue": 1
}
```

---

## 5. Current User (`/me`)

Required role: Authenticated.

### GET `/me`
Return the authenticated user's own profile.

**Response 200:** UserResponse

---

## 6. Users (`/users`)

### GET `/users/{id}`
Required role: `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** UserResponse

---

### PATCH `/users/{id}`
Required role: `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:**
```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "preferredLanguage": "rw",
  "active": true
}
```

**Response 200:** UserResponse

---

## 7. Mothers (`/mothers`)

### POST `/mothers`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`.

**Request body:**
```json
{
  "userId": "uuid",
  "facilityId": "uuid",
  "nationalId": "1199XXXXXXXXXX",
  "geoLocationId": "uuid",
  "dateOfBirth": "1990-05-15",
  "educationLevel": "SECONDARY"
}
```

Generates health ID in format `MH-YYYY-NNNNNN`. Triggers async NIDA verification.

**Response 201:** MotherResponse
```json
{
  "id": "uuid",
  "userId": "uuid",
  "healthId": "MH-2026-000001",
  "nidaVerifiedStatus": "PENDING",
  "dateOfBirth": "1990-05-15",
  "educationLevel": "SECONDARY",
  "facilityId": "uuid",
  "facilityName": "Test HC",
  "geoLocationId": "uuid",
  "sector": "Bumbogo",
  "cell": "Kinyaga",
  "village": "Akakaza",
  "registeredAt": "2026-05-15T10:00:00"
}
```

---

### GET `/mothers/health/{healthId}`
Public. Resolve mother by health ID.

**Response 200:** MotherResponse

---

### GET `/mothers/{id}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `DISTRICT_OFFICER`, `MOH_ADMIN`, `GOVERNMENT_ANALYST`.

Scope enforcement:
- `HEALTH_WORKER`/`FACILITY_ADMIN`: must match mother's facility
- `DISTRICT_OFFICER`: must match mother's geo scope

**Response 200:** MotherResponse

---

### GET `/mothers/pending-nida`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`.

**Response 200:** Array of MotherSummaryResponse

---

## 8. Pregnancies (`/pregnancies`)

### POST `/pregnancies`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:**
```json
{
  "motherId": "uuid",
  "lmpDate": "2026-01-15",
  "edd": "2026-10-22",
  "gravida": 2,
  "para": 1,
  "assignedChwId": "uuid"
}
```

Computes EDD from LMP if `edd` not supplied. Rejects if an ACTIVE pregnancy exists.

**Response 201:** PregnancyResponse

---

### GET `/pregnancies/{id}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** PregnancyResponse

---

### GET `/pregnancies/by-mother/{motherId}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** Array of PregnancyResponse

---

### PATCH `/pregnancies/{id}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:**
```json
{
  "status": "DELIVERED",
  "outcomeNotes": "Normal delivery",
  "lmpDate": "2026-01-15",
  "edd": "2026-10-22",
  "gravida": 2,
  "para": 2,
  "assignedChwId": "uuid"
}
```

`status` enum: `ACTIVE`, `DELIVERED`, `LOST`, `TRANSFERRED`. Terminal states cannot be reopened.

**Response 200:** PregnancyResponse

---

## 9. Health Visits (`/health-visits`)

### POST `/health-visits`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:**
```json
{
  "patientRefId": "uuid",
  "patientType": "MOTHER",
  "facilityId": "uuid",
  "healthWorkerId": "uuid",
  "geoLocationId": "uuid",
  "visitDatetime": "2026-05-15T09:30:00",
  "visitType": "ANC",
  "chiefComplaint": "Regular check-up",
  "weightKg": 65.5,
  "heightCm": 160.0,
  "systolicBp": 120,
  "diastolicBp": 80,
  "muacCm": 28.5,
  "notes": "Patient stable",
  "diagnoses": [
    {
      "icd10Code": "Z34.0",
      "description": "Normal first pregnancy",
      "severity": "LOW",
      "isPrimary": true
    }
  ],
  "prescriptions": [
    {
      "medicationName": "Ferrous Sulfate",
      "dosage": "200mg",
      "frequency": "Once daily",
      "durationDays": 30,
      "instructions": "Take with food"
    }
  ]
}
```

Vitals ranges: weight 0.5–300 kg, height 20–250 cm, systolic 40–300 mmHg, diastolic 20–200 mmHg, MUAC 5–50 cm.

**Response 201:** HealthVisitResponse

---

### GET `/health-visits/{id}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** HealthVisitResponse

---

### GET `/health-visits/by-facility/{facilityId}?page=0&size=20`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** Page of HealthVisitResponse

---

### GET `/health-visits/by-patient?patientRefId={uuid}&patientType=MOTHER&facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** Page of HealthVisitResponse

---

### PATCH `/health-visits/{id}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:** Any subset of visit fields (all optional in update).

**Response 200:** HealthVisitResponse

---

## 10. Children (`/children`)

### POST `/children`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:**
```json
{
  "motherId": "uuid",
  "facilityId": "uuid",
  "geoLocationId": "uuid",
  "birthCertificateNo": "BC-2026-001",
  "firstName": "Amina",
  "gender": "FEMALE",
  "dateOfBirth": "2026-05-01",
  "birthWeightKg": 3.2,
  "deliveryType": "NORMAL"
}
```

`gender` enum: `MALE`, `FEMALE`  
`deliveryType` enum: `NORMAL`, `CAESAREAN`, `ASSISTED`  
`dateOfBirth` must be past or present.

Auto-generates EPI immunization schedule records.

**Response 201:** ChildResponse (includes `vaccinationRecordsCreated` count)

---

### GET `/children/{id}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** ChildResponse

---

### GET `/children/by-mother/{motherId}?facilityId={uuid}&page=0&size=20`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** Page of ChildResponse

---

### GET `/children/by-facility/{facilityId}?page=0&size=20`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** Page of ChildResponse

---

### PATCH `/children/{id}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:**
```json
{
  "firstName": "Amina",
  "birthCertificateNo": "BC-2026-001",
  "healthStatus": "HEALTHY"
}
```

**Response 200:** ChildResponse

---

## 11. Vaccinations (`/vaccinations`)

### GET `/vaccinations/by-child/{childId}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** Array of VaccinationRecordResponse

---

### PATCH `/vaccinations/{id}/administer?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:**
```json
{
  "administeredById": "uuid",
  "administeredDate": "2026-05-15",
  "lotNumber": "LOT-2026-001",
  "notes": "No adverse reactions"
}
```

Age-validated against EPI schedule. Audit-trailed.

**Response 200:** VaccinationRecordResponse

---

## 12. Appointments (`/appointments`)

### POST `/appointments`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:**
```json
{
  "patientRefId": "uuid",
  "patientType": "MOTHER",
  "facilityId": "uuid",
  "healthWorkerId": "uuid",
  "geoLocationId": "uuid",
  "scheduledAt": "2026-07-10T10:00:00",
  "appointmentType": "ANC",
  "notes": "Third ANC visit"
}
```

`scheduledAt` must be in the future. Validates no duplicate booking for same patient in same time slot.

**Response 201:** AppointmentResponse

---

### GET `/appointments/{id}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `DISTRICT_OFFICER`, `MOH_ADMIN`.

**Response 200:** AppointmentResponse

---

### GET `/appointments/patient/{patientRefId}?patientType=MOTHER&facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `DISTRICT_OFFICER`, `MOH_ADMIN`.

**Response 200:** Array of AppointmentResponse

---

### PUT `/appointments/{id}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:**
```json
{
  "facilityId": "uuid",
  "healthWorkerId": "uuid",
  "scheduledAt": "2026-07-15T11:00:00",
  "appointmentType": "ANC",
  "status": "SCHEDULED",
  "notes": "Rescheduled"
}
```

**Response 200:** AppointmentResponse

---

## 13. Consents (`/consents`)

Consent types: `DATA_SHARING`, `TREATMENT`, `VACCINATION`, `GOV_DATA_SHARE`  
`GOV_DATA_SHARE` consent is required before a PATIENT can submit a service request.

### POST `/consents`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Request body:**
```json
{
  "motherId": "uuid",
  "consentType": "DATA_SHARING",
  "granted": true,
  "grantedByRole": "HEALTH_WORKER",
  "expiresAt": "2027-05-15T00:00:00",
  "legalBasis": "Informed consent obtained verbally"
}
```

**Response 201:** ConsentResponse

---

### GET `/consents/{id}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Response 200:** ConsentResponse

---

### GET `/consents/by-mother/{motherId}?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Response 200:** Array of ConsentResponse

---

### PATCH `/consents/{id}/revoke?facilityId={uuid}`
Required role: `HEALTH_WORKER`, `FACILITY_ADMIN`, `MOH_ADMIN`.

**Response 204**

---

## 14. Service Requests (`/service-requests`)

**Prerequisite:** Patient user must have an active `GOV_DATA_SHARE` consent before POST.

`serviceType` enum: `BIRTH_CERT`, `VACCINATION_CARD`, `REFERRAL`, `HEALTH_SUMMARY`, `REPRINT`  
`status` enum: `PENDING`, `APPROVED`, `REJECTED`

### POST `/service-requests`
Required role: Authenticated.

**Request body:**
```json
{
  "serviceType": "BIRTH_CERT",
  "facilityId": "uuid",
  "geoLocationId": "uuid",
  "payload": { "childName": "Amina Doe" }
}
```

Generates sequential reference number (format `SR-YYYY-NNNNN`).

**Response 201:** ServiceRequestResponse
```json
{
  "id": "uuid",
  "referenceNo": "SR-2026-00001",
  "serviceType": "BIRTH_CERT",
  "status": "PENDING",
  "facilityId": "uuid",
  "requesterId": "uuid",
  "rejectionReason": null,
  "submittedAt": "2026-05-28T07:25:49",
  "resolvedAt": null
}
```

---

### GET `/service-requests/{id}`
Required role: `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** ServiceRequestResponse

---

### GET `/service-requests/by-facility/{facilityId}?page=0&size=20`
Required role: `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

Note: `facilityId` is a **path variable**, not a query parameter.

**Response 200:** Page of ServiceRequestResponse

---

### GET `/service-requests/by-status?status=PENDING&page=0&size=20`
Required role: `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** Page of ServiceRequestResponse

---

### PATCH `/service-requests/{id}/approve`
Required role: `FACILITY_ADMIN`, `MOH_ADMIN`.

**Response 200:** ServiceRequestResponse

---

### PATCH `/service-requests/{id}/reject?reason={text}`
Required role: `FACILITY_ADMIN`, `MOH_ADMIN`.

`reason` is a **query parameter** (not request body).

**Response 200:** ServiceRequestResponse

---

## 15. Service Request Documents (`/service-requests/{requestId}/documents`)

**Multipart upload.** Magic-byte MIME detection. Max 10 MB. SHA-256 hash stored.  
Accepted types: `application/pdf`, `image/jpeg`, `image/png`.

`documentType` enum: `ID_COPY`, `BIRTH_PROOF`, `FACILITY_LETTER`, `OTHER`

### POST `/service-requests/{requestId}/documents`
Required role: Authenticated.

**Request:** `multipart/form-data`
- `documentType` (required): enum value
- `file` (required): binary file part

**Response 201:**
```json
{
  "id": "uuid",
  "requestId": "uuid",
  "documentType": "BIRTH_PROOF",
  "filePath": "service-requests/uuid/randomname.pdf",
  "fileHash": "sha256hex",
  "uploadedAt": "2026-05-28T07:31:24"
}
```

---

### GET `/service-requests/{requestId}/documents`
Required role: `FACILITY_ADMIN`, `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** Array of ServiceRequestDocResponse

---

## 16. Government Users (`/government`)

### POST `/government`
Required role: `MOH_ADMIN`.

**Request body:**
```json
{
  "userId": "uuid",
  "govRole": "ANALYST",
  "ministry": "MINISANTE",
  "employeeId": "EMP-001"
}
```

**Response 201:** GovernmentResponse

---

### GET `/government`
Required role: `MOH_ADMIN`.

**Response 200:** Array of GovernmentResponse

---

### GET `/government/{id}`
Required role: `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** GovernmentResponse

---

### GET `/government/by-user/{userId}`
Required role: `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** GovernmentResponse

---

### PATCH `/government/{id}/scope`
Required role: `MOH_ADMIN`.

**Request body:**
```json
{
  "scopedGeoIds": ["uuid1", "uuid2"],
  "canExport": true,
  "canPushHmis": false
}
```

**Response 200:** GovernmentResponse

---

### DELETE `/government/{id}`
Required role: `MOH_ADMIN`.

**Response 204**

---

## 17. Government Reports (`/gov-reports`)

`reportType` enum: `VACCINATION_COVERAGE`, `ANC_ATTENDANCE`, `BIRTH_REGISTRATION`, `MATERNAL_HEALTH`  
`scopeLevel` enum: `NATIONAL`, `PROVINCE`, `DISTRICT`, `SECTOR`  
`period` format: `YYYY-MM` (calendar month) or any string (last 30 days fallback)

### POST `/gov-reports`
Required role: `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Request body:**
```json
{
  "reportType": "ANC_ATTENDANCE",
  "period": "2026-04",
  "geoLocationId": "uuid",
  "scopeLevel": "DISTRICT"
}
```

Server computes aggregates from live data; any client-supplied `aggregates` are ignored. Queues DHIS2 push via HMIS outbox.

**Response 201:** GovReportResponse
```json
{
  "id": "uuid",
  "generatedById": "uuid",
  "geoLocationId": "uuid",
  "reportType": "ANC_ATTENDANCE",
  "period": "2026-04",
  "scopeLevel": "DISTRICT",
  "aggregates": { "anc_visits": 12, "active_pregnancies": 5, "attendance_pct": 80.0 },
  "hmisPushStatus": "QUEUED",
  "generatedAt": "2026-05-28T08:03:56",
  "pushedAt": null
}
```

---

### GET `/gov-reports/{id}`
Required role: `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** GovReportResponse

---

### GET `/gov-reports/by-user/{userId}?page=0&size=20`
Required role: `MOH_ADMIN`, `DISTRICT_OFFICER`.

**Response 200:** Page of GovReportResponse

---

## 18. Gov-Sync Admin (`/admin/gov-sync`)

Required role: `MOH_ADMIN`, `FACILITY_ADMIN` (all endpoints).

`status` enum: `PENDING`, `IN_FLIGHT`, `SUCCEEDED`, `DEAD_LETTER`  
`targetSystem` enum: `IREMBO`, `NIDA`, `HMIS`

### GET `/admin/gov-sync/status`
Outbox sync counters.

**Response 200:**
```json
{ "pending": 2, "in_flight": 0, "succeeded": 45, "dead_letter": 1 }
```

---

### GET `/admin/gov-sync?status={}&targetSystem={}&page=0&size=20`
Filterable log of sync entries.

**Response 200:** Page of GovSyncLog

---

### GET `/admin/gov-sync/dead-letter`
Entries that have exhausted all retries (5 max).

**Response 200:** Array of GovSyncLog

---

### POST `/admin/gov-sync/{id}/retry`
Required role: `MOH_ADMIN`. Re-queue a dead-letter entry.

**Response 204**

---

## 19. Notifications (`/notifications`)

`notificationType` enum: `APPOINTMENT_REMINDER`, `VACCINATION_REMINDER`, `REGISTRATION_CONFIRMATION`, `CUSTOM`

### POST `/notifications/send`
Enqueue an outbound SMS notification.

**Request body:**
```json
{
  "recipientUserId": "uuid",
  "phoneNumber": "+250788XXXXXX",
  "message": "Your appointment is tomorrow at 9 AM",
  "notificationType": "APPOINTMENT_REMINDER",
  "scheduledAt": "2026-06-01T08:00:00"
}
```

Either `recipientUserId` or `phoneNumber` is required.

**Response 200:** NotificationResponse

---

### POST `/notifications/process-queue`
Trigger processing of pending notification queue (normally runs on a schedule).

**Response 202**

---

## 20. Webhooks — Africa's Talking (`/webhooks/at`)

**Public endpoint — no JWT required.** HMAC-SHA256 signature validation enforced in application code.  
Signature header: `X-Africastalking-Signature` (or legacy `X-AT-Signature`).

### POST `/webhooks/at/delivery`
Receive SMS delivery status callback from Africa's Talking.

**Request body (raw JSON, signature verified):**
```json
{
  "id": "ATXid...",
  "status": "Success",
  "phoneNumber": "+250788XXXXXX",
  "networkCode": "63902",
  "failureReason": ""
}
```

`status` values: `Success` → marks DELIVERED; `Failed` / `Rejected` → marks FAILED.

**Response 200** (valid signature) | **401** (missing/invalid signature)

---

## Role Summary

| Role | Primary Access |
|------|---------------|
| `HEALTH_WORKER` | Create/read clinical data (visits, vaccinations, mothers, pregnancies). Facility-scoped. |
| `FACILITY_ADMIN` | All HEALTH_WORKER access + facility management, service request approval. |
| `MOH_ADMIN` | Full platform access including user management, reports, gov-sync. |
| `DISTRICT_OFFICER` | Read analytics for scoped geo districts. |
| `GOVERNMENT_ANALYST` | Read mothers (geo-scoped). |
| `PATIENT` | Submit service requests (requires GOV_DATA_SHARE consent). |

---

## Facility-Scoped Endpoints

The following GET/PATCH endpoints require `?facilityId={uuid}` as a query parameter for facility scope enforcement. Requests without it return 400.

- `GET /pregnancies/{id}?facilityId=`
- `GET /pregnancies/by-mother/{motherId}?facilityId=`
- `PATCH /pregnancies/{id}?facilityId=`
- `GET /health-visits/{id}?facilityId=`
- `GET /health-visits/by-patient?...&facilityId=`
- `PATCH /health-visits/{id}?facilityId=`
- `GET /children/{id}?facilityId=`
- `GET /children/by-mother/{motherId}?facilityId=`
- `PATCH /children/{id}?facilityId=`
- `GET /vaccinations/by-child/{childId}?facilityId=`
- `PATCH /vaccinations/{id}/administer?facilityId=`
- `GET /appointments/{id}?facilityId=`
- `GET /appointments/patient/{patientRefId}?...&facilityId=`
- `PUT /appointments/{id}?facilityId=`
- `GET /consents/{id}?facilityId=`
- `GET /consents/by-mother/{motherId}?facilityId=`
- `PATCH /consents/{id}/revoke?facilityId=`

---

## UserResponse Schema

```json
{
  "id": "uuid",
  "phoneNumber": "+250788XXXXXX",
  "nationalId": "1199XXXXXXXXXX",
  "firstName": "Jane",
  "lastName": "Doe",
  "role": "HEALTH_WORKER",
  "preferredLanguage": "rw",
  "active": true,
  "facilityId": "uuid",
  "geoLocationId": "uuid",
  "createdAt": "2026-01-01T10:00:00",
  "lastLogin": "2026-05-28T08:00:00"
}
```

---

## Error Response Examples

**Validation error (400):**
```json
{
  "success": false,
  "message": "Validation error: dateOfBirth: must be a past date",
  "data": null
}
```

**Auth error (401):**
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "data": null
}
```

**Server error (500):**
```json
{
  "success": false,
  "message": "An unexpected error occurred. Reference: 748f058a-bee5-4695-b9f6-1328929d050d",
  "data": null
}
```

Use the `Reference` UUID to correlate with the application log.
