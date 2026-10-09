# Team distribution and viva guide

Team name can be chosen independently. Suggested: **OriginGuard**. Project: **PatentPulse**.

## Five-member ownership

| Member | Module ownership | Main files | Demonstration responsibility |
|---|---|---|---|
| 1: Lead / cryptography | Uploads, hashing, integration | Hashing, UploadStore, AppServlet | Upload + MATCH/MISMATCH |
| 2: Database / JDBC | Schema, transactions, connection pool | schema.sql, Database, AppContext | Tables, joins, commit/rollback |
| 3: JSP / interface | Dashboard, forms, responsive styles | WEB-INF/views, assets | EL/JSTL + interface |
| 4: Security | Authentication, sessions, filters and access rules | AuthService, AuthServlet, SecurityFilter | Role denial, CSRF, login |
| 5: Review / QA | Assignment, decisions, tests and reporting | LedgerService, ReviewPolicy, tests | Review thresholds and testing |

Member 1 coordinates changes to AppServlet and LedgerService so multiple branches do not overwrite shared code. Each member should explain their module, its inputs/outputs, and at least one failure case.

## Administration module (Mann)

- User roles and account activation: `WEB-INF/views/users.jsp`, `LedgerService.users`, and `LedgerService.changeUser`.
- Audit log display: `WEB-INF/views/audit.jsp` and `LedgerService.audit`; event writes use the shared `Database.audit` method.
- Printable submission receipts: `WEB-INF/views/receipt.jsp`, rendered from existing submission-version records.
- Shared integration points: coordinate edits to `LedgerService.java`, `Database.java`, and `AppServlet.java` with their existing owners. The tables are `users` and `audit_logs`; receipts do not require a new table.

## RTU Advanced Java concepts shown

| Concept | Where demonstrated |
|---|---|
| HTTP GET/POST and Servlets | AuthServlet, AppServlet, FileServlet |
| JSP, EL and JSTL | WEB-INF/views, `<c:forEach>`, `<c:if>`, `<c:out>` |
| Filters | SecurityFilter + filter mapping in web.xml |
| Sessions | HttpSession login state, expiry, CSRF token, ID rotation |
| Listeners | ServletContextListener in AppContext |
| JDBC | PreparedStatement, generated keys, ResultSet, joins |
| Transactions | Database.tx; submission and voting |
| Multipart handling | `<multipart-config>` in web.xml; `request.getPart()` |
| Java cryptography | MessageDigest SHA-256 |
| MVC | controllers → service/DAO → JSP view |

The project maps these concepts; confirm the exact checklist with your instructor's syllabus. Multipart configuration is in web.xml, equivalent in purpose to `@MultipartConfig`, avoiding duplicate mappings.

## Viva questions

**1. Why SHA-256?** It generates a deterministic 256-bit fingerprint. The same bytes give the same digest; a changed file almost certainly gives a different digest. Output is 32 bytes represented as 64 hexadecimal characters.

**2. Is hashing encryption?** No. Hashing is one-way fingerprinting. Encryption protects content confidentiality and is reversible with the appropriate key. Files here are access-controlled, not encrypted at rest by the application.

**3. Does the hash prove who invented an idea?** No. It checks file contents against a stored fingerprint. Authorship, novelty and legal rights require separate evidence and legal processes.

**4. Why BCrypt for passwords instead of SHA-256?** BCrypt is salted and deliberately expensive, making password guessing harder. Fast SHA-256 alone is unsuitable for password storage.

**5. Servlet versus JSP?** Servlets handle requests and coordinate application logic; JSPs render HTML views using data supplied by controllers.

**6. What does a Filter do?** It intercepts requests before the target servlet, enforcing shared security rules. Service-level record checks still enforce ownership/assignment.

**7. What is HttpSession?** Server-side state associated with the browser's session cookie. This app stores the current user and CSRF token, rotates the session ID at login, and invalidates it on logout.

**8. What prevents SQL injection?** PreparedStatement parameters keep user input separate from SQL syntax.

**9. What is a transaction?** A group of database operations committed together or rolled back on failure. Submission metadata, version and audit events are one database transaction; file operations require separate compensation.

**10. Why use a connection pool?** It reuses database connections across requests. AppContext creates the pool at application startup and closes it at shutdown.

**11. How do you stop two votes by the same reviewer?** A reviewer has a unique assignment per version, each assignment permits one review_votes row, and the service rejects existing votes under a version row lock.

**12. Why are historical title and summary copied into document_versions?** A later edit must not silently change the description of an earlier submitted version.

**13. Why store uploads outside the webroot?** It prevents direct public URLs and server execution. An authorized download servlet controls access and uses attachment disposition.

**14. Is this a blockchain?** No. It is a relational research ledger. Database/server administrators remain trusted and audit logs are not independently tamper-evident.

**15. Is the demo using MySQL?** The quick demo uses H2 in MySQL compatibility mode. The external Tomcat deployment uses MySQL Connector/J and the supplied MySQL schema.

**16. Why no Java scriptlets in the views?** JSTL/EL keep data display separate from Java business logic. Only the tiny root index.jsp contains a redirect; all main views use JSP tags.

**17. What are the important limitations?** No trusted timestamp authority, legally certified prior art, independent signature verification, plagiarism detection, public patent search, or malware scanner. Basic per-session login throttling is not production rate limiting.

## Possible future extensions

Trusted timestamp authority (RFC 3161), signed receipts with key rotation, independently anchored audit chains, object storage with encryption, formal reviewer reassignment, stronger rate limiting, backup/recovery automation, and paginated dashboards. Each should be treated as a separate feature with a clearly defined trust model.
