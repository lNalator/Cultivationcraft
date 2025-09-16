package DaoOfModding.Cultivationcraft.Common.Blocks.Plants;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import DaoOfModding.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import DaoOfModding.Cultivationcraft.Common.Capabilities.ChunkQiSources.IChunkQiSources;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSource;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSourceConfig;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import DaoOfModding.Cultivationcraft.Common.Config;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.entity.ProceduralPlantBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantCatalogSavedData;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenome;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public class ProceduralPlantBlock extends BushBlock implements BonemealableBlock, EntityBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3; // 0..3; adjust to genome at runtime
    public static final IntegerProperty SPECIES = IntegerProperty.create("species", 0, 63); // id in catalog
    public static final BooleanProperty HOST_QI = BooleanProperty.create("host_qi");

    public ProceduralPlantBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.DANDELION).noOcclusion().randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(SPECIES, 0).setValue(HOST_QI, false));
    }

    protected boolean mayPlaceOn(BlockState state, LevelReader level, BlockPos pos) {
        // soil check: grass/dirt/farmland etc.
        return state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true; // growth on random ticks
    }

    public void randomTick(BlockState state, Level level, BlockPos pos, RandomSource rng) {
        if (level.isClientSide) return;

        // Increment dynamic age stored on BlockEntity and enforce Tier-3 host rule
        ProceduralPlantBlockEntity be = (ProceduralPlantBlockEntity) level.getBlockEntity(pos);
        if (be != null) {
            be.incrementAge(1);
            int dynTier = be.dynamicTier();
            if (dynTier >= 3 && !state.getValue(HOST_QI)) {
                level.setBlock(pos, state.setValue(HOST_QI, true), 3);
                var srv = (ServerLevel) level;
                int species = state.getValue(SPECIES);
                var g0 = PlantGenomes.getById(srv, species);
                if (g0 != null) be.attachQiSourceIfMissing(srv, g0.qiElement());
            }
            // Sync BE age for client HUD
            level.sendBlockUpdated(pos, state, state, 3);
        }

        int growthStage = state.getValue(AGE);
        if (growthStage >= 3) return;

        int species = state.getValue(SPECIES);
        var g = level.isClientSide ? PlantGenomes.forWorldPos(level, pos)
                : PlantGenomes.getById((ServerLevel) level, species);

        // simple growth rules
        int light = level.getMaxLocalRawBrightness(pos);
        boolean okLight = g.prefersShade() ? light >= 6 : light >= 9;

        if (okLight) {
            float chance = g.growthChance();
            chance *= elementGrowthModifier(level, pos, g);
            chance *= tierGrowthModifier(level, pos, g);
            if (rng.nextFloat() < chance) {
                level.setBlock(pos, state.setValue(AGE, growthStage + 1), 2);
            }
        }
    }

    @Override
    public boolean isBonemealSuccess(Level lvl, RandomSource rng, BlockPos pos, BlockState state) {
        return true;
    }

    public void performBonemeal(Level level, RandomSource rng, BlockPos pos, BlockState state) {
        
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, SPECIES, HOST_QI);
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter blockGetter, BlockPos blockPos, BlockState blockState,
            boolean isClient) {
        return blockState.getValue(AGE) < 3;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource source, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        if (age < 3) {
            level.setBlock(pos, state.setValue(AGE, age + 1), 2);
        }
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        // Middle-click pick block: preserve species in BlockStateTag so the item can show name
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        stack.getOrCreateTagElement("BlockStateTag").putString("species", Integer.toString(state.getValue(SPECIES)));
        stack.getOrCreateTagElement("BlockStateTag").putString("host_qi", state.getValue(HOST_QI) ? "true" : "false");
        if (level.getBlockEntity(pos) instanceof ProceduralPlantBlockEntity be) {
            var data = be.getQiHostData();
            if (data != null) stack.getOrCreateTag().put("QiHostData", data);
            stack.getOrCreateTag().putInt("PlantAge", be.getAge());
        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            var srv = (ServerLevel) level;
            int species = state.getValue(SPECIES);
            var g = PlantGenomes.getById(srv, species);

            // Restore BE dynamic age from item NBT if present
            if (level.getBlockEntity(pos) instanceof ProceduralPlantBlockEntity be) {
                if (stack.hasTag() && stack.getTag().contains("PlantAge")) {
                    be.setAge(stack.getTag().getInt("PlantAge"));
                }
                // If item was T3 by age but host flag was false, enforce T3 host rule on placement
                if (be.dynamicTier() >= 3 && !state.getValue(HOST_QI)) {
                    level.setBlock(pos, state.setValue(HOST_QI, true), 3);
                }
            }

            // Attach QiSource if HOST_QI true after placement
            if (state.getValue(HOST_QI) && g != null) {
                IChunkQiSources chunk = ChunkQiSources.getChunkQiSources(srv.getChunkAt(pos));
                QiSource source;
                if (stack.hasTag() && stack.getTag().contains("QiHostData")) {
                    var nbt = stack.getTag().getCompound("QiHostData");
                    // rewrite pos on restore
                    nbt.putLong("pos", pos.asLong());
                    source = QiSource.DeserializeNBT(nbt);
                } else {
                    source = new QiSource(pos, QiSourceConfig.generateRandomSize(), g.qiElement(), QiSourceConfig.generateRandomQiStorage(), QiSourceConfig.generateRandomQiRegen());
                }
                chunk.getQiSources().add(source);
                if (level.getBlockEntity(pos) instanceof ProceduralPlantBlockEntity be) {
                    be.setQiHostData(source.SerializeNBT());
                }
                PacketHandler.sendChunkQiSourcesToClient(srv.getChunkAt(pos));
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            if (state.hasProperty(HOST_QI) && state.getValue(HOST_QI)) {
                // Remove nearest matching QiSource at this position
                var srv = (ServerLevel) level;
                var sources = ChunkQiSources.getQiSourcesInRange(srv, new net.minecraft.world.phys.Vec3(pos.getX(), pos.getY(), pos.getZ()), 2);
                if (!sources.isEmpty()) {
                    // remove the closest
                    QiSource closest = null;
                    double best = Double.MAX_VALUE;
                    for (var s : sources) {
                        double d = new Vec3(pos.getX(), pos.getY(), pos.getZ()).subtract(s.getPos().getX(), s.getPos().getY(), s.getPos().getZ()).length();
                        if (d < best) { best = d; closest = s; }
                    }
                    if (closest != null) {
                        var chunk = ChunkQiSources.getChunkQiSources(srv.getChunkAt(closest.getPos()));
                        chunk.getQiSources().remove(closest);
                        PacketHandler.sendChunkQiSourcesToClient(srv.getChunkAt(closest.getPos()));
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProceduralPlantBlockEntity(pos, state);
    }

    private float elementGrowthModifier(Level level, BlockPos pos, PlantGenome genome) {
        // Only qiSource proximity aids plant growth per new rules
        float mult = 1.0f;
        if (!level.isClientSide) {
            var srv = (ServerLevel) level;
            int radius = Config.Server.procPlantQiGrowthRadius();
            var sources = ChunkQiSources.getQiSourcesInRange(srv, new Vec3(pos.getX(), pos.getY(), pos.getZ()), radius);
            boolean any = false; boolean match = false;
            for (var s : sources) { any = true; if (s.getElement().equals(genome.qiElement())) { match = true; break; } }
            if (match) mult *= (float)Config.Server.procPlantGrowthBoostQiMatch(); else if (any) mult *= (float)Config.Server.procPlantGrowthBoostQiAny();
        }
        return mult;
    }

    private float tierGrowthModifier(Level level, BlockPos pos, PlantGenome g) {
        // Use dynamic tier derived from per-plant age instead of static genome tier
        int selfTier = 1;
        if (!level.isClientSide) {
            var be = (ProceduralPlantBlockEntity) level.getBlockEntity(pos);
            if (be != null) selfTier = be.dynamicTier();
        }
        float mult = switch (selfTier) { case 3 -> 0.35f; case 2 -> 0.6f; default -> 1.0f; };

        // Penalty if nearby higher-tier plants (dynamic) are present
        int radius = 6;
        if (!level.isClientSide) {
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
                var st = level.getBlockState(p);
                if (st.getBlock() instanceof ProceduralPlantBlock) {
                    var be2 = (ProceduralPlantBlockEntity) level.getBlockEntity(p);
                    int t2 = 1;
                    if (be2 != null) t2 = be2.dynamicTier();
                    else {
                        // Fallback to genome tier if BE missing
                        int species2 = st.getValue(SPECIES);
                        var g2 = PlantGenomes.getById((ServerLevel) level, species2);
                        if (g2 != null) t2 = g2.tier();
                    }
                    if (t2 > selfTier) { mult *= 0.7f; break; }
                }
            }
        }
        return mult;
    }
}
