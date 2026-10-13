# Jay module

## File hashing verification and downloads

UploadStore hashes a stored submission. Verification hashes a separate candidate stream and compares it to the selected version. Download checks authorisation and file existence, then streams the stored bytes. It does not automatically rehash the stored file.

## Files and responsibility

| Project path | Responsibility |
|---|---|
| `src/main/java/com/patentpulse/util/Hashing.java` | Streaming SHA-256 utility. |
| `src/main/java/com/patentpulse/web/FileServlet.java` | Authorised attachment download. |
| `src/test/java/com/patentpulse/HashingTest.java` | Known vectors, changed bytes, empty stream and read boundaries. |
| `src/main/java/com/patentpulse/service/LedgerService.java` | verify and the stored-hash comparison. Shared with all five. |
| `src/main/java/com/patentpulse/service/UploadStore.java` | Upload-time hash integration, shared with Mohit. |
| `src/main/java/com/patentpulse/web/AppServlet.java` | POST /verify handling, shared with Mohit, Nitesh and Mann. |
| `src/main/webapp/WEB-INF/views/document.jsp` | Hash display, verification result and download link, shared with Mohit and Nitesh. |

## Routes

POST /app/verify and GET /file?id=<version-id>

## Data

document_versions.sha256, stored_name, original_name and size_bytes; verification audit events

## Shared coordination

Coordinate storage and upload cleanup with Mohit, access checks with Meet and verification audit records with Mann. Hashing is not encryption or legal proof. Do not claim automatic corrupted-download blocking without implementing and testing it.

## Acceptance checks

1. Run HashingTest and retain observed results.
2. Compare the original sample and expect MATCH.
3. Compare the altered sample and expect MISMATCH.
4. Check that unauthorised version identifiers cannot be downloaded.
5. Check missing stored-file handling.

These are checks to run, not fresh passing results. Use the final merged source and record actual output.

## Git workflow

```sh
git switch main
git pull --ff-only origin main
git switch -c jay-module-update
```

Use a new branch name if it already exists. Copy or edit only agreed files, inspect `git diff`, stage exact paths and commit a description of the real changes. Push the branch and open a pull request into main. Do not replace newer shared files with old copies from the ZIP.
