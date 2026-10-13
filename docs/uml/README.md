# PatentPulse UML and database design

These diagrams describe the supplied implementation. Mermaid blocks render on GitHub. The formal use-case source is also supplied as [use-cases.puml](use-cases.puml) for PlantUML. Flowcharts below are explanatory architecture/activity views; the class, sequence and state diagrams use Mermaid's UML-oriented notation. The ER diagram is a database design view.

## Use cases

| Actor | Main use cases |
|---|---|
| Visitor | Register, log in |
| Authenticated user | Maintain own drafts, submit versions, list own research |
| Owner, assigned reviewer or admin | View authorised version, compare candidate file, download, view receipt |
| Reviewer | View assigned queue, submit one decision per assignment |
| Admin | Assign reviewers, manage non-admin accounts, inspect audit events |

Current personal research routes accept any authenticated role and remain owner-scoped. Restricting creation to researchers only is not enforced by an additional role check in the supplied baseline.

```mermaid
flowchart TD
  V[Visitor] --> A[Register or log in]
  A --> U[Authenticated user]
  U --> D[Own drafts and submissions]
  U --> X[Authorised version access]
  R[Assigned reviewer] --> Q[Queue and decision]
  M[Administrator] --> G[Assignments and account management]
  M --> L[Audit view]
```

## Class diagram

Method lists are selective; they do not reproduce every helper or constructor. Persistence records other than User are represented by maps in the Java implementation, not invented entity classes.

```mermaid
classDiagram
  class SecurityFilter {
    +doFilter(request, response, chain)
  }
  class AuthServlet {
    +doGet(request, response)
    +doPost(request, response)
  }
  class AppServlet {
    +doGet(request, response)
    +doPost(request, response)
  }
  class FileServlet {
    +doGet(request, response)
  }
  class AuthService {
    +register(name, email, password)
    +login(email, password)
    +current(id)
  }
  class LedgerService {
    +saveDraft(user, id, title, field, summary)
    +submit(user, id, title, field, summary, file)
    +version(user, id)
    +history(user, version)
    +assign(user, version, reviewer)
    +vote(user, version, decision, comments)
    +verify(user, version, stream)
  }
  class UploadStore {
    +save(part) Saved
    +path(name) Path
    +delete(file)
  }
  class Hashing {
    +sha256(stream) String
  }
  class ReviewPolicy {
    +status(approvals, rejections) String
    +terminal(status) boolean
  }
  class Database {
    +query(sql, parameters)
    +tx(work)
    +audit(connection, actor, action, type, target, details)
  }
  class User {
    +getId()
    +isAdmin()
    +isReviewer()
  }
  SecurityFilter --> AuthService : refresh account
  AuthServlet --> AuthService
  AuthService --> User : returns
  AuthService --> Database
  AppServlet --> LedgerService
  AppServlet --> UploadStore
  FileServlet --> LedgerService : authorise
  FileServlet --> UploadStore : resolve file
  LedgerService --> Database
  LedgerService --> ReviewPolicy
  LedgerService --> Hashing : compare upload
  UploadStore --> Hashing
```

## Submission sequence

The filter has authenticated the request and checked the POST token before this sequence begins. File validation failure ends before persistence. Database failure triggers attempted file cleanup.

```mermaid
sequenceDiagram
  actor User
  participant App as AppServlet
  participant Store as UploadStore
  participant Ledger as LedgerService
  participant DB as Database
  User->>App: POST /app/submit with metadata and file
  App->>Store: save(part)
  Store->>Store: Validate and store bytes then compute hash
  Store-->>App: Saved metadata
  App->>Ledger: submit(user, metadata, saved)
  Ledger->>DB: Transaction and version insert
  alt Database commit succeeds
    DB-->>Ledger: Version ID
    Ledger-->>App: Version ID
    App-->>User: Redirect to version page
  else Database operation fails
    DB-->>Ledger: Roll back and error
    Ledger-->>App: Error
    App->>Store: Attempt delete(saved)
    App-->>User: Error response
  end
```

## Verification sequence

```mermaid
sequenceDiagram
  actor User
  participant App as AppServlet
  participant Ledger as LedgerService
  participant Hash as Hashing
  participant DB as Database
  User->>App: POST /app/verify with version ID and candidate
  App->>Ledger: verify(user, version, stream)
  Ledger->>DB: Read version and check access
  Ledger->>Hash: sha256(candidate stream)
  Hash-->>Ledger: Candidate digest
  Ledger->>Ledger: Compare candidate digest with recorded hash
  Ledger->>DB: Record INTEGRITY_MATCH or INTEGRITY_MISMATCH
  Ledger-->>App: Result
  App-->>User: Redirect with verification result
```

## Version state diagram

Draft describes a parent research item before its first version, and is not stored in document_versions.status. Each new version starts at SUBMITTED. A terminal version never returns to an earlier state.

```mermaid
stateDiagram-v2
  [*] --> SUBMITTED : version created
  SUBMITTED --> IN_REVIEW : first reviewer assigned
  IN_REVIEW --> IN_REVIEW : first approval or second assignment
  IN_REVIEW --> INTERNALLY_REVIEWED : second approval
  IN_REVIEW --> NEEDS_REVISION : any rejection
  INTERNALLY_REVIEWED --> [*]
  NEEDS_REVISION --> [*]
```

## Review activity view

```mermaid
flowchart TD
  A[Reviewer submits decision] --> B{Assigned and nonterminal?}
  B -->|No| X[Reject request]
  B -->|Yes| C{Already voted?}
  C -->|Yes| X
  C -->|No| D[Store vote in transaction]
  D --> E{Any rejection?}
  E -->|Yes| F[NEEDS_REVISION]
  E -->|No| G{Two approvals?}
  G -->|Yes| H[INTERNALLY_REVIEWED]
  G -->|No| I[IN_REVIEW]
```

## Deployment view

```mermaid
flowchart TD
  B[Browser] --> T[Tomcat 9 with PatentPulse WAR]
  T --> S[Servlets filters and services]
  S --> P[HikariCP connection pool]
  P --> DB[(MySQL)]
  S --> F[Private upload directory]
```

The local demo substitutes embedded Tomcat and H2. Public TLS termination and backup infrastructure are deployment responsibilities and are not supplied as application services.

## Entity relationship diagram

```mermaid
erDiagram
  users ||--o{ blueprints : owns
  blueprints ||--o{ document_versions : contains
  document_versions ||--o{ review_assignments : receives
  users ||--o{ review_assignments : reviews
  users ||--o{ review_assignments : assigns
  review_assignments ||--o| review_votes : records
  users o|--o{ audit_logs : acts
  users {
    bigint id PK
    varchar email UK
    varchar password_hash
    varchar role
    boolean active
  }
  blueprints {
    bigint id PK
    bigint owner_id FK
    varchar title
    varchar field
    text summary
  }
  document_versions {
    bigint id PK
    bigint blueprint_id FK
    int version_number
    varchar stored_name UK
    char sha256
    bigint size_bytes
    varchar status
    timestamp submitted_at
  }
  review_assignments {
    bigint id PK
    bigint version_id FK
    bigint reviewer_id FK
    bigint assigned_by FK
  }
  review_votes {
    bigint id PK
    bigint assignment_id FK,UK
    varchar decision
    text comments
  }
  audit_logs {
    bigint id PK
    bigint actor_id FK
    varchar action
    varchar target_type
    bigint target_id
  }
```

Additional constraints: (blueprint_id, version_number) and (version_id, reviewer_id) are unique. The two-reviewer limit is implemented in the service, not by an ER cardinality constraint. audit_logs.target_id is a polymorphic reference and has no declared foreign key. A receipt is a view of a version.
