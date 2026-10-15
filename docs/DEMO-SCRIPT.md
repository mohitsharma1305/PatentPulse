# Seven-minute lab demonstration

## Before presenting

Run the demo or MySQL deployment. Have four browser profiles or log in/out between roles. Use files from `samples/`; no actual unpublished research is required.

1. **Problem — 45 seconds.** “Research teams need to track document versions, check whether a copy changed, and record internal evaluations. PatentPulse provides an integrity ledger. It does not grant patents or prove inventorship.”
2. **Researcher — 60 seconds.** Sign in as researcher. Create “Research Document Integrity Study”, field “Cybersecurity”, and a summary of at least 20 characters. Save as draft; explain why a metadata-only draft has no file fingerprint. Upload `samples/research-v1.txt` and record it.
3. **Fingerprint — 45 seconds.** Open the new record. Show version number, timestamp, original filename and 64-character SHA-256. Check integrity using `research-v1.txt` (MATCH), then `research-v1-altered.txt` (MISMATCH). The comparison file is streamed and not retained.
4. **Review assignment — 45 seconds.** Sign in as admin. Open All submissions → the version → assign Reviewer One and Reviewer Two. Each is a distinct reviewer; the author cannot self-review.
5. **Review decisions — 60 seconds.** Reviewer One opens Review queue, downloads the file and approves with comments. It remains In Review. Reviewer Two approves; it becomes Internally Reviewed. Repeated voting is rejected.
6. **New version — 45 seconds.** Researcher uploads `research-v2.txt` as a new version. Show that v1 retains its original fingerprint and review result, while v2 starts a separate review. Old version reviewers have no automatic access to v2 until assigned.
7. **Receipt and audit — 45 seconds.** Print the v1 receipt using Save PDF. Show admin audit entries for submission, verification and votes.
8. **Technical explanation — 45 seconds.** Point to web.xml (Servlet/Filter/Listener mappings), Hashing.java, LedgerService transaction and review methods, Database PreparedStatements, JSP JSTL loops, and the MySQL foreign keys.

Optional security demonstration: use a researcher account to open `/app/admin/users` (403) or an unassigned reviewer to request a version (404). Explain that hiding menu links alone would not protect data.
