package com.patentpulse.config;

public final class Config {
  private Config() {}

  public static String get(String key, String fallback) {
    String value = System.getProperty(key);
    if (value == null) value = System.getenv(key);
    return value == null || value.isBlank() ? fallback : value;
  }
}
