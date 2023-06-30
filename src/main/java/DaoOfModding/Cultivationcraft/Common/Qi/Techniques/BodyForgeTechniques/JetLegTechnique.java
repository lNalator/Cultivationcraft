package DaoOfModding.Cultivationcraft.Common.Qi.Techniques.BodyForgeTechniques;

import DaoOfModding.Cultivationcraft.Client.genericClientFunctions;
import DaoOfModding.Cultivationcraft.Client.Animations.BodyPartModelNames;
import DaoOfModding.Cultivationcraft.Client.Physics;
import DaoOfModding.Cultivationcraft.Common.Capabilities.BodyModifications.BodyModifications;
import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import DaoOfModding.Cultivationcraft.Common.PlayerUtils;
import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.BodyPartNames;
import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.Quests.Quest;
import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.Quests.QuestHandler;
import DaoOfModding.Cultivationcraft.Common.Qi.CultivationTypes;
import DaoOfModding.Cultivationcraft.Common.Qi.Stats.BodyPartStatControl;
import DaoOfModding.Cultivationcraft.Common.Qi.Stats.PlayerStatControl;
import DaoOfModding.Cultivationcraft.Common.Qi.Stats.StatIDs;
import DaoOfModding.Cultivationcraft.Common.Qi.Techniques.MovementOverrideTechnique;
import DaoOfModding.Cultivationcraft.Common.Reflection;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import DaoOfModding.Cultivationcraft.Network.ClientPacketHandler;
import DaoOfModding.Cultivationcraft.StaminaHandler;
import DaoOfModding.mlmanimator.Client.Models.MultiLimbedModel;
import DaoOfModding.mlmanimator.Client.Poses.PoseHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

public class JetLegTechnique extends MovementOverrideTechnique
{
    protected float staminaUse = 0.02f;
    protected boolean jump = false;

    public JetLegTechnique()
    {
        super();

        langLocation = "cultivationcraft.technique.jetlegs";
        type = useType.Toggle;
        multiple = false;
        momentum = true;
        momentumFactor = 0.2f;

        icon = new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/icons/jetlegs.png");
    }

    @Override
    public boolean isValid(Player player)
    {
        // Technique is valid if the player is a body cultivator with Jet Legs
        if (CultivatorStats.getCultivatorStats(player).getCultivationType() == CultivationTypes.BODY_CULTIVATOR &&
                BodyModifications.getBodyModifications(player).hasModification(BodyPartNames.legPosition, BodyPartNames.jetLegPart))
            return true;

        return false;
    }

    protected void calculateJetPower(Player player)
    {
        PlayerStatControl stats = BodyPartStatControl.getPlayerStatControl(player);

        float weightModifier = stats.getFlightWeightModifier();

        float upwardsPower = stats.getStats().getStat(StatIDs.flightSpeed);
        double y = (upwardsPower * weightModifier);

        setMomentum(new Vec3(0, y, 0));
    }

    @Override
    public void tickClient(TickEvent.PlayerTickEvent event)
    {
        super.tickClient(event);

        // Don't do anything if the player is in the water
        if (event.player.isInWater())
            return;

        MultiLimbedModel model = PoseHandler.getPlayerPoseHandler(event.player.getUUID()).getPlayerModel();

        // Activate the leg jets if player is moving upwards
        if ((PlayerUtils.isClientPlayerCharacter(event.player) && jump) || (!PlayerUtils.isClientPlayerCharacter(event.player) && currentSpeed.y > 0 && Physics.getDelta(event.player).y > 0))
        {
            model.getLimb(BodyPartModelNames.jetLegLeftEmitter).getModelPart().visible = true;
            model.getLimb(BodyPartModelNames.jetLegRightEmitter).getModelPart().visible = true;

            PoseHandler.getPlayerPoseHandler(event.player.getUUID()).disableJumpingAnimationThisTick = true;

            calculateJetPower(event.player);
        }
        else
        {
            model.getLimb(BodyPartModelNames.jetLegLeftEmitter).getModelPart().visible = false;
            model.getLimb(BodyPartModelNames.jetLegRightEmitter).getModelPart().visible = false;

            setMomentum(new Vec3(0, 0, 0));
        }

        jump = false;

        QuestHandler.progressQuest(event.player, Quest.FLIGHT, currentSpeed.length());
    }

    @Override
    public void tickInactiveClient(TickEvent.PlayerTickEvent event)
    {
        super.tickInactiveClient(event);

        setMomentum(new Vec3(0, 0, 0));
        jump = false;

        QuestHandler.progressQuest(event.player, Quest.FLIGHT, currentSpeed.length());
    }

    @Override
    public void tickServer(TickEvent.PlayerTickEvent event)
    {
        Reflection.allowFlight((ServerPlayer) event.player);

        super.tickServer(event);
        tickInactiveServer(event);
    }

    @Override
    public boolean overwriteJump()
    {
        Player player = genericClientFunctions.getPlayer();
        // Do nothing if the player is in water
        if (player.isInWater())
            return false;

        // Do nothing if player is out of stamina
        if (!StaminaHandler.consumeStamina(player, staminaUse))
            return false;

        jump = true;

        return true;
    }

    @Override
    public void deactivate(Player player)
    {
        if (player.level.isClientSide)
        {
            MultiLimbedModel model = PoseHandler.getPlayerPoseHandler(player.getUUID()).getPlayerModel();

            model.getLimb(BodyPartModelNames.jetLegLeftEmitter).getModelPart().visible = false;
            model.getLimb(BodyPartModelNames.jetLegRightEmitter).getModelPart().visible = false;
        }

        super.deactivate(player);
    }
}
