# Repository maintenance directions

This directory is the maintenance handover for Health Monitor. Read these documents before modifying application code:

1. [Build and safe-upgrade baseline](BUILD_AND_UPGRADE_BASELINE.md) — build steps, pinned baseline, and upgrade sequence.
2. [Known issues and resolution log](KNOWN_ISSUES.md) — findings, changes, verification status, and unresolved risks.

## Working rules

- Preserve existing working behavior and user data. Make the smallest necessary source change.
- Do not rewrite a working subsystem merely to improve style.
- Never reset, migrate destructively, delete, or recreate the user's SQLCipher database as a recovery shortcut.
- Never log passwords, passphrases, biometric material, encryption keys, health-record contents, or transfer payloads.
- Treat database schema, SQLCipher, Keystore, authentication, native ABI, and transfer-protocol changes as high-risk.
- Update the issue register when a problem is found, attempted, fixed, or remains unresolved.
- Update the baseline when a toolchain, SDK, ABI, dependency, or release requirement changes.
- Update the root README after meaningful upgrades so its claims match the actual source and CI behavior.
- Do not describe a CI build as a runtime test. Record physical-device verification separately.

## CI policy

The only GitHub Actions workflow is .github/workflows/test.yml. It builds the debug APK and debug AAB. There are intentionally no automated unit or instrumentation test suites. Build success confirms compilation and packaging only; it does not establish runtime correctness, security, migration safety, or clinical validity.
