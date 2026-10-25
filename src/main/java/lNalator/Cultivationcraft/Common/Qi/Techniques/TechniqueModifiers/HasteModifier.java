package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

// Rhythm axis: everything faster but weaker. Charge is the opposite pole
public class HasteModifier extends TechniqueModifier {

    // TODO core texture (animated coreTexture shown in the cultivation screen): a fast spin with motion-blur streaks (no texture yet, uses blank.png)
    public HasteModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.haste");
        CATEGORY = RHYTHM_CATEGORY;

        unlockQuest = new Quest(Quest.TECHNIQUE_STREAK, 1);
        stabiliseQuest = new Quest(Quest.TECHNIQUE_USED, 1000);

        damageMult = 0.85f;
    }

    // Projectiles and swords +30% speed, +10% per extra rank
    @Override
    public float getTempoMultiplier(Player owner) {
        return 1.3f + 0.1f * (getRank(owner) - 1);
    }

    // The aura pulses every 0.7 s instead of 1 s
    @Override
    public float getAuraIntervalMultiplier(Player owner) {
        return 0.7f;
    }

    @Override
    public float getAuraDamageMultiplier(Player owner) {
        return damageMult;
    }
}
