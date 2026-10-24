package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.Elements.ElementalStates;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Technique hits poison the target and block its regeneration (Sea of Qi included); each rank lengthens both
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID)
public class PoisonModifier extends TechniqueModifier {

    public static final ResourceLocation POISON_ID = new ResourceLocation(Cultivationcraft.MODID, "concept.poison");

    // 3 s, +2 s per extra rank
    protected static final int BASE_DURATION = 60;
    protected static final int RANK_DURATION = 40;

    // TODO core design: a sickly green core bubbling and dripping
    public PoisonModifier() {
        ID = POISON_ID;
        CATEGORY = MODIFIER_CATEGORY;
        allowSameCategory = true;

        // Seconds meditating while poisoned; reset on death
        unlockQuest = new Quest(Quest.POISON_MEDITATION, 300);
        stabiliseQuest = new Quest(Quest.POISON_APPLIED, 1000);
    }

    @Override
    public void onHitEntity(Player owner, Entity hit, float damage, ResourceLocation element, Vec3 pos) {
        super.onHitEntity(owner, hit, damage, element, pos);

        if (owner.level.isClientSide || hit == owner || !(hit instanceof LivingEntity target)) {
            return;
        }

        int duration = BASE_DURATION + RANK_DURATION * (getRank(owner) - 1);
        target.addEffect(new MobEffectInstance(MobEffects.POISON, duration), owner);
        target.addEffect(new MobEffectInstance(ElementalStates.NO_REGEN.get(), duration), owner);
        QuestHandler.progressQuest(owner, Quest.POISON_APPLIED, 1);
    }

    @SubscribeEvent
    public static void blockHealing(LivingHealEvent event) {
        if (event.getEntity().hasEffect(ElementalStates.NO_REGEN.get())) {
            event.setCanceled(true);
        }
    }

    // The unlock quest only counts time poisoned without dying
    @SubscribeEvent
    public static void resetOnDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PoisonModifier poison = new PoisonModifier();

            if (!poison.hasLearnt(player)) {
                CultivatorStats.getCultivatorStats(player).setConceptProgress(POISON_ID, 0);
            }
        }
    }
}
