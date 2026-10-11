# Policy sources

These are the simulation policy sources used for the results, with only data-root paths adapted for the public directory. They require externally supplied native exports:

- `inputs/native-evidence/`: final native tool calibration, furnace and machine recipes, material data and repair curves.
- `inputs/copper-boost-evidence/`: retained copper-head calibration.
- `inputs/reference-reports/` and `inputs/legacy-reference-reports/`: the older paired native terrain exports and catalog/material-yield tables, using the labels in `datasets()`.
- `inputs/fresh/`: the two fresh stock/custom terrain pairs, using the labels in `datasets()`.

The full terrain and recipe exports are not bundled in this report. Consult the parent report's reproduction boundary. Running the scripts requires Python and those inputs. Scripts write local results beside the parent report; use a scratch copy.

`basic_processing.py` constructs conservative furnace/macerator main-output yields. `simulate_players.py` defines terrain access and fixed policies. `sequential_players.py` carries budget state across stages. `adaptive_players.py` adds limited local probes and ore-load trips. `summarize_adaptive.py` checks and summarizes the additional sensitivity results. Their abstractions, exclusions and censored searches are disclosed in the parent report.
