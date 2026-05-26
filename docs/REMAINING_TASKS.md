# MotherHood Journey — Remaining Tasks

> **Reference:** `docs/motherhood-journey-sprint-plan.md` (9 engineers · 5 sprints · 313 pts)  
> **Assessed:** 2026-05-25 against live codebase and endpoint tests  
> **Updated:** 2026-05-25 — all Priority 0 bugs applied and verified (77/77 tests pass)  
> **Legend:** ✅ Done · ⚠️ Partial · ❌ Not started

---

## Sprint Completion Status

| Sprint | Focus | BE Status | FE Status | Notes |
|--------|-------|-----------|-----------|-------|
| Sprint 1 | Foundation & Authentication | ✅ ~90% | ❌ 0% | Refresh token endpoint missing |
| Sprint 2 | Mother Registration & Pregnancy | ✅ ~85% | ❌ 0% | Bugs #3 & #4 block full completion |
| Sprint 3 | Children, Vaccinations & Visits | ✅ ~90% | ❌ 0% | Unit tests missing |
| Sprint 4 | Appointments, Service Requests & Facility Admin | ⚠️ ~70% | ❌ 0% | Facility stats endpoint missing |
| Sprint 5 | Government Portal, Reporting & Hardening | ⚠️ ~50% | ❌ 0% | HMIS push, geo-RBAC, E2E tests missing |

---

## Priority 0 — Critical Bug Fixes ✅ All Applied

All four critical bugs and four secondary issues have been fixed. See `docs/BUGS_AND_FIXES.md`
for root cause analysis and exact code changes. `./mvnw test` reports 77/77 tests passing.

| # | Bug | Status |
|---|-----|--------|
| B-1 | `POST /api/v1/mothers` → 500 (missing `seq_mother_health_id` sequence) | ✅ Fixed — V12 migration added |
| B-2 | `SecurityConfig` blocks MOH_ADMIN / DISTRICT_OFFICER | ✅ Fixed — matchers widened to `.authenticated()` |
| B-3 | `GET /api/v1/mothers/{id}` → 500 (LazyInitializationException) | ✅ Fixed — caller re-fetched in transaction |
| B-4 | `GET /api/v1/admin/dashboard` → 500 (`countByStatus` type mismatch) | ✅ Fixed — enum parameter type corrected |
| B-5 | `GET /api/v1/mothers/pending-nida` → 500 (same type mismatch for NidaVerifiedStatus) | ✅ Fixed — same pattern applied |
| B-6 | Test suite fails under Java 21+ (Mockito inline mocking blocked) | ✅ Fixed — subclass mock maker configured |
| B-7 | VaccinationServiceTest in wrong directory / stale API | ✅ Fixed — moved and rewritten |
| B-8 | Duplicate `@Query` import in UserRepository | ✅ Fixed — removed |

---

## Sprint 1 — Remaining Backend Tasks

### ❌ POST /api/v1/auth/refresh — Refresh token endpoint

The sprint plan specifies `POST /auth/refresh` for issuing a new access token from a valid refresh
token. This endpoint does not exist. Only `POST /api/v1/auth/login` is implemented.

**Owner:** BE3  
**File to create/update:** `identity/controller/AuthController.java`, `identity/service/AuthService.java`  
**What to implement:**
- Validate the submitted refresh token (signature + expiry + stored state)
- Issue a new 15-minute access token
- Return `TokenResponse` with new access token (and optionally a rotated refresh token)

### ⚠️ GitHub Actions CI pipeline

No `.github/workflows/` directory was found. The CI pipeline described in the sprint plan
(compile → test → Checkstyle → Jacoco → Railway deploy) has not been set up.

**Owner:** BE1  
**Files to create:**
- `.github/workflows/ci.yml`
- `.github/workflows/deploy.yml`

---

## Sprint 2 — Remaining Backend Tasks

All Sprint 2 features are functionally implemented. Bug fixes B-3 and B-4 above are the only
blockers remaining.

---

## Sprint 3 — Remaining Backend Tasks

### ❌ Unit tests for ChildService, VaccinationService, SmsService

The sprint plan specifies pure Mockito unit tests (no Spring context, no database) covering:

- `ChildServiceTest` — registering a child creates exactly N vaccination records; unknown mother throws exception
- `VaccinationServiceTest` — `markAdministered` sets correct fields; overdue scanner flips correct records; SMS is enqueued per OVERDUE flip
- `SmsServiceTest` — retry count increments; max retries respected

**Owner:** BE3, BE4, BE5  
**Location:** `src/test/java/com/motherhood/journey/child/service/`, `src/test/java/com/motherhood/journey/notification/service/`

---

## Sprint 4 — Remaining Backend Tasks

### ❌ GET /api/v1/facilities/{id}/stats — Facility KPI aggregation endpoint

This endpoint powers the facility admin dashboard (ANC attendance rate, vaccination coverage,
no-show rate, service request backlog). No `FacilityStatsService` was found in the codebase.

**Owner:** BE3  
**Files to create:**
- `facility/service/FacilityStatsService.java`
- `facility/dto/response/FacilityStatsResponse.java`

**What to implement:**
```
GET /api/v1/facilities/{id}/stats
Roles: FACILITY_ADMIN, MOH_ADMIN

Response:
{
  "ancAttendanceRate": 0.82,          // ANC visits this month / active pregnancies
  "vaccinationCoveragePct": 0.74,     // ADMINISTERED records / total records (this facility)
  "noShowRate": 0.12,                 // NO_SHOW appointments / SCHEDULED last 30 days
  "serviceRequestBacklog": 7          // PENDING + UNDER_REVIEW service requests
}
```

All counts must be scoped to `facility_id` from the path variable. Use JPQL aggregate queries,
not `SELECT *` + Java-side filtering.

### ❌ Integration tests — Appointment and ServiceRequest repositories

The sprint plan requires `@DataJpaTest` integration tests using Testcontainers PostgreSQL
covering `AppointmentRepository` and `ServiceRequestRepository` query correctness.

**Owner:** BE4  
**Location:** `src/test/java/com/motherhood/journey/appointment/repository/`, `src/test/java/com/motherhood/journey/government/repository/`

---

## Sprint 5 — Remaining Backend Tasks

### ❌ HmisApiClient — Push gov reports to MoH HMIS

`GovReport.hmisApiPushStatus` exists and the push status enum is defined, but no `HmisApiClient`
that formats and submits reports to the DHIS2-compatible HMIS endpoint was found.

**Owner:** BE1  
**Files to create/update:**
- `government/service/HmisApiClient.java`
- Update `government/service/GovReportService.java` to add `pushToHmis(UUID reportId)`

**What to implement:**
- Format `GovReport.aggregates` (JSONB) into the HMIS API payload schema
- Create a `GovSyncLog` entry with `target_system=HMIS`, `sync_type=REPORT_PUSH`
- Let the existing `GovSyncService` outbox processor execute the actual HTTP call
- Update `GovReport.hmisApiPushStatus` based on sync result

### ❌ Geo-scoped RBAC (RbacUtils.canAccessGeo)

The sprint plan defines `RbacUtils.canAccessGeo(authentication, geoLocationId)` as a reusable
`@Component` whose methods are called from `@PreAuthorize` SpEL expressions. Currently the
`DISTRICT_OFFICER` role is listed in the `government_users` schema but geo-scope enforcement
on patient data reads is not enforced at the service layer.

**Owner:** BE3  
**Files to create:**
- `security/RbacUtils.java` — Spring `@Component` with `canAccessGeo()`, `canAccessFacility()` methods
- Update `@PreAuthorize` annotations on `GovernmentController`, `GovReportController` to use `@RbacUtils.canAccessGeo(...)`

### ⚠️ government_users CRUD for MOH_ADMIN

`GovernmentController` exists and `GET /api/v1/gov-sync-log/{id}` and `GET /api/v1/gov-sync-log/user/{userId}` work.
However, the following are missing:

| Missing endpoint | Description |
|-----------------|-------------|
| `PATCH /api/v1/government-users/{id}/geo-scope` | Replace `scoped_geo_ids` array for a DISTRICT_OFFICER |
| `PATCH /api/v1/government-users/{id}/permissions` | Toggle `can_export` / `can_push_hmis` flags |
| `GET /api/v1/government-users` | List all government users (MOH_ADMIN only) |

**Owner:** BE2

### ❌ DEAD_LETTER alerting for GovSyncService

When `GovSyncService` sets a `GovSyncLog` row to `DEAD_LETTER` after 5 failed retries, it should
call `SmsService.enqueue()` to send an alert to all `MOH_ADMIN` users. This notification path
is described in the sprint plan but was not found in `GovSyncService.java`.

Also missing:
- `GET /api/v1/gov-sync-log` — filterable by `status` and `target_system`
- `POST /api/v1/gov-sync-log/{id}/retry` — manual retry trigger (MOH_ADMIN only)

**Owner:** BE4

### ❌ End-to-end API tests with @SpringBootTest + Testcontainers

The sprint plan requires three full-stack E2E test flows:

1. `MotherRegistrationE2ETest` — register mother → open pregnancy → register child → vaccination schedule auto-created → mark vaccination administered → verify audit_log
2. `ServiceRequestE2ETest` — submit service request → facility admin approves → gov_sync_log entry created → GovSyncService processes → IREMBO_SUBMITTED
3. `GovReportE2ETest` — generate report → push to HMIS → verify `hmis_push_status=PUSHED`

**Owner:** BE1, BE2  
**Location:** `src/test/java/com/motherhood/journey/e2e/`

### ⚠️ OpenAPI / Swagger annotation completeness

The Swagger UI at `http://localhost:8080/swagger-ui/index.html` works, but most endpoints are
missing `@Operation`, `@Parameter`, and `@ApiResponse` annotations. The sprint plan requires
all endpoints to document 200, 400, 401, 403, and 404 responses.

**Owner:** BE3  
**What to add to each controller:** `@Operation(summary = "...")`, `@ApiResponse(responseCode = "400", ...)`, etc.

---

## Frontend — All Sprints (Not Started)

The Next.js frontend is a separate repository and has not been started. Below is a condensed task
list derived from the sprint plan. Frontend engineers should work against the live API documented
in `docs/API_DOCUMENTATION.md`.

### Sprint 1 Frontend
- [ ] Initialise Next.js 14 app router project with TypeScript + Tailwind + shadcn/ui
- [ ] Configure NextAuth with role-aware JWT (extract `role`, `facilityId`, `geoScopeIds` from Spring Boot login response)
- [ ] `middleware.ts` — role-based route guard for six role portals
- [ ] Login page with i18n (Kinyarwanda, English, French) + Zod validation (+250XXXXXXXXX phone format)
- [ ] Shared layout: `Sidebar.tsx`, `TopBar.tsx`, `MobileNav.tsx`, `PageHeader.tsx`
- [ ] `lib/api/client.ts` — typed fetch wrapper with auto-attach JWT
- [ ] `lib/api/geo.ts` — typed wrappers for all five public geo endpoints
- [ ] `lib/rbac.ts` and `useRole()` hook

### Sprint 2 Frontend
- [ ] `GeoLocationSelect` — cascading 5-level Province→Village dropdown (react-query, `enabled` option)
- [ ] Mother registration multi-step form (NID validation, geo select, health_id display on success)
- [ ] Mother search + list page for health workers
- [ ] Mother profile page (NIDA status badge, pregnancy timeline, CHW assignment)
- [ ] Patient dashboard (EDD countdown, children vaccination status pills, next appointment)
- [ ] Patient consent management page (four toggle cards, Rwanda Law disclaimer, revoke warning dialog)
- [ ] `NidaStatusBadge` and `VaccinationStatusPill` shared status components

### Sprint 3 Frontend
- [ ] Newborn registration form (birth weight, delivery type, auto-created vaccination schedule display)
- [ ] Child profile page + vaccination schedule table (colour-coded by status, mark-administered dialog)
- [ ] Patient vaccination card (mobile-first, printable layout, offline service worker cache)
- [ ] Vaccination session page for health workers (child search by health_id, lot number entry)
- [ ] Visit recording 4-step form (patient type → vitals → ICD-10 diagnoses → prescriptions)
- [ ] `useVaccinations` and `useChildren` react-query hooks + centralised `lib/query-keys.ts`

### Sprint 4 Frontend
- [ ] Appointment scheduling form (patient search, date/time picker, available slots)
- [ ] Patient appointments page (chronological list, cancel button, service request FAB)
- [ ] Service request submission form (service type, document upload drag-and-drop, reference number display)
- [ ] `ServiceRequestTracker` — horizontal stepper component (PENDING → COMPLETED lifecycle)
- [ ] Facility admin service request review queue (approve / reject / escalate)
- [ ] Facility admin KPI dashboard (four stat cards + Recharts charts from `/facilities/{id}/stats`)
- [ ] Facility admin staff management (activate / deactivate health workers)
- [ ] Facility admin appointment calendar (week-view grid, colour-coded by type)

### Sprint 5 Frontend
- [ ] National dashboard with Rwanda SVG choropleth map (drill-down from province → district)
- [ ] Report generation form + `useGenerateReport` mutation hook
- [ ] Report detail page with charts + HMIS push button (MOH_ADMIN only)
- [ ] Sync log monitoring page (auto-refresh every 30 s, DEAD_LETTER retry button)
- [ ] Government user management (scoped_geo_ids multi-select, permissions toggles)
- [ ] District officer dashboard (sector-scoped facility stats table, no-show heat-map grid)
- [ ] WCAG 2.1 AA audit + fixes (axe-core automated + keyboard + screen reader tests)
- [ ] Production deployment (Vercel + Railway, Sentry error tracking, Uptime Robot)

---

## Non-Sprint Remaining Work

### Documentation gaps
- [ ] `docs/ARCHITECTURE.md` — currently empty (only a title); needs system architecture overview
- [ ] `docs/CONTRIBUTING.md` — currently empty; needs Git workflow, PR process, code style guide

### Security hardening
- [ ] Rate limiting — Spring gateway: 100 req/min per IP, 1000 req/min per facility (mentioned in DB handout)
- [ ] JWT token rotation documentation — 90-day secret rotation procedure
- [ ] Dependency scanning — Dependabot config (`.github/dependabot.yml`)

### Production deployment
- [ ] Verify Flyway baseline config for production Railway DB
- [ ] Monthly audit_log partition creation for future months (V3 creates partitions for 2026; need ongoing strategy)
- [ ] `application-prod.yml` with HikariCP tuning for Railway's PostgreSQL plan

---

## Quick Reference — Backend Effort Estimates

| Task | Owner | Est. |
|------|-------|------|
| B-1: V12 sequence migration | BE2 | 15 min |
| B-2: SecurityConfig fix | BE1 | 15 min |
| B-3: MotherService lazy load fix | BE3 | 30 min |
| B-4: AppointmentRepository enum fix | BE4 | 20 min |
| POST /auth/refresh | BE3 | 3 hrs |
| GET /api/v1/facilities/{id}/stats | BE3 | 4 hrs |
| HmisApiClient | BE1 | 4 hrs |
| RbacUtils + geo-scope enforcement | BE3 | 4 hrs |
| government_users CRUD endpoints | BE2 | 3 hrs |
| DEAD_LETTER alerting + sync log endpoints | BE4 | 3 hrs |
| Unit tests (Sprint 3) | BE3/BE4/BE5 | 5 hrs |
| Integration tests (Sprint 4) | BE4 | 4 hrs |
| E2E tests (Sprint 5) | BE1/BE2 | 8 hrs |
| OpenAPI annotation sweep | BE3 | 2 hrs |
| ARCHITECTURE.md + CONTRIBUTING.md | BE1 | 2 hrs |
| GitHub Actions CI/CD | BE1 | 3 hrs |