package com.patentpulse.util;

import java.io.*;
import java.security.*;
import java.util.HexFormat;

public final class Hashing {
  private Hashing() {}

  public static String sha256(InputStream in) throws IOException {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] b = new byte[8192];
      int n;
      while ((n = in.read(b)) != -1) digest.update(b, 0, n);
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
