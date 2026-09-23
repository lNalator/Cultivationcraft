package DaoOfModding.Cultivationcraft.Common.Qi.Techniques;

import DaoOfModding.Cultivationcraft.Common.PlayerUtils;
import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.BodyPartNames;
import DaoOfModding.Cultivationcraft.Client.ClientItemControl;
import DaoOfModding.Cultivationcraft.Client.Renderer;
import DaoOfModding.Cultivationcraft.Common.Capabilities.BodyModifications.BodyModifications;
import DaoOfModding.Cultivationcraft.Common.Capabilities.BodyModifications.IBodyModifications;
import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.ICultivatorStats;
import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.BodyPartOption;
import DaoOfModding.Cultivationcraft.Common.Qi.CultivationTypes;
import DaoOfModding.Cultivationcraft.Common.Qi.Elements.Elements;
import DaoOfModding.Cultivationcraft.Common.Qi.Stats.StatIDs;
import DaoOfModding.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultTechniqueStatIDs;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraft.util.Mth;
import DaoOfModding.Cultivationcraft.Common.Qi.Cultivation.FoundationEstablishmentCultivation;
import DaoOfModding.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import DaoOfModding.Cultivationcraft.Common.Qi.Cultivation.CoreFormingCultivation;

public class DivineSenseTechnique extends Technique
{
    public static final ResourceLocation DETECTION_RADIUS = new ResourceLocation(Cultivationcraft.MODID,
            "cultivationcraft.tstat.divinesense_radius");
    public static final ResourceLocation MAX_HIGHLIGHTS = new ResourceLocation(Cultivationcraft.MODID,
            "cultivationcraft.tstat.divinesense_max_highlights");

    public record SenseProfile(int radius, int maxHighlights, double peakOpacity) {}

    public static SenseProfile getSenseProfile(Player player) {
        var cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();
        int minorStage = Mth.clamp(cultivation.getStage(), 1, Math.max(1, cultivation.getMaxStage())) - 1;
        // Realm entry bonuses are deliberately larger than the gains within a realm.
        if (cultivation instanceof CoreFormingCultivation)
            return new SenseProfile(192 + minorStage * 8, 128 + minorStage * 8, 0.94 + minorStage * 0.005);
        if (cultivation instanceof QiCondenserCultivation)
            return new SenseProfile(64 + minorStage * 4, 64 + minorStage * 4, 0.85 + minorStage * 0.01);
        if (cultivation instanceof FoundationEstablishmentCultivation)
            return new SenseProfile(16 + minorStage * 2, 30 + minorStage * 2, 0.75 + minorStage * 0.02);
        return new SenseProfile(16, 30, 0.75);
    }

    @Override
    public double getTechniqueStat(ResourceLocation stat, Player player) {
        if (DETECTION_RADIUS.equals(stat)) return getSenseProfile(player).radius();
        if (MAX_HIGHLIGHTS.equals(stat)) return getSenseProfile(player).maxHighlights();
        return super.getTechniqueStat(stat, player);
    }

    public DivineSenseTechnique()
    {
        super();

        langLocation = "cultivationcraft.technique.divinesense";
        Element = Elements.noElement;

        type = useType.Toggle;
        multiple = false;

        icon = new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/icons/divinesense.png");
        //setOverlay(new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/overlays/divinesense.png"));

        stats.setStat(StatIDs.staminaDrain, 0.05f);
        addTechniqueStat(DefaultTechniqueStatIDs.qiCost, 1f);
        addTechniqueStat(DETECTION_RADIUS, 16);
        addTechniqueStat(MAX_HIGHLIGHTS, 30);

        effects.add(MobEffects.NIGHT_VISION);
    }

    @Override
    public void tickClient(TickEvent.PlayerTickEvent event)
    {
        if (PlayerUtils.isClientPlayerCharacter(event.player))
            Renderer.QiSourcesVisible = true;

        if (event.player.getFoodData().getFoodLevel() == 0)
            this.deactivate(event.player);
    }

    @Override
    public void tickServer(TickEvent.PlayerTickEvent event)
    {
        super.tickServer(event);

        if (CultivatorStats.getCultivatorStats(event.player).getCultivationType() == CultivationTypes.QI_CONDENSER)
        {
            if (!CultivatorStats.getCultivatorStats(event.player).getCultivation().consumeQi(event.player, getTechniqueStat(DefaultTechniqueStatIDs.qiCost, event.player) / 20f))
                this.deactivate(event.player);
        }
        else if (event.player.getFoodData().getFoodLevel() == 0)
            this.deactivate(event.player);
    }

    @Override
    public void deactivate(Player player)
    {
        super.deactivate(player);

        if (PlayerUtils.isClientPlayerCharacter(player))
            Renderer.QiSourcesVisible = false;
    }

    @Override
    public boolean isValid(Player player)
    {
        ICultivatorStats stats = CultivatorStats.getCultivatorStats(player);

        if (stats.getCultivationType() == CultivationTypes.BODY_CULTIVATOR)
        {
            // If player is a body cultivator and has cultivated Qi Sight then return true
            IBodyModifications modifications = BodyModifications.getBodyModifications(player);

            BodyPartOption eyes = BodyPartNames.getOption(BodyPartNames.startingEyesPart);

            if (!modifications.hasOption(eyes.getPosition(), eyes.getSubPosition()))
                return false;

            if (modifications.getOption(eyes.getPosition(), eyes.getSubPosition()) == eyes)
                return true;
        }
        else if (stats.getCultivationType() == CultivationTypes.QI_CONDENSER)
        {
            return true;
        }

        return false;
    }
}
