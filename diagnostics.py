"""Structured additional diagnostic test catalogue.

Reference intervals are general adult screening defaults and are intentionally
editable at entry time. The reporting UI labels them as general ranges because
laboratory-specific intervals should take precedence.
"""

TEST_CATALOG = {
    "microbiology": [
        ("urine_culture", "Urine culture", "", "text", None, None, "No significant growth"),
        ("blood_culture", "Blood culture", "", "text", None, None, "No growth"),
        ("sputum_culture", "Sputum culture", "", "text", None, None, "No significant pathogen"),
        ("throat_culture", "Throat swab culture", "", "text", None, None, "No significant pathogen"),
        ("wound_culture", "Wound swab culture", "", "text", None, None, "No significant pathogen"),
        ("stool_culture", "Stool culture", "", "text", None, None, "No enteric pathogen"),
        ("malaria_smear", "Malaria smear", "", "text", None, None, "Negative"),
        ("malaria_antigen", "Malaria antigen", "", "text", None, None, "Negative"),
        ("dengue_ns1", "Dengue NS1", "", "text", None, None, "Negative"),
        ("dengue_igm", "Dengue IgM", "", "text", None, None, "Negative"),
        ("dengue_igg", "Dengue IgG", "", "text", None, None, "Negative"),
        ("hiv_ag_ab", "HIV-1/2 Ag/Ab", "", "text", None, None, "Non-reactive"),
        ("hbsag", "HBsAg", "", "text", None, None, "Non-reactive"),
        ("anti_hcv", "Anti-HCV", "", "text", None, None, "Non-reactive"),
        ("vdrl_rpr", "VDRL / RPR", "", "text", None, None, "Non-reactive"),
        ("tb_afb_smear", "TB AFB smear", "", "text", None, None, "Negative"),
        ("tb_gene_xpert", "TB GeneXpert / NAAT", "", "text", None, None, "MTB not detected"),
        ("h_pylori_stool", "H. pylori stool antigen", "", "text", None, None, "Negative"),
        ("h_pylori_breath", "H. pylori urea breath test", "", "text", None, None, "Negative"),
        ("c_diff_toxin", "C. difficile toxin", "", "text", None, None, "Negative"),
        ("covid_pcr", "SARS-CoV-2 PCR/NAAT", "", "text", None, None, "Not detected"),
        ("group_a_strep", "Group A Strep test", "", "text", None, None, "Negative"),
    ],
    "pathology": [
        ("reticulocyte_pct", "Reticulocyte count", "%", "numeric", 0.5, 2.5, "0.5–2.5"),
        ("reticulocyte_abs", "Absolute reticulocyte count", "10^9/L", "numeric", 25, 100, "25–100"),
        ("peripheral_smear", "Peripheral blood smear", "", "text", None, None, "No significant abnormality"),
        ("bone_marrow", "Bone marrow examination", "", "text", None, None, "No significant abnormality"),
        ("histopathology", "Histopathology", "", "text", None, None, "No significant abnormality"),
        ("biopsy_comment", "Biopsy / tissue diagnosis", "", "text", None, None, "See report"),
        ("pap_smear", "Pap smear / cervical cytology", "", "text", None, None, "NILM"),
        ("fnac", "FNAC / cytology", "", "text", None, None, "No malignant cells identified"),
        ("surgical_margin", "Surgical margin", "", "text", None, None, "Negative"),
        ("dysplasia_grade", "Dysplasia grade", "", "text", None, None, "None identified"),
        ("hpv_test", "High-risk HPV test", "", "text", None, None, "Negative"),
        ("occult_malignancy", "Malignancy comment", "", "text", None, None, "Not identified"),
        ("retic_index", "Reticulocyte production index", "", "numeric", 0.5, 2.5, "0.5–2.5"),
        ("immature_granulocyte_pct", "Immature granulocytes", "%", "numeric", 0, 1, "0–1"),
        ("nucleated_rbc", "Nucleated RBC", "/100 WBC", "numeric", 0, 0, "0"),
        ("blast_cells", "Blast cells", "%", "numeric", 0, 0, "0"),
        ("sickle_screen", "Sickle screen", "", "text", None, None, "Negative"),
        ("hemoglobin_electrophoresis", "Haemoglobin electrophoresis", "", "text", None, None, "No abnormal variant"),
        ("g6pd_screen", "G6PD screen", "", "text", None, None, "Normal"),
        ("coombs_direct", "Direct Coombs test", "", "text", None, None, "Negative"),
    ],
    "urine": [
        ("urine_color", "Colour", "", "text", None, None, "Pale yellow–yellow"),
        ("urine_appearance", "Appearance", "", "text", None, None, "Clear"),
        ("urine_specific_gravity", "Specific gravity", "", "numeric", 1.005, 1.030, "1.005–1.030"),
        ("urine_ph", "pH", "", "numeric", 4.5, 8.0, "4.5–8.0"),
        ("urine_protein", "Protein", "", "text", None, None, "Negative / trace"),
        ("urine_glucose", "Glucose", "", "text", None, None, "Negative"),
        ("urine_ketones", "Ketones", "", "text", None, None, "Negative"),
        ("urine_blood", "Blood / haemoglobin", "", "text", None, None, "Negative"),
        ("urine_bilirubin", "Bilirubin", "", "text", None, None, "Negative"),
        ("urine_urobilinogen", "Urobilinogen", "EU/dL", "numeric", 0.1, 1.0, "0.1–1.0"),
        ("urine_nitrite", "Nitrite", "", "text", None, None, "Negative"),
        ("urine_leukocyte_esterase", "Leukocyte esterase", "", "text", None, None, "Negative"),
        ("urine_rbc_hpf", "RBC", "/HPF", "numeric", 0, 2, "0–2"),
        ("urine_wbc_hpf", "WBC", "/HPF", "numeric", 0, 5, "0–5"),
        ("urine_epithelial_hpf", "Epithelial cells", "/HPF", "numeric", 0, 5, "0–5"),
        ("urine_casts", "Casts", "", "text", None, None, "None / occasional hyaline"),
        ("urine_crystals", "Crystals", "", "text", None, None, "None / occasional"),
        ("urine_bacteria", "Bacteria", "", "text", None, None, "None / few"),
        ("urine_albumin", "Urine albumin", "mg/L", "numeric", 0, 20, "<20"),
        ("urine_creatinine", "Urine creatinine", "mg/dL", "numeric", 20, 300, "20–300"),
        ("urine_acr", "Albumin/creatinine ratio", "mg/g", "numeric", 0, 30, "<30"),
        ("urine_culture_comment", "Urine microscopy / culture comment", "", "text", None, None, "No significant growth"),
    ],
    "stool": [
        ("stool_color", "Colour", "", "text", None, None, "Brown"),
        ("stool_consistency", "Consistency", "", "text", None, None, "Formed / soft"),
        ("stool_occult_blood", "Occult blood", "", "text", None, None, "Negative"),
        ("stool_mucus", "Mucus", "", "text", None, None, "Absent"),
        ("stool_rbc_hpf", "RBC", "/HPF", "numeric", 0, 2, "0–2"),
        ("stool_wbc_hpf", "WBC", "/HPF", "numeric", 0, 5, "0–5"),
        ("stool_fat", "Fat globules", "", "text", None, None, "Absent / few"),
        ("stool_reducing_substances", "Reducing substances", "", "text", None, None, "Negative"),
        ("stool_ph", "pH", "", "numeric", 5.5, 7.5, "5.5–7.5"),
        ("stool_ova", "Ova / helminth eggs", "", "text", None, None, "Not seen"),
        ("stool_cysts", "Protozoal cysts", "", "text", None, None, "Not seen"),
        ("stool_parasites", "Parasites", "", "text", None, None, "Not seen"),
        ("stool_calprotectin", "Faecal calprotectin", "ug/g", "numeric", 0, 50, "<50"),
        ("stool_elastase", "Faecal elastase", "ug/g", "numeric", 200, 1000, ">200"),
        ("stool_h_pylori", "H. pylori antigen", "", "text", None, None, "Negative"),
        ("stool_culture_comment", "Stool culture comment", "", "text", None, None, "No enteric pathogen"),
    ],
}

TEST_CATEGORY_LABELS = {
    "microbiology": "Microbiology",
    "pathology": "Pathology",
    "urine": "Urine analysis",
    "stool": "Stool analysis",
}

TEST_MAP = {k: {row[0]: row for row in rows} for k, rows in TEST_CATALOG.items()}
TEST_LOOKUP = {row[0]: (cat, row) for cat, rows in TEST_CATALOG.items() for row in rows}


def get_test(key):
    return TEST_LOOKUP.get(key)


def default_test_range(key):
    hit = get_test(key)
    if not hit:
        return None
    row = hit[1]
    return row[4], row[5], row[6]


def classify_test(key, value_numeric=None, value_text=None):
    hit = get_test(key)
    if not hit:
        return "unknown"
    row = hit[1]
    low, high, normal_text = row[4], row[5], row[6]
    if value_numeric is not None and (low is not None or high is not None):
        if low is not None and value_numeric < low:
            return "low"
        if high is not None and value_numeric > high:
            return "high"
        return "normal"
    text = (value_text or "").strip().lower()
    if not text:
        return "unknown"
    normal_words = [
        "negative", "non-reactive", "not detected", "no growth", "no significant",
        "no enteric", "no malignant", "not identified", "no abnormal", "absent",
        "not seen", "nilm", "normal", "negative / trace", "pale yellow", "yellow",
        "clear", "brown", "formed", "soft", "none", "few", "occasional",
    ]
    abnormal_words = [
        "positive", "reactive", "detected", "malignant", "abnormal", "present",
        "blast", "significant growth", "pathogen",
    ]
    if any(w in text for w in normal_words):
        return "normal"
    if any(w in text for w in abnormal_words):
        return "abnormal"
    return "unknown"

