# Mini WSA — Security Analytics Pipeline

A backend service that ingests security event logs (DLRs), enriches them with attack classification
and threat scoring, stores them, and exposes REST analytics APIs — built as a simplified version of
Akamai's Web Security Analytics platform.

> **DLR — Data Log Record:** A structured log entry produced by a web security gateway for every
> inspected HTTP request. Each DLR captures the request metadata, the matched security rule, the
> enforcement action taken, and geo/client context.

---

## Table of Contents
1. [Build & Run](#build--run)
2. [API Documentation](#api-documentation)
3. [Data Generator](#data-generator)
4. [Logging](#logging)
5. [Architecture & Technology](#architecture--technology)
6. [Testing](#testing)
7. [Feature Deep Dive](#feature-deep-dive)
8. [Tool Installation](#tool-installation)

---

## Build & Run

### What you need
- Git
- [Java 21+](#java-21-jdk)
- [Maven 3.8+](#maven-38)
- [PostgreSQL 15+](#postgresql-15)

Verify:
```bash
java -version    # must show 21 or later
mvn -version     # must show 3.8+
psql --version   # must show 15+
```

### Step 1 — Clone the repository

```bash
git clone https://github.com/avichaiogen/mini-wsa.git
cd mini-wsa
```

### Step 2 — Start PostgreSQL

**Linux (systemd-based distros — Ubuntu, Debian, Fedora):**
```bash
sudo systemctl start postgresql
```

**macOS (Homebrew):**
```bash
brew services start postgresql@<version>
```

**Windows:**
PostgreSQL runs as a Windows Service after installation. Start it from **Services** (`services.msc`),
from **pgAdmin 4**, or via PowerShell as Administrator:
```powershell
Start-Service -Name postgresql-x64-<version>
```

> Replace `<version>` with your installed PostgreSQL version (e.g. `15`, `16`, `17`).

### Step 3 — Create the database (first time only)

The commands below use `miniwsa` / `miniwsa` as the username and password. Replace them with your chosen credentials if different — you will set the same values in `.env` in the next step.

**Linux:**
```bash
sudo -u postgres psql -c "CREATE DATABASE miniwsa;"
sudo -u postgres psql -c "CREATE USER miniwsa WITH PASSWORD 'miniwsa';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE miniwsa TO miniwsa;"
sudo -u postgres psql -d miniwsa -c "GRANT ALL ON SCHEMA public TO miniwsa;"
```

**macOS:**
```bash
psql postgres -c "CREATE DATABASE miniwsa;"
psql postgres -c "CREATE USER miniwsa WITH PASSWORD 'miniwsa';"
psql postgres -c "GRANT ALL PRIVILEGES ON DATABASE miniwsa TO miniwsa;"
psql postgres -d miniwsa -c "GRANT ALL ON SCHEMA public TO miniwsa;"
```

**Windows (run from the PostgreSQL `bin` directory, or use pgAdmin 4):**
```cmd
psql -U postgres -c "CREATE DATABASE miniwsa;"
psql -U postgres -c "CREATE USER miniwsa WITH PASSWORD 'miniwsa';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE miniwsa TO miniwsa;"
psql -U postgres -d miniwsa -c "GRANT ALL ON SCHEMA public TO miniwsa;"
```

> **Why the last command?** PostgreSQL 15+ revoked the default CREATE privilege on the `public` schema. Without it, Flyway cannot create its schema history table and the app will fail to start. The `-d miniwsa` flag is required — this grant must run against the app database, not the default `postgres` database.

### Step 4 — Configure environment

Copy the `.env.example` template and fill in the credentials you chose in Step 3:

```bash
cp .env.example .env
# open .env and set DB_USERNAME and DB_PASSWORD to match Step 3
```

| Variable | Default value | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/miniwsa` | JDBC connection string |
| `DB_USERNAME` | `miniwsa` | PostgreSQL login |
| `DB_PASSWORD` | `miniwsa` | PostgreSQL password |

If you used the example commands in Step 3 unchanged, the defaults already match — no edits needed.

Then export the variables:

**Linux / macOS:**
```bash
set -a; source .env; set +a
```

**Windows CMD:**
```cmd
for /f "tokens=1,2 delims==" %i in (.env) do set %i=%j
```

**Windows PowerShell:**
```powershell
Get-Content .env | ForEach-Object { $k,$v = $_ -split '=',2; [System.Environment]::SetEnvironmentVariable($k,$v) }
```

#### Override DB credentials without editing files

To connect to a different database without touching `.env`:

**Linux / macOS:**
```bash
DB_URL=jdbc:postgresql://myhost:5432/mydb \
DB_USERNAME=myuser \
DB_PASSWORD=mypass \
java -jar target/mini-wsa.jar
```

**Windows PowerShell:**
```powershell
$env:DB_URL="jdbc:postgresql://myhost:5432/mydb"; $env:DB_USERNAME="myuser"; $env:DB_PASSWORD="mypass"; java -jar target/mini-wsa.jar
```

### Step 5 — Build
```bash
mvn clean package -DskipTests
```

### Step 6 — Test
```bash
mvn test
```

Make sure all tests pass before running the app.

### Step 7 — Run
```bash
java -jar target/mini-wsa.jar
```

The app starts on `http://localhost:8080`.

> **Note:** `set -a; source .env; set +a` (or the Windows equivalent) only applies to the current terminal session.
> Re-run it if you open a new terminal before starting the app. Alternatively, set the variables permanently in your OS
> environment (System Properties → Environment Variables on Windows; `/etc/environment` on Linux).

---

## API Documentation

> **Windows:** Replace `curl` with `curl.exe` in GET commands. For POST, use the dedicated PowerShell example below.

### POST /v1/events/ingest — Ingest Events

Accepts a single event or a batch (array). Validates all fields, enriches with attack type and
threat score, and persists. Batch is all-or-nothing — one invalid event rejects the entire batch.

**Single event (Linux / macOS):**
```bash
curl -s -X POST http://localhost:8080/v1/events/ingest \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "evt-001",
    "timestamp": "2026-05-20T14:32:10Z",
    "configId": 14227,
    "policyId": "pol_web1",
    "clientIp": "203.0.113.42",
    "hostname": "www.example.com",
    "path": "/api/v1/login",
    "method": "POST",
    "statusCode": 403,
    "userAgent": "Mozilla/5.0",
    "rule": {
      "id": "950001",
      "name": "SQL_INJECTION",
      "message": "SQL Injection Attack Detected",
      "severity": "CRITICAL",
      "category": "INJECTION"
    },
    "action": "DENY",
    "geoLocation": { "country": "CN", "city": "Beijing" },
    "requestSize": 1024,
    "responseSize": 256
  }'
```

**Single event (Windows PowerShell):**

```powershell
curl.exe -s -X POST http://localhost:8080/v1/events/ingest -H "Content-Type: application/json" -d "{ \`"eventId\`": \`"evt-001\`", \`"timestamp\`": \`"2026-05-20T14:32:10Z\`", \`"configId\`": 14227, \`"policyId\`": \`"pol_web1\`", \`"clientIp\`": \`"203.0.113.42\`", \`"hostname\`": \`"www.example.com\`", \`"path\`": \`"/api/v1/login\`", \`"method\`": \`"POST\`", \`"statusCode\`": 403, \`"userAgent\`": \`"Mozilla/5.0\`", \`"rule\`": { \`"id\`": \`"950001\`", \`"name\`": \`"SQL_INJECTION\`", \`"message\`": \`"SQL Injection Attack Detected\`", \`"severity\`": \`"CRITICAL\`", \`"category\`": \`"INJECTION\`" }, \`"action\`": \`"DENY\`", \`"geoLocation\`": { \`"country\`": \`"CN\`", \`"city\`": \`"Beijing\`" }, \`"requestSize\`": 1024, \`"responseSize\`": 256 }"
```

**Batch:**
```bash
curl -s -X POST http://localhost:8080/v1/events/ingest \
  -H "Content-Type: application/json" \
  -d '[{ ...event1... }, { ...event2... }]'
```

| Response | Meaning |
|---|---|
| `201 Created` | Stored successfully |
| `400 Bad Request` | Validation failure — error details in response body |

---

### GET /v1/stats/summary — Statistics

Returns aggregated statistics for a time range. All parameters are optional.

```bash
curl "http://localhost:8080/v1/stats/summary?configId=14227&from=2026-05-01T00:00:00Z&to=2026-05-31T23:59:59Z"
```

```json
{
  "configId": 14227,
  "timeRange": { "from": "...", "to": "..." },
  "totalEvents": 1523,
  "byCategory": {
    "INJECTION": { "count": 450, "avgThreatScore": 72.3 }
  },
  "byAction": { "DENY": 890, "ALERT": 433, "MONITOR": 200 },
  "topAttackers": [
    { "clientIp": "203.0.113.42", "count": 87, "avgThreatScore": 81.2 }
  ],
  "topTargetedPaths": [
    { "path": "/api/v1/login", "count": 234 }
  ]
}
```

If `configId` is omitted, aggregates across all configurations.
`topAttackers` and `topTargetedPaths` return the top 10 each.

---

### GET /v1/events/samples — Event Samples

Returns individual enriched event records with filtering and pagination.

```bash
curl "http://localhost:8080/v1/events/samples?category=INJECTION&action=DENY&limit=5&offset=0"
```

| Parameter | Default | Notes |
|---|---|---|
| `configId` | — | Optional filter |
| `from` / `to` | — | ISO-8601 time range |
| `category` | — | One of the 7 attack categories |
| `action` | — | DENY, ALERT, or MONITOR |
| `limit` | 20 | Max 100 |
| `offset` | 0 | For pagination |

Response includes a `total` field for building pagination UI.
Results sorted by `timestamp` descending (newest first).

---

## Data Generator

A standalone tool (no Spring context required) that generates realistic synthetic security events
and optionally POSTs them to the ingestion API. Useful for populating a fresh database, load
testing, or sharing a reproducible dataset with teammates.

### Arguments

| Argument | Default | Description |
|---|---|---|
| `--count=N` | `1000` | Total number of events to generate |
| `--batch-size=N` | `100` | Events per output JSON file |
| `--output-dir=DIR` | `generated-events` | Directory to write files into |
| `--waves=N` | `3` | Attack waves — bursts of events sharing a fixed IP, path, and configId |
| `--wave-size=N` | `10` | Events per wave |
| `--ingest=URL` | *(none)* | Base URL of a running server; when set, each file is POSTed to `POST /v1/events/ingest` |

### How to run

If you haven't built the project yet, run `mvn clean package -DskipTests` first.

Run via Maven's exec plugin:
```bash
mvn exec:java -Dexec.args="<args>"
```

> **Windows (CMD & PowerShell):** Quote the *entire* `-D` argument to prevent the shell from
> splitting it. Use double quotes around `-Dexec.args=<args>` instead of around just the value:
> ```
> mvn exec:java "-Dexec.args=--count=500"
> mvn exec:java "-Dexec.args=--count=1000 --ingest=http://localhost:8080"
> ```

> **Important:** Without `--ingest`, files are written to disk only — nothing is sent to the
> database. Always include `--ingest=<url>` when you want data in the DB.
> `--ingest` only POSTs the files created in the **current run** — files from previous runs
> that already exist in the output directory are not re-ingested.

### Examples

**Generate 500 events to disk (inspect files before ingesting):**
```bash
mvn exec:java -Dexec.args="--count=500"
```

**Generate 1000 events and ingest to a local server:**
```bash
mvn exec:java -Dexec.args="--count=1000 --ingest=http://localhost:8080"
```

**Custom batch size and output directory:**
```bash
mvn exec:java -Dexec.args="--count=2000 --batch-size=200 --output-dir=my-events --ingest=http://localhost:8080"
```

**Simulate attack waves (5 waves of 20 events each, then random fill to 500):**
```bash
mvn exec:java -Dexec.args="--count=500 --waves=5 --wave-size=20 --ingest=http://localhost:8080"
```

### Output

The generator always prints a per-configId statistics table before writing any files:

```
=== Data Generator Statistics ===
Total events generated: 1000

ConfigId   | Count | Min Timestamp        | Max Timestamp        | Severity Distribution                    | Top Action
-------------------------------------------------------------------------------------------------------------------
5512       |   198 | 2020-03-12T08:41:00Z | 2026-04-22T17:03:00Z | CRIT:48   HIGH:51   MED:52   LOW:47   | DENY(71)
8801       |   201 | 2020-07-04T11:22:00Z | 2026-05-01T09:14:00Z | CRIT:52   HIGH:49   MED:53   LOW:47   | ALERT(68)
...

Generated 1000 events → generated-events/ (10 files of up to 100 each)
```

When `--ingest` is specified, each file is POSTed and results are shown individually, followed
by a summary line:

```
Ingesting 10 files to http://localhost:8080 …
  [OK  ] events_000.json → 201
  [OK  ] events_001.json → 201
  [FAIL] events_002.json → 400
Ingested: 9 OK, 1 FAILED
```

### File format

Output files are named `events_NNN.json` (3-digit zero-padded index, e.g. `events_000.json`,
`events_001.json`). Repeated runs append new files — existing files are never overwritten. Each
file contains a JSON array ready to POST directly to `POST /v1/events/ingest`.

---

## Logging

The app writes logs to both the console and a rolling file.

### Configuration

Log level and file path are set in `application.yml` — **never edit `log4j2-spring.xml`** for routine changes:

```yaml
logging:
  level:
    com.akamai.miniwsa: INFO   # switch to DEBUG for verbose enrichment traces
  file:
    name: logs/mini-wsa.log
```

### Rolling file policy

- Max file size: **100 MB** (triggers a mid-day roll if needed)
- Daily rollover at midnight
- Max **30** files kept; older files are deleted automatically
- Rolled files are gzip-compressed: `logs/mini-wsa.log.2026-05-25.1.gz`

### Log levels in practice

| Level | What you see |
|---|---|
| `INFO` (default) | Controller: `POST /v1/events/ingest: batch size=N`; service: `Ingesting batch: count=N`, `Batch persisted: count=N` |
| `DEBUG` | Per-event enrichment: `Enriched event: eventId=…, attackType=…, threatScore=…`; query params for Samples and Stats; classifier and scorer outputs |
| `WARN` | All 4xx errors handled by `GlobalExceptionHandler` |
| `ERROR` | 5xx errors — `IngestionBatchException`, unhandled exceptions (with stack trace) |

### OWASP note

`clientIp`, `userAgent`, and `path` values are **never logged** — only counts, IDs, enums, and computed scores appear in log output.

### Manual verification steps

1. Start the app and POST a batch: confirm `INFO Ingesting batch: count=N` appears in both console and `logs/mini-wsa.log`.
2. Change `logging.level.com.akamai.miniwsa: DEBUG` in `application.yml` and restart: confirm `DEBUG Enriched event:` lines appear.
3. Change back to `INFO` and restart: confirm debug lines are gone.
4. To verify rollover: temporarily set `<SizeBasedTriggeringPolicy size="1KB"/>` in `log4j2-spring.xml`, send a few requests, and confirm gzip-compressed rolled files appear.

---

## Architecture & Technology

```
POST /v1/events/ingest
        │
        ▼
 IngestionController
        │
        ▼
 IngestionService
  ├── validate()          ← Bean Validation + OWASP input rules
  ├── AttackClassifier    (category → human-readable attackType)
  ├── ThreatScoreEngine   (computes score 0–100)
  └── SecurityEventRepository.save()
        │
        ▼
   PostgreSQL
        │
        ├── GET /v1/stats/summary  ← StatsService   (SQL GROUP BY aggregations)
        └── GET /v1/events/samples ← SamplesService (filter + paginate)
```

### Technologies

| Component | Choice | Notes |
|---|---|---|
| Language | Java 21 | LTS release |
| Framework | Spring Boot 4.0 | REST, JPA, validation, auto-configuration |
| Storage | PostgreSQL | Relational fit for aggregation queries and ACID batch ingestion |
| Migrations | Flyway | Versioned, reproducible schema evolution |
| Logging | SLF4J + Log4j2 | Async appenders; decoupled facade |
| Testing | JUnit 5 + Mockito + H2 | Unit tests with mocked repos; integration tests with in-memory DB |

**Why PostgreSQL?**

- **Fixed schema** — Every event has the same fields; MongoDB's document flexibility is a liability here, not an asset. A strict schema means the database itself rejects malformed events as a second line of defense after Bean Validation.

- **SQL aggregations** — The stats API's `GROUP BY`, `COUNT`, and `AVG` queries are SQL's native strength — cleaner and more readable than MongoDB's equivalent multi-stage aggregation pipeline.

- **ACID guarantees** *(Atomic, Consistent, Isolated, Durable)* — Atomicity makes all-or-nothing batch ingest a single `@Transactional` call; Isolation prevents the repeat-offender COUNT/insert race under concurrent requests from the same IP. Both require complex workarounds in MongoDB.

- **Scaling path** — TimescaleDB (a drop-in PostgreSQL extension) adds time partitioning on `received_at` with no schema or code changes — the natural upgrade when data grows to hundreds of millions of rows. At big-data volumes: Kafka decouples write throughput and ClickHouse replaces the read path for columnar analytics across billions of events.

## What I Would Improve With More Time

- **Authentication & authorisation** — the ingest endpoint currently accepts events from any caller with no identity verification. In production, `POST /v1/events/ingest` should require an API key or JWT so only trusted sources can write data, and the read endpoints (`GET /v1/stats/summary`, `GET /v1/events/samples`) should be role-scoped to prevent unauthorised access to analytics data.
- **`docker-compose.yml`** — a compose file bundling the app and PostgreSQL would reduce setup from a multi-step manual process to a single `docker compose up`, making it easier to run locally and in CI.

---

## What Was Challenging

- **First project in Java, Spring Boot, and PostgreSQL** — this was my first end-to-end project using all three technologies together. Getting up to speed with Spring Boot's auto-configuration, JPA entity mapping, Flyway migrations, and PostgreSQL driver behaviour simultaneously — while designing a clean, working analytics pipeline — required significant learning investment and was the main challenge throughout the project.

---

## Testing

### Run all tests
```bash
mvn test
```

### Test structure

| Test class | Type | What it covers |
|---|---|---|
| `AttackClassifierTest` | Unit | All 7 category → attackType mappings |
| `ThreatScoreEngineTest` | Unit | All severity/action combos; path bonus; repeat-offender bonus; cap at 100 |
| `IngestionServiceTest` | Unit | Validation errors per field; batch rejection; enrichment called |
| `StatsServiceTest` | Unit | Aggregation logic; configId filter; cross-config aggregation |
| `SamplesServiceTest` | Unit | Pagination defaults; max limit; filter combinations |
| `GlobalExceptionHandlerTest` | Unit | All HTTP error mappings: 400, 404, 405, 415, 500 |
| `DataGeneratorTest` | Unit | Event generation, file writing, batch ingestion logic |
| `IpAddressValidatorTest` | Unit | IPv4 and IPv6 format validation |
| `HttpMethodValidatorTest` | Unit | Allowed HTTP methods; case-insensitive |
| `StatusCodeValidatorTest` | Unit | Valid HTTP status code range (100–599) |
| `NoInjectionValidatorTest` | Unit | SQL injection, XSS, and path traversal pattern detection |

Unit tests use `@ExtendWith(MockitoExtension.class)` — no database required.

---

## Feature Deep Dive

### Attack Classification

Every ingested event is mapped from its raw `rule.category` to a human-readable `attackType`:

| `rule.category` | `attackType` |
|---|---|
| INJECTION | SQL/Command Injection |
| XSS | Cross-Site Scripting |
| PROTOCOL_VIOLATION | Protocol Anomaly |
| DATA_LEAKAGE | Data Exfiltration |
| BOT | Bot Activity |
| DOS | Denial of Service |
| RATE_LIMIT | Rate Limiting |

### Threat Score Calculation

Each event receives an integer threat score from 0 to 100, computed as:

| Factor | Points |
|---|---|
| `severity` = CRITICAL | +40 |
| `severity` = HIGH | +30 |
| `severity` = MEDIUM | +20 |
| `severity` = LOW | +10 |
| `action` = DENY | +20 |
| `action` = ALERT | +10 |
| `action` = MONITOR | +0 |
| `path` contains `/admin` or `/login` | +15 |
| Repeat offender (see below) | +15 |
| Cap | 100 |

### Repeat-Offender Detection

When an event is ingested, the system counts how many prior events from the same `clientIp`
have been received in the last 10 minutes (using server-side `receivedAt`, not client `timestamp`).
If that count is 5 or more, the event is considered a repeat-offender attack and receives +15.

The check is a single indexed SQL query:
```sql
SELECT COUNT(*) FROM security_events
WHERE client_ip = ? AND received_at >= NOW() - INTERVAL '10 minutes'
```

A compound index on `(client_ip, received_at)` makes this fast even under high load.

**Known trade-off:** Under high concurrency, two simultaneous requests from the same IP at exactly
the count-5 boundary may both miss the bonus. This is accepted as a minor approximation — the
threat score is a heuristic signal. At production scale, a Redis sorted set with atomic Lua script
would eliminate the race entirely.

### Input Validation & Security (OWASP)

All incoming fields are validated before processing:
- Required fields enforced with `@NotNull` / `@NotBlank`
- Enum fields (`severity`, `category`, `action`) validated strictly — unknown values return 400
- String fields capped in length to prevent oversized payload attacks
- Numeric fields (`requestSize`, `responseSize`) must be non-negative
- Custom validators enforce format rules: `@ValidIpAddress` (IPv4/IPv6), `@ValidHttpMethod` (7 methods), `@ValidStatusCode` (100–599), `@NoInjection` (blocks SQL, XSS, and path traversal patterns)
- Error responses never expose internal details, stack traces, or database errors
- All queries use JPA parameterized statements — SQL injection is not possible

---

## Tool Installation

### Java 21+ JDK

> Replace `<version>` with your target Java version (21 or later, e.g. `21`, `23`, `24`).

**Linux — Ubuntu / Debian:**
```bash
sudo apt update && sudo apt install -y openjdk-<version>-jdk
```

**Linux — Fedora / RHEL / CentOS Stream:**
```bash
sudo dnf install java-<version>-openjdk-devel
```

**macOS (Homebrew):**
```bash
brew install openjdk@<version>
# Add to shell profile so the JDK is picked up:
echo 'export JAVA_HOME=$(brew --prefix openjdk@<version>)' >> ~/.zprofile
source ~/.zprofile
```

**Windows (winget):**
```powershell
winget install Microsoft.OpenJDK.<version>
```
Or download the installer from [Adoptium](https://adoptium.net) and follow the wizard.

---

### Maven 3.8+

**Linux — Ubuntu / Debian:**
```bash
# Option A — SDKMAN (cross-platform, manages versions)
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install maven

# Option B — apt (version varies by distro; verify with mvn -version afterward)
sudo apt install -y maven
```

**Linux — Fedora / RHEL / CentOS Stream:**
```bash
sudo dnf install maven
```

**macOS (Homebrew):**
```bash
brew install maven
```

**Windows:**
Download the binary ZIP from [maven.apache.org](https://maven.apache.org/download.cgi), unzip it,
and add `<unzip-dir>/bin` to your `PATH`.

---

### PostgreSQL 15+

> Replace `<version>` with your target PostgreSQL version (15 or later, e.g. `15`, `16`, `17`).

**Linux — Ubuntu / Debian** (official PostgreSQL apt repository):
```bash
sudo apt install -y curl ca-certificates
sudo install -d /usr/share/postgresql-common/pgdg
curl -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc --fail \
  https://www.postgresql.org/media/keys/ACCC4CF8.asc
. /etc/os-release
echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] \
  https://apt.postgresql.org/pub/repos/apt ${VERSION_CODENAME}-pgdg main" \
  | sudo tee /etc/apt/sources.list.d/pgdg.list
sudo apt update && sudo apt install -y postgresql-<version>
```

**Linux — Fedora / RHEL / CentOS Stream:**
```bash
sudo dnf install postgresql<version>-server
sudo /usr/pgsql-<version>/bin/postgresql-<version>-setup initdb
sudo systemctl enable postgresql-<version>
```

**macOS (Homebrew):**
```bash
brew install postgresql@<version>
brew services start postgresql@<version>
```

**Windows:**
Download the interactive installer from [postgresql.org/download/windows](https://www.postgresql.org/download/windows/)
and run it. The installer creates the `postgres` superuser and registers a Windows Service.
