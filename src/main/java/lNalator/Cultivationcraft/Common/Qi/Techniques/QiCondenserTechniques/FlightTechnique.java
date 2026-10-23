package lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques;

import lNalator.Cultivationcraft.Client.Animations.GenericQiPoses;
import lNalator.Cultivationcraft.Client.Renderers.QiGlowRenderer;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.ICultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.TechniqueModifier;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.WarpModifier;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultTechniqueStatIDs;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.TechniqueStatModification;
import lNalator.Cultivationcraft.Common.Reflection;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.mlmanimator.Client.Poses.PoseHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;

public class FlightTechnique extends Technique {

    // Warp flight: the flight speed becomes the length of a jump, base speed (0.01) = 5 blocks
    protected static final double WARP_BLOCKS_PER_SPEED = 500;
    protected static final int WARP_INTERVAL = 10;
    // ponytail: vanilla rejects player moves over ~10 blocks in a tick ("moved too quickly"); longer jumps need a server-side teleport packet
    protected static final double WARP_MAX_BLOCKS = 9;

    protected Vec3 lastWarpPos = null;

    public FlightTechnique() {
        super();

        langLocation = "cultivationcraft.technique.flight";
        Element = Elements.noElement;

        type = useType.Toggle;
        multiple = false;

        icon = new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/icons/flight.png");

        canLevel = true;

        addMinTechniqueStat(DefaultTechniqueStatIDs.qiCost, 0.5);

        TechniqueStatModification qiCostModification = new TechniqueStatModification(DefaultTechniqueStatIDs.qiCost);
        TechniqueStatModification movementModification = new TechniqueStatModification(DefaultTechniqueStatIDs.movementSpeed);

        qiCostModification.addStatChange(DefaultTechniqueStatIDs.qiCost, -0.001);
        movementModification.addStatChange(DefaultTechniqueStatIDs.qiCost, 0.05);
        movementModification.addStatChange(DefaultTechniqueStatIDs.movementSpeed, 0.00005);

        addTechniqueStat(DefaultTechniqueStatIDs.qiCost, 5, qiCostModification);
        addTechniqueStat(DefaultTechniqueStatIDs.movementSpeed, 0.01, movementModification);
    }

    @Override
    public boolean isValid(Player player) {
        ICultivatorStats stats = CultivatorStats.getCultivatorStats(player);

        if (stats.getCultivationType() == CultivationTypes.QI_CONDENSER) {
            return true;
        }

        return false;
    }

    @Override
    public void tickClient(TickEvent.PlayerTickEvent event) {
        super.tickClient(event);

        if (!event.player.isOnGround()) {
            // The server pays Qi and decides when flight ends. Client-only spending
            // drifts downward while meditation replenishes the server's reserve.
            event.player.getAbilities().setFlyingSpeed((float) getTechniqueStat(DefaultTechniqueStatIDs.movementSpeed, event.player));
            event.player.getAbilities().flying = true;

            // The client owns its movement, so it does the jumps; the server pays the Qi
            if (hasWarp(event.player)) {
                event.player.getAbilities().setFlyingSpeed(0);

                if (event.player.isLocalPlayer() && event.player.tickCount % WARP_INTERVAL == 0) {
                    warpJump(event.player);
                }
            }

            PoseHandler.addPose(event.player.getUUID(), GenericQiPoses.HandsBehind);

            QiGlowRenderer.setQiVisible(event.player, Elements.getElement(getElement()));
        }
    }

    @Override
    public void tickServer(TickEvent.PlayerTickEvent event) {
        if (!event.player.isOnGround()) {
            if (!spendExternalQi(event.player, 0.05)) {
                deactivate(event.player);
                lNalator.Cultivationcraft.Network.PacketHandler.sendCultivatorTechniquesToClient(event.player);
                return;
            }

            levelUp(event.player, 0.05);

            event.player.getAbilities().setFlyingSpeed((float) getTechniqueStat(DefaultTechniqueStatIDs.movementSpeed, event.player));
            event.player.getAbilities().flying = true;
            Reflection.allowFlight((ServerPlayer) event.player);

            if (hasWarp(event.player)) {
                payWarpJump(event.player);
            }
        }

        lastWarpPos = event.player.position();

        tickTechniqueModifiers(event.player, event.player.position(), getElement());

        super.tickServer(event);
    }

    protected boolean hasWarp(Player player) {
        return CultivatorStats.getCultivatorStats(player).getCultivation().getModifiers().stream().anyMatch(mod -> mod instanceof WarpModifier);
    }

    // Jump in the direction the player is moving (looking up or down moves vertically), shortened until the destination is free
    protected void warpJump(Player player) {
        if (player.zza == 0 && player.xxa == 0) {
            return;
        }

        Vec3 forward = player.getLookAngle().scale(Math.signum(player.zza));
        Vec3 strafe = Vec3.directionFromRotation(0, player.getYRot() - 90).scale(Math.signum(player.xxa));
        Vec3 direction = forward.add(strafe).normalize();

        for (double distance = Math.min(WARP_MAX_BLOCKS, getTechniqueStat(DefaultTechniqueStatIDs.movementSpeed, player) * WARP_BLOCKS_PER_SPEED); distance >= 1; distance -= 1) {
            Vec3 offset = direction.scale(distance);

            if (player.level.noCollision(player, player.getBoundingBox().move(offset))) {
                player.setPos(player.position().add(offset));
                return;
            }
        }
    }

    // A jump shows up server side as a large move in one tick
    protected void payWarpJump(Player player) {
        if (lastWarpPos == null) {
            return;
        }

        double distance = player.position().distanceTo(lastWarpPos);

        if (distance < 2) {
            return;
        }

        if (!CultivatorStats.getCultivatorStats(player).getCultivation().consumeQi(player, distance * 10)) {
            deactivate(player);
            lNalator.Cultivationcraft.Network.PacketHandler.sendCultivatorTechniquesToClient(player);
            return;
        }

        QuestHandler.progressQuest(player, Quest.ENDER_TELEPORT, 1);
        ((ServerLevel) player.level).sendParticles(ParticleTypes.PORTAL, lastWarpPos.x, lastWarpPos.y + 1, lastWarpPos.z, 16, 0.3, 0.5, 0.3, 0.5);
        player.level.playSound(null, player, SoundEvents.ENDERMAN_TELEPORT, player.getSoundSource(), 0.5f, 1.5f);
    }

    @Override
    public void deactivate(Player player) {
        player.getAbilities().setFlyingSpeed(0.05f);
        player.getAbilities().flying = false;

        super.deactivate(player);
    }

    @Override
    public void activate(Player player) {
        // Do nothing if the player is already a passenger
        if (player.isPassenger()) {
            return;
        }

        boolean mount = false;

        for (TechniqueModifier mod : CultivatorStats.getCultivatorStats(player).getCultivation().getModifiers()) {
            if (mod.hasMount()) {
                mount = true;
            }
        }

        if (mount) {
            BlockState source = null;

            for (TechniqueModifier mod : CultivatorStats.getCultivatorStats(player).getCultivation().getModifiers()) {
                if ((source = mod.getMountSource(player)) != null) {
                    break;
                }
            }

            // TEMP - flying can still work if there is a set FORM that works without a source
            if (source == null) {
                return;
            }

            // TODO - Flying mount entity creation
        }

        super.activate(player);
    }
}
