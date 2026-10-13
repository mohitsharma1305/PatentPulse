# Team module READMEs

These files describe assigned ownership, not independently runnable applications. Paths are relative to the project root containing pom.xml.

| Member | Module | README |
|---|---|---|
| Mohit | Research drafts uploads and version history | [Mohit](README_Mohit.md) |
| Meet | Authentication sessions and access control | [Meet](README_Meet.md) |
| Jay | File hashing verification and downloads | [Jay](README_Jay.md) |
| Nitesh | Reviewer assignments decisions and feedback | [Nitesh](README_Nitesh.md) |
| Mann | Administration audit and receipts | [Mann](README_Mann.md) |

`LedgerService.java` is shared by all five. `AppServlet.java` is shared by Mohit, Jay, Nitesh and Mann. `document.jsp` is shared by Mohit, Jay and Nitesh. `UploadStore.java` is shared by Mohit and Jay. Database changes require coordination across affected modules.
