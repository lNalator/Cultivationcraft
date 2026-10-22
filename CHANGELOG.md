# Changelog

## [0.4.4] - WIP - Code cleanup, quest goals and food

### New features and changes

- Core Forming stabilisation quests now share one goal per stage, whatever the core: 1000 at stage 1, then +500 per stage (5000 at stage 9). The concept only decides what is counted, which also replaces the provisional goals of Aura and Grow. Both values are in the server config (`stabilise_goal_base`, `stabilise_goal_step`); quests already in progress keep their previous goal.
- Qi cultivators (external path) can eat foods with potion effects (golden apples, suspicious stew, pufferfish...) to get those effects only: eating gives no Qi, and is allowed even with full Qi. Other foods stay inedible.

### Refactoring

- Removed every raw-type, unchecked and redundant-cast compiler warning with explicit types. Techniques are now registered as typed factories (`Supplier<Technique>`) instead of classes created by reflection, and stat, focus and level lookups take the technique itself; saves stay compatible since techniques and their levels keep the same keys. `DropdownList` and `getBodyPartsOfType` are generic, and block properties are read through their typed constants.
- Removed 336 warnings reported by the Eclipse compiler used by VS Code Java: 282 unused imports, 33 static methods called on an instance, unused local variables, an incomplete enum switch, and the deprecated `Class.newInstance()`, `Item.getFoodProperties()`, `getItemEnchantmentLevel` and `hasChunkAt` calls. The 16 remaining warnings are unavoidable Minecraft/Forge deprecations.

## [0.4.3] - WIP - Elements, Aura and Freezing

### Fixes

- Fixed cultivation getting stuck just below the stage maximum: the breakthrough check truncated each stat level separately, so Qi split across several stats could never reach the cap.
- Quest progress is now only computed on the server; quest progress sent by a client is ignored.
- Fixed the Grow concept's stabilisation quest, which only progressed client-side and could never complete.
- Fixed the "Damage absorbed" quest, which had no trigger and a mismatched translation key.
- Frozen blocks: the freeze duration moved from the block state to the block entity (removing about 12,000 block states), multiblocks no longer drop the wrong block entity, and container contents are no longer lost when a chunk unloads while frozen.
- Fixed players becoming unkillable with the vanilla Absorption effect (golden apple, potion): hits smaller than the absorption amount were cancelled, so the absorption hearts were never used up.
- Fixed the technique selection list not scrolling to its last entries when the slot already holds a technique.

### Elements and elemental states

- Added elemental states as status effects, shared by mobs and players: Wet, Chill, Frozen, Freeze Immunity, Off Balance, Electrified, Stunned and Rooted.
- Each element now has its own identity: Fire burns for 2 s + damage/4 (max 10 s) and melts ice and snow; Water wets, pushes lightly, puts out fire and cools lava; Ice stacks Chill and freezes at 5 stacks (2 s, halved in PvP, then 5 s of immunity); Wind pushes and unbalances; Lightning strikes the target then chains to 1-3 nearby enemies; Earth stuns and ignores 25% of armor; Wood steals 20% life, roots and grows plants.
- Added interactions between every element and every state (49 combinations), for example Water then Ice freezes instantly, Earth shatters a frozen target for double damage, and Wind cannot push a rooted target.
- Mobs now resist or are weak to elements through entity tags (`cultivationcraft:resists/<element>` and `cultivationcraft:weak/<element>`), editable by datapack. Fire-immune mobs take no Qi fire damage.
- Freezing blocks: 5 s + 0.5 s per damage point (max 30 s), refreshed by new hits. Frozen blocks can be broken like the original block and drop its normal loot, containers included, without duplication. Water and lava sources freeze temporarily, and frozen water stays frozen while an Ice Qi source reaches it. Blocks in `cultivationcraft:freeze_immune` never freeze.

### Techniques and concepts

- Added the Aura technique, unlocked by choosing the Aura concept as a core: an elemental zone that pulses every second on nearby entities, with trainable range, damage, intensity and Qi cost. Activate it while sneaking to also affect blocks. A Wind aura deflects enemy projectiles.
- The Aura concept no longer applies a passive area effect to other techniques (which set everything on fire) and no longer doubles their Qi cost. It is unlocked by Qi overflow (30 s meditating in a Qi source with full Qi) and stabilised by hitting entities with the aura.
- Re-enabled Ice Walk: freezes water and lava under your feet, in a radius that grows with training; sneak to sink. Only available when Ice is your main absorbed element.
- Core Forming stabilisation quests get harder with each stage (+50% of the base threshold per stage).
- Elemental concepts are only learnt from the main element absorbed during Qi Condensation; their separate unlock quests are disabled.
- New quest types: entities frozen, burning entities extinguished, lightning chain jumps, and elemental damage resisted.

### Interface and tools

- Added reusable toast notifications with sounds: quest completed, concept learnt, technique mastered, stage cultivation full, refinement cancelled. Each type can be turned off in `cultivationcraft-client.toml`.
- Added the Elemental Debug Stick (creative Tools tab): right-click an entity, a block or the air to apply an element effect & damage (if applicable); sneak + right/left-click to cycle elements.
- Added `/cultidev` (development environment, or `-Dcultivationcraft.dev=true`): set realm and stage, learn concepts, add cores, and measure quest progress per minute.
- Absorption hearts (golden apple, potion) are shown as a yellow layer on the health orb, and as "(+X)" next to the health value.
- Qi source particles are more visible and are only shown within four times the Divine Sense detection radius.
- Network protocol version is now 8.

## [0.4.2] - WIP - Optimization and Refactoring

### Fixes

- Fixed a client freeze when selecting a body forge (Qi Sight) as a body cultivator: a normal-sized player got a zero model scale, which produced a NaN eye height and hung the crosshair raycast.
- Fixed text wrapping looping forever when the available width is narrower than a single character.
- Fixed Qi sources with a non-positive size hanging the world tick when picking a random position.
- External blood tick packets are now processed on the server thread instead of the network thread.
- The server now rejects technique-use packets with an out-of-range slot.

### New features and changes

- Added "Technique 1" to "Technique 9" key bindings (Controls > Cultivation, unbound by default). Each key uses the technique in the matching skill hotbar slot without switching away from the item hotbar; the bound key is shown on the slot's icon.
- Refinement now requires meditation: placing an item in the refinement slot starts Meditate (it must be equipped on the skill hotbar), and the player cannot move while refining. Finishing or removing the item stands the player up; turning Meditate off cancels and returns the item to the inventory.
- The refinement page shows a warning when Meditate is not equipped, on a full-width status line that also displays the remaining time.
- Every cultivation menu page now uses a larger 296 × 208 panel, with the tabs at the same position on each page.

## [0.4.1] - WIP - Optimization and Refactoring

### Performance

- Qi-source searches now inspect only loaded chunks without loading, generating, or waiting for neighboring chunks. Search bounds were tightened, and distance checks avoid unnecessary allocations and square roots.
- Reworked spiritual-plant generation with cached surface heights and environmental checks, bounded cave searches, and configurable patch and placement budgets. Nether plants search below the bedrock roof; shallow-water placement still requires solid ground immediately beneath the water.
- Plants at maximum spiritual growth skip growth calculations. Neighbor searches stop once a tier 3 plant is found, and redundant block updates were reduced.
- Reduced plant synchronization payloads and grouped new Qi-source updates through the existing chunk tick.
- Cached complete layered plant models to avoid repeatedly assembling the same geometry during rendering.

### Refactoring and fixes

- Shared one persistent plant catalog across dimensions, initialized before world generation, with immutable entries and direct species/element lookup indexes.
- Client catalog and name-pool updates now replace immutable snapshots. Plant rendering refreshes when catalog data arrives, and the client catalog clears on disconnect.
- Plants now preserve their current Qi-source reserve when harvested and replanted. Source ownership prevents removing an unrelated nearby source, and newly attached sources are registered for ticking.
- Command-given plants now receive spiritual growth matching their catalog tier; only tier 3 specimens can be given stored Qi-source data.
- Limited catalog size to the block's supported species range while retaining neutral plants. Activated patch-size, growth-bonus, and catalog-tier settings; zero elemental spawn multipliers disable every plant tier.
- Removed unused genome traits, configuration fields, and placement helpers. Plant names now honor the configurable default name pool and report loading errors.

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
