package DaoOfModding.Cultivationcraft.Common.Commands;

import DaoOfModding.Cultivationcraft.Common.Knowledge.KnowledgeEntry;
import DaoOfModding.Cultivationcraft.Common.Knowledge.PlayerKnowledge;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID)
public final class JadeSlipCommands {
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("cultivation").requires(source -> source.hasPermission(2))
                .then(Commands.literal("givejadeslip").then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("entry", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        KnowledgeEntry.all().stream().filter(KnowledgeEntry::available).map(KnowledgeEntry::id), builder))
                                .executes(context -> {
                                    var player = EntityArgument.getPlayer(context, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(context, "entry");
                                    KnowledgeEntry entry = KnowledgeEntry.get(id.toString());
                                    if (entry == null && id.getNamespace().equals("minecraft"))
                                        entry = KnowledgeEntry.get(Cultivationcraft.MODID + ":" + id.getPath());
                                    if (entry == null || !entry.available()) {
                                        context.getSource().sendFailure(Component.translatable("cultivationcraft.jade.invalid"));
                                        return 0;
                                    }
                                    var stack = PlayerKnowledge.create(player.getLevel(), entry, player.getRandom());
                                    if (!player.getInventory().add(stack)) player.drop(stack, false);
                                    return 1;
                                })))));
    }
}
