# Changelog

## [0.4.0] - Spiritual Plants, Alchemy and Knowledge

CultivationCraft begins its standalone development under **lNalator**, with the original owner's approval. Thanks to Bababaa, the original creator. 
This release targets **Minecraft 1.19.2, Forge 43.1.52 or newer, and Java 17**.

This is a fresh release: create a new world. Compatibility with saves from earlier versions is not supported.

### Spiritual plants

- Added procedural spiritual plants with a persistent, world-specific species catalog and genomes. Species have generated names, colors, stem/foliage/fruit variants, growth characteristics, and elemental affinities.
- Added elemental spawning and growth conditions for Fire, Water, Wood, Earth, Wind, Ice, Lightning, and neutral plants, including appropriate Nether generation and shallow-water placement.
- Separated physical maturity, spiritual growth, plant tier, and Qi-source reserves. Spiritual growth determines the plant's tier; Qi-source reserves have their own role.
- Added plant catalog synchronization, layered plant rendering, generated display names, and detailed tooltips. Divine Sense can display the targeted plant's tier and growth.
- Plants can be planted in water. Only Water plants continue generating Qi while submerged.

### Spiritual alchemy

- Added the Alchemy Cauldron with nine persistent ingredient slots and items orbiting inside the block.
- Added a scrollable, zoomable station panel with an advancement-style frame, elemental icons and score totals, a flame indicator, and result slots. The player inventory sits below the station panel.
- Elemental recipe scores use each plant's spiritual growth multiplied by its stack count. Qi-source reserves do not inflate those scores.
- Added JSON-driven pill definitions, effect families, generated names, purity, refinement Qi costs, cooldowns, and recipe difficulty.
- Added **19 pill recipes across tiers 1–3**: food, instant healing, healing over time, instant Qi restoration, Qi restoration over time, cultivation progress, and Qi absorption.
- Cultivation and absorption pills target their intended cultivation realm, with effectiveness halved for each major realm above their intended realm.
- Added family cooldowns and item cooldown overlays. Matching pills with the same name, properties, and purity stack up to 16.
- Recipes require suitable plant tiers and elemental scores; there is no arbitrary minimum number of species or occupied slots. One plant can complete a recipe if it satisfies all requirements.
- Added difficulty-scaled failure chances, impurity penalties, and optional neutral-base success/purity bonuses. Missing mandatory requirements always ruins the batch.
- Failed batches produce 10 alchemy remnants and a small explosion that damages nearby entities, with extra damage to the refiner. Explosion settings are configurable in JSON, scale with recipe complexity, and never destroy blocks.
- Successful batches at 70% purity or less also produce remnants based on lost purity.
- Unknown cauldron previews remain black silhouettes with `???` tooltips. Crafted pills display their normal sprite even before identification.
- Added particle-free Qi-restoration and Qi-absorption status effects with visible effect indicators.

### Qi Transfer and refinement

- Added the external cultivation skill **Qi Transfer**, sending Qi into spiritual plants and alchemy cauldrons.
- Skill progression increases transfer frequency, improves range, and lowers stamina/Qi cost. Plant transfer cost scales with plant tier, and skill progression contributes to cultivation advancement.
- Cauldrons light while receiving Qi and lose one stored Qi every five seconds without a transfer.
- Reworked the former Bind menu into a general **Refinement** tab and slot for flying swords, pill identification, jade slips, and spirit stones.
- Refining a pill reveals its name, effect, and purity without consuming it.
- Added floating, slowly rotating refinement items and Qi streams with particles that grow and fade. Elemental streams flow into bound items; jade-slip knowledge flows toward the player's head; spirit-stone energy flows toward the chest.
- Qi Transfer to a cauldron uses a matching particle stream.

### Jade slips and knowledge

- Added jade slips with generated titles and player-specific knowledge unlocks. Refining a new slip consumes it and reveals its teaching; already-known slips are preserved.
- Recipe slips reveal their matching cauldron preview. Alchemy knowledge can reveal an estimate of resulting pill purity.
- Moved Help entries into the data-driven knowledge system. Existing introductory information starts unlocked; discoverable teachings remain hidden until learned.
- Organized recipes by **Recipe → Pill Type → Pill Recipe**, and grouped other teachings into topics such as Alchemy, Nature, and Refinement.
- Added teachings for spiritual plants, Qi sources, Qi Transfer, Divine Sense, refinement, tribulations, ores, pill recipes, impurities, and recipe failure/difficulty calculations.
- Added chest-specific knowledge weights and rarity pools. Common slips are easier to find; villages favor introductory plant knowledge and simple recipes, mineshafts favor Qi sources and ores, and strongholds/end cities favor rarer knowledge. Stronghold library chests guarantee a jade slip.
- Debug/command Help is visible to operators, including operators outside Creative mode.
- Split long knowledge text into shorter paragraphs for readability.

### Ores and spirit stones

- Added imperial-green jade ore in deepslate, with iron-like generation frequency and connected deposits of at least five ore blocks. It requires an iron pickaxe and drops jade chunks and a little experience.
- Seven jade chunks in a U-shaped crafting pattern produce an Alchemy Cauldron.
- Added pale blue-gray spirit stone ore in deepslate, with diamond-like generation frequency. It requires an iron pickaxe and drops 1–3 spirit stones before Fortune bonuses, plus experience; Silk Touch preserves the ore.
- External cultivators can refine one spirit stone over five seconds for **5 cultivation progress and 50 Qi**. Qi restoration still works when cultivation progress is full; stones are preserved when neither reward can be used.

### Divine Sense, interface and fixes

- Divine Sense detects tagged spiritual plants and spirit stone ore through walls. New detectable blocks can be added through the block tag.
- Detection radius, highlight limit, and strength scale with cultivation, with larger gains at major breakthroughs. Radius and highlight limit appear in the skill's stats.
- Plants appear as two-pixel-wide, three-dimensional crosses tinted by element. Color increases quickly at low spiritual growth and gradually approaches full saturation at high growth; zero growth is white.
- Highlights reach peak opacity three blocks inside detection range and fade near the player. Stable scan snapshots reduce distant highlight blinking.
- Added numerical resource and current/max HP readouts. Available Qi or Available Stamina follows the player's cultivation type.
- Corrected resource synchronization when meditation and expensive toggled skills run together.
- Added argument suggestions for plant catalog, plant-giving, Qi-source, pill-giving, and jade-slip commands. The canonical pill command is `/cultivation givepill`.
- Fixed pill-definition loading, recipe-slip lookup, and nested Help knowledge navigation.
- Fixed the tribulation advancement icon and changed breakthrough wording from “next tier” to “next realm.”
- Removed spiritual-plant right-click debug output and addressed the experimental-world warning caused by ore world-generation registration.


### Known issue under investigation

- A client freeze after death, including `/kill @a`, has been reported with OpenAL errors. The available log does not identify the cause; a thread dump during reproduction is still needed.
