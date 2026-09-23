# Divine Sense block vision

While the local player's Divine Sense is active, nearby spiritual plants receive
thin pale-green outlines visible through walls. The effect has a 16-block
radius, fades over the outer five blocks, and shows at most the nearest 48
tagged blocks. It uses no filled boxes, labels, or flashing effects. Hiding the
HUD also hides these outlines.

Only loaded client chunks are scanned, every ten ticks. Switching worlds,
deactivating the technique, dying, or removing it clears the cache. Broken
blocks are rechecked before drawing. Rendering does not change the plants or
their existing models.

To add other detectable blocks, extend the block tag
`cultivationcraft:divine_sense_visible` with a data pack. For example, add this at
`data/cultivationcraft/tags/blocks/divine_sense_visible.json`:

```json
{
  "replace": false,
  "values": ["minecraft:amethyst_cluster"]
}
```

Use `/reload` after installing the data pack; new targets are picked up by the
next scan. Targets use the bounding box of their outline shape. Blocks with
empty outline shapes are skipped. The radius, opacity and maximum number of
highlights are defined in `DivineSenseBlockRenderer`.

The Stats tab also shows live Available Qi and Available Stamina values below
the scrollable stats list. External cultivators currently use the same pool for
both costs, so their values match. Body cultivators have no stored external Qi,
so Available Qi is zero. Values show up to two decimal places without bars.

Manual checks after building and running the client:

- Spend Qi/stamina while the Stats tab is open; verify the two lower numeric
  readouts update and the remaining stats still scroll.
- Activate Divine Sense near plants, then put a wall between them and the
  player. Check faint outlines, camera rotation, distance fading, and both
  normal and Fabulous graphics. Check a dense group for excessive clutter.
- Disable Divine Sense, break plants, change dimension, or reconnect; verify
  old outlines disappear.
- Add a block through the tag and reload; verify it appears without Java changes.

No build/client launch or Java tests were run or added for this change.
