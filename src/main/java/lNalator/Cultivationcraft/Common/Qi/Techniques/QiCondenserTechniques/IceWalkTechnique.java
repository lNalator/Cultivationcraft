package lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques;

import lNalator.Cultivationcraft.Common.Blocks.BlockRegister;
import lNalator.Cultivationcraft.Common.Blocks.entity.FrozenBlockEntity;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Items.FreezeControl;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultTechniqueStatIDs;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.TechniqueStatModification;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;

// Walk on water and lava by freezing them under your feet, in a radius that grows with training (design E6, option A).
// Sneak to stop freezing and sink.
public class IceWalkTechnique extends Technique {

    protected static final int FREEZE_TICKS = 60;

    public IceWalkTechnique() {
        super();

        langLocation = "cultivationcraft.technique.icewalk";
        Element = Elements.iceElement;

        type = useType.Toggle;
        multiple = false;

        icon = new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/icons/icewalk.png");

        canLevel = true;

        addMinTechniqueStat(DefaultTechniqueStatIDs.qiCost, 0.1);

        TechniqueStatModification qiCostModification = new TechniqueStatModification(DefaultTechniqueStatIDs.qiCost);
        TechniqueStatModification rangeModification = new TechniqueStatModification(DefaultTechniqueStatIDs.range);

        qiCostModification.addStatChange(DefaultTechniqueStatIDs.qiCost, -0.0005);
        rangeModification.addStatChange(DefaultTechniqueStatIDs.range, 0.002);
        rangeModification.addStatChange(DefaultTechniqueStatIDs.qiCost, 0.0005);

        addTechniqueStat(DefaultTechniqueStatIDs.qiCost, 1, qiCostModification);
        addTechniqueStat(DefaultTechniqueStatIDs.range, 1, rangeModification);
    }

    // Only for cultivators whose main absorbed element (over Qi Condensation) is Ice, in Qi Condensation or later
    @Override
    public boolean isValid(Player player) {
        if (CultivatorStats.getCultivatorStats(player).getCultivationType() != CultivationTypes.QI_CONDENSER) {
            return false;
        }

        CultivationType cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();

        while (cultivation != null && !(cultivation instanceof QiCondenserCultivation)) {
            cultivation = cultivation.getPreviousCultivation();
        }

        return cultivation instanceof QiCondenserCultivation qiCondenser && Elements.iceElement.equals(qiCondenser.getCurrentElementFocus());
    }

    @Override
    public void tickServer(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        if (!spendExternalQi(player, 0.05)) {
            deactivate(player);
            return;
        }

        if (!player.isShiftKeyDown()) {
            int range = (int) getTechniqueStat(DefaultTechniqueStatIDs.range, player);
            BlockPos below = player.blockPosition().below();
            int frozen = 0;

            for (BlockPos pos : BlockPos.betweenClosed(below.offset(-range, 0, -range), below.offset(range, 0, range))) {
                if (pos.distSqr(below) <= range * range && freezeLiquid(player.level, pos.immutable())) {
                    frozen++;
                }
            }

            if (frozen > 0) {
                levelUp(player, frozen * 0.05);
            }
        }

        super.tickServer(event);
    }

    // Freeze a water or lava source, or keep an already frozen one frozen
    protected static boolean freezeLiquid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.is(BlockRegister.FROZEN_BLOCK.get())) {
            if (level.getBlockEntity(pos) instanceof FrozenBlockEntity frozen && frozen.getOldBlockState().getBlock() instanceof LiquidBlock) {
                frozen.refresh(FREEZE_TICKS);
            }
            return false;
        }

        if (state.getBlock() instanceof LiquidBlock && level.getFluidState(pos).isSource()) {
            FreezeControl.Freeze(level, pos, FREEZE_TICKS);
            return true;
        }

        return false;
    }
}
