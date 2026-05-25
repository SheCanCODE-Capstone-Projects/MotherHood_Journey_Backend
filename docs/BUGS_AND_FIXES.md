# MotherHood Journey — Known Bugs & Required Fixes

> **Status as of:** 2026-05-25  
> **Found during:** Full live endpoint test against the running application  
> **Severity codes:** 🔴 Critical (endpoint broken) · 🟡 High (security gap) · 🟠 Medium (data integrity) · 🔵 Low (polish)

---

## Bug #1 — Admin Dashboard 500 Error 🔴 Critical

**Endpoint:** `GET /api/v1/admin/dashboard`  
**Symptom:** Returns HTTP 500 on every request.

### Root Cause

`AdminServiceImpl.getDashboard()` calls `appointmentRepository.countByStatus("SCHEDULED")`,
passing a `String` where the entity field is an `AppointmentStatus` **enum** annotated
with `@Enumerated(EnumType.STRING)`.

Hibernate 6 (bundled with Spring Boot 3.2+) no longer auto-converts a raw `String` parameter to
an enum type when binding query parameters. It expects the exact enum type. This causes a
`HibernateException` / type-binding failure at runtime → 500.

### Affected Files

| File | Problem |
|------|---------|
| `appointment/repository/AppointmentRepository.java:27` | `countByStatus(String status)` — wrong parameter type |
| `admin/service/AdminServiceImpl.java:57` | Calls `countByStatus("SCHEDULED")` with a String literal |

### Fix

**Step 1 — Change the repository method signature**

```java
// appointment/repository/AppointmentRepository.java

// Before (line 27):
long countByStatus(String status);

// After:
long countByStatus(AppointmentStatus status);
```

Add the import at the top of `AppointmentRepository.java`:

```java
import com.motherhood.journey.appointment.enums.AppointmentStatus;
```

**Step 2 — Update the call site in AdminServiceImpl**

```java
// admin/service/AdminServiceImpl.java  getDashboard() method

// Before (line 57):
appointmentRepository.countByStatus("SCHEDULED"),

// After:
appointmentRepository.countByStatus(AppointmentStatus.SCHEDULED),
```

Add the import:

```java
import com.motherhood.journey.appointment.enums.AppointmentStatus;
```

**Verification:** `GET /api/v1/admin/dashboard` with a `MOH_ADMIN` token should return 200 with all seven counters.

---

## Bug #2 — SecurityConfig Blocks MOH_ADMIN and DISTRICT_OFFICER 🟡 High

**Affected endpoints:** `GET /api/v1/mothers/**`, `GET /api/v1/children/**`, `GET /api/v1/appointments/**`  
**Symptom:** `MOH_ADMIN`, `DISTRICT_OFFICER`, and `GOVERNMENT_ANALYST` tokens receive HTTP 403 even though the controller `@PreAuthorize` annotations explicitly allow them.

### Root Cause

`SecurityConfig.java` (lines 50–55) enforces URL-level `requestMatchers` that run **before**
`@PreAuthorize` annotations. These matchers restrict the paths to a narrower role set than the
controller allows, so cross-facility roles are rejected at the URL layer and never reach the
method-level annotation.

```java
// Current (too restrictive):
.requestMatchers("/api/v1/mothers/**")
    .hasAnyRole(UserRole.HEALTH_WORKER.name(), UserRole.FACILITY_ADMIN.name())
.requestMatchers("/api/v1/children/**")
    .hasAnyRole(UserRole.HEALTH_WORKER.name(), UserRole.FACILITY_ADMIN.name())
.requestMatchers("/api/v1/appointments/**")
    .hasAnyRole(UserRole.HEALTH_WORKER.name(), UserRole.FACILITY_ADMIN.name(), UserRole.PATIENT.name())
```

### Affected File

`config/SecurityConfig.java` — lines 50–55

### Fix

Replace the three over-restrictive matchers with `authenticated()` (the fine-grained per-endpoint
role checks are already handled by `@PreAuthorize` on each controller method):

```java
// config/SecurityConfig.java — inside filterChain(), replace lines 50–55 with:

.requestMatchers("/api/v1/mothers/**").authenticated()
.requestMatchers("/api/v1/children/**").authenticated()
.requestMatchers("/api/v1/appointments/**").authenticated()
```

The full `authorizeHttpRequests` block after the fix should look like:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/geo/**").permitAll()
    .requestMatchers("/api/v1/auth/**").permitAll()
    .requestMatchers("/webhooks/at/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers("/actuator/health").permitAll()
    .requestMatchers("/api/v1/mothers/**").authenticated()
    .requestMatchers("/api/v1/children/**").authenticated()
    .requestMatchers("/api/v1/appointments/**").authenticated()
    .requestMatchers("/api/v1/reports/**")
        .hasAnyRole(UserRole.GOVERNMENT_ANALYST.name(), UserRole.MOH_ADMIN.name())
    .requestMatchers("/api/v1/facilities/**")
        .hasAnyRole(UserRole.FACILITY_ADMIN.name(), UserRole.MOH_ADMIN.name())
    .anyRequest().authenticated()
)
```

**Verification:** `GET /api/v1/mothers/{id}` with a `MOH_ADMIN` token should proceed past the
URL filter (Bug #3 below still needs fixing for it to return 200).

---

## Bug #3 — GET /api/v1/mothers/{id} LazyInitializationException 🔴 Critical

**Endpoint:** `GET /api/v1/mothers/{id}`  
**Symptom:** Returns HTTP 500. Root exception is `LazyInitializationException: could not initialize proxy — no Session`.

### Root Cause

`MotherService.enforceScope()` (line 85) calls `caller.getFacilityId()`, which accesses the
`facility` lazy proxy on the `User` entity:

```java
// MotherService.java  enforceScope() line 85
if (!caller.getFacilityId().equals(mother.getFacility().getId())) { ... }
```

The `caller` (`User`) was loaded by `CustomUserDetailsService.loadUserByUsername()` inside the
JWT filter — a separate Hibernate session that is closed long before `getMotherById()` runs.
Accessing `caller.getFacility()` (lazy proxy) outside an open session throws
`LazyInitializationException`.

### Affected File

`maternal/service/MotherService.java` — `getMotherById()` (line 64) and `enforceScope()` (line 81)

### Fix

Inject `UserRepository` into `MotherService` and re-fetch the caller within the
`@Transactional(readOnly = true)` boundary so its lazy associations are accessible.

**Step 1 — Add UserRepository to MotherService**

```java
// maternal/service/MotherService.java

import com.motherhood.journey.identity.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class MotherService {

    private final MotherRepository motherRepository;
    private final NidaVerificationService nidaVerificationService;
    private final EntityManager entityManager;
    private final UserRepository userRepository;   // ← add this field
    
    // ... existing methods
}
```

**Step 2 — Re-fetch caller inside the transactional method**

```java
// maternal/service/MotherService.java  getMotherById()

@Transactional(readOnly = true)
public MotherResponse getMotherById(UUID id, User caller) {
    Mother mother = motherRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Mother not found: " + id));

    // Re-load caller within this transaction to avoid LazyInitializationException
    // on the facility proxy set up by CustomUserDetailsService in a separate session.
    User freshCaller = userRepository.findById(caller.getId())
            .orElseThrow(() -> new IllegalArgumentException("Caller user not found"));

    enforceScope(mother, freshCaller);
    return MotherResponse.from(mother);
}
```

**Verification:** `GET /api/v1/mothers/{id}` with a `HEALTH_WORKER` token at the same facility
as the mother should return 200 with the mother's details.

---

## Bug #4 — Missing Flyway Migration for `seq_mother_health_id` 🟠 Medium

**Triggered by:** `POST /api/v1/mothers`  
**Symptom:** HTTP 500 on first deployment or fresh database. Error: `ERROR: relation "seq_mother_health_id" does not exist`.

### Root Cause

`MotherService.generateHealthId()` executes:

```java
entityManager.createNativeQuery("SELECT nextval('seq_mother_health_id')").getSingleResult();
```

The sequence `seq_mother_health_id` was never created in any Flyway migration (V1–V11).
Fresh deployments fail immediately. The workaround of running `CREATE SEQUENCE` manually in
`psql` is not repeatable across environments.

### Fix

Create a new Flyway migration file:

**File:** `src/main/resources/db/migration/V12__add_mother_health_id_sequence.sql`

```sql
-- Creates the sequence used by MotherService.generateHealthId()
-- to generate MH-YYYY-NNNNNN health IDs for registered mothers.
CREATE SEQUENCE IF NOT EXISTS seq_mother_health_id
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 1;
```

**Important:** Do not modify any existing V1–V11 scripts. Flyway will refuse to start if an
already-applied migration script is changed. V12 is the correct next version.

**Verification:** On a clean database, `POST /api/v1/mothers` should succeed and return a
`healthId` in the format `MH-YYYY-NNNNNN` (e.g. `MH-2026-000001`).

---

## Summary Table

| # | Severity | Endpoint | Root Cause | Files to Change |
|---|----------|----------|------------|-----------------|
| 1 | 🔴 Critical | `GET /api/v1/admin/dashboard` | `countByStatus(String)` type mismatch with `AppointmentStatus` enum (Hibernate 6) | `AppointmentRepository.java`, `AdminServiceImpl.java` |
| 2 | 🟡 High | `GET /api/v1/mothers/**` `GET /api/v1/children/**` `GET /api/v1/appointments/**` | URL matchers block MOH_ADMIN / DISTRICT_OFFICER before @PreAuthorize can run | `SecurityConfig.java` |
| 3 | 🔴 Critical | `GET /api/v1/mothers/{id}` | LazyInitializationException on `caller.getFacilityId()` in closed Hibernate session | `MotherService.java` |
| 4 | 🟠 Medium | `POST /api/v1/mothers` | `seq_mother_health_id` sequence missing from Flyway migrations | New `V12__add_mother_health_id_sequence.sql` |

---

## Implementation Order

Fix in this order to unblock testing progressively:

1. **Bug #4 first** — adding the sequence migration unblocks all mother-related tests.
2. **Bug #2 second** — widening SecurityConfig allows proper role testing without code flow changes.
3. **Bug #3 third** — fixes the 500 on `GET /api/v1/mothers/{id}` after roles are unblocked.
4. **Bug #1 last** — fixes the admin dashboard after the domain logic is verified.