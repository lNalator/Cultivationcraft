package lNalator.Cultivationcraft.Common.Qi.Damage;

import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Config;
import net.minecraft.world.entity.Entity;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.PlayerHealthManager;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import lNalator.Cultivationcraft.Common.Qi.Elements.ElementInteractions;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Stats.BodyPartStatControl;
import lNalator.Cultivationcraft.Common.Qi.Stats.PlayerStatModifications;
import lNalator.Cultivationcraft.Common.Qi.Stats.StatIDs;
import lNalator.Cultivationcraft.Common.Qi.Techniques.PassiveTechniques.PassiveTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.TechniqueModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

public class Damage {

    public static float damage(LivingHurtEvent event) {
        QiDamageSource source = damageSourceToQiDamageSource(event.getSource());

        float amount = resistDamage(event.getAmount(), source.damageElement, (Player) event.getEntity());
        amount = doTechniqueResistances(source, amount, (Player) event.getEntity());
        amount *= pressureMultiplier(source.getEntity(), (Player) event.getEntity());

        // Double the invincibility time when hit by lightning so lighting bolts don't double hit
        if (source.getElement().compareTo(Elements.lightningElement) == 0) {
            event.getEntity().hurtTime = event.getEntity().hurtDuration * 2;
        }

        return amount;
    }

    // Golden Core: extra damage when the attacker's core element presses the target's core element
    public static float pressureMultiplier(Entity attacker, Player target) {
        if (!(attacker instanceof Player attackingPlayer)) {
            return 1;
        }

        for (ResourceLocation attackElement : CultivatorStats.getCultivatorStats(attackingPlayer).getCultivation().getElements()) {
            for (ResourceLocation targetElement : CultivatorStats.getCultivatorStats(target).getCultivation().getElements()) {
                if (Elements.presses(attackElement, targetElement)) {
                    return Config.Server.pressureDamageMultiplier();
                }
            }
        }

        return 1;
    }

    public static float doTechniqueResistances(QiDamageSource source, float amount, Player player) {
        // Tick through all active cultivator techniques
        ICultivatorTechniques techs = CultivatorTechniques.getCultivatorTechniques(player);

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            Technique tech = techs.getTechnique(i);

            if (tech != null) {
                if (tech.isValid(player)) {
                    if (tech.isActive()) {
                        amount = tech.onDamage(source, amount, player);
                    }
                }
            }
        }

        for (PassiveTechnique passive : techs.getPassives()) {
            if (passive.isValid(player)) {
                if (passive.isActive()) {
                    amount = passive.onDamage(source, amount, player);
                }
            }
        }

        return amount;
    }

    public static float resistDamage(float damage, ResourceLocation element, Player player) {
        PlayerStatModifications stats = BodyPartStatControl.getStats(player);

        float multiplier = (1 - (stats.getElementalStat(StatIDs.resistanceModifier, element) / 100.0f));

        if (multiplier < 0 && !PlayerHealthManager.getBlood(player).canHeal(element, player)) {
            multiplier = 0;
        }

        return damage * multiplier;
    }

    public static void applyStatusEffects(LivingHurtEvent event) {
        QiDamageSource source = damageSourceToQiDamageSource(event.getSource());

        if (source.getElement() != null && source.doStatusEffect()) {
            // Element interactions with the target's current states (design D3), before this hit adds its own
            ElementInteractions.Result result = ElementInteractions.apply(source.getElement(), source, event.getEntity(), event.getAmount());
            event.setAmount(result.amount());

            if (result.skipStatus()) {
                return;
            }

            Elements.getElement(source.getElement()).applyStatusEffect(source, event.getEntity(), event.getAmount() * elementRankMultiplier(source));
        }
    }

    // A reinforced elemental core makes its states 25% longer and stronger per extra rank.
    // States scale with the damage they are given, so only that amount is raised, not the hit itself
    public static float elementRankMultiplier(QiDamageSource source) {
        if (!(source.getEntity() instanceof Player attacker)) {
            return 1;
        }

        for (TechniqueModifier mod : CultivatorStats.getCultivatorStats(attacker).getCultivation().getModifiers()) {
            if (mod.hasElement() && mod.getElement().equals(source.getElement())) {
                return 1 + 0.25f * (mod.getRank(attacker) - 1);
            }
        }

        return 1;
    }

    public static float damageEntity(LivingHurtEvent event) {
        float damage = event.getAmount() * mobResistance(event.getEntity(), event.getSource());

        if (event.getSource().getEntity() != null && event.getSource().getEntity() instanceof ServerPlayer) {
            QuestHandler.damageProgress((Player) event.getSource().getEntity(), damageSourceToQiDamageSource(event.getSource()), damage);

            // Charge unlock: heavy single hits
            if (damage >= 20) {
                QuestHandler.progressQuest((Player) event.getSource().getEntity(), Quest.BIG_HIT, 1);
            }
        }

        return damage;
    }

    // Mob elemental resistances from entity tags (design D5), editable by datapack:
    // cultivationcraft:resists/<element> = x0.5, cultivationcraft:weak/<element> = x1.5.
    // Only real Qi attacks: converted vanilla damage (explosion counted as Fire...) is left alone.
    public static float mobResistance(LivingEntity entity, DamageSource damage) {
        if (!(damage instanceof QiDamageSource source) || source.getElement() == null) {
            return 1;
        }

        String path = source.getElement().getPath();
        String element = path.substring(path.lastIndexOf('.') + 1);

        if (source.getElement().equals(Elements.fireElement) && entity.fireImmune()) {
            return 0;
        }

        float multiplier = 1;

        if (entity.getType().is(TagKey.create(Registry.ENTITY_TYPE_REGISTRY, new ResourceLocation(Cultivationcraft.MODID, "resists/" + element)))) {
            multiplier *= 0.5f;
        }

        if (entity.getType().is(TagKey.create(Registry.ENTITY_TYPE_REGISTRY, new ResourceLocation(Cultivationcraft.MODID, "weak/" + element)))) {
            multiplier *= 1.5f;
        }

        return multiplier;
    }

    public static boolean shouldCancel(LivingAttackEvent event) {
        QiDamageSource source = damageSourceToQiDamageSource(event.getSource());
        float damage = armorAbsorption((Player) event.getEntity(), source, event.getAmount());
        float resistedDamage = resistDamage(damage, source.damageElement, (Player) event.getEntity());

        if (source.getElement() != null && source.getElement().compareTo(Elements.fireElement) == 0 && event.getEntity().fireImmune() && resistedDamage > 0) {
            resistedDamage = 0;
        }

        if (resistedDamage <= 0) {
            QuestHandler.progressQuest((Player) event.getEntity(), Quest.DAMAGE_RESISTED, event.getAmount());
            QuestHandler.progressQuest((Player) event.getEntity(), Quest.DAMAGE_RESISTED, event.getAmount(), source.getElement().toString());

            float heal = resistedDamage * -1;

            // Cap amount lava can heal to 2hp a second
            if (event.getSource().getMsgId().compareTo(DamageSource.LAVA.getMsgId()) == 0 && heal > 0.2f) {
                heal = 0.2f;
            }

            // If taking negative damage then heal that amount
            event.getEntity().heal(heal);

            return true;
        }

        if (event.getEntity().hurtTime == 0 && event.getEntity().isAlive()) {
            QuestHandler.progressQuest((Player) event.getEntity(), Quest.DAMAGE_TAKEN, resistedDamage);

            QuestHandler.progressQuest((Player) event.getEntity(), Quest.DAMAGE_TAKEN, resistedDamage, source.msgId);

            if (source.isExplosion()) {
                QuestHandler.progressQuest((Player) event.getEntity(), Quest.EXPLOSION_DAMAGE_TAKEN, resistedDamage);
            }

            QuestHandler.progressQuest((Player) event.getEntity(), Quest.DAMAGE_RESISTED, event.getAmount() - resistedDamage);
            QuestHandler.progressQuest((Player) event.getEntity(), Quest.DAMAGE_RESISTED, event.getAmount() - resistedDamage, source.getElement().toString());
        }

        if (event.getSource().getEntity() != null && event.getSource().getEntity() instanceof ServerPlayer) {
            QuestHandler.damageProgress((Player) event.getSource().getEntity(), damageSourceToQiDamageSource(event.getSource()), resistedDamage);
        }

        Vec3 position = source.getSourcePosition();

        if (position == null && source.getEntity() != null) {
            position = source.getEntity().position();
        }

        // Do not spawn blood from internal damage
        if (!source.isInternal()) {
            PlayerHealthManager.getBlood((Player) event.getEntity()).onHit((Player) event.getEntity(), position, event.getAmount());
        }

        return false;
    }

    protected static float armorAbsorption(Player player, QiDamageSource source, float amount) {
        if (!source.isBypassArmor()) {
            amount = CombatRules.getDamageAfterAbsorb(amount, (float) player.getArmorValue(), (float) player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        }

        if (!source.isBypassMagic()) {
            if (player.hasEffect(MobEffects.DAMAGE_RESISTANCE) && source != DamageSource.OUT_OF_WORLD) {
                int i = (player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - i;
                float f = amount * (float) j;
                amount = Math.max(f / 25.0F, 0.0F);
            }

            if (!source.isBypassEnchantments()) {
                int k = EnchantmentHelper.getDamageProtection(player.getArmorSlots(), source);
                if (k > 0) {
                    amount = CombatRules.getDamageAfterMagicAbsorb(amount, (float) k);
                }
            }
        }

        if (amount < 0) {
            amount = 0;
        }

        // Absorption hearts are not subtracted here: vanilla consumes them later in actuallyHurt. Subtracting them
        // in this preview cancelled every hit smaller than the absorption amount, so it was never used up (unkillable).
        return amount;
    }

    protected static QiDamageSource damageSourceToQiDamageSource(DamageSource damage) {
        if (damage instanceof QiDamageSource) {
            return (QiDamageSource) damage;
        }

        return new QiDamageSource(damage);
    }
}
