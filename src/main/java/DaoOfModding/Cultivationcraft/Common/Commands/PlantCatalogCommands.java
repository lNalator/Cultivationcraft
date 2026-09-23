package DaoOfModding.Cultivationcraft.Common.Commands;

import DaoOfModding.Cultivationcraft.Common.Config;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantCatalogSavedData;
import DaoOfModding.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import DaoOfModding.Cultivationcraft.Common.Items.ItemRegister;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSource;
import DaoOfModding.Cultivationcraft.Common.Qi.Elements.Elements;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSourceConfig;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;

import java.util.List;
import java.util.Locale;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlantCatalogCommands {
    private static final SuggestionProvider<CommandSourceStack> ELEMENTS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        Elements.getElements().stream().sorted().forEach(element -> {
            // IDs use cultivationcraft.elements.fire etc.; allow searching by "fire" too.
            if (element.toString().contains(remaining))
                builder.suggest(element.toString(), Component.translatable(element.getPath()));
        });
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> PLANTS = (ctx, builder) -> {
        var data = PlantCatalogSavedData.getOrCreate(ctx.getSource().getLevel(), Config.Server.procPlantCatalogSize());
        String remaining = builder.getRemainingLowerCase();
        for (var entry : data.entries()) {
            String id = Integer.toString(entry.id);
            if (id.startsWith(remaining) || entry.displayName.toLowerCase(Locale.ROOT).contains(remaining)) {
                builder.suggest(id, Component.literal(entry.displayName + " | T" + entry.genome.tier() + " | ")
                        .append(Component.translatable(entry.genome.qiElement().getPath())));
            }
        }
        return builder.buildFuture();
    };

    private static RequiredArgumentBuilder<CommandSourceStack, ResourceLocation> elementArgument() {
        return Commands.argument("element", ResourceLocationArgument.id()).suggests(ELEMENTS);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> tierArgument() {
        return integerSuggestions("tier", 1, 3, "Plant catalog tier", 1, 2, 3);
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> integerSuggestions(
            String name, int min, int max, String hint, int... values) {
        return Commands.argument(name, IntegerArgumentType.integer(min, max)).suggests((ctx, builder) -> {
            for (int value : values) {
                String text = Integer.toString(value);
                if (text.startsWith(builder.getRemaining())) builder.suggest(text, Component.literal(hint));
            }
            return builder.buildFuture();
        });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Boolean> hostArgument() {
        return Commands.argument("host", BoolArgumentType.bool()).suggests((ctx, builder) -> {
            if ("false".startsWith(builder.getRemainingLowerCase()))
                builder.suggest("false", Component.literal("Without stored Qi-source data (default)"));
            if ("true".startsWith(builder.getRemainingLowerCase()))
                builder.suggest("true", Component.literal("Include Qi-source data; only tier 3 plants can host a source"));
            return builder.buildFuture();
        });
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Double> storageArgument() {
        return Commands.argument("storage_fraction", DoubleArgumentType.doubleArg(0, 1)).suggests((ctx, builder) -> {
            for (String value : List.of("0", "0.25", "0.5", "0.75", "1")) {
                if (value.startsWith(builder.getRemaining())) {
                    int capacity = (int) (Double.parseDouble(value) * (QiSourceConfig.MaxStorage - QiSourceConfig.MinStorage))
                            + QiSourceConfig.MinStorage;
                    builder.suggest(value, Component.literal("Maximum capacity: " + capacity + " Qi"));
                }
            }
            return builder.buildFuture();
        });
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cultivation").requires(src -> src.hasPermission(2))
        .then(Commands.literal("help").executes(ctx -> showHelp(ctx.getSource(), "all")))
        .then(Commands.literal("plantcatalog")
            .executes(ctx -> listAll(ctx.getSource(), null, 0))
            .then(Commands.literal("help").executes(ctx -> showHelp(ctx.getSource(), "plantcatalog")))
            .then(Commands.literal("element")
                .then(elementArgument()
                    .executes(ctx -> listAll(ctx.getSource(), ResourceLocationArgument.getId(ctx, "element"), 0))))
            .then(Commands.literal("tier")
                .then(tierArgument()
                    .executes(ctx -> listAll(ctx.getSource(), null, IntegerArgumentType.getInteger(ctx, "tier")))))
            .then(Commands.literal("filter")
                .then(elementArgument()
                    .then(tierArgument()
                        .executes(ctx -> listAll(ctx.getSource(), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "tier"))))))
        )
        .then(Commands.literal("giveplant").executes(ctx -> showHelp(ctx.getSource(), "giveplant"))
            .then(Commands.literal("help").executes(ctx -> showHelp(ctx.getSource(), "giveplant")))
            .then(Commands.literal("id").executes(ctx -> showHelp(ctx.getSource(), "giveplant"))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("id", IntegerArgumentType.integer(0)).suggests(PLANTS)
                        .executes(ctx -> giveById(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "id"), false, 1))
                        .then(hostArgument()
                            .executes(ctx -> giveById(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "id"), BoolArgumentType.getBool(ctx, "host"), 1))
                            .then(integerSuggestions("count", 1, 64, "Number of plants (1-64)", 1, 8, 16, 64)
                                .executes(ctx -> giveById(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "id"), BoolArgumentType.getBool(ctx, "host"), IntegerArgumentType.getInteger(ctx, "count"))))))))
            .then(Commands.literal("filter")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(elementArgument()
                        .executes(ctx -> giveFiltered(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "element"), 0, false, 1))
                        .then(tierArgument()
                            .executes(ctx -> giveFiltered(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "tier"), false, 1))
                            .then(hostArgument()
                                .executes(ctx -> giveFiltered(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "tier"), BoolArgumentType.getBool(ctx, "host"), 1))
                                .then(integerSuggestions("count", 1, 64, "Number of plants (1-64)", 1, 8, 16, 64)
                                    .executes(ctx -> giveFiltered(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "tier"), BoolArgumentType.getBool(ctx, "host"), IntegerArgumentType.getInteger(ctx, "count"))))))))))
        .then(Commands.literal("qisource").executes(ctx -> showHelp(ctx.getSource(), "qisource"))
            .then(Commands.literal("help").executes(ctx -> showHelp(ctx.getSource(), "qisource")))
            .then(Commands.literal("here").executes(ctx -> showHelp(ctx.getSource(), "qisource"))
                .then(elementArgument()
                    .executes(ctx -> addQiHere(ctx.getSource(), ResourceLocationArgument.getId(ctx, "element"), null, null, null))
                    .then(integerSuggestions("size", 1, Integer.MAX_VALUE, "Source radius in blocks", 16, 32, 64)
                        .executes(ctx -> addQiHere(ctx.getSource(), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), null, null))
                        .then(storageArgument()
                            .executes(ctx -> addQiHere(ctx.getSource(), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), DoubleArgumentType.getDouble(ctx, "storage_fraction"), null))
                            .then(integerSuggestions("regen_ticks", 1, Integer.MAX_VALUE, "Ticks for a full refill (20 ticks = 1 second)", 1200, 6000, 72000)
                                .executes(ctx -> addQiHere(ctx.getSource(), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), DoubleArgumentType.getDouble(ctx, "storage_fraction"), IntegerArgumentType.getInteger(ctx, "regen_ticks"))))))))
            .then(Commands.literal("at").executes(ctx -> showHelp(ctx.getSource(), "qisource"))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(elementArgument()
                        .executes(ctx -> addQiAt(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), ResourceLocationArgument.getId(ctx, "element"), null, null, null))
                        .then(integerSuggestions("size", 1, Integer.MAX_VALUE, "Source radius in blocks", 16, 32, 64)
                            .executes(ctx -> addQiAt(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), null, null))
                            .then(storageArgument()
                                .executes(ctx -> addQiAt(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), DoubleArgumentType.getDouble(ctx, "storage_fraction"), null))
                                .then(integerSuggestions("regen_ticks", 1, Integer.MAX_VALUE, "Ticks for a full refill (20 ticks = 1 second)", 1200, 6000, 72000)
                                    .executes(ctx -> addQiAt(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), DoubleArgumentType.getDouble(ctx, "storage_fraction"), IntegerArgumentType.getInteger(ctx, "regen_ticks"))))))))));
        var registered = event.getDispatcher().register(root);
        // Preserve existing scripts while making the canonical spelling consistent.
        event.getDispatcher().register(Commands.literal("cultivation").requires(src -> src.hasPermission(2))
                .then(Commands.literal("plantCatalog")
                        .executes(ctx -> listAll(ctx.getSource(), null, 0))
                        .redirect(registered.getChild("plantcatalog"))));
    }

    private static int showHelp(CommandSourceStack src, String section) {
        src.sendSuccess(Component.literal("Testing commands: <required>, [optional trailing arguments]. Press Tab for values; hover suggestions for details."), false);
        if (section.equals("all") || section.equals("plantcatalog")) {
            usage(src, "/cultivation plantcatalog", "List plant IDs, generated names, elements and catalog tiers. Click a row to prepare giveplant.");
            usage(src, "/cultivation plantcatalog element <element>", "Filter by element.");
            usage(src, "/cultivation plantcatalog tier <tier>", "Filter by tier (1-3).");
            usage(src, "/cultivation plantcatalog filter <element> <tier>", "Combine both filters.");
        }
        if (section.equals("all") || section.equals("giveplant")) {
            usage(src, "/cultivation giveplant id <player> <id> [host] [count]", "Give a specific catalog variant. Tab on id shows names; host defaults to false, count to 1.");
            usage(src, "/cultivation giveplant filter <player> <element> [tier] [host] [count]", "Give a random matching variant. Omit tier to allow any catalog tier; count is 1-64.");
        }
        if (section.equals("all") || section.equals("qisource")) {
            usage(src, "/cultivation qisource here <element> [size] [storage_fraction] [regen_ticks]", "Create at your position. Omitted numbers are randomized.");
            usage(src, "/cultivation qisource at <x> <y> <z> <element> [size] [storage_fraction] [regen_ticks]", "Create at a loaded position; ~ coordinates work.");
            src.sendSuccess(Component.literal("size = radius in blocks; storage_fraction = capacity scale 0-1; regen_ticks = ticks for a full refill (20 ticks/second)."), false);
        }
        return 1;
    }

    private static void usage(CommandSourceStack src, String syntax, String description) {
        src.sendSuccess(Component.literal(syntax + " - " + description), false);
    }

    private static boolean validElement(CommandSourceStack src, ResourceLocation element) {
        if (Elements.getElements().contains(element)) return true;
        src.sendFailure(Component.literal("Unknown element: " + element + ". Press Tab on the element argument to select a registered ID."));
        return false;
    }

    private static int listAll(CommandSourceStack src, ResourceLocation elementFilter, int tierFilter) {
        if (elementFilter != null && !validElement(src, elementFilter)) return 0;
        ServerLevel level = src.getLevel();
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, Config.Server.procPlantCatalogSize());
        int shown = 0;
        for (var e : data.entries()) {
            if (elementFilter != null && !e.genome.qiElement().equals(elementFilter)) continue;
            if (tierFilter != 0 && e.genome.tier() != tierFilter) continue;
            String hex = String.format("%06X", e.genome.colorRGB());
            src.sendSuccess(Component.literal("[" + e.id + "] T" + e.genome.tier() + " " + e.displayName + " elem=" + e.genome.qiElement() + " color=#" + hex)
                    .withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                                    "/cultivation giveplant id @s " + e.id + " false 1"))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.literal("Click to prepare giveplant for this variant")))), false);
            shown++;
        }
        src.sendSuccess(Component.literal(shown + " matching plants. Click a row to prepare giveplant; /cultivation plantcatalog help explains filters."), false);
        return shown;
    }

    private static int giveById(CommandSourceStack src, ServerPlayer player, int id, boolean host, int count) {
        ServerLevel level = src.getLevel();
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, Config.Server.procPlantCatalogSize());
        PlantCatalogSavedData.Entry entry = data.getById(id);
        if (entry == null) {
            src.sendFailure(Component.literal("Unknown plant id: " + id));
            return 0;
        }
        ItemStack stack = makePlantStack(entry, host, count);
        boolean added = player.getInventory().add(stack);
        if (!added) player.drop(stack, false);
        src.sendSuccess(Component.literal("Gave " + count + "x [" + id + "] " + entry.displayName + (host ? " (host)" : "")), true);
        return 1;
    }

    private static int giveFiltered(CommandSourceStack src, ServerPlayer player, ResourceLocation element, int tier, boolean host, int count) {
        if (!validElement(src, element)) return 0;
        ServerLevel level = src.getLevel();
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, Config.Server.procPlantCatalogSize());
        List<PlantCatalogSavedData.Entry> list = new java.util.ArrayList<>();
        for (var e : data.entries()) {
            if (!e.genome.qiElement().equals(element)) continue;
            if (tier != 0 && e.genome.tier() != tier) continue;
            list.add(e);
        }
        if (list.isEmpty()) {
            src.sendFailure(Component.literal("No matching plant for element=" + element + (tier==0?"":" tier="+tier)));
            return 0;
        }
        PlantCatalogSavedData.Entry entry = list.get((int)(Math.random() * list.size()));
        ItemStack stack = makePlantStack(entry, host, count);
        boolean added = player.getInventory().add(stack);
        if (!added) player.drop(stack, false);
        src.sendSuccess(Component.literal("Gave " + count + "x [" + entry.id + "] " + entry.displayName + (host ? " (host)" : "")), true);
        return 1;
    }

    private static ItemStack makePlantStack(PlantCatalogSavedData.Entry entry, boolean host, int count) {
        ItemStack stack = new ItemStack(ItemRegister.PROCEDURAL_PLANT_ITEM.get(), count);
        var bst = stack.getOrCreateTagElement("BlockStateTag");
        bst.putString("species", Integer.toString(entry.id));
        if (host) bst.putString("host_qi", "true");
        if (host) {
            QiSource src = new QiSource(BlockPos.ZERO, QiSourceConfig.generateRandomSize(), entry.genome.qiElement(), QiSourceConfig.generateRandomQiStorage(), QiSourceConfig.generateRandomQiRegen());
            stack.getOrCreateTag().put("QiHostData", src.SerializeNBT());
        }
        return stack;
    }

    private static int addQiHere(CommandSourceStack src, ResourceLocation element, Integer size, Double storage, Integer regen) {
        ServerPlayer player;
        try { player = src.getPlayerOrException(); } catch (Exception e) { src.sendFailure(Component.literal("No player context")); return 0; }
        return addQiAt(src, player.blockPosition(), element, size, storage, regen);
    }

    private static int addQiAt(CommandSourceStack src, BlockPos pos, ResourceLocation element, Integer size, Double storage, Integer regen) {
        if (!validElement(src, element)) return 0;
        ServerLevel level = src.getLevel();
        int s = size != null ? size : QiSourceConfig.generateRandomSize();
        double st = storage != null ? storage : QiSourceConfig.generateRandomQiStorage();
        int rg = regen != null ? regen : QiSourceConfig.generateRandomQiRegen();
        QiSource source = new QiSource(pos, s, element, st, rg);
        var chunkCap = ChunkQiSources.getChunkQiSources(level.getChunkAt(pos));
        chunkCap.getQiSources().add(source);
        PacketHandler.sendChunkQiSourcesToClient(level.getChunkAt(pos));
        src.sendSuccess(Component.literal("Added QiSource at " + pos.getX()+","+pos.getY()+","+pos.getZ()+" elem="+element+" size="+s+" capacity="+source.getQiMax()+" regen_ticks="+rg), true);
        return 1;
    }
}
