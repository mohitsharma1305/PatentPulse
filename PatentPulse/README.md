# PatentPulse — Cryptographic Research Ledger

A complete Advanced Java lab project for an RTU B.Tech fifth-semester team of five. Built with **Java 17, Servlets, JSP + EL/JSTL, Filters, HttpSession, JDBC, and MySQL**. Includes a local H2 demo so you can explore the complete application before installing MySQL.

## Start here

**Fastest option on Windows:** install JDK 17 and Maven 3.9+, extract this project, and double-click **`START-DEMO.bat`**. First launch downloads dependencies and requires internet. Keep the terminal open.

Then open **http://localhost:8080/patentpulse**. If port 8080 is occupied, run `mvn test-compile exec:java -Dexec.mainClass=com.patentpulse.DemoServer -Dexec.classpathScope=test -Ddemo.port=8081` and use port 8081.

On macOS/Linux:

```bash
chmod +x START-DEMO.sh
./START-DEMO.sh
```

Demo users (local demo only):

| Role | Email | Password |
|---|---|---|
| Researcher | researcher@demo.local | PatentPulse!2026 |
| Reviewer 1 | reviewer1@demo.local | PatentPulse!2026 |
| Reviewer 2 | reviewer2@demo.local | PatentPulse!2026 |
| Administrator | admin@demo.local | PatentPulse!2026 |

The demo binds to **127.0.0.1**, stores its H2 database/files in `demo-data`, and uses embedded Tomcat **9**. The demo launcher and H2 are test dependencies and are **not included in the deployable WAR**. Use separate browser profiles/private windows for multiple roles. Normal logout/login also works. Stop the terminal with Ctrl+C before starting another instance.

## Features

- Researcher registration; BCrypt password hashes; researcher/reviewer/admin access.
- Draft metadata, private uploads, immutable submitted versions and version history.
- Streamed SHA-256 fingerprints and UTC server submission timestamps.
- Exact-file comparison returning MATCH or MISMATCH.
- Admin assignment of two distinct reviewers; final per-version decisions.
- Two approvals → Internally Reviewed. Any rejection → Needs Revision.
- Last-200-event audit view, restricted to administrators.
- Printable submission receipts (browser Print / Save PDF).
- Role and account administration for researcher and reviewer access.
- Responsive navy/teal JSP interface, search and status filters.
- CSRF tokens, role filters, per-record access checks, prepared statements, escaped HTML, session rotation, upload limits, private storage, and transaction rollback.

**Integrity and legal scope:** this is an internal document-integrity and research-history tool. A hash does not prove authorship or inventorship; a private upload does not automatically establish legally recognized prior art. Server timestamps are not independent trusted timestamps. Internal reviews do not certify patentability. Receipts are not digitally signed. Database/server administrators remain trusted. No blockchain, patent search, plagiarism detector, email service, or public disclosure is implied.

## MySQL + external Tomcat installation

See **[docs/SETUP-WINDOWS.md](docs/SETUP-WINDOWS.md)** for step-by-step instructions and troubleshooting.

1. Install JDK 17, Maven 3.9+, MySQL 8, and a compatible Tomcat **9.0.x** patch release. Do not deploy this `javax.servlet` application to Tomcat 10/11 without migration.
2. Run `sql/create-database.sql` after replacing its sample database password. Apply `sql/schema.sql` as the database administrator.
3. Configure environment variables using `config.example.env`. The application does **not** automatically load `.env` files: set real environment variables or use Tomcat's `bin/setenv.bat`.
4. Set `BOOTSTRAP_ADMIN_EMAIL` and a strong `BOOTSTRAP_ADMIN_PASSWORD` (12 characters minimum, at most 72 UTF-8 bytes) for first startup. The bootstrap creates an admin only if no admin already exists.
5. Build with `mvn clean package`. Copy `target/patentpulse.war` to Tomcat's `webapps` folder. Start Tomcat, then open `http://localhost:8080/patentpulse`.
6. Remove bootstrap credentials after successful first startup. Register researchers through the UI and use Admin → People & roles to assign reviewers.

`DEMO_MODE` must stay false for real deployments. The example database password is not a default application credential. External Tomcat is for your lab installation; before public hosting apply the hardening notes in `docs/ARCHITECTURE.md`.

## Project structure

```text
src/main/java/com/patentpulse/
  config/     application startup, pool lifecycle, environment configuration
  dao/        prepared JDBC helpers and transaction boundary
  model/      authenticated User bean
  service/    authentication, ledger workflows, review policy, file storage
  util/       SHA-256 streaming and structured application errors
  web/        servlet controllers and security filter
src/main/webapp/
  WEB-INF/views/  JSP pages and shared fragments
  WEB-INF/web.xml servlet, filter, listener and multipart mappings
  assets/        CSS and small progressive-enhancement JavaScript
src/main/resources/schema.sql
src/test/java/com/patentpulse/  automated tests and local demo launcher
sql/                          MySQL setup and schema
samples/                      safe sample research files for your presentation
scripts/                      optional demo-data preparation
```

## Build and test

```bash
mvn clean verify
```

Tests exercise SHA-256 vectors and changes, review thresholds, and real HTTP workflows against embedded Tomcat/JSP + H2 in MySQL compatibility mode. See `docs/TEST-REPORT.md` for the exact verification performed for this release. H2 compatibility tests do not replace testing against your own MySQL installation.

The prebuilt `dist/patentpulse.war` is included for convenience. Rebuild after changing source; deploy the newly built `target/patentpulse.war`.

## Presentation material

- `docs/DEMO-SCRIPT.md` — a 7-minute demonstration
- `docs/ARCHITECTURE.md` — schema, request flow, security and design decisions
- `docs/TEAM-AND-VIVA.md` — five-person work split and viva questions
- `docs/TEST-REPORT.md` — verification results and remaining limits

No real research or personal documents are bundled. Screenshots, if included, use demo accounts and sample records only.
