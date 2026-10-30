"""Pure Python calculation engine called from the native Kotlin UI.
All values are returned as primitive dictionaries for the Kotlin bridge.
Clinical classifications are informational and never a diagnosis.
"""
import math
import ranges
import diagnostics
import units

def _num(v):
    try:
        return float(v) if v not in (None, "", " ") else None
    except (TypeError, ValueError):
        return None

def bmi(weight_kg, height_cm):
    w, h = _num(weight_kg), _num(height_cm)
    return round(w / ((h / 100) ** 2), 1) if w and h and h > 0 else None

def bmr(weight_kg, height_cm, age, sex):
    w, h, a = _num(weight_kg), _num(height_cm), _num(age)
    if not w or not h or not a or h <= 0: return None
    return round(10*w + 6.25*h - 5*a + (5 if str(sex).upper() == "M" else -161), 0)

def calculate_vitals(values):
    data = dict(values or {})
    age, sex = _num(data.get("age")), data.get("sex", "M")
    data["bmi"] = bmi(data.get("weight_kg"), data.get("height_cm"))
    data["bmr_kcal"] = bmr(data.get("weight_kg"), data.get("height_cm"), age, sex)
    result = {}
    for key in ("systolic", "diastolic", "pulse", "spo2", "temp_c", "bmi"):
        value = _num(data.get(key))
        interval = ranges.resolve_range(key, age, sex, None) if value is not None else None
        result[key] = {"value": value, "unit": units.UNITS.get(key, ""),
                       "status": ranges.classify(key, value, age, sex, None) if value is not None else "unknown",
                       "range_low": interval.get("low") if interval else None,
                       "range_high": interval.get("high") if interval else None}
    result["bmi"] = {**result["bmi"], "value": data["bmi"]}
    result["bmr_kcal"] = {"value": data["bmr_kcal"], "unit": "kcal/day"}
    result["note"] = "Reference ranges are informational; use clinician guidance and the reporting laboratory's interval."
    return result

def calculate_labs(values, age=None, sex="M"):
    data = dict(values or {})
    def get(k): return _num(data.get(k))
    trig, total, hdl, ldl = map(get, ("triglycerides","total_cholesterol","hdl","ldl"))
    if data.get("vldl") in (None, "") and trig is not None and ldl is None: data["vldl"] = round(trig/5,2)
    if data.get("non_hdl") in (None, "") and total is not None and hdl is not None: data["non_hdl"] = round(total-hdl,2)
    if data.get("ldl") in (None, "") and total is not None and hdl is not None and trig is not None and trig < 400:
        data["ldl"] = round(total-hdl-trig/5,2)
    if data.get("ldl_hdl_ratio") in (None, "") and ldl is not None and hdl: data["ldl_hdl_ratio"] = round(ldl/hdl,2)
    if data.get("total_hdl_ratio") in (None, "") and total is not None and hdl: data["total_hdl_ratio"] = round(total/hdl,2)
    if data.get("aip") in (None, "") and trig and hdl and trig > 0 and hdl > 0: data["aip"] = round(math.log10(trig/hdl),2)
    tb, direct, protein, albumin = map(get, ("bilirubin_total","bilirubin_direct","total_protein","albumin"))
    if data.get("bilirubin_indirect") in (None, "") and tb is not None and direct is not None: data["bilirubin_indirect"] = round(tb-direct,2)
    if data.get("globulin") in (None, "") and protein is not None and albumin is not None: data["globulin"] = round(protein-albumin,2)
    glob = _num(data.get("globulin"))
    if data.get("a_g_ratio") in (None, "") and albumin is not None and glob: data["a_g_ratio"] = round(albumin/glob,2)
    iron, tibc = get("iron"), get("tibc")
    if data.get("transferrin_sat") in (None, "") and iron is not None and tibc: data["transferrin_sat"] = round(iron/tibc*100,1)
    hb,hct = get("hemoglobin"),get("hematocrit")
    if data.get("mchc") in (None, "") and hb is not None and hct: data["mchc"] = round(hb/hct*100,1)
    fg,ins = get("fasting_glucose"),get("insulin_fasting")
    if data.get("homa_ir") in (None, "") and fg is not None and ins is not None: data["homa_ir"] = round(fg*ins/405,2)
    if data.get("homa_beta") in (None, "") and fg is not None and ins is not None and fg != 63: data["homa_beta"] = round(360*ins/(fg-63),1)
    creat = get("creatinine"); a = _num(age)
    if data.get("egfr") in (None, "") and creat and a:
        female = str(sex).upper() == "F"; k = .7 if female else .9; alpha = -.241 if female else -.302
        data["egfr"] = round(142 * min(creat/k,1)**alpha * max(creat/k,1)**-1.2 * .9938**a * (1.012 if female else 1),1)
    return data

def classify_test(key, value=None, text_value=None):
    return diagnostics.classify_test(key, _num(value), text_value)

def engine_info():
    return {"engine":"Python", "version":"1.0", "catalogue_tests":len(diagnostics.TEST_LOOKUP), "units":len(units.UNITS)}
