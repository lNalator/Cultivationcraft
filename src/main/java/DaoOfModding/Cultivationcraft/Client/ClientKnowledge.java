package DaoOfModding.Cultivationcraft.Client;

import DaoOfModding.Cultivationcraft.Client.GUI.Screens.SelectableText;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import DaoOfModding.Cultivationcraft.Network.Packets.KnowledgePagesPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, value = Dist.CLIENT)
public final class ClientKnowledge {
    private static List<KnowledgePagesPacket.Page> pages = List.of();
    private static int revision;
    public static int revision() { return revision; }
    public static boolean knows(String id) { return pages.stream().anyMatch(page -> page.id().equals(id)); }
    public static void update(List<KnowledgePagesPacket.Page> learned) { pages = List.copyOf(learned); revision++; }
    public static List<SelectableText> helpPages() {
        return pages.stream().map(page -> new SelectableText(page.title(), page.text())).toList();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { update(List.of()); }
}
