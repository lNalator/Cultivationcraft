# Porting to Minecraft 1.20.1 (Forge 47.x)

Assessment of the port from Forge 1.19.2 (43.1.52) to Forge 1.20.1 (47.x), for Cultivationcraft and mlmAnimator. Read-only audit: nothing has been changed yet.

Sources: the migration primers 1.19.2 → 1.19.3 → 1.19.4 → 1.20 ([1](https://gist.github.com/ChampionAsh5357/c21724bafbc630da2ed8899fe0c1d226), [2](https://gist.github.com/ChampionAsh5357/163a75e87599d19ee6b4b879821953e8), [3](https://gist.github.com/ChampionAsh5357/cf818acc53ffea6f4387fe28c2977d56)), marked [P]. Items marked [K] come from general Forge knowledge and should be checked against the docs. Counts come from grep on the current code.

## Summary

| Project | Effort | Blocking points |
|---|---|---|
| mlmAnimator | M, about 2-3 days | SRG reflection (15 lookups, only fail at runtime), mutable JOML maths |
| Cultivationcraft | L, about 8-12 days | Damage system (QiDamageSource), GUI to GuiGraphics (36 files), worldgen to JSON, `Material` removal |

Order: mlmAnimator first (Cultivationcraft depends on it), then Cultivationcraft. Suggested layout: one git branch per Minecraft version (`1.20.1`), as GeckoLib does.

## Build (both projects) [K]

- ForgeGradle 5.1 → 6.0, Gradle 8, Java 17 (unchanged).
- `minecraft 'net.minecraftforge:forge:1.20.1-47.x'`, mappings `official 1.20.1`.
- `mods.toml`: `loaderVersion="[47,)"`, `minecraft` dependency `[1.20.1]`.
- `pack.mcmeta`: `pack_format` 6 → 15.
- Third-party mods, all with a Forge 1.20.1 build and no Java import in the mod: Iron Chests (14.4.x), Jade (11.x), Distant Horizons 2.x (pick the 1.20.1 file id, keep the `remapRefMap` setup under FG6).

## mlmAnimator

40 Java files, ~6.3k lines, client rendering only. No mixin, no access transformer, no registry, no GUI.

| Change | Version | Occurrences | Main files | Effort |
|---|---|---|---|---|
| `com.mojang.math.Vector3f/Vector4f/Matrix3f/Matrix4f` → `org.joml` [P] | 1.19.3 | 22 imports, 93 type refs | Quad, ExtendableCube, ExtendableModelRenderer, ParticleEmitter, MultiLimbedModel | S |
| `vec.transform(matrix)` → `vec.mul(matrix)` (JOML is mutable) [P][K] | 1.19.3 | 10 | ExtendableModelRenderer, ExtendableCube, Quad | M |
| `Vector3f.XP/YP/ZP.rotation(Degrees)` → `Axis.XP...` [K] | 1.19.3 | 15 | ExtendableModelRenderer, PoseHandler, MultiLimbedModel | S |
| `Vector3f.copy()` → `new Vector3f(v)` [K] | 1.19.3 | 3 | ExtendableCube, MultiLimbedDimensions | S |
| `ItemTransforms.TransformType` → `ItemDisplayContext` [P] | 1.19.4 | 3 + 1 call | MultiLimbedModel | S |
| `DamageSource.IN_WALL` → `source.is(DamageTypes.IN_WALL)` [P] | 1.19.4 | 1 | ServerListeners | S |
| `entity.level` → `level()` [P] | 1.20 | 8 | ClientListeners, MultiLimbedModel, MultiLimbedRenderer, ServerListeners, PlayerUtils | S |
| `isOnGround()` → `onGround()` [P] | 1.20 | 4 | PlayerPoseHandler | S |
| `RenderLevelLastEvent` → `RenderLevelStageEvent` [P] | 1.20 | 1 | ClientListeners | S |

Risks:
1. **SRG reflection**: 11 `f_`/`m_` names in MultiLimbedRenderer, ClientReflection and Reflection. They usually survive between versions, but nothing checks them at build time, and the current try/catch only logs failures. Check each one against the 1.20.1 mappings, starting with `moveTowardsClosestSpace`. An access transformer would make them fail at build time instead.
2. **Inventory rendering detection** (`ClientReflection.isRenderingInventory`): `shaderLightDirections` becomes an `org.joml.Vector3f[]`. With the old cast, the ClassCastException is swallowed and inventory rendering breaks silently.
3. **Rendering maths** (ExtendableModelRenderer, ExtendableCube, Quad, ParticleEmitter): JOML objects are changed in place, and `PoseStack.last().pose()` returns the live matrix. A misplaced `.mul()` corrupts the stack. Test limbs, quads, elytra, cape, head/skull, parrots and held items visually.
4. Layers copied from vanilla (cape, armour, held item) will miss 1.20 additions such as armour trims.

## Cultivationcraft

376 Java files.

| Change | Version | Occurrences | Main files | Effort |
|---|---|---|---|---|
| `entity.level` → `level()` [P] | 1.20 | ~188 lines, ~80 files | FlyingSwordEntity, ChanneledAttackTechnique, ServerListeners, BiteTechnique, WaterBlood | M (mechanical) |
| Entity `getLevel()` → `level()` / `serverLevel()` [P] | 1.20 | 39, 13 files | CommonListeners, PlantCatalogCommands, PlayerKnowledge | S |
| `isOnGround()` → `onGround()` [P] | 1.20 | 26, 17 files | SingleLegPart, SpreadTechnique, FlightTechnique, LeapTechnique | S |
| `new BlockPos(double...)` → `BlockPos.containing` [P] | 1.19.4 | ~25 | WaterBlood, ProceduralPlantPatchFeature, ChunkQiSources, QiSource, Physics | S |
| `Registry.X_REGISTRY` → `Registries.X` / `BuiltInRegistries` [P] | 1.19.3 | 8, 6 files | ModWorldgen, Damage, ElementalEffectPacket, IceElement, BlockRegister | S |
| JOML (`Vector3f`, `Matrix4f`, `Axis`, `mulPose`) [P] | 1.19.3 | 23 imports, ~100 uses | QiProjectileRenderer, RefinementItemRenderer, FrozenBlockEntityRenderer, TribulationRenderer | M |
| Creative tabs: `Item.Properties().tab()` → `BuildCreativeModeTabContentsEvent` [P] | 1.19.3/1.20 | 6 | ItemRegister, BlockRegister | S |
| **DamageSource → data-driven DamageType** [P] | 1.19.4 | 41 refs, 20 static `DamageSource.X`, 29 bypass/isFire/isExplosion calls, 15 `new QiDamageSource` | QiDamageSource, Damage, FallModifier, WarpModifier, ServerListeners | **L** |
| **GUI → GuiGraphics** (`blit`, `font.draw`, `blitOffset`, `fill`, `renderTooltip`, `render(PoseStack...)`) [P] | 1.19.4/1.20 | 324 PoseStack refs in 36 files | TechniqueIcons, DropdownList, Scrollbar, PlantBadgeTooltip, TextField, AlchemyCauldronScreen, BetterFontRenderer | **L** |
| `renderButton` → `renderWidget` [P] | 1.19.4 | 2 | CultivationScreen, CultivationTypeScreen | S |
| Overlay event and tooltip/decorator rendering take GuiGraphics [K] | 1.20 | ~18 | ClientListeners, PlantBadgeTooltip, FlyingSwordDecorator | S |
| `TransformType` → `ItemDisplayContext`, `renderStatic` signature [P][K] | 1.19.4 | 11 | FlyingSwordRenderer, FrozenBlockEntityRenderer, AlchemyCauldronRenderer, RefinementItemRenderer | S |
| World rendering (`RenderSystem`, Tesselator) to re-check [K] | 1.19.3-1.20 | 204 + 66 | TribulationRenderer, DivineSenseBlockRenderer, RefinementItemRenderer | M |
| `TextureStitchEvent.Pre.addSprite` removed → `atlases/blocks.json` [P][K] | 1.19.3 | 4 | ProceduralPlantTextures, ProceduralPlantRenderSetup | M |
| `ModelBakeEvent` → `ModelEvent.ModifyBakingResult` [P][K] | 1.19.3 | 1 (+ baked models) | ClientBlockRegister, ProceduralPlantBakedModel, FrozenBlockBakedModel | M |
| **`Material` removed** [P] | 1.20 | 61 refs, 12 `getMaterial()` | Elements (33), HerbivoreFoodStats, BlockElements, BiteTechnique | **M/L** |
| **Worldgen in code → datapack JSON** [P] | 1.19.3 | 27 | ModWorldgen, ModBiomeModifiers | M |
| Advancement triggers: `EntityPredicate.Composite` → `ContextAwarePredicate` [P] | 1.20 | 31, 7 files | CultivationAdvancements, Triggers/* | S/M |
| Loot JSON: `minecraft:alternative` → `any_of` [P] | 1.20 | 7 | data/cultivationcraft/loot_modifiers/*.json | S |
| `sendSuccess(Component)` → `sendSuccess(Supplier)` [P] | 1.20 | 29 | BodyforgeCommand, PlantCatalogCommands, CultiDevCommand, PillCommands, JadeSlipCommands | S |
| Packet registration consumer [P] | 1.20 | 30 | PacketHandler | S |
| Particle provider registration [P] | 1.19.4 | 2 | clientRegister | S |
| `ForgeMod.REACH_DISTANCE`/`ATTACK_RANGE` → `BLOCK_REACH`/`ENTITY_REACH` [P] | 1.20 | 2 | PlayerStatControl | S |
| Explosions: `Level.ExplosionInteraction`, `Explosion` constructor [K] | 1.19.3 | 5 | ExplosionModifier, ElementInteractions | S |
| `ArmorItem.Type` [P] | 1.19.4 | 4 | Technique | S |

The two mixins (`EntityMixin.getTeamColor`, `MinecraftMixin.shouldEntityAppearGlowing`) target methods that still exist in 1.20.1 [K]: low risk, check the refmap.

Risks (more than a rename):
1. **Damage (QiDamageSource)**: it extends `DamageSource(String msgId)` and sets bypass armour / magic / enchantments / explosion per instance. From 1.19.4 these flags are tags of a data-driven DamageType, fixed per type. This needs DamageType JSONs for the combinations used (internal, per element, explosion...), `bypasses_armor`-style tags, a `Holder<DamageType>` from `level.registryAccess()` at each of the 15 creation sites, and `source.is(...)` instead of `getMsgId()` comparisons.
2. **GUI (36 files)**: the custom widget engine (TechniqueIcons, DropdownList, Scrollbar, TextField, BetterFontRenderer) is built on static `GuiComponent.blit`, `getBlitOffset` and `font.draw`. Everything moves to GuiGraphics, with the z offset as `pose().translate`. Large but mechanical.
3. **Procedural plant rendering**: sprites must come from an atlas JSON (`assets/cultivationcraft/atlases/blocks.json`, directory source `block/procplant`) instead of `addSprite`, plus the baked model event and `AlphaOverlayTexture`.
4. **Worldgen**: configured/placed features registered in code (`Holder.hackyErase`) and the biome modifier built on `BuiltinRegistries.BIOME` become JSON (`worldgen/configured_feature`, `worldgen/placed_feature`, `forge/biome_modifier`). The `Feature` classes stay in code.
5. **`Material` removal**: Elements / BlockElements map elements to `Material.WATER`, `AIR`, `WATER_PLANT`... This becomes block tags or state checks (`getFluidState`, `canBeReplaced`), which can change gameplay, not just names.
6. **World rendering / JOML** (TribulationRenderer, DivineSenseBlockRenderer, QiProjectileRenderer, DivineSenseMenuScreen): validate visually.

## Estimate

| Part | Days |
|---|---|
| mlmAnimator (build, JOML, renames, reflection checks, visual tests) | 2-3 |
| Cultivationcraft mechanical changes (renames, `level()`, BlockPos, registries, creative tabs, loot JSON, commands, build) | 1-1.5 |
| GUI → GuiGraphics | 2-3 |
| Damage types | 1.5-2, plus gameplay tests |
| Worldgen JSON, plant atlas and baked models, `Material` | 1.5-2 |
| World rendering / JOML and visual checks | 1 |
| **Total** | **about 10-15 days** |

Next steps after 1.20.1: 1.20.5 (item data components, networking payloads, NeoForge attachments) and 1.21 (`ResourceLocation` constructor) are covered by the primers [4](https://gist.github.com/ChampionAsh5357/53b04132e292aa12638d339abfabf955) and [5](https://gist.github.com/ChampionAsh5357/d895a7b1a34341e19c80870720f9880f), on NeoForge.
