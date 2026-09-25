package DaoOfModding.Cultivationcraft.Common.Commands;

import DaoOfModding.Cultivationcraft.Common.Knowledge.KnowledgeEntry;
import DaoOfModding.Cultivationcraft.Common.Knowledge.PlayerKnowledge;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import net.minecraft.commands.Commands;
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
                .then(Commands.literal("givejadeslip")
                        .executes(context -> {
                            context.getSource().sendSuccess(Component.literal(
                                    "/cultivation givejadeslip <player> <entry> - Tab searches entry IDs, recipe names and titles. Recipes use recipes/<pill_id>."), false);
                            return 1;
                        }).then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("entry", ResourceLocationArgument.id())
                                .suggests((context, builder) -> {
                                    String search = builder.getRemainingLowerCase();
                                    for (KnowledgeEntry entry : KnowledgeEntry.all()) {
                                        if (!entry.hasSlip() || !entry.available()) continue;
                                        String title = entry.resolveTitle(entry.title(), context.getSource().getLevel());
                                        String searchable = entry.id() + " " + entry.recipe() + " " + title + " "
                                                + String.join(" ", entry.titles()) + " " + String.join(" ", entry.aliases());
                                        if (searchable.toLowerCase(java.util.Locale.ROOT).contains(search))
                                            builder.suggest(entry.id().toString(), Component.literal(title));
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    var player = EntityArgument.getPlayer(context, "player");
                                    ResourceLocation id = ResourceLocationArgument.getId(context, "entry");
                                    KnowledgeEntry entry = KnowledgeEntry.resolveSlip(id);
                                    if (entry == null || !entry.hasSlip()) {
                                        context.getSource().sendFailure(Component.translatable("cultivationcraft.jade.invalid"));
                                        return 0;
                                    }
                                    if (!entry.available()) {
                                        context.getSource().sendFailure(Component.translatable("cultivationcraft.jade.missing_recipe", entry.id(), entry.recipe()));
                                        return 0;
                                    }
                                    var stack = PlayerKnowledge.create(player.getLevel(), entry, player.getRandom());
                                    if (!player.getInventory().add(stack)) player.drop(stack, false);
                                    return 1;
                                })))));
    }
}
