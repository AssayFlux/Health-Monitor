# units.py — unit and short-label map for every tracked parameter

UNITS = {
    # Vitals
    "systolic": "mmHg", "diastolic": "mmHg", "pulse": "bpm",
    "weight_kg": "kg", "height_cm": "cm", "bmi": "kg/m2",
    "bmr_kcal": "kcal/day", "tdee_kcal": "kcal/day", "bsa": "m²", "whr": "", "absi": "",
    "spo2": "%", "temp": "degC", "temp_c": "degC",

    # Glycemic
    "fasting_glucose": "mg/dL", "pp_glucose": "mg/dL", "hba1c": "%",
    "insulin_fasting": "uIU/mL", "c_peptide": "ng/mL",
    "homa_ir": "", "homa_beta": "", "fructosamine": "umol/L",

    # Lipids
    "total_cholesterol": "mg/dL", "hdl": "mg/dL", "ldl": "mg/dL",
    "triglycerides": "mg/dL", "vldl": "mg/dL", "non_hdl": "mg/dL",
    "ldl_hdl_ratio": "", "total_hdl_ratio": "", "aip": "",
    "lpa": "nmol/L", "apob": "mg/dL",

    # Liver
    "alt_sgpt": "U/L", "ast_sgot": "U/L", "alp": "U/L", "ggt": "U/L",
    "bilirubin_total": "mg/dL", "bilirubin_direct": "mg/dL",
    "bilirubin_indirect": "mg/dL", "albumin": "g/dL", "globulin": "g/dL",
    "a_g_ratio": "", "total_protein": "g/dL",

    # Kidney
    "creatinine": "mg/dL", "urea": "mg/dL", "uric_acid": "mg/dL",
    "egfr": "mL/min/1.73m2",

    # Electrolytes
    "sodium": "mmol/L", "potassium": "mmol/L", "calcium": "mg/dL",
    "chloride": "mmol/L", "phosphate": "mg/dL", "magnesium": "mg/dL",

    # Iron
    "iron": "ug/dL", "ferritin": "ng/mL", "tibc": "ug/dL",
    "transferrin_sat": "%",

    # Haematology
    "hemoglobin": "g/dL", "hematocrit": "%", "wbc": "cells/uL",
    "platelets": "cells/uL", "rbc": "million/uL", "mpv": "fL",
    "mcv": "fL", "mch": "pg", "mchc": "g/dL", "rdw": "%",
    "neutrophils_pct": "%", "lymphocytes_pct": "%", "monocytes_pct": "%",
    "eosinophils_pct": "%", "basophils_pct": "%",

    # Coagulation
    "esr": "mm/hr", "pt_inr": "", "pct": "sec",
    "aptt": "sec", "d_dimer": "ug/mL FEU",
    "fibrinogen": "mg/dL", "tt": "sec",

    # Vitamins / minerals
    "vitamin_d": "ng/mL", "vitamin_b12": "pg/mL", "folate": "ng/mL",
    "vitamin_a": "ug/dL", "vitamin_e": "mg/L", "vitamin_c": "mg/dL",
    "vitamin_k": "ng/mL", "zinc": "ug/dL", "copper": "ug/dL",
    "selenium": "ug/L",

    # Cardiac / inflammation
    "crp": "mg/L", "homocysteine": "umol/L",
    "troponin": "ng/mL", "bnp": "pg/mL",

    # Thyroid
    "tsh": "mIU/L", "t3": "ng/dL", "t4": "ug/dL",
    "free_t3": "pg/mL", "free_t4": "ng/dL",
    "tpo_ab": "IU/mL", "thyroglobulin": "ng/mL",

    # Hormones
    "cortisol_am": "ug/dL", "prolactin": "ng/mL",
    "testosterone": "ng/dL", "free_testosterone": "pg/mL",
    "estrogen": "pg/mL", "progesterone": "ng/mL",
    "lh": "mIU/mL", "fsh": "mIU/mL",
    "dhea_s": "ug/dL", "shbg": "nmol/L", "amh": "ng/mL",

    # Tumour markers
    "psa_total": "ng/mL", "psa_free": "ng/mL",
    "ca125": "U/mL", "ca19_9": "U/mL", "afp": "ng/mL",
    "cea": "ng/mL", "beta_hcg": "mIU/mL",

    # Autoimmune
    "anti_ccp": "U/mL", "ra_factor": "IU/mL", "ana": "titre",

    # Enzymes
    "ldh": "U/L", "ck_total": "U/L", "ck_mb": "ng/mL",
    "lipase": "U/L", "amylase": "U/L", "ammonia": "umol/L",
}
