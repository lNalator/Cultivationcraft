package lNalator.Cultivationcraft.Common.Qi.Cultivation;

import lNalator.Cultivationcraft.Common.Qi.Stats.BodyPartStatControl;
import lNalator.Cultivationcraft.Client.GUI.Screens.CultivationTypeScreens.CultivationTypeScreen;
import lNalator.Cultivationcraft.Client.GUI.Screens.GenericTabScreen;
import lNalator.Cultivationcraft.Common.Advancements.CultivationAdvancements;
import lNalator.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.FoodStats.QiFoodStats;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.ExternalCultivationHandler;
import lNalator.Cultivationcraft.Common.Qi.QiSource;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Techniques.PassiveTechniques.PassiveTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.TechniqueModifier;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultCultivationStatIDs;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Network.PacketHandler;
import lNalator.Cultivationcraft.StaminaHandler;
import net.minecraft.nbt.CompoundTag;
import lNalator.Cultivationcraft.Common.CultivationNotifier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CultivationType {

    protected ResourceLocation ID = new ResourceLocation(Cultivationcraft.MODID, "cultivation.default");

    protected int techLevel = 0;
    protected int maxedTechsToBreakthrough = 0;

    protected int stage = 1;
    protected int maxStage = 1;

    protected CultivationType previousCultivation = null;
    protected Tribulation tribulation = new Tribulation(maxStage, 100, 1);

    protected boolean isTribulating = false;
    protected boolean hasTribulated = false;

    protected HashMap<String, HashMap<ResourceLocation, Double>> statLevels = new HashMap<>();

    protected ArrayList<TechniqueModifier> modifiers = new ArrayList<>();

    protected PassiveTechnique passive = new PassiveTechnique();

    protected CultivationTypeScreen screen = new CultivationTypeScreen();

    protected ArrayList<CultivationType> advancements = new ArrayList<>();

    protected boolean statLeveling = true;
    protected double qiLevel = 0;

    protected Quest quest;
    protected double questProgress = 0;

    protected CultivationType advancingCultivation = null;

    public CultivationType(int currentStage) {
        stage = currentStage;
        stageCalculations();
    }

    // Put anything here that changes based on the stage
    public void stageCalculations() {

    }

    public Quest getQuest() {
        return quest;
    }

    public void setQuest(Quest newQuest) {
        quest = newQuest;
    }

    public ArrayList<ResourceLocation> getElements() {
        ArrayList<ResourceLocation> ElementList = new ArrayList<>();

        for (TechniqueModifier mod : getModifiers()) {
            if (mod.hasElement()) {
                ElementList.add(mod.getElement());
            }
        }

        if (ElementList.size() == 0) {
            ElementList.add(Elements.noElement);
        }

        return ElementList;
    }

    public boolean progressQuest(double amount) {
        if (questProgress == quest.complete || amount == 0) {
            return false;
        }

        questProgress += amount;

        if (questProgress >= quest.complete) {
            questProgress = quest.complete;

            // TODO: Cultivation quest has been completed
        }

        return true;
    }

    public double getQuestProgress() {
        return questProgress;
    }

    public boolean statsCanLevel() {
        return statLeveling;
    }

    public String getName() {
        return Component.translatable(ID.toLanguageKey()).getString();
    }

    public ResourceLocation getID() {
        return ID;
    }

    public boolean canCultivate(ResourceLocation element) {
        return true;
    }

    public boolean consumeQi(Player player, double qiToConsume) {
        return StaminaHandler.consumeStamina(player, (float) qiToConsume);
    }

    // Each core once, even when reinforced: its rank comes from getModifierRank
    public ArrayList<TechniqueModifier> getModifiers() {
        ArrayList<TechniqueModifier> totalModifiers = new ArrayList<>();

        for (CultivationType cultivation = this; cultivation != null; cultivation = cultivation.getPreviousCultivation()) {
            for (TechniqueModifier mod : cultivation.modifiers) {
                if (totalModifiers.stream().noneMatch(added -> added.ID.equals(mod.ID))) {
                    totalModifiers.add(mod);
                }
            }
        }

        return totalModifiers;
    }

    // Number of stages that chose this core (1 when taken once, +1 per reinforcement)
    public int getModifierRank(ResourceLocation id) {
        int rank = 0;

        for (CultivationType cultivation = this; cultivation != null; cultivation = cultivation.getPreviousCultivation()) {
            for (TechniqueModifier mod : cultivation.modifiers) {
                if (mod.ID.equals(id)) {
                    rank++;
                }
            }
        }

        return rank;
    }

    public boolean canBreakthrough(Player player) {
        if (qiLevel >= techLevel || getTechLevelProgressWithoutPrevious(passive.getClass().toString()) >= techLevel) {
            if (getMaxedTechs() >= maxedTechsToBreakthrough) {
                if (getQuest() == null || getQuestProgress() >= getQuest().complete) {
                    return true;
                }
            }
        }

        return false;
    }

    public ArrayList<CultivationType> getAdvancements(Player player) {
        ArrayList<CultivationType> playerAdvancements = new ArrayList<>();

        for (CultivationType testAdvance : advancements) {
            if (testAdvance.canCultivate(player)) {
                playerAdvancements.add(testAdvance);
            }
        }

        return playerAdvancements;
    }

    // Returns whether the specified player can cultivate this cultivationType
    public boolean canCultivate(Player player) {
        return true;
    }

    public void advanceExtra(Player player, String extra) {
        if (advancingCultivation == null) {
            return;
        }

        CultivationType advancing = advancingCultivation;
        advancingCultivation = null;

        advancing.setPreviousCultivation(this);
        if (!advancing.doPostAdvancementActions(player, extra)) {
            Cultivationcraft.LOGGER.error("Error progressing cultivation to " + advancingCultivation.getID() + " with " + extra);
            return;
        }
        CultivatorStats.getCultivatorStats(player).setCultivation(advancing);
    }

    public void advance(Player player, String advancement) {
        CultivationType advanceTo = null;

        for (CultivationType advance : advancements) {
            if (advance.getID().toString().compareTo(advancement) == 0) {
                advanceTo = advance;
            }
        }

        if (advanceTo == null) {
            Cultivationcraft.LOGGER.error("Tried to advance to unavailable cultivation type " + advancement);
            return;
        }

        advanceTo.doPreAdvancementActions(this, player);

        if (!advanceTo.hasAdvancementOptions()) {
            advanceTo.setPreviousCultivation(this);
            CultivatorStats.getCultivatorStats(player).setCultivation(advanceTo);
        } else {
            advancingCultivation = advanceTo;
        }

        tribulation.reset();
    }

    public void doPreAdvancementActions(CultivationType advancingFrom, Player player) {

    }

    public boolean doPostAdvancementActions(Player player, String extra) {
        return true;
    }

    public CultivationType getAdvancingCultivation() {
        return advancingCultivation;
    }

    public GenericTabScreen getAdvancmentOptionScreen() {
        return null;
    }

    public boolean hasAdvancementOptions() {
        return false;
    }

    public boolean hasTribulated() {
        return hasTribulated;
    }

    public boolean hasTribulation(Player player) {
        if (stage == maxStage && canBreakthrough(player)) {
            return true;
        }

        return false;
    }

    public void tick(Player player) {
        // Realm bonuses (max HP, speed) follow the current cultivation, whatever changed it
        if (BodyPartStatControl.getPlayerStatControl(player).realmBonusSource != this) {
            BodyPartStatControl.updateStats(player);
        }

        for (TechniqueModifier mod : getModifiers()) {
            mod.playerTick(player);
        }

        QiFoodStats food = (QiFoodStats) player.getFoodData();
        boolean qiFull = food.getTrueFoodLevel() >= food.getMaxFood();
        food.setFoodLevel(food.getTrueFoodLevel() + (float) (getCultivationStat(player, DefaultCultivationStatIDs.qiPassiveSpeed) / 20.0));

        if (player.level.isClientSide) {
            return;
        }

        // Sea of Qi (Qi Condensation signature): with full Qi, the surplus slowly regenerates life
        if (qiFull && hasSeaOfQi() && player.getHealth() < player.getMaxHealth()) {
            player.heal(SEA_OF_QI_HEAL_PER_TICK);
        }

        if (isTribulating() && player.isAlive()) {
            if (tribulation.tick(player)) {
                isTribulating = false;
                tribulationComplete(player);
                PacketHandler.sendCultivatorStatsToClient(player);
            }
        }
    }

    // 1 HP every 4 seconds, the pace of vanilla natural regeneration
    protected static final float SEA_OF_QI_HEAL_PER_TICK = 0.25f / 20f;

    // Share of absorbed Qi of this element that turns into cultivation
    public double getCultivationEfficiency(ResourceLocation element) {
        return 1;
    }

    public double getTechniqueTrainingMultiplier() {
        return 1;
    }

    public boolean hasSeaOfQi() {
        return false;
    }

    public int getStage() {
        return stage;
    }

    public int getMaxStage() {
        return maxStage;
    }

    public void startTribulation() {
        isTribulating = true;
    }

    public boolean isTribulating() {
        return isTribulating;
    }

    // Calls on client side before breakingthrough
    // Returns wether to breakthrough or not
    public boolean clientPreBreakthrough(Player player) {
        return true;
    }

    public void breakthrough(Player player, String conditionals) {
    }

    public void tribulationComplete(Player player) {
        hasTribulated = true;

        if (player instanceof ServerPlayer) {
            CultivationAdvancements.HAS_BROKENTROUGH.trigger((ServerPlayer) player, true);
        }
    }

    public CultivationTypeScreen getScreen() {
        return screen;
    }

    public String breakthroughProgress(Player player) {
        String progress = Component.translatable("cultivationcraft.gui.cultivationprogress").getString();

        if (statsCanLevel()) {
            progress = progress + ": " + getTechLevelProgressWithoutPrevious(passive.getClass().toString()) + "/" + techLevel; 
        }else {
            progress = progress + ": " + getQiLevelProgress() + "/" + techLevel;
        }

        if (maxedTechsToBreakthrough > 0) {
            progress += "\n" + Component.translatable("cultivationcraft.gui.completedtech").getString() + ": " + getMaxedTechs() + "/" + maxedTechsToBreakthrough;
        }

        if (hasTribulation(player)) {
            progress += "\n" + Component.translatable("cultivationcraft.gui.tribulationpending").getString();
        }

        if (getQuest() != null) {
            progress += "\n" + (int) questProgress + "/" + (int) getQuest().complete + " " + getQuest().getDescription();
        }

        return progress;
    }

    public int getMaxedTechs() {
        int i = 0;

        for (String tech : statLevels.keySet()) {
            if (passive.getClass().toString().compareTo(tech) != 0 && getTechLevelProgress(tech) >= getMaxTechLevel()) {
                i++;
            }
        }

        return i;
    }

    public PassiveTechnique getPassive() {
        return passive;
    }

    public double getCultivationStat(Player player, ResourceLocation stat) {
        double amount = 0;

        if (getPassive().hasTechniqueStat(stat)) {
            amount += getPassive().getTechniqueStat(stat, player);
        }
        if (stat.equals(DefaultCultivationStatIDs.qiAbsorbSpeed)) {
            amount += lNalator.Cultivationcraft.Common.Alchemy.PillEffects.absorptionBonus(player);
        }

        return amount;
    }

    public void reset(Player player) {
        statLevels = new HashMap<>();
    }

    public double absorbFromQiSource(double amount, Player player) {
        List<QiSource> sources = ChunkQiSources.getQiSourcesInRange(player.level, player.position(), (int) getCultivationStat(player, DefaultCultivationStatIDs.qiAbsorbRange));

        double remaining = amount;
        float toAbsorb = 0;

        // Draw Qi from each Qi source available
        for (QiSource source : sources) {
            // Only absorb from QiSources of the correct element
            if (canCultivate(source.getElement())) {
                if (remaining > 0) {
                    double absorbed = source.absorbQi(remaining, player);
                    remaining -= absorbed;

                    toAbsorb += absorbed;
                }
            }
        }

        if (toAbsorb > amount) {
            return amount;
        }

        if (toAbsorb < 0) {
            return 0;
        }

        return toAbsorb;
    }

    public double absorbFromQiSource(int amount, Player player, QiSource source) {
        double toAbsorb = 0;

        toAbsorb = 0;

        // Only absorb from QiSources of the correct element
        if (canCultivate(source.getElement())) {
            toAbsorb = source.absorbQi(amount, player);
        }

        if (toAbsorb > amount) {
            return amount;
        }

        if (toAbsorb < 0) {
            return 0;
        }

        return toAbsorb;
    }

    public float progressCultivation(Player player, float Qi, ResourceLocation element) {
        if (!canCultivate(element)) {
            return Qi;
        }

        if (Qi < 0) {
            return 0;
        }

        double currentLevel = qiLevel;

        if (statsCanLevel()) {
            var levels = statLevels.get(getPassive().getClass().toString());
            currentLevel = levels == null ? 0 : levels.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        // Don't cultivate if already at the max, using the same measure as canBreakthrough
        if (currentLevel >= techLevel || (statsCanLevel() && sumStatLevels(getPassive().getClass().toString()) >= techLevel)) {
            return Qi;
        }

        // Calculate accepted Qi directly: subtracting two huge floats loses the
        // stage's remaining capacity when creative commands supply Integer.MAX_VALUE.
        // Qi of a less suited element is partly lost: the source still gives it all
        double efficiency = getCultivationEfficiency(element);
        float accepted = (float) Math.min(Qi, (techLevel - currentLevel) / efficiency);
        increaseCultivationLevel(player, (float) (accepted * efficiency));
        return Qi - accepted;
    }

    public void increaseCultivationLevel(Player player, float amount) {
        if (statsCanLevel()) {
            getPassive().levelUp(player, amount); 
        }else {
            qiLevel += amount;

            // Qi-based realms have their own stage cap; inherited technique levels
            // must not truncate Core Formation progress from pills or spirit stones.
            if (qiLevel > techLevel) {
                qiLevel = techLevel;
            }
        }
    }

    public CultivationType getPreviousCultivation() {
        return previousCultivation;
    }

    public Double getStatLevelWithoutPrevious(Technique tech, ResourceLocation stat) {
        Double level = 0.0;

        if (statLevels.containsKey(tech.getClass().toString()) && statLevels.get(tech.getClass().toString()).containsKey(stat)) {
            level += statLevels.get(tech.getClass().toString()).get(stat);
        }

        return level;
    }

    public Double getStatLevel(Technique tech, ResourceLocation stat) {
        if (tech.getClass().toString().compareTo(passive.getClass().toString()) == 0) {
            return getPassiveStatLevel(stat);
        }

        Double level = 0.0;

        if (statLevels.containsKey(tech.getClass().toString()) && statLevels.get(tech.getClass().toString()).containsKey(stat)) {
            level += statLevels.get(tech.getClass().toString()).get(stat);
        }

        if (previousCultivation != null) {
            level += previousCultivation.getStatLevel(tech, stat);
        }

        return level;
    }

    public Double getPassiveStatLevel(ResourceLocation stat) {
        Double level = 0.0;

        if (statsCanLevel()) {
            String passiveString = getPassive().getClass().toString();

            if (statLevels.containsKey(passiveString) && statLevels.get(passiveString).containsKey(stat)) {
                level += statLevels.get(passiveString).get(stat);
            }
        }
        /*else
            level += qiLevel;*/

        if (previousCultivation != null) {
            level += previousCultivation.getPassiveStatLevel(stat);
        }

        return level;
    }

    public int getMaxTechLevelWithoutPrevious() {
        return techLevel;
    }

    public int getMaxTechLevel() {
        int level = 0;

        if (statsCanLevel()) {
            level = techLevel;
        }

        if (previousCultivation != null) {
            level += getPreviousCultivation().getMaxTechLevel();
        }

        return level;
    }

    public int getTechLevelProgress(Technique tech) {
        return getTechLevelProgress(tech.getClass().toString());
    }

    public int getQiLevelProgress() {
        if (qiLevel >= techLevel) {
            return techLevel;
        }

        return (int) qiLevel;
    }

    // Sum a technique's stat levels before truncating: truncating each stat separately lost up to
    // 1 level per stat, so a stage could stop accepting Qi while showing less than its maximum
    protected int sumStatLevels(String tech) {
        if (!statLevels.containsKey(tech)) {
            return 0;
        }

        double total = 0;
        for (double value : statLevels.get(tech).values()) {
            total += value;
        }

        // Float accumulation can leave 299.9999 for a full 300
        return (int) Math.floor(total + 1e-4);
    }

    public int getTechLevelProgressWithoutPrevious(String tech) {
        int progress = 0;

        progress += sumStatLevels(tech);

        return progress;
    }

    public int getTechLevelProgress(String tech) {
        if (tech.compareTo(passive.getClass().toString()) == 0) {
            return getPassiveTechLevelProgress();
        }

        int progress = 0;

        progress += sumStatLevels(tech);

        if (previousCultivation != null) {
            progress += previousCultivation.getTechLevelProgress(tech);
        }

        return progress;
    }

    public int getPassiveTechLevelProgress() {
        int progress = 0;

        String tech = getPassive().getClass().toString();

        progress += sumStatLevels(tech);

        if (previousCultivation != null) {
            progress += previousCultivation.getPassiveTechLevelProgress();
        }

        return progress;
    }

    public void levelTech(Technique tech, double amount, Player player) {
        if (tech.getClass() != passive.getClass()) {
            amount *= getTechniqueTrainingMultiplier();
        }

        int max = getMaxTechLevel();
        int current = getTechLevelProgress(tech);

        if (max == current) {
            // If this technique is maxed then trigger the appropriate achievements
            if (player instanceof ServerPlayer) {
                CultivationAdvancements.TECH_USE.trigger((ServerPlayer) player, tech.getClass().getName(), 2);
            }

            return;
        }

        double amountToLevel = max - current;

        if (amount >= amountToLevel) {
            amount = amountToLevel;

            if (player instanceof ServerPlayer) {
                CultivationAdvancements.TECH_USE.trigger((ServerPlayer) player, tech.getClass().getName(), 2);

                // The passive filling up means the stage's cultivation is full
                boolean stageFull = tech.getClass() == passive.getClass();
                CultivationNotifier.notify(player, CultivationNotifier.Type.PROGRESS,
                        Component.translatable(stageFull ? "cultivationcraft.notify.stage_full" : "cultivationcraft.notify.technique_mastered"),
                        stageFull ? Component.translatable(ID.toLanguageKey()) : Component.translatable(tech.getLangLocation()));
            }
        }

        ResourceLocation toLevel = CultivatorStats.getCultivatorStats(player).getTechniqueFocus(tech);

        if (toLevel == null) {
            toLevel = (ResourceLocation) tech.getTechniqueStats().toArray()[0];
        }

        double currentLevel = amount;

        if (!statLevels.containsKey(tech.getClass().toString())) {
            statLevels.put(tech.getClass().toString(), new HashMap<>());
        }

        if (statLevels.get(tech.getClass().toString()).containsKey(toLevel)) {
            currentLevel += statLevels.get(tech.getClass().toString()).get(toLevel);
        }

        statLevels.get(tech.getClass().toString()).put(toLevel, currentLevel);

        PacketHandler.sendCultivatorStatsToClient(player);
    }

    public void setPreviousCultivation(CultivationType previous) {
        previousCultivation = previous;
    }

    public CompoundTag writeNBT() {
        CompoundTag nbt = new CompoundTag();

        if (getPreviousCultivation() != null) {
            nbt.putString("PREVIOUSCULTNAME", getPreviousCultivation().getClass().toString());
            nbt.put("PREVIOUSCULT", getPreviousCultivation().writeNBT());
        }

        nbt.putInt("STAGE", getStage());
        nbt.putBoolean("TRIBULATING", isTribulating);
        nbt.putBoolean("TRIBULATED", hasTribulated);

        int i = 0;

        for (TechniqueModifier mod : modifiers) {
            nbt.putString("MODIFIER" + i, mod.getID().toString());
            i++;
        }

        if (quest != null) {
            nbt.put("QUEST", quest.writeNBT());
        }

        i = 0;

        for (Map.Entry<String, HashMap<ResourceLocation, Double>> tech : statLevels.entrySet()) {
            nbt.putString("TECH" + i + "CLASS", tech.getKey());

            int j = 0;

            for (Map.Entry<ResourceLocation, Double> techEntry : tech.getValue().entrySet()) {
                nbt.putString("TECH" + i + "ENTRY" + j + "NAME", techEntry.getKey().toString());
                nbt.putDouble("TECH" + i + "ENTRY" + j + "VALUE", techEntry.getValue());

                j++;
            }

            i++;
        }

        nbt.putDouble("QUESTPROGRESS", questProgress);
        nbt.putDouble("QIPROGRESS", qiLevel);

        if (advancingCultivation != null) {
            nbt.putString("ADVANCINGNAME", advancingCultivation.getClass().toString());
            nbt.put("ADVANCING", advancingCultivation.writeNBT());
        }

        return nbt;
    }

    public void readNBT(CompoundTag nbt) {
        if (nbt.contains("PREVIOUSCULTNAME")) {
            CultivationType newCultivation = ExternalCultivationHandler.getCultivation(nbt.getString("PREVIOUSCULTNAME"));
            newCultivation.readNBT(nbt.getCompound("PREVIOUSCULT"));

            previousCultivation = newCultivation;
        }

        stage = nbt.getInt("STAGE");
        stageCalculations();

        isTribulating = nbt.getBoolean("TRIBULATING");
        hasTribulated = nbt.getBoolean("TRIBULATED");

        HashMap<String, HashMap<ResourceLocation, Double>> newTechLevels = new HashMap<>();

        int i = 0;

        modifiers = new ArrayList<>();

        while (nbt.contains("MODIFIER" + i)) {
            modifiers.add(ExternalCultivationHandler.getModifier(new ResourceLocation(nbt.getString("MODIFIER" + i))));
            i++;
        }

        if (nbt.contains("QUEST")) {
            quest = Quest.readNBT(nbt.getCompound("QUEST"));
        }

        i = 0;

        while (nbt.contains("TECH" + i + "CLASS")) {
            int j = 0;

            HashMap<ResourceLocation, Double> values = new HashMap<>();

            while (nbt.contains("TECH" + i + "ENTRY" + j + "NAME")) {
                values.put(new ResourceLocation(nbt.getString("TECH" + i + "ENTRY" + j + "NAME")), nbt.getDouble("TECH" + i + "ENTRY" + j + "VALUE"));
                j++;
            }

            newTechLevels.put(nbt.getString("TECH" + i + "CLASS"), values);

            i++;
        }

        statLevels = newTechLevels;

        questProgress = nbt.getDouble("QUESTPROGRESS");
        qiLevel = nbt.getDouble("QIPROGRESS");

        if (nbt.contains("ADVANCING")) {
            CultivationType newCultivation = ExternalCultivationHandler.getCultivation(nbt.getString("ADVANCINGNAME"));
            newCultivation.readNBT(nbt.getCompound("ADVANCING"));

            advancingCultivation = newCultivation;
        }
    }
}
