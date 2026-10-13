package com.patentpulse;

import java.net.*;
import java.nio.file.*;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.*;
import org.apache.tomcat.util.scan.StandardJarScanner;

/** Local-only teaching launcher. Not included in the production WAR. */
public final class DemoServer {
  public static Tomcat start(int port, String database, Path data) throws Exception {
    Files.createDirectories(data);
    System.setProperty("DB_URL", database);
    System.setProperty("DB_USER", "sa");
    System.setProperty("DB_PASSWORD", "");
    System.setProperty("INIT_SCHEMA", "true");
    System.setProperty("DEMO_MODE", "true");
    System.setProperty("UPLOAD_DIR", data.resolve("uploads").toString());
    Tomcat tomcat = new Tomcat();
    tomcat.setBaseDir(data.resolve("tomcat").toString());
    tomcat.setPort(port);
    tomcat.getConnector().setProperty("address", "127.0.0.1");
    Context context =
        tomcat.addWebapp("/patentpulse", Path.of("src/main/webapp").toAbsolutePath().toString());
    context.setParentClassLoader(DemoServer.class.getClassLoader());
    StandardRoot resources = new StandardRoot(context);
    resources.addPreResources(
        new DirResourceSet(
            resources,
            "/WEB-INF/classes",
            Path.of("target/classes").toAbsolutePath().toString(),
            "/"));
    String jstl =
        Path.of(
                org.apache.taglibs.standard.tag.rt.core.OutTag.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI())
            .toString();
    resources.addPreResources(new FileResourceSet(resources, "/WEB-INF/lib/jstl.jar", jstl, "/"));
    context.setResources(resources);
    ((StandardJarScanner) context.getJarScanner()).setScanManifest(false);
    tomcat.start();
    return tomcat;
  }

  public static void main(String[] args) throws Exception {
    Path data = Path.of("demo-data").toAbsolutePath();
    String database =
        "jdbc:h2:file:"
            + data.resolve("ledger").toString().replace('\\', '/')
            + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_ON_EXIT=FALSE";
    int port = Integer.parseInt(System.getProperty("demo.port", "8080"));
    Tomcat tomcat = start(port, database, data);
    Runtime.getRuntime()
        .addShutdownHook(
            new Thread(
                () -> {
                  try {
                    tomcat.stop();
                    tomcat.destroy();
                  } catch (Exception ignored) {
                  }
                }));
    System.out.println(
        "\nPatentPulse demo: http://localhost:"
            + port
            + "/patentpulse\nDemo password: PatentPulse!2026\n");
    tomcat.getServer().await();
  }
}
