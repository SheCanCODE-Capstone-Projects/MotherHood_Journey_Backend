# Deployment Guide — MotherHood Journey Backend

> IgireRwanda Organization | SheCanCode Bootcamp | Kigali, Rwanda

This guide covers deploying the application to [Railway](https://railway.app) (primary), running it via Docker Compose, and general production operations.

---

## Table of Contents

1. [Railway Deployment](#1-railway-deployment)
2. [Environment Variable Reference](#2-environment-variable-reference)
3. [Database Provisioning](#3-database-provisioning)
4. [Build & Start Commands](#4-build--start-commands)
5. [Domain Configuration](#5-domain-configuration)
6. [Secrets Management](#6-secrets-management)
7. [Database Migrations](#7-database-migrations)
8. [Health Checks](#8-health-checks)
9. [Monitoring](#9-monitoring)
10. [CI/CD Recommendations](#10-cicd-recommendations)
11. [Docker Compose Deployment](#11-docker-compose-deployment)
12. [Rollback Strategy](#12-rollback-strategy)
13. [Audit Log Partitions](#13-audit-log-partitions)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Railway Deployment

### Step-by-step

**1. Create a Railway project**

Log in at [railway.app](https://railway.app) and create a new project.

**2. Add a PostgreSQL plugin**

Inside the project, click **New** → **Database** → **PostgreSQL**. Railway provisions a PostgreSQL 16 instance and exposes the connection details as environment variables automatically.

**3. Add the application service**

Click **New** → **GitHub Repo** and connect the `MotherHood_Journey_Backend` repository. Railway detects the `Dockerfile` at the root and uses it for all builds.

**4. Set environment variables**

In the service **Variables** tab, add every variable from the [Environment Variable Reference](#2-environment-variable-reference) table. At minimum:

```
DB_HOST=postgres.railway.internal
DB_PORT=5432
DB_NAME=railway             ← Railway PostgreSQL plugin default
DB_USERNAME=postgres        ← Railway PostgreSQL plugin default
DB_PASSWORD=<from Railway plugin Variables>
JWT_SECRET=<64+ char random secret>
SPRING_PROFILES_ACTIVE=prod
CORS_ALLOWED_ORIGINS=https://your-frontend-domain.com
```

Railway injects the PostgreSQL credentials automatically into the plugin's `DATABASE_URL`. Copy the individual fields (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`) from the plugin's variable panel and map them to the variable names above.

**5. Deploy**

Push to `main`. Railway auto-deploys on every push. The first deploy takes ~3–5 minutes (Maven dependency download). Subsequent deploys are faster due to Docker layer caching.

**6. Verify**

```bash
curl https://your-railway-domain.up.railway.app/actuator/health
# Expected: {"status":"UP"}
```

---

## 2. Environment Variable Reference

All secrets must be set in the Railway dashboard. Never commit values to source control.

| Variable | Required | Description | Example / Default |
|----------|----------|-------------|-------------------|
| `APP_PORT` | No | HTTP port the server listens on | `8080` |
| `DB_HOST` | **Yes** | PostgreSQL hostname | `postgres.railway.internal` |
| `DB_PORT` | **Yes** | PostgreSQL port | `5432` |
| `DB_NAME` | **Yes** | Database name | `railway` |
| `DB_USERNAME` | **Yes** | Database user | `postgres` |
| `DB_PASSWORD` | **Yes** | Database password | *(secret)* |
| `JWT_SECRET` | **Yes** | HMAC-SHA-512 signing key — minimum 32 chars | *(secret, 64+ recommended)* |
| `JWT_EXPIRATION_MS` | No | Access token TTL in milliseconds | `86400000` (24 h) |
| `JWT_REFRESH_EXPIRATION_MS` | No | Refresh token TTL in milliseconds | `604800000` (7 days) |
| `SPRING_PROFILES_ACTIVE` | **Yes** | Active Spring profile | `prod` |
| `CORS_ALLOWED_ORIGINS` | **Yes** | Comma-separated allowed frontend origins | `https://app.motherhood.rw` |
| `AT_API_KEY` | No* | Africa's Talking API key | *(secret)* |
| `AT_USERNAME` | No* | Africa's Talking username | `motherhood` |
| `IREMBO_BASE_URL` | No* | Irembo API base URL | `https://api.irembo.gov.rw/v1` |
| `IREMBO_API_KEY` | No* | Irembo API key | *(secret)* |
| `NIDA_BASE_URL` | No* | NIDA identity verification base URL | `https://api.nida.gov.rw/v1` |
| `HMIS_BASE_URL` | No* | MoH HMIS API base URL | *(secret)* |
| `HMIS_API_KEY` | No* | MoH HMIS API key | *(secret)* |
| `ESCALATION_PENDING_HOURS` | No | Hours before a PENDING service request is escalated | `48` |

> \* Optional for startup — the application runs without these, but SMS sending, identity verification, and government sync will not function. Health check returns `UNKNOWN` for unconfigured integrations.

### Generating a JWT secret

```bash
openssl rand -base64 48
```

The output (64 characters) is safe to paste directly as `JWT_SECRET`.

---

## 3. Database Provisioning

### Railway (automatic)

Railway's PostgreSQL plugin creates the database automatically. Use the plugin's connection variables to populate `DB_*` environment variables.

### Manual (self-hosted PostgreSQL)

```sql
-- Run as postgres superuser
CREATE DATABASE motherhood_db;
CREATE USER motherhood_user WITH ENCRYPTED PASSWORD 'strongpassword';
GRANT ALL PRIVILEGES ON DATABASE motherhood_db TO motherhood_user;
```

Flyway requires the `motherhood_user` to have `CREATE` permission on the database (needed to create tables on first migration).

---

## 4. Build & Start Commands

### Railway (automatic via Dockerfile)

Railway uses the `Dockerfile` at the project root. No custom build or start commands are needed.

**Build command** (Dockerfile stage 1):
```
mvn package -DskipTests -q
```

**Start command** (Dockerfile stage 3 ENTRYPOINT):
```
java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
     -XX:InitialRAMPercentage=50.0 -XX:+UseG1GC \
     -XX:MaxGCPauseMillis=200 -XX:+UseStringDeduplication \
     -Djava.security.egd=file:/dev/./urandom \
     -Dfile.encoding=UTF-8 \
     -Dspring.profiles.active=prod \
     org.springframework.boot.loader.launch.JarLauncher
```

### Manual JAR build

```bash
./mvnw clean package -DskipTests
java -jar target/motherhood-journey-1.0.0.jar \
     --spring.profiles.active=prod
```

---

## 5. Domain Configuration

### Railway auto-generated domain

Railway assigns a domain like `motherhood-journey-production.up.railway.app`. Use this as your `CORS_ALLOWED_ORIGINS` counterpart if you need the backend to be publicly accessible.

### Custom domain

1. In the Railway service, go to **Settings** → **Domains** → **Custom Domain**.
2. Add your domain (e.g. `api.motherhood.rw`).
3. Create the DNS CNAME record your registrar as instructed by Railway.
4. TLS is provisioned automatically via Let's Encrypt.
5. Update `CORS_ALLOWED_ORIGINS` to include the frontend domain.

---

## 6. Secrets Management

| Concern | Approach |
|---------|----------|
| API keys and DB passwords | Railway environment variables — never in code |
| JWT secret | Railway environment variable — rotate by updating the variable and redeploying |
| Africa's Talking key | Railway environment variable |
| Government API keys | Railway environment variable — set when integration is activated |
| `.env` file | Local development only — in `.gitignore`, never committed |

**Rotating the JWT secret** invalidates all active tokens immediately (users must re-login). Schedule rotation during a low-traffic window.

---

## 7. Database Migrations

Flyway runs automatically on every application startup. All migrations in `src/main/resources/db/migration/` are applied in version order.

### Current migration history

| Migration | Description |
|-----------|------------|
| V1 | Full initial schema |
| V2 | Schema fixes |
| V3 | `audit_log` with monthly partitioning |
| V4 | Constraints and partition extensions |
| V5 | Appointment and notification refinements |
| V6 | Service request sequence (`SR-YYYY-NNNNN`) |
| V7 | Performance indexes |
| V8 | Health visit constraints |
| V9 | ICD-10 / HMIS seed data |
| V10 | Cancellation reason on appointments |
| V11 | `gov_sync_log` missing columns |

### Rules for new migrations

- **Never modify** an existing migration file — Flyway checksums every applied script and will refuse to start if one changes.
- New migrations follow the pattern: `V{N+1}__Short_Description.sql` where N is the current highest version.
- Use `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, and `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` for safety.
- Always add a SQL comment at the top explaining the purpose.

### Running Flyway manually (info / repair)

```bash
./mvnw flyway:info \
  -Dflyway.url=jdbc:postgresql://localhost:5432/motherhood_db \
  -Dflyway.user=postgres \
  -Dflyway.password=yourpassword

# If a migration is marked as failed and was already partially applied:
./mvnw flyway:repair \
  -Dflyway.url=jdbc:postgresql://localhost:5432/motherhood_db \
  -Dflyway.user=postgres \
  -Dflyway.password=yourpassword
```

---

## 8. Health Checks

### Application health endpoint

```
GET /actuator/health
```

Returns `{"status":"UP"}` when healthy. Exposed publicly — no authentication required.

Full response includes component statuses:

```json
{
  "status": "UP",
  "components": {
    "db":             { "status": "UP" },
    "africasTalking": { "status": "UNKNOWN", "details": { "reason": "AT_API_KEY not configured" } },
    "nidaApi":        { "status": "UNKNOWN", "details": { "reason": "NIDA_BASE_URL not configured" } },
    "diskSpace":      { "status": "UP" },
    "ping":           { "status": "UP" }
  }
}
```

`UNKNOWN` status on integrations is expected when the corresponding environment variable is not set. The application functions normally without them.

### Railway health check configuration

Railway is configured to call `GET /actuator/health` every 30 seconds. The application has a `start_period` of 90 seconds to allow Flyway migrations and JVM warmup before health checks begin.

The Dockerfile `HEALTHCHECK` directive:

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
```

---

## 9. Monitoring

### Railway logs

Railway streams `stdout`/`stderr` in real time. The application uses JSON-structured logging in production (`application-prod.yml` sets all levels to `INFO` or higher — no DEBUG, no SQL logging).

**Key log patterns to watch:**

| Pattern | Meaning |
|---------|---------|
| `PARTITION CHECK: Ensure 'audit_log_YYYY_MM' exists` | Monthly audit log partition is missing — add it (see [section 13](#13-audit-log-partitions)) |
| `GovSync DEAD_LETTER` | Government API call failed 5 times — investigate `gov_sync_log` table |
| `Flyway migration ... failed` | Schema migration failed on startup — check migration SQL and Flyway repair |
| `BeanCreationException` | Spring context failed to start — check all required environment variables are set |

### Actuator endpoints

| Endpoint | Access | Purpose |
|----------|--------|---------|
| `/actuator/health` | Public | Application and component status |
| `/actuator/info` | Authenticated | Build version info |
| `/actuator/metrics` | Authenticated | JVM and application metrics |

### Recommended external monitoring

- **Uptime monitoring:** Configure a ping monitor on `/actuator/health` with 1-minute intervals.
- **Error alerting:** Set up Railway log alerts for `ERROR` and `WARN` patterns.
- **Database monitoring:** Monitor PostgreSQL connection count and query latency — Hikari pool is set to max 5 connections.

---

## 10. CI/CD Recommendations

### Current state

Railway auto-deploys on every push to `main`. No separate CI pipeline is configured.

### Recommended pipeline (GitHub Actions)

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'

      - name: Build and test
        run: ./mvnw verify

      - name: Checkstyle
        run: ./mvnw checkstyle:check
```

Add this workflow at `.github/workflows/ci.yml`. Railway deployment continues to trigger automatically on push to `main` after CI passes.

---

## 11. Docker Compose Deployment

For self-hosted or staging environments, use Docker Compose:

```bash
# 1. Configure environment
cp .env.example .env
# Edit .env with production values

# 2. Build and start
docker compose up --build -d

# 3. View logs
docker compose logs -f app

# 4. Health check
curl http://localhost:8080/actuator/health

# 5. Stop
docker compose down
```

**Note:** The `app` service waits for `postgres` to be healthy before starting (`depends_on: condition: service_healthy`). On first start, Flyway runs all migrations automatically.

### Updating the application

```bash
git pull origin main
docker compose up --build -d app
```

Docker layer caching means only the `application` layer (your code) is rebuilt; dependencies are cached.

---

## 12. Rollback Strategy

### Railway rollback

Railway retains deployment history. To roll back:

1. Go to **Deployments** in the Railway service.
2. Find the last successful deployment.
3. Click **Redeploy** on that deployment.

Railway rolls back the container image instantly. Database migrations are **not** rolled back automatically — see below.

### Database migration rollback

Flyway Community does not support automatic rollback. To undo a migration:

1. Write a compensating migration: `V{N+1}__Revert_description.sql`
2. Deploy — Flyway applies the compensating migration on startup.

> **Never** delete or modify an already-applied migration file. Flyway will refuse to start.

### Emergency rollback checklist

1. Identify the last known-good Git SHA: `git log --oneline main`
2. Deploy that SHA via Railway (redeploy from history) or build and push the specific image.
3. If the new migration caused data issues, apply the compensating migration.
4. Notify affected users if downtime exceeded the health check SLA.

---

## 13. Audit Log Partitions

`audit_log` is a range-partitioned table (partitioned by `created_at` month). Partitions must be created in advance — the application logs a `WARNING` on the 1st of each month if the next month's partition is missing.

### Adding a new monthly partition

```sql
CREATE TABLE audit_log_2028_01 PARTITION OF audit_log
    FOR VALUES FROM ('2028-01-01') TO ('2028-02-01');
```

**Pattern:** `audit_log_YYYY_MM` with `FROM ('YYYY-MM-01') TO ('YYYY-{MM+1}-01')`.

For December: `FROM ('2028-12-01') TO ('2029-01-01')`.

### Pre-creating partitions for the year

```sql
-- Run at the start of each year (adjust year as needed)
DO $$
DECLARE
    y int := 2029;
    m int;
BEGIN
    FOR m IN 1..12 LOOP
        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS audit_log_%s_%s PARTITION OF audit_log '
            'FOR VALUES FROM (''%s-%s-01'') TO (''%s-%s-01'')',
            y, lpad(m::text, 2, '0'),
            y, lpad(m::text, 2, '0'),
            CASE WHEN m = 12 THEN y + 1 ELSE y END,
            CASE WHEN m = 12 THEN '01' ELSE lpad((m + 1)::text, 2, '0') END
        );
    END LOOP;
END $$;
```

---

## 14. Troubleshooting

### Application fails to start

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `BeanCreationException: Error creating bean 'jwtUtil'` | `JWT_SECRET` not set or too short (< 32 chars) | Set `JWT_SECRET` to a 64+ char random string |
| `PSQLException: FATAL: password authentication failed` | Wrong `DB_PASSWORD` or `DB_USERNAME` | Verify credentials against the Railway PostgreSQL plugin |
| `FlywayException: Validate failed` | An applied migration file was modified | Run `flyway:repair` and investigate which migration changed |
| `Port 8080 already in use` | Another process is using the port | Change `APP_PORT` env var or stop the conflicting process |
| `seq_mother_health_id does not exist` | Missing Flyway migration for the sequence | Add `V12__add_mother_health_id_sequence.sql` (see BUGS_AND_FIXES.md Bug #4) |

### API returns 403 for admin roles

The URL matchers in `SecurityConfig.java` may be too restrictive. Check Bug #2 in [`docs/BUGS_AND_FIXES.md`](BUGS_AND_FIXES.md) for the exact fix.

### Africa's Talking SMS not sending

1. Verify `AT_API_KEY` and `AT_USERNAME` are set correctly.
2. Check `GET /actuator/health` — `africasTalking` component should show `UP`, not `UNKNOWN`.
3. Query the `sms_notifications` table — rows with `status=FAILED` have error details.
4. In sandbox mode, AT only delivers to whitelisted numbers.

### Government sync stuck in PENDING

1. Check `gov_sync_log` table for rows with `status=PENDING` or `FAILED`.
2. Verify the relevant API base URL and key are set (`IREMBO_BASE_URL`, `NIDA_BASE_URL`, etc.).
3. Check Railway logs for `GovSync` error messages.
4. Rows marked `DEAD_LETTER` after 5 retries require manual investigation.

### Swagger UI inaccessible in production

Swagger UI is intentionally disabled in production (`application-prod.yml`). Access it only in `local` profile:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
# Then: http://localhost:8080/swagger-ui/index.html
```