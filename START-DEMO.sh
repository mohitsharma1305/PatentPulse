#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")"
command -v java >/dev/null || { echo "Install JDK 17 first."; exit 1; }
command -v mvn >/dev/null || { echo "Install Maven 3.9+ first."; exit 1; }
exec mvn test-compile exec:java -Dexec.mainClass=com.patentpulse.DemoServer -Dexec.classpathScope=test
