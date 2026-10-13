# Meet module

## Authentication sessions and access control

Registration forces RESEARCHER. Registered passwords use BCrypt cost 12. Login rotates session ID and CSRF token. Sessions expire after 30 minutes. The filter refreshes account role and active status on requests.

## Files and responsibility

| Project path | Responsibility |
|---|---|
| `src/main/java/com/patentpulse/web/AuthServlet.java` | Login, registration, logout, session rotation and session-scoped delay. |
| `src/main/java/com/patentpulse/service/AuthService.java` | Account validation, BCrypt and active-user lookup. |
| `src/main/java/com/patentpulse/web/SecurityFilter.java` | Route checks, CSRF, response headers and current-account refresh. |
| `src/main/java/com/patentpulse/model/User.java` | Session identity and role helpers. |
| `src/main/webapp/WEB-INF/views/auth.jsp` | Login and registration UI. |
| `src/main/webapp/WEB-INF/views/csrf.jspf` | CSRF hidden-field fragment. |
| `src/main/java/com/patentpulse/service/LedgerService.java` | Coordinate service-level role and ownership checks with all owners. |

## Routes

GET/POST /login, GET/POST /register, POST /logout; filter mapped to /*

## Data

users, shared with Mann

## Shared coordination

Coordinate users-table fields and account-state changes with Mann. Every module still needs per-record service authorisation. The one-minute delay after five failures is session-scoped, not account-wide.

## Acceptance checks

1. Register normally and attempt to submit an elevated role.
2. Check duplicate email and invalid credentials.
3. Check login/logout and protected access after logout.
4. Send a POST without the CSRF token.
5. Deactivate an account through admin and inspect its later access.

These are checks to run, not fresh passing results. Use the final merged source and record actual output.

## Git workflow

```sh
git switch main
git pull --ff-only origin main
git switch -c meet-module-update
```

Use a new branch name if it already exists. Copy or edit only agreed files, inspect `git diff`, stage exact paths and commit a description of the real changes. Push the branch and open a pull request into main. Do not replace newer shared files with old copies from the ZIP.
