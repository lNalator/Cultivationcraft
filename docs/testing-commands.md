# Plant and Qi-source testing commands

Use `/cultivation help` for syntax and argument explanations. Each of
`plantcatalog`, `giveplant`, and `qisource` also has a `help` subcommand.
Existing command permissions are unchanged (permission level 2 / cheats).

Press Tab after these prefixes, including the trailing space:

- `/cultivation plantcatalog element `: registered element IDs with element names.
- `/cultivation plantcatalog tier `: tiers 1, 2, and 3.
- `/cultivation plantcatalog filter `: element, then tier.
- `/cultivation giveplant id @s `: world catalog IDs with generated plant names,
  catalog tiers, and elements in their suggestion tooltips.
- `/cultivation giveplant filter @s `: element, then optional tier, host flag,
  and count. Optional arguments can only be omitted from the end.
- `/cultivation qisource here `: element, then optional radius, capacity scale,
  and regeneration time.
- `/cultivation qisource at `: coordinates (including `~`), then the same
  arguments as `here`.

Typing part of an element ID, such as `fire`, finds the full registered ID in
suggestions. Select the suggestion before executing. Plant ID suggestions can
also be searched by generated name; the inserted argument is still a numeric ID.

`plantcatalog` lists the variants and supports clickable rows that prepare
`giveplant` in chat without executing it. The old `plantCatalog` spelling works
as an alias.

Host flags and numeric suggestions include explanatory tooltips. Numeric
suggestions are examples, not an exhaustive restriction on valid values.
Qi-source `storage_fraction` now correctly accepts 0 through 1, matching the
source's internal capacity scale: 0 selects the configured minimum capacity,
1 the maximum. Each suggested value shows its resulting Qi capacity.
`regen_ticks` is the time for a full refill, with 20 ticks per game second.

Build and runClient manually to check Tab suggestions at each step, filtered
completion, clickable catalog entries, both catalog spellings, and
`/cultivation qisource at ~ ~ ~`. Also check that an unknown element reports an
error without creating a source. No Java tests were added or build run.
