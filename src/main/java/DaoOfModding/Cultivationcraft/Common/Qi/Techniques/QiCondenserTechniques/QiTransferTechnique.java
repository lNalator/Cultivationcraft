package DaoOfModding.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques;

import net.minecraft.server.level.ServerPlayer;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import DaoOfModding.Cultivationcraft.Network.Packets.QiStreamPacket;
import DaoOfModding.Cultivationcraft.Server.BindingVisualSync;
import net.minecraftforge.network.PacketDistributor;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.entity.ProceduralPlantBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Blocks.entity.AlchemyCauldronBlockEntity;
import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import DaoOfModding.Cultivationcraft.Common.Qi.CultivationTypes;
import DaoOfModding.Cultivationcraft.Common.Qi.Techniques.Technique;
import DaoOfModding.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultTechniqueStatIDs;
import DaoOfModding.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.TechniqueStatModification;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

public class QiTransferTechnique extends Technique {
    // Keep the saved stat ID so existing training focus and levels are preserved.
    public static final ResourceLocation TRANSFER_TICK_RATE = new ResourceLocation(Cultivationcraft.MODID,
            "cultivationcraft.tstat.qitransferrate");
    private double pulseProgress;
    private BlockPos targetPos;
    private BlockPos visualTarget;
    private long lastVisualTick;

    public QiTransferTechnique() {
        langLocation = "cultivationcraft.technique.qitransfer";
        type = useType.Channel;
        multiple = false;
        icon = new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/icons/emission.png");
        canLevel = true;

        TechniqueStatModification rate = new TechniqueStatModification(TRANSFER_TICK_RATE);
        rate.addStatChange(TRANSFER_TICK_RATE, 0.001);
        TechniqueStatModification stamina = new TechniqueStatModification(DefaultTechniqueStatIDs.staminaCost);
        stamina.addStatChange(DefaultTechniqueStatIDs.staminaCost, -0.001);
        TechniqueStatModification range = new TechniqueStatModification(DefaultTechniqueStatIDs.range);
        range.addStatChange(DefaultTechniqueStatIDs.range, 0.001);
        addTechniqueStat(TRANSFER_TICK_RATE, 1, rate);
        addTechniqueStat(DefaultTechniqueStatIDs.staminaCost, 1, stamina);
        addTechniqueStat(DefaultTechniqueStatIDs.range, 5, range);
        addMinTechniqueStat(TRANSFER_TICK_RATE, 1);
        addMaxTechniqueStat(TRANSFER_TICK_RATE, 20);
        addMinTechniqueStat(DefaultTechniqueStatIDs.staminaCost, 0.1);
        addMinTechniqueStat(DefaultTechniqueStatIDs.range, 1);
        addMaxTechniqueStat(DefaultTechniqueStatIDs.range, 32);
    }

    @Override
    public boolean isValid(Player player) {
        return CultivatorStats.getCultivatorStats(player).getCultivationType() == CultivationTypes.QI_CONDENSER;
    }

    @Override
    public void activate(Player player) {
        resetPulse();
        super.activate(player);
    }

    @Override
    public void deactivate(Player player) {
        stopVisual(player);
        resetPulse();
        super.deactivate(player);
    }

    private void resetPulse() {
        pulseProgress = 0;
        targetPos = null;
    }

    @Override
    public void tickServer(TickEvent.PlayerTickEvent event) {
        super.tickServer(event);
        Player player = event.player;
        if (!active || !player.isAlive() || player.isSpectator() || !isValid(player)
                || !(player.level instanceof ServerLevel level)) return;
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().scale(getTechniqueStat(DefaultTechniqueStatIDs.range, player)));
        var hit = level.clip(new ClipContext(start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        BlockEntity target = hit.getType() == HitResult.Type.BLOCK ? level.getBlockEntity(hit.getBlockPos()) : null;
        if (!canReceive(target)) {
            stopVisual(player);
            resetPulse();
            return;
        }
        if (!hit.getBlockPos().equals(targetPos)) {
            stopVisual(player);
            resetPulse();
            targetPos = hit.getBlockPos().immutable();
        }
        // Increase frequency, never the amount in a pulse. Fractional timing allows
        // small upgrades to accumulate without batching transfers once a second.
        pulseProgress += getTechniqueStat(TRANSFER_TICK_RATE, player) / 20.0;
        if (pulseProgress + 1.0E-9 < 1) return;
        pulseProgress = Math.max(0, pulseProgress - 1);
        int qiCost = 1;
        if (target instanceof ProceduralPlantBlockEntity plant) {
            qiCost = switch (plant.getTier()) { case 2 -> 1000; case 3 -> 10000; default -> 100; };
        }
        double staminaCost = getTechniqueStat(DefaultTechniqueStatIDs.staminaCost, player);
        var cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();
        // The current cultivation API pays Qi from the stamina pool. Each pulse
        // pays its own conversion cost and stamina overhead before transferring one Qi.
        if (!cultivation.consumeQi(player, qiCost + staminaCost)) {
            player.displayClientMessage(Component.translatable("cultivationcraft.technique.qitransfer.insufficient"), true);
            deactivate(player);
            return;
        }
        if (target instanceof ProceduralPlantBlockEntity plant) plant.receiveQi(1);
        else if (target instanceof AlchemyCauldronBlockEntity cauldron) cauldron.receiveQi(1);
        // Standard progression includes selected training focus, stage limits and mastery.
        levelUp(player, 1);
        if (visualTarget == null || level.getGameTime() - lastVisualTick >= 5) {
            visualTarget = hit.getBlockPos().immutable();
            lastVisualTick = level.getGameTime();
            sendVisual(player, visualTarget, true);
        }
    }

    private void stopVisual(Player player) {
        if (visualTarget != null) sendVisual(player, visualTarget, false);
        visualTarget = null;
    }

    private void sendVisual(Player player, BlockPos target, boolean active) {
        if (!(player instanceof ServerPlayer server)) return;
        PacketHandler.channel.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> server),
                new QiStreamPacket(server.getUUID(),
                        server.level.dimension().location(), target,
                        BindingVisualSync.elementColor(server), active));
    }

    private boolean canReceive(BlockEntity target) {
        if (target instanceof ProceduralPlantBlockEntity plant)
            return plant.getSpiritualGrowth() < ProceduralPlantBlockEntity.MAX_SPIRITUAL_GROWTH;
        return target instanceof AlchemyCauldronBlockEntity cauldron && cauldron.getStoredQi() < Integer.MAX_VALUE;
    }
}
