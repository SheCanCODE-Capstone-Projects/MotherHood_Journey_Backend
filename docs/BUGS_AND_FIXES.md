# MotherHood Journey — Known Bugs & Required Fixes

> **Status as of:** 2026-05-25 (initial discovery) / **Fixes applied:** 2026-05-25
> **Found during:** Full live endpoint test against the running application
> **Severity codes:** 🔴 Critical (endpoint broken) · 🟡 High (security gap) · 🟠 Medium (data integrity) · 🔵 Low (polish)
> **Legend:** ✅ Fixed · 🔧 Documented (fix not yet applied)

---

## Bug #1 — Admin Dashboard 500 Error 🔴 Critical ✅ Fixed

**Endpoint:** `GET /api/v1/admin/dashboard`
**Symptom:** Returns HTTP 500 on every request.

### Root Cause

`AdminServiceImpl.getDashboard()` called `appointmentRepository.countByStatus("SCHEDULED")`,
passing a `String` where the entity field is an `AppointmentStatus` **enum** annotated
with `@Enumerated(EnumType.STRING)`.

Hibernate 6 (bundled with Spring Boot 3.2+) no longer auto-converts a raw `String` parameter to
an enum type when binding query parameters. It expects the exact enum type. This causes a
`HibernateException` / type-binding failure at runtime → 500.

### Affected Files

| File | Problem |
|------|---------|
| `appointment/repository/AppointmentRepository.java` | `countByStatus(String status)` — wrong parameter type |
| `appointment/repository/AppointmentRepository.java` | `findByFacility_IdAndStatus(UUID, String)` — same issue |
| `admin/service/AdminServiceImpl.java` | Calls `countByStatus("SCHEDULED")` with a String literal |

### Fix Applied

**`AppointmentRepository.java`** — changed parameter types to `AppointmentStatus` enum:
```java
// Before:
long countByStatus(String status);
List<Appointment> findByFacility_IdAndStatus(UUID facilityId, String status);

// After:
long countByStatus(AppointmentStatus status);
List<Appointment> findByFacility_IdAndStatus(UUID facilityId, AppointmentStatus status);
```

**`AdminServiceImpl.java`** —
changed call site to use enum constant:
```java
// Before:
appointmentRepository.countByStatus("SCHEDULED"),

// After:
appointmentRepository.countByStatus(AppointmentStatus.SCHEDULED),
```

**Verification:** `GET /api/v1/admin/dashboard` with a `MOH_ADMIN` token returns 200 with all seven counters.

---

## Bug #2 — SecurityConfig Blocks MOH_ADMIN and DISTRICT_OFFICER 🟡 High ✅ Fixed

**Affected endpoints:** `GET /api/v1/mothers/**`, `GET /api/v1/children/**`, `GET /api/v1/appointments/**`
**Symptom:** `MOH_ADMIN`, `DISTRICT_OFFICER`, and `GOVERNMENT_ANALYST` tokens receive HTTP 403 even though the controller `@PreAuthorize` annotations explicitly allow them.

### Root Cause

`SecurityConfig.java` enforced URL-level `requestMatchers` that run **before**
`@PreAuthorize` annotations. These matchers restricted the paths to a narrower role set than the
controller allows, so cross-facility roles were rejected at the URL layer and never reached the
method-level annotation.

```java
// Before (too restrictive):
.requestMatchers("/api/v1/mothers/**")
    .hasAnyRole(UserRole.HEALTH_WORKER.name(), UserRole.FACILITY_ADMIN.name())
.requestMatchers("/api/v1/children/**")
    .hasAnyRole(UserRole.HEALTH_WORKER.name(), UserRole.FACILITY_ADMIN.name())
.requestMatchers("/api/v1/appointments/**")
    .hasAnyRole(UserRole.HEALTH_WORKER.name(), UserRole.FACILITY_ADMIN.name(), UserRole.PATIENT.name())
```

### Fix Applied

**`config/SecurityConfig.java`** — replaced the three over-restrictive matchers with `.authenticated()`:
```java
// After — fine-grained per-endpoint role checks handled by @PreAuthorize:
.requestMatchers("/api/v1/mothers/**").authenticated()
.requestMatchers("/api/v1/children/**").authenticated()
.requestMatchers("/api/v1/appointments/**").authenticated()
```

**Verification:** `GET /api/v1/mothers/{id}` with a `MOH_ADMIN` token now proceeds past the URL filter.

---

## Bug #3 — GET /api/v1/mothers/{id} LazyInitializationException 🔴 Critical ✅ Fixed

**Endpoint:** `GET /api/v1/mothers/{id}`
**Symptom:** Returns HTTP 500. Root exception is `LazyInitializationException: could not initialize proxy — no Session`.

### Root Cause

`MotherService.enforceScope()` called `caller.getFacilityId()`, which accesses the
`facility` lazy proxy on the `User` entity:

```java
if (!caller.getFacilityId().equals(mother.getFacility().getId())) { ... }
```

The `caller` (`User`) was loaded by `CustomUserDetailsService.loadUserByUsername()` inside the
JWT filter — a separate Hibernate session that is closed long before `getMotherById()` runs.
Accessing `caller.getFacility()` (lazy proxy) outside an open session throws
`LazyInitializationException`.

### Fix Applied

**`maternal/service/MotherService.java`** — injected `UserRepository` and re-fetched caller within the `@Transactional` boundary:

```java
// Added field:
private final UserRepository userRepository;

// getMotherById() — re-fetch caller in this transaction:
@Transactional(readOnly = true)
public MotherResponse getMotherById(UUID id, User caller) {
    Mother mother = motherRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Mother not found: " + id));
    User freshCaller = userRepository.findById(caller.getId())
            .orElseThrow(() -> new IllegalArgumentException("Caller not found: " + caller.getId()));
    enforceScope(mother, freshCaller);
    return MotherResponse.from(mother);
}
```

**Verification:** `GET /api/v1/mothers/{id}` with a `HEALTH_WORKER` token at the same facility returns 200.

---

## Bug #4 — Missing Flyway Migration for `seq_mother_health_id` 🟠 Medium ✅ Fixed

**Triggered by:** `POST /api/v1/mothers`
**Symptom:** HTTP 500 on first deployment or fresh database. Error: `ERROR: relation "seq_mother_health_id" does not exist`.

### Root Cause

`MotherService.generateHealthId()` executes:

```java
entityManager.createNativeQuery("SELECT nextval('seq_mother_health_id')").getSingleResult();
```

The sequence `seq_mother_health_id` was never created in any Flyway migration (V1–V11).
Fresh deployments fail immediately.

### Fix Applied

Created **`src/main/resources/db/migration/V12__add_mother_health_id_sequence.sql`**:

```sql
CREATE SEQUENCE IF NOT EXISTS seq_mother_health_id
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    CACHE 1;
```

**Verification:** On a clean database, `POST /api/v1/mothers` succeeds and returns a
`healthId` in the format `MH-YYYY-NNNNNN` (e.g. `MH-2026-000001`).

---

## Bug #5 — MotherRepository.findByNidaVerifiedStatus Type Mismatch 🔴 Critical ✅ Fixed

**Triggered by:** `GET /api/v1/mothers/pending-nida`
**Symptom:** HTTP 500 at runtime. Hibernate 6 type-binding error.

### Root Cause

`MotherService.getPendingNidaVerification()` called:
```java
motherRepository.findByNidaVerifiedStatus(NidaVerifiedStatus.PENDING.name())
```

`Mother.nidaVerifiedStatus` is `@Enumerated(EnumType.STRING)` with type `NidaVerifiedStatus`.
`MotherRepository.findByNidaVerifiedStatus(String)` passes a `String` to an enum field —
the same Hibernate 6 type-binding failure pattern as Bug #1.

### Fix Applied

**`maternal/repository/MotherRepository.java`** — changed method signature:
```java
// Before:
List<Mother> findByNidaVerifiedStatus(String nidaVerifiedStatus);

// After:
List<Mother> findByNidaVerifiedStatus(NidaVerifiedStatus nidaVerifiedStatus);
```

**`maternal/service/MotherService.java`** — changed call site:
```java
// Before:
motherRepository.findByNidaVerifiedStatus(NidaVerifiedStatus.PENDING.name())

// After:
motherRepository.findByNidaVerifiedStatus(NidaVerifiedStatus.PENDING)
```

**Verification:** `GET /api/v1/mothers/pending-nida` returns the list of mothers with PENDING NIDA status.

---

## Bug #6 — Test Suite Fails Under Java 21+ (Mockito inline mocking) 🔵 Low ✅ Fixed

**Symptom:** `./mvnw test` fails with `MockitoException: Could not modify all classes [java.lang.Object, AuditService]` when run on Java 21+.

### Root Cause

Mockito 5.x defaults to **inline bytecode instrumentation** (byte-buddy retransformation) to mock
concrete classes. On Java 21+ (and especially Java 25, which this machine uses), the JVM's module
system blocks retransformation of `java.lang.Object` and other bootstrap classes — a hard JVM
restriction that `--add-opens` alone cannot bypass.

`AuditService` is a concrete `@Service` class (not an interface), so Mockito tried to use inline
mocking, triggering the restriction.

### Fix Applied

Created **`src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`**:
```
mock-maker-subclass
```

This switches Mockito to the **subclass mock maker**, which creates a new subclass of the target
class rather than retransforming existing classes. No JVM module access is required.

Also added JVM `--add-opens` flags to the Surefire plugin in `pom.xml` as defence-in-depth.

**Verification:** `./mvnw test` reports `Tests run: 77, Failures: 0, Errors: 0, Skipped: 0`.

---

## Bug #7 — VaccinationServiceTest in Wrong Directory 🔵 Low ✅ Fixed

**File:** `src/main/tests/VaccinationServiceTest.java` (wrong location, wrong package, wrong API)
**Symptom:** Test file is in `src/main/tests/` (not on the test classpath) and references methods that do not exist in the current `VaccinationServiceImpl`.

### Root Cause

The test was written for a `VaccinationService` API (`markAdministered(UUID, MarkAdministeredRequest, UUID)`) that was later replaced by `administer(UUID, UUID, AdministerVaccinationRequest)`. It was placed in `src/main/tests/` instead of `src/test/java/...`, so it was never compiled as a test.

### Fix Applied

- Moved and rewritten to **`src/test/java/com/motherhood/journey/child/service/VaccinationServiceTest.java`**
- Updated to use the current `VaccinationServiceImpl` API (tests cover `markOverdueAndNotify`)
- Fixed ambiguous `notificationService.enqueue(any())` → `enqueue(any(VaccinationRecord.class))`

---

## Bug #8 — UserRepository Duplicate Import 🔵 Low ✅ Fixed

**File:** `identity/repository/UserRepository.java`
**Symptom:** Duplicate `import org.springframework.data.jpa.repository.Query;` (lines 8–9).

### Fix Applied

Removed the duplicate import. Compilation warning eliminated.

---

## Summary Table

| # | Severity | Endpoint | Root Cause | Status |
|---|----------|----------|------------|--------|
| 1 | 🔴 Critical | `GET /api/v1/admin/dashboard` | `countByStatus(String)` type mismatch — Hibernate 6 | ✅ Fixed |
| 2 | 🟡 High | `GET /api/v1/mothers/**` etc. | URL matchers block MOH_ADMIN before @PreAuthorize | ✅ Fixed |
| 3 | 🔴 Critical | `GET /api/v1/mothers/{id}` | LazyInitializationException on `caller.facility` | ✅ Fixed |
| 4 | 🟠 Medium | `POST /api/v1/mothers` | `seq_mother_health_id` missing from Flyway | ✅ Fixed |
| 5 | 🔴 Critical | `GET /api/v1/mothers/pending-nida` | `findByNidaVerifiedStatus(String)` type mismatch | ✅ Fixed |
| 6 | 🔵 Low | (all tests) | Mockito inline mocking blocked under Java 21+ | ✅ Fixed |
| 7 | 🔵 Low | (test infra) | VaccinationServiceTest in wrong directory / wrong API | ✅ Fixed |
| 8 | 🔵 Low | (compile) | Duplicate @Query import in UserRepository | ✅ Fixed |

---

## Implementation Order Applied

1. **Bug #4** — V12 migration unblocked mother registration on fresh DBs.
2. **Bug #2** — Widened SecurityConfig to unblock MOH_ADMIN / DISTRICT_OFFICER.
3. **Bug #3** — Fixed LazyInitializationException on `GET /mothers/{id}`.
4. **Bug #1** — Fixed Admin dashboard AppointmentStatus type mismatch.
5. **Bug #5** — Fixed NidaVerifiedStatus type mismatch (same root cause as #1).
6. **Bug #6** — Fixed Mockito mock-maker for Java 21+ compatibility.
7. **Bug #7** — Moved and corrected VaccinationServiceTest.
8. **Bug #8** — Removed duplicate import.