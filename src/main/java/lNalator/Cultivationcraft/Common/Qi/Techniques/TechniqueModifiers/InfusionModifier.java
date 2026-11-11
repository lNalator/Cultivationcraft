package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

// Techniques leave their element along their path and around their impact, in a limited version (no block effect)
public class InfusionModifier extends TechniqueModifier {

    // Enchantments that produce or handle an element (design G3); damage-only ones like Impaling do not count
    protected static final Set<Enchantment> ELEMENTAL_ENCHANTMENTS = Set.of(
            Enchantments.FIRE_ASPECT, Enchantments.FLAMING_ARROWS,
            Enchantments.RESPIRATION, Enchantments.AQUA_AFFINITY, Enchantments.DEPTH_STRIDER, Enchantments.RIPTIDE,
            Enchantments.FROST_WALKER, Enchantments.CHANNELING, Enchantments.FALL_PROTECTION, Enchantments.THORNS);

    protected static final String COUNTED_TAG = Cultivationcraft.MODID + "_infusion_counted";

    protected static final double TRAIL_RADIUS = 1;
    protected static final double IMPACT_RADIUS = 2;
    // Infused states are weaker than a direct hit
    protected static final float INFUSED_STRENGTH = 0.5f;

    public InfusionModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.infusion");
        unique = true;
        CATEGORY = MODIFIER_CATEGORY;

        unlockQuest = new Quest(Quest.ELEMENTAL_ITEM, 5);
        stabiliseQuest = new Quest(Quest.ELEMENTAL_EFFECT_APPLIED, 1000);

        allowSameCategory = true;
    }

    // Each item counts once: it is marked when counted, so re-equipping the same one does nothing
    public static void countElementalItem(Player player, ItemStack stack) {
        if (stack.isEmpty() || (stack.hasTag() && stack.getTag().getBoolean(COUNTED_TAG))) {
            return;
        }

        InfusionModifier infusion = new InfusionModifier();

        if (!infusion.canUse(player) || infusion.hasLearnt(player) || !infusion.canLearn(player)) {
            return;
        }

        if (EnchantmentHelper.getEnchantments(stack).keySet().stream().anyMatch(ELEMENTAL_ENCHANTMENTS::contains)) {
            stack.getOrCreateTag().putBoolean(COUNTED_TAG, true);
            QuestHandler.progressQuest(player, Quest.ELEMENTAL_ITEM, 1);
        }
    }

    @Override
    public void tick(Player owner, Vec3 position, ResourceLocation element) {
        // Flight and Qi Barrier tick at the player's own position: only projectiles and swords leave a trail
        if (owner.level.isClientSide || owner.tickCount % 5 != 0 || position.distanceToSqr(owner.position()) < 1) {
            return;
        }

        infuse(owner, position, element, TRAIL_RADIUS, 2);
    }

    @Override
    public void onHitAll(Player owner, Vec3 pos, float damage, ResourceLocation element) {
        if (owner.level.isClientSide) {
            return;
        }

        infuse(owner, pos, element, IMPACT_RADIUS, damage);
    }

    protected void infuse(Player owner, Vec3 pos, ResourceLocation element, double radius, float strength) {
        if (element == null || element.equals(Elements.noElement)) {
            return;
        }

        QiDamageSource source = new QiDamageSource("infusion", owner, element, true);

        for (Entity target : owner.level.getEntities(owner, new AABB(pos, pos).inflate(radius), entity -> entity instanceof LivingEntity && entity.isAlive())) {
            Elements.getElement(element).applyStatusEffect(source, target, strength * INFUSED_STRENGTH);
        }
    }
}
