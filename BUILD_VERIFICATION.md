# Public build verification

The algorithm-6 release uses project-only packages: `classicmining` and `classicminingqa`. The public source paths, archived source, transformer owners, reflective entry points and core-plugin manifest use the same namespaces.

Current release JAR SHA256: `86e33e9080c7b4c2ca4a1b49931ba42661123a747a5395a9acf558fdd3323046`.

Original measured JAR SHA256: `4b4e285f2e4b8a90edbb57aa33328fd64911a36a4ed451beb246ad67aefa6272`. This identifies the binary used in the retained four-site native mining campaign; those historical measurements were not rerun or relabeled as a new campaign.

## Checks on the replacement

- Compared every JAR entry to the original published binary. All 20 entries match after rewriting only package-name UTF8 constants and the core-plugin manifest. Every other class-file byte matches, including method bytecode and non-namespace constants.
- Passed 1,669,278 geometry and compatibility checks, 20,000 deposit plans, and transformer verification against the base and Java-17 GregTech bytecode variants.
- Passed the million-draw distribution check, 256 industrial bodies, determinism/connectivity/depth/ownership checks, and bulk compensation checks.
- Compiled all renamed native QA sources against the pinned GTNH/HorizonQA dependencies.
- Passed fresh native GTNH terrain, starter-tool lifecycle, and prospector-payload tests with the rebuilt JAR.

The package-only change does not replace the documented balance limitations with a complete survival or performance claim. Existing player installations are not modified by this repository cleanup. Keep the algorithm/profile appropriate to your save.
