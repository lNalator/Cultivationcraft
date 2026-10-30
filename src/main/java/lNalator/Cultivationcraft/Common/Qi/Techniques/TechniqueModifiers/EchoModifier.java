package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import lNalator.Cultivationcraft.Cultivationcraft;
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
import java.util.HashMap;
import java.util.List;
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

    private record Echo(Player owner, Entity target, float damage, ResourceLocation element, long tick) {

    }

    private static final List<Echo> pending = new ArrayList<>();
    private static final HashMap<UUID, long[]> streaks = new HashMap<>();
    private static long serverTick = 0;

    // TODO core texture (animated coreTexture shown in the cultivation screen): concentric rings rippling out of the core, repeating with a delay (no texture yet, uses blank.png)
    public EchoModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.echo");
        CATEGORY = MODIFIER_CATEGORY;
        allowSameCategory = true;

        unlockQuest = new Quest(Quest.HIT_STREAK, 1);
        stabiliseQuest = new Quest(Quest.ECHO_HIT, 1000);
    }

    @Override
    public void onHitEntity(Player owner, Entity hit, float damage, ResourceLocation element, Vec3 pos) {
        super.onHitEntity(owner, hit, damage, element, pos);

        if (owner.level().isClientSide || hit == owner) {
            return;
        }

        float power = FIRST_ECHO;
        float step = Config.Server.echoStep() / 100f;

        for (int i = 1; i <= getRank(owner); i++) {
            pending.add(new Echo(owner, hit, damage * power, element, serverTick + (long) ECHO_DELAY * i));
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

            if (echo.target.isAlive() && echo.owner.isAlive() && echo.target.level() == echo.owner.level()) {
                // Echo damage does not go through the modifiers again, so echoes never echo
                echo.target.hurt(new QiDamageSource(echo.owner.level(), ECHO_SOURCE, echo.owner, echo.element, true), echo.damage);
                QuestHandler.progressQuest(echo.owner, Quest.ECHO_HIT, 1);
            }

            return true;
        });
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
