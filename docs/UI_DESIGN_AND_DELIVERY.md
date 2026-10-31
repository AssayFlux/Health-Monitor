# Health Monitor — UI design and delivery specification

## Visual system
- Native Android Jetpack Compose; no WebView.
- Vintage visual direction: subtle paper-like warmth, clean rounded cards, quiet borders, generous spacing, readable labels, restrained decoration, and clear empty/error states.
- Four selectable palettes in Settings; Antique Botanical is the default:
  - Antique Botanical: parchment #F5F0E4, cream #E7DDC8, forest #35483A, sage #66745B, antique brass #B18A55.
  - French Apothecary: parchment #F4EBDD, cream #DED0B9, burgundy #783F45, slate blue #778795, antique taupe #A58B68.
  - Heritage Olive & Terracotta: parchment #F3EBDD, cream #DCCBB1, deep brown #493D32, olive #73734D, terracotta #A65F48.
  - Victorian Rose & Parchment: parchment #F6EFE6, cream #E5D7CA, plum #714B50, rose #B98783, antique gold #9A805C.
- All palettes retain dark, high-contrast text and consistent semantic colours across navigation, cards, forms and charts.
- Phone bottom navigation is intentionally limited to five destinations: Home, Records, Assistant, Analytics and More. The Records hub contains People, Vitals, Reports, Medicines, History and Tests. The More hub contains Reminders, Interoperability, Received & Shared, Privacy and Settings. This avoids squeezing 14 destinations into the phone navigation bar.

## Screen map

### 1. Local account
**Create account / Sign in**
- Local user ID and password; no email, phone, OTP, cloud account, or server.
- Password creation requires at least 12 characters.
- Failed-attempt throttling and clear recovery warning: app data loss without a backup can make local records unrecoverable.
- Optional biometric unlock is only shown when native biometric key enrollment and database-key release are fully wired and tested. Password remains the recovery path.

### 2. Home
- Dark forest-green welcome card.
- Summary metrics for family members, vitals, and reports.
- Quick actions: add person, record vitals, laboratory report, medicine.
- Recent measurements list.

### 3. People
- Family member list and profile creation.
- Date of birth, sex, blood group, height and notes.
- A profile selector is used when adding observations.

### 4. Vitals
- Measurement date and time-of-day.
- Separate left/right arm blood pressure fields, pulse, weight, height, oxygen saturation, temperature and notes.
- Recent observations show units and date.

### 5. Reports and medicines
- Structured quantitative, qualitative, graded and report-style laboratory entries.
- Preserve the laboratory's reported units and reference intervals; do not infer missing results.
- Medicine name, dose/schedule, start/end dates and notes.

### 6. Analytics
- Scientific overview with counts and time-series charts.
- Separate series for blood pressure, pulse, weight, SpO₂, temperature, numeric laboratory analytes by unit, and anthropometric measurements / derived BMI.
- Date-grouped descriptive summaries; missing values are not imputed.
- Show measurement units and limitations; no diagnostic claims or unsupported pediatric percentiles.

### 7. Reminders
- Link each reminder to an existing medicine or appointment.
- One-time or recurring schedule, next due time, notification permission status and cancel action.
- Explain that WorkManager recurring intervals are approximate, not exact medication alarms.

### 8. Secure backup and restore — intended workflow
- Export: choose family profiles and record categories, optionally constrain a date range, preview counts, generate an encrypted .hmbackup archive, and show a separate high-entropy import code once.
- Import: select archive, enter import code, authenticate/decrypt, validate manifest and schema, preview incoming counts and duplicate matches, map incoming people to existing profiles or create new profiles, choose skip/keep-both/replace only where safe, then confirm.
- Restore/merge must execute in one SQL transaction with rollback on any failure; preserve foreign-key relationships and never silently overwrite.
- Keep backup files and import codes separate. Never log the code or include it in the archive.

### 9. PDF export
- Choose profiles and sections before creating the report.
- Preview the selected report scope and clearly state that PDF output is not encrypted unless an encrypted PDF feature is explicitly added.

## Security and release acceptance
- SQLCipher key must be released only after the selected authentication path succeeds.
- Biometric enrollment and unlock must use BiometricPrompt CryptoObject and the authenticated cipher to wrap/unwrap the actual SQLCipher passphrase; password fallback must remain available.
- Backup uses authenticated encryption and validates archive paths, manifest, size limits and schema before import.
- Import/merge is transactional, duplicate-aware and relationship-preserving.
- Migration tests compare schema and record counts/content, and exercise rollback/failure paths.
- CI build, unit tests and emulator instrumentation tests must pass. Physical-device biometric, notification, file-picker and restore tests remain a separate release gate.

## Implementation status (2026-10-30)
- Vintage theme selection UI added with four palette choices; Antique Botanical is the initial default. Surface treatment is warm and softly rounded; the current implementation uses flat cream fills rather than a raster paper texture.
- Existing native screens: local password gate, Home, grouped Records and More hubs, People, Vitals, Reports/Medicines, Analytics, Reminders, History, Tests, Interoperability, Received & Shared, Privacy, Settings and Offline Assistant.
- Existing primitives: SQLCipher database, local Keystore envelope, biometric-key helper, AES-GCM/PBKDF2 archive helper, migration helper and native PDF writer.
- Not yet represented as completed merely because primitives exist: biometric prompt-to-database-key integration; selectable archive export UI; import preview and duplicate/profile mapping; transactional merge/restore; selective PDF UI.
- The current phone navigation uses five bottom destinations with grouped secondary hubs. Build and physical-device verification are still required before declaring the UI or branch release-ready.
