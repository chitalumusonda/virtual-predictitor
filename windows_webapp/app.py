import http.server
import json
import os
import random
import sqlite3
import threading
import time
import urllib.request
import webbrowser
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import parse_qs, urlparse

PORT = 8766
ROOT = Path(__file__).resolve().parent
DB_PATH = ROOT / "virtualbet_ai.sqlite3"

def now_utc_str():
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def init_db():
    with get_db() as conn:
        conn.executescript("""
            CREATE TABLE IF NOT EXISTS unified_fixtures (
                fixture_id TEXT PRIMARY KEY,
                round_id TEXT NOT NULL,
                matchday INTEGER NOT NULL,
                season_id TEXT NOT NULL,
                competition_name TEXT NOT NULL,
                fixture TEXT NOT NULL,
                home_team TEXT NOT NULL,
                away_team TEXT NOT NULL,
                kickoff_utc TEXT NOT NULL,
                captured_utc TEXT NOT NULL,
                odds_home REAL NOT NULL,
                odds_draw REAL NOT NULL,
                odds_away REAL NOT NULL,
                odds_over25 REAL NOT NULL,
                odds_under25 REAL NOT NULL,
                odds_over15 REAL NOT NULL,
                odds_under15 REAL NOT NULL,
                odds_btts_yes REAL NOT NULL,
                odds_btts_no REAL NOT NULL,
                status TEXT NOT NULL,
                ft_home INTEGER,
                ft_away INTEGER,
                outcome TEXT,
                total_goals INTEGER,
                btts_result TEXT,
                updated_utc TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS collection_checkpoint (
                id INTEGER PRIMARY KEY,
                connection_status TEXT NOT NULL,
                saved_start_round TEXT NOT NULL,
                saved_start_matchday INTEGER NOT NULL,
                saved_start_utc TEXT NOT NULL,
                last_sync_utc TEXT NOT NULL,
                last_error TEXT
            );

            CREATE TABLE IF NOT EXISTS predictions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fixture_id TEXT NOT NULL,
                round_id TEXT NOT NULL,
                model_id TEXT NOT NULL,
                market TEXT NOT NULL,
                selection TEXT NOT NULL,
                odds REAL NOT NULL,
                confidence REAL NOT NULL,
                is_correct INTEGER,
                predicted_utc TEXT NOT NULL
            );

            CREATE TABLE IF NOT EXISTS paper_bets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                fixture_id TEXT NOT NULL,
                round_id TEXT NOT NULL,
                fixture TEXT NOT NULL,
                model_id TEXT NOT NULL,
                market TEXT NOT NULL,
                selection TEXT NOT NULL,
                odds REAL NOT NULL,
                stake REAL NOT NULL,
                status TEXT NOT NULL,
                ft_score TEXT,
                gross_payout REAL NOT NULL DEFAULT 0.0,
                net_profit REAL NOT NULL DEFAULT 0.0,
                bankroll_after REAL NOT NULL DEFAULT 0.0,
                placed_utc TEXT NOT NULL
            );
        """)

        # Initialize checkpoint if missing
        cur = conn.cursor()
        cur.execute("SELECT COUNT(*) FROM collection_checkpoint")
        if cur.fetchone()[0] == 0:
            now = now_utc_str()
            conn.execute("""
                INSERT INTO collection_checkpoint (id, connection_status, saved_start_round, saved_start_matchday, saved_start_utc, last_sync_utc, last_error)
                VALUES (1, 'CONNECTED', 'RND-24-001', 24, ?, ?, NULL)
            """, (now, now))

        # Seed initial round if empty
        cur.execute("SELECT COUNT(*) FROM unified_fixtures")
        if cur.fetchone()[0] == 0:
            seed_data(conn)

TEAMS = [
    ("London Blue", 1.35), ("Manchester Red", 1.25), ("Liverpool Red", 1.40),
    ("Manchester Blue", 1.50), ("London Gunner", 1.30), ("Newcastle B&W", 1.15),
    ("Tottenham White", 1.18), ("Aston Claret", 1.12), ("Brighton Seagull", 1.05),
    ("West Ham Iron", 1.02), ("Wolverhampton Gold", 0.95), ("Everton Blue", 0.92)
]

MODELS = [
    {"id": "model_consensus", "name": "Consensus Hive", "title": "Majority Pick Ensemble", "emoji": "🎯", "color": "#F59E0B"},
    {"id": "model_professor", "name": "The Professor", "title": "Statistical Value Hunter", "emoji": "🧠", "color": "#8B5CF6"},
    {"id": "model_forest", "name": "Forest Oracle", "title": "Decision Tree Ensemble", "emoji": "🌲", "color": "#10B981"},
    {"id": "model_titan", "name": "Gradient Titan", "title": "Momentum Booster", "emoji": "⚡", "color": "#EA580C"},
    {"id": "model_market", "name": "Market Maven", "title": "Sharp Odds Specialist", "emoji": "📊", "color": "#0284C7"},
    {"id": "model_defense", "name": "Defense Guardian", "title": "Under 2.5 Shield", "emoji": "🛡️", "color": "#64748B"}
]

def generate_round(conn, matchday, round_id, is_final=False):
    now = now_utc_str()
    shuffled = list(TEAMS)
    random.shuffle(shuffled)
    fixtures = []

    for i in range(len(shuffled) // 2):
        home, h_rate = shuffled[i * 2]
        away, a_rate = shuffled[i * 2 + 1]

        total = h_rate + a_rate
        p_home = (h_rate / total) * 0.70
        p_away = (a_rate / total) * 0.65
        p_draw = max(0.18, 1.0 - p_home - p_away)

        odds_h = round(1.06 / p_home, 2)
        odds_d = round(1.06 / p_draw, 2)
        odds_a = round(1.06 / p_away, 2)

        exp_goals = (h_rate * 1.2) + (a_rate * 1.0)
        p_o25 = 0.58 if exp_goals > 2.5 else 0.44
        odds_o25 = round(1.06 / p_o25, 2)
        odds_u25 = round(1.06 / (1.0 - p_o25), 2)

        odds_o15 = 1.28
        odds_u15 = 3.40
        odds_btts_y = 1.80
        odds_btts_n = 1.95

        fix_id = f"FIX-{round_id}-{i+1}"
        ft_h = random.randint(0, 3) if is_final else None
        ft_a = random.randint(0, 2) if is_final else None
        outcome = None
        tot_goals = None
        btts_res = None

        if is_final:
            tot_goals = ft_h + ft_a
            outcome = "1" if ft_h > ft_a else ("2" if ft_h < ft_a else "X")
            btts_res = "Yes" if ft_h > 0 and ft_a > 0 else "No"

        conn.execute("""
            INSERT OR REPLACE INTO unified_fixtures
            (fixture_id, round_id, matchday, season_id, competition_name, fixture, home_team, away_team,
             kickoff_utc, captured_utc, odds_home, odds_draw, odds_away, odds_over25, odds_under25,
             odds_over15, odds_under15, odds_btts_yes, odds_btts_no, status, ft_home, ft_away,
             outcome, total_goals, btts_result, updated_utc)
            VALUES (?, ?, ?, 'season-2026-zambia', 'English Virtual League', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            fix_id, round_id, matchday, f"{home} - {away}", home, away, now, now,
            odds_h, odds_d, odds_a, odds_o25, odds_u25, odds_o15, odds_u15, odds_btts_y, odds_btts_n,
            "FINAL" if is_final else "UPCOMING", ft_h, ft_a, outcome, tot_goals, btts_res, now
        ))

        # Generate model predictions for this fixture
        generate_fixture_predictions(conn, fix_id, round_id, f"{home} - {away}", odds_h, odds_d, odds_a, odds_o25, odds_u25, odds_btts_y, odds_btts_n, is_final, ft_h, ft_a)

def generate_fixture_predictions(conn, fix_id, round_id, fixture_name, odds_h, odds_d, odds_a, odds_o25, odds_u25, odds_by, odds_bn, is_final, ft_h, ft_a):
    now = now_utc_str()
    # Predictions per individual model
    picks = {
        "model_professor": ("O/U 2.5", "Over 2.5" if odds_o25 < 1.90 else "Under 2.5", odds_o25 if odds_o25 < 1.90 else odds_u25, 0.74),
        "model_forest": ("O/U 2.5", "Over 2.5" if odds_o25 < 2.00 else "Under 2.5", odds_o25 if odds_o25 < 2.00 else odds_u25, 0.69),
        "model_titan": ("O/U 2.5", "Over 2.5", odds_o25, 0.82),
        "model_market": ("O/U 2.5", "Over 2.5" if odds_o25 <= odds_u25 else "Under 2.5", min(odds_o25, odds_u25), 0.65),
        "model_defense": ("O/U 2.5", "Under 2.5", odds_u25, 0.72)
    }

    # Consolidated Consensus: pick most common selection
    selections = [v[1] for v in picks.values()]
    consensus_sel = max(set(selections), key=selections.count)
    consensus_odds = odds_o25 if consensus_sel == "Over 2.5" else odds_u25
    picks["model_consensus"] = ("O/U 2.5", consensus_sel, consensus_odds, 0.85)

    for m_id, (mkt, sel, odd, conf) in picks.items():
        is_correct = None
        if is_final and ft_h is not None and ft_a is not None:
            tot = ft_h + ft_a
            is_correct = 1 if (sel == "Over 2.5" and tot >= 3) or (sel == "Under 2.5" and tot < 3) else 0

        conn.execute("""
            INSERT OR REPLACE INTO predictions (fixture_id, round_id, model_id, market, selection, odds, confidence, is_correct, predicted_utc)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (fix_id, round_id, m_id, mkt, sel, odd, conf, is_correct, now))

        if is_final and is_correct is not None:
            # Place simulated bet
            stake = 10.0
            payout = (stake * odd) if is_correct == 1 else 0.0
            net = (payout - stake) if is_correct == 1 else -stake
            conn.execute("""
                INSERT INTO paper_bets (fixture_id, round_id, fixture, model_id, market, selection, odds, stake, status, ft_score, gross_payout, net_profit, bankroll_after, placed_utc)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1000.0, ?)
            """, (fix_id, round_id, fixture_name, m_id, mkt, sel, odd, stake, "WON" if is_correct == 1 else "LOST", f"{ft_h} - {ft_a}", payout, net, now))

def seed_data(conn):
    for d in range(21, 24):
        generate_round(conn, d, f"RND-{d}", is_final=True)
    generate_round(conn, 24, "RND-24", is_final=False)

class ApiHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == "/api/state":
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            with get_db() as conn:
                cur = conn.cursor()
                cur.execute("SELECT * FROM collection_checkpoint WHERE id = 1")
                cp_row = cur.fetchone()
                checkpoint = dict(cp_row) if cp_row else {}

                cur.execute("SELECT * FROM unified_fixtures WHERE status = 'UPCOMING'")
                upcoming = [dict(r) for r in cur.fetchall()]

                cur.execute("SELECT * FROM unified_fixtures ORDER BY rowid DESC LIMIT 50")
                all_fixtures = [dict(r) for r in cur.fetchall()]

                cur.execute("SELECT * FROM predictions ORDER BY id DESC LIMIT 200")
                predictions = [dict(r) for r in cur.fetchall()]

                cur.execute("SELECT * FROM paper_bets ORDER BY id DESC")
                bets = [dict(r) for r in cur.fetchall()]

                # Calculate financials per model
                model_stats = []
                for m in MODELS:
                    m_bets = [b for b in bets if b["model_id"] == m["id"]]
                    tot_betted = sum(b["stake"] for b in m_bets)
                    tot_profit = sum(b["net_profit"] for b in m_bets if b["status"] == "WON")
                    tot_loss = sum(abs(b["net_profit"]) for b in m_bets if b["status"] == "LOST")
                    net = tot_profit - tot_loss
                    wins = len([b for b in m_bets if b["status"] == "WON"])
                    win_rate = (wins / len(m_bets) * 100) if m_bets else 0.0
                    model_stats.append({
                        "model": m,
                        "total_betted": tot_betted,
                        "total_profit": tot_profit,
                        "total_loss": tot_loss,
                        "net_profit": net,
                        "bankroll": 1000.0 + net,
                        "wins": wins,
                        "losses": len(m_bets) - wins,
                        "win_rate": round(win_rate, 1)
                    })

                resp = {
                    "checkpoint": checkpoint,
                    "upcoming": upcoming,
                    "all_fixtures": all_fixtures,
                    "predictions": predictions,
                    "bets": bets[:40],
                    "model_stats": model_stats,
                    "models": MODELS
                }
                self.wfile.write(json.dumps(resp).encode())
            return
        elif parsed.path == "/" or parsed.path == "/index.html":
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            with open(ROOT / "index.html", "rb") as f:
                self.wfile.write(f.read())
            return
        super().do_GET()

    def do_POST(self):
        parsed = urlparse(self.path)
        length = int(self.headers.get("Content-Length", 0))
        body = json.loads(self.rfile.read(length)) if length > 0 else {}

        with get_db() as conn:
            cur = conn.cursor()
            if parsed.path == "/api/advance_round":
                # Finalize upcoming round
                cur.execute("SELECT DISTINCT matchday, round_id FROM unified_fixtures WHERE status = 'UPCOMING'")
                row = cur.fetchone()
                mday = row[0] if row else 24
                # Finalize
                cur.execute("SELECT fixture_id FROM unified_fixtures WHERE status = 'UPCOMING'")
                up_ids = [r[0] for r in cur.fetchall()]
                for fid in up_ids:
                    ft_h = random.randint(0, 3)
                    ft_a = random.randint(0, 2)
                    tot = ft_h + ft_a
                    out = "1" if ft_h > ft_a else ("2" if ft_h < ft_a else "X")
                    btts = "Yes" if ft_h > 0 and ft_a > 0 else "No"
                    now = now_utc_str()
                    conn.execute("""
                        UPDATE unified_fixtures
                        SET status = 'FINAL', ft_home = ?, ft_away = ?, outcome = ?, total_goals = ?, btts_result = ?, updated_utc = ?
                        WHERE fixture_id = ?
                    """, (ft_h, ft_a, out, tot, btts, now, fid))

                    # Settle predictions & bets
                    cur.execute("SELECT id, model_id, selection, odds FROM predictions WHERE fixture_id = ?", (fid,))
                    for pid, mid, sel, odds in cur.fetchall():
                        won = 1 if (sel == "Over 2.5" and tot >= 3) or (sel == "Under 2.5" and tot < 3) else 0
                        conn.execute("UPDATE predictions SET is_correct = ? WHERE id = ?", (won, pid))
                        stake = 10.0
                        payout = (stake * odds) if won == 1 else 0.0
                        net = (payout - stake) if won == 1 else -stake
                        conn.execute("""
                            INSERT INTO paper_bets (fixture_id, round_id, fixture, model_id, market, selection, odds, stake, status, ft_score, gross_payout, net_profit, bankroll_after, placed_utc)
                            VALUES (?, ?, ?, ?, 'O/U 2.5', ?, ?, ?, ?, ?, ?, ?, 1000.0, ?)
                        """, (fid, f"RND-{mday}", "Fixture", mid, sel, odds, stake, "WON" if won == 1 else "LOST", f"{ft_h} - {ft_a}", payout, net, now))

                # Create next upcoming round
                next_m = mday + 1
                generate_round(conn, next_m, f"RND-{next_m}", is_final=False)
                now = now_utc_str()
                conn.execute("""
                    UPDATE collection_checkpoint
                    SET saved_start_matchday = ?, saved_start_round = ?, saved_start_utc = ?, last_sync_utc = ?
                    WHERE id = 1
                """, (next_m, f"RND-{next_m}", now, now))

            elif parsed.path == "/api/simulate_disconnect":
                now = now_utc_str()
                cur.execute("SELECT saved_start_matchday, saved_start_round FROM collection_checkpoint WHERE id = 1")
                row = cur.fetchone()
                mday, r_id = row[0], row[1]
                conn.execute("""
                    UPDATE collection_checkpoint
                    SET connection_status = 'INTERRUPTED', saved_start_utc = ?, last_error = 'Connection lost. Auto-saved recovery start point.'
                    WHERE id = 1
                """, (now,))

            elif parsed.path == "/api/resume_start_point":
                now = now_utc_str()
                conn.execute("""
                    UPDATE collection_checkpoint
                    SET connection_status = 'CONNECTED', last_sync_utc = ?, last_error = NULL
                    WHERE id = 1
                """, (now,))

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.end_headers()
        self.wfile.write(json.dumps({"status": "ok"}).encode())

if __name__ == "__main__":
    init_db()
    server = http.server.ThreadingHTTPServer(("0.0.0.0", PORT), ApiHandler)
    print(f"VirtualBet AI Server started on port {PORT}. Open http://127.0.0.1:{PORT}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
