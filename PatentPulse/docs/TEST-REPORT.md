# Verification report

Verified on 6 October 2026 with OpenJDK 17, Maven 3.9.9, embedded Tomcat 9.0.106, and H2 2.3.232 in MySQL compatibility mode.

## Automated results

**11 tests passed; 0 failures, 0 errors, 0 skipped.** `mvn package` successfully produces the deployable WAR.

| Test class | Tests | Coverage |
|---|---:|---|
| HashingTest | 4 | Standard SHA-256 vectors, changed bytes, streamed read boundaries |
| ReviewPolicyTest | 4 | One/two approvals, rejection priority, terminal states |
| WorkflowTest | 3 | Real HTTP, rendered JSPs, sessions, database and upload workflow |

The HTTP workflow checks registration, attempted role escalation, login/logout, CSRF rejection, research drafts and uploads, authorized downloads, unauthorized role/record access, escaped HTML, UTF-8 JSP rendering, receipts, MATCH/MISMATCH comparisons, reviewer assignments, duplicate assignment rejection, duplicate vote rejection, two-approval completion, rejection/revision, immutable old-version metadata, separate version authorization, user deactivation and audit records.

## Browser verification

Headless Chromium exercised the actual application: login, filling and submitting a research form, uploading a sample file, checking integrity, and returning to the dashboard. Captured desktop (1440px) and mobile (390px) screenshots. No JavaScript page errors or whole-page horizontal overflow were detected. Mobile sign-out was verified visible. Manually inspected rendered login/dashboard screenshots and corrected JSP encoding and mobile navigation issues.

Screenshots in `docs/screenshots` show synthetic local-demo records. The ZIP does not contain the demo database or uploaded user data.

## Practical limits

- MySQL 8 deployment scripts and JDBC configuration are supplied, but a live MySQL server and external Tomcat installation were not available for integration testing here. H2 compatibility mode is not a substitute for checking your installed MySQL configuration.
- Windows batch instructions were reviewed, but executed tests ran on Linux.
- This is a lab-ready prototype, not a production security audit or legal proof-of-invention service.
- Embedded Tomcat can print shutdown/reflection warnings on Java 17; the pool close hook is implemented and the tests complete successfully.
