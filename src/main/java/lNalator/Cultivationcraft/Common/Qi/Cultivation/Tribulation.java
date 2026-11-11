package lNalator.Cultivationcraft.Common.Qi.Cultivation;

import lNalator.Cultivationcraft.Common.Qi.Techniques.DivineSenseTechnique;
import lNalator.Cultivationcraft.Network.PacketHandler;
import lNalator.Cultivationcraft.Network.Packets.TribulationPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

public class Tribulation
{
    // One lightning strike every 10 seconds
    public static final int STRIKE_INTERVAL = 200;
    // Players this far away still see the storm
    public static final double VISIBLE_RANGE = 256;
    protected static final int SYNC_INTERVAL = 10;

    protected int tick = 0;
    protected int lightningStrikes = 0;
    protected int lightningPower = 0;
    protected float lightningPowerModifier = 0;

    protected final int targetStrikes;

    public Tribulation(int numberOfLightningStrikes, int lightningStrength, float lightningStrengthGrowth)
    {
        targetStrikes = numberOfLightningStrikes;
        lightningPower = lightningStrength;
        lightningPowerModifier = lightningStrengthGrowth;
    }

    public void reset()
    {
        tick = 0;
        lightningStrikes = 0;
    }

    public boolean tick(Player player)
    {
        // Do not do tribulation unless in a dimension with a sky
        if (player.level.dimensionType().hasCeiling() || !player.level.dimensionType().hasSkyLight())
        {
            reset();
            return false;
        }

        // Return true if the player has survived the target number of lightning strikes
        if (targetStrikes == lightningStrikes)
            return true;

        tick++;

        if (player.level.isClientSide)
            return false;

        // The storm above the player, for everyone around (see TribulationRenderer)
        if (tick % SYNC_INTERVAL == 1)
            sync(player);

        if (tick % STRIKE_INTERVAL == 0)
        {
            int strength = (int)(lightningPower * (1 + lightningStrikes * lightningPowerModifier));

            lightning(player.blockPosition(), strength, (ServerLevel) player.level);
        }

        return false;
    }

    // Players with an active Divine Sense sense the tribulation from further away, 2 detection radii beyond the storm
    protected void sync(Player player)
    {
        TribulationPacket packet = new TribulationPacket(player.getId(), player.position(), tick, lightningStrikes);

        for (ServerPlayer other : ((ServerLevel) player.level).players())
        {
            double range = VISIBLE_RANGE;

            if (DivineSenseTechnique.getActive(other) != null)
                range += 2 * DivineSenseTechnique.getSenseProfile(other).radius();

            if (other.distanceToSqr(player) <= range * range)
                PacketHandler.channel.send(PacketDistributor.PLAYER.with(() -> other), packet);
        }
    }

    public void lightning(BlockPos targetPos, float power, ServerLevel level)
    {
        if (!level.canSeeSky(targetPos))
        {
            int y = level.getMaxBuildHeight();

            while (power > 0 && y > targetPos.getY()) {
                BlockPos blockpos = new BlockPos(targetPos.getX(), y, targetPos.getZ());
                BlockState block = level.getBlockState(blockpos);

                if (!block.isAir() && !block.getMaterial().isLiquid()) {
                    float strength = Math.min(block.getDestroySpeed(level, blockpos), 2) * 0.5f;

                    // If the lightning does not have enough enough power left to break through the block, spawn the lightning on the block and return
                    if (strength > power)
                    {
                        spawnLightning(level, blockpos, power);
                        return;
                    }

                    power -= strength;
                    level.destroyBlock(blockpos, true);
                }

                y--;
            }
        }

        spawnLightning(level, targetPos, power);
    }

    public void spawnLightning(ServerLevel level, BlockPos pos, float power)
    {
        lightningStrikes++;

        LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(level);
        lightningbolt.moveTo(Vec3.atBottomCenterOf(pos));
        lightningbolt.setDamage(power);
        level.addFreshEntity(lightningbolt);
    }
}
