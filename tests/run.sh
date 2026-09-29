#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_output="$(mktemp -d)"
trap 'rm -rf "$test_output"' EXIT
java -m jdk.compiler/com.sun.tools.javac.Main -d "$test_output" app/src/main/java/app/androglass/core/TrialLease.java tests/TrialLeaseTest.java
java -cp "$test_output" TrialLeaseTest
