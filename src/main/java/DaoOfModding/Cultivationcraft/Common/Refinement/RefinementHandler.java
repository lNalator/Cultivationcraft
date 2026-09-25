package DaoOfModding.Cultivationcraft.Common.Refinement;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** One item's refinement rules, shared by the slot, progress display and animation. */
public interface RefinementHandler {
    boolean accepts(ItemStack stack);
    boolean isActive(Player player, ItemStack stack);
    float progress(Player player, ItemStack stack);
    float remainingSeconds(Player player, ItemStack stack);
    void tick(ServerPlayer player, ItemStack stack, long elapsedNanos);
    default void onDeselected(ServerPlayer player) {}
    /** Knowledge flows into the head; ordinary refinement sends elemental Qi outward. */
    default boolean flowsTowardPlayer() { return false; }
}
