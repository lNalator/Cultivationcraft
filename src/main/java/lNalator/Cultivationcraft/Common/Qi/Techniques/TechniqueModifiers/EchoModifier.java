package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Network.PacketHandler;
import lNalator.Cultivationcraft.Network.Packets.LightningArcPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.PacketDistributor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Each technique hit repeats on its target, 1 s apart: the first echo at 40% of the hit, each rank adds one more,
// a few points weaker than the previous one (echo_step in the server config), never below 5%
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID)
public class EchoModifier extends TechniqueModifier {

    protected static final String ECHO_SOURCE = "echo";
    protected static final float FIRST_ECHO = 0.4f;
    protected static final float MIN_ECHO = 0.05f;
    protected static final int ECHO_DELAY = 20;

    // Echo unlock: 25 hits taken in a row, at most 1 s apart
    protected static final int STREAK = 25;

    private record Echo(Player owner, Entity target, float damage, ResourceLocation element, long tick, Chain chain) {

    }

    // Echo + Lightning synergy: the echoes of one hit jump from target to target, never twice on the same one
    private static final class Chain {
        Vec3 from;
        final Set<Entity> hit = new HashSet<>();

        Chain(Entity first) {
            from = first.getBoundingBox().getCenter();
            hit.add(first);
        }
    }

    private static final List<Echo> pending = new ArrayList<>();
    private static final HashMap<UUID, long[]> streaks = new HashMap<>();
    private static long serverTick = 0;

    public EchoModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.echo");
        coreTexture = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/cores/echo.png"), 8, 3);
        CATEGORY = MODIFIER_CATEGORY;
        allowSameCategory = true;

        unlockQuest = new Quest(Quest.HIT_STREAK, 1);
        stabiliseQuest = new Quest(Quest.ECHO_HIT, 1000);
    }

    @Override
    public void onHitEntity(Player owner, Entity hit, float damage, ResourceLocation element, Vec3 pos) {
        super.onHitEntity(owner, hit, damage, element, pos);

        if (owner.level.isClientSide || hit == owner) {
            return;
        }

        float power = FIRST_ECHO;
        float step = Config.Server.echoStep() / 100f;

        // Echo + Charge synergy: only the echoes of a charged shot keep its charge, the others repeat the uncharged hit
        if (!CoreSynergies.has(owner, CoreSynergies.ECHO_CHARGE) && ChargeModifier.hitCharge > 1) {
            damage /= ChargeModifier.hitCharge;
        }

        Chain chain = CoreSynergies.has(owner, CoreSynergies.ECHO_LIGHTNING) ? new Chain(hit) : null;

        for (int i = 1; i <= getRank(owner); i++) {
            pending.add(new Echo(owner, hit, damage * power, element, serverTick + (long) ECHO_DELAY * i, chain));
            power = Math.max(MIN_ECHO, power - step);
        }
    }

    @SubscribeEvent
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        serverTick++;

        pending.removeIf(echo -> {
            if (echo.tick > serverTick) {
                return false;
            }

            if (echo.chain != null) {
                jump(echo);
                return true;
            }

            if (echo.target.isAlive() && echo.owner.isAlive() && echo.target.level == echo.owner.level) {
                // Echo damage does not go through the modifiers again, so echoes never echo
                echo.target.hurt(new QiDamageSource(ECHO_SOURCE, echo.owner, echo.element, true), echo.damage);
                QuestHandler.progressQuest(echo.owner, Quest.ECHO_HIT, 1);
            }

            return true;
        });
    }

    // Echo + Lightning synergy: the echo strikes another target within a few blocks of the last one, drawn as a lightning arc.
    // Nothing happens without another target
    private static void jump(Echo echo) {
        Player owner = echo.owner;

        if (!owner.isAlive() || echo.target.level != owner.level) {
            return;
        }

        Chain chain = echo.chain;
        double range = Config.Server.echoLightningRange();
        Vec3 from = chain.from;

        LivingEntity next = owner.level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(range),
                        e -> e != owner && e.isAlive() && !e.isSpectator() && !chain.hit.contains(e) && e.getBoundingBox().getCenter().distanceTo(from) <= range
                                && !(e instanceof TamableAnimal pet && pet.isOwnedBy(owner)))
                .stream().min(Comparator.comparingDouble(e -> e.getBoundingBox().getCenter().distanceToSqr(from))).orElse(null);

        if (next == null) {
            return;
        }

        Vec3 to = next.getBoundingBox().getCenter();
        chain.hit.add(next);
        chain.from = to;

        next.hurt(new QiDamageSource(ECHO_SOURCE, owner, echo.element, true), echo.damage);
        QuestHandler.progressQuest(owner, Quest.ECHO_HIT, 1);

        PacketHandler.channel.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> next), new LightningArcPacket(from, to));
        ((ServerLevel) owner.level).playSound(null, to.x, to.y, to.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4f, 1.8f);
    }

    @SubscribeEvent
    public static void hitTaken(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getSource().getEntity() == null) {
            return;
        }

        // [streak, last hit tick]
        long[] streak = streaks.computeIfAbsent(player.getUUID(), id -> new long[]{0, 0});
        streak[0] = serverTick - streak[1] <= 20 ? streak[0] + 1 : 1;
        streak[1] = serverTick;

        if (streak[0] >= STREAK) {
            QuestHandler.progressQuest(player, Quest.HIT_STREAK, 1);
            streak[0] = 0;
        }
    }
}
