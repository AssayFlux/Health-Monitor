# Stage 3 — Security, privacy, accessibility and release readiness

Status: implementation foundations committed; release verification remains outstanding. This document is not a certification, legal opinion, or independent security assessment.

## Implemented in the application

- Authentication now precedes database creation/open: the local password verifier is checked before the SQLCipher passphrase is handed to the database host.
- Strong-biometric unlock uses BiometricPrompt with a CryptoObject backed by an Android Keystore AES-GCM key requiring per-use strong biometric authentication. The encrypted SQLCipher passphrase is unwrapped only from the successful prompt callback.
- Biometric enrollment is available from Privacy settings. Password login remains the fallback. Disabling biometrics removes only the biometric envelope/key and preserves the password-wrapped database key.
- App foreground loss clears the in-memory passphrase and removes the authenticated UI; the database helper is closed when the authenticated host leaves composition.
- Progressive password-attempt throttling remains in place. Key unwrap and SQLCipher open failures fail closed; the app does not silently create a replacement database key.
- Encrypted local consent records cover Health Connect reads, document OCR, FHIR exchange and local analytics. FHIR, OCR and Health Connect entry points are disabled until the corresponding consent is granted.
- Encrypted application audit events record database unlock, consent changes, biometric enrollment/disable, and create/update/delete operations on core clinical tables through SQLite triggers. The UI labels this as an application audit trail, not a tamper-proof forensic log.
- FLAG_SECURE is applied to activities. Android cloud backup and device-to-device extraction remain disabled. Notification content is generic.
- Appearance palette choice is persisted locally.
- Backup/recovery/sharing remain intentionally unavailable. The former transfer implementation is not restored.

## Backup, recovery and sharing: separate future design

Do not implement this by reviving the deleted SecureTransfer module. A future design must begin with a reviewed threat model and explicit user journeys:

1. Define assets: SQLCipher database, attachment files, database passphrase, member identities, clinical documents and audit history.
2. Define adversaries: lost/stolen unlocked device, malicious app with shared-storage access, shoulder-surfing, archive substitution, wrong-recipient disclosure, compromised destination device, and accidental overwrite.
3. Define cryptographic envelope: versioned authenticated archive, per-export random data-encryption key, authenticated encryption, explicit key-wrapping method, KDF parameters and algorithm agility. Never export the raw SQLCipher key.
4. Define preview before restore: archive integrity, creation time, app/schema version, record counts, member list, attachment count and destination mapping. Do not reveal sensitive previews before local authentication.
5. Define restore transaction: restore into a temporary database, validate schema/integrity/references/attachments, resolve duplicate and conflict policy, require explicit confirmation, atomically swap only after success, and retain a recoverable prior state.
6. Define sharing consent: recipient verification, least-privilege member/category/date selection, expiry/revocation limitations, explicit warning that a recipient can copy data, and a final review screen.
7. Define recovery: recovery-key creation and verification, offline storage guidance, lost-key warnings, key rotation, revocation limitations, and recovery drills. No hidden backdoor or server-held master key.
8. Test tampered archives, wrong passwords, truncated files, storage exhaustion, duplicate IDs, schema downgrade, interrupted restore, wrong member mapping and app process death.

## OWASP MASVS review map

- MASVS-STORAGE: SQLCipher at rest, Keystore envelope, backup exclusion, generic notifications and screenshot protection. Still requires device-level verification, memory inspection and backup artifact review.
- MASVS-AUTH: password verifier, progressive throttling, biometric CryptoObject key release and foreground session lock. Still requires invalidation, enrollment-change and lockout testing.
- MASVS-CRYPTO: AES-GCM envelope and Keystore key usage. Still requires independent parameter review, key lifecycle review and platform coverage.
- MASVS-PLATFORM: Android permissions, Health Connect consent, file pickers and cleartext traffic disabled. Still requires intent/URI and exported-component review.
- MASVS-NETWORK: no app backend or cloud sync is currently configured. Any future network feature requires a separate network threat model.
- MASVS-PRIVACY: local consent and audit foundations, no cloud analytics, generic notifications. Still requires data inventory, retention/deletion workflows and privacy review.

## Accessibility and localisation

- Native Compose controls and Material components provide semantic control roles and standard focus behaviour; review every screen with TalkBack, Switch Access, keyboard navigation and large font/display scaling before release.
- Keep touch targets at least 48dp, do not rely on colour alone, provide meaningful labels for icon-only controls, and verify contrast across all four palettes.
- Date/time persistence must retain explicit local zone/offset where an appointment or reminder depends on wall-clock time; verify daylight-saving and time-zone changes on devices.
- UI text remains substantially hard-coded English. Full resource extraction and reviewed translations (including plural/date/number formatting) are still required; this stage does not claim completed localisation.

## Mandatory release evidence still required

- Run build, unit, instrumentation and migration suites on the final commit; the user has asked to defer these until implementation is frozen.
- Verify password authentication gates the database open on a clean install and upgrade; verify wrong password never opens the DB.
- Verify biometric enrollment, unlock, cancellation, no-enrollment fallback, biometric-set changes, key invalidation and password recovery on physical devices.
- Verify foreground loss locks the UI, closes the database, and requires re-authentication. Review biometric prompt lifecycle behaviour.
- Verify backup/device-transfer artifacts contain no database, preferences, documents or passphrase material.
- Verify consent withdrawal disables each feature immediately and that runtime Android permissions are independently enforced.
- Verify audit coverage, clock correctness, retention and sensitive-data minimisation.
- Complete TalkBack, large text, contrast, keyboard, locale, time-zone and offline testing.
- Obtain independent mobile security review and jurisdiction-specific privacy/legal review before any compliance claim.

Do not claim HIPAA, GDPR, DPDP Act, OWASP MASVS certification or FHIR certification based solely on these code changes.