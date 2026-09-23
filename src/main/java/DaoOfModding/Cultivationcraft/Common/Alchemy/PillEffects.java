package DaoOfModding.Cultivationcraft.Common.Alchemy;

import DaoOfModding.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import DaoOfModding.Cultivationcraft.Common.Qi.BodyParts.FoodStats.QiFoodStats;
import DaoOfModding.Cultivationcraft.Common.Qi.Cultivation.FoundationEstablishmentCultivation;
import DaoOfModding.Cultivationcraft.Common.Qi.CultivationTypes;
import DaoOfModding.Cultivationcraft.Common.Qi.Elements.Elements;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import DaoOfModding.Cultivationcraft.Network.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID)
public final class PillEffects {
    public static CompoundTag data(Player player) { return CultivatorStats.getCultivatorStats(player).getPillData(); }
    public static boolean identified(Player player, ItemStack stack) {
        if (player == null || !stack.hasTag()) return false;
        String entry = stack.getTag().getString("Entry");
        return !entry.isEmpty() && (data(player).getBoolean("Known:" + entry) || data(player).getBoolean("Recipe:" + entry));
    }

    /** Reuses Bind's timing/progress display; never binds, transforms, or consumes the pill. */
    public static void refineInBind(Player player, ItemStack stack, long elapsedNanos) {
        var tag = stack.getOrCreateTag();
        if (tag.getString("Entry").isEmpty() || PillDefinition.get(tag.getString("Pill")) == null) return;
        String owner = player.getStringUUID();
        if (!owner.equals(tag.getString("AnalyzingPlayer"))) {
            tag.putString("AnalyzingPlayer", owner);
            tag.putLong("AnalysisTime", 0);
        }
        long progress = identified(player, stack) ? 5_000_000_000L
                : Math.min(5_000_000_000L, tag.getLong("AnalysisTime") + Math.max(0, Math.min(elapsedNanos, 250_000_000L)));
        tag.putLong("AnalysisTime", progress);
        tag.putFloat("BindPercent", progress / 5_000_000_000f);
        tag.putFloat("BindRemaining", (5_000_000_000L - progress) / 1_000_000_000f);
        if (progress == 5_000_000_000L && !identified(player, stack)) {
            data(player).putBoolean("Known:" + tag.getString("Entry"), true);
            PacketHandler.sendCultivatorStatsToClient(player);
        }
    }

    private static long now(Player player) {
        return player instanceof ServerPlayer server ? server.server.overworld().getGameTime() : player.level.getGameTime();
    }

    public static double absorptionBonus(Player player) {
        if (!(CultivatorStats.getCultivatorStats(player).getCultivation() instanceof FoundationEstablishmentCultivation)) return 0;
        CompoundTag data = data(player);
        return data.getLong("AbsorptionUntil") > now(player) ? data.getDouble("AbsorptionAmount") : 0;
    }

    public static boolean consume(ServerPlayer player, ItemStack stack) {
        if (!stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        PillDefinition definition = PillDefinition.fromStack(stack);
        if (definition == null || tag.getString("Entry").isEmpty()) return false;
        var stats = CultivatorStats.getCultivatorStats(player);
        var cultivation = stats.getCultivation();
        CompoundTag data = data(player);
        long time = now(player);
        String cooldown = "Cooldown:" + definition.group();
        if (data.getLong(cooldown) > time) return reject(player, "cooldown", (data.getLong(cooldown) - time + 19) / 20);
        if (definition.cultivation() && !(cultivation instanceof FoundationEstablishmentCultivation))
            return reject(player, "foundation_only");
        if (definition.effect() == PillDefinition.Effect.FOOD && stats.getCultivationType() != CultivationTypes.BODY_CULTIVATOR)
            return reject(player, "body_only");
        if (definition.group().equals("qi") && (stats.getCultivationType() != CultivationTypes.QI_CONDENSER
                || !(player.getFoodData() instanceof QiFoodStats))) return reject(player, "external_only");
        ResourceLocation affinity = ResourceLocation.tryParse(tag.getString("Affinity"));
        if (definition.cultivation() && (affinity == null || affinity.equals(Elements.noElement) || !cultivation.canCultivate(affinity)))
            return reject(player, "affinity_mismatch");
        double amount = definition.amount();
        // Cooldowns are per effect family and persisted in the player's capability.
        // Purity describes batch quality; potency changes need separate balancing.
        switch (definition.effect()) {
            case HEAL, HEAL_OVER_TIME -> {
                if (player.getHealth() >= player.getMaxHealth()) return reject(player, "full");
                float hp = (float) (amount * player.getMaxHealth());
                if (definition.effect() == PillDefinition.Effect.HEAL) player.heal(hp);
                else schedule(data, "Healing", hp, definition.duration(), time);
            }
            case QI, QI_OVER_TIME -> {
                QiFoodStats food = (QiFoodStats) player.getFoodData();
                if (food.getTrueFoodLevel() >= food.getMaxFood()) return reject(player, "full");
                float qi = (float) (amount * food.getMaxFood());
                if (definition.effect() == PillDefinition.Effect.QI) restoreQi(player, qi);
                else schedule(data, "Qi", qi, definition.duration(), time);
            }
            case CULTIVATION -> {
                float supplied = (float) amount;
                float unused = cultivation.progressCultivation(player, supplied, affinity);
                if (unused >= supplied) return reject(player, "full");
            }
            case ABSORPTION -> {
                data.putDouble("AbsorptionAmount", definition.amount());
                data.putLong("AbsorptionUntil", time + Math.max(1, definition.duration() * 20L));
            }
            case FOOD -> {
                int max = player.getFoodData() instanceof QiFoodStats food ? food.getMaxFood() : 20;
                double current = player.getFoodData() instanceof QiFoodStats food ? food.getTrueFoodLevel() : player.getFoodData().getFoodLevel();
                if (current >= max) return reject(player, "full");
                player.getFoodData().eat((int) definition.amount(), .5f); // Four hunger and four saturation.
            }
        }
        data.putLong(cooldown, time + (long) definition.cooldown() * 20);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        PacketHandler.sendCultivatorStatsToClient(player);
        return true;
    }

    private static boolean reject(Player player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("cultivationcraft.pill." + key, args), true);
        return false;
    }

    private static void schedule(CompoundTag data, String effect, float amount, int seconds, long time) {
        data.putFloat(effect + "PerPulse", amount / seconds);
        data.putInt(effect + "Pulses", seconds);
        data.putLong(effect + "Next", time + 20);
        data.putLong(effect + "End", time + seconds * 20L);
    }

    private static void restoreQi(Player player, float amount) {
        if (CultivatorStats.getCultivatorStats(player).getCultivationType() == CultivationTypes.QI_CONDENSER
                && player.getFoodData() instanceof QiFoodStats food)
            food.setFoodLevel(Math.min(food.getMaxFood(), food.getTrueFoodLevel() + amount));
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !player.isAlive()) return;
        CompoundTag data = data(player);
        long time = now(player);
        for (String effect : new String[]{"Healing", "Qi"}) {
            int pulses = data.getInt(effect + "Pulses");
            if (pulses <= 0 || time < data.getLong(effect + "Next")) continue;
            if (time > data.getLong(effect + "End")) { data.putInt(effect + "Pulses", 0); continue; }
            float amount = data.getFloat(effect + "PerPulse");
            if (effect.equals("Healing")) player.heal(amount);
            else restoreQi(player, amount);
            data.putInt(effect + "Pulses", pulses - 1);
            data.putLong(effect + "Next", time + 20);
        }
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        // Knowledge and cooldowns survive death; temporary benefits do not.
        if (event.isEndConquered()) return;
        CompoundTag data = data(event.getEntity());
        data.remove("AbsorptionUntil");
        data.remove("HealingPulses");
        data.remove("QiPulses");
        PacketHandler.sendCultivatorStatsToClient(event.getEntity());
    }
}
