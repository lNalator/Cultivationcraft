package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Common.Config;
import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ExplosionModifier extends TechniqueModifier {

    public ExplosionModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.explosion");
        unique = true;
        CATEGORY = MODIFIER_CATEGORY;

        unlockQuest = new Quest(Quest.EXPLOSION_DAMAGE_TAKEN, 1);
        stabiliseQuest = new Quest(Quest.EXPLOSION_DAMAGE_DEALT, 10000);

        coreTexture = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/cores/explosion.png"), 4, 4);

        allowSameCategory = true;
        qiMult = 2;
    }

    @Override
    public void onHitAll(Player owner, Vec3 pos, float damage, ResourceLocation element) {
        if (owner.level.isClientSide) {
            return;
        }

        // Temp radius calculation
        float radius = 1 + damage / 5f;

        // Gravity + Explosion synergy: Gravity pulls the targets together at the impact (its own onHitAll), the blast follows.
        // The fuse is seen and heard, so there is time to resist the pull or get out
        if (CoreSynergies.has(owner, CoreSynergies.GRAVITY_EXPLOSION)) {
            ServerLevel level = (ServerLevel) owner.level;
            level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 12, 0.2, 0.2, 0.2, 0.02);
            level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.TNT_PRIMED, SoundSource.PLAYERS, 0.6f, 1.6f);

            CoreSynergies.schedule(Config.Server.gravityExplosionDelay(), () -> {
                if (owner.isAlive() && owner.level == level) {
                    explode(owner, pos, element, radius);
                }
            });
            return;
        }

        explode(owner, pos, element, radius);
    }

    public void onHitTaken(Player owner, float damage, ResourceLocation defensiveElement, QiDamageSource source) {
        if (owner.level.isClientSide) {
            return;
        }

        // Temp radius calculation
        float radius = 1 + damage / 5f;

        explode(owner, owner.position(), defensiveElement, radius);
    }

    public static void explode(Player owner, Vec3 pos, ResourceLocation element, float radius) {
        explode(owner, pos, element, radius, Explosion.BlockInteraction.BREAK);
    }

    public static void explode(Player owner, Vec3 pos, ResourceLocation element, float radius, Explosion.BlockInteraction interaction) {
        // Fire + Explosion synergy: the blast burns what it hits (normal Burn, no damage bonus) and sets the ground on fire
        boolean fire = CoreSynergies.has(owner, CoreSynergies.FIRE_EXPLOSION);

        if (fire) {
            element = Elements.fireElement;
        }

        Explosion explosion = new Explosion(owner.level, owner, new QiDamageSource("explosion.player", owner, element, true), null, pos.x, pos.y, pos.z, radius, fire, interaction);

        explosion.explode();
        explosion.finalizeExplosion(false);

        // A blast that breaks nothing sends no blocks: the client would otherwise remove them on its side
        List<net.minecraft.core.BlockPos> blown = interaction == Explosion.BlockInteraction.NONE ? List.of() : explosion.getToBlow();

        for (ServerPlayer serverplayer : ((ServerLevel) owner.level).players()) {
            if (serverplayer.distanceToSqr(pos) < 4096.0D) {
                serverplayer.connection.send(new ClientboundExplodePacket(pos.x, pos.y, pos.z, radius, blown, explosion.getHitPlayers().get(serverplayer)));
            }
        }
    }
}
