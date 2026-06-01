# MotherHood Journey — Complete Testing Guide

> **Version:** V27 seed data · **Base URL:** `http://localhost:8080` · **All passwords:** `Test@1234`

---

## Table of Contents

1. [Environment Setup](#1-environment-setup)
2. [Running the Application](#2-running-the-application)
3. [Seed Accounts Quick Reference](#3-seed-accounts-quick-reference)
4. [Seed Facilities Quick Reference](#4-seed-facilities-quick-reference)
5. [Authentication — Login & Token Flow](#5-authentication--login--token-flow)
6. [Role-by-Role Testing Paths](#6-role-by-role-testing-paths)
   - 6.1 [MOH_ADMIN — System Administrator](#61-moh_admin--system-administrator)
   - 6.2 [FACILITY_ADMIN — Facility Manager](#62-facility_admin--facility-manager)
   - 6.3 [DISTRICT_OFFICER — Government Inspector](#63-district_officer--government-inspector)
   - 6.4 [HEALTH_WORKER — Clinical Staff](#64-health_worker--clinical-staff)
   - 6.5 [MOTHER — Patient Self-Service](#65-mother--patient-self-service)
7. [End-to-End Journeys](#7-end-to-end-journeys)
   - 7.1 [New Mother Registration → NIDA Verification](#71-new-mother-registration--nida-verification)
   - 7.2 [Antenatal Care (ANC) Visit](#72-antenatal-care-anc-visit)
   - 7.3 [Delivery & Child Registration → Vaccination Schedule](#73-delivery--child-registration--vaccination-schedule)
   - 7.4 [Vaccination Administration](#74-vaccination-administration)
   - 7.5 [Appointment Booking & Lifecycle](#75-appointment-booking--lifecycle)
   - 7.6 [Consent Recording & Revocation](#76-consent-recording--revocation)
   - 7.7 [Government Service Request → Document Upload → Approval](#77-government-service-request--document-upload--approval)
   - 7.8 [District Report Generation](#78-district-report-generation)
   - 7.9 [SMS Notification Queue](#79-sms-notification-queue)
   - 7.10 [Gov Sync Outbox Monitoring](#710-gov-sync-outbox-monitoring)
8. [API Endpoint Reference](#8-api-endpoint-reference)
9. [Public Lookup Endpoints (No Auth)](#9-public-lookup-endpoints-no-auth)
10. [Error Codes & Troubleshooting](#10-error-codes--troubleshooting)
11. [Postman Collection Setup](#11-postman-collection-setup)

---

## 1. Environment Setup

### Required Services

| Service | Default | Notes |
|---------|---------|-------|
| PostgreSQL | `localhost:5432` | DB name: `Motherhood Journey DB` |
| Redis | `localhost:6379` | Refresh-token store |
| Java 21+ | — | Spring Boot 3.x |

### Environment Variables

Create a `.env` file or export these before starting:

```bash
# Database
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME="Motherhood Journey DB"
export DB_USERNAME=your_pg_user
export DB_PASSWORD=your_pg_password

# JWT (any random 64-char string for local testing)
export JWT_SECRET=localDevSecretKeyThatIsAtLeast64CharactersLongForHS512Algorithm00

# Africa's Talking SMS (use sandbox for testing)
export AT_API_KEY=sandbox_key_here
export AT_USERNAME=sandbox
export AT_SENDER_ID=

# Government APIs (left empty — use mock profile instead)
export IREMBO_BASE_URL=
export IREMBO_API_KEY=
export HMIS_BASE_URL=
export HMIS_API_KEY=
export NIDA_BASE_URL=
```

> **For local development always add the `mock` profile** — this activates embedded stubs for NIDA, Irembo, and HMIS so no real government credentials are needed.

---

## 2. Running the Application

```bash
# Standard local + mock government APIs
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,mock

# Or with explicit env file
set -a && source .env && set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,mock
```

After startup, Flyway runs all migrations including V27 which seeds the named test accounts and five facilities.

**Verify the application is up:**
```bash
curl http://localhost:8080/actuator/health
# → {"status":"UP"}
```

**Swagger UI:** `http://localhost:8080/swagger-ui.html`  
**OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

---

## 3. Seed Accounts Quick Reference

All accounts use password **`Test@1234`**. Login field is `phoneNumber`.

| Role | Phone | National ID | Name | Facility |
|------|-------|-------------|------|----------|
| `MOH_ADMIN` | +250788900001 | 1198001000000001 | Jean-Baptiste Habimana | Nyarugenge District Hospital |
| `FACILITY_ADMIN` | +250788900002 | 1198501000000002 | Immaculée Uwimana | Nyarugenge District Hospital |
| `FACILITY_ADMIN` | +250788900003 | 1199001000000003 | Patrick Nzeyimana | Gasabo Health Centre |
| `FACILITY_ADMIN` | +250788900004 | 1198701000000004 | Chantal Mukamana | Rwamagana District Hospital |
| `DISTRICT_OFFICER` | +250788900005 | 1197501000000005 | Alain Bizimana | — (scoped to Kigali) |
| `DISTRICT_OFFICER` | +250788900006 | 1197801000000006 | Sylvie Ingabire | — (scoped to Eastern) |
| `HEALTH_WORKER` | +250788900007 | 1199501000000007 | Eugenie Mukeshimana | Nyarugenge District Hospital |
| `HEALTH_WORKER` | +250788900008 | 1199201000000008 | Clément Hakizimana | Gasabo Health Centre |
| `HEALTH_WORKER` | +250788900009 | 1199801000000009 | Vestine Nyiraneza | Kicukiro Health Centre |
| `HEALTH_WORKER` | +250788900010 | 1199001000000010 | Janvier Tuyishime | Rwamagana District Hospital |
| `MOTHER` | +250788900011 | 1199601000000011 | Goretti Umurungi | Gasabo HC *(active pregnancy ~8 wks)* |
| `MOTHER` | +250788900012 | 1199501000000012 | Yvette Uwizeyimana | Nyarugenge DH *(delivered 2024)* |
| `MOTHER` | +250788900013 | 1199701000000013 | Pascaline Iradukunda | Rwamagana DH *(no pregnancy)* |

---

## 4. Seed Facilities Quick Reference

| Facility Code | Name | Type | District | Phone |
|--------------|------|------|----------|-------|
| KGL-NYR-DH-001 | Nyarugenge District Hospital | DISTRICT_HOSPITAL | Nyarugenge | +250788100001 |
| KGL-GSB-HC-001 | Gasabo Health Centre | HEALTH_CENTER | Gasabo | +250788100002 |
| KGL-KCK-HC-001 | Kicukiro Health Centre | HEALTH_CENTER | Kicukiro | +250788100003 |
| EST-RWM-DH-001 | Rwamagana District Hospital | DISTRICT_HOSPITAL | Rwamagana | +250788100004 |
| SOU-HUY-DH-001 | Huye District Hospital | DISTRICT_HOSPITAL | Huye | +250788100005 |

> **Get facility IDs** (needed for most requests): `GET /api/v1/facilities` — no auth required to browse.

---

## 5. Authentication — Login & Token Flow

### 5.1 Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "phoneNumber": "+250788900001",
  "password": "Test@1234"
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "role": "MOH_ADMIN"
}
```

Use the `accessToken` as `Authorization: Bearer <token>` on all subsequent requests. Tokens expire after **24 hours**; refresh tokens expire after **7 days**.

### 5.2 Refresh Token

```http
POST /api/v1/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGci..."
}
```

Returns a new `accessToken` + rotated `refreshToken`. Reusing a consumed refresh token revokes the entire token family.

### 5.3 Logout

```http
POST /api/v1/auth/logout
Content-Type: application/json

{
  "refreshToken": "eyJhbGci..."
}
```

### 5.4 Account Lockout

After **5 consecutive failed logins** the account locks for **15 minutes**. Use a correct login to test recovery after the window.

---

## 6. Role-by-Role Testing Paths

### 6.1 MOH_ADMIN — System Administrator

**Login:** +250788900001 / Test@1234  
**Scope:** Unrestricted — all facilities, all data.

| What to test | Endpoint |
|---|---|
| Platform dashboard | `GET /api/v1/admin/dashboard` |
| List all users (paginated) | `GET /api/v1/admin/users?page=0&size=20` |
| Deactivate a user | `PATCH /api/v1/admin/users/{userId}/deactivate` |
| Reactivate a user | `PATCH /api/v1/admin/users/{userId}/activate` |
| Create a facility | `POST /api/v1/facilities` |
| List all facilities | `GET /api/v1/facilities` |
| View facility analytics | `GET /api/v1/facilities/{facilityId}/analytics` |
| List government users | `GET /api/v1/government` |
| Update geo scope for district officer | `PATCH /api/v1/government/{govUserId}/scope` |
| View sync outbox status | `GET /api/v1/admin/gov-sync/status` |
| Retry a dead-letter sync | `POST /api/v1/admin/gov-sync/{syncId}/retry` |
| Generate a national report | `POST /api/v1/gov-reports` |
| Send an SMS notification | `POST /api/v1/notifications/send` |
| Trigger notification queue | `POST /api/v1/notifications/process-queue` |

**Sample — Dashboard:**
```bash
TOKEN="<MOH_ADMIN token>"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/admin/dashboard
```

---

### 6.2 FACILITY_ADMIN — Facility Manager

**Logins:**
- Nyarugenge: +250788900002 / Test@1234
- Gasabo: +250788900003 / Test@1234
- Rwamagana: +250788900004 / Test@1234

**Scope:** Own facility only (DISTRICT_OFFICER/MOH_ADMIN can override scope).

| What to test | Endpoint |
|---|---|
| View own profile | `GET /api/v1/me/` |
| Update a user in my facility | `PATCH /api/v1/users/{userId}` |
| View facility details | `GET /api/v1/facilities/{facilityId}` |
| Update facility info | `PUT /api/v1/facilities/{facilityId}` |
| View facility analytics | `GET /api/v1/facilities/{facilityId}/analytics` |
| List mothers in facility | `GET /api/v1/mothers/pending-nida` |
| Create a health visit | `POST /api/v1/health-visits` |
| List visits by facility | `GET /api/v1/health-visits/by-facility/{facilityId}` |
| Create an appointment | `POST /api/v1/appointments` |
| Approve a service request | `PATCH /api/v1/service-requests/{id}/approve` |
| Reject a service request | `PATCH /api/v1/service-requests/{id}/reject?reason=Missing+documents` |
| View sync log | `GET /api/v1/admin/gov-sync` |
| Send notification | `POST /api/v1/notifications/send` |

---

### 6.3 DISTRICT_OFFICER — Government Inspector

**Logins:**
- Kigali City scope: +250788900005 / Test@1234
- Eastern Province scope: +250788900006 / Test@1234

**Scope:** Read-only across scoped geo regions. No clinical data modification.

| What to test | Endpoint |
|---|---|
| View own profile | `GET /api/v1/me/` |
| View a mother (any facility in scope) | `GET /api/v1/mothers/{motherId}` |
| View pregnancies by mother | `GET /api/v1/pregnancies/by-mother/{motherId}` |
| View health visits by facility | `GET /api/v1/health-visits/by-facility/{facilityId}` |
| View children by facility | `GET /api/v1/children/by-facility/{facilityId}` |
| View vaccination records | `GET /api/v1/vaccinations/by-child/{childId}` |
| View appointments | `GET /api/v1/appointments/patient/{patientRefId}?patientType=MOTHER` |
| View service requests by status | `GET /api/v1/service-requests/by-status?status=PENDING` |
| Generate a district report | `POST /api/v1/gov-reports` |
| View own government user record | `GET /api/v1/government/by-user/{userId}` |

**Try scope enforcement:** Alain Bizimana (+250788900005) is scoped to Kigali. Attempting to access Rwamagana data should be denied.

---

### 6.4 HEALTH_WORKER — Clinical Staff

**Logins:**
- Nyarugenge: +250788900007 (Eugenie Mukeshimana)
- Gasabo: +250788900008 (Clément Hakizimana)
- Kicukiro: +250788900009 (Vestine Nyiraneza)
- Rwamagana: +250788900010 (Janvier Tuyishime)

**Scope:** Own facility only.

| What to test | Endpoint |
|---|---|
| View own profile | `GET /api/v1/me/` |
| Register a new mother | `POST /api/v1/mothers` |
| Look up mother by health ID | `GET /api/v1/mothers/health/{healthId}` |
| View pending NIDA mothers | `GET /api/v1/mothers/pending-nida` |
| Open a pregnancy | `POST /api/v1/pregnancies` |
| Update pregnancy status | `PATCH /api/v1/pregnancies/{id}?facilityId={facilityId}` |
| Record ANC health visit | `POST /api/v1/health-visits` |
| Book appointment | `POST /api/v1/appointments` |
| Register a newborn child | `POST /api/v1/children` |
| View child vaccination schedule | `GET /api/v1/vaccinations/by-child/{childId}` |
| Administer a vaccine | `PATCH /api/v1/vaccinations/{vaccinationId}/administer?facilityId={facilityId}` |
| Record consent | `POST /api/v1/consents` |
| Submit service request | `POST /api/v1/service-requests` |

---

### 6.5 MOTHER — Patient Self-Service

**Logins:**
- Goretti (active pregnancy): +250788900011
- Yvette (delivered): +250788900012
- Pascaline (no pregnancy): +250788900013

**Scope:** Own records only.

| What to test | Endpoint |
|---|---|
| View own profile | `GET /api/v1/me/` |
| Look up self by health ID | `GET /api/v1/mothers/health/MH-2026-000001` |
| Submit a service request | `POST /api/v1/service-requests` |
| Upload a document to a request | `POST /api/v1/service-requests/{id}/documents` |

---

## 7. End-to-End Journeys

Each journey below is a sequence of API calls. Substitute `$TOKEN`, `$FACILITY_ID`, etc. with real values. Use `GET /api/v1/facilities` to resolve facility IDs first.

---

### 7.1 New Mother Registration → NIDA Verification

**Actor:** HEALTH_WORKER (e.g., Eugenie Mukeshimana at Nyarugenge)

**Step 1 — Resolve a geo location ID**
```bash
curl "http://localhost:8080/api/v1/geo/resolve?province=Kigali+City&district=Nyarugenge&sector=Gitega&cell=Akabahizi&village=Iterambere"
# → { "id": "<geo-uuid>" }
GEO_ID="<geo-uuid>"
```

**Step 2 — Get facility ID**
```bash
curl "http://localhost:8080/api/v1/facilities?district=Nyarugenge"
FAC_ID="<KGL-NYR-DH-001 uuid>"
```

**Step 3 — Register a new user account for the mother**
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "phoneNumber": "+250789000100",
    "nationalId": "1200001010000100",
    "password": "Test@1234",
    "firstName": "Aline",
    "lastName": "Uwamahoro",
    "role": "MOTHER",
    "geoLocationId": "'$GEO_ID'",
    "facilityId": "'$FAC_ID'"
  }'
# → { "id": "<user-uuid>", ... }
USER_ID="<user-uuid>"
```

**Step 4 — Register the mother profile (HEALTH_WORKER token)**
```bash
curl -X POST http://localhost:8080/api/v1/mothers \
  -H "Authorization: Bearer $HW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "'$USER_ID'",
    "facilityId": "'$FAC_ID'",
    "nationalId": "1200001010000100",
    "geoLocationId": "'$GEO_ID'",
    "dateOfBirth": "2000-01-15",
    "educationLevel": "SECONDARY"
  }'
# → { "healthId": "MH-2026-000004", "nidaVerifiedStatus": "PENDING", ... }
MOTHER_ID="<mother-uuid>"
HEALTH_ID="MH-2026-000004"
```

**Step 5 — Check NIDA verification status**

With the `mock` profile active, the NIDA stub automatically processes NIDs starting with `1` as `MATCH`. The async verifier runs shortly after registration.

```bash
curl -H "Authorization: Bearer $HW_TOKEN" \
  http://localhost:8080/api/v1/mothers/health/$HEALTH_ID
# → { "nidaVerifiedStatus": "VERIFIED" }
```

**Step 6 — Check pending NIDA list**
```bash
curl -H "Authorization: Bearer $HW_TOKEN" \
  http://localhost:8080/api/v1/mothers/pending-nida
# → any mothers still in PENDING state
```

---

### 7.2 Antenatal Care (ANC) Visit

**Actor:** HEALTH_WORKER · **Pre-condition:** Mother exists (use Goretti `MH-2026-000001`)

**Step 1 — Get Goretti's mother UUID and pregnancy UUID**
```bash
curl -H "Authorization: Bearer $HW_TOKEN" \
  http://localhost:8080/api/v1/mothers/health/MH-2026-000001
MOTHER_ID="<goretti-mother-uuid>"

curl -H "Authorization: Bearer $HW_TOKEN" \
  "http://localhost:8080/api/v1/pregnancies/by-mother/$MOTHER_ID?facilityId=$FAC_ID"
PREGNANCY_ID="<active-pregnancy-uuid>"
```

**Step 2 — Record an ANC health visit**
```bash
HW_USER_ID="<Eugenie-or-Clément-user-uuid>"   # from GET /api/v1/me/ with HW token

curl -X POST http://localhost:8080/api/v1/health-visits \
  -H "Authorization: Bearer $HW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "patientRefId": "'$MOTHER_ID'",
    "patientType": "MOTHER",
    "facilityId": "'$FAC_ID'",
    "healthWorkerId": "'$HW_USER_ID'",
    "visitDatetime": "2026-06-01T09:00:00",
    "visitType": "ANC",
    "chiefComplaint": "Routine antenatal check - 8 weeks",
    "weightKg": 62.5,
    "heightCm": 160.0,
    "systolicBp": 110,
    "diastolicBp": 70,
    "diagnoses": [
      {
        "icd10Code": "Z34.00",
        "description": "Encounter for supervision of normal first trimester pregnancy",
        "severity": "MILD",
        "isPrimary": true
      }
    ],
    "prescriptions": [
      {
        "medicationName": "Folic Acid",
        "dosage": "5mg",
        "frequency": "Once daily",
        "durationDays": 90,
        "instructions": "Take with food"
      }
    ]
  }'
VISIT_ID="<health-visit-uuid>"
```

**Step 3 — Retrieve the visit**
```bash
curl -H "Authorization: Bearer $HW_TOKEN" \
  "http://localhost:8080/api/v1/health-visits/$VISIT_ID?facilityId=$FAC_ID"
```

**Step 4 — Book next ANC appointment (4 weeks out)**
```bash
curl -X POST http://localhost:8080/api/v1/appointments \
  -H "Authorization: Bearer $HW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "patientRefId": "'$MOTHER_ID'",
    "patientType": "MOTHER",
    "facilityId": "'$FAC_ID'",
    "healthWorkerId": "'$HW_USER_ID'",
    "scheduledAt": "2026-07-01T09:00:00",
    "appointmentType": "ANC",
    "notes": "Second ANC visit — 12 weeks check"
  }'
```

---

### 7.3 Delivery & Child Registration → Vaccination Schedule

**Actor:** HEALTH_WORKER

**Step 1 — Close the pregnancy as DELIVERED**
```bash
curl -X PATCH "http://localhost:8080/api/v1/pregnancies/$PREGNANCY_ID?facilityId=$FAC_ID" \
  -H "Authorization: Bearer $HW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "DELIVERED",
    "outcomeNotes": "Normal vaginal delivery at 39 weeks. Baby girl, 3.2 kg."
  }'
```

**Step 2 — Register the newborn child**

The system auto-generates the full EPI vaccination schedule on child creation.

```bash
curl -X POST http://localhost:8080/api/v1/children \
  -H "Authorization: Bearer $HW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "motherId": "'$MOTHER_ID'",
    "facilityId": "'$FAC_ID'",
    "geoLocationId": "'$GEO_ID'",
    "firstName": "Amahoro",
    "gender": "FEMALE",
    "dateOfBirth": "2026-06-01",
    "birthWeightKg": 3.2,
    "deliveryType": "NORMAL",
    "birthCertificateNo": "RW-2026-NYR-001234"
  }'
CHILD_ID="<child-uuid>"
```

**Step 3 — View auto-generated vaccination schedule**
```bash
curl -H "Authorization: Bearer $HW_TOKEN" \
  "http://localhost:8080/api/v1/vaccinations/by-child/$CHILD_ID?facilityId=$FAC_ID"
# → list of scheduled vaccines with due dates and PENDING status
```

---

### 7.4 Vaccination Administration

**Actor:** HEALTH_WORKER · **Pre-condition:** Child exists with a vaccination schedule

**Step 1 — Get vaccination record IDs** (from the list above)
```bash
VACC_RECORD_ID="<vaccination-record-uuid>"   # pick the BCG (due at birth)
```

**Step 2 — Administer the vaccine**
```bash
curl -X PATCH "http://localhost:8080/api/v1/vaccinations/$VACC_RECORD_ID/administer?facilityId=$FAC_ID" \
  -H "Authorization: Bearer $HW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "administeredById": "'$HW_USER_ID'",
    "administeredDate": "2026-06-01",
    "lotNumber": "BCG-RW-2026-001",
    "notes": "Administered right deltoid. No adverse reaction."
  }'
# → status changes from PENDING to ADMINISTERED
```

**Step 3 — Verify record updated**
```bash
curl -H "Authorization: Bearer $HW_TOKEN" \
  "http://localhost:8080/api/v1/vaccinations/by-child/$CHILD_ID?facilityId=$FAC_ID"
# → BCG record now shows ADMINISTERED with lot number and date
```

---

### 7.5 Appointment Booking & Lifecycle

**Actor:** HEALTH_WORKER

**Step 1 — Create appointment**
```bash
curl -X POST http://localhost:8080/api/v1/appointments \
  -H "Authorization: Bearer $HW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "patientRefId": "'$MOTHER_ID'",
    "patientType": "MOTHER",
    "facilityId": "'$FAC_ID'",
    "scheduledAt": "2026-06-15T10:30:00",
    "appointmentType": "ANC",
    "notes": "Third ANC visit"
  }'
APPT_ID="<appointment-uuid>"
```

**Step 2 — Confirm appointment**
```bash
curl -X PUT "http://localhost:8080/api/v1/appointments/$APPT_ID?facilityId=$FAC_ID" \
  -H "Authorization: Bearer $HW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "status": "CONFIRMED" }'
```

**Step 3 — Cancel with reason**
```bash
curl -X PUT "http://localhost:8080/api/v1/appointments/$APPT_ID?facilityId=$FAC_ID" \
  -H "Authorization: Bearer $HW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ "status": "CANCELLED", "notes": "Mother rescheduled to next week" }'
```

**Step 4 — List all appointments for a patient**
```bash
curl -H "Authorization: Bearer $HW_TOKEN" \
  "http://localhost:8080/api/v1/appointments/patient/$MOTHER_ID?patientType=MOTHER&facilityId=$FAC_ID"
```

---

### 7.6 Consent Recording & Revocation

**Actor:** HEALTH_WORKER or FACILITY_ADMIN

**Step 1 — Record data-sharing consent**
```bash
curl -X POST http://localhost:8080/api/v1/consents \
  -H "Authorization: Bearer $HW_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "motherId": "'$MOTHER_ID'",
    "consentType": "DATA_SHARING",
    "granted": true,
    "grantedByRole": "HEALTH_WORKER",
    "legalBasis": "LAW_058_2021",
    "expiresAt": "2027-06-01T00:00:00"
  }'
CONSENT_ID="<consent-uuid>"
```

**Step 2 — View consent record**
```bash
curl -H "Authorization: Bearer $HW_TOKEN" \
  "http://localhost:8080/api/v1/consents/$CONSENT_ID?facilityId=$FAC_ID"
```

**Step 3 — Revoke consent**
```bash
curl -X PATCH "http://localhost:8080/api/v1/consents/$CONSENT_ID/revoke?facilityId=$FAC_ID" \
  -H "Authorization: Bearer $HW_TOKEN"
# → revokedAt is now set
```

**Step 4 — List all consents for a mother**
```bash
curl -H "Authorization: Bearer $HW_TOKEN" \
  "http://localhost:8080/api/v1/consents/by-mother/$MOTHER_ID?facilityId=$FAC_ID"
```

---

### 7.7 Government Service Request → Document Upload → Approval

**Actor:** MOTHER submits → FACILITY_ADMIN approves

**Step 1 — Login as Goretti (MOTHER)**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"+250788900011","password":"Test@1234"}'
MOM_TOKEN="<access-token>"
```

**Step 2 — Submit a birth certificate service request**
```bash
GORETTI_FAC_ID="<KGL-GSB-HC-001 uuid>"

curl -X POST http://localhost:8080/api/v1/service-requests \
  -H "Authorization: Bearer $MOM_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "serviceType": "BIRTH_CERTIFICATE",
    "facilityId": "'$GORETTI_FAC_ID'",
    "geoLocationId": "'$GEO_ID'",
    "payload": {
      "childName": "Amahoro Umurungi",
      "dateOfBirth": "2026-06-01",
      "placeOfBirth": "Gasabo Health Centre"
    }
  }'
SR_ID="<service-request-uuid>"
```

**Step 3 — Upload a supporting document (multipart)**
```bash
curl -X POST "http://localhost:8080/api/v1/service-requests/$SR_ID/documents" \
  -H "Authorization: Bearer $MOM_TOKEN" \
  -F "documentType=NATIONAL_ID" \
  -F "file=@/path/to/id_scan.pdf"
```

**Step 4 — FACILITY_ADMIN reviews**
```bash
curl -H "Authorization: Bearer $FADM_TOKEN" \
  http://localhost:8080/api/v1/service-requests/$SR_ID
```

**Step 5 — FACILITY_ADMIN approves**
```bash
curl -X PATCH "http://localhost:8080/api/v1/service-requests/$SR_ID/approve" \
  -H "Authorization: Bearer $FADM_TOKEN"
# → status: APPROVED
```

**Step 5b — Or reject with reason**
```bash
curl -X PATCH "http://localhost:8080/api/v1/service-requests/$SR_ID/reject?reason=Incomplete+documentation" \
  -H "Authorization: Bearer $FADM_TOKEN"
```

---

### 7.8 District Report Generation

**Actor:** DISTRICT_OFFICER or MOH_ADMIN

**Step 1 — Login as Alain Bizimana (DISTRICT_OFFICER)**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"phoneNumber":"+250788900005","password":"Test@1234"}'
DO_TOKEN="<access-token>"
```

**Step 2 — Get a Kigali geo location ID**
```bash
curl "http://localhost:8080/api/v1/geo/resolve?province=Kigali+City&district=Nyarugenge&sector=Gitega&cell=Akabahizi&village=Iterambere"
KIGALI_GEO="<uuid>"
```

**Step 3 — Generate a vaccination coverage report**
```bash
curl -X POST http://localhost:8080/api/v1/gov-reports \
  -H "Authorization: Bearer $DO_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reportType": "VACCINATION_COVERAGE",
    "period": "2026-Q2",
    "scopeLevel": "DISTRICT",
    "geoLocationId": "'$KIGALI_GEO'",
    "aggregates": {
      "totalChildren": 120,
      "fullyVaccinated": 98,
      "partiallyVaccinated": 15,
      "unvaccinated": 7
    }
  }'
REPORT_ID="<report-uuid>"
```

**Step 4 — Retrieve the report**
```bash
curl -H "Authorization: Bearer $DO_TOKEN" \
  http://localhost:8080/api/v1/gov-reports/$REPORT_ID
```

---

### 7.9 SMS Notification Queue

**Actor:** MOH_ADMIN or FACILITY_ADMIN

**Step 1 — Enqueue an SMS**
```bash
# Get Goretti's user ID first
GORETTI_USER_ID="<goretti-user-uuid>"

curl -X POST http://localhost:8080/api/v1/notifications/send \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "recipientUserId": "'$GORETTI_USER_ID'",
    "phoneNumber": "+250788900011",
    "message": "Reminder: Your next ANC visit is scheduled for July 1st at Gasabo Health Centre.",
    "notificationType": "APPOINTMENT_REMINDER",
    "scheduledAt": "2026-06-30T08:00:00"
  }'
```

**Step 2 — Manually trigger the queue processor**
```bash
curl -X POST http://localhost:8080/api/v1/notifications/process-queue \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# → processes queued SMS messages via Africa's Talking
```

> In sandbox mode Africa's Talking accepts but does not deliver real SMS. Check the AT dashboard for delivery receipts.

---

### 7.10 Gov Sync Outbox Monitoring

**Actor:** MOH_ADMIN or FACILITY_ADMIN

**Check outbox counters:**
```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/v1/admin/gov-sync/status
# → { "PENDING": 3, "IN_FLIGHT": 0, "SUCCEEDED": 47, "DEAD_LETTER": 1 }
```

**List sync log:**
```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  "http://localhost:8080/api/v1/admin/gov-sync?page=0&size=25"
```

**View dead-letters:**
```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" \
  http://localhost:8080/api/v1/admin/gov-sync/dead-letter
```

**Retry a failed entry (MOH_ADMIN only):**
```bash
curl -X POST "http://localhost:8080/api/v1/admin/gov-sync/$SYNC_ID/retry" \
  -H "Authorization: Bearer $MOH_TOKEN"
```

---

## 8. API Endpoint Reference

### Authentication (`/api/v1/auth`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/login` | Public | Login with phoneNumber + password |
| POST | `/refresh` | Public | Rotate refresh token |
| POST | `/logout` | Public | Revoke refresh token |
| POST | `/register` | Public | Create new user account |

### Users (`/api/v1/users`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/{id}` | FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | Get user by ID |
| PATCH | `/{id}` | FACILITY_ADMIN, MOH_ADMIN | Update user profile |

### Me (`/api/v1/me`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/` | Any authenticated | Current user profile |

### Admin (`/api/v1/admin`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/dashboard` | MOH_ADMIN | Platform counters |
| GET | `/users` | MOH_ADMIN | List all users |
| PATCH | `/users/{id}/deactivate` | MOH_ADMIN | Deactivate user |
| PATCH | `/users/{id}/activate` | MOH_ADMIN | Activate user |

### Facilities (`/api/v1/facilities`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/` | MOH_ADMIN | Create facility |
| GET | `/` | Any authenticated | List facilities (filter: `?district=&facilityType=`) |
| GET | `/{id}` | Any authenticated | Get facility |
| PUT | `/{id}` | FACILITY_ADMIN, MOH_ADMIN | Update facility |
| DELETE | `/{id}` | MOH_ADMIN | Delete facility |
| GET | `/{id}/analytics` | FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | Facility analytics |

### Geo (`/api/v1/geo`) — All Public

| Method | Path | Description |
|--------|------|-------------|
| GET | `/provinces` | All provinces |
| GET | `/districts?province=` | Districts in a province |
| GET | `/sectors?province=&district=` | Sectors in a district |
| GET | `/cells?province=&district=&sector=` | Cells in a sector |
| GET | `/villages?province=&district=&sector=&cell=` | Villages in a cell |
| GET | `/resolve?province=&district=&sector=&cell=&village=` | Resolve to UUID |
| GET | `/{id}/summary` | Geo summary by UUID |

### Mothers (`/api/v1/mothers`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/` | HEALTH_WORKER, FACILITY_ADMIN | Register mother |
| GET | `/health/{healthId}` | Public | Lookup by health ID |
| GET | `/{id}` | HEALTH_WORKER, FACILITY_ADMIN, DISTRICT_OFFICER, MOH_ADMIN | Get mother |
| GET | `/pending-nida` | HEALTH_WORKER, FACILITY_ADMIN | Pending NIDA list |

### Pregnancies (`/api/v1/pregnancies`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Open pregnancy |
| GET | `/{id}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | Get pregnancy |
| GET | `/by-mother/{motherId}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | Pregnancies by mother |
| PATCH | `/{id}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Update pregnancy |

**Pregnancy status transitions:** `ACTIVE → DELIVERED | LOST | TRANSFERRED` (terminal; cannot reopen)

### Health Visits (`/api/v1/health-visits`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Record visit |
| GET | `/{id}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | Get visit |
| GET | `/by-facility/{facilityId}` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | List by facility |
| GET | `/by-patient?patientRefId=&patientType=&facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | List by patient |
| PATCH | `/{id}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Update visit |

**patientType values:** `MOTHER`, `CHILD`

### Appointments (`/api/v1/appointments`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Create appointment |
| PUT | `/{id}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Update appointment |
| GET | `/{id}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, DISTRICT_OFFICER, MOH_ADMIN | Get appointment |
| GET | `/patient/{patientRefId}?patientType=&facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, DISTRICT_OFFICER, MOH_ADMIN | List by patient |

**Appointment status values:** `SCHEDULED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `NO_SHOW`

### Children (`/api/v1/children`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Register child (auto-creates EPI schedule) |
| GET | `/{id}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | Get child |
| GET | `/by-mother/{motherId}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | Children by mother |
| GET | `/by-facility/{facilityId}` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | Children by facility |
| PATCH | `/{id}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Update child |

### Vaccinations (`/api/v1/vaccinations`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/by-child/{childId}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | Vaccination schedule |
| PATCH | `/{id}/administer?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Administer vaccine |

### Consents (`/api/v1/consents`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Record consent |
| GET | `/{id}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Get consent |
| GET | `/by-mother/{motherId}?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | List by mother |
| PATCH | `/{id}/revoke?facilityId=` | HEALTH_WORKER, FACILITY_ADMIN, MOH_ADMIN | Revoke consent |

### Government Users (`/api/v1/government`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/` | MOH_ADMIN | Create gov user |
| GET | `/` | MOH_ADMIN | List all gov users |
| GET | `/{id}` | MOH_ADMIN, DISTRICT_OFFICER | Get gov user |
| GET | `/by-user/{userId}` | MOH_ADMIN, DISTRICT_OFFICER | Get by user ID |
| PATCH | `/{id}/scope` | MOH_ADMIN | Update geo scope |
| DELETE | `/{id}` | MOH_ADMIN | Deactivate gov user |

### Service Requests (`/api/v1/service-requests`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/` | Any authenticated | Submit request |
| GET | `/{id}` | FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | Get request |
| GET | `/by-facility/{facilityId}` | FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | By facility |
| GET | `/by-status?status=` | MOH_ADMIN, DISTRICT_OFFICER | Filter by status |
| PATCH | `/{id}/approve` | FACILITY_ADMIN, MOH_ADMIN | Approve |
| PATCH | `/{id}/reject?reason=` | FACILITY_ADMIN, MOH_ADMIN | Reject |

**Service request status values:** `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`

### Service Request Documents (`/api/v1/service-requests/{requestId}/documents`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/` | Any authenticated | Upload document (multipart, max 10 MB) |
| GET | `/` | FACILITY_ADMIN, MOH_ADMIN, DISTRICT_OFFICER | List documents |

### Government Reports (`/api/v1/gov-reports`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/` | MOH_ADMIN, DISTRICT_OFFICER | Generate report |
| GET | `/{id}` | MOH_ADMIN, DISTRICT_OFFICER | Get report |
| GET | `/by-user/{userId}` | MOH_ADMIN, DISTRICT_OFFICER | Reports by user |

### Gov Sync (`/api/v1/admin/gov-sync`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| GET | `/status` | MOH_ADMIN, FACILITY_ADMIN | Outbox counters |
| GET | `/` | MOH_ADMIN, FACILITY_ADMIN | Sync log (filter: `?status=&targetSystem=`) |
| GET | `/dead-letter` | MOH_ADMIN, FACILITY_ADMIN | Dead-letter entries |
| POST | `/{id}/retry` | MOH_ADMIN | Retry a failed entry |

### Notifications (`/api/v1/notifications`)

| Method | Path | Roles | Description |
|--------|------|-------|-------------|
| POST | `/send` | MOH_ADMIN, FACILITY_ADMIN | Enqueue SMS |
| POST | `/process-queue` | MOH_ADMIN | Trigger queue processor |

---

## 9. Public Lookup Endpoints (No Auth)

These require no `Authorization` header:

```bash
# Health check
GET /actuator/health

# Geo hierarchy browsing
GET /api/v1/geo/provinces
GET /api/v1/geo/districts?province=Kigali+City
GET /api/v1/geo/sectors?province=Kigali+City&district=Nyarugenge
GET /api/v1/geo/cells?province=Kigali+City&district=Nyarugenge&sector=Gitega
GET /api/v1/geo/villages?province=Kigali+City&district=Nyarugenge&sector=Gitega&cell=Akabahizi
GET /api/v1/geo/resolve?province=Kigali+City&district=Nyarugenge&sector=Gitega&cell=Akabahizi&village=Iterambere
GET /api/v1/geo/{id}/summary

# Mother lookup by health ID
GET /api/v1/mothers/health/MH-2026-000001

# Swagger UI
GET /swagger-ui.html
GET /v3/api-docs
```

---

## 10. Error Codes & Troubleshooting

| HTTP | When | Fix |
|------|------|-----|
| 401 | Missing or expired token | Re-login and get a new access token |
| 403 | Role not permitted | Check the role in Section 8 for that endpoint |
| 403 | Facility scope violation | HEALTH_WORKER/FACILITY_ADMIN tried to access another facility's data |
| 400 | Validation failure | Check required fields; see Swagger for constraints |
| 409 | Duplicate entry | NID, phone, or facility code already exists |
| 409 | Active pregnancy exists | Cannot open a second ACTIVE pregnancy for the same mother |
| 423 | Account locked | Too many failed logins; wait 15 minutes |
| 404 | Not found | Wrong ID or resource doesn't exist |

**Common mistakes:**
- Forgetting `?facilityId=<uuid>` on facility-scoped GET/PATCH endpoints
- Using `patientType` values other than `MOTHER` or `CHILD`
- Scheduling appointments in the past (`scheduledAt` must be a future datetime)
- Sending a national ID that doesn't start with `1` when using the mock NIDA profile (IDs starting with `3` return PARTIAL_MATCH; other prefixes may return NO_MATCH)

---

## 11. Postman Collection Setup

### Environment Variables

Create a Postman environment with these variables:

```
BASE_URL      = http://localhost:8080
ACCESS_TOKEN  = (filled after login)
REFRESH_TOKEN = (filled after login)
FACILITY_ID   = (fill from GET /api/v1/facilities)
GEO_ID        = (fill from GET /api/v1/geo/resolve)
MOTHER_ID     = (fill after registering or from seed data)
CHILD_ID      = (fill after registering child)
PREGNANCY_ID  = (fill after opening pregnancy)
HW_USER_ID    = (fill from GET /api/v1/me/ as HEALTH_WORKER)
```

### Auto-extract Token Script (Tests tab on login request)

```javascript
const json = pm.response.json();
pm.environment.set("ACCESS_TOKEN",  json.accessToken);
pm.environment.set("REFRESH_TOKEN", json.refreshToken);
```

### Authorization Header (all protected requests)

```
Type: Bearer Token
Token: {{ACCESS_TOKEN}}
```

### Quick Login Requests

Add these as saved requests for each role:

| Name | Phone |
|------|-------|
| Login MOH_ADMIN | +250788900001 |
| Login FACILITY_ADMIN (Nyarugenge) | +250788900002 |
| Login FACILITY_ADMIN (Gasabo) | +250788900003 |
| Login DISTRICT_OFFICER (Kigali) | +250788900005 |
| Login HEALTH_WORKER (Nyarugenge) | +250788900007 |
| Login HEALTH_WORKER (Gasabo) | +250788900008 |
| Login MOTHER (Goretti - active pregnancy) | +250788900011 |
| Login MOTHER (Yvette - delivered) | +250788900012 |
| Login MOTHER (Pascaline - no pregnancy) | +250788900013 |

All use body: `{"phoneNumber":"<phone>","password":"Test@1234"}`

---

## Mock Government API Stubs

When running with `--spring.profiles.active=local,mock`, three government stubs are embedded at:

| Stub | Base Path | Behaviour |
|------|-----------|-----------|
| NIDA | `/mock/nida` | NID starts with `1` → MATCH; `3` → PARTIAL_MATCH; other → NO_MATCH |
| Irembo | `/mock/irembo` | Accepts any service request, returns ticket ID |
| HMIS | `/mock/hmis` | Accepts any HMIS push, returns success |

These stubs are used automatically when the `mock` profile is active — no configuration change is needed.

---

*End of Testing Guide — MotherHood Journey Backend v27*
