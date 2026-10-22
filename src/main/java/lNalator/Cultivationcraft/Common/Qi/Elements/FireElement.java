package lNalator.Cultivationcraft.Common.Qi.Elements;

import lNalator.Cultivationcraft.Common.Blocks.BlockRegister;
import lNalator.Cultivationcraft.Common.Blocks.entity.FrozenBlockEntity;
import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Network.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Material;

import java.awt.*;

public class FireElement extends Element {

    public FireElement(ResourceLocation resourcelocation, Color elementColor, double newDensity) {
        super(resourcelocation, elementColor, newDensity);

        effectTickChance = 1.0;
    }

    public void applyStatusEffect(QiDamageSource source, Entity target, float damageAmount) {
        // 2 s + damage/4, capped at 10 s (design D1)
        target.setSecondsOnFire((int) Math.min(10, 2 + damageAmount / 4));

        super.applyStatusEffect(source, target, damageAmount);
    }

    @Override
    public void effectBlock(Level level, BlockPos pos) {
        // Melt ice, snow and frozen blocks first
        BlockState state = level.getBlockState(pos);

        if (!level.isClientSide && state.is(BlockRegister.FROZEN_BLOCK.get()) && level.getBlockEntity(pos) instanceof FrozenBlockEntity frozen) {
            frozen.thawBlock();
            return;
        }

        if (!level.isClientSide && (state.is(Blocks.ICE) || state.is(Blocks.FROSTED_ICE))) {
            level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
            return;
        }

        if (!level.isClientSide && (state.getMaterial() == Material.TOP_SNOW || state.getMaterial() == Material.SNOW || state.is(Blocks.POWDER_SNOW))) {
            level.removeBlock(pos, false);
            return;
        }

        pos = pos.above();

        if (BaseFireBlock.canBePlacedAt(level, pos, Direction.DOWN)) {
            level.setBlockAndUpdate(pos, BaseFireBlock.getState(level, pos));
            level.gameEvent(null, GameEvent.BLOCK_PLACE, pos);

            if (level.isClientSide) {
                ClientPacketHandler.sendElementalEffectToServer(name, level.dimension(), pos);
            }
        }
    }
}
