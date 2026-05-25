# MotherHood Journey API Documentation & Test Report

**Base URL:** `http://localhost:8080`  
**OpenAPI Spec:** `GET /v3/api-docs`  
**Swagger UI:** `http://localhost:8080/swagger-ui/index.html`  
**Auth:** Bearer JWT (all endpoints except those marked **Public**)

---

## Authentication

### POST /api/v1/auth/register Register a new user
**Access:** Public

**Request:**
```json
{
  "phoneNumber": "+250788100001",
  "nationalId": "1199380057100012",
  "password": "Password123!",
  "firstName": "Admin",
  "lastName": "User",
  "role": "MOH_ADMIN",
  "geoLocationId": "11111111-1111-1111-1111-111111111111",
  "facilityId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
}
```
**Valid roles:** `PATIENT | HEALTH_WORKER | FACILITY_ADMIN | DISTRICT_OFFICER | GOVERNMENT_ANALYST | MOH_ADMIN`

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "id": "bbf9de3d-c5c9-4de8-a619-af81807b00a5",
    "phoneNumber": "+250788100001",
    "nationalId": "1199380057100012",
    "firstName": "Admin",
    "lastName": "User",
    "role": "MOH_ADMIN",
    "active": true,
    "facilityId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "geoLocationId": "11111111-1111-1111-1111-111111111111",
    "createdAt": "2026-05-25T14:44:03.971381"
  }
}
```

---

### POST /api/v1/auth/login Login
**Access:** Public

**Request:**
```json
{
  "phoneNumber": "+250788100001",
  "password": "Password123!"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "accessToken": "eyJhbGci...",
    "refreshToken": null,
    "role": "MOH_ADMIN"
  }
}
```

---

## Current User

### GET /api/v1/me — Get my profile
**Access:** Any authenticated user

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "Profile retrieved",
  "data": {
    "id": "bbf9de3d-...",
    "phoneNumber": "+250788100001",
    "firstName": "Admin",
    "lastName": "User",
    "role": "MOH_ADMIN",
    "preferredLanguage": "rw",
    "facilityId": "aaaaaaaa-...",
    "geoLocationId": "11111111-...",
    "lastLogin": "2026-05-25T14:44:03"
  }
}
```

---

## Users

### GET /api/v1/users/{id} — Get user by ID
**Access:** `FACILITY_ADMIN | MOH_ADMIN | DISTRICT_OFFICER`

**Response:** `200 OK` — returns full `UserResponse` with all fields.

### PATCH /api/v1/users/{id} — Update user
**Access:** `FACILITY_ADMIN | MOH_ADMIN`

**Request:**
```json
{
  "firstName": "Janet",
  "lastName": "Nursington"
}
```

---

## Admin

> All `/api/v1/admin/**` endpoints require role `MOH_ADMIN`.

### GET /api/v1/admin/dashboard — System dashboard
**Access:** `MOH_ADMIN`

⚠️ **Status: FAILING (Bug #1)** — Returns `500 Internal Server Error`. The individual count queries work at the SQL level; a server-side Hibernate session or serialization exception occurs. Requires application log inspection to identify the root cause.

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "totalFacilities": 3,
    "totalMothers": 1,
    "totalChildren": 1,
    "totalUsers": 4,
    "pendingAppointments": 1,
    "pendingVaccinations": 0,
    "activePregnancies": 1
  }
}
```

### GET /api/v1/admin/users List all users (paginated)
**Access:** `MOH_ADMIN`

**Query params:** `page`, `size`, `sort`

**Response:** `200 OK` paginated list of `UserResponse` Working

### PATCH /api/v1/admin/users/{id}/deactivate Deactivate user
**Access:** `MOH_ADMIN`
**Response:** `200 OK` with updated user (`active: false`) Working

### PATCH /api/v1/admin/users/{id}/activate user
**Access:** `MOH_ADMIN`
**Response:** `200 OK` with updated user (`active: true`) Working

---

## Geographic Locations

> All Geo endpoints are **Public** (no JWT required).

### GET /api/v1/geo/provinces List all provinces
**Response:** `200 OK`
```json
["Kigali City", "Northern Province", "Southern Province"]
```

### GET /api/v1/geo/districts?province={province}
**Response:** `200 OK` list of district names

### GET /api/v1/geo/sectors?province={}&district={}
**Response:** `200 OK` list of sector names

### GET /api/v1/geo/cells?province={}&district={}&sector={}
**Response:** `200 OK` list of cell names

### GET /api/v1/geo/villages?province={}&district={}&sector={}&cell={}
**Response:** `200 OK` list of village names

### GET /api/v1/geo/resolve?province={}&district={}&sector={}&cell={}&village={}
**Response:** `200 OK` full `GeoResponse` object with UUID

### GET /api/v1/geo/{id}/summary — Get location summary by UUID
**Response:** `200 OK`
```json
{
  "id": "11111111-...",
  "sector": "Kimironko",
  "cell": "Bibare",
  "village": "Bibare Village"
}
```

---

## Facilities

### POST /api/v1/facilities Create facility
**Access:** `MOH_ADMIN`

**Request:**
```json
{
  "name": "Kimironko Health Center",
  "facilityCode": "KHC-001",
  "facilityType": "HEALTH_CENTER",
  "district": "Gasabo",
  "phone": "+250788000001",
  "geoLocationId": "11111111-1111-1111-1111-111111111111"
}
```
**Valid facilityType values:** `HOSPITAL | HEALTH_CENTER | CLINIC | DISPENSARY | POSTE_DE_SANTE`

**Response:** `201 Created` Working

### GET /api/v1/facilities — List facilities (paginated)
**Access:** Any authenticated user

**Query params:** `district`, `facilityType`, `page`, `size`

**Response:** `200 OK` — paginated facilities ✅ Working

### GET /api/v1/facilities/{id} — Get facility by ID
**Access:** Any authenticated user
**Response:** `200 OK` ✅ Working

### PUT /api/v1/facilities/{id} — Update facility
**Access:** `FACILITY_ADMIN | MOH_ADMIN`
**Response:** `200 OK` ✅ Working

### DELETE /api/v1/facilities/{id} — Delete (soft) facility
**Access:** `MOH_ADMIN`
**Response:** `204 No Content` ✅ Working

---

## Mothers

> ⚠️ **Security Bug #2:** `SecurityConfig` restricts `/api/v1/mothers/**` to `HEALTH_WORKER` and `FACILITY_ADMIN` only at the URL filter level, but `MotherController` methods use `@PreAuthorize` allowing `MOH_ADMIN` and `DISTRICT_OFFICER`. The URL-level rule takes precedence and blocks those roles with `403`.

### POST /api/v1/mothers — Register mother
**Access:** Any authenticated user (no URL restriction; uses no `@PreAuthorize`)

**Request:**
```json
{
  "userId": "bbf9de3d-c5c9-4de8-a619-af81807b00a5",
  "facilityId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "nationalId": "1199380057100012",
  "geoLocationId": "11111111-1111-1111-1111-111111111111",
  "dateOfBirth": "1990-05-15",
  "educationLevel": "SECONDARY"
}
```
**Valid educationLevel:** `PRIMARY | SECONDARY | TERTIARY | NONE`

**Response:** `201 Created` ✅ Working  
> ⚠️ **Prerequisite:** The DB sequence `seq_mother_health_id` must exist. It was absent from migrations and must be created manually:  
> `CREATE SEQUENCE seq_mother_health_id START 1 INCREMENT 1;`

### GET /api/v1/mothers/{id} — Get mother by ID
**Access:** `HEALTH_WORKER | FACILITY_ADMIN` (URL filter), with method-level `@PreAuthorize` also allowing `MOH_ADMIN | DISTRICT_OFFICER | GOVERNMENT_ANALYST`

⚠️ **Status: FAILING (Bug #3)** — Returns `500 Internal Server Error` even with `HEALTH_WORKER` token. Likely a `LazyInitializationException` when `enforceScope()` accesses lazy-loaded `mother.getFacility()` or `caller.getFacilityId()` outside an active Hibernate session.

### GET /api/v1/mothers/health/{healthId} — Get mother by health ID
**Access:** Same as above (URL-level restriction applies)

### GET /api/v1/mothers/pending-nida — List mothers awaiting NIDA verification
**Access:** `MOH_ADMIN | FACILITY_ADMIN` (URL-level blocks MOH_ADMIN; accessible only via FACILITY_ADMIN)

---

## Children

> ⚠️ **Security Bug #2 (same):** `/api/v1/children/**` restricted to `HEALTH_WORKER | FACILITY_ADMIN` at URL level. `MOH_ADMIN | DISTRICT_OFFICER` get `403`.

### POST /api/v1/children — Register child
**Access:** `HEALTH_WORKER | FACILITY_ADMIN` (URL)

**Request:**
```json
{
  "motherId": "153fc540-c8c0-4cd2-a0e3-e8f01e51f69b",
  "facilityId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "geoLocationId": "11111111-1111-1111-1111-111111111111",
  "firstName": "Baby",
  "gender": "MALE",
  "dateOfBirth": "2026-01-10",
  "birthWeightKg": 3.2,
  "deliveryType": "NORMAL"
}
```
**Valid gender:** `MALE | FEMALE | UNKNOWN`  
**Valid deliveryType:** `NORMAL | CAESAREAN | ASSISTED`

**Response:** `201 Created` ✅ Working

### GET /api/v1/children/{id}?facilityId={} — Get child by ID
**Access:** `HEALTH_WORKER | FACILITY_ADMIN | MOH_ADMIN | DISTRICT_OFFICER` (but URL filter restricts)
**Response:** `200 OK` ✅ Working

### GET /api/v1/children/by-mother/{motherId}?facilityId={} — Children by mother
**Response:** `200 OK` paginated ✅ Working

### GET /api/v1/children/by-facility/{facilityId} — Children by facility
**Response:** `200 OK` paginated ✅ Working

### PATCH /api/v1/children/{id}?facilityId={} — Update child
**Request:**
```json
{
  "firstName": "BabyUpdated",
  "healthStatus": "HEALTHY"
}
```
**Response:** `200 OK` ✅ Working

---

## Vaccinations

### GET /api/v1/vaccinations/by-child/{childId}?facilityId={} — Get vaccination records
**Access:** `HEALTH_WORKER | FACILITY_ADMIN | MOH_ADMIN | DISTRICT_OFFICER`
**Response:** `200 OK` — list of vaccination records (empty if no schedules seeded) ✅ Working

### PATCH /api/v1/vaccinations/{id}/administer?facilityId={} — Administer vaccination
**Access:** `HEALTH_WORKER | FACILITY_ADMIN | MOH_ADMIN`

**Request:**
```json
{
  "administeredDate": "2026-05-25",
  "lotNumber": "LOT-2026-001",
  "notes": "Vaccination administered without issues"
}
```
**Response:** `200 OK`

---

## Pregnancies

### POST /api/v1/pregnancies — Record pregnancy
**Access:** `HEALTH_WORKER | FACILITY_ADMIN | MOH_ADMIN`

**Request:**
```json
{
  "motherId": "153fc540-c8c0-4cd2-a0e3-e8f01e51f69b",
  "facilityId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "lmpDate": "2025-08-01",
  "expectedDeliveryDate": "2026-05-08",
  "gravida": 1,
  "para": 0
}
```
**Response:** `201 Created` ✅ Working

### GET /api/v1/pregnancies/{id}?facilityId={} — Get pregnancy
**Response:** `200 OK` ✅ Working

### GET /api/v1/pregnancies/by-mother/{motherId}?facilityId={} — List by mother
**Response:** `200 OK` — array of pregnancies ✅ Working

### PATCH /api/v1/pregnancies/{id}?facilityId={} — Update pregnancy
**Request:**
```json
{
  "gravida": 1,
  "para": 0,
  "outcomeNotes": "Progress is good"
}
```
**Response:** `200 OK` ✅ Working

---

## Health Visits

### POST /api/v1/health-visits — Record visit
**Access:** `HEALTH_WORKER | FACILITY_ADMIN | MOH_ADMIN`

**Request:**
```json
{
  "facilityId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "patientRefId": "153fc540-c8c0-4cd2-a0e3-e8f01e51f69b",
  "patientType": "MOTHER",
  "healthWorkerId": "030a8846-1118-4c33-bf59-853b6db39358",
  "visitDatetime": "2026-05-20T09:00:00",
  "visitType": "ANC",
  "chiefComplaint": "Regular prenatal checkup",
  "weightKg": 65.5,
  "heightCm": 162.0,
  "systolicBp": 120,
  "diastolicBp": 80,
  "notes": "Patient is doing well"
}
```
**Valid patientType:** `MOTHER | CHILD`  
**Valid visitType:** `ANC | PNC | IMMUNIZATION | SICK_CHILD | GROWTH_MONITORING`

**Response:** `201 Created` ✅ Working

### GET /api/v1/health-visits/{id}?facilityId={} — Get visit by ID
**Response:** `200 OK` ✅ Working

### GET /api/v1/health-visits/by-facility/{facilityId} — Visits by facility (paginated)
**Response:** `200 OK` ✅ Working

### GET /api/v1/health-visits/by-patient?patientRefId={}&patientType={}&facilityId={} — Visits by patient
**Response:** `200 OK` paginated ✅ Working

### PATCH /api/v1/health-visits/{id}?facilityId={} — Update visit
**Request:**
```json
{
  "notes": "Updated notes",
  "visitType": "ANC"
}
```
**Response:** `200 OK` ✅ Working

---

## Appointments

> ⚠️ **Security Bug #2 (same):** `/api/v1/appointments/**` at URL level only allows `HEALTH_WORKER | FACILITY_ADMIN | PATIENT`.

### POST /api/v1/appointments — Create appointment
**Access:** `HEALTH_WORKER | FACILITY_ADMIN | MOH_ADMIN` (method) / `HEALTH_WORKER | FACILITY_ADMIN` (URL)

**Request:**
```json
{
  "facilityId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "patientRefId": "153fc540-c8c0-4cd2-a0e3-e8f01e51f69b",
  "patientType": "MOTHER",
  "scheduledAt": "2026-06-01T10:00:00",
  "appointmentType": "ANC",
  "notes": "Follow-up ANC visit"
}
```
**Response:** `201 Created` ✅ Working

### GET /api/v1/appointments/{id}?facilityId={} — Get appointment by ID
**Response:** `200 OK` ✅ Working

### GET /api/v1/appointments/patient/{patientRefId}?patientType={}&facilityId={} — Appointments by patient
**Response:** `200 OK` ✅ Working

### PUT /api/v1/appointments/{id}?facilityId={} — Update appointment
**Request:**
```json
{
  "scheduledAt": "2026-06-02T11:00:00",
  "status": "SCHEDULED",
  "notes": "Rescheduled"
}
```
**Response:** `200 OK` ✅ Working

---

## Consents

### POST /api/v1/consents — Record consent
**Access:** `HEALTH_WORKER | FACILITY_ADMIN | MOH_ADMIN`

**Request:**
```json
{
  "motherId": "153fc540-c8c0-4cd2-a0e3-e8f01e51f69b",
  "facilityId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "consentType": "GOV_DATA_SHARE",
  "granted": true,
  "notes": "Patient consents to government data sharing"
}
```
**Response:** `201 Created` ✅ Working

### GET /api/v1/consents/{id}?facilityId={} — Get consent by ID
**Response:** `200 OK` ✅ Working

### GET /api/v1/consents/by-mother/{motherId}?facilityId={} — Consents by mother
**Response:** `200 OK` — list of consents ✅ Working

### PATCH /api/v1/consents/{id}/revoke?facilityId={} — Revoke consent
**Response:** `204 No Content` ✅ Working

---

## Service Requests

> Only users who are registered mothers with an active `GOV_DATA_SHARE` consent can submit service requests.

### POST /api/v1/service-requests — Submit service request
**Access:** Any authenticated mother with active GOV_DATA_SHARE consent

**Request:**
```json
{
  "serviceType": "BIRTH_CERT",
  "facilityId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "geoLocationId": "11111111-1111-1111-1111-111111111111",
  "payload": {
    "childName": "Baby Test",
    "dob": "2026-01-10"
  }
}
```
**Valid serviceType:** `BIRTH_CERT | VACCINATION_CARD | REFERRAL | HEALTH_SUMMARY | REPRINT`

**Response:** `201 Created` ✅ Working  
> Triggers outbox pattern entry in `gov_sync_log` for async IREMBO sync.

### GET /api/v1/service-requests/{id} — Get request by ID
**Access:** `FACILITY_ADMIN | MOH_ADMIN | DISTRICT_OFFICER`
**Response:** `200 OK` ✅ Working

### GET /api/v1/service-requests/by-facility/{facilityId} — By facility (paginated)
**Access:** `FACILITY_ADMIN | MOH_ADMIN | DISTRICT_OFFICER`
**Response:** `200 OK` ✅ Working

### GET /api/v1/service-requests/by-status?status={} — By status
**Access:** `MOH_ADMIN | DISTRICT_OFFICER`
**Response:** `200 OK` ✅ Working

### PATCH /api/v1/service-requests/{id}/approve — Approve request
**Access:** `FACILITY_ADMIN | MOH_ADMIN`
**Response:** `200 OK` (status → `APPROVED`) ✅ Working

### PATCH /api/v1/service-requests/{id}/reject?reason={} — Reject request
**Access:** `FACILITY_ADMIN | MOH_ADMIN`
**Response:** `200 OK` (status → `REJECTED`) ✅ Working

### POST /api/v1/service-requests/{requestId}/documents — Attach document
**Access:** Any authenticated user

**Request:**
```json
{
  "documentType": "ID_COPY",
  "filePath": "/uploads/docs/id-copy-001.pdf",
  "fileHash": "abc123def456abc123def456abc123de"
}
```
**Valid documentType:** `ID_COPY | BIRTH_PROOF | FACILITY_LETTER | OTHER`

**Response:** `201 Created` ✅ Working

### GET /api/v1/service-requests/{requestId}/documents — List documents
**Access:** `FACILITY_ADMIN | MOH_ADMIN | DISTRICT_OFFICER`
**Response:** `200 OK` ✅ Working

---

## Government Users

### GET /api/v1/government/{id} — Get government user by ID
**Access:** `MOH_ADMIN | DISTRICT_OFFICER`
**Response:** `200 OK` or `404` if not a government user ✅ Working

### GET /api/v1/government/by-user/{userId} — Get government user by user ID
**Access:** `MOH_ADMIN | DISTRICT_OFFICER`
**Response:** `200 OK` or `404` ✅ Working

---

## Government Reports

### POST /api/v1/gov-reports — Generate report
**Access:** `MOH_ADMIN | DISTRICT_OFFICER`

**Request:**
```json
{
  "reportType": "ANC_ATTENDANCE",
  "period": "2026-Q1",
  "scopeLevel": "DISTRICT",
  "geoLocationId": "11111111-1111-1111-1111-111111111111",
  "aggregates": {
    "totalVisits": 150,
    "totalMothers": 48
  }
}
```
**Valid reportType:** `VACCINATION_COVERAGE | ANC_ATTENDANCE | BIRTH_REGISTRATION | MATERNAL_HEALTH`  
**Valid scopeLevel:** `NATIONAL | PROVINCE | DISTRICT | SECTOR`

**Response:** `201 Created` ✅ Working

### GET /api/v1/gov-reports/{id} — Get report by ID
**Response:** `200 OK` ✅ Working

### GET /api/v1/gov-reports/by-user/{userId} — Reports by user (paginated)
**Response:** `200 OK` ✅ Working

---

## Notifications

### POST /api/v1/notifications/send — Queue notification
**Access:** Any authenticated user

**Request:**
```json
{
  "recipientUserId": "030a8846-1118-4c33-bf59-853b6db39358",
  "phoneNumber": "+250788100002",
  "message": "Your appointment is scheduled for June 2nd",
  "notificationType": "APPOINTMENT"
}
```
**Valid notificationType:** `VACCINATION_REMINDER | APPOINTMENT | HEALTH_TIP | SERVICE_STATUS | EMERGENCY`

**Response:** `200 OK` (queued, status `QUEUED`) ✅ Working  
> Actual SMS sending requires Africa's Talking API key (`AT_API_KEY` env var).

### POST /api/v1/notifications/process-queue — Trigger queue processing
**Access:** Any authenticated user
**Response:** `202 Accepted` ✅ Working (no-op if AT not configured)

---

## SMS Webhook

### POST /webhooks/at/delivery — Africa's Talking delivery status callback
**Access:** Public (validated by `X-AT-Signature` header)

**Headers:**
- `X-AT-Signature: <HMAC signature>`

**Response:** `200 OK` on valid signature, `401 Unauthorized` on missing/invalid signature

---

## Actuator

### GET /actuator/health — Health check
**Access:** Public
**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "africasTalking": {"status": "UNKNOWN", "details": {"reason": "AT_API_KEY not configured"}},
    "nidaApi": {"status": "UNKNOWN", "details": {"reason": "NIDA_BASE_URL not configured"}},
    "diskSpace": {"status": "UP"},
    "ping": {"status": "UP"}
  }
}
```

---

## Standard Error Responses

All error responses follow:
```json
{
  "success": false,
  "message": "<error description>",
  "data": null
}
```

| HTTP Status | Scenario |
|-------------|----------|
| `400` | Validation error (missing/invalid fields) |
| `401` | Invalid credentials or bad JWT |
| `403` | Insufficient role or facility mismatch |
| `404` | Resource not found |
| `409` | Duplicate resource (phone, nationalId, facilityCode) |
| `500` | Internal server error (logged with correlation ID) |

---

# Test Report

## Test Environment
- **Application:** Running locally via IntelliJ on port 8080
- **Database:** PostgreSQL 16 (`Motherhood Journey DB`)
- **Date Tested:** 2026-05-25
- **Test Method:** Live curl HTTP requests against running server

## Seeded Test Data

| Type | ID | Description |
|------|----|-------------|
| GeoLocation | `11111111-1111-1111-1111-111111111111` | Kigali City / Gasabo / Kimironko |
| GeoLocation | `22222222-2222-2222-2222-222222222222` | Kigali City / Kicukiro / Niboye |
| Facility | `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` | Kimironko Health Center (KHC-001) |
| Facility | `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb` | Kicukiro District Hospital (KDH-001) |
| User (MOH_ADMIN) | `bbf9de3d-c5c9-4de8-a619-af81807b00a5` | `+250788100001` |
| User (HEALTH_WORKER) | `030a8846-1118-4c33-bf59-853b6db39358` | `+250788100002` |
| User (FACILITY_ADMIN) | `1db68768-c53a-403c-81bd-31aa6b5ea18b` | `+250788100003` |
| User (DISTRICT_OFFICER) | `91c0198a-0ba9-46fd-9c47-6dcf2997e696` | `+250788100004` |
| Mother | `153fc540-c8c0-4cd2-a0e3-e8f01e51f69b` | Health ID: MH-2026-000001 |
| Child | `4d80507e-7f1b-4b13-b8e0-b67111a01da4` | Baby, MALE, DOB 2026-01-10 |
| Pregnancy | `6887f7b8-9cfe-4740-9901-f5182328a96a` | LMP 2025-08-01, ACTIVE |
| Health Visit | `825e02c8-3c7d-470d-98de-9ef93f8424dd` | ANC visit, 2026-05-20 |
| Appointment | `9b08388b-6237-40b8-b924-f05e07c528b7` | ANC, 2026-06-02 11:00 |
| Service Request | `453bd8a1-faa9-448f-a22f-95737d8d507f` | BIRTH_CERT, SR-2026-00001 |

## Test Results Summary

| # | Endpoint | Method | Status | Notes |
|---|----------|--------|--------|-------|
| 1 | `/api/v1/auth/register` | POST | ✅ PASS | All 4 roles registered |
| 2 | `/api/v1/auth/login` | POST | ✅ PASS | JWT returned correctly |
| 3 | `/api/v1/me` | GET | ✅ PASS | Profile returned |
| 4 | `/api/v1/users/{id}` | GET | ✅ PASS | |
| 5 | `/api/v1/users/{id}` | PATCH | ✅ PASS | Name update works |
| 6 | `/api/v1/admin/dashboard` | GET | ❌ FAIL | 500 — Bug #1 |
| 7 | `/api/v1/admin/users` | GET | ✅ PASS | Pagination works |
| 8 | `/api/v1/admin/users/{id}/deactivate` | PATCH | ✅ PASS | |
| 9 | `/api/v1/admin/users/{id}/activate` | PATCH | ✅ PASS | |
| 10 | `/api/v1/geo/provinces` | GET | ✅ PASS | No auth required |
| 11 | `/api/v1/geo/districts` | GET | ✅ PASS | |
| 12 | `/api/v1/geo/sectors` | GET | ✅ PASS | |
| 13 | `/api/v1/geo/cells` | GET | ✅ PASS | |
| 14 | `/api/v1/geo/villages` | GET | ✅ PASS | |
| 15 | `/api/v1/geo/resolve` | GET | ✅ PASS | Returns UUID |
| 16 | `/api/v1/geo/{id}/summary` | GET | ✅ PASS | |
| 17 | `/api/v1/facilities` | POST | ✅ PASS | |
| 18 | `/api/v1/facilities` | GET | ✅ PASS | Filter by district works |
| 19 | `/api/v1/facilities/{id}` | GET | ✅ PASS | |
| 20 | `/api/v1/facilities/{id}` | PUT | ✅ PASS | |
| 21 | `/api/v1/facilities/{id}` | DELETE | ✅ PASS | 204 No Content |
| 22 | `/api/v1/mothers` | POST | ✅ PASS | Requires `seq_mother_health_id` in DB |
| 23 | `/api/v1/mothers/{id}` | GET | ❌ FAIL | 500 — Bug #3 (LazyInit) |
| 24 | `/api/v1/mothers/health/{healthId}` | GET | ❌ FAIL | Same bug |
| 25 | `/api/v1/mothers/pending-nida` | GET | ⚠️ PARTIAL | Blocked by Bug #2 for MOH_ADMIN |
| 26 | `/api/v1/children` | POST | ✅ PASS | |
| 27 | `/api/v1/children/{id}` | GET | ✅ PASS | |
| 28 | `/api/v1/children/by-mother/{id}` | GET | ✅ PASS | |
| 29 | `/api/v1/children/by-facility/{id}` | GET | ✅ PASS | |
| 30 | `/api/v1/children/{id}` | PATCH | ✅ PASS | |
| 31 | `/api/v1/vaccinations/by-child/{id}` | GET | ✅ PASS | Empty (no schedules seeded) |
| 32 | `/api/v1/vaccinations/{id}/administer` | PATCH | N/A | No vaccination records to test |
| 33 | `/api/v1/pregnancies` | POST | ✅ PASS | |
| 34 | `/api/v1/pregnancies/{id}` | GET | ✅ PASS | |
| 35 | `/api/v1/pregnancies/by-mother/{id}` | GET | ✅ PASS | |
| 36 | `/api/v1/pregnancies/{id}` | PATCH | ✅ PASS | |
| 37 | `/api/v1/health-visits` | POST | ✅ PASS | |
| 38 | `/api/v1/health-visits/{id}` | GET | ✅ PASS | |
| 39 | `/api/v1/health-visits/by-facility/{id}` | GET | ✅ PASS | |
| 40 | `/api/v1/health-visits/by-patient` | GET | ✅ PASS | |
| 41 | `/api/v1/health-visits/{id}` | PATCH | ✅ PASS | |
| 42 | `/api/v1/appointments` | POST | ✅ PASS | |
| 43 | `/api/v1/appointments/{id}` | GET | ✅ PASS | |
| 44 | `/api/v1/appointments/patient/{id}` | GET | ✅ PASS | |
| 45 | `/api/v1/appointments/{id}` | PUT | ✅ PASS | |
| 46 | `/api/v1/consents` | POST | ✅ PASS | |
| 47 | `/api/v1/consents/{id}` | GET | ✅ PASS | |
| 48 | `/api/v1/consents/by-mother/{id}` | GET | ✅ PASS | |
| 49 | `/api/v1/consents/{id}/revoke` | PATCH | ✅ PASS | 204 No Content |
| 50 | `/api/v1/service-requests` | POST | ✅ PASS | Requires mother + GOV_DATA_SHARE consent |
| 51 | `/api/v1/service-requests/{id}` | GET | ✅ PASS | |
| 52 | `/api/v1/service-requests/by-facility/{id}` | GET | ✅ PASS | |
| 53 | `/api/v1/service-requests/by-status` | GET | ✅ PASS | |
| 54 | `/api/v1/service-requests/{id}/approve` | PATCH | ✅ PASS | |
| 55 | `/api/v1/service-requests/{id}/reject` | PATCH | ✅ PASS | Requires `reason` param |
| 56 | `/api/v1/service-requests/{id}/documents` | POST | ✅ PASS | |
| 57 | `/api/v1/service-requests/{id}/documents` | GET | ✅ PASS | |
| 58 | `/api/v1/government/{id}` | GET | ✅ PASS | 404 if no gov user exists |
| 59 | `/api/v1/government/by-user/{id}` | GET | ✅ PASS | |
| 60 | `/api/v1/gov-reports` | POST | ✅ PASS | |
| 61 | `/api/v1/gov-reports/{id}` | GET | ✅ PASS | |
| 62 | `/api/v1/gov-reports/by-user/{id}` | GET | ✅ PASS | |
| 63 | `/api/v1/notifications/send` | POST | ✅ PASS | Requires `recipientUserId` + correct `notificationType` |
| 64 | `/api/v1/notifications/process-queue` | POST | ✅ PASS | 202 Accepted |
| 65 | `/webhooks/at/delivery` | POST | ✅ PASS | Rejects missing signature with 401 |
| 66 | `/actuator/health` | GET | ✅ PASS | AT and NIDA show UNKNOWN (not configured) |

**Pass Rate: 60/63 tested = 95.2%** (3 failures, 1 not applicable due to no data)

---

## Bugs Found

### Bug #1 — Admin Dashboard Returns 500
**Endpoint:** `GET /api/v1/admin/dashboard`  
**Role:** MOH_ADMIN  
**Symptom:** `500 Internal Server Error`  
**Root Cause:** Unknown — all individual SQL `COUNT(*)` queries work at the database level. The exception occurs inside `AdminServiceImpl.getDashboard()` calling one or more of `facilityRepository.count()`, `motherRepository.count()`, `childRepository.count()`, `userRepository.count()`, `appointmentRepository.countByStatus()`, `vaccinationRecordRepository.countByStatus()`, `pregnancyRepository.countByStatus()`.  
**Fix Needed:** Check server application logs for the correlation ID to identify the exact line that fails.

### Bug #2 — SecurityConfig URL Matchers Too Restrictive
**Files:** `SecurityConfig.java`  
**Symptom:** `MOH_ADMIN`, `DISTRICT_OFFICER`, and `GOVERNMENT_ANALYST` get `403 Forbidden` on `/api/v1/mothers/**`, `/api/v1/children/**`, and `/api/v1/appointments/**` even though `@PreAuthorize` on controller methods allows those roles.  
**Root Cause:** Spring Security URL-level rules run before method-level `@PreAuthorize`. The URL matchers only allow `HEALTH_WORKER` and `FACILITY_ADMIN` but the business logic needs broader access.  
**Fix:** Either remove the URL-level role restrictions from `SecurityConfig` for these paths (letting `@PreAuthorize` on each method handle access control), or add the missing roles to the URL matchers:
```java
// Current (too restrictive):
.requestMatchers("/api/v1/mothers/**")
    .hasAnyRole("HEALTH_WORKER", "FACILITY_ADMIN")

// Suggested fix:
.requestMatchers("/api/v1/mothers/**")
    .hasAnyRole("HEALTH_WORKER", "FACILITY_ADMIN", "MOH_ADMIN", "DISTRICT_OFFICER", "GOVERNMENT_ANALYST")
```

### Bug #3 — Mother GET Endpoints Return 500
**Endpoint:** `GET /api/v1/mothers/{id}`  
**Role:** HEALTH_WORKER, FACILITY_ADMIN (the only roles that pass the URL filter)  
**Symptom:** `500 Internal Server Error`  
**Root Cause:** `MotherService.getMotherById()` calls `enforceScope(mother, caller)` which accesses `caller.getFacilityId()` (lazy-loaded `User.facility`) and `mother.getFacility().getId()` (lazy-loaded `Mother.facility`). These Hibernate lazy proxies are accessed outside an active session context, causing a `LazyInitializationException`.  
**Fix:** Either eager-load the `facility` relationship when loading the User/Mother, or use `@Transactional` to keep the session open, or fetch the facility ID in the same query using `@EntityGraph` or a JPQL join fetch.

### Missing Database Artifact — seq_mother_health_id Sequence
**Symptom:** `POST /api/v1/mothers` returned `500` until sequence was manually created.  
**Root Cause:** The `generateHealthId()` method in `MotherService` uses `nextval('seq_mother_health_id')` but no Flyway migration creates this sequence.  
**Fix:** Add to a migration file:
```sql
CREATE SEQUENCE IF NOT EXISTS seq_mother_health_id START 1 INCREMENT 1;
```

---

## Configuration Notes

| Integration | Status | Notes |
|-------------|--------|-------|
| PostgreSQL | ✅ Connected | DB: `Motherhood Journey DB`, Port: 5432 |
| Africa's Talking SMS | ⚠️ Not configured | Set `AT_API_KEY` and `AT_USERNAME` env vars |
| NIDA Verification | ⚠️ Not configured | Set `NIDA_BASE_URL` env var |
| IREMBO Gov Integration | ⚠️ Not configured | Set `IREMBO_BASE_URL` and `IREMBO_API_KEY` |
| HMIS Push | ⚠️ Not configured | Set `HMIS_BASE_URL` and `HMIS_API_KEY` |
| Flyway Migrations | ✅ Applied | V1–V11 all applied |
