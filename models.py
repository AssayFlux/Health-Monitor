from flask_sqlalchemy import SQLAlchemy
from datetime import datetime, date

db = SQLAlchemy()

class FamilyMember(db.Model):
    __tablename__ = "family_member"
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(80), unique=True, nullable=False)
    emoji = db.Column(db.String(8))
    dob = db.Column(db.Date)
    sex = db.Column(db.String(10))
    blood_group = db.Column(db.String(8))
    country = db.Column(db.String(8))   # ISO-3166 alpha-2
    height_cm = db.Column(db.Float)   # static height in cm
    waist_cm = db.Column(db.Float)
    hip_cm = db.Column(db.Float)
    neck_cm = db.Column(db.Float)
    body_fat_pct = db.Column(db.Float)
    muscle_mass_kg = db.Column(db.Float)
    bmr_kcal = db.Column(db.Float)
    tdee_kcal = db.Column(db.Float)
    activity_level = db.Column(db.String(20))
    is_pregnant = db.Column(db.Boolean, default=False)
    is_athlete = db.Column(db.Boolean, default=False)
    is_diabetic = db.Column(db.Boolean, default=False)
    notes = db.Column(db.String(300))
    address = db.Column(db.String(300))
    vitals = db.relationship("Vitals", backref="member", lazy=True, cascade="all, delete-orphan")
    labs = db.relationship("LabResult", backref="member", lazy=True, cascade="all, delete-orphan")

    @property
    def age(self):
        if not self.dob:
            return None
        t = date.today()
        return t.year - self.dob.year - ((t.month, t.day) < (self.dob.month, self.dob.day))

    @property
    def flags(self):
        f = []
        if self.is_pregnant: f.append("pregnant")
        if self.is_athlete: f.append("athlete")
        if self.is_diabetic: f.append("diabetic")
        return f


class Vitals(db.Model):
    __tablename__ = "vitals"
    id = db.Column(db.Integer, primary_key=True)
    member_id = db.Column(db.Integer, db.ForeignKey("family_member.id"), nullable=False)
    timestamp = db.Column(db.DateTime, default=datetime.now, index=True)
    systolic = db.Column(db.Integer)
    diastolic = db.Column(db.Integer)
    pulse = db.Column(db.Integer)
    weight_kg = db.Column(db.Float)
    height_cm = db.Column(db.Float)
    temp_c = db.Column(db.Float)
    spo2 = db.Column(db.Integer)
    arm = db.Column(db.String(8))      # LEFT / RIGHT
    slot = db.Column(db.String(16))    # MORNING / NOON / AFTERNOON / EVENING / NIGHT
    notes = db.Column(db.String(200))

    @property
    def bmi(self):
        h_cm = self.height_cm or (self.member.height_cm if self.member else None)
        if self.weight_kg and h_cm:
            h = h_cm / 100.0
            return round(self.weight_kg / (h * h), 1)
        return None

    @property
    def bmr(self):
        if not (self.weight_kg and self.height_cm):
            return None
        m = self.member
        sex = (m.sex or "M") if m else "M"
        age = (m.age or 30) if m and m.age else 30
        base = 10 * self.weight_kg + 6.25 * self.height_cm - 5 * age
        return round(base + (5 if sex == "M" else -161), 0)


class LabResult(db.Model):
    __tablename__ = "lab_result"
    id = db.Column(db.Integer, primary_key=True)
    member_id = db.Column(db.Integer, db.ForeignKey("family_member.id"), nullable=False)
    date = db.Column(db.DateTime, default=datetime.now, index=True)
    lab_name = db.Column(db.String(120))
    fasting_glucose = db.Column(db.Float)
    pp_glucose = db.Column(db.Float)
    hba1c = db.Column(db.Float)
    total_cholesterol = db.Column(db.Float)
    hdl = db.Column(db.Float)
    ldl = db.Column(db.Float)
    triglycerides = db.Column(db.Float)
    creatinine = db.Column(db.Float)
    urea = db.Column(db.Float)
    uric_acid = db.Column(db.Float)
    alt_sgpt = db.Column(db.Float)
    ast_sgot = db.Column(db.Float)
    bilirubin_total = db.Column(db.Float)
    albumin = db.Column(db.Float)
    sodium = db.Column(db.Float)
    potassium = db.Column(db.Float)
    calcium = db.Column(db.Float)
    hemoglobin = db.Column(db.Float)
    wbc = db.Column(db.Float)
    platelets = db.Column(db.Float)
    vitamin_d = db.Column(db.Float)
    vitamin_b12 = db.Column(db.Float)
    crp = db.Column(db.Float)
    tsh = db.Column(db.Float)
    t3 = db.Column(db.Float)
    t4 = db.Column(db.Float)
    free_t3 = db.Column(db.Float)
    free_t4 = db.Column(db.Float)
    cortisol_am = db.Column(db.Float)
    prolactin = db.Column(db.Float)
    testosterone = db.Column(db.Float)
    estrogen = db.Column(db.Float)
    progesterone = db.Column(db.Float)
    lh = db.Column(db.Float)
    fsh = db.Column(db.Float)
    insulin_fasting = db.Column(db.Float)


    # --- Extended biochemistry ---
    alp = db.Column(db.Float)                 # alkaline phosphatase U/L
    ggt = db.Column(db.Float)                 # gamma GT U/L
    total_protein = db.Column(db.Float)       # g/dL
    globulin = db.Column(db.Float)            # g/dL (auto: total_protein - albumin)
    a_g_ratio = db.Column(db.Float)           # auto: albumin / globulin
    chloride = db.Column(db.Float)            # mmol/L
    phosphate = db.Column(db.Float)           # mg/dL
    magnesium = db.Column(db.Float)           # mg/dL
    iron = db.Column(db.Float)                # ug/dL
    ferritin = db.Column(db.Float)            # ng/mL
    tibc = db.Column(db.Float)                # ug/dL
    transferrin_sat = db.Column(db.Float)     # % auto
    ldl_hdl_ratio = db.Column(db.Float)       # auto
    total_hdl_ratio = db.Column(db.Float)     # auto
    non_hdl = db.Column(db.Float)             # auto: total - HDL
    vldl = db.Column(db.Float)                # auto: trig / 5
    aip = db.Column(db.Float)                 # auto: atherogenic index
    egfr = db.Column(db.Float)                # auto: CKD-EPI
    lpa = db.Column(db.Float)                 # lipoprotein(a) nmol/L
    apob = db.Column(db.Float)                # apolipoprotein B mg/dL
    homocysteine = db.Column(db.Float)        # umol/L
    bilirubin_direct = db.Column(db.Float)     # mg/dL
    bilirubin_indirect = db.Column(db.Float)   # mg/dL (auto: total - direct)
    rbc = db.Column(db.Float)                  # million/uL
    mpv = db.Column(db.Float)                  # fL
    pct = db.Column(db.Float)                  # prothrombin time seconds
    fibrinogen = db.Column(db.Float)           # mg/dL
    tt = db.Column(db.Float)                   # thrombin time seconds
    anti_ccp = db.Column(db.Float)             # U/mL
    ra_factor = db.Column(db.Float)            # IU/mL
    ana = db.Column(db.Float)                  # titre
    vitamin_a = db.Column(db.Float)            # ug/dL
    vitamin_e = db.Column(db.Float)            # mg/L
    vitamin_c = db.Column(db.Float)            # mg/dL
    psa_total = db.Column(db.Float)            # ng/mL
    psa_free = db.Column(db.Float)             # ng/mL
    ca125 = db.Column(db.Float)                # U/mL
    ca19_9 = db.Column(db.Float)               # U/mL
    afp = db.Column(db.Float)                  # ng/mL
    cea = db.Column(db.Float)                  # ng/mL
    beta_hcg = db.Column(db.Float)             # mIU/mL
    dhea_s = db.Column(db.Float)               # ug/dL
    shbg = db.Column(db.Float)                 # nmol/L
    free_testosterone = db.Column(db.Float)    # pg/mL (auto if SHBG)
    amh = db.Column(db.Float)                  # ng/mL
    vitamin_k = db.Column(db.Float)            # ng/mL
    ammonia = db.Column(db.Float)              # umol/L
    ldh = db.Column(db.Float)                  # U/L
    ck_total = db.Column(db.Float)             # U/L
    ck_mb = db.Column(db.Float)                # ng/mL
    lipase = db.Column(db.Float)               # U/L
    amylase = db.Column(db.Float)              # U/L

    # --- Extended haematology ---
    hematocrit = db.Column(db.Float)          # %
    mcv = db.Column(db.Float)                 # fL (auto)
    mch = db.Column(db.Float)                 # pg (auto)
    mchc = db.Column(db.Float)                # g/dL (auto)
    rdw = db.Column(db.Float)                 # %
    neutrophils_pct = db.Column(db.Float)
    lymphocytes_pct = db.Column(db.Float)
    monocytes_pct = db.Column(db.Float)
    eosinophils_pct = db.Column(db.Float)
    basophils_pct = db.Column(db.Float)
    esr = db.Column(db.Float)                 # mm/hr
    pt_inr = db.Column(db.Float)
    aptt = db.Column(db.Float)                # seconds
    d_dimer = db.Column(db.Float)             # ug/mL FEU
    # --- Diabetes extended ---
    c_peptide = db.Column(db.Float)           # ng/mL
    homa_ir = db.Column(db.Float)             # auto
    homa_beta = db.Column(db.Float)           # auto
    fructosamine = db.Column(db.Float)
    # --- Thyroid extended ---
    tpo_ab = db.Column(db.Float)              # anti-TPO IU/mL
    thyroglobulin = db.Column(db.Float)
    # --- Vitamins / minerals extra ---
    folate = db.Column(db.Float)              # ng/mL
    zinc = db.Column(db.Float)                # ug/dL
    copper = db.Column(db.Float)              # ug/dL
    selenium = db.Column(db.Float)            # ug/L
    # --- Cardiac / inflammation ---
    homocysteine = db.Column(db.Float)
    troponin = db.Column(db.Float)            # ng/mL
    bnp = db.Column(db.Float)                 # pg/mL
    esr_done = db.Column(db.Boolean, default=False)


class ClinicalTestResult(db.Model):
    __tablename__ = "clinical_test_result"
    id = db.Column(db.Integer, primary_key=True)
    member_id = db.Column(db.Integer, db.ForeignKey("family_member.id"), nullable=False, index=True)
    date = db.Column(db.DateTime, default=datetime.now, index=True)
    category = db.Column(db.String(30), nullable=False, index=True)
    parameter = db.Column(db.String(80), nullable=False, index=True)
    value_text = db.Column(db.String(300))
    value_numeric = db.Column(db.Float)
    unit = db.Column(db.String(40))
    ref_low = db.Column(db.Float)
    ref_high = db.Column(db.Float)
    reference_text = db.Column(db.String(120))
    status = db.Column(db.String(20), default="unknown")
    notes = db.Column(db.String(300))
    member = db.relationship("FamilyMember", backref=db.backref("clinical_tests", lazy=True, cascade="all, delete-orphan"))


class Medicine(db.Model):
    __tablename__ = "medicine"
    id = db.Column(db.Integer, primary_key=True)
    member_id = db.Column(db.Integer, db.ForeignKey("family_member.id"), nullable=False)
    name = db.Column(db.String(120), nullable=False)
    dose = db.Column(db.String(120))
    started = db.Column(db.Date)
    ended = db.Column(db.Date)
    notes = db.Column(db.String(300))
    created = db.Column(db.DateTime, default=datetime.now)
    member = db.relationship("FamilyMember",
        backref=db.backref("medicines", lazy=True, cascade="all, delete-orphan"))

    @property
    def is_active(self):
        """A medicine is ACTIVE unless it has a valid end date.

        Rules:
        - No ended date at all           -> active (continuing)
        - ended is not a real date       -> active
        - ended is earlier than started  -> active (bad data, treat as continuing)
        - ended is today or in the past  -> discontinued
        - ended is in the future         -> active (planned stop; still being taken)
        """
        from datetime import date as _date

        # 1. No end date -> still continuing
        if self.ended is None:
            return True

        # 2. Not a date object at all -> treat as continuing
        if not isinstance(self.ended, _date):
            return True

        # 3. End date earlier than start -> bad data, treat as continuing
        if isinstance(self.started, _date) and self.ended < self.started:
            return True

        # 4. End date in the future -> still being taken
        if self.ended > _date.today():
            return True

        # 5. Valid end date today or in the past -> discontinued
        return False
