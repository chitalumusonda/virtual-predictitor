# VirtualBet AI - Windows WebApp & Desktop Runner

This folder contains the complete Windows WebApp and desktop runner for **VirtualBet AI**.

## How to Run on Windows

1. Double-click **`Start Windows.bat`**.
2. It will launch the local Python server and automatically open your default web browser (Edge, Chrome, Firefox) to `http://127.0.0.1:8766`.
3. To install as a native desktop application on Windows:
   - In Microsoft Edge or Google Chrome, click the **Install App** icon in the address bar (or Menu -> Apps -> "Install VirtualBet AI").
   - It will open in a standalone native Windows window with its own taskbar icon and desktop shortcut!

## Features Included in the Windows WebApp

1. **Pre-Match Predictions Board**:
   - Check predictions from all AI models before games kick off.
   - Fun graphical mascots, colors, and confidence meters for each model.
   - Consolidated Consensus top pick highlighted in gold (picks the most common prediction).
   - Multi-market forecasts: 1X2, Over/Under 2.5, Over/Under 1.5, BTTS (Both Teams To Score), and Double Chance.

2. **Player Simulator**:
   - Run simulations using each individual model separately, or using the Consolidated Consensus model.
   - Ultra-clear financial summary: **TOTAL BETTED**, **TOTAL PROFIT**, **TOTAL LOSS**, and **NET OUTCOME**.
   - Clear bet-by-bet ledger with amount betted, match score, profit/loss, and running bankroll.

3. **Auto-Save Start Point on Interruption**:
   - When connection is lost or interrupted, the app automatically preserves the current collection state and start point (matchday, round ID, and timestamp) in the local SQLite database.
   - Resume button picks up right where you left off without missing games or duplicating records.

4. **Unified Records**:
   - Pre-match odds and corresponding subsequent match results are stored in the exact same record, linking pre-game lines directly to final scores and outcomes.

5. **Model Rankings Leaderboard**:
   - Models are dynamically ranked from best to worst based on prediction accuracy (% correct), win rates, and ROI.
