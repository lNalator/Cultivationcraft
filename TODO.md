# TODO

Grouped by category. Each item has a priority (P1 = next, P2 = soon, P3 = later) and a size (S / M / L).

## Visual feedback

- **P2 · S** Divine Sense: spiritual plant markers should shrink/grow with the plant's current Qi amount (`Client/Renderers/DivineSenseBlockRenderer.java`).
- **P2 · M** Elements: bigger visual feedback (particles, sounds) when an element hits a block or an entity, for clearer player feedback (`Common/Qi/Elements/*`).

## Interface

- **P2 · S** Widen the technique selection list so longer names fit.
- **P2 · M** Humanize the knowledge (Help) texts so they read naturally (`data/cultivationcraft/knowledge/**`).

## Concepts (Core Forming)

- **P2 · M** Purge an absorbed element: slowly clear (at a low rate) the Qi of one element absorbed during Qi Condensation, so a player can still advance their realm and, if they spend the time, change their main element to get a different or rarer core.
- **P2 · M** Menu showing the concepts the player owns and their effects.
- **P3 · L** Let the player modify a concept's effect, on chosen techniques or directly. Groundwork for the technique rework below.

## Techniques

- **P3 · L** Technique rework: the player customises each technique by placing points in the stats they want, within the "technique training" level already unlocked.
