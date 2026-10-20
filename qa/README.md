# QA tools

`check_publication.py` checks the Git index for forbidden assets and credential-like markers. Run it after staging the final source. It is a conservative guard, not a complete secret scanner or license audit.

`homebrew/build_qa_rom.py` generates an original lawful NES control-test cartridge. See its README for exact filename and usage. Generated games are local test outputs, excluded from publication.

The fold and touch geometry JUnit tests live under `lemuroid-app/src/test`. Run them with `./gradlew :lemuroid-app:testFreeDynamicDebugUnitTest`.

When an Android SDK is unavailable, `run_host_jvm_tests.py` compiles an explicit subset of the actual production Kotlin files and existing JUnit classes with Kotlin 2.0.21 and JDK 17+. It uses no Android/Compose stubs. Requires `curl`; dependencies come from a read-only Gradle Maven cache or Maven Central. Provide a new output directory outside the repository:

```sh
python3 qa/run_host_jvm_tests.py --output /tmp/emuui-host-jvm-results
```

Use `--java /path/to/jdk/bin/java` or `--gradle-cache /path/to/gradle/caches/modules-2/files-2.1` when needed. The output includes compilation and JUnit logs, the selected and excluded files, source/dependency SHA256 values and result counts. This deliberately excludes tests whose source dependencies require Android or Compose, including cover-drawable and window-cutout adapter tests; it does not replace the app Gradle unit-test task or Android instrumentation.

The actual result matrix is in `docs/QA.md`. Do not infer passed Android gameplay or hardware behavior from a geometry or cartridge-only test.
