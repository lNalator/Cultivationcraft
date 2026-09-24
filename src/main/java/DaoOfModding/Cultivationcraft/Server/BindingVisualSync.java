package DaoOfModding.Cultivationcraft.Server;

import DaoOfModding.Cultivationcraft.Common.Items.JadeSlipItem;
import DaoOfModding.Cultivationcraft.Common.Knowledge.PlayerKnowledge;
import DaoOfModding.Cultivationcraft.Common.Alchemy.PillDefinition;
import DaoOfModding.Cultivationcraft.Common.Alchemy.PillEffects;
import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import DaoOfModding.Cultivationcraft.Common.Capabilities.FlyingSwordContainerItemStack.FlyingSwordContainerItemStack;
import DaoOfModding.Cultivationcraft.Common.FlyingSwordBind;
import DaoOfModding.Cultivationcraft.Common.Items.AlchemyPillItem;
import DaoOfModding.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import DaoOfModding.Cultivationcraft.Common.Qi.Elements.Elements;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import DaoOfModding.Cultivationcraft.Network.Packets.BindingVisualPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

/** Synchronizes a visual copy only; the real item never leaves the binding inventory. */
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID)
public final class BindingVisualSync {
    private record State(ItemStack item, int color, ResourceLocation dimension) {}
    private static final Map<ServerPlayer, State> active = new WeakHashMap<>();

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        ItemStack stack = FlyingSwordContainerItemStack.getCapability(player).getItemStackHandler().getStackInSlot(0);
        State previous = active.get(player);
        if (!player.isAlive() || player.isSpectator() || !isWorking(player, stack)) {
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
        int color = stack.getItem() instanceof JadeSlipItem ? 0xFFFFFF : elementColor(player);
        ResourceLocation dimension = player.level.dimension().location();
        if (previous == null || previous.color != color || !previous.dimension.equals(dimension)
                || !ItemStack.matches(previous.item, visual) || player.tickCount % 20 == 0) {
            active.put(player, new State(visual, color, dimension));
            send(player, visual, color);
        }
    }

    private static boolean isWorking(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof JadeSlipItem)
            return PlayerKnowledge.canRefine(player, stack);
        if (stack.getItem() instanceof AlchemyPillItem)
            return stack.hasTag() && !stack.getTag().getString("Entry").isEmpty()
                    && PillDefinition.get(stack.getTag().getString("Pill")) != null && !PillEffects.identified(player, stack);
        return stack.getItem() instanceof SwordItem
                && !(FlyingSwordBind.isBound(stack) && player.getUUID().equals(FlyingSwordBind.getOwner(stack)));
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
                new BindingVisualPacket(player.getUUID(), player.level.dimension().location(), item, color));
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer) || !(event.getTarget() instanceof ServerPlayer target)) return;
        State state = active.get(target);
        if (state != null) PacketHandler.channel.send(PacketDistributor.PLAYER.with(() -> viewer),
                new BindingVisualPacket(target.getUUID(), state.dimension, state.item, state.color));
    }
}
