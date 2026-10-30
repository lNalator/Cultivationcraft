package lNalator.Cultivationcraft.Common.Qi.Techniques;

import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.Elements.Element;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.AuraModifier;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.TechniqueModifier;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultTechniqueStatIDs;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.TechniqueStatModification;
import lNalator.Cultivationcraft.Cultivationcraft;
import org.joml.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

// Elemental zone around the cultivator (design D4), unlocked by the Aura concept.
// Prototype of the Nascent Soul domain: zone, element, effects on entities and blocks, cost and stats.
public class AuraTechnique extends Technique {

    public static final ResourceLocation intensity = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.tstat.intensity");

    protected static final int PULSE_TICKS = 20;
    protected static final int BLOCKS_PER_PULSE = 4;

    // Released mode also affects blocks; Contained (default) only entities. Chosen by sneaking when activating.
    protected boolean released = false;
    protected int pulseTimer = 0;

    public AuraTechnique() {
        super();

        langLocation = "cultivationcraft.technique.aura";
        Element = Elements.noElement;

        type = useType.Toggle;
        multiple = false;

        icon = new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/icons/iceaura.png");

        canLevel = true;

        addMinTechniqueStat(DefaultTechniqueStatIDs.qiCost, 0.1);

        TechniqueStatModification qiCostModification = new TechniqueStatModification(DefaultTechniqueStatIDs.qiCost);
        TechniqueStatModification rangeModification = new TechniqueStatModification(DefaultTechniqueStatIDs.range);
        TechniqueStatModification damageModification = new TechniqueStatModification(DefaultTechniqueStatIDs.damage);
        TechniqueStatModification intensityModification = new TechniqueStatModification(intensity);

        qiCostModification.addStatChange(DefaultTechniqueStatIDs.qiCost, -0.0005);
        rangeModification.addStatChange(DefaultTechniqueStatIDs.range, 0.002);
        rangeModification.addStatChange(DefaultTechniqueStatIDs.qiCost, 0.0005);
        damageModification.addStatChange(DefaultTechniqueStatIDs.damage, 0.001);
        intensityModification.addStatChange(intensity, 0.0005);

        addTechniqueStat(DefaultTechniqueStatIDs.qiCost, 1, qiCostModification);
        addTechniqueStat(DefaultTechniqueStatIDs.range, 4, rangeModification);
        addTechniqueStat(DefaultTechniqueStatIDs.damage, 1, damageModification);
        addTechniqueStat(intensity, 0.25, intensityModification);
    }

    @Override
    public boolean isValid(Player player) {
        for (TechniqueModifier mod : CultivatorStats.getCultivatorStats(player).getCultivation().getModifiers()) {
            if (mod.getID().equals(AuraModifier.AURA_ID)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void activate(Player player) {
        super.activate(player);

        if (!player.level().isClientSide) {
            released = player.isShiftKeyDown();
            pulseTimer = 0;
            player.displayClientMessage(Component.translatable(langLocation + (released ? ".released" : ".contained")), true);
        }
    }

    @Override
    public void tickServer(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        double range = getTechniqueStat(DefaultTechniqueStatIDs.range, player);

        // A bigger zone costs more
        if (!spendExternalQi(player, 0.05 * range / 4)) {
            deactivate(player);
            return;
        }

        ResourceLocation element = getElement();

        if (Elements.windElement.equals(element)) {
            deflectProjectiles(player, range);
        }

        float interval = PULSE_TICKS;
        for (TechniqueModifier mod : CultivatorStats.getCultivatorStats(player).getCultivation().getModifiers()) {
            interval *= mod.getAuraIntervalMultiplier(player);
        }

        if (++pulseTimer >= interval) {
            pulseTimer = 0;
            pulse(player, element, range);
        }

        super.tickServer(event);
    }

    protected void pulse(Player player, ResourceLocation element, double range) {
        double damage = getTechniqueStat(DefaultTechniqueStatIDs.damage, player);
        for (TechniqueModifier mod : CultivatorStats.getCultivatorStats(player).getCultivation().getModifiers()) {
            damage *= mod.getAuraDamageMultiplier(player);
        }
        double chance = getTechniqueStat(intensity, player);
        int hits = 0;

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                e -> e != player && e.isAlive() && e.distanceTo(player) <= range && !e.isSpectator()
                        && !(e instanceof TamableAnimal pet && pet.isOwnedBy(player)))) {
            // Status at reduced intensity: applied on a fraction of the pulses
            boolean status = Math.random() < chance;
            target.hurt(new QiDamageSource(player.level(), "player", player, element, status), (float) damage);

            // Pure aura: Qi pressure slows
            if (status && Elements.noElement.equals(element)) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40));
            }

            hits++;
        }

        if (hits > 0) {
            QuestHandler.progressQuest(player, Quest.AURA_HIT, hits);
            levelUp(player, hits);
        }

        Element el = Elements.getElement(element);

        if (released) {
            for (int i = 0; i < BLOCKS_PER_PULSE; i++) {
                Vec3 offset = new Vec3(Math.random() * 2 - 1, Math.random() * 2 - 1, Math.random() * 2 - 1).scale(range);
                el.effectBlock(player.level(), BlockPos.containing(player.position().add(offset)));
            }
        }

        ring(player, el, range);
    }

    // Wind aura pushes enemy projectiles away
    protected void deflectProjectiles(Player player, double range) {
        for (Projectile projectile : player.level().getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(range),
                p -> p.getOwner() != player)) {
            Vec3 away = projectile.position().subtract(player.position()).normalize().scale(0.3);
            projectile.setDeltaMovement(projectile.getDeltaMovement().add(away));
            projectile.hurtMarked = true;
        }
    }

    protected void ring(Player player, Element el, double range) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }

        DustParticleOptions dust = new DustParticleOptions(new Vector3f(el.color.getRed() / 255f, el.color.getGreen() / 255f, el.color.getBlue() / 255f), 1.5f);

        for (int i = 0; i < 32; i++) {
            double angle = Math.PI * 2 * i / 32;
            level.sendParticles(dust, player.getX() + Math.cos(angle) * range, player.getY() + 0.2, player.getZ() + Math.sin(angle) * range, 1, 0, 0, 0, 0);
        }
    }
}
