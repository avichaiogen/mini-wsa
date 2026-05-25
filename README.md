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
- [Java 21](#java-21-jdk)
- [Maven 3.8+](#maven-38)
- [PostgreSQL 15+](#postgresql-15)

Verify:
```bash
java -version    # must show 21.x
mvn -version     # must show 3.8+
psql --version   # must show 15+
```

### Step 1 — Start PostgreSQL

**Linux (systemd-based distros — Ubuntu, Debian, Fedora):**
```bash
sudo systemctl start postgresql
```

**macOS (Homebrew):**
```bash
brew services start postgresql@15
```

**Windows:**
PostgreSQL runs as a Windows Service after installation. Start it from **Services** (`services.msc`),
from **pgAdmin 4**, or via PowerShell as Administrator:
```powershell
Start-Service -Name postgresql-x64-15
```

### Step 2 — Create the database (first time only)

**Linux:**
```bash
sudo -u postgres psql -c "CREATE DATABASE miniwsa;"
sudo -u postgres psql -c "CREATE USER miniwsa WITH PASSWORD 'miniwsa';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE miniwsa TO miniwsa;"
```

**macOS:**
```bash
psql postgres -c "CREATE DATABASE miniwsa;"
psql postgres -c "CREATE USER miniwsa WITH PASSWORD 'miniwsa';"
psql postgres -c "GRANT ALL PRIVILEGES ON DATABASE miniwsa TO miniwsa;"
```

**Windows (run from the PostgreSQL `bin` directory, or use pgAdmin 4):**
```cmd
psql -U postgres -c "CREATE DATABASE miniwsa;"
psql -U postgres -c "CREATE USER miniwsa WITH PASSWORD 'miniwsa';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE miniwsa TO miniwsa;"
```

### Step 3 — Configure credentials via `.env`

`.env.example` is a template showing the three environment variables the app reads at startup:

| Variable | Default value | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/miniwsa` | JDBC connection string |
| `DB_USERNAME` | *(your DB user)* | PostgreSQL login |
| `DB_PASSWORD` | *(your DB password)* | PostgreSQL password |

Copy the template and fill in your credentials:
```bash
cp .env.example .env
# open .env and set DB_USERNAME and DB_PASSWORD to match what you used in Step 2
```

Then export the variables before running the app:

**Linux / macOS:**
```bash
source .env
```

**Windows CMD:**
```cmd
for /f "tokens=1,2 delims==" %i in (.env) do set %i=%j
```

**Windows PowerShell:**
```powershell
Get-Content .env | ForEach-Object { $k,$v = $_ -split '=',2; [System.Environment]::SetEnvironmentVariable($k,$v) }
```

> **Note:** `source .env` (or the Windows equivalent) only applies to the current terminal session.
> Re-run it if you open a new terminal. Alternatively, set the variables permanently in your OS
> environment (System Properties → Environment Variables on Windows; `/etc/environment` on Linux).

### Step 4 — Build
```bash
mvn clean package -DskipTests
```

### Step 5 — Run
```bash
java -jar target/mini-wsa-*.jar
```

The app starts on `http://localhost:8080`.

> **Note:** If you restart your terminal, run `source .env` again before starting the app.

### Override DB credentials without editing files
```bash
DB_URL=jdbc:postgresql://myhost:5432/mydb \
DB_USERNAME=myuser \
DB_PASSWORD=mypass \
java -jar target/mini-wsa-*.jar
```

---

## API Documentation

### POST /v1/events/ingest — Ingest Events

Accepts a single event or a batch (array). Validates all fields, enriches with attack type and
threat score, and persists. Batch is all-or-nothing — one invalid event rejects the entire batch.

**Single event:**
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

Build the project first if you haven't already:
```bash
mvn clean package -DskipTests
```

Then run via Maven's exec plugin:
```bash
mvn exec:java -Dexec.args="<args>"
```

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
| `INFO` (default) | Batch ingestion: `Ingesting batch: count=N`, `Batch persisted: count=N`; controller entry |
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
  ├── EnrichmentService
  │    ├── AttackClassifier    (category → human-readable attackType)
  │    └── ThreatScoreEngine   (computes score 0–100)
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

**Scaling path for production:**
TimescaleDB (PostgreSQL extension) adds automatic time partitioning and continuous aggregates.
Kafka in front of ingestion decouples write throughput. ClickHouse handles analytics at extreme scale.

---

## Testing

### Run unit tests (no database needed)
```bash
mvn test
```

### Run all tests including integration tests
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
| `IngestionControllerIT` | Integration | POST single → 201; POST batch → 201; invalid → 400; data persisted |
| `StatsControllerIT` | Integration | Real data aggregation; empty range; configId filter |
| `SamplesControllerIT` | Integration | Pagination; sort order; category/action filters |

Unit tests use `@ExtendWith(MockitoExtension.class)` — no database required.
Integration tests use `@SpringBootTest` + H2 in PostgreSQL mode — no database setup needed.

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
- Error responses never expose internal details, stack traces, or database errors
- All queries use JPA parameterized statements — SQL injection is not possible

---

## Tool Installation

### Java 21 JDK

**Linux — Ubuntu / Debian:**
```bash
sudo apt update && sudo apt install -y openjdk-21-jdk
```

**Linux — Fedora / RHEL / CentOS Stream:**
```bash
sudo dnf install java-21-openjdk-devel
```

**macOS (Homebrew):**
```bash
brew install openjdk@21
# Add to shell profile so the JDK is picked up:
echo 'export JAVA_HOME=$(brew --prefix openjdk@21)' >> ~/.zprofile
source ~/.zprofile
```

**Windows (winget):**
```powershell
winget install Microsoft.OpenJDK.21
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

**Windows (winget):**
```powershell
winget install Apache.Maven
```
Or download the binary ZIP from [maven.apache.org](https://maven.apache.org/download.cgi), unzip it,
and add `<unzip-dir>/bin` to your `PATH`.

---

### PostgreSQL 15+

**Linux — Ubuntu / Debian** (official PostgreSQL apt repository, guarantees version ≥ 15):
```bash
sudo apt install -y curl ca-certificates
sudo install -d /usr/share/postgresql-common/pgdg
curl -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc --fail \
  https://www.postgresql.org/media/keys/ACCC4CF8.asc
. /etc/os-release
echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] \
  https://apt.postgresql.org/pub/repos/apt ${VERSION_CODENAME}-pgdg main" \
  | sudo tee /etc/apt/sources.list.d/pgdg.list
sudo apt update && sudo apt install -y postgresql-15
```

**Linux — Fedora / RHEL / CentOS Stream:**
```bash
sudo dnf install postgresql15-server
sudo /usr/pgsql-15/bin/postgresql-15-setup initdb
sudo systemctl enable postgresql-15
```

**macOS (Homebrew):**
```bash
brew install postgresql@15
brew services start postgresql@15
```

**Windows:**
Download the interactive installer from [postgresql.org/download/windows](https://www.postgresql.org/download/windows/)
and run it. The installer creates the `postgres` superuser and registers a Windows Service.
