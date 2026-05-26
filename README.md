# MotherHood Journey — Backend

> A production-ready maternal and child health platform for Rwanda, built by IgireRwanda Organization as part of the SheCanCode Bootcamp.

---

## Problem Statement

Across Rwanda, community health workers manually track ANC visits, vaccination schedules, and birth registrations on paper cards that are easily lost, duplicated, or never reported up the chain. Facility administrators have no real-time visibility into no-show rates or coverage gaps. Ministry of Health analysts wait weeks for aggregated statistics that could inform rapid response.

MotherHood Journey replaces paper-based tracking with a secure digital system that:
- Gives CHWs a structured interface for recording visits, registering newborns, and tracking vaccination status.
- Sends automated SMS reminders to mothers before appointments and when vaccinations become overdue.
- Provides facility administrators with live dashboards on service delivery and coverage.
- Enables government analysts to generate aggregated health reports and push data to MoH HMIS — with full consent enforcement and audit trails required by Rwanda Law No. 058/2021.

---

## Features

| Domain | Capabilities |
|--------|-------------|
| **Identity & Auth** | JWT-based auth, 6 RBAC roles, Rwanda NID verification via NIDA API |
| **Geographic Hierarchy** | Full Rwanda 5-level admin tree (Province → District → Sector → Cell → Village), seeded from MoH data |
| **Mothers** | Registration, NIDA cross-check, digital health ID (`MH-YYYY-NNNNNN`) generation |
| **Pregnancies** | ANC tracking, gravida/para records, CHW assignment, full obstetric history |
| **Children** | Birth registration, digital birth certificates, growth monitoring, health status tracking |
| **Vaccinations** | EPI schedule seeding, per-child records, automated OVERDUE detection via cron |
| **Clinical Visits** | Polymorphic ANC/PNC/immunization/growth-monitoring visit records with vitals |
| **Appointments** | Scheduling, 24-hour SMS reminders, no-show tracking for analytics |
| **Consent** | Data-sharing consent enforcement per Rwanda Law No. 058/2021 |
| **Government Integration** | Irembo service requests, NIDA identity verification, MoH HMIS reporting via outbox pattern |
| **SMS Notifications** | Africa's Talking outbox with webhook-based delivery tracking |
| **Audit Log** | Immutable PHI access log, 7-year retention, monthly partitioning |
| **Admin Dashboard** | System-wide counters for facilities, mothers, children, and pending tasks |

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 21 |
| Framework | Spring Boot | 3.2.5 |
| Database | PostgreSQL | 16 |
| Migrations | Flyway | 9 (managed by Spring Boot BOM) |
| ORM | Spring Data JPA / Hibernate | 6 |
| Security | Spring Security + JJWT | 0.12.5 |
| Rate Limiting | Bucket4j | 8.10.1 |
| Caching | Caffeine + Spring Cache | managed |
| API Docs | SpringDoc OpenAPI (Swagger UI) | 2.5.0 |
| Build | Maven | 3.9 |
| Container | Docker (multi-stage, Eclipse Temurin 21 JRE Alpine) | — |
| SMS | Africa's Talking API | — |
| Gov APIs | NIDA, Irembo, MoH HMIS | — |
| Testing | JUnit 5, Spring Boot Test, Spring Security Test | — |

---

## Documentation

| Document | Description |
|----------|-------------|
| [API Documentation](docs/API_DOCUMENTATION.md) | All 19 controllers — endpoints, request/response shapes, valid enum values, role requirements, live test report |
| [Sprint Plan](docs/motherhood-journey-sprint-plan.md) | 5 sprints · 9 engineers · 313 story points — task assignments and descriptions |
| [Remaining Tasks](docs/REMAINING_TASKS.md) | Cross-reference of sprint plan vs. codebase — what is done, what is missing, effort estimates |
| [Bugs & Fixes](docs/BUGS_AND_FIXES.md) | 4 known bugs with root cause analysis and exact code fixes |
| [Database Design](docs/DATABASE_DESIGN.md) | 10 table groups, key enumerations, compliance notes |
| [DB Design Handout](docs/MotherHood_Journey_DB_Handout.md) | Full schema reference: all 16 tables, RBAC matrix, government integration architecture, security checklist |
| [Folder Structure](docs/MotherHoodJourney_FolderStructure.md) | Domain-first package layout guide — what every folder is and where new code goes |
| [Deployment Guide](docs/DEPLOYMENT.md) | Railway deployment, environment variables, health check, migration runbook |
| [Architecture](docs/ARCHITECTURE.md) | System architecture overview *(in progress)* |
| [Contributing](docs/CONTRIBUTING.md) | Git workflow and code style guide *(in progress)* |

---

## Quick Start (Local)

### Prerequisites

- Java 21
- Docker + Docker Compose
- Maven 3.9+

### 1 — Clone and configure environment

```bash
git clone <repo-url>
cd MotherHood_Journey_Backend
cp .env.example .env   # fill in values (see table below)
```

### 2 — Start the database

```bash
docker compose up -d postgres
```

### 3 — Run the application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Or build and run the jar:

```bash
./mvnw clean package -DskipTests
java -jar target/*.jar --spring.profiles.active=local
```

The API starts on `http://localhost:8080`.

### 4 — Explore the API

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI spec:** `http://localhost:8080/v3/api-docs`
- **Health check:** `http://localhost:8080/actuator/health`

---

## Environment Variables

| Variable | Required | Description | Default |
|----------|----------|-------------|---------|
| `DB_HOST` | Yes | PostgreSQL host | `localhost` |
| `DB_PORT` | Yes | PostgreSQL port | `5432` |
| `DB_NAME` | Yes | Database name | `Motherhood Journey DB` |
| `DB_USERNAME` | Yes | PostgreSQL username | — |
| `DB_PASSWORD` | Yes | PostgreSQL password | — |
| `JWT_SECRET` | Yes | HS512 secret (≥64 chars) | — |
| `JWT_EXPIRATION_MS` | No | Access token lifetime | `86400000` (24 h) |
| `JWT_REFRESH_EXPIRATION_MS` | No | Refresh token lifetime | `604800000` (7 days) |
| `AT_API_KEY` | Yes | Africa's Talking API key | — |
| `AT_USERNAME` | Yes | Africa's Talking username | — |
| `IREMBO_BASE_URL` | No | Irembo government API base URL | `""` |
| `IREMBO_API_KEY` | No | Irembo API key | `""` |
| `HMIS_BASE_URL` | No | MoH HMIS API base URL | `""` |
| `HMIS_API_KEY` | No | HMIS API key | `""` |
| `NIDA_BASE_URL` | No | NIDA identity verification base URL | `""` |
| `CORS_ALLOWED_ORIGINS` | No | Frontend origin(s) | `http://localhost:3000` |
| `APP_PORT` | No | Server port | `8080` |

---

## Role System

Six roles with strictly enforced access control (URL-level matchers + `@PreAuthorize`):

| Role | Abbreviation | Access |
|------|-------------|--------|
| `PATIENT` | PAT | Own profile, own children, own appointments |
| `HEALTH_WORKER` | HW | Register mothers/children, record visits, administer vaccines |
| `FACILITY_ADMIN` | FA | All data for their facility, approve service requests, manage staff |
| `DISTRICT_OFFICER` | DO | Read access scoped to authorized geographic sectors |
| `GOVERNMENT_ANALYST` | GA | Generate and view aggregated reports |
| `MOH_ADMIN` | MOH | Full system access including user management and HMIS push |

---

## Key API Endpoints

| Area | Base path | Public |
|------|-----------|--------|
| Auth | `/api/v1/auth` | Yes |
| Geo (Rwanda hierarchy) | `/api/v1/geo` | Yes |
| Webhook (Africa's Talking) | `/webhooks/at` | Yes (signature-validated) |
| Mothers | `/api/v1/mothers` | No |
| Pregnancies | `/api/v1/pregnancies` | No |
| Children | `/api/v1/children` | No |
| Vaccinations | `/api/v1/vaccinations` | No |
| Health Visits | `/api/v1/health-visits` | No |
| Appointments | `/api/v1/appointments` | No |
| Consent Records | `/api/v1/consents` | No |
| Service Requests | `/api/v1/service-requests` | No |
| Service Request Documents | `/api/v1/service-requests/{id}/documents` | No |
| Government Reports | `/api/v1/gov-reports` | No |
| Government Sync (admin) | `/api/v1/gov-sync` | No (MOH_ADMIN) |
| Government Users | `/api/v1/government` | No (MOH_ADMIN) |
| Notifications | `/api/v1/notifications` | No |
| Facilities | `/api/v1/facilities` | No |
| Admin Dashboard | `/api/v1/admin` | No (MOH_ADMIN) |
| Users | `/api/v1/users` | No (MOH_ADMIN) |
| User Profile | `/api/v1/me` | No |

Full request/response documentation: [docs/API_DOCUMENTATION.md](docs/API_DOCUMENTATION.md)

---

## Architecture Summary

```
┌──────────────────────────────────────────────────────────┐
│  Next.js Frontend (separate repo)                        │
│  next-auth · @tanstack/react-query · Tailwind · shadcn   │
└───────────────────────┬──────────────────────────────────┘
                        │ HTTPS / JWT
┌───────────────────────▼──────────────────────────────────┐
│  Spring Boot 3.2.5 (this repo)                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌───────────┐   │
│  │ identity │ │ maternal │ │  child   │ │appointment│   │
│  │ geo      │ │ consent  │ │government│ │notification│  │
│  └──────────┘ └──────────┘ └──────────┘ └───────────┘   │
│  JwtFilter → SecurityConfig → @PreAuthorize              │
│  AuditAspect (every PHI access logged)                   │
│  GovSyncService outbox (NIDA · HMIS · Irembo)            │
└───────────────────────┬──────────────────────────────────┘
                        │ JDBC / Flyway V1–V12
┌───────────────────────▼──────────────────────────────────┐
│  PostgreSQL 16                                           │
│  16 tables · geo_locations (14,000+ Rwanda rows)         │
│  audit_log (partitioned by month)                        │
│  gov_sync_log (outbox pattern)                           │
└──────────────────────────────────────────────────────────┘
                        │
        ┌───────────────┼────────────────┐
        ▼               ▼                ▼
  Africa's Talking   NIDA API       MoH HMIS / Irembo
  (SMS via outbox)  (ID verify)    (reports / service requests)
```

Domain-first package structure: `com.motherhood.journey.<domain>.<layer>`.
See [docs/MotherHoodJourney_FolderStructure.md](docs/MotherHoodJourney_FolderStructure.md) for
the full layout guide.

---

## Database

Flyway manages all schema changes. Never use `spring.jpa.hibernate.ddl-auto: create` or `update`.

| Migration | What it creates |
|-----------|----------------|
| V1 | Full initial schema (users, facilities, geo_locations, mothers, pregnancies, children, vaccination_schedules, vaccination_records, health_visits, appointments, consent_records, service_requests, sms_notifications, government tables) |
| V2 | Schema fixes |
| V3 | audit_log table with monthly partitioning |
| V4 | Constraints and partitions |
| V5 | Appointments and notifications refinements |
| V6 | Service request sequence (`SR-YYYY-NNNNN`) |
| V7 | Performance indexes |
| V8 | Health visit constraints |
| V9 | ICD-10 / HMIS code seed data |
| V10 | Cancellation reason on appointments |
| V11 | GovSyncLog missing columns |
| V12 | `seq_mother_health_id` sequence for health ID generation |

Rwanda geo-location hierarchy (5 provinces → 30 districts → 416 sectors → 2,148 cells → ~14,000
villages) is seeded in V1. Never duplicate geo data — always join to `geo_locations`.

See [docs/MotherHood_Journey_DB_Handout.md](docs/MotherHood_Journey_DB_Handout.md) for the full
schema reference with column descriptions, constraints, and the RBAC permission matrix.

---

## Government Integration

All external API calls use the **outbox pattern** via `gov_sync_log`:

1. Write to `gov_sync_log` with `status=PENDING` before any external call
2. `GovSyncService` (@Scheduled every 2 min) processes PENDING rows
3. On failure: increments `retry_count`, sets `next_retry_at` with exponential backoff
4. After 5 retries: `status=DEAD_LETTER` → MOH_ADMIN alert SMS queued

Connected systems:

| System | Purpose |
|--------|---------|
| **NIDA** | Identity verification at mother registration |
| **MoH HMIS** | Push aggregated health statistics, pull vaccination schedules |
| **Irembo** | Submit citizen service requests (birth certificates, referrals) |
| **Africa's Talking** | Outbound SMS reminders + delivery status webhooks |

---

## Resolved Issues

All four critical bugs listed in [docs/BUGS_AND_FIXES.md](docs/BUGS_AND_FIXES.md) are resolved on `main`:

| # | Endpoint | Status |
|---|----------|--------|
| 1 | `POST /api/v1/mothers` — `seq_mother_health_id` missing | ✅ Added in `V20__Mother_Health_Id_Sequence.sql` |
| 2 | SecurityConfig blocking MOH_ADMIN / DISTRICT_OFFICER | ✅ URL matchers now defer to per-method `@PreAuthorize` |
| 3 | `GET /api/v1/mothers/{id}` LazyInitializationException | ✅ `MotherService.getMotherById` re-loads caller inside the transaction |
| 4 | `GET /api/v1/admin/dashboard` enum type mismatch | ✅ `AppointmentRepository.countByStatus(AppointmentStatus)` is now typed |

---

## Running Tests

```bash
# Unit tests only
./mvnw test

# Single test class
./mvnw test -Dtest=ChildServiceTest

# Full build with tests
./mvnw verify
```

Test suites cover: child registration, consent workflow, facility CRUD, service requests, JWT generation/validation, facility RBAC, security boundary enforcement, and date utilities.

Note: additional integration tests are tracked in [docs/REMAINING_TASKS.md](docs/REMAINING_TASKS.md).

---

## Contributing

See [docs/CONTRIBUTING.md](docs/CONTRIBUTING.md) for:
- Branch naming conventions (`feature/`, `bugfix/`, `hotfix/`)
- Commit message format (Conventional Commits)
- Pull request process and review checklist
- Code style guidelines (Google Java Style)
- Testing requirements before submitting a PR
- Security and responsible disclosure process

---

## License

This project was developed by IgireRwanda Organization as part of the SheCanCode Bootcamp, Kigali, Rwanda.

All rights reserved © 2026 IgireRwanda Organization.

---

## Team

| ID | Name | Discipline | Domain |
|----|------|------------|--------|
| BE1 | Agnes Mbabazi | Backend | Infra & CI/CD |
| BE2 | Ange Umukundwa | Backend | Migrations & audit |
| BE3 | Numukobwa Diane | Backend | Core clinical domain |
| BE4 | Rosine Muhoza | Backend | Scheduling & async jobs |
| BE5 | Clesence Niyirema Sabato | Backend | Gov integration & facility stats |
| FE1 | IMFURANKUNDA Cherly | Frontend | Data-heavy pages & maps |
| FE2 | BYUKUSENGE Immaculee | Frontend | API client & query infra |
| FE3 | UWAYESU Lydie | Frontend | Forms & validation |
| FE4 | Emelyne | Frontend | Patient-facing mobile views |

---

*IgireRwanda Organization · SheCanCode Bootcamp · Kacyiru, Kigali, Rwanda*
