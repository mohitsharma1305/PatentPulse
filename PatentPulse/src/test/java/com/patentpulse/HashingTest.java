package com.patentpulse;

import static org.junit.jupiter.api.Assertions.*;

import com.patentpulse.util.Hashing;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class HashingTest {
  private String hash(String value) throws Exception {
    return Hashing.sha256(new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void knownSha256Vector() throws Exception {
    assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash("abc"));
  }

  @Test
  void changingOneByteChangesFingerprint() throws Exception {
    assertNotEquals(hash("research"), hash("Research"));
  }

  @Test
  void emptyStreamHasStandardDigest() throws Exception {
    assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash(""));
  }

  @Test
  void streamReadBoundariesDoNotChangeDigest() throws Exception {
    byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);
    InputStream slow =
        new ByteArrayInputStream(bytes) {
          public synchronized int read(byte[] b, int off, int len) {
            return super.read(b, off, Math.min(1, len));
          }
        };
    assertEquals(hash("abc"), Hashing.sha256(slow));
  }
}
