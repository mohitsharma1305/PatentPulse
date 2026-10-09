package com.patentpulse.config;

import com.patentpulse.dao.*;
import com.patentpulse.service.*;
import com.zaxxer.hikari.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import javax.servlet.*;
import org.mindrot.jbcrypt.BCrypt;

public class AppContext implements ServletContextListener {
  private HikariDataSource pool;

  public void contextInitialized(ServletContextEvent event) {
    try {
      HikariConfig cfg = new HikariConfig();
      cfg.setJdbcUrl(
          Config.get(
              "DB_URL",
              "jdbc:mysql://localhost:3306/patentpulse?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"));
      cfg.setUsername(Config.get("DB_USER", "patentpulse"));
      cfg.setPassword(Config.get("DB_PASSWORD", ""));
      cfg.setMaximumPoolSize(8);
      cfg.setConnectionTimeout(10000);
      cfg.setConnectionInitSql("SET TIME ZONE 'UTC'");
      if (cfg.getJdbcUrl().startsWith("jdbc:mysql:"))
        cfg.setConnectionInitSql("SET time_zone = '+00:00'");
      pool = new HikariDataSource(cfg);
      Database db = new Database(pool);
      if (Boolean.parseBoolean(Config.get("INIT_SCHEMA", "false"))) {
        String sql;
        try (var in = AppContext.class.getResourceAsStream("/schema.sql")) {
          sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        db.tx(
            c -> {
              for (String statement : sql.split(";"))
                if (!statement.isBlank()) Database.update(c, statement);
              return null;
            });
      }
      String email = Config.get("BOOTSTRAP_ADMIN_EMAIL", "");
      String password = Config.get("BOOTSTRAP_ADMIN_PASSWORD", "");
      if (!email.isBlank() && !password.isBlank()) {
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
          throw new IllegalArgumentException("Bootstrap password must be 12–72 UTF-8 bytes.");
        db.tx(
            c -> {
              if (Database.query(c, "SELECT id FROM users WHERE role='ADMIN'").isEmpty())
                Database.insert(
                    c,
                    "INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,'ADMIN')",
                    "Administrator",
                    email.toLowerCase(java.util.Locale.ROOT),
                    BCrypt.hashpw(password, BCrypt.gensalt(12)));
              return null;
            });
      }
      if (Boolean.parseBoolean(Config.get("DEMO_MODE", "false"))) seedDemo(db);
      ServletContext ctx = event.getServletContext();
      ctx.setAttribute("db", db);
      ctx.setAttribute("auth", new AuthService(db));
      ctx.setAttribute("ledger", new LedgerService(db));
      ctx.setAttribute(
          "store",
          new UploadStore(
              Path.of(
                  Config.get(
                      "UPLOAD_DIR", System.getProperty("user.home") + "/patentpulse-uploads"))));
      ctx.setAttribute("demoMode", Boolean.parseBoolean(Config.get("DEMO_MODE", "false")));
    } catch (Exception e) {
      if (pool != null) pool.close();
      throw new IllegalStateException(
          "PatentPulse startup failed. Check database/environment configuration.", e);
    }
  }

  private void seedDemo(Database db) throws Exception {
    String[] roles = {"ADMIN", "RESEARCHER", "REVIEWER", "REVIEWER"};
    String[] names = {"Admin", "Mohit Sharma", "Reviewer One", "Reviewer Two"};
    String[] emails = {
      "admin@demo.local", "researcher@demo.local", "reviewer1@demo.local", "reviewer2@demo.local"
    };
    String hash = BCrypt.hashpw("PatentPulse!2026", BCrypt.gensalt(10));
    db.tx(
        c -> {
          for (int i = 0; i < roles.length; i++)
            if (Database.query(c, "SELECT id FROM users WHERE email=?", emails[i]).isEmpty())
              Database.insert(
                  c,
                  "INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,?)",
                  names[i],
                  emails[i],
                  hash,
                  roles[i]);
          return null;
        });
  }

  public void contextDestroyed(ServletContextEvent e) {
    if (pool != null) pool.close();
  }
}
