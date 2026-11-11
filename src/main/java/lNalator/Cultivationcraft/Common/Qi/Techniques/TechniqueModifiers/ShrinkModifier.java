package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

// The opposite of Grow on the Size axis: smaller, cheaper and weaker techniques
public class ShrinkModifier extends TechniqueModifier {

    public ShrinkModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.shrink");
        coreTexture = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/cores/shrink.png"), 8, 3);
        CATEGORY = SIZE_CATEGORY;

        unlockQuest = new Quest(Quest.CRAWL, 60);
        stabiliseQuest = new Quest(Quest.TECHNIQUE_USED, 1000);

        damageMult = 0.75f;
        qiMult = 0.6f;
        size = new Vec3(0.75, 0.75, 0.75);
        itemSize = 0.5f;
    }

    // Each rank shrinks by another 20%
    @Override
    public Vec3 getSize(Player owner) {
        return size.scale(Math.pow(0.8, getRank(owner) - 1));
    }
}
