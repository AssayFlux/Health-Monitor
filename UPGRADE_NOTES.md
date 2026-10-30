# Health Monitor — Clinical data linkage upgrade

This version preserves the existing application and adds a unified monitoring layer.

## Data flow
Entry forms → SQLite → canonical metric series → Graphs & Trends → Statistics → Print preview/report.

The graph panel now auto-selects the first available metric and has separate tabs for Vitals, Weight, Anthropometric, Lab results and Diagnostic tests.

## Anthropometric trends
BMI, BMR, TDEE and BSA are calculated from each recorded weight and use the original measurement timestamp, so they are longitudinal rather than a single dashboard snapshot.

## Additional diagnostics
A new `clinical_test_result` table stores 80 configurable microbiology, pathology, urine and stool parameters. Numeric results can be graphed and statistically analysed; qualitative results are shown in the clinical report with status colouring.

Reference intervals are general defaults. The reporting laboratory's own interval should take precedence when it differs.

## Monitoring grade
The grade is a transparent monitoring index based on recorded observations and configured ranges. Missing data are not treated as abnormal. Active medicines are included as clinical context and medication-data warnings, but a medicine is not penalised merely because it is prescribed.

The report also includes observed trajectories and a clearly labelled 30-day mathematical extrapolation where enough data exist. This is not a medical prediction or diagnosis.

## Database and fresh deployments
The repository does not include a patient database. On startup, `db.create_all()` creates the required empty schema in the configured SQLite database. Keep runtime database files and backups out of version control; `.gitignore` excludes local database files.


## Final graph/weight consistency patch — 2026-09-26
- Weight readings are now resolved by the newest non-null weight record rather than the newest vital row.
- Weight is included in the anthropometric monitoring domain and is evaluated against a height-derived BMI reference interval when height is available.
- Dashboard, statistics, overall monitoring, graphs and print summaries use the same latest-weight source.
- Weight trend availability and anthropometric trends use the complete stored history rather than the dashboard's 50-row display limit.
- Interactive Plotly date sliders now share one consistent styled configuration across chart types.
- Dashboard weight tab reports the number of detected weight readings and latest recorded weight.
