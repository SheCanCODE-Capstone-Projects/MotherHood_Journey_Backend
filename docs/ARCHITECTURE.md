# MotherHood Journey — Architecture

> IgireRwanda Organization | SheCanCode Bootcamp | Kigali, Rwanda

---

## 1. High-Level System Architecture

```
┌────────────────────────────────────────────────────────────────────┐
│                            Clients                                 │
│   Next.js Frontend  │  Mobile Browser  │  Government Portals       │
└──────────┬──────────────────┬─────────────────────┬───────────────┘
           │  HTTPS + JWT     │                     │ Callbacks
┌──────────▼──────────────────▼─────────────────────▼───────────────┐
│               Spring Boot 3.2.5 — REST API (:8080)                 │
│                                                                    │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌─────────────┐  │
│  │  Identity  │  │  Maternal  │  │   Child &  │  │  Government │  │
│  │  & Auth    │  │  & Consent │  │  Vaccines  │  │  Integration│  │
│  └────────────┘  └────────────┘  └────────────┘  └─────────────┘  │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌─────────────┐  │
│  │    Geo     │  │Appointment │  │Notification│  │    Admin    │  │
│  │  Hierarchy │  │ Scheduler  │  │  (SMS)     │  │  Dashboard  │  │
│  └────────────┘  └────────────┘  └────────────┘  └─────────────┘  │
│                                                                    │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  Security: JwtFilter → SecurityConfig → @PreAuthorize        │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  Background: ReminderScheduler (vaccination · appointments · │  │
│  │  gov-sync retry)                                             │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────┬───────────────────────────────────────┘
                             │  JDBC / Flyway V1–V11
┌────────────────────────────▼───────────────────────────────────────┐
│                       PostgreSQL 16                                 │
│  16 tables · geo_locations (14,000+ Rwanda rows)                    │
│  audit_log (monthly partitions) · gov_sync_log (outbox)             │
└────────────────────────────┬───────────────────────────────────────┘
                             │  Async via gov_sync_log outbox
           ┌─────────────────┼──────────────────────┐
           ▼                 ▼                      ▼
      Africa's Talking    NIDA API           MoH HMIS / Irembo
      (SMS delivery)    (ID verify)         (reports / service requests)
```

---

## 2. Backend Architecture

### 2.1 Package Organisation — Domain-First

The codebase uses a **domain-first, then layer** package structure. Each business domain is a self-contained vertical slice.

```
com.motherhood.journey/
├── maternal/       ← Mothers, pregnancies, health visits, diagnoses, prescriptions
├── child/          ← Children, vaccination schedules, vaccination records
├── appointment/    ← Booking, SMS reminders, no-show tracking
├── consent/        ← Data-sharing consent (Rwanda Law No. 058/2021)
├── identity/       ← Users, roles, JWT authentication
├── notification/   ← SMS outbox via Africa's Talking
├── government/     ← Service requests, gov reports, government users
├── geo/            ← Rwanda 5-level administrative hierarchy
├── admin/          ← MOH_ADMIN dashboard aggregations
├── me/             ← Current user profile endpoint
├── common/         ← ApiResponse, GlobalExceptionHandler, DateUtils
├── config/         ← SecurityConfig, CorsConfig, OpenApiConfig
├── security/       ← JwtFilter, JwtUtil, CustomUserDetailsService
└── scheduler/      ← ReminderScheduler (cron jobs)
```

### 2.2 Layered Structure Per Domain

Every domain (except `admin` and `me` which are thin) follows the same internal layer pattern:

| Layer | Responsibility |
|-------|---------------|
| `controller/` | HTTP routing only — no business logic |
| `service/` | Interface + `ServiceImpl` — all business rules |
| `repository/` | JPA repositories — no SQL in controllers |
| `entity/` | JPA-mapped DB tables — never sent directly to clients |
| `dto/request/` | Validated incoming payloads (`@NotBlank`, `@NotNull`) |
| `dto/response/` | Safe outgoing payloads — no sensitive fields |
| `enums/` | Fixed-value fields stored as `VARCHAR` in PostgreSQL |

### 2.3 Standard API Response Shape

Every endpoint returns the same envelope:

```json
{
  "success": true,
  "message": "Resource retrieved",
  "data": { ... }
}
```

Errors return `success: false` with a human-readable `message` and `data: null`. HTTP status codes align with the error type (400, 401, 403, 404, 409, 500).

---

## 3. Frontend Architecture

> The frontend is maintained in a separate repository. This backend is designed to serve it via REST.

**Expected stack:** Next.js · next-auth · @tanstack/react-query · Tailwind CSS · shadcn/ui

The backend enforces CORS via `CorsConfig.java` using the `CORS_ALLOWED_ORIGINS` environment variable. In production this is set to the deployed frontend domain.

---

## 4. Database Architecture

### 4.1 Design Principles

- **UUID primary keys** on all tables (no sequential integer IDs exposed in URLs).
- **Flyway-managed schema** — `ddl-auto: validate` prevents Hibernate from touching the schema.
- **Never duplicate geo data** — all entities reference `geo_locations` via FK, never embed province/district/sector strings.
- **Facility-scoped multi-tenancy** — every patient record, visit, and service request carries a `facility_id`. Cross-facility access requires explicit role elevation (`MOH_ADMIN`, `DISTRICT_OFFICER`).
- **Outbox pattern** for all external API calls — written to `gov_sync_log` before execution, retried with exponential backoff, escalated after 5 failures.
- **Immutable audit log** — `audit_log` is append-only, partitioned monthly, retained 7 years per MoH policy.

### 4.2 Table Groups

| Group | Tables |
|-------|--------|
| Geo & Admin | `geo_locations` |
| Users & Roles | `users`, `government_users` |
| Facilities | `facilities` |
| Mothers & Pregnancies | `mothers`, `pregnancies` |
| Children & Vaccination | `children`, `vaccination_schedules`, `vaccination_records` |
| Clinical Visits | `health_visits`, `diagnoses`, `prescriptions` |
| Appointments | `appointments` |
| Consent | `consent_records` |
| Government Integration | `service_requests`, `service_request_docs`, `gov_sync_log`, `gov_reports` |
| Notifications & Audit | `sms_notifications`, `audit_log` |

Full schema with column definitions and indexes: [`docs/DATABASE_DESIGN.md`](DATABASE_DESIGN.md)

### 4.3 Migration History

| Migration | Description |
|-----------|------------|
| V1 | Full initial schema (all core tables) |
| V2 | Schema fixes and missing constraints |
| V3 | `audit_log` table with monthly partitioning |
| V4 | Additional constraints and partition extensions |
| V5 | Appointment and notification refinements |
| V6 | Service request sequence (`SR-YYYY-NNNNN`) |
| V7 | Performance indexes |
| V8 | Health visit constraints |
| V9 | ICD-10 / HMIS code seed data |
| V10 | Cancellation reason on appointments |
| V11 | `gov_sync_log` missing columns |
| V12 | `seq_mother_health_id` sequence for `MH-YYYY-NNNNNN` health ID generation |

---

## 5. Authentication & Authorization Flow

### 5.1 JWT Authentication

```
Client                JwtFilter              SecurityContext
  │                      │                        │
  │── POST /auth/login ──►│                        │
  │                      │ (public — no filter)    │
  │◄── { accessToken } ──│                        │
  │                      │                        │
  │── GET /api/v1/me ────►│                        │
  │  Authorization:       │                        │
  │  Bearer <token>       │ extract + validate JWT  │
  │                      │──── set Authentication ─►│
  │                      │    (userId, role,        │
  │                      │     facilityId)          │
  │                      │                        │
  │                      │        @PreAuthorize    │
  │                      │        checks role ◄────│
  │◄──── 200 / 403 ───────│                        │
```

Token payload contains: `sub` (userId), `role`, `facilityId`, `exp`.

Signing algorithm: HMAC-SHA-512. Minimum secret length: 32 characters (64+ recommended).

### 5.2 Role Hierarchy & Permissions

| Role | Scope | Key Permissions |
|------|-------|----------------|
| `PATIENT` | Own records | View own profile, own appointments, own children |
| `HEALTH_WORKER` | Own facility | Register mothers/children, record visits, administer vaccines |
| `FACILITY_ADMIN` | Own facility | All HW permissions + approve/reject service requests, manage staff |
| `DISTRICT_OFFICER` | Scoped geo sectors | Read access across facilities in authorized sectors |
| `GOVERNMENT_ANALYST` | National | Generate and view aggregated reports |
| `MOH_ADMIN` | National (full) | All permissions + user management, HMIS push, system dashboard |

### 5.3 Dual-Layer Access Control

Access control is enforced at two levels:

1. **URL-level** (`SecurityConfig.java`) — coarse-grained, determines which paths are reachable.
2. **Method-level** (`@PreAuthorize`) — fine-grained per endpoint, role + facility checks.

Both layers must pass. See [`docs/BUGS_AND_FIXES.md`](BUGS_AND_FIXES.md) Bug #2 for the known conflict between URL matchers and `@PreAuthorize` on mother/child/appointment paths.

### 5.4 Facility Scope Enforcement

Every service method that reads PHI calls `enforceScope(resource, caller)` which compares the resource's `facility_id` against the caller's `facilityId`. Cross-facility access is blocked unless the caller holds `MOH_ADMIN` or `DISTRICT_OFFICER` with the correct `scoped_geo_ids`.

---

## 6. API Structure

**Base path:** `/api/v1`  
**Auth:** `Authorization: Bearer <JWT>` on all non-public endpoints.  
**Pagination:** Standard Spring `Pageable` — `?page=0&size=20&sort=createdAt,desc`

| Path prefix | Domain |
|-------------|--------|
| `/api/v1/auth` | Authentication (public) |
| `/api/v1/geo` | Rwanda geography (public) |
| `/api/v1/me` | Current user profile |
| `/api/v1/users` | User management |
| `/api/v1/admin` | Admin dashboard (MOH_ADMIN) |
| `/api/v1/facilities` | Facility management |
| `/api/v1/mothers` | Mother registration and records |
| `/api/v1/pregnancies` | Pregnancy management |
| `/api/v1/children` | Child registration |
| `/api/v1/vaccinations` | Vaccination records |
| `/api/v1/health-visits` | Clinical visit records |
| `/api/v1/appointments` | Appointment scheduling |
| `/api/v1/consents` | Consent management |
| `/api/v1/service-requests` | Government service requests |
| `/api/v1/gov-reports` | Aggregated health reports |
| `/api/v1/government` | Government user management |
| `/api/v1/notifications` | SMS notification queue |
| `/webhooks/at` | Africa's Talking callbacks (public, signature-validated) |
| `/actuator/health` | Health check (public) |

Full endpoint reference: [`docs/API_DOCUMENTATION.md`](API_DOCUMENTATION.md)

---

## 7. Service / Module Boundaries

Each domain module owns its data. Cross-domain calls go through service interfaces, never direct repository access:

```
appointment/service ──► notification/service   (queue SMS reminder)
government/service  ──► consent/service         (check GOV_DATA_SHARE before HMIS push)
maternal/service    ──► geo/service             (resolve geo_location_id from NID)
child/service       ──► vaccination/service     (seed vaccination records on registration)
scheduler           ──► appointment/repository  (scan upcoming appointments)
scheduler           ──► vaccination/repository  (scan overdue records)
scheduler           ──► gov_sync_log/repository (retry failed outbox entries)
```

---

## 8. Background Jobs (Scheduler)

`ReminderScheduler.java` runs three cron jobs:

| Job | Schedule | Action |
|-----|----------|--------|
| Vaccination OVERDUE scan | Daily 06:00 | Scans `PENDING` vaccination records past `due_date + window_days`, flips to `OVERDUE`, queues SMS |
| Appointment reminder | Hourly | Finds appointments within 24 hours with `reminder_sent=false`, sends SMS, marks sent |
| Gov sync retry | Every 5 minutes | Retries `PENDING`/`FAILED` `gov_sync_log` rows with exponential backoff; marks `DEAD_LETTER` after 5 failures |

---

## 9. Government Integration — Outbox Pattern

All external API calls follow the transactional outbox pattern to ensure exactly-once delivery and full auditability:

```
ServiceImpl                gov_sync_log          External API
    │                           │                    │
    │── write PENDING ─────────►│                    │
    │   (idempotency_key)        │                    │
    │                           │                    │
    │        [scheduler tick]    │                    │
    │                           │── call API ────────►│
    │                           │◄─ 200 OK / 5xx ─────│
    │                           │                    │
    │                           │ status = SUCCEEDED  │
    │                           │   or FAILED         │
    │                           │   (retry_count++)   │
    │                           │   next_retry_at += 2^n minutes
    │                           │                    │
    │                           │ [after 5 retries]  │
    │                           │ status = DEAD_LETTER│
    │                           │ → alert SMS queued  │
```

---

## 10. Deployment Architecture

```
GitHub main branch
       │
       │  push → auto-deploy
       ▼
   Railway
   ┌─────────────────────────────────────┐
   │  App Service                        │
   │  Docker image (3-stage build)       │
   │  Eclipse Temurin 21 JRE Alpine      │
   │  G1GC · MaxRAMPercentage=75%        │
   │  ENTRYPOINT: JarLauncher (exec-form)│
   │  Health check: /actuator/health     │
   └─────────────────┬───────────────────┘
                     │ JDBC (internal hostname)
   ┌─────────────────▼───────────────────┐
   │  PostgreSQL 16 (Railway plugin)     │
   │  Flyway migrations on startup       │
   │  Monthly audit_log partitions       │
   └─────────────────────────────────────┘
```

Docker image build stages:
1. **builder** — Maven + JDK 21 Alpine, downloads dependencies, runs `mvn package -DskipTests`
2. **extractor** — extracts Spring Boot layered JAR into 4 ordered layers
3. **runtime** — JRE Alpine only, non-root `appuser`, copies layers least-to-most volatile for optimal cache

---

## 11. Scalability Considerations

| Concern | Current Approach | Production Path |
|---------|-----------------|----------------|
| Connection pool | HikariCP max 5 | Increase per available DB connections |
| Caching | Caffeine in-process | Redis for multi-instance |
| SMS delivery | AT single-process outbox | Dedicated worker process |
| Audit log volume | Monthly table partitions | Archival to object storage (S3/GCS) after 2 years |
| Gov sync | 5-minute cron poll | Replace with event-driven queue (SQS/RabbitMQ) |
| Read scaling | Single DB | Read replica for gov analysts and report queries |

---

## 12. Security Architecture

| Control | Implementation |
|---------|---------------|
| Authentication | HMAC-SHA-512 JWT, 24h expiry, min 32-char secret |
| Authorisation | URL matchers + `@PreAuthorize` + `enforceScope()` |
| Password storage | BCrypt (Spring Security default) |
| Transport | HTTPS enforced in production (Railway TLS) |
| SQL injection | Parameterised JPA queries only — no string concatenation |
| Rate limiting | Bucket4j — login endpoint rate-limited to prevent brute force |
| PHI audit | Every PHI read/write logged to `audit_log` (immutable) |
| Consent gate | `GOV_DATA_SHARE` consent checked before every HMIS push |
| Secrets | All credentials via environment variables — never in code or committed files |
| Token revocation | Stateless JWT — no server-side state; short expiry is the primary control |
| Webhook integrity | Africa's Talking `X-AT-Signature` HMAC header validated before processing |
| Data retention | Audit log 7 years; other PHI per MoH policy |

---

## 13. Request Lifecycle

```
HTTP Request
    │
    ├─ 1. JwtFilter
    │       ├─ extract Bearer token from Authorization header
    │       ├─ validate signature and expiry (JwtUtil)
    │       └─ set Authentication in SecurityContextHolder
    │
    ├─ 2. SecurityConfig URL matchers
    │       └─ coarse role check (PERMIT_ALL / authenticated / hasAnyRole)
    │
    ├─ 3. Controller method
    │       ├─ @PreAuthorize — fine-grained role check
    │       └─ @Valid — request body validation (Bean Validation)
    │
    ├─ 4. Service layer
    │       ├─ business logic
    │       ├─ enforceScope() — facility boundary check
    │       └─ repository calls (within @Transactional)
    │
    ├─ 5. Repository / Hibernate
    │       └─ parameterised query → PostgreSQL
    │
    ├─ 6. Entity → DTO mapping (service layer)
    │
    └─ 7. GlobalExceptionHandler (if exception thrown)
            └─ maps to ApiResponse with appropriate HTTP status
```

---

## 14. Future Extensibility

| Feature | Integration Point |
|---------|-----------------|
| Push notifications (FCM) | Add `notification/service` channel alongside SMS outbox |
| Multilingual SMS | `preferred_language` field on `users` already present; extend SMS templates |
| Offline mobile sync | Add sync endpoint + `last_modified_at` on entities |
| Frontend mobile app | Same JWT API — no backend changes required |
| Additional gov systems | Add new `target_system` enum value + handler in `GovSyncService` |
| Expanded audit fields | `audit_log` schema accepts new `action` values without migration |
| Report export (CSV/PDF) | `can_export` flag on `government_users` gates the export endpoint |
