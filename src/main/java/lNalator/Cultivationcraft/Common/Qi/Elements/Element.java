package lNalator.Cultivationcraft.Common.Qi.Elements;

import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.awt.*;
import java.util.ArrayList;

public class Element {

    protected final ResourceLocation name;
    public final Color color;
    public final double density;

    protected double effectTickChance;

    protected ArrayList<ElementVariant> variant = new ArrayList<>();

    public Element(ResourceLocation resourcelocation, Color elementColor, double newDensity) {
        name = resourcelocation;
        color = elementColor;
        density = newDensity;
        effectTickChance = 0;
    }

    public String getName() {
        return Component.translatable(name.getPath()).getString();
    }

    public ResourceLocation getResourceLocation() {
        return name;
    }

    public boolean shouldDoBlockEffect() {
        if (Math.random() < effectTickChance) {
            return true;
        }

        return false;
    }

    // TODO: bigger visual feedback (particles, sound) when an element hits an entity or a block (also effectBlock)
    public void applyStatusEffect(QiDamageSource source, Entity target, float damageAmount) {
        // Every element counts for the elemental-effect quests, not only those with a block effect
        if (damageAmount > 0 && name.compareTo(Elements.noElement) != 0) {
            if (target instanceof Player) {
                QuestHandler.progressQuest((Player) target, Quest.ELEMENTALY_EFFECTED, 1);
                QuestHandler.progressQuest((Player) target, Quest.ELEMENTALY_EFFECTED, 1, name.toString());
            }

            if (source.getEntity() instanceof Player) {;
                QuestHandler.progressQuest((Player) source.getEntity(), Quest.ELEMENTAL_EFFECT_APPLIED, 1);
                QuestHandler.progressQuest((Player) source.getEntity(), Quest.ELEMENTAL_EFFECT_APPLIED, 1, name.toString());
            }
        }
    }

    public void effectBlock(Level level, BlockPos pos) {
    }

    // Block effect with the hit's damage, for elements whose block effect scales (Ice freeze duration)
    public void effectBlock(Level level, BlockPos pos, float damage) {
        effectBlock(level, pos);
    }

    public float resistanceModifier(Player player) {
        return 0;
    }

    public void addVariant(ElementVariant element) {
        variant.add(element);
    }

    public Element getMutation() {
        for (ElementVariant variant : variant) {
            if (variant.tryMutate()) {
                return variant;
            }
        }

        return this;
    }
}
