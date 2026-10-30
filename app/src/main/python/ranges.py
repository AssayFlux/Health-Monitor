RANGES = {
    "systolic": [
        {"sex": None, "age_min": 1, "age_max": 12, "flag": None, "low": 90, "high": 110},
        {"sex": None, "age_min": 13, "age_max": 17, "flag": None, "low": 95, "high": 120},
        {"sex": None, "age_min": 18, "age_max": 64, "flag": None, "low": 90, "high": 120},
        {"sex": None, "age_min": 65, "age_max": 130, "flag": None, "low": 90, "high": 130},
    ],
    "diastolic": [
        {"sex": None, "age_min": 1, "age_max": 12, "flag": None, "low": 55, "high": 75},
        {"sex": None, "age_min": 13, "age_max": 17, "flag": None, "low": 60, "high": 80},
        {"sex": None, "age_min": 18, "age_max": 64, "flag": None, "low": 60, "high": 80},
        {"sex": None, "age_min": 65, "age_max": 130, "flag": None, "low": 60, "high": 85},
    ],
    "pulse": [
        {"sex": None, "age_min": 1, "age_max": 12, "flag": None, "low": 70, "high": 110},
        {"sex": None, "age_min": 13, "age_max": 130, "flag": None, "low": 60, "high": 100},
    ],
    "bmi": [
        {"sex": None, "age_min": 2, "age_max": 19, "flag": None, "low": 14, "high": 24},
        {"sex": None, "age_min": 20, "age_max": 64, "flag": None, "low": 18.5, "high": 24.9},
        {"sex": None, "age_min": 65, "age_max": 130, "flag": None, "low": 22, "high": 27},
    ],
    "spo2": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 95, "high": 100}],
    "temp": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 36.1, "high": 37.2}],
    "fasting_glucose": [
        {"sex": None, "age_min": 1, "age_max": 17, "flag": None, "low": 70, "high": 100},
        {"sex": None, "age_min": 18, "age_max": 130, "flag": "diabetic", "low": 80, "high": 130},
        {"sex": None, "age_min": 18, "age_max": 130, "flag": "pregnant", "low": 70, "high": 95},
        {"sex": None, "age_min": 18, "age_max": 130, "flag": None, "low": 70, "high": 100},
    ],
    "pp_glucose": [
        {"sex": None, "age_min": 1, "age_max": 130, "flag": "diabetic", "low": 70, "high": 180},
        {"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 70, "high": 140},
    ],
    "hba1c": [
        {"sex": None, "age_min": 1, "age_max": 130, "flag": "diabetic", "low": 4.0, "high": 7.0},
        {"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 4.0, "high": 5.6},
    ],
    "total_cholesterol": [
        {"sex": None, "age_min": 1, "age_max": 19, "flag": None, "low": 0, "high": 170},
        {"sex": None, "age_min": 20, "age_max": 130, "flag": None, "low": 0, "high": 200},
    ],
    "hdl": [
        {"sex": "M", "age_min": 20, "age_max": 130, "flag": None, "low": 40, "high": 999},
        {"sex": "F", "age_min": 20, "age_max": 130, "flag": None, "low": 50, "high": 999},
        {"sex": None, "age_min": 1, "age_max": 19, "flag": None, "low": 40, "high": 999},
    ],
    "ldl": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 0, "high": 100}],
    "triglycerides": [
        {"sex": None, "age_min": 1, "age_max": 9, "flag": None, "low": 0, "high": 75},
        {"sex": None, "age_min": 10, "age_max": 17, "flag": None, "low": 0, "high": 90},
        {"sex": None, "age_min": 18, "age_max": 130, "flag": None, "low": 0, "high": 150},
    ],
    "creatinine": [
        {"sex": "M", "age_min": 18, "age_max": 130, "flag": None, "low": 0.74, "high": 1.35},
        {"sex": "F", "age_min": 18, "age_max": 130, "flag": None, "low": 0.59, "high": 1.04},
        {"sex": None, "age_min": 1, "age_max": 17, "flag": None, "low": 0.3, "high": 0.7},
    ],
    "urea": [
        {"sex": None, "age_min": 1, "age_max": 17, "flag": None, "low": 5, "high": 25},
        {"sex": None, "age_min": 18, "age_max": 130, "flag": None, "low": 15, "high": 45},
    ],
    "uric_acid": [
        {"sex": "M", "age_min": 18, "age_max": 130, "flag": None, "low": 3.5, "high": 7.2},
        {"sex": "F", "age_min": 18, "age_max": 130, "flag": None, "low": 2.6, "high": 6.0},
        {"sex": None, "age_min": 1, "age_max": 17, "flag": None, "low": 2.0, "high": 5.5},
    ],
    "alt_sgpt": [
        {"sex": "M", "age_min": 18, "age_max": 130, "flag": None, "low": 7, "high": 55},
        {"sex": "F", "age_min": 18, "age_max": 130, "flag": None, "low": 7, "high": 45},
        {"sex": None, "age_min": 1, "age_max": 17, "flag": None, "low": 5, "high": 30},
    ],
    "ast_sgot": [
        {"sex": "M", "age_min": 18, "age_max": 130, "flag": None, "low": 8, "high": 48},
        {"sex": "F", "age_min": 18, "age_max": 130, "flag": None, "low": 8, "high": 43},
        {"sex": None, "age_min": 1, "age_max": 17, "flag": None, "low": 10, "high": 40},
    ],
    "bilirubin_total": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 0.1, "high": 1.2}],
    "albumin": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 3.5, "high": 5.0}],
    "sodium": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 135, "high": 145}],
    "potassium": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 3.5, "high": 5.1}],
    "calcium": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 8.6, "high": 10.3}],
    "hemoglobin": [
        {"sex": "M", "age_min": 18, "age_max": 130, "flag": None, "low": 13.5, "high": 17.5},
        {"sex": "F", "age_min": 18, "age_max": 130, "flag": "pregnant", "low": 11.0, "high": 14.0},
        {"sex": "F", "age_min": 18, "age_max": 130, "flag": None, "low": 12.0, "high": 15.5},
        {"sex": None, "age_min": 1, "age_max": 17, "flag": None, "low": 11.0, "high": 16.0},
    ],
    "wbc": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 4000, "high": 11000}],
    "platelets": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 150000, "high": 450000}],
    "vitamin_d": [
        {"sex": None, "age_min": 1, "age_max": 17, "flag": None, "low": 20, "high": 100},
        {"sex": None, "age_min": 18, "age_max": 130, "flag": None, "low": 30, "high": 100},
    ],
    "vitamin_b12": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 200, "high": 900}],
    "crp": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 0, "high": 5}],
    "tsh": [
        {"sex": None, "age_min": 1, "age_max": 17, "flag": None, "low": 0.7, "high": 6.0},
        {"sex": None, "age_min": 18, "age_max": 64, "flag": "pregnant", "low": 0.1, "high": 2.5},
        {"sex": None, "age_min": 18, "age_max": 64, "flag": None, "low": 0.4, "high": 4.0},
        {"sex": None, "age_min": 65, "age_max": 130, "flag": None, "low": 0.4, "high": 5.9},
    ],
    "t3": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 80, "high": 200}],
    "t4": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 5.0, "high": 12.0}],
    "free_t3": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 2.3, "high": 4.2}],
    "free_t4": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 0.8, "high": 1.8}],
    "cortisol_am": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 6, "high": 23}],
    "prolactin": [
        {"sex": "M", "age_min": 18, "age_max": 130, "flag": None, "low": 4, "high": 15},
        {"sex": "F", "age_min": 18, "age_max": 130, "flag": "pregnant", "low": 35, "high": 350},
        {"sex": "F", "age_min": 18, "age_max": 130, "flag": None, "low": 4, "high": 23},
    ],
    "testosterone": [
        {"sex": "M", "age_min": 18, "age_max": 130, "flag": None, "low": 300, "high": 1000},
        {"sex": "F", "age_min": 18, "age_max": 130, "flag": None, "low": 15, "high": 70},
    ],
    "estrogen": [{"sex": "F", "age_min": 18, "age_max": 130, "flag": None, "low": 20, "high": 350}],
    "progesterone": [{"sex": "F", "age_min": 18, "age_max": 130, "flag": None, "low": 0.1, "high": 25}],
    "lh": [
        {"sex": "M", "age_min": 18, "age_max": 130, "flag": None, "low": 1.7, "high": 8.6},
        {"sex": "F", "age_min": 18, "age_max": 130, "flag": None, "low": 1.0, "high": 11.4},
    ],
    "fsh": [
        {"sex": "M", "age_min": 18, "age_max": 130, "flag": None, "low": 1.5, "high": 12.4},
        {"sex": "F", "age_min": 18, "age_max": 130, "flag": None, "low": 1.5, "high": 12.4},
    ],
    "insulin_fasting": [{"sex": None, "age_min": 1, "age_max": 130, "flag": None, "low": 2.6, "high": 24.9}],
    # ---- Extended biochemistry ----
    "alp": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":44,"high":147},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":44,"high":147},
        {"sex":None,"age_min":1,"age_max":17,"flag":None,"low":100,"high":400},
    ],
    "ggt": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":8,"high":61},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":5,"high":36},
    ],
    "total_protein": [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":6.0,"high":8.3}],
    "globulin":      [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":2.0,"high":3.5}],
    "a_g_ratio":     [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":1.0,"high":2.5}],
    "chloride":      [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":98,"high":107}],
    "phosphate":     [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":2.5,"high":4.5}],
    "magnesium":     [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":1.7,"high":2.2}],
    "iron": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":65,"high":175},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":50,"high":170},
    ],
    "ferritin": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":30,"high":400},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":13,"high":150},
    ],
    "tibc":            [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":250,"high":450}],
    "transferrin_sat": [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":20,"high":50}],
    "ldl_hdl_ratio":   [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":3.5}],
    "total_hdl_ratio": [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":5.0}],
    "non_hdl":         [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":130}],
    "vldl":            [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":5,"high":40}],
    "aip":             [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":-0.3,"high":0.1}],
    "egfr":            [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":90,"high":200}],
    "lpa":             [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":75}],
    "apob": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":0,"high":90},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":0,"high":80},
    ],
    "homocysteine":    [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":5,"high":15}],

    # ---- Haematology extended ----
    "hematocrit": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":38.8,"high":50.0},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":34.9,"high":44.5},
    ],
    "mcv":  [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":80,"high":100}],
    "mch":  [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":27,"high":33}],
    "mchc": [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":32,"high":36}],
    "rdw":  [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":11.5,"high":14.5}],
    "neutrophils_pct": [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":40,"high":75}],
    "lymphocytes_pct": [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":20,"high":45}],
    "monocytes_pct":   [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":2,"high":10}],
    "eosinophils_pct": [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":1,"high":6}],
    "basophils_pct":   [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":2}],
    "esr": [
        {"sex":"M","age_min":18,"age_max":49,"flag":None,"low":0,"high":15},
        {"sex":"M","age_min":50,"age_max":130,"flag":None,"low":0,"high":20},
        {"sex":"F","age_min":18,"age_max":49,"flag":None,"low":0,"high":20},
        {"sex":"F","age_min":50,"age_max":130,"flag":None,"low":0,"high":30},
    ],
    "pt_inr":          [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0.8,"high":1.2}],
    "aptt":            [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":25,"high":35}],
    "d_dimer":         [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":0.5}],

    # ---- Diabetes extended ----
    "c_peptide":       [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0.8,"high":3.9}],
    "homa_ir":         [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0.5,"high":2.0}],
    "homa_beta":       [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":100,"high":300}],
    "fructosamine":    [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":205,"high":285}],

    # ---- Thyroid extended ----
    "tpo_ab":          [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":35}],
    "thyroglobulin":   [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":3,"high":40}],

    # ---- Vitamins / minerals extra ----
    "folate":          [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":3,"high":17}],
    "zinc":            [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":60,"high":120}],
    "copper":          [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":70,"high":140}],
    "selenium":        [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":70,"high":150}],

    # ---- Cardiac / inflammation extra ----
    "troponin":        [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":0.04}],
    "bnp":             [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":100}],

    "bilirubin_direct":   [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0.0,"high":0.3}],
    "bilirubin_indirect": [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0.2,"high":0.8}],
    "rbc": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":4.5,"high":5.9},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":4.1,"high":5.1},
    ],
    "mpv":        [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":7.5,"high":11.5}],
    "pct":        [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":11,"high":13.5}],
    "fibrinogen": [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":200,"high":400}],
    "tt":         [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":14,"high":21}],
    "anti_ccp":   [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":17}],
    "ra_factor":  [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":14}],
    "ana":        [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":1}],
    "vitamin_a":  [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":30,"high":80}],
    "vitamin_e":  [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":5,"high":20}],
    "vitamin_c":  [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0.4,"high":2.0}],
    "vitamin_k":  [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0.1,"high":2.2}],
    "psa_total":  [{"sex":"M","age_min":40,"age_max":130,"flag":None,"low":0,"high":4.0}],
    "psa_free":   [{"sex":"M","age_min":40,"age_max":130,"flag":None,"low":0.25,"high":5.0}],
    "ca125":      [{"sex":"F","age_min":18,"age_max":130,"flag":None,"low":0,"high":35}],
    "ca19_9":     [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":37}],
    "afp":        [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":10}],
    "cea":        [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":3.0}],
    "beta_hcg":   [{"sex":"F","age_min":18,"age_max":130,"flag":None,"low":0,"high":5}],
    "dhea_s": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":80,"high":560},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":35,"high":430},
    ],
    "shbg": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":10,"high":57},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":18,"high":144},
    ],
    "free_testosterone": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":47,"high":244},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":0.5,"high":9.0},
    ],
    "amh":        [{"sex":"F","age_min":18,"age_max":130,"flag":None,"low":1.0,"high":4.0}],
    "ammonia":    [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":11,"high":35}],
    "ldh":        [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":140,"high":280}],
    "ck_total":   [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":30,"high":200}],
    "ck_mb":      [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":5}],
    "lipase":     [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":13,"high":60}],
    "amylase":    [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":25,"high":125}],

    "waist_cm": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":0,"high":94},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":0,"high":80},
    ],
    "hip_cm": [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":200}],
    "waist_hip_ratio": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":0,"high":0.95},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":0,"high":0.85},
    ],
    "neck_cm":       [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":45}],
    "body_fat_pct": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":10,"high":22},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":18,"high":30},
    ],
    "muscle_mass_kg": [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":999}],
    "bmr_kcal":       [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":9999}],
    "tdee_kcal":      [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":9999}],
    "whr": [
        {"sex":"M","age_min":18,"age_max":130,"flag":None,"low":0,"high":0.95},
        {"sex":"F","age_min":18,"age_max":130,"flag":None,"low":0,"high":0.85},
    ],
    "absi":           [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":0.083}],
    "bsa":            [{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":1.0,"high":2.5}],
    "ideal_weight_kg":[{"sex":None,"age_min":1,"age_max":130,"flag":None,"low":0,"high":999}],

}

FRIENDLY = {
    # Vitals
    "systolic":"SBP", "diastolic":"DBP", "pulse":"HR",
    "bmi":"BMI", "spo2":"SpO2", "temp":"Temp",
    "weight_kg":"Weight", "height_cm":"Height",
    "bmr_kcal":"BMR", "tdee_kcal":"TDEE", "bsa":"BSA", "whr":"WHR", "absi":"ABSI",
    # Glycemic
    "fasting_glucose":"FBS", "pp_glucose":"PPBS", "hba1c":"HbA1c",
    "insulin_fasting":"Fasting Insulin", "c_peptide":"C-peptide",
    "homa_ir":"HOMA-IR", "homa_beta":"HOMA-Beta", "fructosamine":"Fructosamine",
    # Lipids
    "total_cholesterol":"TC", "hdl":"HDL-C", "ldl":"LDL-C",
    "triglycerides":"TG", "vldl":"VLDL-C", "non_hdl":"Non-HDL-C",
    "ldl_hdl_ratio":"LDL/HDL", "total_hdl_ratio":"TC/HDL",
    "aip":"AIP", "lpa":"Lp(a)", "apob":"ApoB",
    # Liver
    "alt_sgpt":"ALT (SGPT)", "ast_sgot":"AST (SGOT)", "alp":"ALP", "ggt":"GGT",
    "bilirubin_total":"TBil", "bilirubin_direct":"DBil", "bilirubin_indirect":"IBil",
    "albumin":"Alb", "globulin":"Glob", "a_g_ratio":"A/G",
    "total_protein":"TP",
    # Kidney
    "creatinine":"Cr", "urea":"Urea", "uric_acid":"UA",
    "egfr":"eGFR",
    # Electrolytes
    "sodium":"Na+", "potassium":"K+", "calcium":"Ca2+",
    "chloride":"Cl-", "phosphate":"PO4", "magnesium":"Mg2+",
    # Iron
    "iron":"Serum Iron", "ferritin":"Ferritin", "tibc":"TIBC",
    "transferrin_sat":"TSAT",
    # Haematology
    "hemoglobin":"Hb", "hematocrit":"Hct", "wbc":"WBC",
    "platelets":"PLT", "rbc":"RBC", "mpv":"MPV",
    "mcv":"MCV", "mch":"MCH", "mchc":"MCHC", "rdw":"RDW",
    "neutrophils_pct":"Neut%", "lymphocytes_pct":"Lymph%",
    "monocytes_pct":"Mono%", "eosinophils_pct":"Eos%", "basophils_pct":"Baso%",
    # Coagulation
    "esr":"ESR", "pt_inr":"PT/INR", "pct":"PT",
    "aptt":"APTT", "d_dimer":"D-dimer",
    "fibrinogen":"Fibrinogen", "tt":"TT",
    # Vitamins
    "vitamin_d":"25-OH Vit D", "vitamin_b12":"Vit B12",
    "folate":"Folate", "vitamin_a":"Vit A", "vitamin_e":"Vit E",
    "vitamin_c":"Vit C", "vitamin_k":"Vit K",
    "zinc":"Zn", "copper":"Cu", "selenium":"Se",
    # Cardiac
    "crp":"CRP", "homocysteine":"Hcy",
    "troponin":"Troponin", "bnp":"BNP",
    # Thyroid
    "tsh":"TSH", "t3":"T3", "t4":"T4",
    "free_t3":"FT3", "free_t4":"FT4",
    "tpo_ab":"Anti-TPO", "thyroglobulin":"Tg",
    # Hormones
    "cortisol_am":"Cortisol (AM)", "prolactin":"PRL",
    "testosterone":"Testosterone", "free_testosterone":"Free T",
    "estrogen":"Estradiol", "progesterone":"Progesterone",
    "lh":"LH", "fsh":"FSH",
    "dhea_s":"DHEA-S", "shbg":"SHBG", "amh":"AMH",
    # Tumour markers
    "psa_total":"PSA Total", "psa_free":"PSA Free",
    "ca125":"CA-125", "ca19_9":"CA 19-9",
    "afp":"AFP", "cea":"CEA", "beta_hcg":"Beta-hCG",
    # Autoimmune
    "anti_ccp":"Anti-CCP", "ra_factor":"RA Factor", "ana":"ANA",
    # Enzymes
    "ldh":"LDH", "ck_total":"CK Total", "ck_mb":"CK-MB",
    "lipase":"Lipase", "amylase":"Amylase", "ammonia":"NH3",
}


FIELD_ALIASES = {
    "temp_c": "temp",
}
if "temp_c" not in FRIENDLY:
    FRIENDLY["temp_c"] = FRIENDLY.get("temp", "Temperature")

def _canonical_metric(metric):
    return FIELD_ALIASES.get(metric, metric)

def resolve_range(metric, age=None, sex=None, flags=None):
    metric = _canonical_metric(metric)
    flags = flags or []
    rules = RANGES.get(metric, [])
    if not rules:
        return None
    for want_flag in list(flags) + [None]:
        for want_sex in [sex, None]:
            for r in rules:
                if r.get("flag") != want_flag:
                    continue
                if r.get("sex") and r.get("sex") != want_sex:
                    continue
                if age is not None:
                    if not (r["age_min"] <= age <= r["age_max"]):
                        continue
                return r
    return rules[0]


def classify(metric, value, age=None, sex=None, flags=None):
    if value is None:
        return "unknown"
    r = resolve_range(metric, age, sex, flags)
    if not r:
        return "unknown"
    if value < r["low"]:
        return "low"
    if value > r["high"]:
        return "high"
    return "normal"


def explain(metric, value, age=None, sex=None, flags=None):
    if value is None:
        return "No reading"
    r = resolve_range(metric, age, sex, flags)
    name = FRIENDLY.get(metric, metric)
    if not r:
        return "{0}: {1}".format(name, value)
    lo = r["low"]
    hi = r["high"]
    stt = classify(metric, value, age, sex, flags)
    if stt == "normal":
        return "{0} is in the healthy range ({1}-{2}).".format(name, lo, hi)
    if stt == "low":
        return "LOW: {0}. Healthy range {1}-{2}.".format(name, lo, hi)
    return "HIGH: {0}. Healthy range {1}-{2}.".format(name, lo, hi)
