# countries.py — ISO-3166 alpha-2 → display name
COUNTRIES = [
    ("IN","India"), ("US","United States"), ("GB","United Kingdom"),
    ("CA","Canada"), ("AU","Australia"), ("NZ","New Zealand"),
    ("AE","United Arab Emirates"), ("SA","Saudi Arabia"),
    ("SG","Singapore"), ("MY","Malaysia"), ("PK","Pakistan"),
    ("BD","Bangladesh"), ("LK","Sri Lanka"), ("NP","Nepal"),
    ("DE","Germany"), ("FR","France"), ("IT","Italy"), ("ES","Spain"),
    ("NL","Netherlands"), ("SE","Sweden"), ("NO","Norway"),
    ("DK","Denmark"), ("FI","Finland"), ("CH","Switzerland"),
    ("IE","Ireland"), ("PT","Portugal"), ("GR","Greece"),
    ("PL","Poland"), ("CZ","Czechia"), ("AT","Austria"),
    ("BE","Belgium"), ("JP","Japan"), ("KR","South Korea"),
    ("CN","China"), ("HK","Hong Kong"), ("TW","Taiwan"),
    ("TH","Thailand"), ("VN","Vietnam"), ("PH","Philippines"),
    ("ID","Indonesia"), ("BR","Brazil"), ("MX","Mexico"),
    ("AR","Argentina"), ("CL","Chile"), ("ZA","South Africa"),
    ("NG","Nigeria"), ("KE","Kenya"), ("EG","Egypt"),
    ("IL","Israel"), ("TR","Turkey"), ("RU","Russia"),
    ("UA","Ukraine"), ("RO","Romania"), ("HU","Hungary"),
]

def country_name(code):
    for c, n in COUNTRIES:
        if c == code:
            return n
    return code or "-"
