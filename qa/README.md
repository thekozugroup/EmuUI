# QA tools

`check_publication.py` checks the Git index for forbidden assets and credential-like markers. Run it after staging the final source. It is a conservative guard, not a complete secret scanner or license audit.

`homebrew/build_qa_rom.py` generates an original lawful NES control-test cartridge. See its README for exact filename and usage. Generated games are local test outputs, excluded from publication.

The fold and touch geometry JUnit tests live under `lemuroid-app/src/test`. Run them with `./gradlew :lemuroid-app:testFreeDynamicDebugUnitTest`.

The actual result matrix is in `docs/QA.md`. Do not infer passed Android gameplay or hardware behavior from a geometry or cartridge-only test.
