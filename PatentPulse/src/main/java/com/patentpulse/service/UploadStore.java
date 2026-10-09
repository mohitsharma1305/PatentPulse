package com.patentpulse.service;

import com.patentpulse.util.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.servlet.http.Part;

public class UploadStore {
  public static final long LIMIT = 10 * 1024 * 1024;
  private final Path root;

  public record Saved(String original, String stored, String hash, long size) {}

  public UploadStore(Path root) throws IOException {
    this.root = root.toAbsolutePath().normalize();
    Files.createDirectories(this.root);
  }

  public Saved save(Part part) throws IOException {
    Problem.require(
        part != null && part.getSize() > 0 && part.getSize() <= LIMIT,
        400,
        "Choose a nonempty file up to 10 MB.");
    String name = part.getSubmittedFileName();
    Problem.require(name != null, 400, "A file is required.");
    name = name.replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1);
    Problem.require(
        !name.isBlank() && name.length() <= 255 && !name.matches(".*[\\p{Cntrl}].*"),
        400,
        "Invalid filename.");
    String ext =
        name.contains(".")
            ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)
            : "";
    Problem.require(
        Set.of("pdf", "txt", "java", "py", "js", "c", "cpp", "h", "md", "json", "csv")
            .contains(ext),
        400,
        "Allowed: PDF, TXT, MD, JAVA, PY, JS, C, CPP, H, JSON, CSV.");
    String stored = UUID.randomUUID() + ".bin";
    Path file = path(stored);
    try {
      try (InputStream in = part.getInputStream();
          OutputStream out = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
        byte[] b = new byte[8192];
        int n;
        long total = 0;
        while ((n = in.read(b)) != -1) {
          total += n;
          Problem.require(total <= LIMIT, 400, "File exceeds 10 MB.");
          out.write(b, 0, n);
        }
      }
      if (ext.equals("pdf")) {
        try (InputStream in = Files.newInputStream(file)) {
          Problem.require(
              new String(in.readNBytes(5), java.nio.charset.StandardCharsets.US_ASCII)
                  .equals("%PDF-"),
              400,
              "File does not have a PDF header.");
        }
      } else {
        try (InputStream in = Files.newInputStream(file)) {
          byte[] b = new byte[8192];
          int n;
          while ((n = in.read(b)) != -1)
            for (int i = 0; i < n; i++)
              Problem.require(
                  b[i] != 0, 400, "Source and text uploads must not contain binary NUL bytes.");
        }
      }
      try (InputStream in = Files.newInputStream(file)) {
        return new Saved(name, stored, Hashing.sha256(in), Files.size(file));
      }
    } catch (Exception e) {
      Files.deleteIfExists(file);
      throw e;
    }
  }

  public Path path(String name) {
    Path p = root.resolve(name).normalize();
    Problem.require(p.getParent().equals(root), 400, "Invalid stored file path.");
    return p;
  }

  public void delete(Saved file) throws IOException {
    Files.deleteIfExists(path(file.stored()));
  }
}
