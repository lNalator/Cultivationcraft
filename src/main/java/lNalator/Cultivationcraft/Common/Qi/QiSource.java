package lNalator.Cultivationcraft.Common.Qi;

import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Elements.Element;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class QiSource {

    protected BlockPos pos;
    protected int range;
    protected ResourceLocation Element;

    protected double qiMax;
    protected double qiCurrent;

    protected int qiRegen;

    protected double previousCurrent = 0;

    protected boolean toUpdate = true;
    private boolean plantOwned;

    public boolean isPlantOwned() {
        return plantOwned;
    }

    public void setPlantOwned() {
        plantOwned = true;
    }

    public static final int minSpawnTime = 30;
    public static final int maxSpawnTime = 10;

    // Client only: last game tick a particle was considered for this source
    public long spawnedTick = -1;

    protected HashMap<UUID, Double> currentAbsorbing = new HashMap<UUID, Double>();
    protected HashMap<UUID, Double> absorbing = new HashMap<UUID, Double>();

    public QiSource(BlockPos position, int size, ResourceLocation element, double qi, int regen) {
        this(position, size, element, qi, 1, regen);
    }

    public QiSource(BlockPos position, int size, ResourceLocation element, double qi, double current, int regen) {
        pos = position;
        range = size;
        Element = element;

        qiMax = qi;
        qiCurrent = current;
        // Sources saved before the reserve rework could refill in a minute
        qiRegen = Math.max(regen, QiSourceConfig.MaxRegen);
    }

    public BlockPos getPos() {
        return pos;
    }

    public int getSize() {
        return range;
    }

    public ResourceLocation getElement() {
        return Element;
    }

    public int getQiCurrent() {
        return (int) (qiCurrent * getQiMax());
    }

    // Return how many ticks should wait before a QiParticle spawn
    public int getSpawnTick() {
        return (int) ((1 - qiCurrent) * minSpawnTime);
    }

    public int getQiMax() {
        return (int) (qiMax * (QiSourceConfig.MaxReserve - QiSourceConfig.MinReserve)) + QiSourceConfig.MinReserve;
    }

    // Absorption speed multiplier from the Qi the source holds now: its size and how full it is
    public double getDensityBonus() {
        double bonus = Math.sqrt(getQiCurrent() / (QiSourceConfig.MinReserve / 2.0));

        return Math.max(Config.Server.qiSourceMinBonus(), Math.min(Config.Server.qiSourceMaxBonus(), bonus));
    }

    // Density tier shown to the player: 0 poor, 1 normal, 2 rich, 3 abundant
    public int getDensityTier() {
        double bonus = getDensityBonus();

        return bonus < 0.75 ? 0 : bonus < 1.5 ? 1 : bonus < 2.5 ? 2 : 3;
    }

    // Reserve size shown to the player: 0 small (about 80% of sources), 1 medium, 2 large, 3 huge
    public int getSizeTier() {
        return qiMax < 0.1 ? 0 : qiMax < 0.4 ? 1 : qiMax < 0.8 ? 2 : 3;
    }

    // Body cultivation counts Qi in the old reserve unit
    public double absorbBodyQi(double toAbsorb, Player player) {
        return absorbQi(toAbsorb / QiSourceConfig.BODY_QI_SCALE, player) * QiSourceConfig.BODY_QI_SCALE;
    }

    public HashMap<UUID, Double> getAbsorbingPlayers() {
        return currentAbsorbing;
    }

    public BlockPos getRandomPos() {
        Vec3 randomPos;

        // A non-positive size would never satisfy the loop below
        if (getSize() <= 0) {
            return pos;
        }

        do {
            randomPos = new Vec3((Math.random() * getSize() * 2) - getSize(), Math.random() * getSize() * 2 - getSize(), Math.random() * getSize() * 2 - getSize());
        } while (randomPos.length() > getSize());

        return BlockPos.containing(pos.getX() + randomPos.x, pos.getY() + randomPos.y, pos.getZ() + randomPos.z);
    }

    // Apply elemental effects to all blocks in range
    public void effectAllBlocks(Level level) {
        Element element = Elements.getElement(getElement());

        for (int x = -range; x <= range; x++) {
            for (int y = -range; y <= range; y++) {
                for (int z = -range; z <= range; z++) {
                    if (new Vec3(x, y, z).length() <= range) {
                        element.effectBlock(level, new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z));
                    }
                }
            }
        }
    }

    public boolean tick(Level level) {
        Element element = Elements.getElement(getElement());

        if (!level.isClientSide && element.shouldDoBlockEffect() && Config.Server.qiSourceElementalEffectsOn()) {
            element.effectBlock(level, getRandomPos());
        }

        boolean update = toUpdate;
        toUpdate = false;

        // Tell clients when absorption stops too, or a full source would keep streaming to them
        if (!currentAbsorbing.isEmpty() && absorbing.isEmpty()) {
            update = true;
        }

        // Clear the list of players absorbing from this source
        currentAbsorbing = absorbing;
        absorbing = new HashMap<UUID, Double>();

        // A submerged non-water plant retains its reserve but cannot replenish it.
        boolean submergedPlant = level.getBlockState(pos).getBlock() instanceof lNalator.Cultivationcraft.Common.Blocks.Plants.ProceduralPlantBlock
                && !lNalator.Cultivationcraft.Common.Blocks.Plants.world.ProceduralPlantElementConditions
                        .canGenerateQiInWater(level, pos, getElement());
        if (!submergedPlant) {
            qiCurrent += (1.0 / qiRegen);
        }

        if (qiCurrent > 1) {
            qiCurrent = 1;
        }

        if (previousCurrent != qiCurrent) {
            update = true;
        }

        previousCurrent = qiCurrent;

        return update;
    }

    // Try to absorb the specified amount of qi, returning the amount absorbed
    public double absorbQi(double toAbsorb, Player player) {
        if (toAbsorb > getQiCurrent()) {
            toAbsorb = getQiCurrent();
        }

        subtractQi(toAbsorb);

        if (toAbsorb > 0) {
            // Blood and meditation both draw from the source in a tick: the stream shows their total
            absorbing.merge(player.getUUID(), toAbsorb, Double::sum);
            toUpdate = true;
        }

        return toAbsorb;
    }

    public void subtractQi(double toSubtract) {
        qiCurrent -= toSubtract / (double) getQiMax();

        if (qiCurrent < 0) {
            qiCurrent = 0;
        }
    }

    public CompoundTag SerializeNBT() {
        CompoundTag nbt = new CompoundTag();

        nbt.putLong("pos", pos.asLong());
        nbt.putBoolean("plantOwned", plantOwned);
        nbt.putInt("range", range);
        nbt.putString("element", Element.toString());
        nbt.putDouble("qimax", qiMax);
        nbt.putDouble("qicurrent", qiCurrent);
        nbt.putDouble("qiregen", qiRegen);

        int i = 0;
        for (Map.Entry<UUID, Double> player : currentAbsorbing.entrySet()) {
            nbt.putUUID("player" + i, player.getKey());
            nbt.putDouble("amount" + i, player.getValue());
            i++;
        }

        return nbt;
    }

    public void WriteBuffer(FriendlyByteBuf buffer) {
        buffer.writeNbt(SerializeNBT());
    }

    public static QiSource DeserializeNBT(CompoundTag nbt) {
        BlockPos newPos = BlockPos.of(nbt.getLong("pos"));
        int size = nbt.getInt("range");
        ResourceLocation element = new ResourceLocation(nbt.getString("element"));
        double qiMax = nbt.getDouble("qimax");
        double qiCurrent = nbt.getDouble("qicurrent");
        int qiRegen = nbt.getInt("qiregen");

        HashMap<UUID, Double> absorbing = new HashMap<UUID, Double>();

        int i = 0;
        while (nbt.hasUUID("player" + i)) {
            absorbing.put(nbt.getUUID("player" + i), nbt.getDouble("amount" + i));
            i++;
        }

        QiSource newSource = new QiSource(newPos, size, element, qiMax, qiCurrent, qiRegen);
        newSource.plantOwned = nbt.getBoolean("plantOwned");
        newSource.currentAbsorbing = absorbing;

        return newSource;
    }

    public static QiSource ReadBuffer(FriendlyByteBuf buffer) {
        return DeserializeNBT(buffer.readNbt());
    }

    // Default Qi to absorb without using a QiSource
    public static int getDefaultQi() {
        return 10;
    }
}
