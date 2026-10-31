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
    low, high, _normal_text = row[4], row[5], row[6]
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



# Extended multi-discipline catalogue. Numeric limits are intentionally unset
# unless a laboratory-specific interval is supplied with the patient's report.
# Each record: key, display name, result_kind, units, default_unit, normal_hint.
EXTENDED_TESTS = {
 "biochemistry": [
  ("glucose_random","Random blood glucose","quantitative",["mg/dL","mmol/L"],"mg/dL","Use lab interval"),
  ("glucose_postprandial","Post-prandial glucose","quantitative",["mg/dL","mmol/L"],"mg/dL","Use lab interval"),
  ("urea","Blood urea","quantitative",["mg/dL","mmol/L"],"mg/dL","Use lab interval"),
  ("bun","Blood urea nitrogen","quantitative",["mg/dL","mmol/L"],"mg/dL","Use lab interval"),
  ("uric_acid","Uric acid","quantitative",["mg/dL","umol/L"],"mg/dL","Use lab interval"),
  ("sodium","Sodium","quantitative",["mmol/L","mEq/L"],"mmol/L","Use lab interval"),
  ("potassium","Potassium","quantitative",["mmol/L","mEq/L"],"mmol/L","Use lab interval"),
  ("chloride","Chloride","quantitative",["mmol/L","mEq/L"],"mmol/L","Use lab interval"),
  ("calcium_total","Total calcium","quantitative",["mg/dL","mmol/L"],"mg/dL","Use lab interval"),
  ("calcium_ionized","Ionized calcium","quantitative",["mmol/L"],"mmol/L","Use lab interval"),
  ("phosphate","Phosphate","quantitative",["mg/dL","mmol/L"],"mg/dL","Use lab interval"),
  ("magnesium","Magnesium","quantitative",["mg/dL","mmol/L"],"mg/dL","Use lab interval"),
  ("alt","ALT / SGPT","quantitative",["U/L"],"U/L","Use lab interval"),
  ("ast","AST / SGOT","quantitative",["U/L"],"U/L","Use lab interval"),
  ("alp","Alkaline phosphatase","quantitative",["U/L"],"U/L","Use lab interval"),
  ("ggt","Gamma-glutamyl transferase","quantitative",["U/L"],"U/L","Use lab interval"),
  ("bilirubin_total","Total bilirubin","quantitative",["mg/dL","umol/L"],"mg/dL","Use lab interval"),
  ("bilirubin_direct","Direct bilirubin","quantitative",["mg/dL","umol/L"],"mg/dL","Use lab interval"),
  ("total_protein","Total protein","quantitative",["g/dL","g/L"],"g/dL","Use lab interval"),
  ("albumin","Albumin","quantitative",["g/dL","g/L"],"g/dL","Use lab interval"),
  ("iron","Serum iron","quantitative",["ug/dL","umol/L"],"ug/dL","Use lab interval"),
  ("tibc","TIBC","quantitative",["ug/dL","umol/L"],"ug/dL","Use lab interval"),
  ("ferritin","Ferritin","quantitative",["ng/mL","ug/L"],"ng/mL","Use lab interval"),
  ("crp","C-reactive protein","quantitative",["mg/L","mg/dL"],"mg/L","Use lab interval"),
  ("hs_crp","High-sensitivity CRP","quantitative",["mg/L"],"mg/L","Use lab interval"),
  ("procalcitonin","Procalcitonin","quantitative",["ng/mL","ug/L"],"ng/mL","Use lab interval"),
  ("amylase","Amylase","quantitative",["U/L"],"U/L","Use lab interval"),
  ("lipase","Lipase","quantitative",["U/L"],"U/L","Use lab interval"),
  ("ldh","LDH","quantitative",["U/L"],"U/L","Use lab interval"),
  ("ck_total","Creatine kinase","quantitative",["U/L"],"U/L","Use lab interval"),
  ("troponin_i","Troponin I","quantitative",["ng/L","ng/mL"],"ng/L","Assay-specific decision limit"),
  ("troponin_t","Troponin T","quantitative",["ng/L","ng/mL"],"ng/L","Assay-specific decision limit"),
  ("bnp","BNP","quantitative",["pg/mL"],"pg/mL","Use lab interval"),
  ("nt_probnp","NT-proBNP","quantitative",["pg/mL"],"pg/mL","Use lab interval"),
  ("tsh","TSH","quantitative",["uIU/mL","mIU/L"],"uIU/mL","Use lab interval"),
  ("free_t4","Free T4","quantitative",["ng/dL","pmol/L"],"ng/dL","Use lab interval"),
  ("free_t3","Free T3","quantitative",["pg/mL","pmol/L"],"pg/mL","Use lab interval"),
  ("vitamin_d","25-OH Vitamin D","quantitative",["ng/mL","nmol/L"],"ng/mL","Use lab interval"),
  ("vitamin_b12","Vitamin B12","quantitative",["pg/mL","pmol/L"],"pg/mL","Use lab interval"),
  ("folate","Folate","quantitative",["ng/mL","nmol/L"],"ng/mL","Use lab interval"),
  ("psa","PSA","quantitative",["ng/mL"],"ng/mL","Age- and assay-specific interval"),
 ],
 "haematology": [
  ("hemoglobin","Haemoglobin","quantitative",["g/dL","g/L"],"g/dL","Use lab interval"),
  ("hematocrit","Haematocrit / PCV","quantitative",["%","L/L"],"%","Use lab interval"),
  ("rbc","Red blood cell count","quantitative",["million/uL","10^12/L"],"million/uL","Use lab interval"),
  ("wbc","White blood cell count","quantitative",["/uL","10^9/L"],"/uL","Use lab interval"),
  ("platelets","Platelet count","quantitative",["/uL","10^9/L"],"/uL","Use lab interval"),
  ("neutrophils","Neutrophils","quantitative",["%","10^9/L"],"%","Use lab interval"),
  ("lymphocytes","Lymphocytes","quantitative",["%","10^9/L"],"%","Use lab interval"),
  ("eosinophils","Eosinophils","quantitative",["%","10^9/L"],"%","Use lab interval"),
  ("esr","ESR","quantitative",["mm/hr"],"mm/hr","Age- and method-specific interval"),
  ("pt","Prothrombin time","quantitative",["seconds"],"seconds","Use lab interval"),
  ("inr","INR","quantitative",["ratio"],"ratio","Clinical indication-specific target"),
  ("aptt","aPTT","quantitative",["seconds"],"seconds","Use lab interval"),
 ],
 "microbiology": [
  ("urine_culture_result","Urine culture","qualitative",["text"],"text","Organism, colony count and sensitivity"),
  ("blood_culture_result","Blood culture","qualitative",["text"],"text","Growth / no growth; organism"),
  ("sputum_culture_result","Sputum culture","qualitative",["text"],"text","Organism and sensitivity"),
  ("afb_smear_result","AFB smear","qualitative",["text"],"text","Positive / negative; grading"),
  ("gram_stain","Gram stain","qualitative",["text"],"text","Morphology and reaction"),
  ("fungal_koh","KOH mount","qualitative",["text"],"text","Seen / not seen"),
  ("malaria_rapid","Malaria rapid antigen","screening",["text"],"text","Positive / negative"),
  ("dengue_ns1_result","Dengue NS1 antigen","screening",["text"],"text","Positive / negative"),
  ("hiv_screen","HIV screening assay","screening",["text"],"text","Reactive / non-reactive; confirm per protocol"),
  ("hbsag_screen","HBsAg screening","screening",["text"],"text","Reactive / non-reactive"),
  ("hcv_screen","HCV antibody screening","screening",["text"],"text","Reactive / non-reactive"),
  ("tb_naat","TB NAAT / molecular test","qualitative",["text"],"text","Detected / not detected"),
  ("covid_naat","SARS-CoV-2 NAAT","qualitative",["text"],"text","Detected / not detected"),
  ("stool_ova_parasite","Stool ova and parasites","qualitative",["text"],"text","Seen / not seen"),
  ("antibiotic_sensitivity","Antimicrobial susceptibility","qualitative",["text"],"text","S/I/R per lab standard"),
 ],
 "imaging": [
  ("xray_chest","Chest X-ray","report",["text"],"text","Impression / findings"),
  ("xray_other","X-ray","report",["text"],"text","Impression / findings"),
  ("ultrasound_abdomen","Ultrasound abdomen","report",["text"],"text","Impression / findings"),
  ("usg_pelvis","Ultrasound pelvis","report",["text"],"text","Impression / findings"),
  ("ct_brain","CT brain","report",["text"],"text","Impression / findings"),
  ("ct_chest","CT chest","report",["text"],"text","Impression / findings"),
  ("ct_abdomen","CT abdomen","report",["text"],"text","Impression / findings"),
  ("mri_brain","MRI brain","report",["text"],"text","Impression / findings"),
  ("mri_spine","MRI spine","report",["text"],"text","Impression / findings"),
  ("mammography","Mammography","graded",["BI-RADS"],"BI-RADS","Record category and report"),
  ("bone_density","DEXA bone density","quantitative",["g/cm2","T-score","Z-score"],"T-score","Use report and population-specific interpretation"),
  ("ecg","Electrocardiogram","report",["text"],"text","Interpretation by qualified clinician"),
  ("echocardiogram","Echocardiogram","report",["text"],"text","Measurements and impression"),
  ("pet_ct","PET-CT","report",["text"],"text","Impression / findings"),
  ("endoscopy","Endoscopy","report",["text"],"text","Findings / biopsy correlation"),
 ],
 "screening": [
  ("fo_bt","Faecal occult blood screening","screening",["text"],"text","Positive / negative"),
  ("pap_screen","Cervical cytology screening","screening",["text"],"text","Use Bethesda report"),
  ("hpv_screen","High-risk HPV screening","screening",["text"],"text","Detected / not detected"),
  ("newborn_screen","Newborn screening panel","screening",["text"],"text","Screen positive / negative; confirmatory testing"),
  ("hearing_screen","Hearing screening","screening",["text"],"text","Pass / refer"),
  ("vision_screen","Vision screening","screening",["text"],"text","Pass / refer"),
  ("tb_screen","Tuberculosis screening","screening",["text"],"text","Screen result; diagnostic confirmation as indicated"),
 ],
}

def catalogue_json():
    import json
    result = []
    for category, rows in EXTENDED_TESTS.items():
        for key, name, kind, units_list, default_unit, hint in rows:
            result.append({"category": category, "key": key, "name": name,
                           "kind": kind, "units": units_list,
                           "default_unit": default_unit, "reference_hint": hint})
    for category, rows in TEST_CATALOG.items():
        for key, name, unit, kind, low, high, hint in rows:
            result.append({"category": category, "key": key, "name": name,
                           "kind": "quantitative" if kind == "numeric" else "qualitative",
                           "units": [unit] if unit else ["text"],
                           "default_unit": unit or "text", "reference_hint": hint})
    return json.dumps(result)

def classify_result(value_numeric=None, value_text=None, ref_low=None, ref_high=None,
                    qualitative_status="unknown"):
    """Classify using supplied report interval; never infer a universal interval."""
    if value_numeric is not None:
        try:
            value = float(value_numeric)
            low = float(ref_low) if ref_low not in (None, "") else None
            high = float(ref_high) if ref_high not in (None, "") else None
            if low is not None and value < low: return "low"
            if high is not None and value > high: return "high"
            if low is not None or high is not None: return "normal"
            return "unclassified"
        except (TypeError, ValueError):
            return "unclassified"
    allowed = {"normal", "abnormal", "positive", "negative", "reactive",
               "non_reactive", "detected", "not_detected", "borderline",
               "critical", "pending", "unclassified"}
    return qualitative_status if qualitative_status in allowed else "unclassified"
