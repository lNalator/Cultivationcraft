package lNalator.Cultivationcraft.Common.Qi.Elements;

import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.Effects.Wind;
import lNalator.Cultivationcraft.Common.Qi.Effects.WindInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.RegistryObject;

// What an element does to a target already carrying a state (design D3), applied before the hit's own status.
// A state that an interaction removes is consumed, so the same combination does not trigger twice in a row.
public final class ElementInteractions {

    public record Result(float amount, boolean skipStatus) {
    }

    public static Result apply(ResourceLocation element, QiDamageSource source, LivingEntity t, float amount) {
        if (t.level.isClientSide || amount <= 0) {
            return new Result(amount, false);
        }

        boolean onFire = t.isOnFire();
        boolean wet = has(t, ElementalStates.WET);
        boolean chill = has(t, ElementalStates.CHILL);
        boolean frozen = has(t, ElementalStates.FROZEN);
        boolean offBalance = has(t, ElementalStates.OFF_BALANCE);
        boolean electrified = has(t, ElementalStates.ELECTRIFIED);
        boolean stunned = has(t, ElementalStates.STUNNED);
        boolean rooted = has(t, ElementalStates.ROOTED);
        boolean skip = false;

        if (is(element, Elements.fireElement)) {
            if (wet) { remove(t, ElementalStates.WET); amount *= 0.5f; steam(t); skip = true; }
            if (chill || frozen) { remove(t, ElementalStates.CHILL); remove(t, ElementalStates.FROZEN); amount *= 1.5f; }
            if (offBalance) { spreadFire(t, 3); }
            if (electrified) {
                remove(t, ElementalStates.ELECTRIFIED);
                t.level.explode(null, t.getX(), t.getY() + 0.5, t.getZ(), 0.5f, Explosion.BlockInteraction.NONE);
            }
            if (stunned) { t.setRemainingFireTicks(t.getRemainingFireTicks() + 40); }
            if (rooted) { remove(t, ElementalStates.ROOTED); amount *= 1.5f; }
        } else if (is(element, Elements.waterElement)) {
            if (onFire) { steam(t); }
            if (chill) { ElementalStates.addChill(t, source.getEntity()); }
            if (frozen) { extend(t, ElementalStates.FROZEN, 20); }
            if (electrified) { conduct(t, source); }
            if (stunned) { extend(t, ElementalStates.STUNNED, 10); }
            if (rooted) { extend(t, ElementalStates.ROOTED, 20); }
        } else if (is(element, Elements.iceElement)) {
            if (onFire) { t.clearFire(); steam(t); skip = true; }
            else if (wet && !has(t, ElementalStates.FREEZE_IMMUNE)) { remove(t, ElementalStates.WET); ElementalStates.freeze(t, source.getEntity()); skip = true; }
            if (frozen) { extend(t, ElementalStates.FROZEN, 10); }
            if (offBalance && t.isOnGround()) { push(t, source, 0.3f); }
            if (electrified) { extend(t, ElementalStates.ELECTRIFIED, 20); }
            if (stunned) { ElementalStates.addChill(t, source.getEntity()); }
            if (rooted && !has(t, ElementalStates.FREEZE_IMMUNE)) {
                remove(t, ElementalStates.ROOTED);
                t.addEffect(new MobEffectInstance(ElementalStates.FROZEN.get(), 20));
                skip = true;
            }
        } else if (is(element, Elements.windElement)) {
            if (rooted) {
                // Anchored by its roots: no push, no Off Balance
                return new Result(amount, true);
            }
            if (onFire) { t.setRemainingFireTicks((int) (t.getRemainingFireTicks() * 1.5f)); spreadFire(t, 2); }
            if (wet) { remove(t, ElementalStates.WET); ElementalStates.addChill(t, source.getEntity()); }
            if (chill) { ElementalStates.addChill(t, source.getEntity()); }
            if (frozen) { push(t, source, amount * 0.025f); }
            if (offBalance) { push(t, source, amount * 0.025f * 0.25f); }
            if (electrified) { remove(t, ElementalStates.ELECTRIFIED); amount *= 1.1f; }
            if (stunned) { t.setDeltaMovement(t.getDeltaMovement().add(0, 0.8, 0)); t.hurtMarked = true; }
        } else if (is(element, Elements.lightningElement)) {
            if (onFire) { amount *= 1.25f; }
            if (frozen) { amount *= 1.25f; }
            if (offBalance) { t.addEffect(new MobEffectInstance(ElementalStates.STUNNED.get(), 10)); }
            if (electrified) { remove(t, ElementalStates.ELECTRIFIED); amount *= 1.5f; skip = true; }
            if (stunned) { extend(t, ElementalStates.STUNNED, 10); }
            if (rooted) { remove(t, ElementalStates.ROOTED); amount *= 0.5f; skip = true; }
            // Wet (x1.5, +1 chain target) is handled with the chain in LightningElement
            if (wet) { amount *= 1.5f; }
        } else if (is(element, Elements.earthElement)) {
            amount = pierceArmor(t, amount);
            if (onFire) { t.clearFire(); }
            if (wet) { t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); }
            if (chill) { amount *= 1.25f; }
            if (frozen) {
                remove(t, ElementalStates.FROZEN);
                t.addEffect(new MobEffectInstance(ElementalStates.FREEZE_IMMUNE.get(), ElementalStates.FREEZE_IMMUNE_TICKS));
                amount *= 2f;
            }
            if (offBalance) { t.addEffect(new MobEffectInstance(ElementalStates.STUNNED.get(), 40)); }
            if (electrified) { remove(t, ElementalStates.ELECTRIFIED); }
            if (stunned) { extend(t, ElementalStates.STUNNED, 20); }
            if (rooted) { amount *= 1.25f; }
        } else if (is(element, Elements.woodElement)) {
            // Chill, Off Balance and Stunned change the Wood status itself (see WoodElement)
            if (onFire) { t.setRemainingFireTicks(t.getRemainingFireTicks() + 40); }
            if (wet) { t.addEffect(new MobEffectInstance(ElementalStates.ROOTED.get(), 40)); }
            if (electrified) { remove(t, ElementalStates.ELECTRIFIED); }
            if (rooted) { extend(t, ElementalStates.ROOTED, 20); }
        }

        return new Result(amount, skip);
    }

    static boolean is(ResourceLocation a, ResourceLocation b) {
        return a != null && a.compareTo(b) == 0;
    }

    static boolean has(LivingEntity t, RegistryObject<MobEffect> state) {
        return t.hasEffect(state.get());
    }

    static void remove(LivingEntity t, RegistryObject<MobEffect> state) {
        t.removeEffect(state.get());
    }

    // ponytail: no per-state cap beyond the effect's own; extensions stay small (0.5-1 s)
    static void extend(LivingEntity t, RegistryObject<MobEffect> state, int ticks) {
        MobEffectInstance current = t.getEffect(state.get());
        if (current != null) {
            t.removeEffect(state.get());
            t.addEffect(new MobEffectInstance(state.get(), current.getDuration() + ticks, current.getAmplifier()));
        }
    }

    static void steam(LivingEntity t) {
        if (t.level instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CLOUD, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        }
    }

    static void spreadFire(LivingEntity t, int seconds) {
        for (LivingEntity near : t.level.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(1.5), e -> e != t)) {
            near.setSecondsOnFire(seconds);
        }
    }

    // Water conducts lightning: small damage to wet entities nearby
    static void conduct(LivingEntity t, QiDamageSource source) {
        for (LivingEntity near : t.level.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(4), e -> e != t && e != source.getEntity() && e.hasEffect(ElementalStates.WET.get()))) {
            near.hurt(DamageSource.MAGIC, 2);
        }
    }

    static void push(LivingEntity t, QiDamageSource source, float speed) {
        Vec3 from = source.getSourcePosition();
        if (from == null) {
            return;
        }
        Wind.addWindEffect(t, new WindInstance(t.position().subtract(from).normalize(), speed, speed * 8));
    }

    // Earth ignores 25% of the target's armor: scale the damage by how much less armor would have absorbed
    static float pierceArmor(LivingEntity t, float amount) {
        float armor = t.getArmorValue();
        float toughness = (float) t.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
        float full = CombatRules.getDamageAfterAbsorb(amount, armor, toughness);
        if (full <= 0) {
            return amount;
        }
        return amount * CombatRules.getDamageAfterAbsorb(amount, armor * 0.75f, toughness) / full;
    }
}
