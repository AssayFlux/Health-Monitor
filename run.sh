#!/data/data/com.termux/files/usr/bin/bash
set -e
cd "$(dirname "$0")"
echo "Starting Health Monitor..."
echo "Project: $(pwd)"
echo "Open: http://127.0.0.1:5000"
exec python app.py
