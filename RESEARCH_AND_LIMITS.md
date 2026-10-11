# What makes this model worth testing

The evidence-backed proposition is reproducible geology-inspired ore placement, bounded discovery probabilities, and measured native extraction. Scientific methods make the assumptions inspectable; they do not certify enjoyment or identical completion time.

Iron formations motivate layered beds. Porphyry copper deposits motivate broad mineralization with intersecting fracture networks. Tin/tungsten lodes motivate narrow bodies and rich shoots. Magmatic nickel/copper/platinum deposits motivate lenses. Pegmatites motivate zoned mineral/crystal pockets. These are representative shape analogues, not individual geological validation of every active GTNH material.

Sources researched for the baseline model:

- [USGS iron formations](https://www.usgs.gov/publications/sedimentary-facies-iron-formation)
- [USGS porphyry copper deposit model](https://www.usgs.gov/publications/porphyry-copper-deposit-model)
- [BGS Cornish lodes and mineral zoning](https://webapps.bgs.ac.uk/memoirs/docs/B01739.html)
- [USGS lithium-cesium-tantalum pegmatites](https://www.usgs.gov/publications/mineral-deposit-model-lithium-cesium-tantalum-pegmatites)
- [USGS magmatic nickel-copper-PGE deposits](https://www.usgs.gov/publications/occurrence-model-magmatic-sulfide-rich-nickel-copper-platinum-group-element-deposits)

Naquadah-family cores, neutronium/adamantium lenses and magical crystal pockets are explicit fictional design analogues. Native planetary access and chemistry take precedence. Real ore grades, biome exclusions and real search distances are not copied into the game.

## Progression boundary

The audited two-Stargate reference expands the pinned CoreMod SG crafting graph and native quest dependencies. It is a component graph, not a complete raw-ore bill or elapsed-hours simulation. Alternative processing, byproducts, recycling, magic and parallel factories prevent converting its component count into one honest ore multiplier.

For demand D, sustained throughput cannot exceed min(r_mining, r_processing, r_transport). Extra ore matters only when physical mining limits production. A resource that moves to an unchanged native void/space source need not depend on the changed terrain any longer. An isolated mining-rate change alters required extraction work by D*(1/r_new-1/r_stock); it does not predict the whole journey.

Pinned gameplay references: GT5 5.09.54.205, NewHorizonsCoreMod 2.9.84, and the installed 2.9.0-RC-2 quest/recipe data. [SG crafting source](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/2.9.84/src/main/java/com/dreammaster/scripts/ScriptSGCraft.java). The native audits preserve recipe quantities/NBT, behavioral flags, assembly-line research, void distributions and SpaceMiningData. Strict output ordering and other auxiliary metadata are separately limited.

## Outstanding uncertainty

Four paired Overworld sites and finite planet samples cannot rule out every progression shortage. The algorithm-5 planet plans remain unchanged in algorithm 6, including the previously reported Neutronium/Adamantium sample outliers. The final report must retain them as open balance concerns. Natural background probability is positive; it is not a guarantee of a find within a fixed distance.

Headless branch mining retains native harvest gates but omits hazards and travel. Processing tests provide power/water and manually transfer the primary output between machines. Prospecting payload checks do not establish the rendered HUD or cached vein-map accuracy. A complete survival/factory playthrough remains necessary before promising equal difficulty or completion length.

## Preserved failures

The algorithm-6 v1 source compacted pure associated minerals in Tin-primary layers. V2 limits that correction to mixed finds or pure Tin-smelting roles. V1 source, artifacts and results remain local.

The first native processing fixture selected the wrong FluidRegistry overload; the next supplied one MV amp to every machine and stalled in thermal centrifuging. The corrected fixture uses the native machine's rated amperage while retaining MV voltage. Its next run completed washing and thermal centrifuging, but the 100-tick power-probe sampling missed active macerator cycles of the same length. Startup is now sampled tick by tick before the four-tick unsupplied energy probe. These are retained QA fixture failures, not successful processing results or changes to gameplay recipes.
