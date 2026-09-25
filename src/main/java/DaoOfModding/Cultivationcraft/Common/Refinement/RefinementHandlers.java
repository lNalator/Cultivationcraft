package DaoOfModding.Cultivationcraft.Common.Refinement;

import DaoOfModding.Cultivationcraft.Common.Advancements.CultivationAdvancements;
import DaoOfModding.Cultivationcraft.Common.Alchemy.PillDefinition;
import DaoOfModding.Cultivationcraft.Common.Alchemy.PillEffects;
import DaoOfModding.Cultivationcraft.Common.FlyingSwordBind;
import DaoOfModding.Cultivationcraft.Common.FlyingSwordController;
import DaoOfModding.Cultivationcraft.Common.Items.AlchemyPillItem;
import DaoOfModding.Cultivationcraft.Common.Items.JadeSlipItem;
import DaoOfModding.Cultivationcraft.Common.Knowledge.PlayerKnowledge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import java.util.ArrayList;
import java.util.List;

public final class RefinementHandlers {
    private static final List<RefinementHandler> handlers = new ArrayList<>();
    static {
        register(new SwordRefinement());
        register(new PillRefinement());
        register(new JadeSlipRefinement());
    }
    /** Register new item behavior during setup; the first matching handler wins. */
    public static void register(RefinementHandler handler) { handlers.add(java.util.Objects.requireNonNull(handler)); }
    public static RefinementHandler find(ItemStack stack) {
        if (stack.isEmpty()) return null;
        for (RefinementHandler handler : handlers) if (handler.accepts(stack)) return handler;
        return null;
    }
    public static boolean isActive(Player player, ItemStack stack) {
        RefinementHandler handler = find(stack);
        return handler != null && handler.isActive(player, stack);
    }
    public static void tick(ServerPlayer player, ItemStack stack, long elapsedNanos) {
        RefinementHandler selected = find(stack);
        for (RefinementHandler handler : handlers) if (handler != selected) handler.onDeselected(player);
        if (selected != null) selected.tick(player, stack, elapsedNanos);
    }
    private abstract static class FiveSecondRefinement implements RefinementHandler {
        @Override public float remainingSeconds(Player player, ItemStack stack) { return 5 * (1 - progress(player, stack)); }
    }
    private static final class PillRefinement extends FiveSecondRefinement {
        @Override public boolean accepts(ItemStack stack) { return stack.getItem() instanceof AlchemyPillItem; }
        @Override public boolean isActive(Player player, ItemStack stack) {
            return stack.hasTag() && !stack.getTag().getString("Entry").isEmpty()
                    && PillDefinition.get(stack.getTag().getString("Pill")) != null && !PillEffects.identified(player, stack);
        }
        @Override public float progress(Player player, ItemStack stack) { return PillEffects.analysisProgress(player, stack) / 1000f; }
        @Override public void tick(ServerPlayer player, ItemStack stack, long elapsedNanos) { PillEffects.refinePill(player, stack, elapsedNanos); }
    }
    private static final class JadeSlipRefinement extends FiveSecondRefinement {
        @Override public boolean flowsTowardPlayer() { return true; }
        @Override public boolean accepts(ItemStack stack) { return stack.getItem() instanceof JadeSlipItem; }
        @Override public boolean isActive(Player player, ItemStack stack) { return PlayerKnowledge.canRefine(player, stack); }
        @Override public float progress(Player player, ItemStack stack) { return PlayerKnowledge.progress(player, stack) / 1000f; }
        @Override public void tick(ServerPlayer player, ItemStack stack, long elapsedNanos) { PlayerKnowledge.refine(player, stack, elapsedNanos); }
        @Override public void onDeselected(ServerPlayer player) { PlayerKnowledge.refine(player, ItemStack.EMPTY, 0); }
    }
    private static final class SwordRefinement implements RefinementHandler {
        @Override public boolean accepts(ItemStack stack) { return stack.getItem() instanceof SwordItem; }
        @Override public boolean isActive(Player player, ItemStack stack) {
            return !(FlyingSwordBind.isBound(stack) && player.getUUID().equals(FlyingSwordBind.getOwner(stack)));
        }
        @Override public float progress(Player player, ItemStack stack) {
            // Negative progress represents removing another cultivator's binding.
            if (!isActive(player, stack)) return 1;
            return (float) Math.max(-1, Math.min(1, FlyingSwordBind.getBindTime(stack) / (double) Math.max(1, FlyingSwordBind.getBindTimeMax(stack))));
        }
        @Override public float remainingSeconds(Player player, ItemStack stack) {
            return isActive(player, stack) ? Math.max(0, (FlyingSwordBind.getBindTimeMax(stack) - FlyingSwordBind.getBindTime(stack)) / 1_000_000_000f) : 0;
        }
        @Override public void tick(ServerPlayer player, ItemStack stack, long elapsedNanos) {
            if (!FlyingSwordController.startFlyingSwordBind(stack, player.getUUID())) return;
            long time = FlyingSwordBind.getBindTime(stack) + Math.max(0, elapsedNanos);
            FlyingSwordBind.setBindTime(stack, time);
            if (FlyingSwordBind.isBound(stack) && time > 0) {
                FlyingSwordBind.setBound(stack, false);
                FlyingSwordBind.setOwner(stack, null);
                FlyingSwordController.removeFlyingItem(stack);
            }
            // Keep the legacy sword tags compatible with existing item data.
            stack.getOrCreateTag().putFloat("BindRemaining", remainingSeconds(player, stack));
            stack.getOrCreateTag().putFloat("BindPercent", progress(player, stack));
            if (time > FlyingSwordBind.getBindTimeMax(stack)) {
                FlyingSwordBind.setOwner(stack, player.getUUID());
                FlyingSwordBind.setBound(stack, true);
                FlyingSwordController.addFlyingItem(stack, player.getUUID());
                CultivationAdvancements.HAS_FLYING_SWORD.trigger(player, false);
            }
        }
    }
}
