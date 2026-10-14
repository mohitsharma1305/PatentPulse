package com.patentpulse;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkflowTest {
  @TempDir static Path temp;
  Tomcat server;
  String base;

  @BeforeAll
  void start() throws Exception {
    server =
        DemoServer.start(
            0, "jdbc:h2:mem:workflow;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", temp);
    base = "http://127.0.0.1:" + server.getConnector().getLocalPort() + "/patentpulse";
  }

  @AfterAll
  void stop() throws Exception {
    if (server != null) {
      server.stop();
      server.destroy();
    }
  }

  class Browser {
    HttpClient client =
        HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    String csrf;

    HttpResponse<String> get(String path) throws Exception {
      return client.send(
          HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
          HttpResponse.BodyHandlers.ofString());
    }

    String token(String html) {
      Matcher m = Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"").matcher(html);
      assertTrue(
          m.find(),
          "Expected CSRF token in rendered JSP: "
              + html.substring(0, Math.min(500, html.length())));
      return m.group(1);
    }

    void login(String email) throws Exception {
      var page = get("/login");
      assertEquals(200, page.statusCode());
      csrf = token(page.body());
      assertEquals(
          302, post("/login", Map.of("email", email, "password", "PatentPulse!2026")).statusCode());
      var home = get("/app/dashboard");
      assertEquals(200, home.statusCode(), home.body());
      assertTrue(home.body().contains("+ New submission"), "JSP source must render as UTF-8");
      csrf = token(home.body());
    }

    HttpResponse<String> post(String path, Map<String, String> values) throws Exception {
      Map<String, String> fields = new LinkedHashMap<>(values);
      fields.putIfAbsent("csrf", csrf);
      String body =
          fields.entrySet().stream()
              .map(e -> enc(e.getKey()) + "=" + enc(e.getValue()))
              .collect(java.util.stream.Collectors.joining("&"));
      return client.send(
          HttpRequest.newBuilder(URI.create(base + path))
              .header("Content-Type", "application/x-www-form-urlencoded")
              .POST(HttpRequest.BodyPublishers.ofString(body))
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> upload(
        String path, Map<String, String> fields, String field, String name, String content)
        throws Exception {
      String boundary = "PatentPulseBoundary" + UUID.randomUUID();
      StringBuilder b = new StringBuilder();
      Map<String, String> f = new LinkedHashMap<>(fields);
      f.put("csrf", csrf);
      f.forEach(
          (k, v) ->
              b.append("--")
                  .append(boundary)
                  .append("\r\nContent-Disposition: form-data; name=\"")
                  .append(k)
                  .append("\"\r\n\r\n")
                  .append(v)
                  .append("\r\n"));
      b.append("--")
          .append(boundary)
          .append("\r\nContent-Disposition: form-data; name=\"")
          .append(field)
          .append("\"; filename=\"")
          .append(name)
          .append("\"\r\nContent-Type: text/plain\r\n\r\n")
          .append(content)
          .append("\r\n--")
          .append(boundary)
          .append("--\r\n");
      return client.send(
          HttpRequest.newBuilder(URI.create(base + path))
              .header("Content-Type", "multipart/form-data; boundary=" + boundary)
              .POST(HttpRequest.BodyPublishers.ofString(b.toString()))
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }
  }

  static String enc(String v) {
    return URLEncoder.encode(v, StandardCharsets.UTF_8);
  }

  static String location(HttpResponse<?> r) {
    assertEquals(302, r.statusCode(), r.body().toString());
    return r.headers().firstValue("location").orElseThrow();
  }

  static String getId(String location) {
    return location.substring(location.indexOf("id=") + 3).split("&")[0];
  }

  Map<String, String> details(String title) {
    return Map.of(
        "title",
        title,
        "field",
        "Cybersecurity",
        "summary",
        "A practical study of document integrity and research provenance.");
  }

  @Test
  void completeResearchReviewAndVerificationWorkflow() throws Exception {
    Browser researcher = new Browser(),
        admin = new Browser(),
        reviewer1 = new Browser(),
        reviewer2 = new Browser();
    researcher.login("researcher@demo.local");
    admin.login("admin@demo.local");
    reviewer1.login("reviewer1@demo.local");
    reviewer2.login("reviewer2@demo.local");
    assertEquals(403, researcher.get("/app/admin/users").statusCode());
    assertEquals(403, researcher.get("/app/review").statusCode());
    assertEquals(403, researcher.post("/draft", Map.of("csrf", "wrong")).statusCode());
    String title = "Integrity <script>alert(1)</script>";
    String draftId = getId(location(researcher.post("/app/draft", details(title))));
    assertEquals(200, researcher.get("/app/new?id=" + draftId).statusCode());
    Map<String, String> fields = new HashMap<>(details(title));
    fields.put("blueprintId", draftId);
    String version =
        getId(
            location(
                researcher.upload(
                    "/app/submit",
                    fields,
                    "document",
                    "research.txt",
                    "Original research content.")));
    var document = researcher.get("/app/document?id=" + version);
    assertEquals(200, document.statusCode(), document.body());
    assertTrue(document.body().contains("&lt;script&gt;"));
    assertFalse(document.body().contains("<script>alert(1)</script>"));
    assertEquals(404, reviewer1.get("/app/document?id=" + version).statusCode());
    assertEquals(404, reviewer1.get("/file?id=" + version).statusCode());
    assertEquals(
        403,
        reviewer1
            .post(
                "/app/review/vote",
                Map.of(
                    "versionId",
                    version,
                    "decision",
                    "APPROVE",
                    "comments",
                    "This is a sufficiently clear research summary."))
            .statusCode());
    var download = researcher.get("/file?id=" + version);
    assertEquals(200, download.statusCode());
    assertEquals("Original research content.", download.body());
    assertEquals(
        "private, no-store", download.headers().firstValue("cache-control").orElseThrow());
    assertEquals("nosniff", download.headers().firstValue("x-content-type-options").orElseThrow());
    assertTrue(
        download
            .headers()
            .firstValue("content-disposition")
            .orElseThrow()
            .contains("attachment;"));
    assertEquals(200, researcher.get("/app/receipt?id=" + version).statusCode());
    location(
        researcher.upload(
            "/app/verify",
            Map.of("versionId", version),
            "comparison",
            "check.txt",
            "Original research content."));
    assertTrue(
        researcher.get("/app/document?id=" + version).body().contains("<strong>MATCH</strong>"));
    location(
        researcher.upload(
            "/app/verify",
            Map.of("versionId", version),
            "comparison",
            "check.txt",
            "Altered research content."));
    assertTrue(
        researcher.get("/app/document?id=" + version).body().contains("<strong>MISMATCH</strong>"));
    location(admin.post("/app/admin/assign", Map.of("versionId", version, "reviewerId", "3")));
    assertEquals(
        409,
        admin
            .post("/app/admin/assign", Map.of("versionId", version, "reviewerId", "3"))
            .statusCode());
    location(admin.post("/app/admin/assign", Map.of("versionId", version, "reviewerId", "4")));
    assertEquals(200, reviewer1.get("/app/review").statusCode());
    assertEquals(200, reviewer1.get("/app/document?id=" + version).statusCode());
    Map<String, String> vote =
        Map.of(
            "versionId",
            version,
            "decision",
            "APPROVE",
            "comments",
            "The approach and integrity checks are clearly described.");
    location(reviewer1.post("/app/review/vote", vote));
    assertEquals(409, reviewer1.post("/app/review/vote", vote).statusCode());
    location(reviewer2.post("/app/review/vote", vote));
    assertTrue(
        researcher.get("/app/document?id=" + version).body().contains("INTERNALLY REVIEWED"));
    assertFalse(researcher.get("/app/document?id=" + version).body().contains("Reviewer One"));
    fields.put("title", "Version two");
    String next =
        getId(
            location(
                researcher.upload(
                    "/app/submit", fields, "document", "revision.txt", "Changed content.")));
    assertNotEquals(version, next);
    assertTrue(
        researcher.get("/app/document?id=" + version).body().contains("Integrity &lt;script&gt;"));
    assertEquals(404, reviewer1.get("/app/document?id=" + next).statusCode());
    location(admin.post("/app/admin/assign", Map.of("versionId", next, "reviewerId", "3")));
    location(
        reviewer1.post(
            "/app/review/vote",
            Map.of(
                "versionId",
                next,
                "decision",
                "REJECT",
                "comments",
                "Please provide more detail about your evaluation method.")));
    assertTrue(researcher.get("/app/document?id=" + next).body().contains("NEEDS REVISION"));
    assertEquals(200, admin.get("/app/admin/users").statusCode());
    assertEquals(200, admin.get("/app/admin/queue").statusCode());
    assertTrue(admin.get("/app/admin/audit").body().contains("INTEGRITY_MISMATCH"));
    assertEquals(409, researcher.post("/app/draft", fields).statusCode());
    location(
        admin.post(
            "/app/admin/user", Map.of("userId", "2", "role", "RESEARCHER", "active", "false")));
    assertEquals(302, researcher.get("/app/dashboard").statusCode());
    location(
        admin.post(
            "/app/admin/user", Map.of("userId", "2", "role", "RESEARCHER", "active", "true")));
  }

  @Test
  void registrationIgnoresRoleEscalationAndInvalidFilesAreRejected() throws Exception {
    Browser b = new Browser();
    b.csrf = b.token(b.get("/register").body());
    location(
        b.post(
            "/register",
            Map.of(
                "name",
                "Test Researcher",
                "email",
                "test@example.edu",
                "password",
                "PatentPulse!2026",
                "role",
                "ADMIN")));
    b.login("test@example.edu");
    assertEquals(403, b.get("/app/admin/users").statusCode());
    assertEquals(
        400,
        b.upload("/app/submit", details("Blocked executable"), "document", "unsafe.exe", "content")
            .statusCode());
    assertEquals(
        400,
        b.upload(
                "/app/submit",
                details("Invalid PDF file"),
                "document",
                "fake.pdf",
                "Not a PDF file")
            .statusCode());
    assertEquals(302, b.post("/logout", Map.of()).statusCode());
    assertEquals(302, b.get("/app/dashboard").statusCode());
  }

  @Test
  void anonymousAccessRedirectsAndCsrfIsRequired() throws Exception {
    Browser b = new Browser();
    assertEquals(302, b.get("/app/dashboard").statusCode());
    b.csrf = b.token(b.get("/login").body());
    assertEquals(
        403,
        b.post(
                "/login",
                Map.of(
                    "csrf", "invalid", "email", "admin@demo.local", "password", "PatentPulse!2026"))
            .statusCode());
  }
}
