package com.patentpulse.model;

import java.io.Serializable;

public class User implements Serializable {
  private final long id;
  private final String name, email, role;

  public User(long id, String name, String email, String role) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.role = role;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getRole() {
    return role;
  }

  public boolean isAdmin() {
    return "ADMIN".equals(role);
  }

  public boolean isReviewer() {
    return "REVIEWER".equals(role);
  }
}
