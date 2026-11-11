package lNalator.Cultivationcraft.Common.Qi.Cultivation;

import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.CultivationNotifier;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Techniques.PassiveTechniques.CultivationPassives.QiCondenserPassive;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QiCondenserCultivation extends CultivationType {

    protected HashMap<ResourceLocation, Float> elementProgression = new HashMap<ResourceLocation, Float>();

    // Purge: while meditating, the Qi of one element is removed at a quarter of the absorption speed, the most recent
    // stage first, and leaves a void in its stage. Cultivated Qi fills the voids first, the oldest stage first
    public static final float PURGE_SPEED = 0.25f;
    // Near a source of an element that presses the purged one (Water dissolves Fire), the purge goes faster
    public static final float DOMINANT_PURGE_BONUS = 1.5f;
    // Pure Qi feeds every element, but less well: 1 Pure Qi fills 0.5 void
    public static final float PURE_VOID_FILL = 0.5f;

    // Void left in this stage by purges
    protected float voidQi = 0;
    // Element being purged, kept from stage to stage
    protected ResourceLocation purgingElement = Elements.noElement;

    public QiCondenserCultivation() {
        this(1);
    }

    public QiCondenserCultivation(int cultivationStage) {
        super(cultivationStage);
        passive = new QiCondenserPassive();
        maxedTechsToBreakthrough = 3;
        maxStage = 8;
        tribulation = new Tribulation(maxStage, 50, 0.4f);

        advancements.add(new CoreFormingCultivation());

        ID = new ResourceLocation(Cultivationcraft.MODID, "cultivation.qicondensation");
    }

    @Override
    public boolean hasSeaOfQi() {
        return true;
    }

    public void stageCalculations() {
        techLevel = 200 + (100 * stage);
    }

    // Core Forming is blocked while a void is left
    public boolean canBreakthrough(Player player) {
        if (stage == maxStage && getVoid() > 0) {
            return false;
        }

        return super.canBreakthrough(player);
    }

    @Override
    public String breakthroughProgress(Player player) {
        String progress = super.breakthroughProgress(player);

        if (getVoid() > 0) {
            progress += "\n" + Component.translatable("cultivationcraft.gui.purge.void_left", (int) Math.ceil(getVoid())).getString();
        }

        return progress;
    }

    @Override
    public void breakthrough(Player player, String conditionals) {
        if (stage < maxStage) {
            QiCondenserCultivation newCultivation = new QiCondenserCultivation(stage + 1);
            newCultivation.setPreviousCultivation(this);
            newCultivation.purgingElement = purgingElement;

            CultivatorStats.getCultivatorStats(player).setCultivation(newCultivation);
        }
    }

    @Override
    public float progressCultivation(Player player, float Qi, ResourceLocation element) {
        if (Qi == 0) {
            return 0;
        }

        // The void left by purges is filled first, even when the stage is full
        float remaining = fillVoid(Qi, element);

        if (remaining <= 0) {
            return 0;
        }

        float changedQi = super.progressCultivation(player, remaining, element);

        float usedQi = remaining - changedQi;

        if (!elementProgression.containsKey(element)) {
            elementProgression.put(element, usedQi); 
        }else {
            elementProgression.put(element, elementProgression.get(element) + usedQi);
        }

        return changedQi;
    }

    @Override
    public void reset(Player player) {
        super.reset(player);

        elementProgression = new HashMap<>();
        voidQi = 0;
        purgingElement = Elements.noElement;
    }

    // This stage then the previous Qi Condensation stages, the most recent first
    protected List<QiCondenserCultivation> getStages() {
        List<QiCondenserCultivation> stages = new ArrayList<>();

        for (CultivationType cultivation = this; cultivation instanceof QiCondenserCultivation condenser; cultivation = cultivation.getPreviousCultivation()) {
            stages.add(condenser);
        }

        return stages;
    }

    public ResourceLocation getPurgingElement() {
        return purgingElement;
    }

    public boolean isPurging() {
        return !purgingElement.equals(Elements.noElement);
    }

    // Any absorbed element can be purged, not Pure Qi
    public boolean canPurge(ResourceLocation element) {
        return !element.equals(Elements.anyElement) && getElementAmount(element) > 0;
    }

    public void setPurgingElement(ResourceLocation element) {
        purgingElement = canPurge(element) ? element : Elements.noElement;
    }

    // Qi of this element absorbed over every Qi Condensation stage
    public float getElementAmount(ResourceLocation element) {
        return getCombinedElements().getOrDefault(element, 0f);
    }

    // Qi of each element absorbed over every Qi Condensation stage, Pure Qi under anyElement
    public HashMap<ResourceLocation, Float> getElementAmounts() {
        return new HashMap<>(getCombinedElements());
    }

    // Void left by purges over every Qi Condensation stage
    public float getVoid() {
        float total = 0;

        for (QiCondenserCultivation stage : getStages()) {
            total += stage.voidQi;
        }

        return total;
    }

    // Removes up to this much Qi of the purged element, the most recent stage first. Returns the Qi purged
    public float purge(Player player, float amount) {
        if (!isPurging()) {
            return 0;
        }

        float purged = 0;

        for (QiCondenserCultivation stage : getStages()) {
            if (purged >= amount) {
                break;
            }

            float held = stage.elementProgression.getOrDefault(purgingElement, 0f);
            float removed = Math.min(held, amount - purged);

            if (removed > 0) {
                stage.elementProgression.put(purgingElement, held - removed);
                stage.voidQi += removed;
                purged += removed;
            }
        }

        if (getElementAmount(purgingElement) <= 0) {
            CultivationNotifier.notify(player, CultivationNotifier.Type.PROGRESS, Component.translatable("cultivationcraft.notify.purge_complete"),
                    Component.translatable(purgingElement.getPath()));
            purgingElement = Elements.noElement;
        }

        return purged;
    }

    // Fills the voids with this Qi, the oldest stage first. Returns the Qi left
    protected float fillVoid(float qi, ResourceLocation element) {
        float rate = element.equals(Elements.anyElement) ? PURE_VOID_FILL : 1;
        List<QiCondenserCultivation> stages = getStages();

        for (int i = stages.size() - 1; i >= 0 && qi > 0; i--) {
            QiCondenserCultivation stage = stages.get(i);

            if (stage.voidQi <= 0) {
                continue;
            }

            float filled = Math.min(stage.voidQi, qi * rate);

            stage.voidQi -= filled;
            stage.elementProgression.put(element, stage.elementProgression.getOrDefault(element, 0f) + filled);
            qi -= filled / rate;
        }

        return Math.max(0, qi);
    }

    // Returns the % of current cultivation that is of the selected element for this stage of cultivation
    public float getElementFocusAmountRaw(ResourceLocation element) {
        float progress = getTechLevelProgressWithoutPrevious(passive.getClass().toString());
        float amount = 1;

        // Only try and calculate the % if any Qi has been cultivated so far
        if (progress > 0) {
            amount = 0;

            if (elementProgression.containsKey(element)) {
                amount = elementProgression.get(element);
            }

            // If this is not the anyElement, add values from the any element to this one
            if (element.compareTo(Elements.anyElement) != 0) {
                if (elementProgression.containsKey(Elements.anyElement)) {
                    amount += elementProgression.get(Elements.anyElement);
                }
            }

            if (amount != 0) {
                amount /= progress;
            }
        }

        if (amount > 1) {
            amount = 1;
        }

        // ensuring the % isn't as high if this cultivation level is not yet finished
        amount *= (float) getTechLevelProgressWithoutPrevious(passive.getClass().toString()) / (float) getMaxTechLevelWithoutPrevious();

        if (getPreviousCultivation() instanceof QiCondenserCultivation) {
            amount += ((QiCondenserCultivation) getPreviousCultivation()).getElementFocusAmountRaw(element);
        }

        return amount;
    }

    // Returns the % of current cultivation that is of the selected element
    public float getElementFocusAmount(ResourceLocation element) {
        float amount = getElementFocusAmountRaw(element);

        amount /= (stage - (1 - (float) getTechLevelProgressWithoutPrevious(passive.getClass().toString()) / (float) getMaxTechLevelWithoutPrevious()));

        if (amount > 1) {
            return 1;
        }

        return amount;
    }

    public ResourceLocation getCurrentElementFocus() {
        float amount = 0;
        ResourceLocation currentElement = Elements.noElement;

        HashMap<ResourceLocation, Float> combinedElements = getCombinedElements();

        for (ResourceLocation element : combinedElements.keySet()) {
            if (element.compareTo(Elements.anyElement) != 0) {
                if (combinedElements.get(element) > amount) {
                    amount = combinedElements.get(element);
                    currentElement = element;
                }
            }
        }

        return currentElement;
    }

    protected HashMap<ResourceLocation, Float> getCombinedElements() {
        if (getPreviousCultivation() instanceof QiCondenserCultivation) {
            return combine(elementProgression, ((QiCondenserCultivation) getPreviousCultivation()).getCombinedElements());
        }

        return elementProgression;
    }

    protected HashMap<ResourceLocation, Float> combine(HashMap<ResourceLocation, Float> set1, HashMap<ResourceLocation, Float> set2) {
        HashMap<ResourceLocation, Float> output = new HashMap<ResourceLocation, Float>();

        for (ResourceLocation element : set1.keySet()) {
            output.put(element, set1.get(element));
        }

        for (ResourceLocation element : set2.keySet()) {
            float value = 0;
            if (output.containsKey(element)) {
                value = output.get(element);
            }

            output.put(element, value + set2.get(element));
        }

        return output;
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag nbt = super.writeNBT();

        int i = 0;
        for (Map.Entry<ResourceLocation, Float> elementEntry : elementProgression.entrySet()) {
            nbt.putString("ELEMENT" + i + "NAME", elementEntry.getKey().toString());
            nbt.putFloat("ELEMENT" + i + "VALUE", elementEntry.getValue());
            i++;
        }

        nbt.putFloat("VOID", voidQi);
        nbt.putString("PURGING", purgingElement.toString());

        return nbt;
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        super.readNBT(nbt);

        int i = 0;
        HashMap<ResourceLocation, Float> values = new HashMap<>();

        while (nbt.contains("ELEMENT" + i + "NAME")) {
            values.put(new ResourceLocation(nbt.getString("ELEMENT" + i + "NAME")), nbt.getFloat("ELEMENT" + i + "VALUE"));

            i++;
        }

        elementProgression = values;
        voidQi = nbt.getFloat("VOID");
        purgingElement = nbt.contains("PURGING") ? new ResourceLocation(nbt.getString("PURGING")) : Elements.noElement;
    }
}
