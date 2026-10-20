#!/usr/bin/env bash
cd "$(dirname "$0")" || exit
echo "Starting VirtualBet AI WebApp..."
open "http://127.0.0.1:8766" 2>/dev/null || xdg-open "http://127.0.0.1:8766" 2>/dev/null &
python3 app.py || python app.py
