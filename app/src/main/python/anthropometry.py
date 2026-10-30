"""Anthropometry engine. WHO child z-scores are withheld unless official LMS tables are supplied."""
import json, math
from datetime import date

MEASURES = {
 "weight_kg":("Weight","kg",{"kg":1,"g":.001}),
 "height_cm":("Standing height","cm",{"cm":1,"m":100}),
 "length_cm":("Recumbent length","cm",{"cm":1,"m":100}),
 "waist_cm":("Waist circumference","cm",{"cm":1,"in":2.54}),
 "hip_cm":("Hip circumference","cm",{"cm":1,"in":2.54}),
 "neck_cm":("Neck circumference","cm",{"cm":1,"in":2.54}),
 "head_cm":("Head circumference","cm",{"cm":1,"in":2.54}),
 "mid_upper_arm_cm":("Mid-upper arm circumference","cm",{"cm":1,"mm":.1}),
 "arm_span_cm":("Arm span","cm",{"cm":1,"m":100}),
 "sitting_height_cm":("Sitting height","cm",{"cm":1,"m":100}),
 "calf_cm":("Calf circumference","cm",{"cm":1,"in":2.54}),
 "skin_fold_triceps_mm":("Triceps skinfold","mm",{"mm":1,"cm":10}),
 "skin_fold_subscapular_mm":("Subscapular skinfold","mm",{"mm":1,"cm":10}),
}
PROFILES = {
 "WHO_GLOBAL":{"label":"WHO global","source":"WHO adult BMI classification","url":"https://www.who.int/news-room/fact-sheets/detail/obesity-and-overweight"},
 "SOUTH_ASIAN":{"label":"South Asian risk-screening","source":"WHO Expert Consultation on Asian BMI action points; IDF ethnicity-specific waist thresholds","url":"https://pubmed.ncbi.nlm.nih.gov/14726171/"},
 "WHO_CHILD":{"label":"WHO Child Growth Standards / 5–19 reference","source":"WHO Child Growth Standards and 5–19 years growth reference","url":"https://www.who.int/tools/child-growth-standards"},
}
def _num(v):
 try:
  x=float(v); return x if math.isfinite(x) and x>0 else None
 except (TypeError,ValueError): return None
def _age(dob,when):
 try:
  a=date.fromisoformat(str(dob)[:10]); b=date.fromisoformat(str(when)[:10]); days=(b-a).days
  return days/365.2425 if days>=0 else None
 except (TypeError,ValueError): return None
def calculate(payload):
 p=dict(payload or {}); when=p.get("measurement_date"); age=_age(p.get("date_of_birth"),when)
 profile=str(p.get("population_profile") or "WHO_GLOBAL").upper()
 if profile not in ("WHO_GLOBAL","SOUTH_ASIAN"): profile="WHO_GLOBAL"
 values={}; errors=[]
 for key,(label,unit,units) in MEASURES.items():
  raw=_num(p.get(key))
  if raw is None: continue
  chosen=str((p.get("units") or {}).get(key) or unit).lower()
  if chosen not in units: errors.append("Unsupported unit for "+label); continue
  values[key]=round(raw*units[chosen],3)
 weight=values.get("weight_kg"); height=values.get("height_cm") or values.get("length_cm")
 waist=values.get("waist_cm"); hip=values.get("hip_cm"); derived={}
 if weight and height:
  bmi=weight/(height/100)**2
  item={"value":round(bmi,2),"unit":"kg/m²","formula":"weight_kg / (height_m × height_m)","source":"Calculated; entered measurements remain authoritative"}
  if age is not None and age>=20:
   item["classification"]=("severe_thinness" if bmi<16 else "moderate_thinness" if bmi<17 else "mild_thinness" if bmi<18.5 else "normal_range" if bmi<25 else "overweight" if bmi<30 else "obesity_class_I" if bmi<35 else "obesity_class_II" if bmi<40 else "obesity_class_III")
   item["reference"]="WHO global adult BMI classification"
   if profile=="SOUTH_ASIAN":
    item["population_action_points_kg_m2"]=[23,27.5]
    item["population_note"]="South Asian action points are risk-screening cut-points, not diagnostic categories."
  else:
   item["classification"]="age_specific_growth_reference_required"
   item["reference"]="WHO BMI-for-age; official age- and sex-specific LMS tables required"
   item["z_score"]=None
   item["note"]="No child percentile or z-score is invented without official WHO LMS data."
  derived["bmi"]=item
  derived["body_surface_area"]={"value":round(math.sqrt(height*weight/3600),3),"unit":"m²","formula":"sqrt(height_cm × weight_kg / 3600)","reference":"Mosteller formula; no diagnostic range"}
 if waist and hip: derived["waist_hip_ratio"]={"value":round(waist/hip,3),"unit":"ratio","formula":"waist_cm / hip_cm","reference":"Interpret with sex- and population-specific guidance; no universal range"}
 if waist and height: derived["waist_height_ratio"]={"value":round(waist/height,3),"unit":"ratio","formula":"waist_cm / height_cm","reference":"Screening index; adult/population context required"}
 if values.get("arm_span_cm") and height: derived["arm_span_height_ratio"]={"value":round(values["arm_span_cm"]/height,3),"unit":"ratio","formula":"arm_span_cm / height_cm","reference":"Descriptive only"}
 growth="WHO_CHILD" if age is not None and age<20 else profile
 return {"measurement_date":when,"age_years_at_measurement":round(age,3) if age is not None else None,"sex_context":p.get("sex","unknown"),"population_profile":profile,"profile":PROFILES[profile],"growth_reference":PROFILES[growth],"measurements":values,"derived":derived,"errors":errors,"provenance":{"measured_values_are_authoritative":True,"derived_values_are_estimates":True,"missing_measurements_remain_blank":True},"clinical_note":"Indices are screening/descriptive measures, not diagnoses. Standardize technique. Pediatric growth assessment requires exact age, sex and official WHO growth reference data."}
def calculate_json(payload): return json.dumps(calculate(payload),allow_nan=False)
