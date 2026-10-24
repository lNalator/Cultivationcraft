# TODO

Grouped by chantier/category. Each item has a priority (P1 = next, P2 = soon, P3 = later) and a size (S / M / L).

## 0.4.5

### Concepts (Core Forming)

- **P2 · L** Specific, representative design for each core created by a concept, shown in the cultivation screen (e.g. Warp gets an ender-like disappearance effect, Ice a frozen look). Ideas are noted as `TODO core design` above each concept's constructor (`Common/Qi/Techniques/TechniqueModifiers/*Modifier.java`); cores without a texture use `textures/cores/blank.png`.
- **P2 · M** Purge an absorbed element: slowly clear (at a low rate) the Qi of one element absorbed during Qi Condensation, so a player can still advance their realm and, if they spend the time, change their main element to get a different or rarer core.
- **P3 · M** Undecided offers: Warp flight jumps beyond 9 blocks (server-side teleport; speed past the cap now shortens the Warp Delay instead); Haste reducing the techniques' 0.25 s cooldown.
- **P3 · S** Warp teleporting Qi projectiles (`WarpModifier.QiProjectileSpawnOverride`).
- **P3 · L** Synergies between cores (e.g. Aura + Explosion, Infusion + Warp, Fire + Explosion).
- **P3 · L** Paused axes: Body (Blood / Vitality) and Perception (Light / Shadow).
- **P3 · L** Let the player modify a concept's effect, on chosen techniques or directly. Groundwork for the technique rework below.

### Qi sources and Divine Sense

- **P2 · S** Divine Sense: spiritual plant markers should shrink/grow with the plant's current Qi amount (`Client/Renderers/DivineSenseBlockRenderer.java`).
- **P2 · M** Divine Sense radial menu with multiple selection (Qi sources, spiritual blocks, entities, everything). Mask stored in the capability and filtered in `DivineSenseTechnique`, plus an entity render in `DivineSenseBlockRenderer`.

### Tribulation

- **P3 · S** Divine Sense marker showing a tribulation through walls, from further away.
- **P3 · S** A ray of light on the player when they survive the tribulation, as the clouds part.

## Minecraft versions

- **P1 · L** Provide the mod for Minecraft versions above 1.19.2, up to at least 1.21, or even v26.

## Elements and visual feedback

- **P2 · M** Elements: bigger visual feedback (particles, sounds) when an element hits a block or an entity (`Common/Qi/Elements/*`).
- **P2 · M** QiGlowRenderer: element-coloured halo for Qi Barrier, meditation, breakthrough and tribulation, pill effects and higher realms seen in Divine Sense (`Client/Renderers/QiGlowRenderer.java`, `render()` is empty).
- **P3 · S** Replace the placeholder icons of the elemental states.
- **P3 · S** Remove the vanilla freeze damage (1 every 2 s) while frozen? (undecided)
- **P3 · M** Ice Walk in the air (option B), later.

## Formations

- **P2 · L** Formations, a whole chantier: protection formations (a barrier around a zone), Qi gathering formations (e.g. faster regeneration of a Qi source), and others.

## Flying swords

- **P2 · M** Smooth controller: inertia, arc turns, U-turns, block avoidance, guard and attack modes. Fix the two TODOs in `Common/FlyingSwordEntity.java` and `FlyingSwordController.java` (bad reverse turning, a block commented "bugged as hell").
- **P3 · L** Flying sword AI rework, to do together (Enzo + Claude). Includes the Levitation behaviour: swords hovering above the player between attacks.

## Spiritual plants

- **P2 · M** More plant presets: 6-8 variants per part instead of 2 stems, 2 foliages and 2 fruits, with new parts (flower, root, thorns, glow) weighted by element (`Common/Blocks/Plants/world/PlantVisuals.java`, `ProceduralPlantBakedModel` cache).

## Interface

- **P2 · S** Widen the technique selection list so longer names fit.
- **P2 · M** Humanize the knowledge (Help) texts so they read naturally (`data/cultivationcraft/knowledge/**`).
- **P2 · M** Knowledge page rework: side navigation, search, illustrations, unread entries marked (`Client/GUI/Screens/HelpScreen.java`).
- **P3 · M** Cultivation menu redesign (Enzo has a design idea).

## Techniques

- **P3 · L** Technique rework: the player customises each technique by placing points in the stats they want, within the "technique training" level already unlocked.

## Body cultivation (internal path)

- **P3 · L** Internal cultivation rework, a big feature for later:
  - body realms (Skin → Flesh → Bone → Blood → Marrow → Organs);
  - power-oriented modifications (ice flesh, crystal bones...), to keep or remove (decision postponed);
  - `BodyPartNames.java` moved to JSON;
  - balance with the external path;
  - forge and default quest thresholds, merge of "time alive" / "Live", player elemental resistances coming from the body, elemental body parts.
- **P3 · S** Bone size effect only lasts one tick, because mlmAnimator resets the size to 1.
- **P3 · S** Leftovers: "lock the body whilst drawing" TODO in `Client/GUI/Screens/BodyforgeScreen.java`; the "evolved limb" advancement fires when a forge is selected instead of when it evolves.

## Later realms

- **P3 · L** Realms after Core Forming: Nascent Soul with a customisable domain (the Aura technique is the prototype). Remove the stage 9 block in `CoreFormingCultivation.canBreakthrough`.

## Known bugs

- **P2 · M** Client freeze after death (OpenAL), listed as a known issue since 0.4.0: needs a thread dump.
