package com.patentpulse.web;

import com.patentpulse.model.User;
import com.patentpulse.service.*;
import com.patentpulse.util.Problem;
import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class AppServlet extends HttpServlet {
  private LedgerService ledger() {
    return (LedgerService) getServletContext().getAttribute("ledger");
  }

  private User user(HttpServletRequest r) {
    return (User) r.getSession().getAttribute("user");
  }

  protected void doGet(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    try {
      String path = r.getPathInfo() == null ? "/dashboard" : r.getPathInfo();
      User u = user(r);
      r.setAttribute("page", path);
      switch (path) {
        case "/dashboard":
          var rows = ledger().mine(u, param(r, "q"), param(r, "status"));
          r.setAttribute("records", rows);
          var all = ledger().mine(u, "", "");
          r.setAttribute("totalCount", all.size());
          r.setAttribute(
              "draftCount", all.stream().filter(x -> x.get("version_id") == null).count());
          r.setAttribute(
              "reviewedCount",
              all.stream().filter(x -> "INTERNALLY_REVIEWED".equals(x.get("status"))).count());
          view(r, s, "dashboard");
          break;
        case "/new":
          if (!param(r, "id").isBlank()) {
            r.setAttribute("draft", ledger().draft(u, id(r, "id")));
            r.setAttribute(
                "hasVersions",
                !((com.patentpulse.dao.Database) getServletContext().getAttribute("db"))
                    .query("SELECT id FROM document_versions WHERE blueprint_id=?", id(r, "id"))
                    .isEmpty());
          }
          view(r, s, "editor");
          break;
        case "/document":
          var v = ledger().version(u, id(r, "id"));
          r.setAttribute("doc", v);
          r.setAttribute("history", ledger().history(u, v));
          r.setAttribute("reviews", ledger().reviews(u, id(r, "id")));
          if (u.isAdmin()) r.setAttribute("reviewers", ledger().reviewers(u));
          if (u.isReviewer()) {
            var assignments =
                ((com.patentpulse.dao.Database) getServletContext().getAttribute("db"))
                    .query(
                        "SELECT a.id FROM review_assignments a LEFT JOIN review_votes r ON"
                            + " r.assignment_id=a.id WHERE a.version_id=? AND a.reviewer_id=? AND"
                            + " r.id IS NULL",
                        id(r, "id"),
                        u.getId());
            r.setAttribute(
                "canVote",
                !assignments.isEmpty() && !ReviewPolicy.terminal(v.get("status").toString()));
          }
          view(r, s, "document");
          break;
        case "/review":
        case "/admin/queue":
          r.setAttribute("records", ledger().queue(u));
          view(r, s, "queue");
          break;
        case "/admin/users":
          r.setAttribute("users", ledger().users(u));
          view(r, s, "users");
          break;
        case "/admin/audit":
          r.setAttribute("events", ledger().audit(u));
          view(r, s, "audit");
          break;
        default:
          throw new Problem(404, "Page not found.");
      }
    } catch (Problem e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected void doPost(HttpServletRequest r, HttpServletResponse s)
      throws ServletException, IOException {
    try {
      User u = user(r);
      String path = r.getPathInfo();
      long result;
      switch (path) {
        case "/draft":
          result =
              ledger()
                  .saveDraft(
                      u,
                      optionalId(r, "blueprintId"),
                      param(r, "title"),
                      param(r, "field"),
                      param(r, "summary"));
          redirect(r, s, "/app/new?id=" + result + "&saved=1");
          break;
        case "/submit":
          UploadStore store = (UploadStore) getServletContext().getAttribute("store");
          UploadStore.Saved saved = store.save(r.getPart("document"));
          try {
            result =
                ledger()
                    .submit(
                        u,
                        optionalId(r, "blueprintId"),
                        param(r, "title"),
                        param(r, "field"),
                        param(r, "summary"),
                        saved);
          } catch (Exception e) {
            try {
              store.delete(saved);
            } catch (Exception cleanup) {
              e.addSuppressed(cleanup);
            }
            throw e;
          }
          redirect(r, s, "/app/document?id=" + result + "&submitted=1");
          break;
        case "/verify":
          Part file = r.getPart("comparison");
          Problem.require(
              file != null && file.getSize() > 0 && file.getSize() <= UploadStore.LIMIT,
              400,
              "Choose a file up to 10 MB.");
          String match;
          try (InputStream in = file.getInputStream()) {
            match = ledger().verify(u, id(r, "versionId"), in);
          }
          r.getSession()
              .setAttribute("verification", Map.of("id", id(r, "versionId"), "result", match));
          redirect(r, s, "/app/document?id=" + id(r, "versionId"));
          break;
        case "/admin/assign":
          ledger().assign(u, id(r, "versionId"), id(r, "reviewerId"));
          redirect(r, s, "/app/document?id=" + id(r, "versionId"));
          break;
        case "/review/vote":
          ledger().vote(u, id(r, "versionId"), param(r, "decision"), param(r, "comments"));
          redirect(r, s, "/app/document?id=" + id(r, "versionId"));
          break;
        case "/admin/user":
          ledger()
              .changeUser(u, id(r, "userId"), param(r, "role"), "true".equals(param(r, "active")));
          redirect(r, s, "/app/admin/users");
          break;
        default:
          throw new Problem(404, "Action not found.");
      }
    } catch (Problem e) {
      throw e;
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  private void view(HttpServletRequest r, HttpServletResponse s, String name)
      throws ServletException, IOException {
    r.getRequestDispatcher("/WEB-INF/views/" + name + ".jsp").forward(r, s);
  }

  private void redirect(HttpServletRequest r, HttpServletResponse s, String to) throws IOException {
    s.sendRedirect(r.getContextPath() + to);
  }

  private String param(HttpServletRequest r, String name) {
    String v = r.getParameter(name);
    return v == null ? "" : v;
  }

  private long optionalId(HttpServletRequest r, String name) {
    return param(r, name).isBlank() ? 0 : id(r, name);
  }

  private long id(HttpServletRequest r, String name) {
    try {
      long n = Long.parseLong(param(r, name));
      Problem.require(n > 0, 400, "Invalid record ID.");
      return n;
    } catch (NumberFormatException e) {
      throw new Problem(400, "Invalid record ID.");
    }
  }
}
