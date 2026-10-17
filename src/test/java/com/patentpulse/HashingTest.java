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
  void utf8InputIsHashedAsUtf8() throws Exception {
    assertEquals(
        "4a99557e4033c3539de2eb65472017cad5f9557f7a0625a09f1c3f6e2ba69c4c", hash("é"));
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
  void nullStreamIsRejected() {
    assertThrows(NullPointerException.class, () -> Hashing.sha256(null));
  }

  @Test
  void hashingDoesNotCloseCallerOwnedStream() throws Exception {
    boolean[] closed = {false};
    InputStream stream =
        new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)) {
          @Override
          public void close() throws IOException {
            closed[0] = true;
            super.close();
          }
        };

    Hashing.sha256(stream);

    assertFalse(closed[0]);
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

  @Test
  void streamReadBoundariesAcrossBufferDoNotChangeDigest() throws Exception {
    String value = "a".repeat(20_000);
    byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
    InputStream chunked =
        new ByteArrayInputStream(bytes) {
          public synchronized int read(byte[] b, int off, int len) {
            return super.read(b, off, Math.min(1024, len));
          }
        };

    assertEquals(hash(value), Hashing.sha256(chunked));
  }

  @Test
  void zeroLengthBulkReadFallsBackToSingleByteRead() throws Exception {
    int[] bulkReads = {0};
    int[] singleByteReads = {0};
    InputStream stream =
        new InputStream() {
          @Override
          public int read(byte[] bytes, int offset, int length) {
            return ++bulkReads[0] == 1 ? 0 : -1;
          }

          @Override
          public int read() {
            singleByteReads[0]++;
            return -1;
          }
        };

    assertEquals(hash(""), Hashing.sha256(stream));
    assertEquals(1, singleByteReads[0]);
  }
}
