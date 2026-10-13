package com.patentpulse.service;

import com.patentpulse.dao.Database;
import com.patentpulse.model.User;
import com.patentpulse.util.Problem;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
  private final Database db;
  private static final String DUMMY = BCrypt.hashpw("invalid-password", BCrypt.gensalt(10));

  public AuthService(Database db) {
    this.db = db;
  }

  public User current(long id) throws Exception {
    return user(Database.one(db.query("SELECT * FROM users WHERE id=? AND active=TRUE", id)));
  }

  public User login(String email, String password) throws Exception {
    var row =
        Database.one(
            db.query("SELECT * FROM users WHERE email=?", email.toLowerCase(Locale.ROOT).trim()));
    boolean valid =
        password.getBytes(StandardCharsets.UTF_8).length <= 72
            && BCrypt.checkpw(password, row == null ? DUMMY : (String) row.get("password_hash"));
    Problem.require(
        row != null && valid && Boolean.TRUE.equals(row.get("active")),
        401,
        "Invalid email or password.");
    return user(row);
  }

  public long register(String name, String email, String password) throws Exception {
    Problem.require(
        name != null && name.trim().length() >= 2 && name.length() <= 100,
        400,
        "Name must contain 2–100 characters.");
    Problem.require(
        email != null && email.length() <= 190 && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"),
        400,
        "Enter a valid email address.");
    Problem.require(
        password.length() >= 10 && password.getBytes(StandardCharsets.UTF_8).length <= 72,
        400,
        "Password must have at least 10 characters and at most 72 UTF-8 bytes.");
    String hash = BCrypt.hashpw(password, BCrypt.gensalt(12));
    try {
      return db.tx(
          c -> {
            long id =
                Database.insert(
                    c,
                    "INSERT INTO users(name,email,password_hash,role) VALUES(?,?,?,'RESEARCHER')",
                    name.trim(),
                    email.trim().toLowerCase(Locale.ROOT),
                    hash);
            Database.audit(c, id, "REGISTER", "USER", id, "Researcher account created");
            return id;
          });
    } catch (java.sql.SQLException e) {
      if ("23000".equals(e.getSQLState()) || "23505".equals(e.getSQLState()))
        throw new Problem(409, "An account with this email already exists.");
      throw e;
    }
  }

  private User user(Map<String, Object> r) {
    return r == null
        ? null
        : new User(
            Database.id(r, "id"),
            (String) r.get("name"),
            (String) r.get("email"),
            (String) r.get("role"));
  }
}
