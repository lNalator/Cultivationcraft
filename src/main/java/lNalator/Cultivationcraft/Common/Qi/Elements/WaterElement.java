package lNalator.Cultivationcraft.Common.Qi.Elements;

import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.awt.*;

public class WaterElement extends Element {

    public WaterElement(ResourceLocation resourcelocation, Color elementColor, double newDensity) {
        super(resourcelocation, elementColor, newDensity);

        effectTickChance = 1;
    }

    public void applyStatusEffect(QiDamageSource source, Entity target, float damageAmount) {
        if (target.isOnFire() && source.getEntity() instanceof Player player && !target.level.isClientSide) {
            QuestHandler.progressQuest(player, Quest.EXTINGUISH, 1);
        }

        target.clearFire();

        if (damageAmount > 0 && target instanceof LivingEntity living && !target.level.isClientSide) {
            living.addEffect(new MobEffectInstance(ElementalStates.WET.get(), 160));

            // Light push away from the source
            Vec3 from = source.getSourcePosition();
            if (from != null) {
                living.knockback(0.3, from.x - target.getX(), from.z - target.getZ());
            }
        }

        super.applyStatusEffect(source, target, damageAmount);
    }

    @Override
    public void effectBlock(Level level, BlockPos pos) {
        if (level.isClientSide) {
            return;
        }

        for (BlockPos at : new BlockPos[]{pos, pos.above()}) {
            if (level.getBlockState(at).getBlock() instanceof BaseFireBlock) {
                level.removeBlock(at, false);
                continue;
            }

            // Vanilla rules: lava source -> obsidian, flowing lava -> cobblestone
            FluidState fluid = level.getFluidState(at);
            if (fluid.is(Fluids.LAVA)) {
                level.setBlockAndUpdate(at, Blocks.OBSIDIAN.defaultBlockState());
            } else if (fluid.is(Fluids.FLOWING_LAVA)) {
                level.setBlockAndUpdate(at, Blocks.COBBLESTONE.defaultBlockState());
            }
        }
    }
}
