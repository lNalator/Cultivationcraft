# Changelog

## [0.4.5] - WIP - Realm passives and Core Forming concepts

### New features and changes

- Realm passives: each stage broken through now gives a bonus, recalculated from the cultivation history so existing saves get it too. Foundation Establishment: +1 max HP per stage. Qi Condensation: +1 max HP and +1% movement speed per stage. Core Forming: +2 max HP per stage.
- Sea of Qi (Qi Condensation signature, kept in Core Forming): with full Qi, the surplus slowly regenerates life (1 HP every 4 seconds).
- Golden Core (Core Forming signature): elements press each other in the loop Fire > Wood > Wind > Earth > Water > Fire, and Ice and Lightning press and are pressed by every other element. A player whose core element presses the target player's core deals ×1.25 damage (`pressure_damage_multiplier` in the server config). Pure stays outside the loop and makes techniques cost 10% less Qi.
- Core Forming has its own passive: the Qi Condensation stats plus the Qi absorption range (+1 block every 500 points), levelled with cultivated Qi and the chosen focus like in Qi Condensation. Qi already cultivated in Core Forming on existing saves is moved into Max Qi. Technique training caps now keep growing in Core Forming, and each technique use trains 1 point plus 1 per Core Forming stage (4 points at stage 3).
- Core Forming cultivators can cultivate in every Qi source, at an efficiency set by their core: 125% in their own element (150% for Ice and Lightning cores), 50% in other elements, 10% in an element that presses their core (Ice and Lightning cores keep 50%), and 100% in pure and "any" sources and for Pure cores. Values are in the server config (`own_element_efficiency`, `variant_own_element_efficiency`, `other_element_efficiency`, `pressing_element_efficiency`).
- Core Forming breakthroughs can reinforce a core already owned instead of choosing a new concept, which removes the block at stage 7. Each extra rank: elemental cores make their states 25% longer and stronger, Grow +25% size, Falling +50% falling damage. Aura, Explosion, Warp and Elasticity are unique and cannot be reinforced.
- Non-elemental concept unlock quests now progress from Qi Condensation stage 4, instead of only in Core Forming. When choosing a core, an owned core that can be reinforced shows the rank it would reach, e.g. "(rank 2)". The first core, chosen when leaving Qi Condensation, must still be the elemental one.
- Added the Infusion concept (unique): techniques leave their element along their path (Qi projectiles, flying swords) and in a 2-block zone around their impact, with weaker states and no block effect. Unlocked by equipping 5 different items with an elemental enchantment (Fire Aspect, Flame, Respiration, Aqua Affinity, Depth Strider, Riptide, Frost Walker, Channeling, Feather Falling, Thorns); each item only counts once.
- New exclusive Mass axis: Falling and Levitation cannot be taken together. Mass only acts on a real fall of over 2 blocks, not while swimming, climbing, gliding, riding or flying. Falling now makes the player fall 1.5x faster. Added Levitation: falls are slowed to Slow Falling speed with no fall damage (sneak to fall at normal speed: only the distance fallen from the moment the player sneaks counts for fall damage), and Qi projectiles last 50% longer per rank. Levitation is unlocked by falling slowly for 60 seconds (Slow Falling effect).
- New exclusive Field axis: Gravity pulls the entities around a technique's impact towards it, Repulsion pushes them away and slows enemy projectiles to 30% of their speed. Radius 3 blocks, stronger near the impact; each rank adds 1 block and 25% force. Rooted targets are not moved. Gravity is unlocked by taking 50 fall damage, Repulsion by being knocked back 50 times; both are stabilised by moving entities.
- New exclusive Rhythm axis. Haste: Qi projectiles and flying swords +30% speed (+10% per extra rank), the aura pulses every 0.7 s, and damage is reduced by 15%; unlocked by using the same technique 50 times in a row (toggled techniques like Flight do not count). Charge: holding Qi Emission's key charges the shot up to x2 damage and Qi cost in 2 s (+0.5 per extra rank), flying swords are 25% slower but deal x1.5 damage, and the aura pulses every 1.5 s with x1.5 damage; unlocked by landing 10 hits of 20 damage or more.
- Added the Echo concept: each technique hit repeats on its target 1 s later at 40% of its damage; each extra rank adds another echo, 5 points weaker than the previous one (`echo_step` in the server config), never below 5%. Echoes cost no Qi and never echo themselves. Unlocked by taking 25 hits in a row, at most 1 s apart.
- Added the Poison concept: technique hits poison the target and block all its regeneration (new Blocked Regeneration state, Sea of Qi included) for 3 s, +2 s per extra rank. Unlocked by meditating while poisoned for 5 minutes in total; the count restarts on death.
- Falling landings now show a ground slam: fake copies of the ground blocks lift and tilt away from the impact then settle back, over cracks and block particles, with a heavy impact sound. Purely visual, the world is never changed.
- Flight with the Warp concept no longer glides: the player teleports by jumps every 0.5 s in the direction they move (look up or down to jump vertically). The flight speed sets the jump length (5 blocks at base, 9 at most), and each jump costs 10 Qi per block.
- Grow now also enlarges the flying swords' hitbox, matching their bigger model.
- Elasticity's fall bounce costs half as much Qi (half the damage taken instead of all of it).

### Fixes

- Fixed Qi source particles disappearing in front of and behind clouds: they were drawn with the depth test off, which also stopped them writing depth, so the clouds drawn afterwards painted over them. They now stay visible through clouds like through walls.
- Fixed the Falling landing shockwave missing some mobs in its zone: its hit box was flat at the player's feet, so mobs standing exactly on the same level were not detected. It now covers one block up and down and uses the same circle as the ground slam visual.
- Fixed Qi sources being ticked up to 3 times per server tick (two listeners, one of them at both the start and end of the tick): the list of players absorbing was synced empty right after being filled, so the Qi stream to a meditating player was not shown, and sources regenerated 3 times faster than configured. They now tick once per tick.
- Fixed the Qi stream from a source to a meditating player being missing or patchy: particle spawning added up frame times instead of following game ticks, so it depended on the frame rate. A source being absorbed now sends a continuous stream (one particle every 2 ticks to each absorbing player), stream particles live long enough to reach players outside the source's radius, the stream stops when absorption stops, and two players absorbing at once can no longer crash the renderer.
- Fixed Item Enhance giving its concept effects (Explosion, Infusion...) the attacker instead of the entity hit, so they happened on the player.
- Fixed Falling's extra gravity on projectiles being applied at any speed (a stray `;` disabled its speed cap).
- Network protocol version is now 9.
- The core chosen at a Core Forming breakthrough is now checked on the server (learnt and allowed), instead of trusting the client.

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
