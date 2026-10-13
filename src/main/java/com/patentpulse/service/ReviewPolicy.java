package com.patentpulse.service;

public final class ReviewPolicy {
  private ReviewPolicy() {}

  public static String status(long approvals, long rejections) {
    return rejections > 0 ? "NEEDS_REVISION" : approvals >= 2 ? "INTERNALLY_REVIEWED" : "IN_REVIEW";
  }

  public static boolean terminal(String status) {
    return status.equals("INTERNALLY_REVIEWED") || status.equals("NEEDS_REVISION");
  }
}
