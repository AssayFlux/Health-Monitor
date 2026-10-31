# Three-stage global-readiness delivery plan

This plan is an engineering roadmap, not a claim of certification or clinical validation. Work is delivered in three gated stages. Each stage must build and pass its relevant tests before the next stage is treated as complete.

## Stage 1 — Stabilise and complete the core personal health record

- Remove the current encrypted share/transfer implementation and all user-facing entry points. Backup and sharing are deferred to a separately designed project.
- Preserve the local SQLCipher database, Android Keystore key wrapping, and local account verifier. Removing transfer must not weaken at-rest database encryption.
- Complete family-member profile lifecycle and member-scoped record browsing.
- Add structured health history: conditions, allergies, procedures, immunisations, family history, and encounters.
- Improve medicine schedules, adherence records, and appointment-linked reminders while clearly documenting Android scheduling limits.
- Add per-member and date/category filters to reports and analytics; ensure PDF export can be limited to selected records.
- Add schema migration, validation, and regression coverage for new record types.
- Acceptance: clean build, unit tests, migration tests, no broken references to the removed transfer module, and manual review of record ownership/filtering.

## Stage 2 — Interoperable clinical data

- Introduce a versioned, normalized clinical data model with stable identifiers, provenance, status, effective dates, and source units.
- Map supported concepts to HL7 FHIR R4 resources (including Patient, Observation, DiagnosticReport, MedicationStatement/MedicationRequest, Condition, AllergyIntolerance, Procedure, Immunization, and Encounter as appropriate).
- Add terminology adapters for LOINC, SNOMED CT, UCUM and jurisdiction-appropriate ICD mappings. Confirm licensing and distribution terms before shipping terminology content.
- Add validated import/export against a selected implementation guide. FHIR compatibility alone is not proof of interoperability; validate against the target jurisdiction's profiles.
- Add document attachment metadata and OCR-assisted extraction. Never silently convert OCR output into a confirmed clinical record.
- Integrate Android Health Connect where data types and device support permit; keep user consent and provenance visible.
- Acceptance: schema/profile validation, terminology/unit tests, malformed-input tests, and traceable source provenance.

## Stage 3 — Security, accessibility and release readiness

- Integrate BiometricPrompt with the actual SQLCipher key-release flow, retaining a tested password fallback and safe key invalidation handling.
- Design backup, recovery, and sharing from first principles as a separate feature with explicit scope, threat model, preview, conflict handling, and transactional restore. Do not reintroduce the removed transfer code as-is.
- Add consent records, access/audit history, and clear user-facing privacy controls.
- Review against OWASP MASVS and applicable privacy obligations in each launch jurisdiction; obtain independent security and legal review where required.
- Improve offline behaviour, accessibility, localisation, date/time-zone handling, and large-text support.
- Expand migration, device, permission, notification, security, and release CI coverage.
- Acceptance: evidence-backed release checklist, physical-device test matrix, accessibility review, security review, and jurisdiction-specific compliance assessment.

## Scope boundaries

- The application remains a personal health record and informational tool unless clinical validation, regulatory assessment, and operational controls justify a different claim.
- No diagnosis, treatment recommendation, autonomous medication change, or unvalidated risk score is included.
- HIPAA, GDPR, India's DPDP framework, and other laws have different scopes and triggers. No feature checklist automatically makes the app compliant.


## Stage 2 implementation status (2026-10-31)

The Stage 2 code foundation is now present in the application: a versioned normalized clinical projection, legacy-row projection with provenance labels, FHIR R4 collection Bundle export, staged FHIR import with base mandatory-element checks and explicit member confirmation, LOINC/SNOMED CT/UCUM/ICD system identifiers and common UCUM unit normalization, Health Connect read integration for heart rate/steps/weight, and image OCR with a manual-review state. See [Stage 2 interoperability notes](STAGE_2_INTEROPERABILITY.md) for boundaries.

Stage 2 is **implemented but not release-verified**. A jurisdiction-specific implementation guide has not been selected, no terminology distribution is bundled, not all local analytes are mapped to coded numeric Observations, document OCR currently accepts images rather than PDFs, and no build/tests/device validation have been run at the user's request. Do not describe this as certified FHIR conformance or completed clinical interoperability.


## Stage 3 implementation status (2026-10-31)

Stage 3 implementation foundations have been committed: password/strong-biometric authentication now precedes SQLCipher opening; biometric CryptoObject key wrapping and password fallback are wired; foreground loss clears the in-memory key and closes the authenticated database host; consent controls and encrypted application audit records are present; backup/device transfer remain disabled; screenshot/notification safeguards and persistent appearance selection remain in place. See [Stage 3 security, accessibility and release readiness](STAGE_3_SECURITY_ACCESSIBILITY_RELEASE.md).

Stage 3 is **implemented but not release-verified**. Full TalkBack/large-text/keyboard/localisation review, device testing of lifecycle and biometric invalidation, complete audit coverage, MASVS assessment, independent security review and jurisdiction-specific legal/privacy assessment remain outstanding. No certification or compliance claim is made.
