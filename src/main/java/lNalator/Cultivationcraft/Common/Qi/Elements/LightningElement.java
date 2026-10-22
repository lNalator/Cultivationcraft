package lNalator.Cultivationcraft.Common.Qi.Elements;

import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

public class LightningElement extends ElementVariant {

    protected static final double CHAIN_RANGE = 6;

    public LightningElement(ResourceLocation Element, Color elementColor, double newDensity, double chance) {
        super(Element, elementColor, newDensity, chance);

        effectTickChance = 1.0 / 200.0;
    }

    // Strike the target, then chain to 1-3 nearby enemies (design D6). Only cultivator attacks chain:
    // vanilla lightning bolts have no attacker, which also stops a bolt from spawning more bolts.
    public void applyStatusEffect(QiDamageSource source, Entity target, float damageAmount) {
        Entity attacker = source.getEntity();

        if (!(target.level instanceof ServerLevel level) || attacker == null || damageAmount <= 0 || !(target instanceof LivingEntity living)) {
            super.applyStatusEffect(source, target, damageAmount);
            return;
        }

        if (damageAmount >= 5) {
            visualBolt(level, target.position());
        }

        // Wet targets conduct to one more enemy
        int chains = Math.min(3, 1 + (int) (damageAmount / 10)) + (living.hasEffect(ElementalStates.WET.get()) ? 1 : 0);

        living.addEffect(new MobEffectInstance(ElementalStates.ELECTRIFIED.get(), 60));

        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(CHAIN_RANGE),
                e -> e != target && e != attacker && e.isAlive() && !(e instanceof Player p && p.isSpectator()));
        nearby.sort(Comparator.comparingDouble(e -> e.distanceToSqr(target)));

        String msgId = attacker instanceof Player ? "player" : "mob";

        for (LivingEntity next : nearby.subList(0, Math.min(chains, nearby.size()))) {
            // No status on chained hits, so the chain cannot chain again
            next.hurt(new QiDamageSource(msgId, attacker, name, false), damageAmount * 0.5f);
            next.addEffect(new MobEffectInstance(ElementalStates.ELECTRIFIED.get(), 60));
            visualBolt(level, next.position());

            if (attacker instanceof Player player) {
                QuestHandler.progressQuest(player, Quest.LIGHTNING_CHAIN, 1);
            }
        }

        super.applyStatusEffect(source, target, damageAmount);
    }

    protected static void visualBolt(ServerLevel level, Vec3 pos) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(pos);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    @Override
    public void effectBlock(Level level, BlockPos pos) {
        LightningBolt lightningbolt = EntityType.LIGHTNING_BOLT.create(level);
        lightningbolt.moveTo(Vec3.atBottomCenterOf(pos));
        level.addFreshEntity(lightningbolt);
    }
}
