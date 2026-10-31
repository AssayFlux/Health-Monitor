#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "\${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
echo "Health Monitor — manual improvement audit"
echo "This script is deliberately not wired into push/PR automation."
command -v gradle >/dev/null 2>&1 || { echo "Install Gradle 8.13+ and Android SDK 36." >&2; exit 2; }
gradle :app:lintDebug --no-daemon
python3 -m pip install --user ruff bandit
export PATH="$HOME/.local/bin:$PATH"
ruff check app/src/main/python tests
bandit -r app/src/main/python -ll
PYTHONPATH=app/src/main/python python3 -m unittest discover -s tests/integrity -v
PYTHONPATH=app/src/main/python python3 -m unittest discover -s tests/security -v
cat <<'CHECKLIST'
Manual-only review checklist:
[ ] TalkBack, Switch Access, keyboard focus, large text and contrast.
[ ] Localisation, date/time/number formats and content descriptions.
[ ] Clinical reference intervals verified against authoritative lab sources.
[ ] FHIR profiles, terminology mappings, provenance and duplicate handling.
[ ] Migration rollback, biometric key invalidation and password fallback on devices.
[ ] Performance on low-memory and older supported Android devices.
[ ] Record findings, owners, evidence and accepted risks.
CHECKLIST
