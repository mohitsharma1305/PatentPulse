package com.patentpulse.dao;

import java.sql.*;
import java.util.*;
import javax.sql.DataSource;

public class Database {
  private final DataSource pool;

  public Database(DataSource pool) {
    this.pool = pool;
  }

  @FunctionalInterface
  public interface Work<T> {
    T run(Connection c) throws Exception;
  }

  public <T> T tx(Work<T> work) throws Exception {
    try (Connection c = pool.getConnection()) {
      c.setAutoCommit(false);
      try {
        T result = work.run(c);
        c.commit();
        return result;
      } catch (Exception e) {
        c.rollback();
        throw e;
      }
    }
  }

  public List<Map<String, Object>> query(String sql, Object... args) throws SQLException {
    try (Connection c = pool.getConnection()) {
      return query(c, sql, args);
    }
  }

  public static List<Map<String, Object>> query(Connection c, String sql, Object... args)
      throws SQLException {
    try (PreparedStatement p = c.prepareStatement(sql)) {
      bind(p, args);
      try (ResultSet r = p.executeQuery()) {
        List<Map<String, Object>> rows = new ArrayList<>();
        ResultSetMetaData m = r.getMetaData();
        while (r.next()) {
          Map<String, Object> row = new LinkedHashMap<>();
          for (int i = 1; i <= m.getColumnCount(); i++) {
            Object value = r.getObject(i);
            if (value instanceof Timestamp) {
              Timestamp stamp =
                  r.getTimestamp(i, Calendar.getInstance(TimeZone.getTimeZone("UTC")));
              value = stamp.toInstant().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
            }
            row.put(m.getColumnLabel(i).toLowerCase(Locale.ROOT), value);
          }
          rows.add(row);
        }
        return rows;
      }
    }
  }

  public static int update(Connection c, String sql, Object... args) throws SQLException {
    try (PreparedStatement p = c.prepareStatement(sql)) {
      bind(p, args);
      return p.executeUpdate();
    }
  }

  public static long insert(Connection c, String sql, Object... args) throws SQLException {
    try (PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bind(p, args);
      p.executeUpdate();
      try (ResultSet r = p.getGeneratedKeys()) {
        if (!r.next()) throw new SQLException("Missing generated key");
        return r.getLong(1);
      }
    }
  }

  private static void bind(PreparedStatement p, Object[] args) throws SQLException {
    for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
  }

  public static Map<String, Object> one(List<Map<String, Object>> rows) {
    return rows.isEmpty() ? null : rows.get(0);
  }

  public static long id(Map<String, Object> row, String key) {
    return ((Number) row.get(key)).longValue();
  }

  public static void audit(
      Connection c, long actor, String action, String type, long target, String detail)
      throws SQLException {
    update(
        c,
        "INSERT INTO audit_logs(actor_id,action,target_type,target_id,details) VALUES(?,?,?,?,?)",
        actor,
        action,
        type,
        target,
        detail);
  }
}
