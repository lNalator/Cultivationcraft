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
    public static boolean knows(String id) { return pages.stream().anyMatch(page -> (page.id().equals(id) || page.aliases().contains(id))); }
    public static void update(List<KnowledgePagesPacket.Page> learned) { pages = List.copyOf(learned); revision++; }
    public static List<SelectableText> helpPages() {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return List.of();
        int cultivation = DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats
                .getCultivatorStats(player).getCultivationType();
        Node root = new Node("");
        for (var page : pages) {
            if (page.operatorOnly() && !player.hasPermissions(2)) continue;
            if (page.cultivationType() >= 0 && page.cultivationType() != cultivation) continue;
            Node node = root;
            for (String part : page.category()) node = node.child(part);
            node = node.child(page.title());
            node.page = page;
        }
        return root.children.values().stream().map(Node::render).toList();
    }

    /** Merge category headers with their own articles, regardless of registry iteration order. */
    private static final class Node {
        private final String title;
        private final java.util.Map<String, Node> children = new java.util.LinkedHashMap<>();
        private KnowledgePagesPacket.Page page;
        private Node(String title) { this.title = title; }
        private Node child(String title) { return children.computeIfAbsent(title, Node::new); }
        private SelectableText render() {
            SelectableText result;
            if (page != null && !page.translationKey().isEmpty())
                result = new SelectableText(net.minecraft.network.chat.Component.translatable(page.translationKey()),
                        net.minecraft.network.chat.Component.translatable(page.translationKey() + ".text"));
            else result = new SelectableText(title, page == null ? "Select an unlocked entry below." : page.text());
            children.values().forEach(child -> result.addItem(child.render()));
            return result;
        }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { update(List.of()); }
}
