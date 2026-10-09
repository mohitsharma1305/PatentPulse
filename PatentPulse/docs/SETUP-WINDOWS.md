# Windows setup — PatentPulse

## Option A: run the full local demo first

1. Install **JDK 17** and **Apache Maven 3.9+**. Add their `bin` directories to PATH. Open a new terminal and run `java -version` and `mvn -version`; both should report Java 17.
2. Extract the ZIP. Open the folder containing `pom.xml` and double-click `START-DEMO.bat`.
3. Wait for `PatentPulse demo: http://localhost:8080/patentpulse` in the terminal. The first run downloads Maven dependencies and needs internet.
4. Open that address and use the demo accounts listed in README.md. The database and uploads persist in `demo-data`.
5. Leave the terminal open while using the app. Ctrl+C stops it. Do not run two launchers against the same demo database.

You do not need MySQL or a separate Tomcat installation for this option. For your final JDBC/MySQL lab demonstration, use Option B.

## Option B: MySQL 8 and Tomcat 9

### 1. Database

Install MySQL 8. In `sql/create-database.sql`, replace `REPLACE_WITH_A_STRONG_DATABASE_PASSWORD` with your chosen database password. Run in MySQL Workbench as a database administrator:

- Open and execute `sql/create-database.sql`.
- Open and execute `sql/schema.sql`.

The runtime account has SELECT, INSERT and UPDATE only. Schema creation is performed separately by the administrator. If a `patentpulse` account already exists, `CREATE USER IF NOT EXISTS` does not change its password; use your existing password or explicitly update it in MySQL.

### 2. Tomcat

Extract Apache Tomcat **9.0.x**. This source uses `javax.servlet`; Tomcat 10+ uses `jakarta.servlet` and is not directly compatible. The embedded demo pins Tomcat 9.0.106 for reproducible tests; review supported patch versions before exposing any deployment publicly.

Create `TOMCAT_FOLDER\bin\setenv.bat` with this template, replacing the example paths and credentials:

```bat
@echo off
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "DB_URL=jdbc:mysql://localhost:3306/patentpulse?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
set "DB_USER=patentpulse"
set "DB_PASSWORD=YOUR_DATABASE_PASSWORD"
set "UPLOAD_DIR=C:\patentpulse-data\uploads"
set "INIT_SCHEMA=false"
set "DEMO_MODE=false"
set "BOOTSTRAP_ADMIN_EMAIL=admin@college.edu"
set "BOOTSTRAP_ADMIN_PASSWORD=YOUR_UNIQUE_ADMIN_PASSWORD"
```

Use a bootstrap password of at least 12 characters and at most 72 UTF-8 bytes. Keep this file private and out of Git. `setenv.bat` is used by Tomcat startup scripts; if you installed Tomcat as a Windows service, configure equivalent service environment/JVM properties or run it using the scripts for the lab.

### 3. Build and deploy

Open PowerShell or Command Prompt in the project folder:

```powershell
mvn clean package
Copy-Item .\target\patentpulse.war C:\apache-tomcat-9\webapps\patentpulse.war
```

Replace `C:\apache-tomcat-9` with your folder. Or copy the bundled `dist/patentpulse.war` manually if you only want to deploy the prebuilt release.

Run Tomcat's `bin\startup.bat`. Open **http://localhost:8080/patentpulse**. Sign in with the bootstrap admin email/password. Remove the two bootstrap variables from `setenv.bat` after successful setup; existing users remain in MySQL.

Register a researcher and two reviewers using the public registration page. All new accounts begin as RESEARCHER. Sign in as admin, open **People & roles**, and promote two accounts to REVIEWER. The public form cannot create an admin account.

### 4. Import into an IDE

- **IntelliJ IDEA:** Open the folder containing `pom.xml`; import Maven dependencies; select JDK 17. Use the bundled demo script if your edition does not provide Tomcat integration.
- **Eclipse:** File → Import → Maven → Existing Maven Projects; choose this project; configure Java 17 and a Tomcat 9 server if using the Servers view.
- **VS Code:** Open the folder, use Java tooling or run the Maven commands in the terminal.

## Troubleshooting

| Problem | Fix |
|---|---|
| `mvn` or `java` not recognized | Update PATH, then reopen your terminal. |
| Port 8080 already in use | Stop the other server or use `-Ddemo.port=8081` for demo mode. |
| Tomcat 404 | Confirm the WAR is named `patentpulse.war`, deployed, and startup has no errors. |
| App startup fails | Check Tomcat logs, MySQL service, DB credentials, schema, and writable upload directory. |
| `javax.servlet` class errors | Use Tomcat 9, not Tomcat 10/11. |
| Upload rejected | Use an allowed extension and a nonempty file ≤10 MB; PDFs require a `%PDF-` header. |
| Form expired / 403 | Refresh and retry. Logging in/out changes the session/CSRF token. |
| Cannot disable reviewer | Complete their active pending reviews first. |
| Maven dependency errors | Check internet/proxy configuration; retry with `mvn -U clean package`. |
| Demo H2 database in use | Stop the previous demo process before starting a second one. |

To remove demo data, first stop the demo and back up anything needed. `demo-data` contains the complete demo database and uploaded files. Do not remove it if you need those records.
