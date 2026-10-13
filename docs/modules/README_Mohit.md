# Mohit module

## Research drafts uploads and version history

Saving metadata creates a draft. Submission writes the file and inserts a metadata snapshot with a new version number. Later submissions preserve old snapshots. AppServlet attempts storage cleanup when submission persistence fails.

## Files and responsibility

| Project path | Responsibility |
|---|---|
| `src/main/java/com/patentpulse/service/UploadStore.java` | File validation, safe storage and cleanup. Shared with Jay for hashing. |
| `src/main/webapp/WEB-INF/views/editor.jsp` | Draft, upload and submission form. |
| `src/main/webapp/WEB-INF/views/dashboard.jsp` | Own research listing, search and status filtering. |
| `src/main/webapp/WEB-INF/views/document.jsp` | Research details and version-history section. Shared with Jay and Nitesh. |
| `src/main/java/com/patentpulse/service/LedgerService.java` | mine, draft, saveDraft, submit, validate and history. Shared with all five. |
| `src/main/java/com/patentpulse/web/AppServlet.java` | Dashboard, editor, draft and submission routes. Shared with Jay, Nitesh and Mann. |

## Routes

GET /app/dashboard, GET /app/new, POST /app/draft, POST /app/submit, GET /app/document

## Data

blueprints and document_versions

## Shared coordination

Meet supplies the current session user; Jay owns SHA-256 implementation; Nitesh adds review sections; Mann uses version data for receipts. Mohit coordinates root build files, schemas and shared assets.

## Acceptance checks

1. Create a draft, reopen it and confirm its owner-scoped fields.
2. Submit a supported file and inspect stored metadata.
3. Reject empty, oversized and unsupported files.
4. Submit a second version and confirm the earlier snapshot still exists.
5. Verify another user cannot edit the record.

These are checks to run, not fresh passing results. Use the final merged source and record actual output.

## Git workflow

```sh
git switch main
git pull --ff-only origin main
git switch -c mohit-module-update
```

Use a new branch name if it already exists. Copy or edit only agreed files, inspect `git diff`, stage exact paths and commit a description of the real changes. Push the branch and open a pull request into main. Do not replace newer shared files with old copies from the ZIP.
