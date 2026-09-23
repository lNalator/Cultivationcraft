package DaoOfModding.Cultivationcraft.Common.Commands;

import DaoOfModding.Cultivationcraft.Common.Config;
import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantCatalogSavedData;
import DaoOfModding.Cultivationcraft.Common.Capabilities.ChunkQiSources.ChunkQiSources;
import DaoOfModding.Cultivationcraft.Common.Items.ItemRegister;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSource;
import DaoOfModding.Cultivationcraft.Common.Qi.QiSourceConfig;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;

import java.util.List;

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
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cultivation").requires(src -> src.hasPermission(2))
        .then(Commands.literal("plantCatalog")
            .executes(ctx -> listAll(ctx.getSource(), null, 0))
            .then(Commands.literal("element")
                .then(Commands.argument("id", ResourceLocationArgument.id())
                    .executes(ctx -> listAll(ctx.getSource(), ResourceLocationArgument.getId(ctx, "id"), 0))))
            .then(Commands.literal("tier")
                .then(Commands.argument("value", IntegerArgumentType.integer(1,3))
                    .executes(ctx -> listAll(ctx.getSource(), null, IntegerArgumentType.getInteger(ctx, "value")))))
            .then(Commands.literal("filter")
                .then(Commands.argument("id", ResourceLocationArgument.id())
                    .then(Commands.argument("tier", IntegerArgumentType.integer(1,3))
                        .executes(ctx -> listAll(ctx.getSource(), ResourceLocationArgument.getId(ctx, "id"), IntegerArgumentType.getInteger(ctx, "tier"))))))
        )
        .then(Commands.literal("giveplant")
            .then(Commands.literal("id")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("id", IntegerArgumentType.integer(0))
                        .executes(ctx -> giveById(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "id"), false, 1))
                        .then(Commands.argument("host", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                            .executes(ctx -> giveById(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "id"), com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "host"), 1))
                            .then(Commands.argument("count", IntegerArgumentType.integer(1,64))
                                .executes(ctx -> giveById(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), IntegerArgumentType.getInteger(ctx, "id"), com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "host"), IntegerArgumentType.getInteger(ctx, "count"))))))))
            .then(Commands.literal("filter")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("element", ResourceLocationArgument.id())
                        .executes(ctx -> giveFiltered(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "element"), 0, false, 1))
                        .then(Commands.argument("tier", IntegerArgumentType.integer(1,3))
                            .executes(ctx -> giveFiltered(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "tier"), false, 1))
                            .then(Commands.argument("host", com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                .executes(ctx -> giveFiltered(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "tier"), com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "host"), 1))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1,64))
                                    .executes(ctx -> giveFiltered(ctx.getSource(), EntityArgument.getPlayer(ctx, "player"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "tier"), com.mojang.brigadier.arguments.BoolArgumentType.getBool(ctx, "host"), IntegerArgumentType.getInteger(ctx, "count"))))))))))
        .then(Commands.literal("qisource")
            .then(Commands.literal("here")
                .then(Commands.argument("element", ResourceLocationArgument.id())
                    .executes(ctx -> addQiHere(ctx.getSource(), ResourceLocationArgument.getId(ctx, "element"), null, null, null))
                    .then(Commands.argument("size", IntegerArgumentType.integer(1))
                        .executes(ctx -> addQiHere(ctx.getSource(), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), null, null))
                        .then(Commands.argument("storage", IntegerArgumentType.integer(1))
                            .executes(ctx -> addQiHere(ctx.getSource(), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), IntegerArgumentType.getInteger(ctx, "storage"), null))
                            .then(Commands.argument("regen", IntegerArgumentType.integer(1))
                                .executes(ctx -> addQiHere(ctx.getSource(), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), IntegerArgumentType.getInteger(ctx, "storage"), IntegerArgumentType.getInteger(ctx, "regen")))))))))
            .then(Commands.literal("at")
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .then(Commands.argument("element", ResourceLocationArgument.id())
                        .executes(ctx -> addQiAt(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), ResourceLocationArgument.getId(ctx, "element"), null, null, null))
                        .then(Commands.argument("size", IntegerArgumentType.integer(1))
                            .executes(ctx -> addQiAt(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), null, null))
                            .then(Commands.argument("storage", IntegerArgumentType.integer(1))
                                .executes(ctx -> addQiAt(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), IntegerArgumentType.getInteger(ctx, "storage"), null))
                                .then(Commands.argument("regen", IntegerArgumentType.integer(1))
                                    .executes(ctx -> addQiAt(ctx.getSource(), BlockPosArgument.getLoadedBlockPos(ctx, "pos"), ResourceLocationArgument.getId(ctx, "element"), IntegerArgumentType.getInteger(ctx, "size"), IntegerArgumentType.getInteger(ctx, "storage"), IntegerArgumentType.getInteger(ctx, "regen")))))))));
        event.getDispatcher().register(root);
    }

    private static int listAll(CommandSourceStack src, ResourceLocation elementFilter, int tierFilter) {
        ServerLevel level = src.getLevel();
        PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, Config.Server.procPlantCatalogSize());
        int shown = 0;
        for (var e : data.entries()) {
            if (elementFilter != null && !e.genome.qiElement().equals(elementFilter)) continue;
            if (tierFilter != 0 && e.genome.tier() != tierFilter) continue;
            String hex = String.format("%06X", e.genome.colorRGB());
            src.sendSuccess(Component.literal("[" + e.id + "] T" + e.genome.tier() + " " + e.displayName + " elem=" + e.genome.qiElement() + " color=#" + hex), false);
            shown++;
        }
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

    private static int addQiHere(CommandSourceStack src, ResourceLocation element, Integer size, Integer storage, Integer regen) {
        ServerPlayer player;
        try { player = src.getPlayerOrException(); } catch (Exception e) { src.sendFailure(Component.literal("No player context")); return 0; }
        return addQiAt(src, player.blockPosition(), element, size, storage, regen);
    }

    private static int addQiAt(CommandSourceStack src, BlockPos pos, ResourceLocation element, Integer size, Integer storage, Integer regen) {
        ServerLevel level = src.getLevel();
        int s = size != null ? size : QiSourceConfig.generateRandomSize();
        double st = storage != null ? storage : QiSourceConfig.generateRandomQiStorage();
        int rg = regen != null ? regen : QiSourceConfig.generateRandomQiRegen();
        QiSource source = new QiSource(pos, s, element, st, rg);
        var chunkCap = ChunkQiSources.getChunkQiSources(level.getChunkAt(pos));
        chunkCap.getQiSources().add(source);
        PacketHandler.sendChunkQiSourcesToClient(level.getChunkAt(pos));
        src.sendSuccess(Component.literal("Added QiSource at " + pos.getX()+","+pos.getY()+","+pos.getZ()+" elem="+element+" size="+s+" storage="+st+" regen="+rg), true);
        return 1;
    }
}
