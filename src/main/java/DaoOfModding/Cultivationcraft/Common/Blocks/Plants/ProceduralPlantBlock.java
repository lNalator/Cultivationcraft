package DaoOfModding.Cultivationcraft.Common.Blocks.Plants;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.entity.ProceduralPlantBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenome;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantGenomes;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.ProceduralPlantElementConditions;
import DaoOfModding.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import DaoOfModding.Cultivationcraft.Common.Capabilities.ChunkQiSources.IChunkQiSources;
import DaoOfModding.Cultivationcraft.Common.Config;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSource;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSourceConfig;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;

public class ProceduralPlantBlock extends BushBlock implements BonemealableBlock, EntityBlock {
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 1, 3);
    public static final IntegerProperty SPECIES = IntegerProperty.create("species", 0, 63);
    public static final BooleanProperty HOST_QI = BooleanProperty.create("host_qi");

    private static final int NEIGHBOR_SCAN_RADIUS = 6;
    private static final String TAG_SPIRITUAL_GROWTH = "SpiritualGrowth";
    private static final String TAG_LEGACY_AGE = "PlantAge";

    public ProceduralPlantBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.DANDELION).noOcclusion().randomTicks());
        this.registerDefaultState(this.stateDefinition.any().setValue(TIER, 1).setValue(SPECIES, 0).setValue(HOST_QI, false));
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            Component message;
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ProceduralPlantBlockEntity plant) {
                int species = state.getValue(SPECIES);
                PlantGenome genome = PlantGenomes.getById((ServerLevel) level, species);
                String element = genome != null ? genome.qiElement().toString() : "unknown";
                int growth = plant.getSpiritualGrowth();
                int tier = plant.getTier();
                boolean host = state.getValue(HOST_QI);
                boolean hasQiData = plant.getQiHostData() != null;
                message = Component.literal("[ProcPlant] species=" + species + " element=" + element + " tier=" + tier + " growth=" + growth + " hostQi=" + host + " qiData=" + hasQiData);
            } else {
                message = Component.literal("[ProcPlant] Missing block entity");
            }
            player.displayClientMessage(message, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
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
        int whole = (int)Math.floor(growth);
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
        } else {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
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
        builder.add(TIER, SPECIES, HOST_QI);
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

        if (workingState.getValue(HOST_QI) && genome != null) {
            IChunkQiSources chunk = ChunkQiSources.getChunkQiSources(server.getChunkAt(pos));
            QiSource source;
            if (stack.hasTag() && stack.getTag().contains("QiHostData")) {
                var nbt = stack.getTag().getCompound("QiHostData");
                nbt.putLong("pos", pos.asLong());
                source = QiSource.DeserializeNBT(nbt);
            } else {
                source = new QiSource(pos, QiSourceConfig.generateRandomSize(), genome.qiElement(), QiSourceConfig.generateRandomQiStorage(), QiSourceConfig.generateRandomQiRegen());
            }
            chunk.getQiSources().add(source);
            if (level.getBlockEntity(pos) instanceof ProceduralPlantBlockEntity plant) {
                plant.setQiHostData(source.SerializeNBT());
            }
            PacketHandler.sendChunkQiSourcesToClient(server.getChunkAt(pos));
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && state.getBlock() != newState.getBlock()) {
            if (state.hasProperty(HOST_QI) && state.getValue(HOST_QI)) {
                ServerLevel server = (ServerLevel) level;
                var sources = ChunkQiSources.getQiSourcesInRange(server, new Vec3(pos.getX(), pos.getY(), pos.getZ()), 2);
                if (!sources.isEmpty()) {
                    QiSource closest = null;
                    double best = Double.MAX_VALUE;
                    for (var s : sources) {
                        double d = new Vec3(pos.getX(), pos.getY(), pos.getZ()).subtract(s.getPos().getX(), s.getPos().getY(), s.getPos().getZ()).length();
                        if (d < best) {
                            best = d;
                            closest = s;
                        }
                    }
                    if (closest != null) {
                        var chunk = ChunkQiSources.getChunkQiSources(server.getChunkAt(closest.getPos()));
                        chunk.getQiSources().remove(closest);
                        PacketHandler.sendChunkQiSourcesToClient(server.getChunkAt(closest.getPos()));
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
            float bonus = 4.0f * tier;
            if (genome != null && source.getElement().equals(genome.qiElement())) {
                bonus *= 1.5f;
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
            BlockState otherState = level.getBlockState(otherPos);
            if (otherState.getBlock() instanceof ProceduralPlantBlock) {
                int otherTier = otherState.hasProperty(TIER) ? otherState.getValue(TIER) : 1;
                BlockEntity neighbor = level.getBlockEntity(otherPos);
                if (neighbor instanceof ProceduralPlantBlockEntity otherPlant) {
                    otherTier = otherPlant.getTier();
                }
                if (otherTier > highest) {
                    highest = otherTier;
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







