"""Structured observation helpers for Health Monitor.
No diagnosis is inferred from a qualitative finding or inter-arm BP difference.
"""
import json
import math

TIME_OF_DAY = ("morning", "noon", "afternoon", "evening", "night", "overnight", "unknown")
BODY_SIDES = ("left", "right", "bilateral", "midline", "not_applicable", "unknown")
QUALITATIVE_STATUSES = (
    "positive", "negative", "reactive", "non_reactive", "detected",
    "not_detected", "present", "absent", "seen", "not_seen", "trace",
    "normal", "abnormal", "borderline", "indeterminate", "inconclusive",
    "not_determined", "pending", "not_done", "not_applicable",
    "insufficient_sample", "contaminated", "invalid", "pass", "refer",
    "susceptible", "intermediate", "resistant", "weakly_positive",
    "strongly_positive", "equivocal", "other"
)
ABSENT_REASONS = (
    "not_recorded", "not_measured", "not_available", "not_applicable",
    "patient_declined", "unable_to_obtain", "instrument_error",
    "insufficient_sample", "pending", "unknown"
)

def _number(value):
    if value is None or value == "":
        return None
    try:
        number = float(value)
        return number if math.isfinite(number) else None
    except (TypeError, ValueError):
        return None

def blood_pressure_comparison(left_systolic=None, left_diastolic=None,
                              right_systolic=None, right_diastolic=None,
                              repeat_confirmed=False):
    """Compare two arm readings; threshold is a prompt, not a diagnosis."""
    ls, ld, rs, rd = map(_number, (left_systolic, left_diastolic,
                                    right_systolic, right_diastolic))
    if ls is None or rs is None:
        return {"available": False, "systolic_difference": None,
                "diastolic_difference": None, "higher_systolic_arm": None,
                "repeat_prompt": False, "preferred_arm": None,
                "message": "Enter systolic readings for both arms to compare them."}
    sd = round(abs(ls-rs), 1)
    dd = round(abs(ld-rd), 1) if ld is not None and rd is not None else None
    higher = "left" if ls > rs else "right" if rs > ls else "equal"
    prompt = sd > 15
    preferred = higher if prompt and repeat_confirmed else None
    if prompt and not repeat_confirmed:
        message = "Systolic difference is over 15 mmHg. Repeat both-arm measurements; do not treat this single comparison as a diagnosis."
    elif preferred:
        message = "Difference was marked as confirmed on repeat. Use the higher-reading arm for subsequent BP measurements when clinically appropriate."
    else:
        message = "Both-arm comparison recorded. Interpret alongside repeat measurements and clinical guidance."
    return {"available": True, "systolic_difference": sd,
            "diastolic_difference": dd, "higher_systolic_arm": higher,
            "repeat_prompt": prompt, "preferred_arm": preferred,
            "message": message}

def normalize_observation(payload):
    """Validate a flexible quantitative/qualitative observation record."""
    data = dict(payload or {})
    kind = str(data.get("result_kind") or "quantitative").lower()
    if kind not in ("quantitative", "qualitative", "graded", "report", "boolean"):
        raise ValueError("Unsupported result_kind")
    period = str(data.get("time_of_day") or "unknown").lower()
    if period not in TIME_OF_DAY:
        raise ValueError("Invalid time_of_day")
    side = str(data.get("body_side") or "not_applicable").lower()
    if side not in BODY_SIDES:
        raise ValueError("Invalid body_side")
    result = {
        "parameter": str(data.get("parameter") or "").strip(),
        "result_kind": kind,
        "recorded_date": data.get("recorded_date") or None,
        "recorded_time": data.get("recorded_time") or None,
        "time_of_day": period,
        "body_side": side,
        "value_numeric": None,
        "value_text": None,
        "qualitative_status": None,
        "unit": str(data.get("unit") or "").strip(),
        "absent_reason": None,
        "source": str(data.get("source") or "user_recorded"),
        "method": data.get("method") or None,
        "notes": str(data.get("notes") or "").strip()
    }
    if kind == "quantitative":
        number = _number(data.get("value_numeric"))
        if number is None:
            reason = str(data.get("absent_reason") or "not_recorded")
            if reason not in ABSENT_REASONS:
                raise ValueError("Invalid absent_reason")
            result["absent_reason"] = reason
        else:
            result["value_numeric"] = number
    else:
        status = str(data.get("qualitative_status") or "").lower()
        text = str(data.get("value_text") or "").strip()
        if status and status not in QUALITATIVE_STATUSES:
            raise ValueError("Invalid qualitative_status")
        result["qualitative_status"] = status or None
        result["value_text"] = text or None
        if not status and not text:
            reason = str(data.get("absent_reason") or "not_determined")
            if reason not in ABSENT_REASONS:
                raise ValueError("Invalid absent_reason")
            result["absent_reason"] = reason
    return result

def normalize_observation_json(payload):
    return json.dumps(normalize_observation(payload), allow_nan=False)

def blood_pressure_comparison_json(payload):
    data = dict(payload or {})
    return json.dumps(blood_pressure_comparison(
        data.get("left_systolic"), data.get("left_diastolic"),
        data.get("right_systolic"), data.get("right_diastolic"),
        bool(data.get("repeat_confirmed", False))), allow_nan=False)
