package lNalator.Cultivationcraft.Common.Commands;

import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.ICultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.PlayerHealthManager;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CoreFormingCultivation;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Common.Qi.ExternalCultivationHandler;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.QiModifier;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.TechniqueModifier;
import lNalator.Cultivationcraft.Network.PacketHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Development-only shortcuts to reach a cultivation state quickly (design C1).
// Registered in the dev environment, or in a normal game launched with -Dcultivationcraft.dev=true.
// Quest completion and Qi refill already exist under /cultivation completeQuest / fillQi.
public class CultiDevCommand {

    public static boolean enabled() {
        return !FMLEnvironment.production || Boolean.getBoolean("cultivationcraft.dev");
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!enabled()) {
            return;
        }

        dispatcher.register(Commands.literal("cultidev").requires(source -> source.hasPermission(2))
                .then(Commands.literal("realm")
                        .then(Commands.literal("qi_condensation").then(Commands.argument("stage", IntegerArgumentType.integer(1, 8))
                                .executes(ctx -> realm(ctx, false))))
                        .then(Commands.literal("core_forming").then(Commands.argument("stage", IntegerArgumentType.integer(1, 9))
                                .executes(ctx -> realm(ctx, true)))))
                .then(Commands.literal("learn")
                        .then(Commands.literal("all").executes(ctx -> learn(ctx, null)))
                        .then(Commands.argument("concept", ResourceLocationArgument.id()).suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                ExternalCultivationHandler.getModifiers().stream().map(TechniqueModifier::getID), builder))
                                .executes(ctx -> learn(ctx, ResourceLocationArgument.getId(ctx, "concept")))))
                .then(Commands.literal("quest").then(Commands.literal("rate").executes(CultiDevCommand::rate)))
                .then(Commands.literal("core")
                        .then(Commands.argument("concept", ResourceLocationArgument.id()).suggests((ctx, builder) -> SharedSuggestionProvider.suggestResource(
                                ExternalCultivationHandler.getModifiers().stream().map(TechniqueModifier::getID), builder))
                                .executes(CultiDevCommand::core))));
    }

    // Started measurements: player -> {game time, quest progress}
    protected static final Map<UUID, double[]> rates = new HashMap<>();

    // First call starts measuring the current cultivation quest, second call prints its progress per minute
    protected static int rate(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CultivationType cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();

        if (cultivation.getQuest() == null) {
            ctx.getSource().sendFailure(Component.literal("No cultivation quest to measure"));
            return 0;
        }

        double[] start = rates.remove(player.getUUID());

        if (start == null) {
            rates.put(player.getUUID(), new double[]{player.level.getGameTime(), cultivation.getQuestProgress()});
            ctx.getSource().sendSuccess(Component.literal("Measuring \"" + cultivation.getQuest().getDescription() + "\"; run the command again to stop"), false);
            return 1;
        }

        double minutes = (player.level.getGameTime() - start[0]) / 1200.0;
        double perMinute = minutes > 0 ? (cultivation.getQuestProgress() - start[1]) / minutes : 0;
        ctx.getSource().sendSuccess(Component.literal(String.format("%.1f per minute over %.1f min (quest needs %.0f)", perMinute, minutes, cultivation.getQuest().complete)), false);
        return 1;
    }

    protected static int realm(CommandContext<CommandSourceStack> ctx, boolean coreForming) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ICultivatorStats stats = CultivatorStats.getCultivatorStats(player);
        int stage = IntegerArgumentType.getInteger(ctx, "stage");
        // Build on the realm below the target: the stages and cores of the realms above are dropped,
        // otherwise cores condensed in Core Forming stay owned in Qi Condensation (or count twice)
        CultivationType current = stats.getCultivation();

        while (current instanceof CoreFormingCultivation || (!coreForming && current instanceof QiCondenserCultivation)) {
            current = current.getPreviousCultivation();
        }

        stats.setCultivationType(CultivationTypes.QI_CONDENSER);

        if (coreForming) {
            CoreFormingCultivation cultivation = new CoreFormingCultivation(stage);

            // A Core Forming stage always has a core (the cultivation screen draws it):
            // the main element when coming from Qi Condensation, Pure otherwise
            TechniqueModifier firstCore = new QiModifier();

            if (current instanceof QiCondenserCultivation qiCondenser) {
                // Learn Pure and the main element as a real breakthrough would
                cultivation.doPreAdvancementActions(current, player);
                cultivation.setPreviousCultivation(current);

                for (TechniqueModifier mod : ExternalCultivationHandler.getModifiers()) {
                    if (mod.hasElement() && mod.getElement().equals(qiCondenser.getCurrentElementFocus())) {
                        firstCore = mod;
                    }
                }
            }

            cultivation.setCore(firstCore);

            stats.setCultivation(cultivation);
        } else {
            QiCondenserCultivation cultivation = new QiCondenserCultivation(stage);
            cultivation.setPreviousCultivation(current);
            stats.setCultivation(cultivation);
        }

        // Qi cultivators use Qi instead of food: without this a player with no cultivation crashes on tick
        PlayerHealthManager.updateFoodStats(player);
        PacketHandler.sendCultivatorStatsToClient(player);
        ctx.getSource().sendSuccess(Component.literal("Realm set to " + (coreForming ? "Core Forming " : "Qi Condensation ") + stage), true);
        return 1;
    }

    protected static int learn(CommandContext<CommandSourceStack> ctx, ResourceLocation id) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        int learnt = 0;

        for (TechniqueModifier mod : ExternalCultivationHandler.getModifiers()) {
            if ((id == null || mod.getID().equals(id)) && mod.getUnlockQuest() != null) {
                mod.learn(player);
                learnt++;
            }
        }

        ctx.getSource().sendSuccess(Component.literal("Learnt " + learnt + " concept(s)"), true);
        return learnt;
    }

    protected static int core(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        if (!(CultivatorStats.getCultivatorStats(player).getCultivation() instanceof CoreFormingCultivation cultivation)) {
            ctx.getSource().sendFailure(Component.literal("Use /cultidev realm core_forming <stage> first"));
            return 0;
        }

        TechniqueModifier mod = ExternalCultivationHandler.getModifier(ResourceLocationArgument.getId(ctx, "concept"));

        if (mod == null) {
            ctx.getSource().sendFailure(Component.literal("Unknown concept"));
            return 0;
        }

        // No category or duplicate checks: this is for testing
        cultivation.setCore(mod);
        PacketHandler.sendCultivatorStatsToClient(player);
        ctx.getSource().sendSuccess(Component.literal("Added core " + mod.getID()), true);
        return 1;
    }
}
