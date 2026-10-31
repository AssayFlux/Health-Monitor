# Findings, fixes and unresolved issues

Last reviewed: 2026-10-31

This register is maintained with repository changes. Status describes source-level work; it is not a claim of physical-device verification.

## Resolved in this maintenance branch — pending build/device verification

| ID | Finding | Change made | Remaining evidence |
|---|---|---|---|
| HM-SEC-01 | SQLCipher helper retained the same password byte-array reference that the caller wiped after initialization. A later database reopen could use zeroed key bytes. | HealthDb now passes an owned copy to SQLCipher and clears that owned buffer only after SQLiteOpenHelper.close(). The caller can clear its original buffer after initialization. | Build verification and physical-device close/reopen, process-restart and password/biometric unlock checks. |
| HM-SEC-02 | QR transfer sent health-record JSON as readable plain text over local HTTP. | Sender now encrypts payloads with AES-256-GCM using a fresh 256-bit secret carried in the scanned QR. Receiver authenticates/decrypts before validating or saving. Old unencrypted payloads are rejected. | Build verification and two-device transfer test. Verify wrong-secret/tampered-payload rejection and QR secrecy. |
| HM-APP-01 | Initial dashboard queries and counts ran during Compose rendering without an exception boundary. | Dashboard list/count reads now use guarded reads and show a database-read warning rather than propagating those initial read exceptions. | Build verification. Other screen-specific database calls and event handlers still need broader review; mitigation is partial outside the dashboard. |

## Open — do not mark resolved without evidence

| ID | Priority | Finding / risk | Current state and next safe action |
|---|---|---|---|
| HM-SEC-03 | High | Local transfer still uses HTTP. AES-GCM protects payload confidentiality and integrity, but IP/port, timing, request metadata and bearer token header remain visible to a network observer. The manifest still permits cleartext traffic for compatibility with the local HTTP client. | OPEN. Replace HTTP with authenticated TLS or a carefully pinned local transport before claiming transport security. Do not simply disable cleartext traffic: that would break the current HTTP sender. |
| HM-SEC-04 | High | Transfer encryption secret is present in the QR. Anyone who photographs, screenshots, or scans that QR during its five-minute lifetime can obtain the secret. | OPEN. Keep QR display physically controlled and short-lived. A future design should use authenticated key exchange or receiver public-key encryption so the QR does not expose a reusable symmetric secret. |
| HM-APP-02 | High | Not every database operation throughout every screen is protected by a consistent repository-level error boundary. | OPEN. Move reads/writes behind a repository/use-case layer, execute blocking I/O off the main thread, and present recoverable errors consistently. Preserve data and avoid automatic database recreation. |
| HM-DB-01 | High | Wrong SQLCipher key, incompatible file, or damaged database can still fail on the first page read. | OPEN. Add a non-destructive integrity/recovery flow and distinguish wrong key, native-load failure, migration failure and possible corruption. Never delete the original database automatically. |
| HM-SEC-05 | Medium | Biometric key invalidation, biometric enrollment changes and password fallback need physical-device verification. | OPEN. Verify on supported Android versions and document recovery behavior before changing key aliases or envelopes. |
| HM-INTEROP-01 | Medium | FHIR mapping is a limited exchange layer and is not equivalent to full profile validation. | OPEN. Validate exported/imported resources against FHIR R4 and any declared implementation guide before describing exchange as fully conformant. |
| HM-TRANSFER-01 | Medium | Transfer input validation, duplicate handling, imported ID relationships and large payload behavior need real-device coverage. | OPEN. Exercise malformed, duplicate, tampered, oversized and interrupted transfers on two devices. Confirm existing member profiles are not modified. |
| HM-CI-01 | Medium | Automated unit and instrumentation tests were removed by project direction. The single workflow checks only whether the debug APK and AAB build. | KNOWN LIMITATION. The workflow is not a test suite and does not validate runtime behavior, security, migration correctness or clinical calculations. |
| HM-RELEASE-01 | Medium | CI builds debug artifacts only; no release signing or production release gate is configured. | OPEN. Add a protected release process only when release distribution is planned. Never store signing credentials in source control. |

## Historical crash findings

### SQLCipher native method not found

A prior device report showed UnsatisfiedLinkError for SQLiteConnection.nativeOpen(). Application startup now explicitly loads the SQLCipher native library and retains an initialization error for safe reporting. A prior main-branch CI run had a successful emulator instrumentation result, but that does not replace verification on the affected Motorola device.

### “File is not a database” on first schema read

This can mean a wrong key, a different cipher configuration, a non-SQLCipher file, or file damage. It does not by itself prove physical corruption. The key-array ownership defect above is a concrete code issue that could cause later opens to use zeroed bytes; it is not proof that it was the only cause of the reported incident.

**Data safety:** do not uninstall the app, clear app storage, delete the database, or generate a replacement database as a troubleshooting shortcut. Preserve the original app data for diagnosis.

## Update log

- 2026-10-31 — Removed the previous Python regression and Android instrumentation suites as requested; replaced the multi-stage workflow with one APK/AAB build workflow.
- 2026-10-31 — Added owned SQLCipher key-buffer lifecycle in HealthDb.
- 2026-10-31 — Added AES-256-GCM encryption for QR transfer payloads; unencrypted transfer payloads are rejected.
- 2026-10-31 — Guarded initial dashboard list and count reads; full app-wide database error handling remains open.
- 2026-10-31 — Updated this register and the root README to distinguish source changes from verified behavior.
