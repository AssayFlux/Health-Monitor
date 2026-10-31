"""On-device Python calculation engine. Called only from the native Kotlin UI.
Calculations are informational and are not a substitute for clinical judgement.
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
    w,h=_num(weight_kg),_num(height_cm)
    return round(w/((h/100)**2),1) if w and h and h>0 else None

def bmr(weight_kg,height_cm,age,sex):
    w,h,a=map(_num,(weight_kg,height_cm,age))
    if not w or not h or not a or h<=0:return None
    return round(10*w+6.25*h-5*a+(5 if str(sex).upper()=="M" else -161),0)

def autocalc_labs(data,age=None,sex="M"):
    """Fill derived values without overwriting user-entered results or missing values."""
    data=dict(data or {})
    def g(k):return _num(data.get(k))
    def missing(k):return data.get(k) in (None,""," ")
    trig,total,hdl,ldl=map(g,("triglycerides","total_cholesterol","hdl","ldl"))
    if missing("vldl") and trig is not None and ldl is None:data["vldl"]=round(trig/5,2)
    if missing("non_hdl") and total is not None and hdl is not None:data["non_hdl"]=round(total-hdl,2)
    if missing("ldl") and total is not None and hdl is not None and trig is not None and trig<400:data["ldl"]=round(total-hdl-trig/5,2)
    ldl=g("ldl")
    if missing("ldl_hdl_ratio") and ldl is not None and hdl not in (None,0):data["ldl_hdl_ratio"]=round(ldl/hdl,2)
    if missing("total_hdl_ratio") and total is not None and hdl not in (None,0):data["total_hdl_ratio"]=round(total/hdl,2)
    if missing("aip") and trig and hdl and trig>0 and hdl>0:data["aip"]=round(math.log10(trig/hdl),2)
    tb,direct,protein,albumin=map(g,("bilirubin_total","bilirubin_direct","total_protein","albumin"))
    if missing("bilirubin_indirect") and tb is not None and direct is not None:data["bilirubin_indirect"]=round(tb-direct,2)
    if missing("globulin") and protein is not None and albumin is not None:data["globulin"]=round(protein-albumin,2)
    glob=g("globulin")
    if missing("a_g_ratio") and albumin is not None and glob not in (None,0):data["a_g_ratio"]=round(albumin/glob,2)
    iron,tibc=g("iron"),g("tibc")
    if missing("transferrin_sat") and iron is not None and tibc not in (None,0):data["transferrin_sat"]=round(iron/tibc*100,1)
    hb,hct,rbc=map(g,("hemoglobin","hematocrit","rbc"))
    if missing("mcv") and hct is not None and rbc not in (None,0):data["mcv"]=round(hct/rbc*10,1)
    if missing("mch") and hb is not None and rbc not in (None,0):data["mch"]=round(hb/rbc*10,1)
    if missing("mchc") and hb is not None and hct not in (None,0):data["mchc"]=round(hb/hct*100,1)
    fg,ins=g("fasting_glucose"),g("insulin_fasting")
    if missing("homa_ir") and fg is not None and ins is not None:data["homa_ir"]=round(fg*ins/405,2)
    if missing("homa_beta") and fg is not None and ins is not None and fg!=63:data["homa_beta"]=round(360*ins/(fg-63),1)
    creat,a=g("creatinine"),_num(age)
    if missing("egfr") and creat and a:
        female=str(sex).upper()=="F";k=.7 if female else .9;alpha=-.241 if female else -.302
        ratio=creat/k
        data["egfr"]=round(142*min(ratio,1)**alpha*max(ratio,1)**-1.2*.9938**a*(1.012 if female else 1),1)
    return data

def calculate_labs(values,age=None,sex="M"):
    return autocalc_labs(values,age,sex)

def calculate_vitals(values):
    data=dict(values or {})
    age,sex=_num(data.get("age")),data.get("sex","M")
    data["bmi"]=bmi(data.get("weight_kg"),data.get("height_cm"))
    data["bmr_kcal"]=bmr(data.get("weight_kg"),data.get("height_cm"),age,sex)
    result={}
    for key in ("systolic","diastolic","pulse","spo2","temp_c","bmi"):
        value=_num(data.get(key))
        interval=ranges.resolve_range(key,age,sex,None) if value is not None else None
        result[key]={"value":data["bmi"] if key=="bmi" else value,"unit":units.UNITS.get(key,""),"status":ranges.classify(key,value,age,sex,None) if value is not None else "unknown","range_low":interval.get("low") if interval else None,"range_high":interval.get("high") if interval else None}
    result["bmr_kcal"]={"value":data["bmr_kcal"],"unit":"kcal/day"}
    result["note"]="Informational only. Follow clinician guidance and the reporting laboratory's reference interval."
    return result

def classify_test(key,value=None,text_value=None):
    return diagnostics.classify_test(key,_num(value),text_value)

def engine_info():
    return {"engine":"Python","version":"1.1","catalogue_tests":len(diagnostics.TEST_LOOKUP),"units":len(units.UNITS)}


def _decode_json_mapping(values):
    """Decode the JSON boundary used by Kotlin/Chaquopy and validate its shape."""
    import json

    if isinstance(values, str):
        values = json.loads(values)
    if not isinstance(values, dict):
        raise TypeError("Expected a JSON object (Python dict)")
    return values


def calculate_vitals_json(values):
    import json
    return json.dumps(calculate_vitals(_decode_json_mapping(values)), allow_nan=False)


def calculate_labs_json(values, age=None, sex="M"):
    import json
    return json.dumps(calculate_labs(_decode_json_mapping(values), age, sex), allow_nan=False)


def summarize_results(records):
    """Backend statistics for a sequence of numeric observations."""
    values = [_num(r.get("value")) for r in (records or [])]
    values = [v for v in values if v is not None and math.isfinite(v)]
    if not values:
        return {"count": 0, "mean": None, "minimum": None, "maximum": None,
                "latest": None, "change": None, "trend": "insufficient_data"}
    mean = sum(values) / len(values)
    change = values[-1] - values[-2] if len(values) > 1 else None
    return {"count": len(values), "mean": round(mean, 2),
            "minimum": min(values), "maximum": max(values),
            "latest": values[-1], "change": round(change, 2) if change is not None else None,
            "trend": "increasing" if change is not None and change > 0 else
                     "decreasing" if change is not None and change < 0 else
                     "stable" if change == 0 else "insufficient_data"}

def explain_result(status, parameter="", value=None, unit="", low=None, high=None):
    """Plain-language, non-diagnostic explanation based on supplied interval."""
    label = (parameter or "This result").strip()
    shown = f"{value} {unit}".strip() if value is not None else ""
    interval = ""
    if low is not None or high is not None:
        interval = f" The report reference interval is {low if low is not None else '—'} to {high if high is not None else '—'} {unit}."
    messages = {
        "normal": "Within the reference interval supplied by the reporting laboratory.",
        "low": "Below the lower reference limit supplied by the reporting laboratory.",
        "high": "Above the upper reference limit supplied by the reporting laboratory.",
        "borderline": "Borderline according to the status selected for this report.",
        "abnormal": "Flagged as abnormal by the report or reviewer.",
        "positive": "A positive finding. Its meaning depends on the test and clinical context.",
        "negative": "A negative finding. This does not always exclude disease.",
        "reactive": "Reactive screening result. Follow the test's confirmatory pathway.",
        "non_reactive": "Non-reactive result for this assay.",
        "detected": "The target was detected by this test.",
        "not_detected": "The target was not detected in this sample.",
        "pending": "The result is pending.",
        "critical": "Marked critical. Follow the laboratory's urgent notification process.",
        "unclassified": "No reliable classification is available; check the report and reference interval."
    }
    key = str(status or "unclassified").lower()
    return {"title": label, "value": shown, "status": key,
            "explanation": messages.get(key, messages["unclassified"]) + interval,
            "disclaimer": "This explanation is educational, not a diagnosis. Interpret with the full report and a qualified clinician."}

def dashboard_statistics(records):
    """Counts by explicit status; no clinical inference is performed."""
    counts = {}
    for record in records or []:
        status = str(record.get("status") or "unclassified").lower()
        counts[status] = counts.get(status, 0) + 1
    total = sum(counts.values())
    return {"total": total, "by_status": counts,
            "normal_percent": round(100 * counts.get("normal", 0) / total, 1) if total else 0,
            "flagged": sum(counts.get(k, 0) for k in
                           ("low", "high", "abnormal", "positive", "reactive", "detected", "critical"))}

def summarize_results_json(records):
    import json
    return json.dumps(summarize_results(records), allow_nan=False)

def explain_result_json(status, parameter="", value=None, unit="", low=None, high=None):
    import json
    return json.dumps(explain_result(status, parameter, value, unit, low, high), allow_nan=False)

def dashboard_statistics_json(records):
    import json
    return json.dumps(dashboard_statistics(records), allow_nan=False)
