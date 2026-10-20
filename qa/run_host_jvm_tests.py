#!/usr/bin/env python3
"""Run an explicit Android-independent subset of the real Kotlin/JUnit sources.

Requires JDK 17+ and curl. No Android/Compose classes or replacement stubs are used.
Dependencies are reused read-only from Gradle's Maven cache when available, otherwise
downloaded from Maven Central into the output directory. This is not the Android
Gradle unit-test task, instrumentation, native-core testing, or release certification.
"""

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import zipfile


APP = "lemuroid-app/src/"
PACKAGE = "java/com/swordfish/lemuroid/app/"
SOURCES = [
    "mobile/feature/emuui/FoldGeometry.kt",
    "mobile/feature/emuui/CornerOverlayGeometry.kt",
    "mobile/feature/home/ArtworkAccent.kt",
    "mobile/feature/home/ConsoleLibrary.kt",
    "mobile/feature/home/LauncherNavigation.kt",
    "mobile/feature/home/SavedGameplayPreviewResolver.kt",
    "mobile/shared/compose/ui/ThemeMode.kt",
    "shared/billing/SaveArchive.kt",
    "shared/covers/SecureThumbnailInterceptor.kt",
    "shared/game/GameLaunchRecovery.kt",
    "shared/game/GameRecreationCheckpoint.kt",
    "shared/library/CoreUpdateOutcome.kt",
    "shared/startup/BoundedSubmission.kt",
    "shared/startup/SerialSubmissionQueue.kt",
]
TESTS = [
    "mobile/feature/emuui/FoldGeometryTest.kt",
    "mobile/feature/emuui/CornerOverlayGeometryTest.kt",
    "mobile/feature/emuui/ConsolePolishGeometryTest.kt",
    "mobile/feature/home/ArtworkAccentTest.kt",
    "mobile/feature/home/ConsoleLibraryTest.kt",
    "mobile/feature/home/LauncherNavigationTest.kt",
    "mobile/feature/home/LauncherPresentationLayoutTest.kt",
    "mobile/feature/home/LauncherSectionNavigationTest.kt",
    "mobile/feature/home/SavedGameplayPreviewResolverTest.kt",
    "mobile/shared/compose/ui/ThemeModeTest.kt",
    "shared/billing/SaveArchiveTest.kt",
    "shared/covers/SecureThumbnailInterceptorTest.kt",
    "shared/game/GameLaunchRecoveryTest.kt",
    "shared/game/GameRecreationCheckpointTest.kt",
    "shared/library/CoreDownloadsTest.kt",
    "shared/library/CoreUpdateAttemptTest.kt",
    "shared/startup/BoundedSubmissionTest.kt",
    "shared/startup/SerialSubmissionQueueTest.kt",
    "shared/saves/SaveRetryTest.kt",
]
DEPENDENCIES = [
    ("org.jetbrains.kotlin", "kotlin-compiler-embeddable", "2.0.21"),
    ("org.jetbrains.kotlin", "kotlin-stdlib", "2.0.21"),
    ("org.jetbrains.kotlin", "kotlin-script-runtime", "2.0.21"),
    ("org.jetbrains.kotlin", "kotlin-reflect", "1.6.10"),
    ("org.jetbrains.kotlin", "kotlin-daemon-embeddable", "2.0.21"),
    ("org.jetbrains.intellij.deps", "trove4j", "1.0.20200330"),
    ("org.jetbrains", "annotations", "13.0"),
    ("org.jetbrains.kotlinx", "kotlinx-coroutines-core-jvm", "1.6.4"),
    ("junit", "junit", "4.13.2"),
    ("org.hamcrest", "hamcrest-core", "1.3"),
    ("com.squareup.okhttp3", "okhttp", "4.9.1"),
    ("com.squareup.okio", "okio", "2.10.0"),
    ("com.squareup.retrofit2", "retrofit", "2.9.0"),
]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def dependency(group, artifact, version, cache, output):
    name = f"{artifact}-{version}.jar"
    candidates = sorted((cache / group / artifact / version).glob(f"*/{name}"))
    if candidates:
        path = candidates[0]
    else:
        path = output / "dependencies" / name
        path.parent.mkdir(exist_ok=True)
        url = f"https://repo.maven.apache.org/maven2/{group.replace('.', '/')}/{artifact}/{version}/{name}"
        subprocess.run(["curl", "--fail", "--location", "--silent", "--show-error", "--max-time", "180", url, "--output", str(path)], check=True)
    with zipfile.ZipFile(path) as jar:
        if jar.testzip() is not None:
            raise ValueError(f"Corrupt dependency: {path}")
    return path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", required=True, type=Path, help="New evidence directory outside repository")
    parser.add_argument("--java", default="java", help="JDK 17+ java executable")
    parser.add_argument("--gradle-cache", type=Path, default=Path(os.environ.get("GRADLE_USER_HOME", Path.home() / ".gradle")) / "caches/modules-2/files-2.1")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    output = args.output.resolve()
    if output == root or root in output.parents:
        parser.error("Output must be outside the repository")
    output.mkdir(parents=True, exist_ok=False)
    started = time.time()
    files = [root / (APP + "main/" + PACKAGE + path) for path in SOURCES]
    files += [root / "retrograde-app-shared/src/main/java/com/swordfish/lemuroid/lib/core/CoreDownloads.kt"]
    files += [root / "retrograde-util/src/main/java/com/swordfish/lemuroid/common/kotlin/Utils.kt"]
    files += [root / (APP + "test/" + PACKAGE + path) for path in TESTS]
    before = {str(path.relative_to(root)): digest(path) for path in files}
    jars = [dependency(*coordinate, args.gradle_cache, output) for coordinate in DEPENDENCIES]
    classes = output / "classes"
    classes.mkdir()
    classpath = os.pathsep.join(map(str, jars))
    compile_command = [args.java, "-Xmx2g", "-cp", classpath, "org.jetbrains.kotlin.cli.jvm.K2JVMCompiler", "-no-stdlib", "-no-reflect", "-jvm-target", "17", "-classpath", classpath, "-d", str(classes), *map(str, files)]
    manifest = {
        "scope": "Selected real Android-independent Kotlin/JUnit sources; no Android or Compose stubs",
        "excluded_test_files": sorted(str(path.relative_to(root)) for path in (root / APP / "test").rglob("*Test.kt") if path not in files),
        "source_sha256": before,
        "dependencies": [{"coordinate": ":".join(coordinate), "path": str(jar), "sha256": digest(jar)} for coordinate, jar in zip(DEPENDENCIES, jars)],
        "compile_command": compile_command,
    }
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2) + "\n")
    compile_result = subprocess.run(compile_command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    (output / "compile.log").write_text(compile_result.stdout)
    print(compile_result.stdout, end="")
    result = {"compile_exit_code": compile_result.returncode, "test_classes": ["com.swordfish.lemuroid.app." + path.removesuffix(".kt").replace("/", ".") for path in TESTS]}
    exit_code = compile_result.returncode
    if exit_code == 0:
        test_command = [args.java, "-cp", str(classes) + os.pathsep + classpath, "org.junit.runner.JUnitCore", *result["test_classes"]]
        result["test_command"] = test_command
        test_result = subprocess.run(test_command, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
        (output / "junit.log").write_text(test_result.stdout)
        print(test_result.stdout, end="")
        result["test_exit_code"] = test_result.returncode
        success = re.search(r"OK \((\d+) tests?\)", test_result.stdout)
        failure = re.search(r"Tests run: (\d+),\s+Failures: (\d+)", test_result.stdout)
        result["tests_run"] = int(success[1]) if success else int(failure[1]) if failure else None
        result["failures"] = 0 if success else int(failure[2]) if failure else None
        exit_code = test_result.returncode
        if success is None and failure is None:
            exit_code = 1
    after = {str(path.relative_to(root)): digest(path) for path in files}
    result["source_unchanged_during_run"] = before == after
    if before != after:
        exit_code = 1
    result["elapsed_seconds"] = round(time.time() - started, 3)
    result["status"] = "PASS" if exit_code == 0 else "FAIL"
    (output / "result.json").write_text(json.dumps(result, indent=2) + "\n")
    print(f"{result['status']}: evidence at {output}")
    return exit_code


if __name__ == "__main__":
    sys.exit(main())
