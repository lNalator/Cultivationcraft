package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques.ItemEnhanceTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques.QiBarrierTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class GrowModifier extends TechniqueModifier {

    // TODO core texture (animated coreTexture shown in the cultivation screen): a core slowly swelling and shrinking, with growth rings (no texture yet, uses blank.png)
    public GrowModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.grow");
        CATEGORY = SIZE_CATEGORY;

        unlockQuest = new Quest(Quest.GROWPLANT, 1);
        // ponytail: placeholder threshold until the per-minute measurement (design C1)
        stabiliseQuest = new Quest(Quest.DAMAGE_ABSORBED, 1000);

        damageMult = 2;
        qiMult = 2;
        size = new Vec3(1.5, 1.5, 1.5);
        itemSize = 2;
    }

    @Override
    public float getQiCostModifier(Technique tech) {
        if (tech instanceof QiBarrierTechnique || tech instanceof ItemEnhanceTechnique) {
            return qiMult;
        }

        return 1;
    }

    // Each rank adds 25% size
    @Override
    public Vec3 getSize(Player owner) {
        return size.scale(1 + 0.25 * (getRank(owner) - 1));
    }
}
