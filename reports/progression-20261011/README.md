# Preliminary player-policy progression evidence, 2026-10-11

The model supports a substantial increase in manual resource-acquisition time for several Steam/LV budgets. It does not establish a complete survival playthrough or a reliable total number of hours from a new world to the first drill. Late-stage searches were censored by finite sampled terrain and a 24-hour per-phase limit.

## What was actually run

- 1,080 fixed-policy resource-budget mining attempts: nine budgets, five paired world seeds, eight policy starts, stock grid search, stock branch search and mod branch search. These are five independent seeds, not 1,080 independent worlds.
- 120 continuous mining campaigns carrying excavations, exposed deposits, incidental raw ores, surplus materials and repair demand forward. Tool sequence: trained flint, trained copper, bronze, steel, then an assumed unlocked alumite head for the late subtotal.
- Two fresh stock/mod pairs of naturally generated Overworld terrain (24 by 24 chunks), Nether and Moon terrain (12 by 12 chunks each). Older paired snapshots are 18 by 18 chunks.
- Native tool harvest gates and speeds, actual retained-head leveling, 1,000-stone repair curves, initialized furnace/machine/crafting recipes, and 779 stage quests. Two fresh pairs of native Ore Drilling Plant tests.
- Measured original binary SHA256: `4b4e285f2e4b8a90edbb57aa33328fd64911a36a4ed451beb246ad67aefa6272`. The current public release SHA256 is `86e33e9080c7b4c2ca4a1b49931ba42661123a747a5395a9acf558fdd3323046`; the namespace-only replacement is documented in [build verification](https://github.com/finalpantasy/classic-mining-for-gtnh/blob/main/BUILD_VERIFICATION.md). These campaign measurements used the original binary and have not been relabeled as fresh tests of the renamed binary. Only isolated QA and audit scripts changed.

## Independent resource-budget comparisons

Hours include modeled digging, searching, travel and handling/repair time. Ore processing is excluded here. Medians only include completed attempts; unequal completion rates mean these medians must not be interpreted as an unbiased whole-world estimate. Paired deltas include only policy starts completing both versions.

| Budget | Stock grid hours (completed) | Mod branch hours (completed) | Paired median extra hours (pairs) |
|---|---:|---:|---:|
| Flint bootstrap | 1.78 (32/40) | 1.21 (32/40) | -0.9 (32) |
| Steam starter factory | 2.26 (32/40) | 2.85 (40/40) | 0.83 (32) |
| Bricked blast furnace minerals | 4.01 (24/40) | 6.64 (25/40) | 1.74 (19) |
| Early LV working stock | 4.26 (32/40) | 11.3 (26/40) | 7.03 (26) |
| LV blast furnace mica route | 4.64 (8/40) | 16.99 (3/40) | Not estimated (0) |
| LV to MV common-metal reserve | 3.5 (32/40) | 5.62 (37/40) | 1.68 (32) |
| MV to HV factory expansion | Not estimated (0/40) | Not estimated (0/40) | Not estimated (0) |
| Steam iron stress test | 7.47 (32/40) | 10.99 (32/40) | 1.22 (24) |
| EV drill common-metal subtotal | Not estimated (0/40) | Not estimated (0/40) | Not estimated (0) |

Budgets (assumptions, in refined metal units unless noted):

- Flint bootstrap: 32 iron, 12 copper, 4 tin. Steam starter: 76 iron, 93 copper, 31 tin. BBF minerals: 32 gypsum and 16 calcite ore-block equivalents.
- Early LV working stock: 726 iron, 294 copper, 244 tin, 8 gold. LV EBF route: 32 mica ore-block equivalents, 64 nickel, 128 copper.
- LV to MV reserve: 256 iron, 256 copper, 128 tin, 24 gold. MV to HV expansion: 1,024 iron, 512 copper, 256 tin, 128 nickel, 64 gold.
- Stress test: 4,000 iron. EV drill common-metal subtotal: 418 iron, 272 copper, 24 tin, 192 nickel, 40 gold. It excludes titanium, tungsten, circuits, chemicals, rocket and multiblock costs.
- Steam and LV quantities are working budgets scaled from representative native machine-package mineral subtotals. The other reserves are explicit scenario choices. They are not exact costs to finish every quest or construct a complete factory.

## Continuous mining campaign

These stages keep previous discoveries and incidental ores. A campaign stops at its first unfinished budget. Later-stage medians therefore describe survivors, with progressively smaller and biased samples. Do not sum the medians to invent a typical full playthrough.

| Stage | Stock hours (finished / reached) | Mod hours (finished / reached) | Stock / mod single-lane ore processing hours |
|---|---:|---:|---:|
| Starter copper head | 0.24 (40/40) | 1.02 (32/40) | 0.02 / 0.02 |
| Bronze tooling and initial iron | 1.9 (32/40) | 1.45 (31/32) | 1.05 / 0.31 |
| Steam starter machines | 0.11 (32/32) | 2.18 (31/31) | 0.32 / 0.59 |
| Bricked blast furnace minerals | 3.47 (24/32) | 3.95 (29/31) | 0.27 / 0.26 |
| Steam steel and LV working stock | 0.45 (24/24) | 4.85 (29/29) | 6.28 / 8.85 |
| LV EBF mica and nickel reserve | 2.22 (16/24) | 6.54 (26/29) | 1.46 / 3.02 |
| LV to MV common-metal reserve | 0.18 (16/16) | 4.82 (26/26) | 3.08 / 4.07 |
| MV to HV common-metal expansion | Not estimated (0/16) | 20.05 (1/26) | Not estimated / 16.31 |
| EV drill common-metal subtotal | Not estimated (0/0) | Not estimated (0/1) | Not estimated |

## Matched continuous campaign totals

Only starts that completed both versions are compared below. These are simulated fixed-policy resource budgets, not observed human progression. These totals cover the chosen mineral budgets and travel/repair handling, not every action required to reach the named tier. The LV comparison has 21 matched starts across three seeds; the MV comparison has 12 across only two seeds.

| Budget route | Stock mining/search hours | Mod mining/search hours | Median paired extra hours | Stock / mod repairs |
|---|---:|---:|---:|---:|
| Through LV working-stock budget | 7.57 | 16.57 | 8.39 | 120 / 226 |
| Through MV reserve budget | 10.32 | 36.64 | 23.94 | 167.5 / 412.0 |

The paired delta is the median of individual differences; subtracting the two column medians need not produce the same number. Survivorship, only two or three contributing seeds, fixed pickaxe policies and assumed budgets make these scenario results, not a precise prediction for a new player.

## Partial discovery and exploration-policy sensitivity

Ore count is not the same as ore a player can find. A stock GTNH player can exploit public vein-grid knowledge, then mine a large body after discovery. A scattered-deposit player may find only a fraction of the ore in the same chunk and spend more time deciding where to search next. Drill throughput and exhaustive ore censuses cannot substitute for this player problem.

The baseline policies already credit only physically encountered/harvestable blocks, adjacent exposed faces and remembered deposits. They do not award the full ore contents of a chunk. However, their fixed tunnels, priorities and carry assumptions are simplified; the 23.94-hour MV delta must not be treated as a calibrated human prediction.

An additional **60 sensitivity campaigns** used five seed pairs and four starts per policy. Stock retained vein-grid searching. Both stock and mod paid optimistic round trips after each load of 768 raw ores (12 stacks), plus 60 seconds unloading. One mod policy also made four physical four-block local probes around a wanted ore find, rather than only following immediately connected ore. Probe directions were fixed geometric choices; hidden ore locations never chose destinations. The policy kept previous finds and incidental ores and stopped at a completed objective or the existing search bound.

| Policy | Completed LV budget | Completed MV budget | Median recovery of normal ore in excavated chunks |
|---|---:|---:|---:|
| Stock grid, ore-load trips | 12/20 | 8/20 | 5.81% |
| Mod branch, ore-load trips | 15/20 | 12/20 | 2.78% |
| Mod branch plus local probes and trips | 15/20 | 15/20 | 3.53% |

Recovery is retrospective accounting over full GT ores at Y1-99 inside chunks where the model excavated. It includes ore irrelevant or unharvestable at the current stage and pools campaigns of different lengths. These percentages demonstrate partial extraction; they are not useful-ore discovery probabilities or an equal-effort efficiency comparison. Small ores remain omitted.

For matched stock-grid/local-probe completions, the median additional mining/search work was **8.26 hours through the LV budget** (11 pairs across three seeds) and **18.00 hours through the MV budget** (seven pairs across two seeds). The corresponding fixed mod branch policy with ore-load trips gave 10.65 and 31.98 hours, respectively, on different matched cohorts. Those columns cannot be subtracted to infer a causal improvement.

Comparing the two mod policies directly on the same successful starts, probing reduced mining/search work by a median **2.81 hours through LV** and **4.69 hours through MV**. Both comparisons span four seeds, with 14 and 12 matched starts respectively. Completion differences still censor failures and prevent a whole-population conclusion. [Sensitivity summaries](adaptive-summary.json) and [every campaign](adaptive-campaigns.json).

This is evidence that player strategy matters. It is another bounded scripted policy, not a validated human agent. Caving, surface clues, choosing a new region, filling bags opportunistically, hammer mining and a full physical inventory/processing scheduler still require testing.

## Balance assessment and candidates to test

The current profile can be a reasonable preference tradeoff for someone who enjoys frequent smaller discoveries. The measured scenario cost is meaningful at early tiers, though, and supports a grind concern rather than proving that progression is impossible. Neither the 24-hour baseline nor the 18-hour sensitivity result establishes total additional playtime to a drill or a percentage of a full GTNH playthrough.

No generator, recipe, current release or existing world is changed by this report. Keep the current alpha available for comparison. Before choosing new defaults:

1. Test an optional early-game profile that redistributes some existing planned iron/copper/tin mass from rare industrial jackpots into more frequent small or medium discoveries. Favor materials harvestable with the actual early tools, in native eligible hosts and height ranges. Increasing inaccessible high-harvest-level ore would not solve Stone-age search friction.
2. Preserve large finds and bulk supply for later extraction. A blanket `depositsPerChunk` increase changes all materials and generation work and is not supported by this analysis. The previous four-site drill means do not guarantee early manual balance.
3. Compare fixed branch, local probing, caves/surface clues and hammer policies using the same budgets and starts. Include actual inventory loads, repair ingredients, native processing gates, fuel and tail search times. Mica/nickel and planetary progression need targeted checks; finite-area failures do not justify arbitrary global buffs.
4. If the design goal is a modest mining penalty, use a **proposed** benchmark of at most 1.5 times stock median manual resource-acquisition work on common Steam/LV budgets, while retaining failed searches, seed variation and bulk-extraction results. This is a design choice, not an established balance standard. Expand the world sample and verify real survival play before adopting it.

Any placement/profile change would need fresh-world/version handling and new native checks. This report does not authorize retrogen or updating an existing world to a different ore plan.

## Measured tools and machine timing

- Retained flint head: harvest boost after 274 native stone blocks, 113 initial durability. Native repair test: nine repairs for 1,000 stone blocks.
- Retained copper head: boost after 458 native stone blocks, 135 durability. Eight repairs for 1,000 stone blocks. The campaign charges deliberate training and an assumed four repair ingots in the starter budget.
- Bronze: 285 initial durability and three native repairs per 1,000 stone. Steel: 300 durability and two per 1,000 stone. Long-run rates extrapolate average native repair gains and wear; evolving modifiers are not faithfully simulated.
- Native stone break time: flint 0.60 seconds, copper 0.45, bronze/steel 0.35. Interaction overhead is added separately.
- Installed primitive blast furnace recipe: one iron plus four coal/charcoal, 7,200 ticks = six minutes for one steel ingot. A 200-steel example takes 20 furnace-hours and 800 fuel items. Four furnaces reduce ideal elapsed processing to five hours, but require four structures. This steel queue/fuel is a separate planning example, not included in the ore-processing column.
- Installed electric blast furnace iron-dust steel recipe: 120 EU/t, 500 ticks = 25 seconds per steel, with coal dust and oxygen inputs. Building and supplying that EBF is a separate gate.
- Recipe-derived ore-processing queues use conservative furnace/macerator main outputs, avoiding speculative chemical byproduct recovery. A queue is machine work, not necessarily extra active playtime. Parallel machines and overclocking change elapsed time.

## First Ore Drilling Plant correction

The first Ore Drilling Plant tested here can operate at MV power, but its controller is an EV build. The initialized recipe requires one EV hull, four titanium frames, four EV circuits, four EV motors, four EV pumps, four EV conveyors and four tungsten gears; 72 L solder, 1,920 EU/t in an assembler for 400 ticks (20 seconds). That is just the controller, not the complete formed plant, power network, pipes, drilling fluid or space progression. MV operating voltage must not be interpreted as an MV crafting unlock. This says nothing about the unlock tier of other miners or handheld drills.

Native runtime results, at 20 game ticks per second (game-time throughput, not measured CPU/TPS performance):

| Seed | Stock plant game hours | Mod plant game hours |
|---|---:|---:|
| 20261011 | 54.39 | 35.33 |
| -20261011 | 47.68 | 26.51 |

All four individual drill tests finished and cleared their scanned GT ores. The two seed1 composite runs had later harness failures; their individual drill tests passed. Seed2 complete suites passed. These runs do not establish equal metal/hour, full progression viability or FPS/TPS impact.

## Behavioral assumptions and remaining limits

- Player policies inspect tunnel blocks and adjacent exposed faces. Stock policy knows the installed equal-spaced vein grid and the public native companion-ore catalog. It searches a local vein envelope after recognizing a relevant vein. Mod policy targets published height ranges, follows connected ores and revisits exposed ores.
- Walking 3.5 blocks/s; targeting/repositioning 0.15 s/block; repair handling 20 s; food/torch/inventory handling 20 s per 128 blocks; marginal flint gathering 3 s per repair. Shafts and local connectors are excavation proxies, not faithful Minecraft movement physics.
- Ore wear is modeled at twice stone wear. Bronze repair adds 0.75 copper and 0.25 tin demand, steel repair adds one iron. Survival resource queues are credited optimistically; a full asynchronous crafting/inventory scheduler is not implemented.
- Small ores, caves, loot, quest rewards, trades, mobs, lava/water hazards, exact hunger/torch fuel, enchantments, hammer area mining, alternative miners and a complete factory/rocket/chemical bill are omitted. Stone-host calibrations do not reproduce every host hardness. Alumite/cobalt repair ingredients are not fully modeled, so late subtotals are weaker evidence.
- The exploratory `milestone-demands.json` has unresolved circuit/recipe leaves and incomplete infrastructure. Its machine time subtotal and the exploratory chemical `native-processing-potentials.json` are excluded from gameplay conclusions.
- Finite-area failures are censored searches, not proof that an ore is absent from the world or progression is impossible. Seed-level uncertainty and learning/player adaptation dominate any apparent precision.
- There is no defensible single total from fresh spawn through EV/IV yet. The strongest current conclusion is extra early manual mining and repair work, with rare-material search risk; the blanket claim that progression is impossible remains unproven.

## Evidence and reproduction boundary

The JSON/CSV files in this directory contain all final per-attempt resource-budget outputs and summaries, including failed/censored searches. [Native facts](native-facts.json) retains the queried drill and steel recipes, tool repair curves, native dimensional ore censuses and drill timings. [Build verification](../../BUILD_VERIFICATION.md) distinguishes the measured original binary from the current namespace-only replacement.

The [method sources](method/) expose the fixed and probing policies. Their file paths are adapted for public use; they are not a new simulation run. Reproduction requires the pinned GTNH/native QA environment and original terrain/recipe exports. Those full inputs, server worlds, runtime dependencies and logs are not included here. This is not a portable one-click reproduction bundle. The aggregate result integrity check does not validate human realism or a complete survival playthrough.
