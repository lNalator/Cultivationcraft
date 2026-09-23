Understood: **reviewed pill list only for now**. Pill tier determines both the recipe’s difficulty and its minimum plant requirement; elemental score alone cannot replace that requirement.

I’d use these rules:

| Pill tier | Required plants | Ingredient variety |
|---|---|---:|
| T1 | At least one T1 plant | 2–3 variants |
| T2 | At least one T2 plant | 4–5 variants |
| T3 | At least one T3 plant | 6–8 variants |
| T4 | **Two T3 plants, or one T3 plant hosting a Qi source** | 9 variants |

For T4, I’d count two consumed T3 plants even if they come from the same stack; that stack still represents only one ingredient variant. A Qi-source plant’s **SpiritualGrowth contributes to the score**, while its source reserve remains separate.

The revised targets below use actual **SpiritualGrowth × consumed quantity**. Since T2 starts at 100 and T3 at 1,000, the principal ingredient requirements now reflect that.

**Healing pills**

All healing percentages refer to maximum HP.

| Pill | Tier | Required elemental scores | Refining qi | Effect |
|---|---|---|---:|---|
| Blood Mending | T1 | Wood 20, Earth 10 | 50 | Instantly restore 25% HP |
| Green Sprout | T1 | Wood 30, Wind 10 | 60 | Restore 40% HP over 10 seconds |
| Vital Essence | T2 | Wood 150, Wind 50, Lightning 10 | 150 | Restore 45% HP; cleanse poison and bleeding |
| Verdant Renewal | T2 | Wood 180, Water 60, Earth 60 | 180 | Restore 70% HP over 20 seconds |
| Jade Marrow | T3 | Wood 1,200, Earth 300, Wind 200, Lightning 100 | 450 | Restore 70% HP; grant absorption equal to 10% HP for 30 seconds |
| Earthen Bone-Knitting | T3 | Wood 1,200, Earth 500, Water 250, Lightning 150 | 520 | Restore 50% HP; cleanse eligible debuffs; reduce damage by 25% for 20 seconds |
| Nine-Turns Life | T4 Incredible | Wood 3,000, Earth 1,200, Wind 600, Lightning 400 | 1,500 | Full heal; regenerate 2% HP/second for 30 seconds |
| Rebirth Lotus | T4 Mythical | Wood 6,000, Water 3,000, Earth 1,500, Lightning 1,000, Wind 500 | 4,500 | Full heal; prevent one death within 10 minutes and restore 50% HP |

Wood remains the main healing ingredient. Earth supports protection, Water supports sustained restoration, and Lightning becomes increasingly important for exceptional effects.

**Qi restoration pills**

Percentages refer to maximum stored qi.

| Pill | Tier | Required elemental scores | Refining qi | Effect |
|---|---|---|---:|---|
| Spirit Recovery | T1 | Water 20, Fire 10 | 50 | Instantly restore 10% qi |
| Dewdrop | T1 | Water 30, Wind 10 | 60 | Restore 15% qi over 10 seconds |
| Qi Replenishing | T2 | Water 150, Fire 50, Wind 20 | 150 | Instantly restore 30% qi |
| Flowing Stream | T2 | Water 180, Wind 90, Lightning 10 | 180 | Triple passive qi regeneration for 30 seconds |
| Ocean Heart | T3 | Water 1,200, Fire 300, Wind 200, Lightning 100 | 450 | Instantly restore 70% qi |
| Void Well | T3 | Water 1,200, Wind 500, Ice 250, Lightning 150 | 520 | Restore 50% qi; reduce eligible technique costs by 25% for 30 seconds |
| Heavenly Spring | T4 Incredible | Water 3,000, Wind 1,200, Lightning 400, Ice 300, Fire 300 | 1,500 | Full qi; double passive regeneration for 60 seconds |
| Boundless Sea | T4 Mythical | Water 6,000, Wind 3,000, Lightning 1,200, Ice 1,200, Fire 600 | 4,500 | Full qi; eligible techniques cost no qi for 20 seconds, then triple regeneration for 60 seconds |

I’d continue excluding **Qi Transfer** from technique-cost discounts, preserving the energy cost of feeding plants and refining pills.

**Cultivation pills**

These accept all eight elements and require a minimum **combined score**, alongside their tier and ingredient-variety requirements. The crafted pill retains the element with the highest contribution.

The progress rewards below remain my proposed values for the mod’s current progression—not the larger values from the original study.

| Pill | Tier | Minimum combined score | Refining qi | Proposed effect |
|---|---|---:|---:|---|
| Qi Gathering | T1 | 30 | 50 | +5 cultivation progress |
| Lingering Dew | T1 | 40 | 60 | +1 qi absorption speed for 3 minutes |
| Spirit Nourishing | T2 | 200 | 150 | +30 cultivation progress |
| Meridian Tempering | T2 | 300 | 190 | +45 cultivation progress over 3 minutes |
| Dao Insight | T3 | 1,500 | 450 | +90 cultivation progress |
| Core Condensing | T3 | 2,000 | 540 | +120 cultivation progress; increased tolerance penalty |
| Heaven-Opening | T4 Incredible | 5,000 | 1,500 | +300 cultivation progress; +20% absorption of the pill’s element for 5 minutes |
| Dao Origin | T4 Mythical | 12,000 | 4,500 | +1,000 cultivation progress; permanent +1 absorption speed, capped at +5 per player |

For example, a Dao Insight batch containing **Wood 1,100, Water 300, and Earth 150** produces a **Wood-affinity Dao Insight Pill**, provided its plant-tier and ingredient-variety requirements are also satisfied.

I’d preserve these safeguards:

- Cultivation progress respects element compatibility and the current stage’s capacity.
- Pills do not automatically satisfy breakthrough quests, technique mastery, or tribulations.
- Temporary effects refresh instead of stacking.
- A tied dominant score requires choosing among the tied elements before refining.
- Tolerance remains undefined; Core Condensing’s additional penalty needs that system specified first.

Your failure formula can remain unchanged:

```text
Failure % = clamp(
    5 + 8×(pill difficulty−1)
      + 8×(pill difficulty−cauldron tier)
      + 6×impurity,
    2, 90
)
```

Missing the required plant tier should be a **recipe requirement failure**, regardless of how high the elemental scores are. Oversupplying otherwise valid ingredients contributes impurity.

One remaining balance issue is refining duration: **4,500 qi takes 75 minutes at the starting transfer rate**, or **3 minutes 45 seconds at 20 transfers/second**. That makes Mythical pills strongly dependent on Qi Transfer training. I’d retain those costs if that dependency is intentional.

The optional **Bigu Pill** can remain T1: Earth 20 + Wood 10, 40 refining qi, restoring 4 hunger and 4 saturation for body cultivators.

One rule still needs your preference: **can a higher-tier plant satisfy a lower-tier pill’s plant requirement—for example, a T3 plant replacing the required T2 plant?** I recommend allowing it, with excess growth still contributing impurity.


## Answers to your questions:

First :

- I agree, T4 should count 2 T3 plants even from the same stack, T3 plants are rare enough
- I agree, excluding Qi Transfer would make sure the cultivator is not using the pills tu "cheese" the process of plant handling
- Qi Gathering should give more than only 5, as it would not be worth the hassle, 5 cultivation progress is really easily attainable, let it give more cultivation progress, maybe 50 but is only usefull for certain stages (core formation and golden core cultivator would not care as it would just give nothing). Same point for Spirit Nourishing & Dao Insight, please rename the pills to match the average stage the pill will be able to be used in.
- Meridian Tempering should have the same, increased effect as Lingering Dew = qi absorption 2 for example.
- The cap on the stat increase is a good addition, I agree with the safeguards, a tied dominant score would give equal rate of getting either element when refining, as a risk. The pills should have a quite large cooldown based on their effectiveness to prevent spam usage of good pills.
- Please also add usage of "Normal" tier plants in pills as a base,it should not be NEEDED persay for most recipe and its presence does not have to be ennormous contrary to the other elements, but only serve as a base, having the right amount of neutral element in a pill alchemy process would give a slight boost to the success rate/purity of the pill.
- I agree with the missed recipe req failure penality
- You are right lets tone down a little bit qi req for high tiered pills for now.
- Bigu pill is a good adition, instead of earth take neutral plant type to serve as a simple recipe for early game.
- Yes for the replacement from T3 to T2 for example.



Before implementing resume the plant for now. I suggest for the creation to base ourselves on the model of procedural plants as many parameters could change from 1 pill to another. Pill name should be generated from a catalog of names, of course the lower Tiered pills should only allow a few array of unimpressive name to chose from


## Second part of plan

Here is the consolidated plan before implementation. **No code changes yet.** Numbers introduced below are proposed balancing defaults.

**Pills will use a generated catalog**, following the procedural-plant approach. Each catalog entry will define its name, tier, effect, recipe, refining cost, cooldown, and cultivation restrictions.

The mechanical identity stays separate from its generated name. For example, a Foundation progression pill could receive different names between worlds while retaining a clearly described purpose. Names remain stable within a world.

| Tier | Naming style | Example generated names |
|---|---|---|
| T1 | Small pool of simple names | Root Pellet, Clear Dew Pill, Gentle Sprout Pill |
| T2 | Broader, more refined vocabulary | Jade Meridian Pill, Flowing Spirit Pill |
| T3 | Strong cultivation imagery | Azure Core Pill, Thunder Marrow Pill |
| T4 Incredible | Rare, prestigious combinations | Nine-Turn Heavenly Spring Pill |
| T4 Mythical | Distinctive legendary names | Primordial Lotus of Rebirth |

Generation should follow the pill’s **effect family, tier, and intended realm**, keeping names coherent. Tooltips will always reveal the actual effect and restrictions.

**Ingredient requirements are now:**

| Pill tier | Minimum plant requirement | Ingredient variety |
|---|---|---:|
| T1 | One T1-or-higher plant | 2–3 variants |
| T2 | One T2-or-higher plant | 4–5 variants |
| T3 | One T3 plant | 6–8 variants |
| T4 | Two T3 plants, **including from one stack**, or one T3 Qi-source plant | 9 variants |

A higher-tier plant can satisfy a lower-tier requirement. Its entire consumed growth contributes to the recipe and can create excess-score impurity.

Elemental scores remain **SpiritualGrowth × consumed quantity**. Qi-source reserves remain separate; possessing a source satisfies the special ingredient condition without adding that reserve to the elemental score.

**Cultivation pills will have stronger effects and explicit realm limits.** These are their mechanical family names; generated names will use the corresponding realm vocabulary.

| Family | Tier | Proposed effect | Effective realm | Shared cultivation-pill cooldown |
|---|---|---|---|---:|
| Foundation Nourishing | T1 | +50 cultivation progress | Foundation only | 5 minutes |
| Foundation Absorption | T1 | Qi Absorption I: +1 absorption speed for 3 minutes | Foundation | 5 minutes |
| Qi Condensation Nourishing | T2 | +200 cultivation progress | Qi Condensation only | 10 minutes |
| Meridian Absorption | T2 | Qi Absorption II: +2 absorption speed for 3 minutes | Foundation and Qi Condensation | 10 minutes |
| Core Formation Insight | T3 | +500 cultivation progress | Core Formation only | 15 minutes |
| Core Condensing | T3 | +750 cultivation progress | Core Formation only | 20 minutes |
| Heaven-Opening | T4 Incredible | +1,500 progress; elemental absorption bonus for 5 minutes | Core Formation; future higher realms require explicit balancing | 30 minutes |
| Dao Origin | T4 Mythical | +3,000 progress; permanent +1 absorption speed | Core Formation; future higher realms require explicit balancing | 60 minutes |

Foundation pills will provide **no benefit in Core Formation or Golden Core**. Attempting to consume an ineffective pill should explain the restriction and preserve the item.

Progress respects the current stage’s remaining capacity. It does not automatically complete mastery requirements, quests, tribulations, or breakthroughs. The permanent absorption bonus remains capped at **+5 per player**.

I would leave Core Condensing’s previously suggested tolerance penalty out of the first implementation: the long cooldown already controls repetition, and tolerance still lacks agreed rules.

**Cultivation affinity is decided during refining.**

- The highest contributing eligible element becomes the pill’s stored affinity.
- Tied highest elements receive **equal probability**: two tied elements each have 50%, three each have one-third.
- The result is rolled once when the pill is produced.
- Consumption respects the cultivator’s elemental compatibility.

For neutral ingredients, I’m treating **“Normal” as the existing None/neutral element**. I propose that neutral contributes to stabilization rather than competing for cultivation affinity. Cultivation recipes would therefore require their minimum score from non-neutral elements, preventing neutral ingredients alone from producing an elemental cultivation pill.

**Neutral plants become an optional stabilizing ingredient for most recipes.**

A starting balance could be:

- Ideal neutral score: **10% of the recipe’s required non-neutral score**, with a minimum of 10.
- Between **75% and 125%** of that ideal amount: full stabilization benefit.
- Full benefit: **−3 percentage points of failure chance** and **+5 percentage points of displayed purity**, capped at 100%.
- Below that band, the benefit increases gradually from zero.
- Above it, the benefit decreases, reaching zero at twice the ideal amount. Excess neutral score also contributes impurity.
- Omitting neutral provides neither a stabilization bonus nor a missing-ingredient penalty.

Neutral occupies one of the existing nine slots and counts toward ingredient variety. The purity bonus would initially describe batch quality; any additional potency bonus should be explicitly balanced before adding it.

**Bigu remains the simple exception:** neutral is mandatory.

| Recipe | Refining qi | Effect | Cooldown |
|---|---:|---|---:|
| Neutral 20 + Wood 10 | 20 | Restore 4 hunger and 4 saturation | 30 seconds |

Its required neutral ingredient would not also earn the optional-neutral bonus. Initially, its food effect applies to body cultivators.

**Healing and qi-restoration effects remain as reviewed**, with shared cooldowns per family to prevent alternating differently named pills to bypass restrictions.

| Tier | Healing-family cooldown | Qi-restoration-family cooldown |
|---|---:|---:|
| T1 | 45 seconds | 60 seconds |
| T2 | 2 minutes | 3 minutes |
| T3 | 5 minutes | 7 minutes |
| T4 Incredible | 15 minutes | 20 minutes |
| T4 Mythical | 30 minutes | 30 minutes |

Stronger variants can have longer individual cooldowns. Temporary effects refresh rather than stack, and qi-cost discounts **exclude Qi Transfer**.

**Refining costs will be reduced**, especially for exceptional pills:

| Tier | Proposed refining qi | Starting transfer time |
|---|---:|---:|
| T1 | 30–50 | 30–50 seconds |
| T2 | 100–150 | 1 minute 40 seconds–2 minutes 30 seconds |
| T3 | 250–350 | 4 minutes 10 seconds–5 minutes 50 seconds |
| T4 Incredible | 600 | 10 minutes |
| T4 Mythical | 1,200 | 20 minutes |

Training Qi Transfer shortens those times. At 20 transfers per second, the two T4 costs take 30 and 60 seconds respectively.

**Batch outcomes retain the agreed failure model:**

```text
Failure % = clamp(
    5 + 8×(difficulty−1)
      + 8×(difficulty−cauldron tier)
      + 6×impurity
      − neutral stabilization bonus,
    2, 90
)
```

Missing mandatory scores, ingredient variety, or plant-tier requirements causes a guaranteed failed batch if refining is attempted. Failed batches consume their committed ingredients and refining qi, producing alchemy remnants.

The interface should preview the selected recipe, requirements, elemental scores, neutral stabilization, required refining qi, and estimated failure chance. This also makes the generated catalog understandable without requiring players to memorize what each generated name means.