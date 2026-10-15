package com.patentpulse.util;

import java.io.*;
import java.security.*;
import java.util.HexFormat;
import java.util.Objects;

public final class Hashing {
  private Hashing() {}

  public static String sha256(InputStream in) throws IOException {
    Objects.requireNonNull(in, "in");
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] b = new byte[8192];
      int n;
      while ((n = in.read(b)) != -1) {
        if (n == 0) {
          int value = in.read();
          if (value == -1) break;
          digest.update((byte) value);
        } else {
          digest.update(b, 0, n);
        }
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
