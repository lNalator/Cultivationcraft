package lNalator.Cultivationcraft.Common.Blocks.Plants;

import lNalator.Cultivationcraft.Common.Blocks.Plants.entity.ProceduralPlantBlockEntity;
import lNalator.Cultivationcraft.Common.Blocks.Plants.world.PlantGenome;
import lNalator.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import lNalator.Cultivationcraft.Common.Blocks.Plants.world.ProceduralPlantElementConditions;
import lNalator.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

public class ProceduralPlantBlock extends BushBlock implements BonemealableBlock, EntityBlock, net.minecraft.world.level.block.SimpleWaterloggedBlock {

    public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 3);
    public static final IntegerProperty SPECIES = IntegerProperty.create("species", 0, 63);
    public static final BooleanProperty WATERLOGGED = net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty HOST_QI = BooleanProperty.create("host_qi");

    private static final int NEIGHBOR_SCAN_RADIUS = 6;
    private static final String TAG_SPIRITUAL_GROWTH = "SpiritualGrowth";
    private static final String TAG_LEGACY_AGE = "PlantAge";

    public ProceduralPlantBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.DANDELION).noOcclusion().randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(TIER, 1).setValue(SPECIES, 0).setValue(HOST_QI, false).setValue(WATERLOGGED, false));
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(WATERLOGGED,
                context.getLevel().getFluidState(context.getClickedPos()).is(net.minecraft.tags.FluidTags.WATER));
    }

    @Override
    public net.minecraft.world.level.material.FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? net.minecraft.world.level.material.Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
            net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, net.minecraft.world.level.material.Fluids.WATER,
                    net.minecraft.world.level.material.Fluids.WATER.getTickDelay(level));
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        // Retain this plant's solid-floor rule instead of Forge's default plains-flower soil rule.
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    protected boolean mayPlaceOn(BlockState state, LevelReader level, BlockPos pos) {
        if (state.isAir()) {
            return false;
        }
        if (!state.isFaceSturdy(level, pos, Direction.UP)) {
            return false;
        }
        return Block.isFaceFull(state.getCollisionShape(level, pos), Direction.UP);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.isClientSide) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ProceduralPlantBlockEntity plant)) {
            return;
        }

        if (plant.getSpiritualGrowth() >= ProceduralPlantBlockEntity.MAX_SPIRITUAL_GROWTH) {
            return;
        }

        PlantGenome genome = PlantGenomes.getById(level, state.getValue(SPECIES));
        int currentTier = plant.getTier();

        float environmentModifier = ProceduralPlantElementConditions.growthModifier(level, pos, genome);
        if (environmentModifier <= 0.0f) {
            return;
        }

        float growth = environmentModifier;
        float qiBonus = computeQiBonus(level, pos, currentTier, genome);
        if (qiBonus > 0.0f) {
            growth += qiBonus * environmentModifier;
        }
        int highestNeighborTier = currentTier < 3 ? findHighestNeighborTier(level, pos, currentTier) : currentTier;
        if (highestNeighborTier > currentTier && currentTier > 0) {
            float ratio = (float) highestNeighborTier / (float) currentTier;
            if (ratio > 0.0f) {
                growth /= ratio;
            }
        }

        if (growth <= 0.0f) {
            return;
        }
        int whole = (int) Math.floor(growth);
        float fractional = growth - whole;
        if (fractional > 0.0f && random.nextFloat() < fractional) {
            whole++;
        }
        if (whole <= 0) {
            return;
        }
        plant.incrementSpiritualGrowth(whole);

        int newTier = plant.getTier();
        BlockState newState = state;
        if (newTier != state.getValue(TIER)) {
            newState = newState.setValue(TIER, newTier);
        }

        boolean shouldHostQi = newTier >= 3;
        if (newState.getValue(HOST_QI) != shouldHostQi) {
            newState = newState.setValue(HOST_QI, shouldHostQi);
        }

        if (!newState.equals(state)) {
            level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        }

        if (shouldHostQi && genome != null) {
            plant.attachQiSourceIfMissing(level, genome.qiElement());
        }
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    public boolean isValidBonemealTarget(BlockGetter level, BlockPos pos, BlockState state, boolean isClient) {
        return false;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        // Intentionally left blank: spiritual plants ignore vanilla bonemeal.
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER, SPECIES, HOST_QI, WATERLOGGED);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        stack.getOrCreateTagElement("BlockStateTag").putString("species", Integer.toString(state.getValue(SPECIES)));
        stack.getOrCreateTagElement("BlockStateTag").putString("tier", Integer.toString(state.getValue(TIER)));
        stack.getOrCreateTagElement("BlockStateTag").putString("host_qi", state.getValue(HOST_QI) ? "true" : "false");
        if (level.getBlockEntity(pos) instanceof ProceduralPlantBlockEntity plant) {
            var data = plant.getQiHostData();
            if (data != null) {
                stack.getOrCreateTag().put("QiHostData", data);
            }
            int growth = plant.getSpiritualGrowth();
            stack.getOrCreateTag().putInt(TAG_SPIRITUAL_GROWTH, growth);
            stack.getOrCreateTag().putInt(TAG_LEGACY_AGE, growth);
        }
        return stack;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }

        ServerLevel server = (ServerLevel) level;
        int species = state.getValue(SPECIES);
        PlantGenome genome = PlantGenomes.getById(server, species);
        BlockState workingState = state;

        if (level.getBlockEntity(pos) instanceof ProceduralPlantBlockEntity plant) {
            int storedGrowth = readGrowthFromItem(stack);
            if (storedGrowth >= 0) {
                plant.setSpiritualGrowth(storedGrowth);
            }

            int tier = plant.getTier();
            workingState = workingState.setValue(TIER, tier);

            boolean hostFlag = false;
            if (stack.hasTag()) {
                var tag = stack.getTag();
                if (tag.contains("QiHostData")) {
                    hostFlag = true;
                } else if (tag.contains("BlockStateTag")) {
                    var bst = tag.getCompound("BlockStateTag");
                    hostFlag = "true".equalsIgnoreCase(bst.getString("host_qi"));
                }
            }
            if (tier < 3) {
                hostFlag = false;
            }
            workingState = workingState.setValue(HOST_QI, hostFlag);
        }

        if (!workingState.equals(state)) {
            level.setBlock(pos, workingState, Block.UPDATE_CLIENTS);
            workingState = level.getBlockState(pos);
        }

        if (workingState.getValue(HOST_QI) && genome != null
                && level.getBlockEntity(pos) instanceof ProceduralPlantBlockEntity plant) {
            if (stack.hasTag() && stack.getTag().contains("QiHostData")) {
                plant.setQiHostData(stack.getTag().getCompound("QiHostData"));
            }
            plant.attachQiSourceIfMissing(server, genome.qiElement());
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (level instanceof ServerLevel server && state.getBlock() != newState.getBlock()) {
            var chunk = server.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
            if (chunk != null && ChunkQiSources.getChunkQiSources(chunk).getQiSources()
                    .removeIf(source -> source.isPlantOwned() && source.getPos().equals(pos))) {
                chunk.setUnsaved(true);
                PacketHandler.sendChunkQiSourcesToClient(chunk);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProceduralPlantBlockEntity(pos, state);
    }

    private float computeQiBonus(ServerLevel level, BlockPos pos, int tier, PlantGenome genome) {
        if (tier <= 0) {
            return 0.0f;
        }
        int radius = Config.Server.procPlantQiGrowthRadius();
        var sources = ChunkQiSources.getQiSourcesInRange(level, new Vec3(pos.getX(), pos.getY(), pos.getZ()), radius);
        if (sources.isEmpty()) {
            return 0.0f;
        }
        float best = 0.0f;
        for (var source : sources) {
            float bonus = (float) Config.Server.procPlantGrowthBoostQiAny() * tier;
            if (genome != null && source.getElement().equals(genome.qiElement())) {
                bonus = (float) Config.Server.procPlantGrowthBoostQiMatch() * tier;
            }
            if (bonus > best) {
                best = bonus;
            }
        }
        return best;
    }

    private int findHighestNeighborTier(ServerLevel level, BlockPos pos, int selfTier) {
        int highest = selfTier;
        for (BlockPos otherPos : BlockPos.betweenClosed(pos.offset(-NEIGHBOR_SCAN_RADIUS, -1, -NEIGHBOR_SCAN_RADIUS), pos.offset(NEIGHBOR_SCAN_RADIUS, 1, NEIGHBOR_SCAN_RADIUS))) {
            if (otherPos.equals(pos)) {
                continue;
            }
            var chunk = level.getChunkSource().getChunkNow(otherPos.getX() >> 4, otherPos.getZ() >> 4);
            if (chunk == null) {
                continue;
            }
            BlockState otherState = chunk.getBlockState(otherPos);
            if (otherState.getBlock() instanceof ProceduralPlantBlock) {
                int otherTier = otherState.hasProperty(TIER) ? otherState.getValue(TIER) : 1;
                if (otherTier > highest) {
                    highest = otherTier;
                    if (highest == 3) {
                        return highest;
                    }
                }
            }
        }
        return highest;
    }

    private int readGrowthFromItem(ItemStack stack) {
        if (!stack.hasTag()) {
            return -1;
        }
        var tag = stack.getTag();
        if (tag.contains(TAG_SPIRITUAL_GROWTH)) {
            return tag.getInt(TAG_SPIRITUAL_GROWTH);
        }
        if (tag.contains(TAG_LEGACY_AGE)) {
            return tag.getInt(TAG_LEGACY_AGE);
        }
        return -1;
    }
}
