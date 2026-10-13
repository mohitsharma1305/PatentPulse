# Mann module

## Administration audit and receipts

Admins can update non-admin researcher/reviewer accounts. Existing admin accounts cannot be changed through this page. Unfinished active reviewer assignments block deactivation or demotion. The audit view shows the latest 200 events. Receipts read version data and have no separate table.

## Files and responsibility

| Project path | Responsibility |
|---|---|
| `src/main/webapp/WEB-INF/views/users.jsp` | Role and active-status administration UI. |
| `src/main/webapp/WEB-INF/views/audit.jsp` | Latest audit event display. |
| `src/main/webapp/WEB-INF/views/receipt.jsp` | Printable submission receipt. |
| `src/main/java/com/patentpulse/service/LedgerService.java` | users, changeUser and audit. Shared with all five. |
| `src/main/java/com/patentpulse/dao/Database.java` | Audit insertion and shared JDBC helpers. Coordinate all schema impacts. |
| `src/main/java/com/patentpulse/web/AppServlet.java` | User administration, audit and receipt routes, shared with Mohit, Jay and Nitesh. |

## Routes

GET /app/admin/users, POST /app/admin/user, GET /app/admin/audit, GET /app/receipt

## Data

users and audit_logs; read document_versions and blueprints for receipts

## Shared coordination

Coordinate account semantics with Meet, receipt version data with Mohit and review-related events with Nitesh. Receipts are unsigned and do not grant patent rights. Database operators can alter audit data; append-only or tamper-proof storage is not implemented.

## Acceptance checks

1. Reject admin routes for ordinary users.
2. Change an eligible user role and active flag.
3. Reject edits to admin accounts.
4. Check unfinished-reviewer protection.
5. Inspect audit ordering and authorised receipt access.

These are checks to run, not fresh passing results. Use the final merged source and record actual output.

## Git workflow

```sh
git switch main
git pull --ff-only origin main
git switch -c mann-module-update
```

Use a new branch name if it already exists. Copy or edit only agreed files, inspect `git diff`, stage exact paths and commit a description of the real changes. Push the branch and open a pull request into main. Do not replace newer shared files with old copies from the ZIP.
