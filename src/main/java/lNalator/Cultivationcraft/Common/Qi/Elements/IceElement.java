package lNalator.Cultivationcraft.Common.Qi.Elements;

import lNalator.Cultivationcraft.Common.Blocks.BlockRegister;
import lNalator.Cultivationcraft.Common.Blocks.entity.FrozenBlockEntity;
import lNalator.Cultivationcraft.Common.Items.FreezeControl;
import lNalator.Cultivationcraft.Common.Qi.Stats.BodyPartStatControl;
import lNalator.Cultivationcraft.Common.Qi.Stats.StatIDs;
import net.minecraft.core.BlockPos;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.tags.TagKey;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.awt.*;

public class IceElement extends ElementVariant {

    public IceElement(ResourceLocation Element, Color elementColor, double newDensity, double chance) {
        super(Element, elementColor, newDensity, chance);

        effectTickChance = 1.0 / 20.0;
    }

    @Override
    public void applyStatusEffect(QiDamageSource source, Entity target, float damageAmount) {
        if (damageAmount > 0 && target instanceof LivingEntity living) {
            ElementalStates.addChill(living, source.getEntity());
        }

        super.applyStatusEffect(source, target, damageAmount);
    }

    public static final int BASE_FREEZE_TICKS = 100;
    public static final int MAX_FREEZE_TICKS = 600;
    public static final TagKey<Block> FREEZE_IMMUNE = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation(Cultivationcraft.MODID, "freeze_immune"));

    @Override
    public void effectBlock(Level level, BlockPos pos) {
        effectBlock(level, pos, 0);
    }

    // Freeze for 5 s + 0.5 s per damage point, max 30 s; hitting a frozen block refreshes it (design E1)
    @Override
    public void effectBlock(Level level, BlockPos pos, float damage) {
        if (level.isClientSide) {
            return;
        }

        int duration = (int) Math.min(MAX_FREEZE_TICKS, BASE_FREEZE_TICKS + damage * 10);
        BlockState block = level.getBlockState(pos);

        if (block.is(BlockRegister.FROZEN_BLOCK.get())) {
            if (level.getBlockEntity(pos) instanceof FrozenBlockEntity frozen) {
                frozen.refresh(duration);
            }
            return;
        }

        // Every block freezes except air, ice, flowing fluids, unbreakable blocks and the freeze_immune tag (design E3).
        // Water and lava sources freeze temporarily too (design D7/E5).
        FluidState fluid = level.getFluidState(pos);
        boolean liquid = block.getBlock() instanceof LiquidBlock;

        if (block.isAir() || block.is(Blocks.ICE) || (liquid && !fluid.isSource()) || block.getDestroySpeed(level, pos) < 0 || block.is(FREEZE_IMMUNE)) {
            return;
        }

        FreezeControl.Freeze(level, pos, duration);
    }

    public float resistanceModifier(Player player) {
        return (int) (BodyPartStatControl.getStats(player).getElementalStat(StatIDs.resistanceModifier, Elements.waterElement) * -0.5f);
    }
}
