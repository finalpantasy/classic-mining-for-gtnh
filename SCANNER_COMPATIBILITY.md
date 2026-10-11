# Prospecting evidence and limits

Classic Mining uses native natural full GT ore blocks and native material identities. The pinned electric prospector source reads actual blocks through OreManager.getOreInfo, skips non-natural ores, and filters small ores in its relevant scan mode. It does not require the old ore-seed grid to identify those blocks.

The final algorithm-6 v2 native API fixture exercised all 256 material identities through direct prospecting and electric ProspectingPacket encode/decode in both relevant modes. Algorithm 6 changes placement and bulk mixtures, not these block identities or scanner code. Run label: science6-v2-readiness; exact artifact identity is recorded in the final evidence report.

These checks support material recognition and payload serialization. They do not prove a powered asynchronous scan, network delivery, rendered colors, tooltips, range coverage, or the complete HUD on a client.

VisualProspecting's cached vein labels use a separate vein/cache model. Their accuracy for irregular deposits and multiple related minerals is not established. A vein-name map pin is not a complete description of the actual underground body. Use physical prospecting readings and record actual finds until that integration is verified. No scanner configuration is silently disabled by this mod.

Local pinned references: classic-mining/reference/BehaviourProspecting.java, BehaviourDetravToolElectricProspector.java and VisualProspectingConfig.java. GTNH 2.9.0-RC-2. Compatibility remains specific to these tested versions.
