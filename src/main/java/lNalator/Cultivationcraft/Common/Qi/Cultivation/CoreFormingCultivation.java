package lNalator.Cultivationcraft.Common.Qi.Cultivation;

import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Client.GUI.Screens.ConceptScreen;
import lNalator.Cultivationcraft.Client.GUI.Screens.GenericTabScreen;
import lNalator.Cultivationcraft.Client.GUI.Screens.CultivationTypeScreens.CoreFormingScreen;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.ExternalCultivationHandler;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Techniques.PassiveTechniques.CultivationPassives.CoreFormingPassive;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.*;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultCultivationStatIDs;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;

public class CoreFormingCultivation extends CultivationType
{
    public CoreFormingCultivation() {
        this(1);
    }

    public CoreFormingCultivation(int cultivationStage)
    {
        super(cultivationStage);
        maxedTechsToBreakthrough = 0;
        maxStage = 9;
        passive = new CoreFormingPassive();
        screen = new CoreFormingScreen();
        tribulation = new Tribulation(maxStage, 75, 0.6f);

        ID = new ResourceLocation(Cultivationcraft.MODID, "cultivation.coreforming");
    }

    public void setCore(TechniqueModifier mod)
    {
        modifiers.add(mod);

        // Every core shares the same stabilisation goal for a given stage, growing by a fixed step:
        // stage 1 (the elemental core) is the easiest. The concept only decides what is counted.
        Quest base = mod.getStabaliseQuest();
        setQuest(new Quest(base.mode, Config.Server.stabiliseGoal(stage), base.extra));
    }

    public TechniqueModifier getFocus()
    {
        return modifiers.get(0);
    }

    @Override
    public boolean hasAdvancementOptions()
    {
        return true;
    }

    @Override
    public void doPreAdvancementActions(CultivationType advancingFrom, Player player)
    {
        // Learn basic concepts without requiring their quests
        new QiModifier().learn(player);

        ResourceLocation currentElementFocus = ((QiCondenserCultivation) advancingFrom).getCurrentElementFocus();

        if (currentElementFocus.compareTo(Elements.earthElement) == 0 || currentElementFocus.compareTo(Elements.anyElement) == 0)
            new EarthModifier().learn(player);

        if (currentElementFocus.compareTo(Elements.fireElement) == 0 || currentElementFocus.compareTo(Elements.anyElement) == 0)
            new FireModifier().learn(player);

        if (currentElementFocus.compareTo(Elements.iceElement) == 0 || currentElementFocus.compareTo(Elements.anyElement) == 0)
            new IceModifier().learn(player);

        if (currentElementFocus.compareTo(Elements.lightningElement) == 0 || currentElementFocus.compareTo(Elements.anyElement) == 0)
            new LightningModifier().learn(player);

        if (currentElementFocus.compareTo(Elements.waterElement) == 0 || currentElementFocus.compareTo(Elements.anyElement) == 0)
            new WaterModifier().learn(player);

        if (currentElementFocus.compareTo(Elements.windElement) == 0 || currentElementFocus.compareTo(Elements.anyElement) == 0)
            new WindModifier().learn(player);

        if (currentElementFocus.compareTo(Elements.woodElement) == 0 || currentElementFocus.compareTo(Elements.anyElement) == 0)
            new WoodModifier().learn(player);
    }

    @Override
    public boolean doPostAdvancementActions(Player player, String extra)
    {
        TechniqueModifier mod = ExternalCultivationHandler.getModifier(new ResourceLocation(extra));

        if (mod.canChooseAsCore(player))
            setCore(mod);
        else
            return  false;

        return true;
    }

    @Override
    public GenericTabScreen getAdvancmentOptionScreen()
    {
        return new ConceptScreen(true);
    }

    // Every Qi source can be cultivated, at an efficiency set by the core's element (Golden Core pressure)
    @Override
    public double getCultivationEfficiency(ResourceLocation element)
    {
        for (ResourceLocation core : getElements())
        {
            // Pure Qi suits every core
            if (element.equals(Elements.anyElement) || element.equals(Elements.noElement) || core.equals(Elements.noElement))
                return 1;

            boolean variant = core.equals(Elements.iceElement) || core.equals(Elements.lightningElement);

            if (element.equals(core))
                return variant ? Config.Server.variantOwnElementEfficiency() : Config.Server.ownElementEfficiency();

            // Ice and Lightning are pressed by everything, so they only take the plain malus
            if (!variant && Elements.presses(element, core))
                return Config.Server.pressingElementEfficiency();
        }

        return Config.Server.otherElementEfficiency();
    }

    @Override
    public boolean canCultivate(Player player)
    {
        if (CultivatorStats.getCultivatorStats(player).getCultivation().getClass() == QiCondenserCultivation.class)
            return true;

        return false;
    }

    @Override
    public boolean hasSeaOfQi() {
        return true;
    }

    // Golden Core: each technique use trains 1 point plus 1 per Core Forming stage
    @Override
    public double getTechniqueTrainingMultiplier() {
        return 1 + stage;
    }

    // Saves from before the Core Forming passive kept this stage's Qi as a plain level: move it into Max Qi
    @Override
    public void readNBT(CompoundTag nbt) {
        super.readNBT(nbt);

        String passiveKey = passive.getClass().toString();

        if (qiLevel > 0 && !statLevels.containsKey(passiveKey)) {
            HashMap<ResourceLocation, Double> levels = new HashMap<>();
            levels.put(DefaultCultivationStatIDs.maxQi, Math.min(qiLevel, techLevel));
            statLevels.put(passiveKey, levels);
        }

        qiLevel = 0;
    }

    public void stageCalculations()
    {
        techLevel = 1000;
    }

    // TEMP whilst no further cultivation
    public boolean canBreakthrough(Player player) {
        if (stage == maxStage)
            return false;

        return super.canBreakthrough(player);
    }

    @Override
    public boolean clientPreBreakthrough(Player player)
    {
        Minecraft.getInstance().forceSetScreen(new ConceptScreen(true));

        return false;
    }

    @Override
    public void breakthrough(Player player, String conditionals)
    {
        if (stage < maxStage)
        {
            if (conditionals.length() == 0)
            {
                Cultivationcraft.LOGGER.error("Tried to advance cultivation without providing a core");
            }
            else
            {
                // The core comes from the client: check it can be chosen (learnt, allowed, not a unique core already owned)
                TechniqueModifier core = ExternalCultivationHandler.getModifier(new ResourceLocation(conditionals));

                if (core == null || !core.canChooseAsCore(player))
                {
                    Cultivationcraft.LOGGER.error("Tried to advance cultivation with an unavailable core " + conditionals);
                    return;
                }

                CoreFormingCultivation newCultivation = new CoreFormingCultivation(stage + 1);
                newCultivation.setCore(core);
                newCultivation.setPreviousCultivation(this);

                CultivatorStats.getCultivatorStats(player).setCultivation(newCultivation);
            }
        }
    }
}
