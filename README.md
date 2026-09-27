<div align="center">

# Data Entry Management Platform

**A server-rendered data-entry dashboard built with Spring Boot 4, Thymeleaf, HTMX and MySQL — with zero JavaScript framework and zero REST plumbing.**

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![HTMX](https://img.shields.io/badge/HTMX-2.0.4-0098E6?logo=htmx&logoColor=white)](https://htmx.org/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3.8-7952B3?logo=bootstrap&logoColor=white)](https://getbootstrap.com/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3-005C0F?logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Highlights](#highlights)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Running Without Docker](#running-without-docker)
- [Configuration](#configuration)
- [Database Schema](#database-schema)
- [HTTP Endpoints](#http-endpoints)
- [Validation Rules](#validation-rules)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)
- [Specifications](#specifications)

---

## Overview

A production-shaped CRUD application for tracking equipment movement between
users and warehouses. Every write is validated on the server, every list is
searchable and paginated, and the dashboard summarises the dataset on load.

The interesting part is what the project deliberately does **not** contain.
There is no SPA, no client-side router, no JSON REST layer, and no JavaScript
build step. Forms post to controllers, controllers render Thymeleaf, and HTMX
```text
swaps HTML fragments into the page. `app.js` exists only
for confirmations, toasts and animations.

That constraint is the point: the whole application is a jar plus a MySQL
container, and it is debuggable with browser dev tools alone. The only
hand-written front-end code is `app.js` (220 lines of confirmations, toasts and
animations) and `app.css` (527 lines), and neither has a build step.

### Highlights

| | |
| --- | --- |
| **Dashboard** | Total entries, today's entries and the five most recent records, computed in a single repository pass. |
| **Create & edit** | One shared form, pre-filled on edit. Submission is a partial swap — no full page reload. |
| **Search** | Case-insensitive match across product, description and both users. Results arrive as a swapped table fragment. |
| **Delete** | Confirmation modal, then an HTMX `DELETE` that returns a fresh table fragment. |
| **Pagination** | Server-side, ten rows per page, with page-size independent total counts. |
| **Validation** | Bean Validation is authoritative; every rule is mirrored by an HTML attribute purely as a convenience. |
| **Health checks** | Actuator `/actuator/health` backs the Docker health check, so `docker compose ps` reflects the real database state. |
| **Self-diagnosing startup** | A wrong database port is reported as a wrong database port, not as a Hibernate dialect error. |

---

## Tech Stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 (Spring MVC, Spring Data JPA) |
| View layer | Thymeleaf server-side rendering |
| Interactivity | HTMX 2.0.4 (HTML over the wire) |
| Styling | Bootstrap 5.3.8, Bootstrap Icons 1.13.1, Inter (custom CSS) |
| Validation | Jakarta Bean Validation |
| Database | MySQL 8.0 with HikariCP connection pool |
| Operations | Docker, Docker Compose, Spring Boot Actuator |
| Build | Maven Wrapper (3.9.16) |

---

## Architecture

Data flows in one direction, and each layer is only ever allowed to call the one
below it:

```text
        Browser
           │  form submit / htmx request
           ▼
  ┌─────────────────────┐
  │  Thymeleaf + Bootstrap  (rendered HTML, no client framework)
  └─────────────────────┘
           │  full page render  ──────────►  GET  /collection
           │  fragment render  ──────────►  POST /data-entry, GET /collection/search
           ▼
  ┌─────────────────────┐
  │  Controller  (@Controller)          HTTP boundary, validation binding
  └─────────────────────┘
           │
           ▼
  ┌─────────────────────┐
  │  Service  (@Service)                business rules, mapping, transactions
  └─────────────────────┘
           │
           ▼
  ┌─────────────────────┐
  │  Repository (JpaRepository)         derived queries + @Query, no SQL strings
  └─────────────────────┘
           │
           ▼
      ┌──────────┐
      │  MySQL 8  │
      └──────────┘
```

**Controllers never touch the database.** A controller binds and validates the
request, calls one service method, and names a view. All rules live in the
service, which is also the only place that opens a transaction.

### Full page vs. fragment

The same template can be rendered either as a complete page or as a partial
update. Thymeleaf `th:fragment` is what makes one file serve both purposes, with
one rule attached:

> Files under `templates/fragments/` **must** have exactly one root element and
> must **not** include `<!DOCTYPE html>` or `<html>`. Only the four real pages
> (`index`, `data-entry`, `collection`, `error`) may have a document wrapper.

A stray document wrapper in a fragment leaks an entire HTML document into
`innerHTML`, which breaks the swap in ways that are tedious to debug.

---

## Quick Start

### Prerequisites

- Docker Desktop running
- Ports `3306` and `8081` free on the host (override in `.env` if not)

### 1. Configure

```bash
git clone https://github.com/soloStack-Dev/Data_entry_management.git
cd Data_entry_management
cp .env.example .env
```

`.env` is git-ignored, so real credentials never reach the repository. Adjust
`DB_PASSWORD`, `DB_ROOT_PASSWORD`, and the two port mappings if needed.

### 2. Run

```bash
docker compose up -d --build
```

### 3. Use

| What | Where |
| --- | --- |
| Application | <http://localhost:8081> |
| Health check | <http://localhost:8081/actuator/health> |
| Database (from the host) | `127.0.0.1:3306` |
| Database (from the Docker network) | `mysql:3306` |

> If `3306` or `8081` is already taken on your machine, set `DB_HOST_PORT` and
> `APP_PORT` in `.env` to free ports. On a machine where a local MySQL service
> occupies `3306`, set `DB_HOST_PORT=3310` — the application picks that up
> automatically, in Docker and from Maven alike.

Watch the two services come up healthy:

```bash
docker compose ps
```

```
NAME               IMAGE                        STATUS
data-entry-app     data-entry-platform:latest   Up (healthy)   0.0.0.0:8081->8080/tcp
data-entry-mysql   mysql:8.0                    Up (healthy)   0.0.0.0:3306->3306/tcp
```

The schema is created automatically on first start and the data survives
`docker compose down`. Use `down -v` to destroy the volume as well.

```bash
docker compose down        # stop, keep data
docker compose down -v     # stop, delete data
```

---

## Running Without Docker

Only the application runs on the host; MySQL stays in Docker.

```bash
docker compose up -d mysql
cd demo
./mvnw spring-boot:run            # Windows: .\mvnw.cmd spring-boot:run
```

That is the whole procedure. **No environment variables need to be set by
hand** — the application loads the same `.env` file that Docker Compose uses, so
the database port and the application port already match.

| | Docker Compose | Maven on the host |
| --- | --- | --- |
| Start with | `docker compose up -d --build` | `cd demo` → `.\mvnw.cmd spring-boot:run` |
| Application | <http://localhost:8081> | <http://localhost:8082> |
| Database (from host) | `localhost:3310` | `localhost:3310` |
| Database (from app) | `mysql:3306` | `localhost:3310` |
| Health | <http://localhost:8081/actuator/health> | <http://localhost:8082/actuator/health> |

The two modes use different application ports so that both can run at the same
time while you switch between them: `APP_PORT` (8081) is published by Compose,
while `MAVEN_APP_PORT` (8082) is used when the app runs on the host. MySQL is
always `localhost:3310` from the host in either mode.

### Where the values come from

Highest priority first:

1. **Environment variables** — `DB_HOST`, `DB_PORT`, `SERVER_PORT`, and the rest.
2. **The `.env` file** — found in the working directory or one level above it.
3. **The defaults in `application.properties`** — host `localhost`, port `3306`.

Because environment variables win, Docker Compose, CI, and a production
deployment all keep working unchanged. The `.env` file is only a fallback.

Two names in `.env` are mapped to the names the application reads, so the file
does not have to duplicate a value in two places:

| `.env` name | Used as |
| --- | --- |
| `DB_HOST_PORT` | `DB_PORT` |
| `MAVEN_APP_PORT` (falling back to `APP_PORT`) | `SERVER_PORT` |

Two switches control the mechanism, if you ever need them:

```properties
app.dotenv.enabled=false        # ignore .env completely
app.dotenv.file=/path/to/env    # load a different file
```

### Overriding for a single run

```bash
SERVER_PORT=9000 DB_PORT=3310 ./mvnw spring-boot:run
```

```powershell
$env:SERVER_PORT = "9000"
$env:DB_PORT     = "3310"
.\mvnw.cmd spring-boot:run
```

### Running the packaged jar

The jar reads `.env` in exactly the same way, as long as it is started from the
repository root or from `demo/`.

```bash
./mvnw clean package -DskipTests
java -jar target/data-entry-platform-0.0.1-SNAPSHOT.jar
```

The same settings can be passed as JVM system properties:

```bash
java -DSERVER_PORT=8082 -DDB_PORT=3310 -jar target/data-entry-platform-0.0.1-SNAPSHOT.jar
```

Full operator documentation — database GUI setup, `mysqldump` backups, log
inspection, port conflicts — is in **[RUN_COMMANDS.txt](RUN_COMMANDS.txt)**.

---

## Configuration

Every environment-specific value is a `${ENV_VAR:default}` placeholder in
`application.properties`, so a single jar runs unchanged on a laptop, inside a
container, and on a VM.

### Application

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP port the application listens on |
| `LOG_LEVEL` | `INFO` | Log level for the application package |

### Database

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_HOST` | `localhost` | MySQL host — `mysql` inside Compose |
| `DB_PORT` | `3306` | MySQL port — `3306` inside Compose |
| `DB_NAME` | `data_entry_db` | Schema name, created if absent |
| `DB_USERNAME` | `datauser` | Application user |
| `DB_PASSWORD` | `datapassword` | Application password |
| `DB_POOL_SIZE` | `10` | Maximum HikariCP pool size |

### JPA, caching and logs

| Variable | Default | Purpose |
| --- | --- | --- |
| `JPA_DDL_AUTO` | `update` | Schema strategy — use `validate` with Flyway in production |
| `JPA_SHOW_SQL` | `false` | Print every statement |
| `THYMELEAF_CACHE` | `false` | `true` in the container, where templates are baked in |

### `.env` only

These names are for Docker Compose's port publishing, and the application
derives two of its own settings from them (see
[Where the values come from](#where-the-values-come-from)).

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_HOST_PORT` | `3306` | Host port published for MySQL — also used as `DB_PORT` |
| `APP_PORT` | `8081` | Host port published for the application |
| `MAVEN_APP_PORT` | `8082` | Application port when running on the host — used as `SERVER_PORT` |
| `DB_ROOT_PASSWORD` | `rootpassword` | MySQL `root` password inside the container |

---

## Database Schema

One table, created automatically by Hibernate against the specification.

```sql
CREATE TABLE data_entries (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    product_name VARCHAR(150)  NOT NULL,
    description  VARCHAR(2000) NOT NULL,
    timing       TIME          NOT NULL,
    entry_date   DATE          NOT NULL,
    type         VARCHAR(50)   NOT NULL,
    from_user    VARCHAR(100)  NOT NULL,
    to_user      VARCHAR(100)  NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    KEY idx_product_name (product_name),
    KEY idx_entry_date   (entry_date),
    KEY idx_type         (type),
    CONSTRAINT data_entries_chk_1
        CHECK (type IN ('PURCHASE','SALE','TRANSFER','RETURN','OTHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

`type` is deliberately a `VARCHAR(50)` and **not** a MySQL `ENUM`. A native
enum silently changes whenever a constant is renamed, and it cannot be extended
by a plain `ALTER`. The Java side stays a proper enum via an auto-applied
`AttributeConverter`, and Hibernate still enforces the allowed values with a
`CHECK` constraint — so the database remains portable and the set of values is
still guaranteed.

The three secondary indexes are declared on the entity because spec 05 defines
them. `idx_product_name` supports the search filter, `idx_entry_date` supports
the dashboard's "today" count, and `idx_type` supports type-based lookups.

### Entry types

`PURCHASE` · `SALE` · `TRANSFER` · `RETURN` · `OTHER`

---

## HTTP Endpoints

All UI routes return HTML. There is no JSON API.

| Method | Route | Returns | Notes |
| --- | --- | --- | --- |
| `GET` | `/` | Full page | Dashboard: totals and recent records |
| `GET` | `/data-entry` | Full page | Create form |
| `POST` | `/data-entry` | Fragment | Validates, then re-renders form + alert |
| `GET` | `/data-entry/edit/{id}` | Full page | Pre-filled edit form |
| `POST` | `/data-entry/edit/{id}` | — | `HX-Redirect: /collection` on success |
| `GET` | `/collection` | Full page | Paginated table, ten per page |
| `GET` | `/collection/search?search=` | Fragment | Case-insensitive filter |
| `DELETE` | `/collection/{id}` | Fragment | Re-renders the table; `HX-Trigger` on success |
| `GET` | `/actuator/health` | JSON | `UP` only when the database answers |
| `GET` | `/actuator/info` | JSON | Application name |

### How partial updates behave

| Outcome | Status | Body swapped | Client signal |
| --- | --- | --- | --- |
| Success | `200` | Yes | `HX-Trigger` / rendered alert |
| Validation failure | `200` | Yes (the form again) | Inline field errors |
| Record missing | `404` | **No** | `X-App-Error` header → toast |

The third row is intentional. HTMX ignores error responses by default, and that
is exactly what is wanted: a rejected delete must not wipe the table off the
screen. The friendly message travels in a header instead, and `app.js` renders it
as a toast.

---

## Validation Rules

Server-side rules are authoritative; the browser constraints are a convenience
that mirrors them.

| Field | Rules |
| --- | --- |
| Product name | Required, 2–150 characters, trimmed |
| Description | Required, max 2000 characters, trimmed |
| Timing | Required, HTML `time` input |
| Date | Required, HTML `date` input |
| Type | Required, one of the five known values |
| From user | Required, max 100 characters, trimmed |
| To user | Required, max 100 characters, trimmed |
| From ≠ To | Case-insensitive, whitespace-insensitive |

The last rule is a business rule rather than a field constraint, because it
compares two fields at once — something Bean Validation cannot express on either
field in isolation. It therefore lives in the service layer and is reported as a
form-level error.

Duplicate submissions are prevented by disabling the submit button for the
duration of the HTMX request and re-enabling it on completion, including on
failure.

---

## Testing

```bash
docker compose up -d mysql
cd demo
./mvnw test            # Windows: .\mvnw.cmd test
```

> The suite loads a real application context, so **MySQL must be running first**.
> There is no embedded database and no Testcontainers layer — the test asserts
> that the production wiring actually starts. The database port comes from
> `.env`, so no variable needs to be exported.

---

## Project Structure

```text
Data_entry_management/
├── docker-compose.yml              # MySQL 8.0 + app, healthchecks, named volume
├── .env.example                    # tracked, password-free configuration template
├── RUN_COMMANDS.txt                # operator guide: startup, DB access, backups
├── AGENTS.md                       # conventions and hard-won gotchas for contributors
│
└── demo/                           # the only Maven module
    ├── Dockerfile                  # multi-stage build → JRE, runs as non-root
    ├── pom.xml
    ├── data_entry_platform_specs/  # the 7 numbered specifications
    └── src/main/
        ├── java/com/example/dataentry/
        │   ├── DataEntryApplication.java
         │   ├── config/             # navigation model, .env loader, startup DB check
        │   ├── controller/         # dashboard, data entry, collection
        │   ├── service/            # business rules, mapping, transactions
        │   ├── repository/         # Spring Data JPA
        │   ├── entity/             # DataEntry, EntryType, EntryTypeConverter
        │   ├── dto/                # DataEntryRequest form object
        │   └── exception/          # not-found + scoped exception handler
        └── resources/
            ├── application.properties
            ├── static/css/app.css
            ├── static/js/app.js
            └── templates/
                ├── index.html · data-entry.html · collection.html · error.html
                └── fragments/      # header, navbar, alerts, form, table, pagination
```

---

## Troubleshooting

**`Cannot connect to the database.` / `Unable to determine Dialect without JDBC metadata`**

The database was not reachable. A startup check intercepts this and reports the
exact JDBC URL it tried, the username, and the server's own reason — the URL
already contains the host and port that were used, so compare that with
`docker compose ps`:

```text
JDBC URL : jdbc:mysql://localhost:3310/data_entry_db...
Reason   : Access denied for user 'datauser'@'localhost'
```

`Access denied` almost always means the port belongs to a *different* MySQL than
the one holding `datauser`. Compare against `docker compose ps`; the published
database port there must match `DB_HOST_PORT` in `.env`.

**`Address already in use` / `port is already allocated`**

Another process owns the port. Change `APP_PORT` or `DB_HOST_PORT` in `.env`.
Note that a local `mysqld` service commonly occupies 3306, which is why the
template publishes MySQL on 3306 only as a default.

**Application starts but the table is missing**

The first start creates the schema. If the volume was created by an earlier run
with a different database name, remove it and let it rebuild:

```bash
docker compose down -v && docker compose up -d --build
```

**Template edits are not showing**

Caching is off by default when running from source. If you set
`THYMELEAF_CACHE=true`, restart the application after changing a template.

---

## Specifications

The application was implemented against seven numbered specifications kept in
[`demo/data_entry_platform_specs/`](demo/data_entry_platform_specs/):

| Spec | Subject |
| --- | --- |
| `00` | Project overview, pages, data flow, package structure |
| `01` | UI components |
| `02` | Visual style, animation, icons |
| `03` | Input validation |
| `04` | Background processes |
| `05` | Database, MySQL, Docker |
| `06` | Spring Boot configuration |

They are the authoritative source of truth. If code and specification disagree,
the specification wins — and the code should be corrected.

---

## Contributing

1. Read the relevant spec in `demo/data_entry_platform_specs/` first.
2. Read [`AGENTS.md`](AGENTS.md) — it records the conventions, and more
   importantly the traps that have already cost time here.
3. Keep the layering intact: controller → service → repository, no database
   access in controllers.
4. Keep fragments free of document wrappers.
5. Java and XML use **tabs**.
6. Verify with `./mvnw test` and a `docker compose up -d --build` before opening
   a pull request.

---

<div align="center">

**Built with Spring Boot, Thymeleaf, HTMX, Bootstrap and MySQL.**

</div>
