package DaoOfModding.Cultivationcraft.Common.Commands;

import DaoOfModding.Cultivationcraft.Common.Blocks.Plants.world.PlantCatalogSavedData;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlantCatalogCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("cultivation")
            .requires(src -> src.hasPermission(2)).then(Commands.literal("plantCatalog"))
            .executes(ctx -> {
                CommandSourceStack src = ctx.getSource();
                ServerLevel level = src.getLevel();
                PlantCatalogSavedData data = PlantCatalogSavedData.getOrCreate(level, 50);
                int shown = 0;
                for (var e : data.entries()) {
                    src.sendSuccess(Component.literal("[" + e.id + "] " + e.displayName + " color=#" + String.format("%06X", e.genome.colorRGB())), false);
                    if (++shown >= 50) break;
                }
                return shown;
            });
        event.getDispatcher().register(root);
    }
}

