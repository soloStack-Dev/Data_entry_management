# AGENTS.md

## Layout

- Root is the deployment project; `demo/` is the only Maven module. **All Maven commands run from `demo/`** - there is no root/aggregator POM.
- `demo/data_entry_platform_specs/` holds 8 numbered spec docs that are the authoritative requirements source. Read the relevant one before touching UI, validation, DB, or config. Each file is written to be used standalone as a prompt.
- `RUN_COMMANDS.txt` is the operator-facing guide: startup, database access, and troubleshooting. Update it whenever a port, container name, or command changes.
- `.env` holds this machine's real ports/passwords and is git-ignored; `.env.example` is the committed template. Never hard-code credentials anywhere else.
- Not a git repo yet (`.gitignore`/`.gitattributes` exist, no `.git`). `HELP.md` is gitignored - it is Initializr boilerplate, not project docs.

## Ports on this machine (already decided - do not re-litigate)

| What | Host port | Why |
| --- | --- | --- |
| App (container) | 8081 | 8080 is taken by another container |
| App (run from Maven/jar) | 8082 | set with `SERVER_PORT` |
| MySQL (container) | 3310 | 3306 is taken by a **native** `mysqld` service |

Compose defaults stay portable (3306/8080); the local overrides live in `.env`. Inside the network the app uses `mysql:3306`; from the host it needs `DB_PORT=3310`.

## Commands

Run from `demo/`:

```bash
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
./mvnw test                     # Windows: .\mvnw.cmd test
./mvnw clean package -DskipTests
```

From the repository root:

```bash
docker compose up -d --build    # build image, start MySQL 8.0 + app
docker compose ps               # both services should report (healthy)
docker compose logs -f app
docker compose down             # keeps data;  down -v  destroys it
```

- Wrapper is `distributionType=only-script` pinned to Maven 3.9.16 - it downloads the distribution on first use (needs network). System Maven 3.9.11 also installed.
- No lint, formatter, or codegen plugin is configured, so there is no style gate to run.
- Java and XML use **tabs** (Initializr default). Match it.
- `./mvnw test` **requires the `mysql` container to be running** - there is no embedded database. Set `DB_PORT=3310` first or it fails on the datasource.

## Spring Boot 4.1.1 quirks

- Boot 4 module names, **not** the Boot 3 ones: `spring-boot-starter-webmvc` (not `-web`), and per-module test starters like `spring-boot-starter-webmvc-test` instead of an aggregate `spring-boot-starter-test`. Do not "correct" these coordinates - the Boot 3 names will not resolve.
- There is no `spring-boot-starter-test` in the POM. Add the specific `<module>-test` starter for whatever module you need to test.
- Java 21. No H2, no Testcontainers, no `application-test.properties`.

## Architecture

- Server-rendered Thymeleaf + Bootstrap + HTMX. No SPA, no JSON REST API for the UI, no JS framework. CDN assets: Bootstrap 5.3.8, Bootstrap Icons 1.13.1, htmx 2.0.4, Inter via jsDelivr.
- Layering is fixed: controller -> service -> repository. No database access in controllers.
- Routes: `/`, `GET/POST /data-entry`, `GET/POST /data-entry/edit/{id}`, `/collection`, `/collection/search?search=`, `DELETE /collection/{id}`.
- **Full page vs fragment is decided by whether the file is used as a Thymeleaf *view* or an *include*.** `fragments/*.html` are returned as views for HTMX responses, so they must have exactly one root element and **no `<!DOCTYPE html>`/`<html>` wrapper** - only the four real pages may have one. A wrapper in a fragment leaks a whole HTML document into `innerHTML`.
- Each fragment declares `th:fragment` on that single root element, which lets the same file work as a view name and as an include.
- `EntryType` is persisted through `EntryTypeConverter` (`autoApply`), not `@Enumerated`, so the column stays `varchar(50)` per spec 05 instead of becoming a native MySQL `enum`.
- `spring.jpa.open-in-view=false`, so anything lazy must be fetched in the service layer.
- Dev uses `ddl-auto=update`; specs say production should use Flyway instead (not added yet).

## Traps that have already bitten

- **`#fields.globalErrors()` returns `String[]`, not `FieldError` objects.** Use `${err}`, never `${err.defaultMessage}`.
- **`@ControllerAdvice` with no `basePackages` breaks the error page.** A catch-all `Exception` handler also intercepts Spring Boot's internal `/error` dispatch and then fails with "No converter for LinkedHashMap". Scope it to `com.example.dataentry.controller`.
- **HTMX does not swap 4xx/5xx bodies by default.** That is relied upon deliberately: a failed delete must not wipe the table. The friendly message travels in the `X-App-Error` header and `app.js` shows it as a toast.
- The form `date` field maps to the entity's `entryDate` / column `entry_date`. The conversion lives in exactly two methods in `DataEntryService` (`applyRequest` and `toForm`); do not spread it.
- **A `@Bean` method name must not equal its `@Configuration` class name.** A `@Configuration` class is itself registered under its decapitalised class name, so `class DataSourceStartupCheck` + `@Bean dataSourceStartupCheck()` collides and fails with "a bean with that name has already been defined". Use a distinct method name.
- **The app does not read `.env`; only Docker Compose does.** Without `DB_PORT=3310` the default 3306 reaches the *native* `mysqld`, which rejects `datauser`. `DataSourceStartupCheck` (a `BeanPostProcessor`) now validates the DataSource before JPA uses it and reports the URL, username and real reason; that is why the old `Unable to determine Dialect without JDBC metadata` no longer appears.
- The jar name is `data-entry-platform-0.0.1-SNAPSHOT.jar`. `artifactId` was renamed from `demo`.
