package com.patentpulse.util;

public class Problem extends RuntimeException {
  public final int status;

  public Problem(int status, String message) {
    super(message);
    this.status = status;
  }

  public static void require(boolean condition, int status, String message) {
    if (!condition) throw new Problem(status, message);
  }
}
