package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques.FlyingSwordFormationTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

// Rhythm axis, opposite of Haste: everything slower but stronger. Qi Emission charges while its key is held (see QiEmission)
public class ChargeModifier extends TechniqueModifier {

    public static final ResourceLocation CHARGE_ID = new ResourceLocation(Cultivationcraft.MODID, "concept.charge");

    // Ticks to reach the full charge
    public static final int CHARGE_TICKS = 40;

    // TODO core texture (animated coreTexture shown in the cultivation screen): light slowly filling the core, then a bright flash when full (no texture yet, uses blank.png)
    public ChargeModifier() {
        ID = CHARGE_ID;
        CATEGORY = RHYTHM_CATEGORY;

        unlockQuest = new Quest(Quest.BIG_HIT, 10);
        stabiliseQuest = new Quest(Quest.BIG_HIT, 1000);
    }

    // Full charge: x2 damage and cost, +0.5 per extra rank
    public float getMaxCharge(Player owner) {
        return 2 + 0.5f * (getRank(owner) - 1);
    }

    // Swords strike less often but harder
    @Override
    public float getDamageMultiplier(Player owner, Technique tech) {
        return tech instanceof FlyingSwordFormationTechnique ? 1.5f : 1;
    }

    @Override
    public float getTempoMultiplier(Player owner) {
        return 0.75f;
    }

    // The aura pulses every 1.5 s, 1.5x stronger
    @Override
    public float getAuraIntervalMultiplier(Player owner) {
        return 1.5f;
    }

    @Override
    public float getAuraDamageMultiplier(Player owner) {
        return 1.5f;
    }
}
