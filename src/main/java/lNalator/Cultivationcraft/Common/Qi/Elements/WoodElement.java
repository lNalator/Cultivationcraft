package lNalator.Cultivationcraft.Common.Qi.Elements;

import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.awt.*;
import java.util.Map;
import java.util.WeakHashMap;

public class WoodElement extends Element {

    protected static final int ROOT_COOLDOWN = 100;
    // Last game time each target was rooted
    protected final Map<Entity, Long> lastRooted = new WeakHashMap<>();

    public WoodElement(ResourceLocation resourcelocation, Color elementColor, double newDensity) {
        super(resourcelocation, elementColor, newDensity);
    }

    public void applyStatusEffect(QiDamageSource source, Entity target, float damageAmount) {
        // Only cultivator attacks drain and root, not cactus or berry bushes
        if (damageAmount > 0 && source.getEntity() instanceof LivingEntity attacker && target instanceof LivingEntity living && !target.level.isClientSide) {
            // Life steal, x1.5 on a stunned target (design D3)
            attacker.heal(damageAmount * (living.hasEffect(ElementalStates.STUNNED.get()) ? 0.3f : 0.2f));

            long now = target.level.getGameTime();
            boolean offBalance = living.hasEffect(ElementalStates.OFF_BALANCE.get());

            if (offBalance || now - lastRooted.getOrDefault(target, -ROOT_COOLDOWN - 1L) > ROOT_COOLDOWN) {
                // Cold slows the roots down
                boolean cold = living.hasEffect(ElementalStates.CHILL.get()) || living.hasEffect(ElementalStates.FROZEN.get());
                living.addEffect(new MobEffectInstance(ElementalStates.ROOTED.get(), cold ? 10 : 20));
                lastRooted.put(target, now);
            }
        }

        super.applyStatusEffect(source, target, damageAmount);
    }

    @Override
    public void effectBlock(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }

        if (level.getBlockState(pos).is(Blocks.DIRT)) {
            level.setBlockAndUpdate(pos, Blocks.GRASS_BLOCK.defaultBlockState());
        }

        for (BlockPos at : new BlockPos[]{pos, pos.above()}) {
            BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, at);
        }
    }
}
