# Contributing to MotherHood Journey

> IgireRwanda Organization | SheCanCode Bootcamp | Kigali, Rwanda

Thank you for contributing. This document covers everything needed to make a clean, reviewable contribution: local setup, branching, commits, pull requests, code style, and security reporting.

---

## Table of Contents

1. [Local Setup for Contributors](#1-local-setup-for-contributors)
2. [Contribution Workflow](#2-contribution-workflow)
3. [Branch Naming Conventions](#3-branch-naming-conventions)
4. [Commit Message Conventions](#4-commit-message-conventions)
5. [Pull Request Process](#5-pull-request-process)
6. [Code Style Guidelines](#6-code-style-guidelines)
7. [Documentation Standards](#7-documentation-standards)
8. [Testing Requirements](#8-testing-requirements)
9. [Issue Reporting Guidelines](#9-issue-reporting-guidelines)
10. [Review Process](#10-review-process)
11. [Security & Responsible Disclosure](#11-security--responsible-disclosure)

---

## 1. Local Setup for Contributors

### Prerequisites

- Java 21 (use [SDKMAN](https://sdkman.io) or your system package manager)
- Maven 3.9+
- Docker & Docker Compose
- Git configured with your name and email

### First-time setup

```bash
# 1. Fork the repository on GitHub, then clone your fork
git clone https://github.com/<your-username>/MotherHood_Journey_Backend.git
cd MotherHood_Journey_Backend

# 2. Add the upstream remote
git remote add upstream https://github.com/SheCanCODE-Capstone-Projects/MotherHood_Journey_Backend.git

# 3. Configure your environment
cp .env.example .env
# Edit .env — set DB_HOST, DB_NAME, DB_USERNAME, DB_PASSWORD, JWT_SECRET at minimum
# Leave AT_API_KEY, IREMBO_*, NIDA_*, HMIS_* empty for local dev

# 4. Start the database
docker compose up -d postgres

# 5. Build and run
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Verify the application is running:

```bash
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP", ...}
```

Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Keeping your fork up to date

```bash
git fetch upstream
git checkout main
git merge upstream/main
git push origin main
```

---

## 2. Contribution Workflow

```
main (protected)
  │
  └─ feature/your-description
       │
       ├─ commit: feat: add vaccination reminder endpoint
       ├─ commit: test: add VaccinationServiceTest
       └─ PR → reviewed → squash-merged → main
```

1. Always branch from the latest `main`.
2. Keep branches short-lived (days, not weeks).
3. One feature or fix per branch.
4. Never push directly to `main`. All changes go through pull requests.
5. Self-review your diff before requesting review.

---

## 3. Branch Naming Conventions

| Type | Pattern | Example |
|------|---------|---------|
| New feature | `feature/<short-description>` | `feature/vaccination-reminder-cron` |
| Bug fix | `bugfix/<issue-or-description>` | `bugfix/lazy-init-mother-service` |
| Hotfix (production) | `hotfix/<short-description>` | `hotfix/security-config-role-matchers` |
| Documentation | `docs/<short-description>` | `docs/update-api-reference` |
| Refactoring | `refactor/<short-description>` | `refactor/appointment-service-extract-scope` |
| Database migration | `migration/<description>` | `migration/add-mother-health-id-sequence` |

Rules:
- All lowercase, hyphen-separated. No underscores or spaces.
- Keep it under 50 characters.
- Include a ticket/issue number if one exists: `feature/GH-42-vaccination-overdue-sms`

---

## 4. Commit Message Conventions

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <short summary in present tense>

[optional body — explain WHY, not WHAT]

[optional footer — e.g. Closes #42]
```

### Types

| Type | When to use |
|------|------------|
| `feat` | New feature or endpoint |
| `fix` | Bug fix |
| `test` | Adding or correcting tests |
| `docs` | Documentation changes only |
| `refactor` | Code restructuring without behaviour change |
| `perf` | Performance improvement |
| `chore` | Dependency updates, build config, non-code changes |
| `migration` | New Flyway migration file |

### Scope (optional but recommended)

Use the domain package name: `maternal`, `child`, `appointment`, `consent`, `identity`, `notification`, `government`, `geo`, `admin`, `security`, `scheduler`.

### Examples

```
feat(appointment): add 24-hour SMS reminder cron job

fix(security): widen URL matchers to allow MOH_ADMIN on /mothers/**

Resolves Bug #2 from BUGS_AND_FIXES.md — URL-level matchers were
blocking MOH_ADMIN before @PreAuthorize could run.

Closes #87

test(child): add ChildServiceTest for facility ownership boundary

migration: add V12 seq_mother_health_id sequence

Fixes Bug #4 — seq was missing from all Flyway migrations, causing
500 on fresh deployments when POST /api/v1/mothers was called.
```

### Rules

- Summary line: imperative mood, lowercase, no period, max 72 characters.
- Never write `updated X` — write `update X`.
- Never reference "the code" or "the file" — reference the behaviour.

---

## 5. Pull Request Process

### Before opening a PR

- [ ] Branch is up to date with `main`
- [ ] `./mvnw test` passes with no failures
- [ ] No new compiler warnings introduced
- [ ] New code has corresponding unit tests (see [Testing Requirements](#8-testing-requirements))
- [ ] Any new environment variable is added to `.env.example` with a placeholder value
- [ ] Any new Flyway migration follows `V{N+1}__Description.sql` naming
- [ ] Existing migrations are NOT modified

### PR title format

Follow the same Conventional Commits format:

```
feat(government): add HMIS push endpoint for monthly reports
fix(maternal): resolve LazyInitializationException in MotherService
```

### PR description template

```markdown
## Summary
<!-- 2–3 bullet points describing what changed and why -->

## Type of change
- [ ] Bug fix
- [ ] New feature
- [ ] Refactoring
- [ ] Documentation
- [ ] Database migration

## Testing
<!-- Describe what you tested and how. Include curl commands or test class names. -->

## Checklist
- [ ] Tests pass locally (`./mvnw test`)
- [ ] No sensitive data committed (no secrets in .env or code)
- [ ] Flyway migration added if schema changed
- [ ] `.env.example` updated if new env vars added
- [ ] API_DOCUMENTATION.md updated if new endpoints added
```

### Review SLA

PRs are reviewed within 2 working days. If no review after 3 days, ping the reviewer in the PR comments.

---

## 6. Code Style Guidelines

The project uses **Google Java Style** enforced via `checkstyle.xml` at the project root.

### Formatting

- **Indentation:** 4 spaces (no tabs)
- **Line length:** 120 characters max
- **Braces:** Allman-adjacent (opening brace on same line)
- **Imports:** No wildcard imports — one class per import

Run Checkstyle before committing:

```bash
./mvnw checkstyle:check
```

### Naming

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `MotherServiceImpl` |
| Methods | camelCase | `registerMother()` |
| Constants | UPPER_SNAKE | `MAX_RETRY_COUNT` |
| Variables | camelCase | `facilityId` |
| Packages | lowercase | `com.motherhood.journey.maternal` |
| DB columns/tables | snake_case (in entity `@Column`) | `health_id`, `geo_location_id` |

### Architecture rules

- **Controllers** contain zero business logic. Only call the service and return `ApiResponse`.
- **Services** own all business logic. Never call another domain's repository directly — go through the domain's service interface.
- **Repositories** are injected only into `ServiceImpl` classes.
- **Entities** are never returned from controllers. Always convert to a response DTO in the service layer.
- **Never use `ddl-auto: create` or `ddl-auto: update`** — Flyway manages the schema.
- **All external API calls** go through `gov_sync_log` outbox first.

### Comments

Write comments only for non-obvious behaviour — hidden constraints, workarounds, subtle invariants. Do not comment what the code does; use clear naming instead.

### Lombok

Use Lombok annotations on entities and DTOs:
- `@Data` on DTOs
- `@Getter @Setter` or `@Data` on entities (be careful with `@EqualsAndHashCode` on JPA entities — use `@EqualsAndHashCode(onlyExplicitlyIncluded = true)`)
- `@RequiredArgsConstructor` on services with `final` fields (constructor injection)
- No `@AllArgsConstructor` on JPA entities

---

## 7. Documentation Standards

### Code documentation

- No Javadoc on obvious methods (getters, setters, simple CRUD).
- Add a short Javadoc comment on service interface methods that have non-trivial preconditions or side effects.
- Complex algorithms get an inline comment explaining the *why*, not the *what*.

### API documentation

When adding or modifying an endpoint, update [`docs/API_DOCUMENTATION.md`](API_DOCUMENTATION.md):

1. Add or update the endpoint section with method, path, access level, request body, and response example.
2. Add a row to the Test Results Summary table.
3. If the endpoint has known issues, add a warning admonition.

### Migration documentation

When adding a Flyway migration, add a row to the **Migration History** table in [`docs/ARCHITECTURE.md`](ARCHITECTURE.md).

### Environment variables

Every new environment variable must be:
1. Added to `.env.example` with a descriptive placeholder value.
2. Added to the **Environment Variables** table in [`README.md`](../README.md).
3. Added to the **Environment Variable** table in [`docs/DEPLOYMENT.md`](DEPLOYMENT.md).

---

## 8. Testing Requirements

### What must be tested

| Code path | Test type required |
|-----------|-------------------|
| Service business logic | Unit test with mocked repository |
| Facility scope enforcement (`enforceScope`) | Unit test — success and rejection cases |
| JWT generation and validation | Unit test |
| Security boundary (cross-facility access) | Unit test |
| Controller layer | Optional — prefer service-level tests |
| Repository custom queries | Integration test (if complex `@Query`) |

### Test structure

Mirror the main source structure:

```
src/test/java/com/motherhood/journey/
├── maternal/service/MotherServiceTest.java
├── child/service/ChildServiceTest.java
├── security/JwtUtilTest.java
└── ...
```

### Naming convention

```java
@Nested
@DisplayName("when registering a child")
class RegisterChild {

    @Test
    @DisplayName("assigns correct facility when mother exists")
    void assignsCorrectFacility() { ... }

    @Test
    @DisplayName("throws when mother not found")
    void throwsWhenMotherNotFound() { ... }
}
```

### Running tests

```bash
# All tests
./mvnw test

# Single class
./mvnw test -Dtest=ChildServiceTest

# Specific nested class
./mvnw test -Dtest="ChildServiceTest\$RegisterChild"
```

### Test data

- Use `UUID.randomUUID()` for test IDs — never hardcode UUIDs.
- Use `Mockito.mock()` or `@MockBean` — do not hit a real database in unit tests.
- Use descriptive variable names: `motherAtKimironko`, `callerHealthWorker`.

---

## 9. Issue Reporting Guidelines

Use [GitHub Issues](https://github.com/SheCanCODE-Capstone-Projects/MotherHood_Journey_Backend/issues) to report bugs, suggest features, or ask questions.

### Bug report

```markdown
**Endpoint / Area:** GET /api/v1/mothers/{id}

**Role used:** HEALTH_WORKER

**Expected behaviour:** Returns 200 with mother details

**Actual behaviour:** Returns 500 Internal Server Error

**Steps to reproduce:**
1. Register a mother via POST /api/v1/mothers
2. Authenticate as HEALTH_WORKER at the same facility
3. GET /api/v1/mothers/{id}

**Environment:** Local / Railway production

**Relevant logs or error message:**
(paste stack trace or correlation ID)
```

### Feature request

```markdown
**Problem this solves:**
<!-- What is currently impossible or painful? -->

**Proposed solution:**
<!-- Brief description of the change -->

**Affected domain / endpoint:**
<!-- e.g. scheduler, maternal, government -->

**Acceptance criteria:**
<!-- What does "done" look like? -->
```

---

## 10. Review Process

### Reviewer responsibilities

- Review within 2 working days of assignment.
- Check for business logic correctness, security implications, test coverage, and documentation completeness.
- Leave actionable comments — suggest the fix, not just the problem.
- Approve only when all checklist items are met and all comments are resolved.

### Author responsibilities

- Respond to all review comments within 1 working day.
- Do not push unrelated changes while a PR is under review.
- When all comments are resolved, re-request review.
- Do not merge without at least one approval.

### Merge strategy

- **Default:** Squash and merge — keeps `main` history linear.
- **Exception:** Merge commit for large feature branches with meaningful intermediate commits.
- Delete the branch after merge.

---

## 11. Security & Responsible Disclosure

### Sensitive data

- **Never** commit secrets, API keys, passwords, or personal data (NID numbers, phone numbers).
- `.env` is in `.gitignore` — verify before committing.
- If you accidentally commit a secret, rotate it immediately and force-push (with team lead approval).

### Security vulnerabilities

If you discover a security vulnerability:

1. **Do not open a public GitHub issue.**
2. Email the project lead directly at the IgireRwanda organisation contact.
3. Include: affected component, steps to reproduce, potential impact.
4. Allow 72 hours for acknowledgement and 14 days for a fix before public disclosure.

### PHI handling

This system processes Protected Health Information (PHI) subject to Rwanda Law No. 058/2021 on Personal Data Protection. When contributing:

- Do not log PHI (names, NID numbers, health data) — use correlation IDs in logs instead.
- Verify that `audit_log` entries are created for any new PHI access pattern.
- Ensure `GOV_DATA_SHARE` consent is checked before any data-sharing operation.
- Never write test cases using real patient data.
