# Nitesh module

## Reviewer assignments decisions and feedback

Admins assign up to two distinct active reviewers. Self-review is rejected. Each assignment accepts one vote. Any rejection closes the version as NEEDS_REVISION; two approvals produce INTERNALLY_REVIEWED. Comments require 10–3000 characters under the source validation rules.

## Files and responsibility

| Project path | Responsibility |
|---|---|
| `src/main/java/com/patentpulse/service/ReviewPolicy.java` | Status calculation and terminal-state helper. |
| `src/main/webapp/WEB-INF/views/queue.jsp` | Reviewer and administrator queue. |
| `src/test/java/com/patentpulse/ReviewPolicyTest.java` | Threshold, rejection and terminal-state tests. |
| `src/main/java/com/patentpulse/service/LedgerService.java` | reviews, queue, reviewers, assign, lockVersion and vote. Shared with all five. |
| `src/main/java/com/patentpulse/web/AppServlet.java` | Queue, assignment and decision routes, shared with Mohit, Jay and Mann. |
| `src/main/webapp/WEB-INF/views/document.jsp` | Assignment controls, decision form and feedback. Shared with Mohit and Jay. |

## Routes

GET /app/review, GET /app/admin/queue, POST /app/admin/assign, POST /app/review/vote

## Data

review_assignments, review_votes and document_versions.status

## Shared coordination

Although admins use assignment controls, this development feature belongs to Nitesh. Coordinate version IDs with Mohit, permission checks with Meet and audit events with Mann.

## Acceptance checks

1. Reject duplicate, third and self-review assignments.
2. Confirm one approval leaves IN_REVIEW.
3. Confirm two approvals finish the review.
4. Confirm any rejection requires a new revision.
5. Reject duplicate votes and decisions on terminal versions.

These are checks to run, not fresh passing results. Use the final merged source and record actual output.

## Git workflow

```sh
git switch main
git pull --ff-only origin main
git switch -c nitesh-module-update
```

Use a new branch name if it already exists. Copy or edit only agreed files, inspect `git diff`, stage exact paths and commit a description of the real changes. Push the branch and open a pull request into main. Do not replace newer shared files with old copies from the ZIP.
