package DaoOfModding.Cultivationcraft.Server;

import DaoOfModding.Cultivationcraft.Common.Refinement.RefinementHandlers;
import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import DaoOfModding.Cultivationcraft.Common.Capabilities.RefinementInventory.RefinementInventory;
import DaoOfModding.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import DaoOfModding.Cultivationcraft.Common.Qi.Elements.Elements;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import DaoOfModding.Cultivationcraft.Network.Packets.RefinementVisualPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

/** Synchronizes a visual copy only; the real item never leaves the refinement inventory. */
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID)
public final class RefinementVisualSync {
    private record State(ItemStack item, int color, ResourceLocation dimension) {}
    private static final Map<ServerPlayer, State> active = new WeakHashMap<>();

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        ItemStack stack = RefinementInventory.getCapability(player).getItemStackHandler().getStackInSlot(0);
        State previous = active.get(player);
        if (!player.isAlive() || player.isSpectator() || !RefinementHandlers.isActive(player, stack)) {
            if (active.remove(player) != null) send(player, ItemStack.EMPTY, 0xFFFFFF);
            return;
        }
        ItemStack visual = stack.copy();
        visual.setCount(1);
        // Progress changes every tick but does not change the item's appearance.
        if (visual.hasTag()) {
            visual.getTag().remove("BindRemaining");
            visual.getTag().remove("BindPercent");
            visual.getTag().remove(Cultivationcraft.MODID + "bindtime");
        }
        int color = RefinementHandlers.find(stack).flowsTowardPlayer() ? 0xFFFFFF : elementColor(player);
        ResourceLocation dimension = player.level.dimension().location();
        if (previous == null || previous.color != color || !previous.dimension.equals(dimension)
                || !ItemStack.matches(previous.item, visual) || player.tickCount % 20 == 0) {
            active.put(player, new State(visual, color, dimension));
            send(player, visual, color);
        }
    }

    public static int elementColor(ServerPlayer player) {
        var cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();
        ResourceLocation element = Elements.noElement;
        if (cultivation instanceof QiCondenserCultivation condenser) element = condenser.getCurrentElementFocus();
        // After Core Formation the chosen element is carried by cultivation modifiers.
        if (element.equals(Elements.noElement)) {
            for (ResourceLocation candidate : cultivation.getElements()) {
                if (!candidate.equals(Elements.noElement) && !candidate.equals(Elements.anyElement)) {
                    element = candidate;
                    break;
                }
            }
        }
        var definition = Elements.getElement(element);
        return definition == null ? 0xFFFFFF : definition.color.getRGB() & 0xFFFFFF;
    }

    private static void send(ServerPlayer player, ItemStack item, int color) {
        PacketHandler.channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new RefinementVisualPacket(player.getUUID(), player.level.dimension().location(), item, color));
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer) || !(event.getTarget() instanceof ServerPlayer target)) return;
        State state = active.get(target);
        if (state != null) PacketHandler.channel.send(PacketDistributor.PLAYER.with(() -> viewer),
                new RefinementVisualPacket(target.getUUID(), state.dimension, state.item, state.color));
    }
}
