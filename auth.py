import os
import subprocess
import json
from functools import wraps
from flask import session, redirect, url_for
from werkzeug.security import generate_password_hash, check_password_hash

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

DEFAULT_PASSWORD_HASH = generate_password_hash("family123")
PASSWORD_FILE = os.path.join(BASE_DIR, "master.hash")


def load_password_hash():
    try:
        with open(PASSWORD_FILE) as f:
            return f.read().strip()
    except FileNotFoundError:
        save_password_hash(DEFAULT_PASSWORD_HASH)
        return DEFAULT_PASSWORD_HASH


def save_password_hash(h):
    with open(PASSWORD_FILE, "w") as f:
        f.write(h)


def verify_password(pw):
    return check_password_hash(load_password_hash(), pw)


def change_password(new_pw):
    save_password_hash(generate_password_hash(new_pw))


def fingerprint_available():
    """Check if termux-fingerprint exists on PATH without needing the `which` binary."""
    import shutil
    if shutil.which("termux-fingerprint"):
        return True
    # Termux sometimes puts it here even if PATH lookup fails
    for p in ("/data/data/com.termux/files/usr/bin/termux-fingerprint",
              "/system/bin/termux-fingerprint"):
        if os.path.exists(p):
            return True
    return False


def fingerprint_auth():
    try:
        r = subprocess.run(["termux-fingerprint"],
                           capture_output=True, text=True, timeout=60)
    except FileNotFoundError:
        return False, "Termux:API app is not installed."
    except subprocess.TimeoutExpired:
        return False, "Fingerprint timed out."

    raw = (r.stdout or "").strip()
    if not raw:
        return False, "No response from sensor."

    try:
        data = json.loads(raw)
    except Exception:
        return False, "Unexpected sensor reply."

    result = data.get("auth_result", "")
    if result == "AUTH_RESULT_SUCCESS":
        return True, "ok"
    if result == "AUTH_RESULT_FAILED":
        return False, "Fingerprint did not match."
    return False, "Sensor error: {0}".format(result or "unknown")


def login_required(f):
    @wraps(f)
    def w(*a, **kw):
        if not session.get("authenticated"):
            return redirect(url_for("login"))
        return f(*a, **kw)
    return w
