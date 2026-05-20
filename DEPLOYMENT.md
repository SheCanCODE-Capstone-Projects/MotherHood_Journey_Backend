# Deployment Guide — MotherHood Journey Backend

## Required Environment Variables

| Variable | Description | Example |
|---|---|---|
| `APP_PORT` | HTTP port the app listens on | `8080` |
| `DB_HOST` | PostgreSQL host | `postgres.railway.internal` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `motherhood_db` |
| `DB_USERNAME` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | *(secret)* |
| `JWT_SECRET` | JWT signing secret — **minimum 32 characters** | *(secret, 64+ chars recommended)* |
| `JWT_EXPIRATION_MS` | Token TTL in milliseconds | `86400000` (24h) |
| `AT_API_KEY` | Africa's Talking API key | *(secret)* |
| `AT_USERNAME` | Africa's Talking username | `motherhood` |
| `IREMBO_BASE_URL` | Irembo API base URL | `https://api.irembo.gov.rw` |
| `IREMBO_API_KEY` | Irembo API key | *(secret)* |
| `NIDA_BASE_URL` | NIDA identity verification API base URL | `https://api.nida.gov.rw` |
| `ESCALATION_PENDING_HOURS` | Hours before a PENDING SR is escalated | `48` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed frontend origins | `https://app.motherhood.rw` |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |

## Railway Deployment Steps

1. Create a new Railway project and add a PostgreSQL plugin.
2. Set all environment variables above in the Railway dashboard under **Variables**.
3. Set `SPRING_PROFILES_ACTIVE=prod` to activate production logging.
4. Connect your GitHub repository — Railway auto-deploys on push to `main`.
5. Railway uses the `Dockerfile` at the project root for the build.

## Production Profile

Set `SPRING_PROFILES_ACTIVE=prod` to activate `application-prod.yml` which:
- Sets all log levels to INFO/WARN (no DEBUG, no SQL logging)
- Prevents PHI from appearing in Railway logs

## Database Migrations

Flyway runs automatically on startup. Migrations are in:
```
src/main/resources/db/migration/
  V1__Initial_Schema.sql
  V2__Schema_Fixes.sql
  V3__Audit_Log_Partitioning.sql
  V4__Constraints_And_Partitions.sql
  V5__Appointments_And_Notifications.sql
  V6__SR_Sequence.sql
  V7__Performance_Indexes.sql
```

**Never edit existing migration files.** Always add a new `V{n}__Description.sql`.

## Health Check

Railway health check endpoint: `GET /actuator/health`

This endpoint is public and returns `{"status":"UP"}` when the app is healthy.

## JWT Secret Requirements

The JWT secret **must be at least 32 characters** (256 bits). The app will fail to start with a shorter secret. Use a strong random value:

```bash
openssl rand -base64 48
```

## Audit Log Partitions

`V3__Audit_Log_Partitioning.sql` creates monthly partitions through August 2026.
`V4__Constraints_And_Partitions.sql` extends them through December 2027.

Add new partitions monthly by running:

```sql
CREATE TABLE audit_log_YYYY_MM PARTITION OF audit_log
    FOR VALUES FROM ('YYYY-MM-01') TO ('YYYY-MM+1-01');
```

Example for 2028-01:
```sql
CREATE TABLE audit_log_2028_01 PARTITION OF audit_log
    FOR VALUES FROM ('2028-01-01') TO ('2028-02-01');
```

The application logs a WARNING on the 1st of each month if the next partition is approaching.
Monitor Railway logs for: `PARTITION CHECK: Ensure 'audit_log_YYYY_MM' exists`
