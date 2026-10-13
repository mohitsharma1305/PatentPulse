# Source integration and validation

Date: 13 October 2026.

## What was integrated

Restored the missing application files from the supplied complete PatentPulse archive: Maven build, startup/configuration classes, database helper and schemas, user model, shared business services and controller, JSP views, shared assets, demo launchers, sample files and missing tests.

Existing repository Java files, HashingTest, module notes, users.jsp and admin-users.css were retained. The existing admin page has its own stylesheet and empty-state handling; it was not replaced with the archive's older page. Existing documentation and team commit history remain intact.

The workflow test now checks that the preserved administrator page renders its protected-account label, that its stylesheet is served, and that its rendered CSRF token can be used for account changes.

## Fresh build result

Environment: Linux, OpenJDK 17.0.20, Maven 3.9.9. Dependencies use the supplied pom.xml baseline. Tests start embedded Tomcat 9.0.106 with H2 2.3.232 in MySQL compatibility mode.

Command: `mvn clean verify` (the build environment supplied a private Maven proxy settings file for dependency downloads).

| Suite | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| HashingTest | 4 | 0 | 0 | 0 |
| ReviewPolicyTest | 4 | 0 | 0 | 0 |
| WorkflowTest | 3 | 0 | 0 | 0 |
| Total | 11 | 0 | 0 | 0 |

Result: **BUILD SUCCESS**. Maven produced `target/patentpulse.war`.

The HTTP workflow suite exercises registration, session/CSRF checks, access restrictions, drafts, submissions, downloads, receipts, matching and mismatching candidate files, reviewer assignments and decisions, version history, administration and audit output. These passing scenarios are functional checks, not a complete security audit or performance benchmark.

Build output, dependency installations, local database/upload data and the prebuilt archive WAR are excluded from the commit. Rebuild the WAR from the integrated source.

## Remaining deployment checks

- Run `mvn clean verify` on the team's final merged checkout.
- Start `START-DEMO.bat` on the intended Windows machine and review the UI.
- Configure and test the real MySQL/Tomcat installation; H2 tests do not replace that check.
- Record the final demo video and complete guide/member details in submission documents.

The earlier `docs/TEST-REPORT.md` remains the archived 6 October report. This file records the new integration run separately.
