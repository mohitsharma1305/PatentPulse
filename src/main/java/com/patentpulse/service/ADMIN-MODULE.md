# Administration Module — Mann

Responsibilities:
- Manage user roles and account activation.
- Display the audit log.
- Provide printable submission receipts.

Planned view files:
- `src/main/webapp/WEB-INF/views/users.jsp`
- `src/main/webapp/WEB-INF/views/audit.jsp`
- `src/main/webapp/WEB-INF/views/receipt.jsp`

Shared files:
- `LedgerService.java` — user listing, `changeUser`, and audit retrieval.
- `Database.java` — shared audit-writing method.
- `AppServlet.java` — administration, audit, and receipt request routing.

Database tables: `users`, `audit_logs`. Receipts use existing submission records; no receipt table is planned.

Current stage: folder setup and responsibility documentation. Coordinate changes to shared files with their owners before implementation.
