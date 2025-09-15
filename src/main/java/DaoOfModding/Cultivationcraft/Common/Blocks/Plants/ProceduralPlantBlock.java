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

        int age = state.getValue(AGE);
        if (age >= 3) return;

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
                level.setBlock(pos, state.setValue(AGE, age + 1), 2);
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
        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && state.getValue(HOST_QI)) {
            var srv = (ServerLevel) level;
            int species = state.getValue(SPECIES);
            var g = PlantGenomes.getById(srv, species);
            if (g != null) {
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
        float mult = 1.0f;
        String path = genome.qiElement().getPath();
        float temp = level.getBiome(pos).value().getBaseTemperature();

        if (path.contains("fire")) {
            if (temp > 0.8f) mult *= (float)Config.Server.fireHotMult();
            else mult *= (float)Config.Server.fireColdMult();
        }
        if (path.contains("water")) {
            if (nearWater(level, pos, 3)) mult *= (float)Config.Server.waterNearMult();
            else mult *= (float)Config.Server.waterFarMult();
        }
        if (path.contains("ice")) {
            if (temp < 0.3f) mult *= (float)Config.Server.iceColdMult();
            else mult *= (float)Config.Server.iceWarmMult();
        }
        if (path.contains("wind") || path.contains("air")) {
            int sea = level.getSeaLevel();
            if (pos.getY() > sea + 40) mult *= (float)Config.Server.windHighAltMult();
            else mult *= (float)Config.Server.windLowAltMult();
        }
        if (path.contains("lightning")) {
            int sky = level.getMaxLocalRawBrightness(pos);
            if (sky >= 12) mult *= (float)Config.Server.lightningBrightMult();
            else mult *= (float)Config.Server.lightningDimMult();
        }
        if (path.contains("earth")) {
            var below = level.getBlockState(pos.below());
            if (below.is(Blocks.STONE) || below.is(Blocks.DEEPSLATE) || below.is(BlockTags.DIRT)) mult *= (float)Config.Server.earthGoodGroundMult();
            else mult *= (float)Config.Server.earthBadGroundMult();
        }
        if (path.contains("wood")) {
            int light = level.getMaxLocalRawBrightness(pos);
            if (light >= 9) mult *= (float)Config.Server.woodGoodLightMult();
            else mult *= (float)Config.Server.woodBadLightMult();
        }

        // Qi source proximity boosts
        if (!level.isClientSide) {
            var srv = (ServerLevel) level;
            int radius = Config.Server.procPlantQiGrowthRadius();
            var sources = ChunkQiSources.getQiSourcesInRange(srv, new Vec3(pos.getX(), pos.getY(), pos.getZ()), radius);
            boolean any = false; boolean match = false;
            for (var s : sources) { any = true; if (s.getElement().equals(genome.qiElement())) { match = true; break; } }
            if (match) mult *= 1.8f; else if (any) mult *= 1.2f;
        }

        return mult;
    }

    private float tierGrowthModifier(Level level, BlockPos pos, PlantGenome g) {
        float mult = switch (g.tier()) { case 3 -> 0.35f; case 2 -> 0.6f; default -> 1.0f; };
        // Penalty if higher-tier plants nearby "feed" on qi
        int radius = 6;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-radius, -1, -radius), pos.offset(radius, 1, radius))) {
            var st = level.getBlockState(p);
            if (st.getBlock() instanceof ProceduralPlantBlock) {
                int species2 = st.getValue(SPECIES);
                if (!level.isClientSide) {
                    var g2 = PlantGenomes.getById((ServerLevel) level, species2);
                    if (g2 != null && g2.tier() > g.tier()) { mult *= 0.7f; break; }
                }
            }
        }
        return mult;
    }

    private boolean nearWater(Level level, BlockPos center, int radius) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++)
            for (int dz = -radius; dz <= radius; dz++)
                for (int dy = -1; dy <= 1; dy++) {
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    var st = level.getBlockState(m);
                    if (st.getBlock() == Blocks.WATER || st.getFluidState().isSource()) return true;
                }
        return false;
    }
}
