package lNalator.Cultivationcraft.Client;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Client.Renderers.QiGlowRenderer;
import lNalator.Cultivationcraft.Client.Renderers.QiSourceRenderer;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.Blood.Blood;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.FoodStats.QiFoodStats;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.PlayerHealthManager;
import lNalator.Cultivationcraft.Common.Qi.Stats.BodyPartStatControl;
import lNalator.Cultivationcraft.Common.Qi.Stats.StatIDs;
import lNalator.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import com.mojang.math.Vector3f;


public class Renderer
{
    protected static final animatedTexture healthOrb = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/gui/healthorb.png"));

    public static boolean QiSourcesVisible = false;
    // Qi particles are only shown within 4x the Divine Sense detection radius
    public static double QiSenseRadius = 16;

    public static void render(float partialTick)
    {
        if (!Minecraft.getInstance().isPaused())
                QiSourceRenderer.renderQiSources(partialTick);

        if (Renderer.QiSourcesVisible)
            QiGlowRenderer.render();

        renderTechniques();
    }

    public static void renderTechniques()
    {
        // Loop through all players in the world
        for (Player player : Minecraft.getInstance().level.players())
        {
            if (player.isAlive())
            {
                // Grab the players technique list and try to render them
                ICultivatorTechniques techniques = CultivatorTechniques.getCultivatorTechniques(player);

                for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++)
                    if (techniques.getTechnique(i) != null && techniques.getTechnique(i).isActive())
                        techniques.getTechnique(i).render();
            }
        }

        // Grab the player characters techniques and render them from the players view
        if (genericClientFunctions.getPlayer() != null && genericClientFunctions.getPlayer().isAlive())
        {
            ICultivatorTechniques techniques = CultivatorTechniques.getCultivatorTechniques(genericClientFunctions.getPlayer());

            for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++)
                if (techniques.getTechnique(i) != null && techniques.getTechnique(i).isActive())
                    techniques.getTechnique(i).renderPlayerView();
        }
    }

    public static void renderTechniqueOverlays()
    {
        // Grab the player characters techniques and render their overlays
        if (genericClientFunctions.getPlayer() != null)
        {
            ICultivatorTechniques techniques = CultivatorTechniques.getCultivatorTechniques(genericClientFunctions.getPlayer());

            for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++)
                if (techniques.getTechnique(i) != null && techniques.getTechnique(i).isActive())
                    techniques.getTechnique(i).renderOverlay();
        }
    }

    public static double getAspectRatio()
    {
        return (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double)Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    public static void renderHP()
    {
        float health = genericClientFunctions.getPlayer().getHealth();
        // Use the real max health (attribute) so the orb scales with it like the qi orb, instead of overflowing above the reservoir
        float maxHealth = Math.max(genericClientFunctions.getPlayer().getMaxHealth(), BodyPartStatControl.getStats(genericClientFunctions.getPlayer()).getStat(StatIDs.maxHP));
        float healthPercent = maxHealth > 0 ? Math.max(0, Math.min(1, health/maxHealth)) : 0;

        int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        Blood blood = PlayerHealthManager.getBlood(genericClientFunctions.getPlayer());
        Vector3f colour = blood.getColour();

        RenderSystem.setShaderColor(colour.x(), colour.y(), colour.z(), 0.7f);
        blood.getOrbFilling().render((int)(scaledWidth * 0.1), (int)(scaledHeight - (10 + 40 * healthPercent)), 40, (int)(40 * healthPercent), healthPercent);

        // Absorption hearts (golden apple...) as a yellow layer in front, like saturation on the stamina orb
        float absorption = genericClientFunctions.getPlayer().getAbsorptionAmount();
        if (absorption > 0 && maxHealth > 0)
        {
            float absorptionPercent = Math.min(1, absorption / maxHealth);
            RenderSystem.setShaderColor(1.0F, 0.85F, 0.1F, 0.6f);
            blood.getOrbFilling().render((int)(scaledWidth * 0.1), (int)(scaledHeight - (10 + 40 * absorptionPercent)), 40, (int)(40 * absorptionPercent), absorptionPercent);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        healthOrb.render((int)(scaledWidth * 0.1), scaledHeight - 50, 40, 40);

        PlayerHealthManager.getLungs(genericClientFunctions.getPlayer()).render((int)(scaledWidth * 0.9) - 40, scaledHeight - 50);
    }

    public static void renderStamina()
    {
        int scaledWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int scaledHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        float stamina = genericClientFunctions.getPlayer().getFoodData().getFoodLevel();
        float maxStamina = BodyPartStatControl.getStats(genericClientFunctions.getPlayer()).getStat(StatIDs.maxStamina);

        if (genericClientFunctions.getPlayer().getFoodData() instanceof QiFoodStats)
        {
            stamina = ((QiFoodStats) genericClientFunctions.getPlayer().getFoodData()).getTrueFoodLevel();
            maxStamina = ((QiFoodStats) genericClientFunctions.getPlayer().getFoodData()).getMaxFood();
        }

        float staminaPercent = stamina/maxStamina;
        float saturationPercent =  genericClientFunctions.getPlayer().getFoodData().getSaturationLevel()/maxStamina;

        QiFoodStats stats = new QiFoodStats();
        if (genericClientFunctions.getPlayer().getFoodData() instanceof QiFoodStats)
            stats = (QiFoodStats)genericClientFunctions.getPlayer().getFoodData();

        RenderSystem.setShaderColor(stats.getColor().x(), stats.getColor().y(), stats.getColor().z(), 0.7f);
        stats.getOrb().render((int)(scaledWidth * 0.1 - 5), (int)(scaledHeight - (5 + 50 * staminaPercent)), 50, (int)(50 * staminaPercent), staminaPercent);

        RenderSystem.setShaderColor(stats.getSaturationColor().x(), stats.getSaturationColor().y(), stats.getSaturationColor().z(), 0.5f);
        stats.getOrb().render((int)(scaledWidth * 0.1 - 5), (int)(scaledHeight - (5 + 50 * saturationPercent)), 50, (int)(50 * saturationPercent), saturationPercent);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        healthOrb.render((int)(scaledWidth * 0.1 - 5), scaledHeight - 55, 50, 50);
    }
}
