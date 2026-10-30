package lNalator.Cultivationcraft.Common.Qi.Elements;

import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.awt.*;

public class EarthElement extends Element {

    public EarthElement(ResourceLocation resourcelocation, Color elementColor, double newDensity) {
        super(resourcelocation, elementColor, newDensity);
    }

    public void applyStatusEffect(QiDamageSource source, Entity target, float damageAmount) {
        // Only cultivator attacks stun, not stalactites
        if (damageAmount > 0 && source.getEntity() != null && target instanceof LivingEntity living && !target.level().isClientSide) {
            living.addEffect(new MobEffectInstance(ElementalStates.STUNNED.get(), 20));
        }

        super.applyStatusEffect(source, target, damageAmount);
    }
}
