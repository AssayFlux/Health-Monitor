# Stage 2 — Clinical interoperability implementation notes

This document records the Stage 2 implementation boundary. It does not claim certification or conformance to a national implementation guide.

## Implemented foundations

- A versioned normalized projection (clinical_records, model version 1) with stable logical UUIDs, member association, resource type, terminology identifiers, status, effective dates, numeric/text values, UCUM unit metadata, source system/reference, verification state and record timestamp.
- Idempotent projection of legacy history, laboratory report, medicine and vital rows into the normalized model. Legacy rows are explicitly labelled legacy_unreviewed; the original tables remain operational source-of-truth tables.
- FHIR R4 JSON Bundle export for Patient, Observation, DiagnosticReport, MedicationStatement, Condition, AllergyIntolerance, Procedure, Immunization and Encounter where corresponding local records exist.
- FHIR Bundle import staging for supported resource types. Imported records remain pending and are not promoted until a user chooses a family profile and confirms the individual item.
- Base Bundle/resource mandatory-element checks on import. This is not a substitute for the HL7 validator or a jurisdiction-specific implementation guide.
- Terminology system identifiers for LOINC, SNOMED CT, UCUM, ICD-10 and ICD-10-CM; UCUM unit normalization for a limited common-unit set.
- Health Connect read integration for heart rate, steps and weight over a recent 30-day window, subject to runtime permission. Source references and device-import verification state are retained.
- On-device ML Kit OCR for selected report images. OCR text and document metadata are stored in a review-needed state; OCR output is never automatically turned into a clinical record.

## Important interoperability limits

1. FHIR profile validation: the app emits/accepts base R4-shaped resources. No national or regional implementation guide has been selected, so profile validation, terminology binding validation and server round-trip testing remain outstanding.
2. Terminology licensing: no SNOMED CT, LOINC or ICD terminology distribution is bundled. Identifiers supplied by a source are preserved; the app must not infer equivalence. Distribution and licensing must be reviewed before shipping terminology content.
3. Mapping coverage: existing local data is not yet fully normalized to analyte-specific Observation resources. The legacy projection preserves provenance and descriptive context; it does not fabricate missing values or codes.
4. FHIR scope: export is a collection Bundle, not a transactional server submission. Import supports selected resource types and requires explicit member association. Unsupported resources are skipped rather than guessed.
5. Health Connect: availability, data types and user permission vary by Android device and provider. This adapter is read-only and currently maps heart rate, step count and weight; it does not claim exhaustive Health Connect coverage.
6. Document storage: the current OCR path is image-based. The app stores document URI metadata and extracted text, not an immutable archival copy of the original document. Users must retain the original source.
7. Clinical review: OCR, imported FHIR resources and device observations are provenance-labelled. They must be checked against source documents and must not be treated as a diagnosis or prescription.

## Before Stage 2 can be called release-ready

- Select target jurisdictions and implementation guides; validate export/import against the relevant published profiles and terminology bindings.
- Complete structured mapping of every supported local vital, laboratory analyte, medication and encounter field, including UCUM conversions and source-unit preservation.
- Add explicit FHIR export/import conflict resolution, duplicate detection and update semantics.
- Add PDF document OCR, durable content retention policy and attachment lifecycle controls.
- Exercise Health Connect availability, permission revocation, pagination and device-specific record coverage.
- Complete automated tests and real-device verification after implementation is frozen.
