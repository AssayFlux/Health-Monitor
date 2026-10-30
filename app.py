import os
import json
import csv
import io
import statistics as st
import math
from datetime import datetime

from flask import (
    Flask, render_template, request, redirect, url_for,
    session, flash, jsonify, send_file, Response,
)
import pandas as pd
import plotly.graph_objects as go
import plotly.io as pio

from config import Config
from models import db, FamilyMember, Vitals, LabResult, Medicine, ClinicalTestResult
import auth as A
import ranges as R
from units import UNITS
from countries import COUNTRIES, country_name
from diagnostics import TEST_CATALOG, TEST_CATEGORY_LABELS, TEST_MAP, TEST_LOOKUP, classify_test, default_test_range


app = Flask(__name__)
app.config.from_object(Config)
db.init_app(app)


BIOCHEM_FIELDS = [
    "fasting_glucose", "pp_glucose", "hba1c", "insulin_fasting", "c_peptide",
    "homa_ir", "homa_beta", "fructosamine",
    "total_cholesterol", "hdl", "ldl", "triglycerides",
    "vldl", "non_hdl", "ldl_hdl_ratio", "total_hdl_ratio", "aip", "lpa", "apob",
    "alt_sgpt", "ast_sgot", "alp", "ggt",
    "bilirubin_total", "bilirubin_direct", "bilirubin_indirect",
    "albumin", "globulin", "a_g_ratio", "total_protein",
    "creatinine", "urea", "uric_acid", "egfr",
    "sodium", "potassium", "calcium", "chloride", "phosphate", "magnesium",
    "iron", "ferritin", "tibc", "transferrin_sat",
    "vitamin_d", "vitamin_b12", "folate", "zinc", "copper", "selenium",
    "crp", "homocysteine", "troponin", "bnp",
    "tsh", "t3", "t4", "free_t3", "free_t4", "tpo_ab", "thyroglobulin",
    "cortisol_am", "prolactin", "testosterone", "estrogen",
    "progesterone", "lh", "fsh",
    "rbc",
]

HAEM_FIELDS = [
    "hemoglobin", "hematocrit", "wbc", "platelets",
    "mcv", "mch", "mchc", "rdw",
    "neutrophils_pct", "lymphocytes_pct", "monocytes_pct",
    "eosinophils_pct", "basophils_pct",
    "esr", "pt_inr", "aptt", "d_dimer",
    "pct",
]

LAB_FIELDS = [
    "fasting_glucose",
    "pp_glucose",
    "hba1c",
    "insulin_fasting",
    "c_peptide",
    "fructosamine",
    "homa_ir",
    "homa_beta",
    "total_cholesterol",
    "hdl",
    "ldl",
    "triglycerides",
    "vldl",
    "non_hdl",
    "ldl_hdl_ratio",
    "total_hdl_ratio",
    "aip",
    "lpa",
    "apob",
    "alt_sgpt",
    "ast_sgot",
    "alp",
    "ggt",
    "bilirubin_total",
    "bilirubin_direct",
    "bilirubin_indirect",
    "albumin",
    "globulin",
    "a_g_ratio",
    "total_protein",
    "creatinine",
    "urea",
    "uric_acid",
    "egfr",
    "sodium",
    "potassium",
    "calcium",
    "chloride",
    "phosphate",
    "magnesium",
    "iron",
    "ferritin",
    "tibc",
    "transferrin_sat",
    "tsh",
    "t3",
    "t4",
    "free_t3",
    "free_t4",
    "tpo_ab",
    "thyroglobulin",
    "vitamin_d",
    "vitamin_b12",
    "folate",
    "vitamin_a",
    "vitamin_e",
    "vitamin_c",
    "vitamin_k",
    "zinc",
    "copper",
    "selenium",
    "cortisol_am",
    "prolactin",
    "testosterone",
    "free_testosterone",
    "estrogen",
    "progesterone",
    "lh",
    "fsh",
    "dhea_s",
    "shbg",
    "amh",
    "crp",
    "homocysteine",
    "troponin",
    "bnp",
    "ldh",
    "ck_total",
    "ck_mb",
    "lipase",
    "amylase",
    "ammonia",
    "anti_ccp",
    "ra_factor",
    "ana",
    "psa_total",
    "psa_free",
    "ca125",
    "ca19_9",
    "afp",
    "cea",
    "beta_hcg",
    "hemoglobin",
    "hematocrit",
    "rbc",
    "wbc",
    "platelets",
    "mcv",
    "mch",
    "mchc",
    "rdw",
    "mpv",
    "neutrophils_pct",
    "lymphocytes_pct",
    "monocytes_pct",
    "eosinophils_pct",
    "basophils_pct",
    "esr",
    "pt_inr",
    "pct",
    "aptt",
    "d_dimer",
    "fibrinogen",
    "tt",
]
VITAL_RAW = ["systolic", "diastolic", "pulse", "weight_kg", "height_cm", "spo2", "temp_c"]
VITAL_SHOW = ["systolic", "diastolic", "pulse", "bmi", "weight_kg", "spo2", "temp"]

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
UPLOAD_DIR = os.path.join(BASE_DIR, "static", "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)
LOGO_META = os.path.join(BASE_DIR, "logo.meta")
APP_META = os.path.join(BASE_DIR, "app.meta")
PREFS_FILE = os.path.join(BASE_DIR, "prefs.json")


def _read(path, default=""):
    try:
        with open(path) as f:
            return f.read().strip()
    except Exception:
        return default


def _write(path, val):
    with open(path, "w") as f:
        f.write(val)


def get_logo():
    n = _read(LOGO_META)
    if not n:
        return None
    if not os.path.exists(os.path.join(UPLOAD_DIR, n)):
        return None
    return n


def get_app_meta():
    raw = _read(APP_META)
    name = "Health Monitor"
    tag = "Family Records"
    if raw and "|" in raw:
        parts = raw.split("|", 1)
        name = parts[0] or name
        tag = parts[1] or tag
    return {"app_name": name, "app_tagline": tag}


def load_prefs():
    try:
        with open(PREFS_FILE) as f:
            return json.load(f)
    except Exception:
        return {
            "theme": "light",
            "units": "metric",
            "datefmt": "ymd",
            "user_name": "Admin",
            "user_emoji": "A",
            "country": None,
        }


def save_prefs(p):
    with open(PREFS_FILE, "w") as f:
        json.dump(p, f)


def _f(v):
    try:
        if v in (None, "", " "):
            return None
        return float(v)
    except Exception:
        return None


def _i(v):
    try:
        if v in (None, "", " "):
            return None
        return int(v)
    except Exception:
        return None




@app.route("/edit_lab/<int:mid>/<int:lab_id>", methods=["GET", "POST"])
@A.login_required
def edit_lab(mid, lab_id):
    member = FamilyMember.query.get_or_404(mid)
    lab = LabResult.query.get_or_404(lab_id)
    if lab.member_id != mid:
        return redirect(url_for("dashboard", mid=mid))

    if request.method == "POST":
        for f in LAB_FIELDS:
            setattr(lab, f, _f(request.form.get(f)))
        lab.lab_name = request.form.get("lab_name") or None

        raw = (request.form.get("lab_date") or "").strip()
        if raw:
            try:
                d = datetime.strptime(raw, "%Y-%m-%d").date()
                lab.date = datetime(d.year, d.month, d.day, 9, 0)
            except Exception:
                pass

        data = {f: getattr(lab, f) for f in LAB_FIELDS}
        data = autocalc_labs(data, member)
        valid_cols = {c.name for c in LabResult.__table__.columns}
        for k, v in data.items():
            if k in valid_cols:
                setattr(lab, k, v)

        db.session.commit()
        flash("Lab result updated.", "success")
        return redirect(url_for("dashboard", mid=mid))

    # GET — safe date string
    date_str = lab.date.strftime("%Y-%m-%d") if lab.date else datetime.now().strftime("%Y-%m-%d")
    return render_template("edit_lab.html", member=member, lab=lab, units=UNITS,
                           lab_fields=LAB_FIELDS, friendly=R.FRIENDLY,
                           date_str=date_str)


def autocalc_labs(data, member=None):
    """Fill derived lab values from the primary ones. Skip when a source
    field is missing — never write 0 for missing data."""
    g = data.get
    def safe(v):
        try:
            return float(v) if v not in (None, "", " ") else None
        except Exception:
            return None

    # --- Lipid panel ---
    trig = safe(g("triglycerides"))
    total = safe(g("total_cholesterol"))
    hdl = safe(g("hdl"))
    ldl = safe(g("ldl"))

    # VLDL = triglycerides / 5  (Friedewald)
    if data.get("vldl") is None and trig is not None and ldl is None:
        data["vldl"] = round(trig / 5.0, 2)

    # Non-HDL = total - HDL
    if data.get("non_hdl") is None and total is not None and hdl is not None:
        data["non_hdl"] = round(total - hdl, 2)

    # LDL (Friedewald) = total - HDL - trig/5  when LDL missing and trig < 400
    if data.get("ldl") is None and total is not None and hdl is not None \
            and trig is not None and trig < 400:
        data["ldl"] = round(total - hdl - trig / 5.0, 2)

    # LDL/HDL ratio
    if data.get("ldl_hdl_ratio") is None and ldl is not None and hdl and hdl != 0:
        data["ldl_hdl_ratio"] = round(ldl / hdl, 2)

    # Total/HDL ratio
    if data.get("total_hdl_ratio") is None and total is not None and hdl and hdl != 0:
        data["total_hdl_ratio"] = round(total / hdl, 2)

    # AIP = log10(trig / HDL)
    if data.get("aip") is None and trig is not None and hdl and trig > 0 and hdl > 0:
        import math
        data["aip"] = round(math.log10(trig / hdl), 2)

    # --- Liver panel ---
    tb = safe(g("bilirubin_total"))
    db_val = safe(g("bilirubin_direct"))
    if data.get("bilirubin_indirect") is None and tb is not None and db_val is not None:
        data["bilirubin_indirect"] = round(tb - db_val, 2)

    # --- Liver panel ---
    tp = safe(g("total_protein"))
    alb = safe(g("albumin"))
    if data.get("globulin") is None and tp is not None and alb is not None:
        data["globulin"] = round(tp - alb, 2)
    if data.get("a_g_ratio") is None and alb is not None and data.get("globulin"):
        data["a_g_ratio"] = round(alb / data["globulin"], 2)

    # --- Iron studies ---
    iron = safe(g("iron"))
    tibc = safe(g("tibc"))
    if data.get("transferrin_sat") is None and iron is not None and tibc and tibc != 0:
        data["transferrin_sat"] = round((iron / tibc) * 100, 1)

    # --- Haematology indices ---
    hb = safe(g("hemoglobin"))
    hct = safe(g("hematocrit"))
    rbc = None  # not captured yet
    if data.get("mcv") is None and hct is not None and rbc:
        data["mcv"] = round((hct / rbc) * 10, 1)
    if data.get("mch") is None and hb is not None and rbc:
        data["mch"] = round((hb / rbc) * 10, 1)
    if data.get("mchc") is None and hb is not None and hct:
        data["mchc"] = round((hb / hct) * 100, 1)

    # --- Diabetes ---
    fg = safe(g("fasting_glucose"))
    ins = safe(g("insulin_fasting"))
    if data.get("homa_ir") is None and fg is not None and ins is not None:
        data["homa_ir"] = round((fg * ins) / 405.0, 2)
    if data.get("homa_beta") is None and fg is not None and ins is not None and fg and fg != 63:
        data["homa_beta"] = round((360 * ins) / (fg - 63), 1)

    # --- eGFR (CKD-EPI 2021, sex-specific) ---
    creat = safe(g("creatinine"))
    if data.get("egfr") is None and creat and member and member.age:
        sex = (member.sex or "M").upper()
        k = 0.7 if sex == "F" else 0.9
        a = -0.241 if sex == "F" else -0.302
        ratio = creat / k
        egfr = 142 * (min(ratio, 1) ** a) * (max(ratio, 1) ** -1.200) \
               * (0.9938 ** member.age) * (1.012 if sex == "F" else 1.0)
        data["egfr"] = round(egfr, 1)

    return data


def autocalc_anthropometric(member, weight_kg=None, height_cm=None):
    """Compute anthropometric values from weight, height, age, sex."""
    import math
    h = height_cm if height_cm is not None else member.height_cm
    w = weight_kg
    if not h or h <= 0:
        return {}

    out = {}

    # BSA (Du Bois 1916)
    if w:
        out["bsa"] = round(0.007184 * (h ** 0.725) * (w ** 0.425), 2)

    # WHR
    if member.waist_cm and member.hip_cm and member.hip_cm > 0:
        out["whr"] = round(member.waist_cm / member.hip_cm, 2)
        out["waist_hip_ratio"] = out["whr"]

    # ABSI (A Body Shape Index)
    if member.waist_cm and w and h:
        bmi = w / ((h / 100.0) ** 2)
        out["absi"] = round(member.waist_cm / (bmi ** (2 / 3) * (h ** 0.5)), 4)

    # Ideal Body Weight (Devine 1974)
    if h:
        inches = h / 2.54
        if member.sex == "F":
            out["ideal_weight_kg"] = round(45.5 + 2.3 * (inches - 60), 1) if inches > 60 else 45.5
        else:
            out["ideal_weight_kg"] = round(50.0 + 2.3 * (inches - 60), 1) if inches > 60 else 50.0

    # BMR (Mifflin-St Jeor)
    age = member.age or 30
    if w and h:
        base = 10 * w + 6.25 * h - 5 * age
        out["bmr_kcal"] = round(base + (5 if member.sex == "M" else -161), 0)

    # TDEE
    if "bmr_kcal" in out:
        factors = {"sedentary": 1.2, "light": 1.375, "moderate": 1.55,
                   "active": 1.725, "athlete": 1.9}
        f = factors.get((member.activity_level or "").lower(), 1.375)
        out["tdee_kcal"] = round(out["bmr_kcal"] * f, 0)

    # Body Fat % (US Navy)
    neck = member.neck_cm
    waist = member.waist_cm
    if neck and waist and h:
        if member.sex == "F" and member.hip_cm:
            try:
                bf = 495 / (1.29579 - 0.35004 * math.log10(waist + member.hip_cm - neck)
                            + 0.22100 * math.log10(h)) - 450
                out["body_fat_pct"] = round(bf, 1)
            except Exception:
                pass
        elif member.sex != "F":
            try:
                bf = 495 / (1.0324 - 0.19077 * math.log10(waist - neck)
                            + 0.15456 * math.log10(h)) - 450
                out["body_fat_pct"] = round(bf, 1)
            except Exception:
                pass

    return out


def _has_any_value(rows, field):
    for r in rows:
        if getattr(r, field, None) is not None:
            return True
    return False


# Canonical metric mapping used by dashboard graphs, statistics, trends and print.
# The key is the metric name exposed to the UI; the value is the database/model
# attribute. Derived BMI is calculated from each individual vital reading.
VITAL_METRIC_ATTR = {
    "systolic": "systolic",
    "diastolic": "diastolic",
    "pulse": "pulse",
    "spo2": "spo2",
    "temp_c": "temp_c",
}

WEIGHT_METRICS = {"weight_kg": "weight_kg"}
ANTHRO_METRICS = {
    "bmi": "bmi",
    "bmr_kcal": "bmr_kcal",
    "tdee_kcal": "tdee_kcal",
    "bsa": "bsa",
}

SPECIAL_TEST_CATEGORIES = tuple(TEST_CATALOG.keys())

def anthropometric_series(member, metric, vitals):
    """Longitudinal derived anthropometric series based on each recorded weight.
    This keeps derived values tied to the exact vital/weight timestamp."""
    out = []
    for v in vitals or []:
        if not v.timestamp or v.weight_kg is None:
            continue
        calc = autocalc_anthropometric(member, v.weight_kg, v.height_cm or member.height_cm)
        if metric == "bmi":
            value = v.bmi
        else:
            value = calc.get(metric)
        if value is not None:
            out.append((v.timestamp, value))
    return out

def clinical_metric_series(member, metric, clinical_tests=None):
    rows = clinical_tests if clinical_tests is not None else ClinicalTestResult.query.filter_by(member_id=member.id).order_by(ClinicalTestResult.date).all()
    return [(r.date, r.value_numeric) for r in rows if r.parameter == metric and r.date is not None and r.value_numeric is not None]

def available_clinical_metrics(member_id):
    rows = ClinicalTestResult.query.filter_by(member_id=member_id).all()
    return [key for key in TEST_LOOKUP if any(r.parameter == key and r.value_numeric is not None for r in rows)]

def clinical_test_display_rows(member, rows):
    age, sex, flags = ctx(member)
    out = []
    for r in rows:
        status = r.status or classify_test(r.parameter, r.value_numeric, r.value_text)
        out.append({
            "id": r.id, "date": r.date, "category": r.category,
            "category_label": TEST_CATEGORY_LABELS.get(r.category, r.category.title()),
            "parameter": r.parameter, "label": dict((x[0], x[1]) for x in TEST_CATALOG.get(r.category, [])).get(r.parameter, r.parameter),
            "value": r.value_numeric if r.value_numeric is not None else r.value_text,
            "value_numeric": r.value_numeric, "unit": r.unit or "",
            "reference": r.reference_text or "-", "status": status, "notes": r.notes,
        })
    return out

def metric_range(metric, age=None, sex=None, flags=None, member=None):
    if metric in TEST_LOOKUP:
        row = TEST_LOOKUP[metric][1]
        if row[4] is None and row[5] is None:
            return None
        return {"low": row[4], "high": row[5]}
    # Weight has no universal fixed range. When a member's height is known,
    # derive a transparent weight interval from the same BMI reference range
    # used everywhere else. This makes weight participate consistently in
    # status, statistics, overall monitoring and print output.
    if metric == "weight_kg" and member is not None and member.height_cm:
        bmi_r = R.resolve_range("bmi", age, sex, flags)
        if bmi_r and bmi_r.get("low") is not None and bmi_r.get("high") is not None:
            h_m = float(member.height_cm) / 100.0
            if h_m > 0:
                return {
                    "low": round(bmi_r["low"] * h_m * h_m, 1),
                    "high": round(bmi_r["high"] * h_m * h_m, 1),
                }
    return R.resolve_range(metric, age, sex, flags)


def latest_weight_record(vitals):
    """Return the newest recorded weight, not merely the newest vital row."""
    return next((v for v in reversed(vitals or []) if v.weight_kg is not None), None)

def metric_label(metric):
    if metric in TEST_LOOKUP:
        return TEST_LOOKUP[metric][1][1]
    return R.FRIENDLY.get(metric, metric)

def metric_unit(metric):
    if metric in TEST_LOOKUP:
        return TEST_LOOKUP[metric][1][2] or ""
    return UNITS.get(metric, "")

def metric_classify(metric, value, age=None, sex=None, flags=None, text_value=None, member=None):
    if metric in TEST_LOOKUP:
        return classify_test(metric, value, text_value)
    if metric == "weight_kg" and member is not None:
        r = metric_range(metric, age, sex, flags, member)
        if value is None or not r:
            return "unknown"
        if value < r["low"]:
            return "low"
        if value > r["high"]:
            return "high"
        return "normal"
    return R.classify(metric, value, age, sex, flags)

def metric_series(member, metric, vitals=None, labs=None):
    """Return the canonical chronological (timestamp, value) series for one metric.

    Every graph/statistics/print consumer uses this function so entered values
    cannot silently diverge between screens.
    """
    if vitals is None:
        vitals = (Vitals.query.filter_by(member_id=member.id)
                  .order_by(Vitals.timestamp).all())
    if labs is None:
        labs = (LabResult.query.filter_by(member_id=member.id)
                .order_by(LabResult.date).all())

    if metric in VITAL_METRIC_ATTR:
        attr = VITAL_METRIC_ATTR[metric]
        return [(v.timestamp, getattr(v, attr, None)) for v in vitals
                if v.timestamp is not None and getattr(v, attr, None) is not None]
    if metric in WEIGHT_METRICS:
        return [(v.timestamp, getattr(v, WEIGHT_METRICS[metric], None)) for v in vitals
                if v.timestamp is not None and getattr(v, WEIGHT_METRICS[metric], None) is not None]
    if metric in ANTHRO_METRICS:
        return anthropometric_series(member, metric, vitals)
    if metric in LAB_FIELDS:
        return [(l.date, getattr(l, metric, None)) for l in labs
                if l.date is not None and getattr(l, metric, None) is not None]
    if metric in TEST_LOOKUP:
        return clinical_metric_series(member, metric)
    return []

def metric_value_rows(member, metric, vitals=None, labs=None):
    """Canonical rows for charting, preserving the actual entered timestamps."""
    return [{"timestamp": t, metric: value} for t, value in
            metric_series(member, metric, vitals, labs)]

def build_stat_rows(member, vitals, labs):
    """Build the exact statistic rows shared by dashboard, Statistics and print."""
    age, sex, flags = ctx(member)
    rows = []
    clinical_rows = ClinicalTestResult.query.filter_by(member_id=member.id).order_by(ClinicalTestResult.date).all()

    def add(metric, label=None, unit=None):
        pairs = clinical_metric_series(member, metric, clinical_rows) if metric in TEST_LOOKUP else metric_series(member, metric, vitals, labs)
        vals = [v for _, v in pairs]
        if not vals:
            return
        s = compute_stats(vals)
        r = metric_range(metric, age, sex, flags, member)
        if metric in TEST_LOOKUP:
            tr = TEST_LOOKUP[metric][1]
            rng = tr[6] or "-"
            if unit is None:
                unit = tr[2]
        else:
            rng = ("%.4g - %.4g" % (r["low"], r["high"])) if r else "-"
        normal = high = low = 0
        for v in vals:
            status = metric_classify(metric, v, age, sex, flags, member=member)
            if status == "normal": normal += 1
            elif status == "high": high += 1
            elif status == "low": low += 1
        iqr = round(s["q3"] - s["q1"], 2) if s.get("q1") is not None and s.get("q3") is not None else None
        spread = round(s["max"] - s["min"], 2) if s.get("min") is not None else 0
        latest_status = metric_classify(metric, s["latest"], age, sex, flags, member=member)
        rows.append({
            "field": metric, "label": label or metric_label(metric),
            "unit": unit if unit is not None else metric_unit(metric),
            "range": rng, "latest_status": latest_status, "status": latest_status,
            "latest": s["latest"], "avg": s["avg"], "median": s["median"],
            "mode": s["mode"], "min": s["min"], "max": s["max"], "sd": s["sd"],
            "q1": s["q1"], "q3": s["q3"], "iqr": iqr, "spread": spread,
            "n": s["n"], "normal": normal, "high": high, "low": low,
            "in_range_pct": round(100 * normal / s["n"], 1) if s["n"] else 0,
            "stats": {**s, "normal": normal, "high": high, "low": low, "iqr": iqr},
            "bar_svg": make_inrange_bar(round(100 * normal / s["n"], 1)) if s["n"] else "",
        })

    for metric in VITAL_METRIC_ATTR:
        add(metric)
    for metric in WEIGHT_METRICS:
        add(metric)
    for metric in ANTHRO_METRICS:
        add(metric)
    for metric in LAB_FIELDS:
        add(metric)
    for metric in TEST_LOOKUP:
        pairs = clinical_metric_series(member, metric, clinical_rows)
        if pairs:
            label = next((x[1] for x in TEST_CATALOG[next(iter([c for c, rows in TEST_CATALOG.items() if any(x[0] == metric for x in rows)]))] if x[0] == metric), metric)
            add(metric, label=label)

    # Derived anthropometric values are snapshots, not longitudinal series.
    # BMI is intentionally excluded here because it is already a per-reading
    # series above, so Statistics/Print cannot show two different BMIs.
    latest_w = latest_weight_record(vitals)
    latest_weight = latest_w.weight_kg if latest_w else None
    anthropo = autocalc_anthropometric(member, latest_weight)
    derived_labels = {
        "bmr_kcal": ("BMR", "kcal/day"),
        "tdee_kcal": ("TDEE", "kcal/day"),
        "bsa": ("BSA", "m²"),
        "whr": ("WHR", ""),
        "waist_hip_ratio": ("Waist/Hip ratio", ""),
        "absi": ("ABSI", ""),
        "ideal_weight_kg": ("Ideal weight", "kg"),
        "body_fat_pct": ("Body fat", "%"),
    }
    for field, value in anthropo.items():
        if field in ANTHRO_METRICS or field == "bmi" or value is None:
            continue
        label, unit = derived_labels.get(field, (R.FRIENDLY.get(field, field), UNITS.get(field, "")))
        rr = metric_range(field, age, sex, flags, member)
        stt = metric_classify(field, value, age, sex, flags, member=member) if rr else "unknown"
        n_ok = 1 if stt == "normal" else 0
        n_hi = 1 if stt == "high" else 0
        n_lo = 1 if stt == "low" else 0
        range_text = ("%.4g - %.4g" % (rr["low"], rr["high"])) if rr else "-"
        rows.append({
            "field": field, "label": label, "unit": unit, "range": range_text,
            "latest_status": stt, "status": stt, "latest": value,
            "avg": value, "median": value, "mode": value, "min": value,
            "max": value, "sd": 0, "q1": value, "q3": value, "iqr": 0,
            "spread": 0, "n": 1, "normal": n_ok, "high": n_hi, "low": n_lo,
            "in_range_pct": 100 if rr and stt == "normal" else 0,
            "stats": {"n": 1, "avg": value, "min": value, "max": value,
                      "sd": 0, "latest": value, "median": value,
                      "mode": value, "q1": value, "q3": value,
                      "normal": n_ok, "high": n_hi, "low": n_lo, "iqr": 0},
            "bar_svg": make_inrange_bar(100 if rr and stt == "normal" else 0),
        })
    return rows

def available_lab_metrics(member_id):
    """Return the list of lab-field names that have at least one value
    for this member, respecting the full LAB_FIELDS list."""
    rows = LabResult.query.filter_by(member_id=member_id).all()
    return [f for f in LAB_FIELDS if _has_any_value(rows, f)]


def available_vital_metrics(member_id):
    rows = Vitals.query.filter_by(member_id=member_id).all()
    return [f for f in VITAL_METRIC_ATTR
            if _has_any_value(rows, VITAL_METRIC_ATTR[f])]


def _fmt_date(dt):
    """Format a date/datetime using the preference-selected format."""
    if dt is None:
        return "-"
    try:
        fmt = load_prefs().get("datefmt", "ymd")
    except Exception:
        fmt = "ymd"
    if fmt == "dmy":
        return dt.strftime("%d-%m-%Y")
    return dt.strftime("%Y-%m-%d")


def ctx(member):
    if not member:
        return (None, None, [])
    return (member.age, member.sex, member.flags)


def _clean_vals(series):
    """Return only finite numeric values."""
    out = []
    for v in series:
        if v is None:
            continue
        try:
            f = float(v)
        except Exception:
            continue
        if math.isnan(f) or math.isinf(f):
            continue
        out.append(f)
    return out


def compute_stats(series):
    vals = _clean_vals(series)
    if not vals:
        return {"n":0,"avg":None,"min":None,"max":None,"sd":None,"latest":None,
                "median":None,"mode":None,"q1":None,"q3":None}
    srt = sorted(vals)
    n = len(vals)
    def _pct(p):
        k = (n - 1) * p
        f = int(k); c = min(f + 1, n - 1)
        return srt[f] + (srt[c] - srt[f]) * (k - f)
    try:
        mode_val = st.mode(vals) if n > 1 else vals[0]
    except Exception:
        mode_val = vals[0]
    try:
        sd = round(st.pstdev(vals), 2) if n > 1 else 0
    except Exception:
        sd = 0
    return {
        "n": n,
        "avg": round(st.mean(vals), 2),
        "min": round(min(vals), 2),
        "max": round(max(vals), 2),
        "sd": sd,
        "latest": vals[-1],
        "median": round(st.median(vals), 2),
        "mode": round(mode_val, 2),
        "q1": round(_pct(0.25), 2),
        "q3": round(_pct(0.75), 2),
    }


@app.context_processor
def inject_globals():
    members = []
    if session.get("authenticated"):
        members = FamilyMember.query.order_by(FamilyMember.name).all()
    meta = get_app_meta()
    ss = None
    if request.endpoint == "settings" and request.view_args:
        ss = request.view_args.get("section", "branding")
    return {
        "prefs": load_prefs(),
        "members": members,
        "logo": get_logo(),
        "settings_section": ss, "units": UNITS,
        "countries": COUNTRIES, "country_name": country_name,
        "resolve_range": R.resolve_range,
        "app_name": meta["app_name"],
        "app_tagline": meta["app_tagline"],
        "project_root": BASE_DIR,
        "now_global": datetime.now().strftime("%Y-%m-%d %H:%M"),
    }


@app.route("/login", methods=["GET", "POST"])
def login():
    if request.method == "POST":
        if request.form.get("mode") == "password":
            if A.verify_password(request.form.get("password", "")):
                session["authenticated"] = True
                session.permanent = True
                return redirect(url_for("index"))
            flash("Wrong password.", "error")
    return render_template("login.html", fp_available=A.fingerprint_available())


@app.route("/login/fingerprint", methods=["POST"])
def login_fingerprint():
    ok, msg = A.fingerprint_auth()
    if ok:
        session["authenticated"] = True
        session.permanent = True
        return jsonify({"ok": True})
    return jsonify({"ok": False, "error": msg}), 401


@app.route("/logout")
def logout():
    session.clear()
    return redirect(url_for("login"))


@app.route("/change_password", methods=["POST"])
def change_password():
    if not session.get("authenticated"):
        return redirect(url_for("login"))
    cur = request.form.get("current_password", "")
    new = request.form.get("new_password", "")
    con = request.form.get("confirm_password", "")
    if not A.verify_password(cur):
        flash("Current password is incorrect.", "error")
    elif new != con:
        flash("New passwords do not match.", "error")
    elif len(new) < 4:
        flash("Password too short.", "error")
    else:
        A.change_password(new)
        flash("Password updated.", "success")
    return redirect(url_for("settings", section="security"))


@app.route("/")
@A.login_required
def index():
    members = FamilyMember.query.order_by(FamilyMember.name).all()

    # ---- counts ----
    total_vitals = Vitals.query.count()
    total_labs = LabResult.query.count()
    total_meds = Medicine.query.filter(Medicine.ended.is_(None)).count()

    # ---- per-member summary + alerts ----
    member_cards = []
    alerts = []

    for m in members:
        age, sex, flags = ctx(m)
        last_v = (Vitals.query.filter_by(member_id=m.id)
                  .order_by(Vitals.timestamp.desc()).first())
        last_l = (LabResult.query.filter_by(member_id=m.id)
                  .order_by(LabResult.date.desc()).first())

        # count out-of-range vitals in latest reading
        trouble = 0
        if last_v:
            for k in VITAL_SHOW:
                v = getattr(last_v, k, None)
                stt = R.classify(k, v, age, sex, flags)
                if stt in ("high", "low"):
                    trouble += 1
                    alerts.append({
                        "member": m,
                        "metric": k,
                        "label": R.FRIENDLY.get(k, k),
                        "value": v,
                        "status": stt,
                        "when": last_v.timestamp,
                    })

        # count out-of-range labs in latest lab report
        if last_l:
            for k in LAB_FIELDS:
                v = getattr(last_l, k, None)
                stt = R.classify(k, v, age, sex, flags)
                if stt in ("high", "low"):
                    trouble += 1
                    alerts.append({
                        "member": m,
                        "metric": k,
                        "label": R.FRIENDLY.get(k, k),
                        "value": v,
                        "status": stt,
                        "when": last_l.date,
                    })

        active_meds = (Medicine.query.filter_by(member_id=m.id)
                       .filter(Medicine.ended.is_(None)).count())
        recent_v = (Vitals.query.filter_by(member_id=m.id)
                    .order_by(Vitals.timestamp.desc()).count())

        member_cards.append({
            "m": m,
            "age": age,
            "sex": sex,
            "flags": flags,
            "last_v": last_v,
            "last_l": last_l,
            "trouble": trouble,
            "active_meds": active_meds,
            "vitals_count": recent_v,
        })

    # ---- recent activity feed ----
    recent_vitals = (Vitals.query.order_by(Vitals.timestamp.desc())
                     .limit(6).all())
    recent_labs = (LabResult.query.order_by(LabResult.date.desc())
                   .limit(6).all())

    activity = []
    for v in recent_vitals:
        activity.append({
            "kind": "vitals",
            "member": v.member,
            "when": v.timestamp,
            "summary": "BP {0}/{1} · Pulse {2}".format(
                v.systolic or "-", v.diastolic or "-", v.pulse or "-"),
        })
    for l in recent_labs:
        activity.append({
            "kind": "labs",
            "member": l.member,
            "when": l.date,
            "summary": l.lab_name or "Lab report",
        })
    activity.sort(
        key=lambda a: (a["when"] is not None, a["when"] or datetime.min),
        reverse=True,
    )
    activity = activity[:8]

    # sort alerts worst first
    alerts.sort(
        key=lambda a: (a["status"] != "high",
                       a["when"] or datetime.min),
        reverse=False,
    )
    alerts = alerts[:12]

    return render_template(
        "index.html",
        members=members,
        member_cards=member_cards,
        alerts=alerts,
        activity=activity,
        total_vitals=total_vitals,
        total_labs=total_labs,
        total_meds=total_meds,
    )


@app.route("/add_member", methods=["POST"])
@A.login_required
def add_member():
    name = (request.form.get("name") or "").strip()
    if name and not FamilyMember.query.filter_by(name=name).first():
        dob = None
        if request.form.get("dob"):
            try:
                dob = datetime.strptime(request.form["dob"], "%Y-%m-%d").date()
            except Exception:
                pass
        db.session.add(FamilyMember(
            name=name,
            emoji=(request.form.get("emoji") or None),
            sex=(request.form.get("sex") or None),
            dob=dob,
            blood_group=request.form.get("blood_group") or None,
            country=(request.form.get("country") or load_prefs().get("country")),
            height_cm=None,  # height comes from member profile
            waist_cm=_f(request.form.get("waist_cm")),
            hip_cm=_f(request.form.get("hip_cm")),
            neck_cm=_f(request.form.get("neck_cm")),
            activity_level=request.form.get("activity_level") or None,
            is_pregnant=bool(request.form.get("is_pregnant")),
            is_athlete=bool(request.form.get("is_athlete")),
            is_diabetic=bool(request.form.get("is_diabetic")),
            notes=request.form.get("notes") or None,
            address=request.form.get("address") or None,
        ))
        db.session.commit()
    return redirect(url_for("index"))



@app.route("/edit_member/<int:mid>", methods=["POST"])
@A.login_required
def edit_member(mid):
    m = FamilyMember.query.get_or_404(mid)

    name = (request.form.get("name") or "").strip()
    if name and name != m.name:
        clash = FamilyMember.query.filter_by(name=name).first()
        if clash and clash.id != mid:
            flash("Another member already uses that name.", "error")
            return redirect(url_for("dashboard", mid=mid))
        m.name = name

    m.emoji = (request.form.get("emoji") or "").strip() or None
    m.sex = (request.form.get("sex") or "").strip() or None
    m.blood_group = (request.form.get("blood_group") or "").strip() or None
    m.country = (request.form.get("country") or "").strip() or None
    m.address = (request.form.get("address") or "").strip() or None
    m.notes = (request.form.get("notes") or "").strip() or None

    hv = request.form.get("height_cm")
    m.height_cm = _f(hv) if hv else None

    dob_raw = (request.form.get("dob") or "").strip()
    if dob_raw:
        try:
            m.dob = datetime.strptime(dob_raw, "%Y-%m-%d").date()
        except Exception:
            pass

    db.session.commit()
    flash("Member updated.", "success")
    return redirect(url_for("dashboard", mid=mid))


@app.route("/delete_member/<int:mid>", methods=["POST"])
@A.login_required
def delete_member(mid):
    m = FamilyMember.query.get_or_404(mid)
    db.session.delete(m)
    db.session.commit()
    return redirect(url_for("index"))


@app.route("/member/<int:mid>")
@A.login_required
def dashboard(mid):
    m = FamilyMember.query.get_or_404(mid)
    vitals_q = Vitals.query.filter_by(member_id=mid)
    # ---- date filter applies consistently to both vitals and labs ----
    date_from = (request.args.get("from") or "").strip()
    date_to   = (request.args.get("to")   or "").strip()
    from_dt = to_dt = None
    try:
        if date_from:
            from_dt = datetime.strptime(date_from, "%Y-%m-%d")
        if date_to:
            to_dt = datetime.strptime(date_to, "%Y-%m-%d").replace(hour=23, minute=59, second=59)
    except Exception:
        pass

    vitals_q = vitals_q
    if from_dt:
        vitals_q = vitals_q.filter(Vitals.timestamp >= from_dt)
    if to_dt:
        vitals_q = vitals_q.filter(Vitals.timestamp <= to_dt)
    vitals = vitals_q.order_by(Vitals.timestamp.desc()).limit(50).all()

    labs_q = LabResult.query.filter_by(member_id=mid)
    if from_dt:
        labs_q = labs_q.filter(LabResult.date >= from_dt)
    if to_dt:
        labs_q = labs_q.filter(LabResult.date <= to_dt)
    labs = labs_q.order_by(LabResult.date.desc()).all()
    meds = (Medicine.query.filter_by(member_id=mid)
            .order_by(Medicine.ended.is_(None).desc(),
                      Medicine.started.desc()).all())
    age, sex, flags = ctx(m)

    latest = vitals[0] if vitals else None
    latest_flags = {}
    if latest:
        for k in VITAL_SHOW:
            v = getattr(latest, {"temp": "temp_c", "bmi": "bmi"}.get(k, k), None)
            latest_flags[k] = {
                "value": v,
                "status": R.classify(k, v, age, sex, flags),
                "explain": R.explain(k, v, age, sex, flags),
            }

    lab_rows = []
    for l in labs:
        row = {"id": l.id, "date": l.date, "lab_name": l.lab_name, "cells": []}
        for f in LAB_FIELDS:
            v = getattr(l, f)
            stt = R.classify(f, v, age, sex, flags) if v is not None else "empty"
            row["cells"].append({"field": f, "value": v, "status": stt})
        lab_rows.append(row)

    # Use every stored weight reading for derived calculations; a later BP-only
    # row must not erase the latest available weight.
    all_vitals = (Vitals.query.filter_by(member_id=mid).order_by(Vitals.timestamp).all())
    all_labs = (LabResult.query.filter_by(member_id=mid).order_by(LabResult.date).all())
    latest_weight_v = latest_weight_record(all_vitals)
    anthropo = autocalc_anthropometric(m, latest_weight_v.weight_kg if latest_weight_v else None)
    # Weight is stored on Vitals but is a separate Graphs & Trends domain.
    # Do not derive its availability from VITAL_METRIC_ATTR (which intentionally
    # excludes weight); use the canonical metric_series() instead.
    graph_weight = ["weight_kg"] if metric_series(m, "weight_kg", all_vitals, all_labs) else []
    graph_vitals = available_vital_metrics(mid)
    graph_vitals = [x for x in graph_vitals if x != "weight_kg"]
    graph_anthro = [x for x in ANTHRO_METRICS if metric_series(m, x, all_vitals, all_labs)]
    graph_labs = available_lab_metrics(mid)
    graph_special = available_clinical_metrics(mid)
    clinical_tests = ClinicalTestResult.query.filter_by(member_id=mid).order_by(ClinicalTestResult.date).all()
    # Overall/statistics use the complete history, not the dashboard display limit.
    overall = overall_health(m, all_vitals, all_labs, clinical_tests)
    # ---- group vitals by date → time-of-day (BP/Pulse/SpO2/Temp only) ----
    from collections import OrderedDict
    vg = OrderedDict()
    for v in vitals:
        if not v.timestamp:
            continue
        # Skip rows that only have weight — those belong to Weight history
        has_vitals = any(
            getattr(v, k, None) is not None
            for k in ("systolic", "diastolic", "pulse", "spo2", "temp_c")
        )
        if not has_vitals:
            continue
        day_key = v.timestamp.strftime("%Y-%m-%d")
        if day_key not in vg:
            vg[day_key] = {}
        slot = (v.slot or "").upper()
        if slot not in ("MORNING", "NOON", "AFTERNOON", "EVENING", "NIGHT"):
            slot = "MORNING"  # fallback
        vg[day_key].setdefault(slot, []).append(v)
    vitals_grouped = list(vg.items())
    weights = [v for v in all_vitals if v.weight_kg is not None]

    # Latest-reading cards use the same canonical metric definitions as graphs,
    # statistics and print.
    all_vitals = (Vitals.query.filter_by(member_id=mid).order_by(Vitals.timestamp).all())
    all_labs = (LabResult.query.filter_by(member_id=mid).order_by(LabResult.date).all())
    summary = []
    latest_v_all = all_vitals[-1] if all_vitals else None
    if latest_v_all:
        for metric in ("systolic", "diastolic", "pulse", "bmi", "spo2", "temp_c"):
            value = getattr(latest_v_all, metric, None)
            if value is None:
                continue
            summary.append({"metric": metric, "label": metric_label(metric),
                            "value": value,
                            "status": R.classify(metric, value, age, sex, flags),
                            "range_text": (lambda rr: ("%.4g - %.4g" % (rr["low"], rr["high"])) if rr else "-")(metric_range(metric, age, sex, flags, m))})
    if latest_weight_v:
        wr = metric_range("weight_kg", age, sex, flags, m)
        summary.append({"metric": "weight_kg", "label": metric_label("weight_kg"),
                        "value": latest_weight_v.weight_kg,
                        "status": metric_classify("weight_kg", latest_weight_v.weight_kg, age, sex, flags, member=m) if wr else "unknown",
                        "range_text": ("%.4g - %.4g" % (wr["low"], wr["high"])) if wr else "-"})
    latest_l_all = all_labs[-1] if all_labs else None
    if latest_l_all:
        for metric in LAB_FIELDS:
            value = getattr(latest_l_all, metric, None)
            if value is None:
                continue
            summary.append({"metric": metric, "label": metric_label(metric),
                            "value": value,
                            "status": R.classify(metric, value, age, sex, flags),
                            "range_text": (lambda rr: ("%.4g - %.4g" % (rr["low"], rr["high"])) if rr else "-")(metric_range(metric, age, sex, flags))})

    return render_template(
        "dashboard.html",
        member=m, vitals=vitals, lab_rows=lab_rows, medicines=meds,
        latest=latest, latest_flags=latest_flags,
        age=age, sex=sex, flags=flags,
        friendly=R.FRIENDLY,
        vital_fields=VITAL_SHOW,
        lab_fields=LAB_FIELDS, units=UNITS, weights=weights, classify=R.classify, vitals_grouped=vitals_grouped, graph_vitals=graph_vitals, graph_weight=graph_weight, graph_anthro=graph_anthro, graph_labs=graph_labs, graph_special=graph_special, clinical_tests=clinical_test_display_rows(m, clinical_tests), test_catalog=TEST_CATALOG, test_category_labels=TEST_CATEGORY_LABELS, anthropo=anthropo, overall=overall, summary=summary,
    )


@app.route("/add_vitals/<int:mid>", methods=["POST"])
@A.login_required
def add_vitals(mid):
    # timestamp: build from date + slot
    ts_date_raw = (request.form.get("timestamp_date") or "").strip()
    slot_raw = (request.form.get("slot") or "").strip().upper()
    slot_hours = {"MORNING": 8, "NOON": 12, "AFTERNOON": 15,
                  "EVENING": 18, "NIGHT": 21}
    ts = None
    if ts_date_raw:
        try:
            d = datetime.strptime(ts_date_raw, "%Y-%m-%d").date()
            hr = slot_hours.get(slot_raw, 12)
            ts = datetime(d.year, d.month, d.day, hr, 0)
        except Exception:
            ts = None
    if ts is None:
        # fall back to form's datetime-local OR now
        ts_raw = (request.form.get("timestamp") or "").strip()
        if ts_raw:
            try:
                ts = datetime.fromisoformat(ts_raw)
            except Exception:
                ts = None
    if ts is None:
        ts = datetime.now()

    arm_val = (request.form.get("arm") or "").strip().upper() or None
    if arm_val not in ("LEFT", "RIGHT"):
        arm_val = None
    slot_val = (request.form.get("slot") or "").strip().upper() or None
    if slot_val not in ("MORNING", "NOON", "AFTERNOON", "EVENING", "NIGHT"):
        slot_val = None

    v = Vitals(
        member_id=mid,
        timestamp=ts,
        systolic=_i(request.form.get("systolic")),
        diastolic=_i(request.form.get("diastolic")),
        pulse=_i(request.form.get("pulse")),
        weight_kg=_f(request.form.get("weight_kg")),
        height_cm=None,  # height comes from member profile
        temp_c=_f(request.form.get("temp_c")),
        spo2=_i(request.form.get("spo2")),
        arm=arm_val,
        slot=slot_val,
        notes=request.form.get("notes") or None,
    )
    db.session.add(v)
    db.session.commit()
    return redirect(url_for("dashboard", mid=mid))


@app.route("/add_labs/<int:mid>", methods=["POST"])
@A.login_required
def add_labs(mid):
    member = FamilyMember.query.get_or_404(mid)

    data = {f: _f(request.form.get(f)) for f in LAB_FIELDS}
    data["lab_name"] = request.form.get("lab_name") or None

    # auto-calculate derived values, leaving anything unfilled as None
    data = autocalc_labs(data, member)

    raw = (request.form.get("lab_date") or "").strip()
    dt = None
    if raw:
        try:
            d = datetime.strptime(raw, "%Y-%m-%d").date()
            dt = datetime(d.year, d.month, d.day, 9, 0)
        except Exception:
            dt = None
    if dt is None:
        dt = datetime.now()

    # Build only the kwargs that exist as columns
    valid_cols = {c.name for c in LabResult.__table__.columns}
    safe_data = {k: v for k, v in data.items() if k in valid_cols}

    db.session.add(LabResult(member_id=mid, date=dt, **safe_data))
    db.session.commit()
    return redirect(url_for("dashboard", mid=mid))


@app.route("/add_clinical_tests/<int:mid>", methods=["POST"])
@A.login_required
def add_clinical_tests(mid):
    member = FamilyMember.query.get_or_404(mid)
    raw_date = (request.form.get("clinical_date") or "").strip()
    try:
        d = datetime.strptime(raw_date, "%Y-%m-%d") if raw_date else datetime.now()
    except Exception:
        d = datetime.now()
    for category, rows in TEST_CATALOG.items():
        for key, label, unit, kind, low, high, ref_text in rows:
            raw = (request.form.get(key) or "").strip()
            if not raw:
                continue
            numeric = None
            if kind == "numeric":
                try:
                    numeric = float(raw)
                except Exception:
                    numeric = None
            status = classify_test(key, numeric, raw if numeric is None else None)
            db.session.add(ClinicalTestResult(member_id=member.id, date=d, category=category, parameter=key, value_text=raw, value_numeric=numeric, unit=unit, ref_low=low, ref_high=high, reference_text=ref_text, status=status))
    db.session.commit()
    flash("Additional diagnostic results saved.", "success")
    return redirect(url_for("dashboard", mid=mid))


@app.route("/delete_clinical_test/<int:mid>/<int:test_id>", methods=["POST"])
@A.login_required
def delete_clinical_test(mid, test_id):
    row = ClinicalTestResult.query.get_or_404(test_id)
    if row.member_id == mid:
        db.session.delete(row)
        db.session.commit()
        flash("Diagnostic result deleted.", "success")
    return redirect(url_for("dashboard", mid=mid))


# =================== MEDICINES ===================
@app.route("/add_medicine/<int:mid>", methods=["POST"])
@A.login_required
def add_medicine(mid):
    name = (request.form.get("name") or "").strip()
    if not name:
        flash("Medicine name is required.", "error")
        return redirect(url_for("dashboard", mid=mid))

    def _d(key):
        v = (request.form.get(key) or "").strip()
        if not v:
            return None
        try:
            return datetime.strptime(v, "%Y-%m-%d").date()
        except Exception:
            return None

    started = _d("started")
    ended = _d("ended")

    # If an end date was given but is earlier than the start, ignore it —
    # the medicine stays active (continuing) rather than being marked ended.
    if started and ended and ended < started:
        ended = None
        flash("End date was before start date — medicine marked as continuing.", "error")

    db.session.add(Medicine(
        member_id=mid,
        name=name,
        dose=request.form.get("dose") or None,
        started=started,
        ended=ended,
        notes=request.form.get("notes") or None,
    ))
    db.session.commit()
    flash("Medicine added.", "success")
    return redirect(url_for("dashboard", mid=mid))


@app.route("/end_medicine/<int:mid>/<int:med_id>", methods=["POST"])
@A.login_required
def end_medicine(mid, med_id):
    med = Medicine.query.get_or_404(med_id)
    if med.member_id == mid and med.is_active:
        # accept an optional date from the form; default to today
        raw = (request.form.get("ended") or "").strip()
        ended = None
        if raw:
            try:
                ended = datetime.strptime(raw, "%Y-%m-%d").date()
            except Exception:
                ended = None
        if ended is None:
            ended = datetime.now().date()

        # sanity: must be >= started, otherwise refuse to discontinue
        if med.started and ended < med.started:
            flash("End date cannot be before start date.", "error")
            return redirect(url_for("dashboard", mid=mid))

        med.ended = ended
        db.session.commit()
        flash("Marked as discontinued.", "success")
    return redirect(url_for("dashboard", mid=mid))


@app.route("/delete_medicine/<int:mid>/<int:med_id>", methods=["POST"])
@A.login_required
def delete_medicine(mid, med_id):
    med = Medicine.query.get_or_404(med_id)
    if med.member_id == mid:
        db.session.delete(med)
        db.session.commit()
        flash("Medicine removed.", "success")
    return redirect(url_for("dashboard", mid=mid))


COLORS = {
    "normal": "#16a34a",
    "high": "#dc2626",
    "low": "#2563eb",
    "unknown": "#6b7280",
}


def build_chart(df, metric, member, chart_type="line"):
    """Interactive Plotly chart with healthy band, average line, median,
    and +/-1 SD lines. chart_type: line | bar | scatter | area"""
    age, sex, flags = ctx(member)
    r = metric_range(metric, age, sex, flags, member)
    lo = r["low"] if r else None
    hi = r["high"] if r else None

    vals = [v for v in df[metric].tolist() if v is not None]
    s = compute_stats(vals)

    fig = go.Figure()

    if lo is not None and hi is not None:
        fig.add_hrect(
            y0=lo, y1=hi,
            fillcolor="rgba(13,148,136,0.18)",
            line_width=0,
            annotation_text="Healthy range {0}-{1}".format(lo, hi),
            annotation_position="top left",
        )

    if s["avg"] is not None:
        fig.add_hline(y=s["avg"], line=dict(color="#7c3aed", dash="dash", width=2),
                      annotation_text="Avg {0}".format(s["avg"]),
                      annotation_position="right")
    if s["avg"] is not None and s["sd"]:
        fig.add_hline(y=s["avg"]+s["sd"], line=dict(color="#a78bfa", dash="dot", width=1),
                      annotation_text="+1 SD", annotation_position="right")
        fig.add_hline(y=max(0, s["avg"]-s["sd"]), line=dict(color="#a78bfa", dash="dot", width=1),
                      annotation_text="-1 SD", annotation_position="right")
    if s.get("median") is not None:
        fig.add_hline(y=s["median"], line=dict(color="#0d9488", dash="dashdot", width=1),
                      annotation_text="Median {0}".format(s["median"]),
                      annotation_position="left")

    if chart_type == "bar":
        colors = []
        for v in df[metric]:
            if v is None:
                colors.append("#d1d5db")
                continue
            stt = metric_classify(metric, v, age, sex, flags, member=member)
            colors.append(COLORS.get(stt, "#6b7280"))
        fig.add_trace(go.Bar(
            x=df["timestamp"], y=df[metric],
            name=metric_label(metric),
            marker=dict(color=colors, line=dict(color="white", width=1)),
            hovertemplate="%{x}<br>%{y}<extra></extra>",
        ))
    elif chart_type == "scatter":
        xs, ys, cs, txt = [], [], [], []
        for t, v in zip(df["timestamp"], df[metric]):
            if v is None:
                continue
            stt = metric_classify(metric, v, age, sex, flags, member=member)
            xs.append(t); ys.append(v); cs.append(COLORS.get(stt, "#6b7280"))
            txt.append("{0}: {1}".format(metric_label(metric), v))
        fig.add_trace(go.Scatter(
            x=xs, y=ys, mode="markers",
            marker=dict(size=14, color=cs, line=dict(color="white", width=1)),
            text=txt, hoverinfo="text", showlegend=False,
        ))
    elif chart_type == "area":
        fig.add_trace(go.Scatter(
            x=df["timestamp"], y=df[metric],
            mode="lines+markers",
            name=metric_label(metric),
            connectgaps=False,
            line=dict(color="#1f3a5f", width=2),
            marker=dict(size=8),
            fill="tozeroy",
            fillcolor="rgba(31,58,95,0.12)",
        ))
    else:
        fig.add_trace(go.Scatter(
            x=df["timestamp"], y=df[metric],
            mode="lines+markers",
            name=metric_label(metric),
            connectgaps=False,
            line=dict(color="#1f3a5f", width=2),
            marker=dict(size=9),
        ))

    if chart_type in ("line", "area"):
        xs, ys, cs, txt = [], [], [], []
        for t, v in zip(df["timestamp"], df[metric]):
            if v is None:
                continue
            stt = metric_classify(metric, v, age, sex, flags, member=member)
            xs.append(t); ys.append(v); cs.append(COLORS.get(stt, "#6b7280"))
            txt.append("{0}: {1}".format(metric_label(metric), v))
        fig.add_trace(go.Scatter(
            x=xs, y=ys, mode="markers",
            marker=dict(size=13, color=cs, line=dict(color="white", width=1)),
            text=txt, hoverinfo="text", showlegend=False,
        ))

    fig.update_layout(
        title="{0} - {1}".format(member.name, metric_label(metric)),
        xaxis=dict(
            type="date",
            rangeslider=dict(
                visible=True,
                thickness=0.09,
                bgcolor="#f1f5f9",
                bordercolor="#cbd5e1",
                borderwidth=1,
                yaxis=dict(rangemode="auto"),
            ),
        ),
        yaxis=dict(title=metric_label(metric)),
        hovermode="x unified",
        template="plotly_white",
        height=560,
    )

    return pio.to_html(fig, full_html=False, include_plotlyjs=False,
                       config={"displaylogo": False}), s



@app.route("/chart/<int:mid>/<metric>")
@A.login_required
def chart(mid, metric):
    chart_type = request.args.get("type", "line")
    if chart_type not in ("line", "bar", "scatter", "area"):
        chart_type = "line"
    m = FamilyMember.query.get_or_404(mid)

    metric = {"temp": "temp_c"}.get(metric, metric)
    if metric in VITAL_METRIC_ATTR or metric in WEIGHT_METRICS or metric in ANTHRO_METRICS or metric in LAB_FIELDS or metric in TEST_LOOKUP:
        vit_rows = Vitals.query.filter_by(member_id=mid).order_by(Vitals.timestamp).all()
        lab_rows_chart = LabResult.query.filter_by(member_id=mid).order_by(LabResult.date).all()
        data = metric_value_rows(m, metric, vit_rows, lab_rows_chart)
        if not data:
            return "<p class='hint'>No readings yet.</p>"
        df = pd.DataFrame(data)
    else:
        return "<p class='hint'>Unknown metric.</p>", 404

    html, s = build_chart(df, metric, m, chart_type)

    stat_html = (
        "<div class='chart-stats-strip'>"
        "<div class='css-cell'><span>Latest</span><b>{0}</b></div>"
        "<div class='css-cell'><span>Average</span><b>{1}</b></div>"
        "<div class='css-cell'><span>Median</span><b>{2}</b></div>"
        "<div class='css-cell'><span>Mode</span><b>{3}</b></div>"
        "<div class='css-cell'><span>Min</span><b>{4}</b></div>"
        "<div class='css-cell'><span>Max</span><b>{5}</b></div>"
        "<div class='css-cell'><span>Std Dev</span><b>{6}</b></div>"
        "<div class='css-cell'><span>Readings</span><b>{7}</b></div>"
        "</div>"
    ).format(
        s["latest"] if s["latest"] is not None else "-",
        s["avg"] if s["avg"] is not None else "-",
        s.get("median") if s.get("median") is not None else "-",
        s.get("mode") if s.get("mode") is not None else "-",
        s["min"] if s["min"] is not None else "-",
        s["max"] if s["max"] is not None else "-",
        s["sd"] if s["sd"] is not None else "-",
        s["n"],
    )
    return stat_html + html

# =================== LIGHTWEIGHT SVG CHARTS (print) ===================
def _printify_svg(svg_str):
    """Add inline print-color-adjust to every SVG element so Chrome
    renders colours on paper."""
    # Ensure the svg root already has it (added in make_svg_chart)
    # Add a style block inside the svg
    if "<defs>" in svg_str:
        svg_str = svg_str.replace(
            "</defs>",
            "</defs><style>"
            "*{ -webkit-print-color-adjust: exact !important;"
            "   print-color-adjust: exact !important; }"
            "text{ font-family: Inter, Arial, sans-serif; }"
            "</style>",
            1,
        )
    else:
        # Insert a <style> right after the opening <svg ...>
        svg_str = svg_str.replace(
            ">",
            "><style>*{ -webkit-print-color-adjust: exact !important;"
            " print-color-adjust: exact !important; }</style>",
            1,
        )
    return svg_str


def make_svg_chart(member, df, metric, label, width=600, height=170):
    """Build a chart as a base64 <img>. Chrome ALWAYS prints <img> tags,
    unlike inline SVG which it sometimes drops."""
    import base64 as _b64

    age, sex, flags = ctx(member)
    r = metric_range(metric, age, sex, flags, member)
    lo = r["low"] if r else None
    hi = r["high"] if r else None

    pairs = [(t, v) for t, v in zip(df["timestamp"], df[metric])
             if v is not None and t is not None]
    if not pairs:
        return ""

    vals = [v for _, v in pairs]
    vmin, vmax = min(vals), max(vals)
    y_min = vmin if lo is None else min(vmin, lo)
    y_max = vmax if hi is None else max(vmax, hi)
    if y_max == y_min:
        y_max = y_min + 1
    pad = (y_max - y_min) * 0.15
    y_min -= pad
    y_max += pad
    y_span = y_max - y_min

    pl, pr, pt, pb = 24, 24, 32, 24
    cw = width - pl - pr
    ch = height - pt - pb

    tmin = min(t for t, _ in pairs)
    tmax = max(t for t, _ in pairs)
    tspan = (tmax - tmin).total_seconds() or 1.0

    def x_at(t):
        return pl + ((t - tmin).total_seconds() / tspan) * cw

    def y_at(v):
        return pt + ((y_max - v) / y_span) * ch

    parts = [
        '<svg xmlns="http://www.w3.org/2000/svg" '
        'viewBox="0 0 %d %d" width="%d" height="%d">'
        % (width, height, width, height)
    ]

    # Teal healthy band
    if lo is not None and hi is not None:
        y_lo = y_at(max(lo, y_min))
        y_hi = y_at(min(hi, y_max))
        parts.append(
            '<rect x="%d" y="%.1f" width="%d" height="%.1f" '
            'fill="#b7e3dc"/>'
            % (pl, y_hi, cw, max(0, y_lo - y_hi))
        )

    # Purple dashed average
    avg = sum(vals) / len(vals)
    y_avg = y_at(avg)
    parts.append(
        '<line x1="%d" y1="%.1f" x2="%d" y2="%.1f" '
        'stroke="#7c3aed" stroke-dasharray="4 3" stroke-width="1.6"/>'
        % (pl, y_avg, pl + cw, y_avg)
    )

    # Navy trend line
    if len(pairs) >= 2:
        path_d = "M " + " L ".join(
            "%.1f,%.1f" % (x_at(t), y_at(v)) for t, v in pairs
        )
        parts.append(
            '<path d="%s" fill="none" stroke="#1f3a5f" '
            'stroke-width="2" stroke-linejoin="round"/>' % path_d
        )

    # Coloured status dots
    for t, v in pairs:
        stt = metric_classify(metric, v, age, sex, flags, member=member)
        col = {"normal": "#22c55e", "high": "#ef4444",
               "low": "#7c3aed"}.get(stt, "#6b7280")
        parts.append(
            '<circle cx="%.1f" cy="%.1f" r="4" '
            'fill="%s" stroke="#fff" stroke-width="1"/>'
            % (x_at(t), y_at(v), col)
        )

    parts.append('</svg>')
    svg = "".join(parts)
    b64 = _b64.b64encode(svg.encode("utf-8")).decode("ascii")
    return '<img alt="chart" src="data:image/svg+xml;base64,' + b64 + '" ' \
           'style="display:block;width:100%;height:auto;max-width:100%" />'


def make_inrange_bar(in_range_pct, width=90, height=10):
    """Return a tiny inline SVG bar showing the in-range % colour-coded."""
    pct = max(0.0, min(100.0, float(in_range_pct or 0)))
    filled = width * pct / 100.0
    if pct >= 80:   col = "#22c55e"
    elif pct >= 60: col = "#eab308"
    elif pct >= 40: col = "#f97316"
    else:           col = "#ef4444"
    svg = (
        f'<svg xmlns="http://www.w3.org/2000/svg" '
        f'viewBox="0 0 {width} {height}" width="{width}" height="{height}" '
        f'style="display:inline-block;vertical-align:middle">'
        f'<rect x="0" y="0" width="{width}" height="{height}" rx="{height/2}" '
        f'fill="#f1f5f9" stroke="#e3ddd3" stroke-width="0.5"/>'
        f'<rect x="0" y="0" width="{filled:.1f}" height="{height}" '
        f'rx="{height/2}" fill="{col}"/>'
        f'</svg>'
    )
    return svg


def make_donut_svg(normal, high, low, size=110):
    """Donut chart of Normal / High / Low readings."""
    total = (normal or 0) + (high or 0) + (low or 0)
    if total <= 0:
        return ""
    cx = cy = size / 2
    r = size / 2 - 8
    C = 2 * 3.141592653589793 * r
    def arc(frac, colour, offset):
        dash = C * frac
        return (
            f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="none" '
            f'stroke="{colour}" stroke-width="14" '
            f'stroke-dasharray="{dash:.2f} {C-dash:.2f}" '
            f'stroke-dashoffset="{-offset:.2f}" '
            f'transform="rotate(-90 {cx} {cy})"/>'
        )
    f_norm = normal / total
    f_high = high / total
    f_low = low / total
    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {size} {size}" '
        f'width="{size}" height="{size}" style="display:inline-block">',
        f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="none" stroke="#f1f5f9" stroke-width="14"/>',
        arc(f_norm, "#22c55e", 0),
        arc(f_high, "#ef4444", C * f_norm),
        arc(f_low,  "#7c3aed", C * (f_norm + f_high)),
        f'<text x="{cx}" y="{cy-2}" text-anchor="middle" font-family="Inter,Arial" '
        f'font-size="16" font-weight="700" fill="#2c2f36">{total}</text>',
        f'<text x="{cx}" y="{cy+12}" text-anchor="middle" font-family="Inter,Arial" '
        f'font-size="7.5" fill="#5b6068" letter-spacing=".4">READINGS</text>',
        '</svg>',
    ]
    return "".join(parts)


def make_grade_bar(overall_pct):
    """Horizontal progress bar for the overall grade."""
    pct = max(0.0, min(100.0, float(overall_pct or 0)))
    return (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 260 20" '
        'width="100%" height="20" preserveAspectRatio="none">'
        f'<rect x="0" y="6" width="260" height="8" rx="4" fill="#f1f5f9"/>'
        f'<rect x="0" y="6" width="{260*pct/100:.1f}" height="8" rx="4" fill="#22c55e"/>'
        '</svg>'
    )


def trend_signal(pairs):
    """Describe observed trajectory without pretending it is a medical prediction."""
    if len(pairs) < 2:
        return {"direction": "insufficient data", "slope": None, "projection": None, "n": len(pairs)}
    ordered = sorted(pairs, key=lambda x: x[0])
    vals = [float(v) for _, v in ordered]
    days = [(t - ordered[0][0]).total_seconds() / 86400.0 for t, _ in ordered]
    if len(vals) >= 3 and max(days) > 0:
        mx = sum(days) / len(days); my = sum(vals) / len(vals)
        den = sum((x-mx)**2 for x in days)
        slope = sum((x-mx)*(y-my) for x,y in zip(days, vals)) / den if den else 0.0
        projected = vals[-1] + slope * 30
    else:
        slope = (vals[-1] - vals[0]) / max(days[-1], 1.0)
        projected = vals[-1] + slope * 30
    span = max(abs(vals[-1]), 1e-9)
    relative = abs(vals[-1] - vals[0]) / span
    direction = "stable" if relative < 0.03 else ("rising" if vals[-1] > vals[0] else "falling")
    return {"direction": direction, "slope": round(slope, 4), "projection": round(projected, 2), "n": len(vals)}


def overall_health(member, vitals, labs, clinical_tests=None):
    """Transparent monitoring index, not a diagnosis or medical clearance.

    Scores only measured numeric observations against configured ranges. Missing
    domains are excluded rather than treated as abnormal. Medicines are shown as
    clinical context and data-quality/review flags, never penalised merely because
    a medicine is prescribed.
    """
    clinical_tests = clinical_tests if clinical_tests is not None else ClinicalTestResult.query.filter_by(member_id=member.id).order_by(ClinicalTestResult.date).all()
    rows = build_stat_rows(member, vitals, labs)
    numeric_rows = [r for r in rows if r.get("n", 0) and r.get("range") not in (None, "-")]
    total_normal = sum(r["normal"] for r in numeric_rows)
    total_high = sum(r["high"] for r in numeric_rows)
    total_low = sum(r["low"] for r in numeric_rows)

    # Qualitative diagnostic results are counted separately.
    abnormal_special = [r for r in clinical_tests if (r.status or "") in ("abnormal", "high", "low")]
    special_numeric = [r for r in clinical_tests if r.value_numeric is not None]
    special_normal = sum(1 for r in special_numeric if r.status == "normal")
    special_high = sum(1 for r in special_numeric if r.status == "high")
    special_low = sum(1 for r in special_numeric if r.status == "low")
    total_readings = total_normal + total_high + total_low + special_normal + special_high + special_low
    base_pct = (100 * (total_normal + special_normal) / total_readings) if total_readings else 0

    domain_map = {
        "Vitals": {"systolic","diastolic","pulse","spo2","temp_c"},
        "Anthropometric": {"weight_kg","bmi","bmr_kcal","tdee_kcal","bsa"},
        "Metabolic / glucose": {"fasting_glucose","pp_glucose","hba1c","insulin_fasting","c_peptide","homa_ir","homa_beta","fructosamine"},
        "Lipids": {"total_cholesterol","hdl","ldl","triglycerides","vldl","non_hdl","ldl_hdl_ratio","total_hdl_ratio","aip","lpa","apob"},
        "Liver": {"alt_sgpt","ast_sgot","alp","ggt","bilirubin_total","bilirubin_direct","bilirubin_indirect","albumin","globulin","a_g_ratio","total_protein"},
        "Kidney / electrolytes": {"creatinine","urea","uric_acid","egfr","sodium","potassium","calcium","chloride","phosphate","magnesium"},
        "Haematology": {"hemoglobin","hematocrit","rbc","wbc","platelets","mcv","mch","mchc","rdw","mpv","neutrophils_pct","lymphocytes_pct","monocytes_pct","eosinophils_pct","basophils_pct","esr","pt_inr","pct","aptt","d_dimer","fibrinogen","tt"},
        "Thyroid / hormones": {"tsh","t3","t4","free_t3","free_t4","tpo_ab","thyroglobulin","cortisol_am","prolactin","testosterone","free_testosterone","estrogen","progesterone","lh","fsh","dhea_s","shbg","amh"},
        "Vitamins / inflammation": {"vitamin_d","vitamin_b12","folate","vitamin_a","vitamin_e","vitamin_c","vitamin_k","zinc","copper","selenium","crp","homocysteine"},
        "Cardiac / enzymes": {"troponin","bnp","ldh","ck_total","ck_mb","lipase","amylase","ammonia"},
        "Autoimmune / markers": {"anti_ccp","ra_factor","ana","psa_total","psa_free","ca125","ca19_9","afp","cea","beta_hcg"},
    }
    domains = []
    for name, fields in domain_map.items():
        rr = [r for r in numeric_rows if r["field"] in fields]
        if not rr:
            continue
        n = sum(r["n"] for r in rr); ok = sum(r["normal"] for r in rr); hi = sum(r["high"] for r in rr); lo = sum(r["low"] for r in rr)
        score = round(100 * ok / n, 1) if n else 0
        domains.append({"name": name, "score": score, "n": n, "normal": ok, "high": hi, "low": lo})
    # Additional test categories become their own domains.
    for cat, label in TEST_CATEGORY_LABELS.items():
        rr = [r for r in clinical_tests if r.category == cat]
        if not rr:
            continue
        numeric = [r for r in rr if r.value_numeric is not None]
        ok = sum(1 for r in numeric if r.status == "normal")
        hi = sum(1 for r in numeric if r.status == "high")
        lo = sum(1 for r in numeric if r.status == "low")
        abnormal_text = sum(1 for r in rr if r.value_numeric is None and r.status == "abnormal")
        qualitative_normal = sum(1 for r in rr if r.value_numeric is None and r.status == "normal")
        qualitative_known = qualitative_normal + abnormal_text
        denom = len(numeric) + qualitative_known
        score = round(100 * (ok + qualitative_normal) / denom, 1) if denom else 100.0
        domains.append({"name": label, "score": score, "n": denom, "normal": ok + qualitative_normal, "high": hi, "low": lo, "abnormal": abnormal_text})

    overall_pct = round(sum(d["score"] * d["n"] for d in domains) / sum(d["n"] for d in domains), 1) if domains else round(base_pct, 1)

    # Warnings are factual flags, not diagnoses.
    warnings = []
    for r in numeric_rows:
        if r["latest_status"] in ("high", "low") and r["n"] >= 2:
            abnormal_count = r["high"] + r["low"]
            if abnormal_count / r["n"] >= 0.5:
                warnings.append(f"{r['label']}: {abnormal_count}/{r['n']} recorded values are outside the configured range.")
    for r in abnormal_special:
        warnings.append(f"{r.category.title()} — {r.parameter}: recorded result is {r.status}.")

    # Trend outlook for the most recent abnormal or repeatedly measured metrics.
    trend_items = []
    for metric in list(VITAL_METRIC_ATTR) + list(WEIGHT_METRICS) + list(ANTHRO_METRICS) + list(LAB_FIELDS) + list(TEST_LOOKUP):
        pairs = metric_series(member, metric, vitals, labs)
        if len(pairs) >= 2:
            tr = trend_signal(pairs)
            latest = pairs[-1][1]
            latest_status = metric_classify(metric, latest, *ctx(member), member=member)
            trend_items.append({"metric": metric, "label": metric_label(metric), "status": latest_status, **tr})
    trend_items.sort(key=lambda x: (x["status"] == "normal", -x["n"]))
    trend_items = trend_items[:12]

    medicines = list(getattr(member, "medicines", []) or [])
    active_meds = [m for m in medicines if m.is_active]
    med_warnings = []
    seen = {}
    for med in active_meds:
        key = med.name.strip().lower()
        seen[key] = seen.get(key, 0) + 1
        if not med.dose:
            med_warnings.append(f"Medication {med.name}: dose/frequency is not recorded.")
    for name, count in seen.items():
        if count > 1:
            med_warnings.append(f"Medication list contains {count} active entries named {name}; verify whether they are duplicates or different formulations.")
    warnings.extend(med_warnings)

    advice = [
        "Use the reference interval printed by the reporting laboratory when it differs from the general range shown here.",
        "Review repeated or persistent out-of-range results with a qualified clinician rather than interpreting one isolated result alone.",
    ]
    if not vitals:
        advice.append("No vital-sign history is available; the monitoring summary is therefore incomplete.")
    if not labs and not clinical_tests:
        advice.append("No laboratory or diagnostic-test history is available yet.")
    if active_meds:
        advice.append("Bring the current medication list, dose and start dates to the clinical review; no medication is treated as harmful merely because it is prescribed.")

    coverage_n = sum(d["n"] for d in domains)
    if len(domains) >= 7 and coverage_n >= 30:
        coverage = "Broad coverage"
    elif len(domains) >= 3 and coverage_n >= 10:
        coverage = "Moderate coverage"
    else:
        coverage = "Limited coverage"

    known_qualitative = sum(1 for r in clinical_tests if r.status in ("normal", "abnormal"))
    if total_readings == 0 and known_qualitative == 0:
        grade = "Insufficient data — no monitoring grade"
    elif overall_pct >= 90: grade = "A — Monitoring profile mostly within configured ranges"
    elif overall_pct >= 75: grade = "B — Mostly within configured ranges; review flagged items"
    elif overall_pct >= 60: grade = "C — Mixed results; clinical review of flagged domains is appropriate"
    elif overall_pct >= 40: grade = "D — Multiple out-of-range observations; clinician review is important"
    else: grade = "E — Substantial out-of-range data; prompt clinical review is appropriate"

    return {
        "rows": rows, "domains": domains, "warnings": warnings[:20], "advice": advice,
        "trend_items": trend_items, "medication_count": len(active_meds), "medication_warnings": med_warnings,
        "total_normal": total_normal + special_normal, "total_high": total_high + special_high,
        "total_low": total_low + special_low, "total_readings": total_readings,
        "overall_pct": overall_pct, "grade": grade, "coverage": coverage,
        "grade_bar_svg": make_grade_bar(overall_pct),
        "donut_svg": make_donut_svg(total_normal + special_normal, total_high + special_high, total_low + special_low),
        "note": "Monitoring index based only on recorded values and configured reference ranges; it is not a diagnosis or medical clearance.",
    }

def build_print_charts_svg(member, vitals, labs):
    """Build the printable trend cards from the exact same canonical series used
    by the interactive graphs and Statistics page."""
    charts = []
    age, sex, flags = ctx(member)

    for metric in list(VITAL_METRIC_ATTR) + list(WEIGHT_METRICS) + list(ANTHRO_METRICS) + list(LAB_FIELDS) + list(TEST_LOOKUP):
        pairs = metric_series(member, metric, vitals, labs)
        if not pairs:
            continue
        df = pd.DataFrame([{"timestamp": t, metric: value} for t, value in pairs])
        unit_str = metric_unit(metric)
        title = metric_label(metric) + (f" ({unit_str})" if unit_str else "")
        svg = make_svg_chart(member, df, metric, title, width=520, height=150)
        if not svg:
            continue
        vals = [v for _, v in pairs]
        stats = compute_stats(vals)
        status = metric_classify(metric, stats["latest"], age, sex, flags, member=member)
        r = metric_range(metric, age, sex, flags, member)
        if metric in TEST_LOOKUP:
            tr = TEST_LOOKUP[metric][1]
            rng = tr[6] or "-"
            if unit is None:
                unit = tr[2]
        else:
            rng = ("%.4g - %.4g" % (r["low"], r["high"])) if r else "-"
        charts.append({"id": metric, "title": metric_label(metric),
                       "svg": svg, "stats": stats, "unit": unit_str,
                       "status": status, "range": rng})
    return charts



def build_print_charts(member, vitals, labs):
    """Return a list of {id,title,html,stats} for compact A4-landscape print charts."""
    age, sex, flags = ctx(member)

    vdf = pd.DataFrame([{
        "timestamp": v.timestamp,
        "systolic": v.systolic, "diastolic": v.diastolic,
        "pulse": v.pulse,
        "weight_kg": v.weight_kg, "bmi": v.bmi,
        "spo2": v.spo2, "temp_c": v.temp_c, "temp": v.temp_c,
    } for v in vitals]) if vitals else pd.DataFrame()

    ldf = pd.DataFrame([{
        "timestamp": l.date,
        "fasting_glucose": l.fasting_glucose, "pp_glucose": l.pp_glucose,
        "hba1c": l.hba1c,
        "total_cholesterol": l.total_cholesterol, "hdl": l.hdl, "ldl": l.ldl,
        "triglycerides": l.triglycerides,
        "creatinine": l.creatinine, "urea": l.urea, "uric_acid": l.uric_acid,
        "hemoglobin": l.hemoglobin, "wbc": l.wbc, "platelets": l.platelets,
        "tsh": l.tsh, "t3": l.t3, "t4": l.t4,
        "vitamin_d": l.vitamin_d, "vitamin_b12": l.vitamin_b12,
        "crp": l.crp,
    } for l in labs]) if labs else pd.DataFrame()

    specs = [
        ("bp",      "Blood Pressure",  "v", [("systolic","Systolic","#a94442"),
                                             ("diastolic","Diastolic","#2c5b8f")]),
        ("pulse",   "Pulse",           "v", [("pulse","Pulse (bpm)","#5d7a68")]),
        ("bmi",     "BMI",             "v", [("bmi","BMI","#5d7a68")]),
        ("weight",  "Weight",          "v", [("weight_kg","Weight (kg)","#b98a6f")]),
        ("spo2",    "Oxygen (SpO2)",   "v", [("spo2","SpO2 (%)","#2c5b8f")]),
        ("glucose", "Fasting Sugar",   "l", [("fasting_glucose","mg/dL","#a94442")]),
        ("hba1c",   "HbA1c",           "l", [("hba1c","%","#a94442")]),
        ("chol",    "Cholesterol",     "l", [("total_cholesterol","Total","#a94442"),
                                             ("ldl","LDL","#b98a6f"),
                                             ("hdl","HDL","#5d7a68")]),
        ("trig",    "Triglycerides",   "l", [("triglycerides","mg/dL","#b98a6f")]),
        ("creat",   "Creatinine",      "l", [("creatinine","mg/dL","#5d7a68")]),
        ("hb",      "Hemoglobin",      "l", [("hemoglobin","g/dL","#a94442")]),
        ("tsh",     "TSH",             "l", [("tsh","mIU/L","#5d7a68")]),
    ]

    charts = []
    first = True
    for spec_id, title, src_tag, cols in specs:
        df = vdf if src_tag == "v" else ldf
        if df is None or df.empty:
            continue
        has = any((c in df.columns and df[c].notna().any()) for c, _, _ in cols)
        if not has:
            continue

        fig = go.Figure()
        primary = cols[0][0]
        r = R.resolve_range(primary, age, sex, flags)
        if r:
            fig.add_hrect(y0=r["low"], y1=r["high"],
                          fillcolor="rgba(125,155,136,0.18)", line_width=0,
                          layer="below")

        chart_stats = None
        for col, label, color in cols:
            if col not in df.columns:
                continue
            vals = [v for v in df[col].tolist() if v is not None]
            if not vals:
                continue
            s = compute_stats(vals)
            if chart_stats is None:
                chart_stats = s

            fig.add_trace(go.Scatter(
                x=df["timestamp"], y=df[col],
                mode="lines+markers", name=label,
                connectgaps=False,
                line=dict(color=color, width=1.6),
                marker=dict(size=5),
            ))
            xmin = df["timestamp"].min()
            xmax = df["timestamp"].max()
            fig.add_trace(go.Scatter(
                x=[xmin, xmax], y=[s["avg"], s["avg"]],
                mode="lines",
                line=dict(color=color, dash="dash", width=1),
                opacity=0.55, showlegend=False, hoverinfo="skip",
            ))

        fig.update_layout(
            margin=dict(l=42, r=12, t=34, b=26),
            width=680, height=240,
            xaxis=dict(type="date", tickfont=dict(size=9),
                       showgrid=True, gridcolor="#eef0f2"),
            yaxis=dict(tickfont=dict(size=9),
                       showgrid=True, gridcolor="#eef0f2"),
            plot_bgcolor="#fff", paper_bgcolor="#fff",
            font=dict(family="Inter, sans-serif", size=10, color="#2c2f36"),
            title=dict(text=title, font=dict(size=11.5, color="#1f3a5f"),
                       x=0.02, xanchor="left", y=0.97),
            legend=dict(orientation="h", yanchor="bottom", y=1.0, xanchor="left",
                        x=0, font=dict(size=9), bgcolor="rgba(0,0,0,0)"),
            showlegend=len(cols) > 1,
        )

        html = pio.to_html(
            fig, full_html=False,
            include_plotlyjs=("cdn" if first else False),
            config={"displayModeBar": False, "staticPlot": True},
        )
        first = False
        charts.append({"id": spec_id, "title": title,
                       "html": html, "stats": chart_stats})
    return charts


@app.route("/report/<int:mid>")
@A.login_required
def report(mid):
    m = FamilyMember.query.get_or_404(mid)
    vitals = Vitals.query.filter_by(member_id=mid).order_by(Vitals.timestamp).all()
    labs = LabResult.query.filter_by(member_id=mid).order_by(LabResult.date).all()
    age, sex, flags = ctx(m)

    latest_v = vitals[-1] if vitals else None
    latest_l = labs[-1] if labs else None
    latest_weight_v = latest_weight_record(vitals)

    def _range_text(metric):
        r = metric_range(metric, age, sex, flags, m)
        if not r:
            return ""
        return "{0} - {1}".format(r["low"], r["high"])

    summary = []
    if latest_v:
        for metric in ("systolic","diastolic","pulse","bmi","spo2"):
            val = getattr(latest_v, metric, None)
            if val is None:
                continue
            summary.append({
                "metric": metric,
                "label": metric_label(metric),
                "value": val,
                "status": R.classify(metric, val, age, sex, flags),
                "range_text": _range_text(metric),
            })
    if latest_weight_v:
        wr = metric_range("weight_kg", age, sex, flags, m)
        summary.append({"metric": "weight_kg", "label": metric_label("weight_kg"),
                        "value": latest_weight_v.weight_kg,
                        "status": metric_classify("weight_kg", latest_weight_v.weight_kg, age, sex, flags, member=m) if wr else "unknown",
                        "range_text": ("%.4g - %.4g" % (wr["low"], wr["high"])) if wr else "-"})
    if latest_l:
        for metric in LAB_FIELDS:
            val = getattr(latest_l, metric, None)
            if val is None:
                continue
            summary.append({
                "metric": metric,
                "label": metric_label(metric),
                "value": val,
                "status": R.classify(metric, val, age, sex, flags),
                "range_text": _range_text(metric),
            })

    clinical_tests_raw = ClinicalTestResult.query.filter_by(member_id=mid).order_by(ClinicalTestResult.date).all()
    clinical_tests = clinical_test_display_rows(m, clinical_tests_raw)
    overall = overall_health(m, vitals, labs, clinical_tests_raw)
    charts = build_print_charts_svg(m, vitals, labs)
    medicines = (Medicine.query.filter_by(member_id=mid)
                 .order_by(Medicine.ended.is_(None).desc(),
                           Medicine.started.desc()).all())
    weights = (Vitals.query.filter_by(member_id=mid)
               .filter(Vitals.weight_kg.isnot(None))
               .order_by(Vitals.timestamp).all())

    anthropo = autocalc_anthropometric(m, latest_weight_v.weight_kg if latest_weight_v else None)
    # Statistics shown in print are the exact same rows used by the Statistics page.
    stats_blocks = overall.get("rows", [])

    return render_template(
        "print_report.html",
        member=m, vitals=vitals, labs=labs,
        lab_fields=LAB_FIELDS, vital_fields=VITAL_SHOW,
        age=age, sex=sex, flags=flags,
        friendly=R.FRIENDLY, classify=R.classify,
        summary=summary, charts=charts, medicines=medicines, weights=weights,
        units=UNITS,
        biochem_fields=BIOCHEM_FIELDS, haem_fields=HAEM_FIELDS, anthropo=anthropo, overall=overall, stats_blocks=stats_blocks, clinical_tests=clinical_tests, test_category_labels=TEST_CATEGORY_LABELS,
        now=datetime.now().strftime("%Y-%m-%d %H:%M"),
    )



@app.route("/settings")
@app.route("/settings/<section>")
@A.login_required
def settings(section="branding"):
    return render_template(
        "settings.html",
        prefs=load_prefs(),
        settings_section=section,
        fp_available=A.fingerprint_available(),
    )


@app.route("/settings/prefs", methods=["POST"])
@A.login_required
def save_prefs_route():
    p = load_prefs()
    p["theme"] = request.form.get("theme", "light")
    p["units"] = request.form.get("units", "metric")
    p["datefmt"] = request.form.get("datefmt", "ymd")
    p["country"] = (request.form.get("country") or "").strip() or None
    save_prefs(p)
    flash("Preferences saved.", "success")
    return redirect(url_for("settings", section="preferences"))


@app.route("/settings/profile", methods=["POST"])
@A.login_required
def save_profile():
    p = load_prefs()
    p["user_name"] = (request.form.get("user_name") or "Admin").strip()
    p["user_emoji"] = (request.form.get("user_emoji") or "A").strip()[:4]
    save_prefs(p)
    flash("Profile updated.", "success")
    return redirect(url_for("settings", section="preferences"))


@app.route("/settings/logo", methods=["POST"])
@A.login_required
def upload_logo():
    f = request.files.get("logo")
    if f and f.filename:
        ext = os.path.splitext(f.filename)[1].lower()
        if ext in (".png", ".jpg", ".jpeg", ".svg", ".webp", ".gif"):
            name = "logo" + ext
            f.save(os.path.join(UPLOAD_DIR, name))
            _write(LOGO_META, name)
            flash("Logo updated.", "success")
        else:
            flash("Unsupported file type.", "error")
    return redirect(url_for("settings", section="branding"))


@app.route("/settings/logo/remove", methods=["POST"])
@A.login_required
def remove_logo():
    n = get_logo()
    if n:
        try:
            os.remove(os.path.join(UPLOAD_DIR, n))
        except Exception:
            pass
        try:
            os.remove(LOGO_META)
        except Exception:
            pass
    flash("Logo removed.", "success")
    return redirect(url_for("settings", section="branding"))


@app.route("/settings/branding", methods=["POST"])
@A.login_required
def save_branding():
    name = (request.form.get("app_name") or "Health Monitor").strip()
    tag = (request.form.get("app_tagline") or "Family Records").strip()
    _write(APP_META, "{0}|{1}".format(name, tag))
    flash("Branding saved.", "success")
    return redirect(url_for("settings", section="branding"))


@app.route("/settings/export/vitals.csv")
@A.login_required
def export_vitals_csv():
    out = io.StringIO()
    w = csv.writer(out)
    w.writerow(["member", "timestamp"] + VITAL_RAW + ["notes"])
    for v in Vitals.query.order_by(Vitals.member_id, Vitals.timestamp).all():
        w.writerow([v.member.name, v.timestamp.isoformat()]
                   + [getattr(v, f) for f in VITAL_RAW]
                   + [v.notes or ""])
    return send_file(
        io.BytesIO(out.getvalue().encode()),
        mimetype="text/csv",
        as_attachment=True,
        download_name="vitals.csv",
    )


@app.route("/settings/export/labs.csv")
@A.login_required
def export_labs_csv():
    out = io.StringIO()
    w = csv.writer(out)
    w.writerow(["member", "date", "lab_name"] + LAB_FIELDS)
    for l in LabResult.query.order_by(LabResult.member_id, LabResult.date).all():
        w.writerow([l.member.name, l.date.isoformat(), l.lab_name or ""]
                   + [getattr(l, f) for f in LAB_FIELDS])
    return send_file(
        io.BytesIO(out.getvalue().encode()),
        mimetype="text/csv",
        as_attachment=True,
        download_name="labs.csv",
    )


@app.route("/settings/backup")
@A.login_required
def backup_db():
    stamp = datetime.now().strftime("%Y%m%d_%H%M")
    return send_file("health.db", as_attachment=True,
                     download_name="health_{0}.db".format(stamp))



@app.route("/manifest.json")
def manifest():
    meta = get_app_meta()
    logo = get_logo()
    if logo:
        icon_url = url_for("static", filename="uploads/" + logo)
    else:
        icon_url = url_for("static", filename="icon.svg")
    data = {
        "name": meta["app_name"],
        "short_name": meta["app_name"][:12],
        "description": meta["app_tagline"],
        "start_url": "/",
        "scope": "/",
        "display": "standalone",
        "orientation": "portrait-primary",
        "background_color": "#f5f3ef",
        "theme_color": "#232a35",
        "icons": [
            {"src": icon_url, "sizes": "192x192",
             "type": "image/svg+xml", "purpose": "any maskable"},
            {"src": icon_url, "sizes": "512x512",
             "type": "image/svg+xml", "purpose": "any maskable"},
        ],
    }
    return Response(json.dumps(data), mimetype="application/manifest+json")


@app.route("/sw.js")
def service_worker():
    js = (
        "const CACHE = 'hm-v1';\n"
        "const ASSETS = ['/', '/login', '/static/style.css', '/static/icon.svg'];\n"
        "self.addEventListener('install', function(e) {\n"
        "  e.waitUntil(caches.open(CACHE).then(function(c){ return c.addAll(ASSETS).catch(function(){}); }));\n"
        "  self.skipWaiting();\n"
        "});\n"
        "self.addEventListener('activate', function(e) {\n"
        "  e.waitUntil(caches.keys().then(function(ks){\n"
        "    return Promise.all(ks.filter(function(k){ return k !== CACHE; }).map(function(k){ return caches.delete(k); }));\n"
        "  }));\n"
        "  self.clients.claim();\n"
        "});\n"
        "self.addEventListener('fetch', function(e) {\n"
        "  const req = e.request;\n"
        "  if (req.method !== 'GET') return;\n"
        "  e.respondWith(\n"
        "    fetch(req).then(function(res){\n"
        "      const copy = res.clone();\n"
        "      caches.open(CACHE).then(function(c){ c.put(req, copy); }).catch(function(){});\n"
        "      return res;\n"
        "    }).catch(function(){ return caches.match(req).then(function(r){ return r || caches.match('/'); }); })\n"
        "  );\n"
        "});\n"
    )
    return Response(js, mimetype="application/javascript")


# =================== DATA PREVIEW ===================



@app.route("/visual-key")
@A.login_required
def visual_key():
    return render_template("visual_key.html", now=datetime.now().strftime("%Y-%m-%d %H:%M"))


@app.route("/stats/<int:mid>")
@A.login_required
def stats_page(mid):
    """Full descriptive statistics — vitals + labs + derived anthropometric."""
    m = FamilyMember.query.get_or_404(mid)
    age, sex, flags = ctx(m)

    vrows = Vitals.query.filter_by(member_id=mid).order_by(Vitals.timestamp).all()
    lrows = LabResult.query.filter_by(member_id=mid).order_by(LabResult.date).all()
    crows = ClinicalTestResult.query.filter_by(member_id=mid).order_by(ClinicalTestResult.date).all()

    # Use the same canonical statistics rows used by dashboard and print.
    blocks = build_stat_rows(m, vrows, lrows)
    overall = overall_health(m, vrows, lrows, crows)

    return render_template("stats.html",
                           member=m,
                           blocks=blocks,
                           age=age, sex=sex, flags=flags, overall=overall,
                           clinical_tests=clinical_test_display_rows(m, crows), test_category_labels=TEST_CATEGORY_LABELS,
                           now=datetime.now().strftime("%Y-%m-%d %H:%M"))



@app.route("/delete_lab/<int:mid>/<int:lab_id>", methods=["POST"])
@A.login_required
def delete_lab(mid, lab_id):
    lab = LabResult.query.get_or_404(lab_id)
    if lab.member_id == mid:
        db.session.delete(lab)
        db.session.commit()
        flash("Lab result deleted.", "success")
    return redirect(url_for("dashboard", mid=mid))



@app.route("/delete_vitals/<int:mid>/<int:vid>", methods=["POST"])
@A.login_required
def delete_vitals(mid, vid):
    v = Vitals.query.get_or_404(vid)
    if v.member_id == mid:
        db.session.delete(v)
        db.session.commit()
        flash("Vitals reading deleted.", "success")
    return redirect(url_for("dashboard", mid=mid))


app.jinja_env.filters["datefmt"] = _fmt_date

@app.route("/edit_vitals/<int:mid>/<int:vid>", methods=["GET", "POST"])
@A.login_required
def edit_vitals(mid, vid):
    member = FamilyMember.query.get_or_404(mid)
    v = Vitals.query.get_or_404(vid)
    if v.member_id != mid:
        return redirect(url_for("dashboard", mid=mid))

    if request.method == "POST":
        v.systolic = _i(request.form.get("systolic"))
        v.diastolic = _i(request.form.get("diastolic"))
        v.pulse = _i(request.form.get("pulse"))
        v.spo2 = _i(request.form.get("spo2"))
        v.temp_c = _f(request.form.get("temp_c"))
        v.weight_kg = _f(request.form.get("weight_kg"))
        v.notes = request.form.get("notes") or None

        slot = (request.form.get("slot") or "").strip().upper() or None
        if slot not in ("MORNING", "NOON", "AFTERNOON", "EVENING", "NIGHT"):
            slot = None
        v.slot = slot
        arm = (request.form.get("arm") or "").strip().upper() or None
        if arm not in ("LEFT", "RIGHT"):
            arm = None
        v.arm = arm

        raw = (request.form.get("timestamp_date") or "").strip()
        if raw:
            try:
                d = datetime.strptime(raw, "%Y-%m-%d").date()
                hours = {"MORNING": 8, "NOON": 12, "AFTERNOON": 15,
                         "EVENING": 18, "NIGHT": 21}
                hr = hours.get(slot, 12)
                v.timestamp = datetime(d.year, d.month, d.day, hr, 0)
            except Exception:
                pass

        db.session.commit()
        flash("Vitals reading updated.", "success")
        return redirect(url_for("dashboard", mid=mid))

    return render_template("edit_vitals.html",
                           member=member, v=v,
                           date_str=v.timestamp.strftime("%Y-%m-%d") if v.timestamp else "")



# ---------- Expose clinical helpers to every template ----------
app.jinja_env.globals["classify"]      = R.classify
app.jinja_env.globals["explain"]       = R.explain
app.jinja_env.globals["resolve_range"] = R.resolve_range

with app.app_context():
    db.create_all()


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=False)
