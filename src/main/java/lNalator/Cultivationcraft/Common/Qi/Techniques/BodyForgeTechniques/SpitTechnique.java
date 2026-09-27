package lNalator.Cultivationcraft.Common.Qi.Techniques.BodyForgeTechniques;

import lNalator.Cultivationcraft.Client.ClientListeners;
import lNalator.Cultivationcraft.Client.Particles.Spit.SpitParticleData;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.ICultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.Lungs.Breath.Breath;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Cultivationcraft;
import DaoOfModding.mlmanimator.Client.Poses.PlayerPoseHandler;
import DaoOfModding.mlmanimator.Client.Poses.PoseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

public class SpitTechnique extends Technique {

    public SpitTechnique() {
        super();

        type = useType.Channel;
        multiple = false;

        langLocation = "cultivationcraft.technique.spit";
        Element = Elements.noElement;

        icon = new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/icons/spit.png");
    }

    // Ticks on client side, only called if Technique is active
    public void tickClient(TickEvent.PlayerTickEvent event) {
        // Set the element of the bite attack to be the same as the bone's element
        /*if (Element == null)
            Element = BodyModifications.getBodyModifications(event.player).getOption(BodyPartNames.bodyPosition, BodyPartNames.boneSubPosition).getElement();*/

        Vec3 look = event.player.getLookAngle().scale(0.5f);

        PlayerPoseHandler handler = PoseHandler.getPlayerPoseHandler(event.player.getUUID());

        if (ClientListeners.tick % 5 == 0) {
            Minecraft.getInstance().level.addParticle(new SpitParticleData(Breath.WATER), event.player.getX(), event.player.getY() - handler.getPlayerModel().getEyeHeight(), event.player.getZ(), look.x, look.y, look.z);
        }

        super.tickClient(event);
    }
}
