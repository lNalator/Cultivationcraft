package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import com.mojang.math.Vector3f;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.Elements.Element;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Synergies between cores: a new effect when two given cores are owned together, never a multiplier of their bonuses.
 * Fixed values (no rank scaling) in the server config. Hidden: the Concepts menu shows "?" for a possible synergy with an
 * owned core and reveals it once both are owned. Never between two elemental cores (a player only has one), and Haste never
 * affects Warp, even through a synergy. Each effect is coded where its cores act; this class holds the list and the shared tools.
 */
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID)
public final class CoreSynergies {

    public record Synergy(String name, ResourceLocation first, ResourceLocation second) {

        public boolean involves(ResourceLocation core) {
            return first.equals(core) || second.equals(core);
        }

        public ResourceLocation other(ResourceLocation core) {
            return first.equals(core) ? second : first;
        }

        public String langKey() {
            return "cultivationcraft.synergy." + name;
        }
    }

    private static ResourceLocation concept(String name) {
        return new ResourceLocation(Cultivationcraft.MODID, "concept." + name);
    }

    public static final ResourceLocation AURA = concept("aura");
    public static final ResourceLocation EXPLOSION = concept("explosion");
    public static final ResourceLocation INFUSION = concept("infusion");
    public static final ResourceLocation WARP = concept("warp");
    public static final ResourceLocation FIRE = concept("fire");
    public static final ResourceLocation GRAVITY = concept("gravity");
    public static final ResourceLocation REPULSION = concept("repulsion");
    public static final ResourceLocation ICE = concept("ice");
    public static final ResourceLocation ECHO = concept("echo");
    public static final ResourceLocation LIGHTNING = concept("lightning");
    public static final ResourceLocation CHARGE = concept("charge");
    public static final ResourceLocation POISON = concept("poison");
    public static final ResourceLocation FALL = concept("fall");
    public static final ResourceLocation EARTH = concept("earth");
    public static final ResourceLocation WOOD = concept("wood");
    public static final ResourceLocation LEVITATION = concept("levitation");
    public static final ResourceLocation WIND = concept("wind");
    public static final ResourceLocation GROW = concept("grow");
    public static final ResourceLocation SHRINK = concept("shrink");

    private static final List<Synergy> SYNERGIES = new ArrayList<>();
    public static final List<Synergy> ALL = Collections.unmodifiableList(SYNERGIES);

    public static final Synergy AURA_EXPLOSION = add("aura_explosion", AURA, EXPLOSION);
    public static final Synergy INFUSION_WARP = add("infusion_warp", INFUSION, WARP);
    public static final Synergy FIRE_EXPLOSION = add("fire_explosion", FIRE, EXPLOSION);
    public static final Synergy GRAVITY_EXPLOSION = add("gravity_explosion", GRAVITY, EXPLOSION);
    public static final Synergy REPULSION_ICE = add("repulsion_ice", REPULSION, ICE);
    public static final Synergy ECHO_LIGHTNING = add("echo_lightning", ECHO, LIGHTNING);
    public static final Synergy ECHO_CHARGE = add("echo_charge", ECHO, CHARGE);
    public static final Synergy POISON_INFUSION = add("poison_infusion", POISON, INFUSION);
    public static final Synergy FALL_EARTH = add("fall_earth", FALL, EARTH);
    public static final Synergy FALL_WOOD = add("fall_wood", FALL, WOOD);
    public static final Synergy LEVITATION_WIND = add("levitation_wind", LEVITATION, WIND);
    public static final Synergy GROW_AURA = add("grow_aura", GROW, AURA);
    public static final Synergy SHRINK_WARP = add("shrink_warp", SHRINK, WARP);

    private static Synergy add(String name, ResourceLocation first, ResourceLocation second) {
        Synergy synergy = new Synergy(name, first, second);
        SYNERGIES.add(synergy);
        return synergy;
    }

    public static boolean has(Player player, Synergy synergy) {
        if (player == null) {
            return false;
        }

        CultivationType cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();

        return cultivation.getModifierRank(synergy.first) > 0 && cultivation.getModifierRank(synergy.second) > 0;
    }

    // Concepts menu: one line per synergy of this concept with an owned core, "?" until both cores are owned
    public static MutableComponent describe(Player player, ResourceLocation concept) {
        MutableComponent text = Component.empty();
        CultivationType cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();

        for (Synergy synergy : ALL) {
            if (!synergy.involves(concept)) {
                continue;
            }

            ResourceLocation other = synergy.other(concept);

            if (cultivation.getModifierRank(other) <= 0) {
                continue;
            }

            boolean revealed = cultivation.getModifierRank(concept) > 0;
            text.append("\n\n").append(Component.translatable("cultivationcraft.synergy.with", Component.translatable(other.toLanguageKey())))
                    .append("\n").append(revealed ? Component.translatable(synergy.langKey()) : Component.literal("?"));
        }

        return text;
    }

    // The element of the player's elemental core, or null (only Infusion + Warp needs it outside of a technique)
    public static ResourceLocation coreElement(Player player) {
        for (TechniqueModifier mod : CultivatorStats.getCultivatorStats(player).getCultivation().getModifiers()) {
            if (mod.hasElement()) {
                return mod.getElement();
            }
        }

        return null;
    }

    // Shrink + Warp: teleports and Warp flight jumps cost less Qi
    public static double warpCost(Player player, double cost) {
        return has(player, SHRINK_WARP) ? cost * Config.Server.shrinkWarpCost() : cost;
    }

    // ---- Server-side scheduling and lingering zones ----

    private record Delayed(long tick, Runnable action) {

    }

    private static final class Zone {
        final Player owner;
        final ServerLevel level;
        final Vec3 pos;
        final ResourceLocation element;
        final double radius;
        final long end;

        Zone(Player owner, ServerLevel level, Vec3 pos, ResourceLocation element, double radius, long end) {
            this.owner = owner;
            this.level = level;
            this.pos = pos;
            this.element = element;
            this.radius = radius;
            this.end = end;
        }
    }

    private static final List<Delayed> delayed = new ArrayList<>();
    private static final List<Zone> zones = new ArrayList<>();
    private static long serverTick = 0;

    public static void schedule(int ticks, Runnable action) {
        delayed.add(new Delayed(serverTick + ticks, action));
    }

    // Infusion + Warp: every teleport leaves a zone of the core's element where it started
    public static void onWarp(Player owner, Vec3 from) {
        if (owner == null || owner.level.isClientSide || !has(owner, INFUSION_WARP)) {
            return;
        }

        ResourceLocation element = coreElement(owner);

        if (element == null || element.equals(Elements.noElement)) {
            return;
        }

        zones.add(new Zone(owner, (ServerLevel) owner.level, from, element, Config.Server.warpInfusionRadius(), serverTick + Config.Server.warpInfusionTicks()));
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        serverTick++;

        if (!delayed.isEmpty()) {
            List<Delayed> due = new ArrayList<>();
            delayed.removeIf(task -> {
                if (task.tick > serverTick) {
                    return false;
                }

                due.add(task);
                return true;
            });

            // Run after the removal: an action can schedule another one
            due.forEach(task -> task.action.run());
        }

        zones.removeIf(zone -> {
            if (zone.end < serverTick || !zone.owner.isAlive() || zone.owner.isRemoved() || zone.owner.level != zone.level) {
                return true;
            }

            // Same strength as the Infusion trail, with the Poison + Infusion regeneration block when owned
            if (serverTick % 5 == 0) {
                new InfusionModifier().infuse(zone.owner, zone.pos, zone.element, zone.radius, 2);
            }

            if (serverTick % 4 == 0) {
                Element el = Elements.getElement(zone.element);
                DustParticleOptions dust = new DustParticleOptions(new Vector3f(el.color.getRed() / 255f, el.color.getGreen() / 255f, el.color.getBlue() / 255f), 1.2f);

                for (int i = 0; i < 6; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double distance = Math.sqrt(Math.random()) * zone.radius;
                    zone.level.sendParticles(dust, zone.pos.x + Math.cos(angle) * distance, zone.pos.y + 0.1 + Math.random() * 0.5, zone.pos.z + Math.sin(angle) * distance, 1, 0, 0.02, 0, 0);
                }
            }

            return false;
        });
    }
}
