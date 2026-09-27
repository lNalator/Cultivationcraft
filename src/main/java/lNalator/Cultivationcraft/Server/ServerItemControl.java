package lNalator.Cultivationcraft.Server;

import lNalator.Cultivationcraft.Client.GUI.Screens.StatScreen;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Containers.RefinementMenuProvider;
import lNalator.Cultivationcraft.Network.PacketHandler;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.ICultivatorStats;
import lNalator.Cultivationcraft.Common.Register;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.server.ServerLifecycleHooks;

public class ServerItemControl {

    public static boolean loaded = false;

    public static void sendPlayerStats(Player player, Player target) {
        PacketHandler.sendCultivatorStatsToSpecificClient(target, (ServerPlayer) player);
        PacketHandler.sendBodyModificationsToSpecificClient(target, (ServerPlayer) player);
    }

    public static void handleKeyPress(Register.keyPresses keyPressed, ServerPlayer pressedBy) {
        if (keyPressed == Register.keyPresses.REFINEMENT_SCREEN) {
            MenuProvider refinementMenuProvider = new RefinementMenuProvider(pressedBy);
            NetworkHooks.openScreen(pressedBy, refinementMenuProvider);
        }

        if (keyPressed == Register.keyPresses.SKILLHOTBARSWITCH) {
            SkillHotbarServer.switchActive(pressedBy.getUUID());
        }
    }
}
