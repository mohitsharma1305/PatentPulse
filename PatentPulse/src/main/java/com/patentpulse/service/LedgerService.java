package com.patentpulse.service;

import static com.patentpulse.dao.Database.*;

import com.patentpulse.dao.Database;
import com.patentpulse.model.User;
import com.patentpulse.util.Problem;
import java.sql.*;
import java.util.*;

public class LedgerService {
  private final Database db;

  public LedgerService(Database db) {
    this.db = db;
  }

  private static final String VERSION =
      "SELECT v.*,b.owner_id,u.name AS owner_name FROM document_versions v JOIN blueprints b ON"
          + " b.id=v.blueprint_id JOIN users u ON u.id=b.owner_id ";

  public List<Map<String, Object>> mine(User u, String q, String status) throws Exception {
    String sql =
        "SELECT b.*,v.id AS version_id,v.version_number,v.status,v.sha256,v.submitted_at FROM"
            + " blueprints b LEFT JOIN document_versions v ON v.blueprint_id=b.id AND"
            + " v.version_number=(SELECT MAX(x.version_number) FROM document_versions x WHERE"
            + " x.blueprint_id=b.id) WHERE b.owner_id=? AND (LOWER(b.title) LIKE ? OR"
            + " LOWER(b.field) LIKE ?)";
    List<Object> a =
        new ArrayList<>(
            List.of(
                u.getId(),
                "%" + q.toLowerCase(Locale.ROOT) + "%",
                "%" + q.toLowerCase(Locale.ROOT) + "%"));
    if (status.equals("DRAFT")) sql += " AND v.id IS NULL";
    else if (!status.isBlank()) {
      sql += " AND v.status=?";
      a.add(status);
    }
    return db.query(sql + " ORDER BY b.id DESC", a.toArray());
  }

  public Map<String, Object> draft(User u, long id) throws Exception {
    var b = one(db.query("SELECT * FROM blueprints WHERE id=? AND owner_id=?", id, u.getId()));
    Problem.require(b != null, 404, "Research record not found.");
    return b;
  }

  public long saveDraft(User u, long id, String title, String field, String summary)
      throws Exception {
    validate(title, field, summary);
    return db.tx(
        c -> {
          long result = id;
          if (id == 0)
            result =
                insert(
                    c,
                    "INSERT INTO blueprints(owner_id,title,field,summary) VALUES(?,?,?,?)",
                    u.getId(),
                    title.trim(),
                    field.trim(),
                    summary.trim());
          else {
            var b = one(query(c, "SELECT * FROM blueprints WHERE id=? FOR UPDATE", id));
            Problem.require(
                b != null && id(b, "owner_id") == u.getId(), 404, "Research record not found.");
            Problem.require(
                query(c, "SELECT id FROM document_versions WHERE blueprint_id=?", id).isEmpty(),
                409,
                "Submitted records are immutable. Upload a new version instead.");
            update(
                c,
                "UPDATE blueprints SET title=?,field=?,summary=? WHERE id=?",
                title.trim(),
                field.trim(),
                summary.trim(),
                id);
          }
          Database.audit(c, u.getId(), "SAVE_DRAFT", "BLUEPRINT", result, "Draft metadata saved");
          return result;
        });
  }

  public long submit(
      User u, long blueprintId, String title, String field, String summary, UploadStore.Saved file)
      throws Exception {
    validate(title, field, summary);
    return db.tx(
        c -> {
          long bid = blueprintId;
          if (bid == 0)
            bid =
                insert(
                    c,
                    "INSERT INTO blueprints(owner_id,title,field,summary) VALUES(?,?,?,?)",
                    u.getId(),
                    title.trim(),
                    field.trim(),
                    summary.trim());
          else {
            var b = one(query(c, "SELECT * FROM blueprints WHERE id=? FOR UPDATE", bid));
            Problem.require(
                b != null && id(b, "owner_id") == u.getId(), 404, "Research record not found.");
            update(
                c,
                "UPDATE blueprints SET title=?,field=?,summary=? WHERE id=?",
                title.trim(),
                field.trim(),
                summary.trim(),
                bid);
          }
          int number =
              ((Number)
                      one(query(
                              c,
                              "SELECT COALESCE(MAX(version_number),0)+1 AS next_number FROM"
                                  + " document_versions WHERE blueprint_id=?",
                              bid))
                          .get("next_number"))
                  .intValue();
          long vid =
              insert(
                  c,
                  "INSERT INTO"
                      + " document_versions(blueprint_id,version_number,title,field,summary,original_name,stored_name,sha256,size_bytes)"
                      + " VALUES(?,?,?,?,?,?,?,?,?)",
                  bid,
                  number,
                  title.trim(),
                  field.trim(),
                  summary.trim(),
                  file.original(),
                  file.stored(),
                  file.hash(),
                  file.size());
          Database.audit(c, u.getId(), "SUBMIT_VERSION", "VERSION", vid, "SHA-256 " + file.hash());
          return vid;
        });
  }

  private void validate(String title, String field, String summary) {
    Problem.require(
        title != null && title.trim().length() >= 3 && title.length() <= 180,
        400,
        "Title must contain 3–180 characters.");
    Problem.require(
        field != null && !field.isBlank() && field.length() <= 80,
        400,
        "Field is required (up to 80 characters).");
    Problem.require(
        summary != null && summary.trim().length() >= 20 && summary.length() <= 10000,
        400,
        "Summary must contain 20–10,000 characters.");
  }

  public Map<String, Object> version(User u, long id) throws Exception {
    var v = one(db.query(VERSION + "WHERE v.id=?", id));
    Problem.require(v != null, 404, "Version not found.");
    boolean allowed =
        u.isAdmin()
            || id(v, "owner_id") == u.getId()
            || (u.isReviewer()
                && !db.query(
                        "SELECT id FROM review_assignments WHERE version_id=? AND reviewer_id=?",
                        id,
                        u.getId())
                    .isEmpty());
    Problem.require(allowed, 404, "Version not found.");
    return v;
  }

  public List<Map<String, Object>> history(User u, Map<String, Object> v) throws Exception {
    if (u.isAdmin() || id(v, "owner_id") == u.getId())
      return db.query(
          "SELECT id,version_number,submitted_at,status,sha256 FROM document_versions WHERE"
              + " blueprint_id=? ORDER BY version_number DESC",
          v.get("blueprint_id"));
    return db.query(
        "SELECT v.id,v.version_number,v.submitted_at,v.status,v.sha256 FROM document_versions v"
            + " JOIN review_assignments a ON a.version_id=v.id WHERE v.blueprint_id=? AND"
            + " a.reviewer_id=? ORDER BY v.version_number DESC",
        v.get("blueprint_id"),
        u.getId());
  }

  public List<Map<String, Object>> reviews(User u, long version) throws Exception {
    version(u, version);
    String sql =
        "SELECT a.id,a.reviewer_id,u.name AS reviewer_name,r.decision,r.comments,r.voted_at FROM"
            + " review_assignments a JOIN users u ON u.id=a.reviewer_id LEFT JOIN review_votes r ON"
            + " r.assignment_id=a.id WHERE a.version_id=?";
    var rows = db.query(sql, version);
    if (!u.isAdmin())
      for (var row : rows) {
        row.remove("reviewer_name");
        row.remove("reviewer_id");
      }
    return rows;
  }

  public List<Map<String, Object>> queue(User u) throws Exception {
    if (u.isAdmin()) return db.query(VERSION + " ORDER BY v.id DESC");
    Problem.require(u.isReviewer(), 403, "Reviewer access required.");
    return db.query(
        "SELECT v.*,a.id AS assignment_id,r.decision FROM review_assignments a JOIN"
            + " document_versions v ON v.id=a.version_id LEFT JOIN review_votes r ON"
            + " r.assignment_id=a.id WHERE a.reviewer_id=? ORDER BY a.id DESC",
        u.getId());
  }

  public List<Map<String, Object>> users(User u) throws Exception {
    Problem.require(u.isAdmin(), 403, "Admin access required.");
    return db.query("SELECT id,name,email,role,active,created_at FROM users ORDER BY id");
  }

  public List<Map<String, Object>> reviewers(User u) throws Exception {
    Problem.require(u.isAdmin(), 403, "Admin access required.");
    return db.query(
        "SELECT id,name,email FROM users WHERE active=TRUE AND role='REVIEWER' ORDER BY name");
  }

  public void assign(User u, long version, long reviewer) throws Exception {
    Problem.require(u.isAdmin(), 403, "Admin access required.");
    db.tx(
        c -> {
          var v = lockVersion(c, version);
          Problem.require(
              !ReviewPolicy.terminal((String) v.get("status")),
              409,
              "This review has already finished.");
          Problem.require(
              id(v, "owner_id") != reviewer, 400, "Authors cannot review their own work.");
          Problem.require(
              !query(
                      c,
                      "SELECT id FROM users WHERE id=? AND role='REVIEWER' AND active=TRUE FOR"
                          + " UPDATE",
                      reviewer)
                  .isEmpty(),
              400,
              "Choose an active reviewer.");
          var assignments =
              query(c, "SELECT reviewer_id FROM review_assignments WHERE version_id=?", version);
          Problem.require(assignments.size() < 2, 409, "Two reviewers are already assigned.");
          for (var a : assignments)
            Problem.require(id(a, "reviewer_id") != reviewer, 409, "Reviewer already assigned.");
          insert(
              c,
              "INSERT INTO review_assignments(version_id,reviewer_id,assigned_by) VALUES(?,?,?)",
              version,
              reviewer,
              u.getId());
          update(c, "UPDATE document_versions SET status='IN_REVIEW' WHERE id=?", version);
          Database.audit(
              c, u.getId(), "ASSIGN_REVIEWER", "VERSION", version, "Assigned reviewer " + reviewer);
          return null;
        });
  }

  private Map<String, Object> lockVersion(Connection c, long id) throws Exception {
    var v = one(query(c, "SELECT * FROM document_versions WHERE id=? FOR UPDATE", id));
    Problem.require(v != null, 404, "Version not found.");
    var b = one(query(c, "SELECT owner_id FROM blueprints WHERE id=?", v.get("blueprint_id")));
    v.put("owner_id", b.get("owner_id"));
    return v;
  }

  public void vote(User u, long version, String decision, String comments) throws Exception {
    Problem.require(u.isReviewer(), 403, "Reviewer access required.");
    Problem.require(Set.of("APPROVE", "REJECT").contains(decision), 400, "Invalid decision.");
    Problem.require(
        comments != null && comments.trim().length() >= 10 && comments.length() <= 3000,
        400,
        "Review comments must contain 10–3,000 characters.");
    db.tx(
        c -> {
          var v = lockVersion(c, version);
          Problem.require(id(v, "owner_id") != u.getId(), 403, "Self-review is not allowed.");
          var a =
              one(
                  query(
                      c,
                      "SELECT id FROM review_assignments WHERE version_id=? AND reviewer_id=?",
                      version,
                      u.getId()));
          Problem.require(a != null, 403, "You are not assigned to this version.");
          Problem.require(
              query(c, "SELECT id FROM review_votes WHERE assignment_id=?", a.get("id")).isEmpty(),
              409,
              "Your review has already been submitted.");
          Problem.require(
              !ReviewPolicy.terminal((String) v.get("status")),
              409,
              "This review has already finished.");
          insert(
              c,
              "INSERT INTO review_votes(assignment_id,decision,comments) VALUES(?,?,?)",
              a.get("id"),
              decision,
              comments.trim());
          var votes =
              query(
                  c,
                  "SELECT r.decision FROM review_votes r JOIN review_assignments a ON"
                      + " a.id=r.assignment_id WHERE a.version_id=?",
                  version);
          long approvals = votes.stream().filter(r -> r.get("decision").equals("APPROVE")).count();
          long rejects = votes.size() - approvals;
          String status = ReviewPolicy.status(approvals, rejects);
          update(c, "UPDATE document_versions SET status=? WHERE id=?", status, version);
          Database.audit(c, u.getId(), "REVIEW_" + decision, "VERSION", version, status);
          return null;
        });
  }

  public String verify(User u, long version, java.io.InputStream stream) throws Exception {
    var v = version(u, version);
    String digest = com.patentpulse.util.Hashing.sha256(stream);
    String result = digest.equals(v.get("sha256")) ? "MATCH" : "MISMATCH";
    db.tx(
        c -> {
          Database.audit(
              c,
              u.getId(),
              "INTEGRITY_" + result,
              "VERSION",
              version,
              "Compared SHA-256 " + digest);
          return null;
        });
    return result;
  }

  public void changeUser(User u, long target, String role, boolean active) throws Exception {
    Problem.require(u.isAdmin(), 403, "Admin access required.");
    Problem.require(
        Set.of("RESEARCHER", "REVIEWER").contains(role),
        400,
        "Role must be Researcher or Reviewer.");
    db.tx(
        c -> {
          var person = one(query(c, "SELECT * FROM users WHERE id=? FOR UPDATE", target));
          Problem.require(person != null, 404, "User not found.");
          Problem.require(
              !person.get("role").equals("ADMIN"), 400, "Admin accounts cannot be changed here.");
          if (!active || !role.equals("REVIEWER"))
            Problem.require(
                query(
                        c,
                        "SELECT a.id FROM review_assignments a JOIN document_versions v ON"
                            + " v.id=a.version_id LEFT JOIN review_votes r ON r.assignment_id=a.id"
                            + " WHERE a.reviewer_id=? AND r.id IS NULL AND v.status IN"
                            + " ('SUBMITTED','IN_REVIEW')",
                        target)
                    .isEmpty(),
                409,
                "This reviewer has unfinished assignments. Complete those reviews first.");
          update(c, "UPDATE users SET role=?,active=? WHERE id=?", role, active, target);
          Database.audit(c, u.getId(), "UPDATE_USER", "USER", target, role + " / active=" + active);
          return null;
        });
  }

}
