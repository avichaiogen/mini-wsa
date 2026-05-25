# Mini WSA — Security Analytics Pipeline

A backend service that ingests security event logs (DLRs), enriches them with attack classification
and threat scoring, stores them, and exposes REST analytics APIs — built as a simplified version of
Akamai's Web Security Analytics platform.

---

## Table of Contents
1. [Build & Run](#build--run)
2. [API Documentation](#api-documentation)
3. [Architecture & Technology](#architecture--technology)
4. [Testing](#testing)
5. [Feature Deep Dive](#feature-deep-dive)

---

## Build & Run

### What you need
- Java 21
- Maven 3.9+
- PostgreSQL 15+

Verify:
```bash
java -version    # must show 21.x
mvn -version     # must show 3.9+
psql --version   # must show 15+
```

### Step 1 — Start PostgreSQL
```bash
sudo systemctl start postgresql
```

### Step 2 — Create the database (first time only)
```bash
sudo -u postgres psql -c "CREATE DATABASE miniwsa;"
sudo -u postgres psql -c "CREATE USER miniwsa WITH PASSWORD 'miniwsa';"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE miniwsa TO miniwsa;"
```

### Step 3 — Set up credentials
```bash
cp .env.example .env
# Edit .env if your DB host/user/password differ from the defaults
source .env
```

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

### Technology Choices

| Component | Choice | Why |
|---|---|---|
| Language | Java 21 | LTS, virtual threads, modern switch expressions |
| Framework | Spring Boot 4.0 | Industry standard; auto-configuration; mature ecosystem |
| Storage | PostgreSQL | SQL aggregations are native fit for the stats API; ACID transactions for batch ingestion |
| Migrations | Flyway | Versioned, reproducible schema evolution |
| Logging | SLF4J + Log4j2 | SLF4J decouples code from impl; Log4j2 async appenders for high-throughput ingestion |
| Testing | JUnit 5 + Mockito + Testcontainers | Unit tests with mocked repos; integration tests against real PostgreSQL |

**Why PostgreSQL over MongoDB:**
The stats API requires GROUP BY, top-N, and time-window COUNT queries — SQL's native strength.
The schema is fixed and well-known so document flexibility offers no benefit.
ACID transactions enforce the all-or-nothing batch ingestion contract.

**Scaling path for production:**
TimescaleDB (PostgreSQL extension) adds automatic time partitioning and continuous aggregates.
Kafka in front of ingestion decouples write throughput. ClickHouse handles analytics at extreme scale.

---

## Testing

### Run unit tests (no database needed)
```bash
mvn test
```

### Run all tests including integration tests (requires Docker for Testcontainers)
```bash
mvn verify
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
Integration tests use `@SpringBootTest` + Testcontainers, which spin up a real PostgreSQL container automatically.

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
