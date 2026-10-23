package lNalator.Cultivationcraft.Common.Qi.Elements;

import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Quests.QuestHandler;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Elemental states applied by element hits (design D2/E4), shared by mobs and players
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID)
public final class ElementalStates {

    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Cultivationcraft.MODID);

    // Water: only matters for interactions, and keeps the target from burning
    public static final RegistryObject<MobEffect> WET = EFFECTS.register("wet", () -> new State(0x3F76E4));
    // Ice: -10% speed per level (amplifier 0-3), the 5th hit freezes instead
    public static final RegistryObject<MobEffect> CHILL = EFFECTS.register("chill", () -> new State(0x9FE6F5)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "6c1f0b4e-5d3a-4f7e-9b1a-2c3d4e5f6a01", -0.1, AttributeModifier.Operation.MULTIPLY_TOTAL));
    // Ice: cannot move, jump, attack or use items
    public static final RegistryObject<MobEffect> FROZEN = EFFECTS.register("frozen", () -> new State(0xC9F4FF)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "6c1f0b4e-5d3a-4f7e-9b1a-2c3d4e5f6a02", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
    // Given when Frozen ends, so a target cannot be kept frozen forever
    public static final RegistryObject<MobEffect> FREEZE_IMMUNE = EFFECTS.register("freeze_immune", () -> new State(MobEffectCategory.BENEFICIAL, 0xE8FBFF));
    // Wind: +50% knockback taken, no sprinting
    public static final RegistryObject<MobEffect> OFF_BALANCE = EFFECTS.register("off_balance", () -> new State(0x7FE0A0));
    // Lightning: interrupts channelled techniques when applied
    public static final RegistryObject<MobEffect> ELECTRIFIED = EFFECTS.register("electrified", () -> new State(0xFFF04A));
    // Earth: heavy slowness, no jumping
    public static final RegistryObject<MobEffect> STUNNED = EFFECTS.register("stunned", () -> new State(0xB07A3C)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "6c1f0b4e-5d3a-4f7e-9b1a-2c3d4e5f6a03", -0.8, AttributeModifier.Operation.MULTIPLY_TOTAL));
    // Wood: cannot move or jump, can still fight
    public static final RegistryObject<MobEffect> ROOTED = EFFECTS.register("rooted", () -> new State(0x3E8E3E)
            .addAttributeModifier(Attributes.MOVEMENT_SPEED, "6c1f0b4e-5d3a-4f7e-9b1a-2c3d4e5f6a04", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));

    // Poison concept: blocks every regeneration while it lasts
    public static final RegistryObject<MobEffect> NO_REGEN = EFFECTS.register("no_regen", () -> new State(0x4E9331));

    public static final int CHILL_DECAY_TICKS = 60;
    public static final int MAX_CHILL = 5;
    public static final int FROZEN_TICKS = 40;
    public static final int FREEZE_IMMUNE_TICKS = 100;

    public static void init(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private static final class State extends MobEffect {

        private State(int color) {
            this(MobEffectCategory.HARMFUL, color);
        }

        private State(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    public static boolean has(Entity entity, RegistryObject<MobEffect> state) {
        return entity instanceof LivingEntity living && living.hasEffect(state.get());
    }

    // One Ice hit: +1 Chill, or Frozen on the 5th level (design E4)
    public static void addChill(LivingEntity target, Entity source) {
        if (target.level.isClientSide || target.hasEffect(FROZEN.get()) || target.hasEffect(FREEZE_IMMUNE.get())) {
            return;
        }

        MobEffectInstance chill = target.getEffect(CHILL.get());
        int level = chill == null ? 1 : chill.getAmplifier() + 2;

        if (level >= MAX_CHILL) {
            target.removeEffect(CHILL.get());
            freeze(target, source);
            return;
        }

        target.removeEffect(CHILL.get());
        target.addEffect(new MobEffectInstance(CHILL.get(), CHILL_DECAY_TICKS, level - 1));
    }

    public static void freeze(LivingEntity target, Entity source) {
        // Halved in PvP
        int duration = target instanceof Player && source instanceof Player ? FROZEN_TICKS / 2 : FROZEN_TICKS;
        target.addEffect(new MobEffectInstance(FROZEN.get(), duration));

        if (source instanceof Player player) {
            QuestHandler.progressQuest(player, Quest.FREEZE_ENTITY, 1);
        }
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.hasEffect(FROZEN.get())) {
            // Vanilla frost overlay and shaking as the frozen visual
            entity.setTicksFrozen(entity.getTicksRequiredToFreeze() + 2);
        }

        if (entity.hasEffect(OFF_BALANCE.get()) && entity.isSprinting()) {
            entity.setSprinting(false);
        }

        if (entity.hasEffect(WET.get()) && entity.isOnFire()) {
            entity.clearFire();
        }

        if (entity.level.isClientSide) {
            return;
        }

        // Chill loses one level every 3 s without a new hit. Done here, before the effect expires,
        // because adding an effect from the expiry event happens while effects are being iterated.
        MobEffectInstance chill = entity.getEffect(CHILL.get());
        if (chill != null && chill.getDuration() <= 1 && chill.getAmplifier() > 0) {
            entity.removeEffect(CHILL.get());
            entity.addEffect(new MobEffectInstance(CHILL.get(), CHILL_DECAY_TICKS, chill.getAmplifier() - 1));
        }

        MobEffectInstance frozen = entity.getEffect(FROZEN.get());
        if (frozen != null && frozen.getDuration() <= 1) {
            entity.addEffect(new MobEffectInstance(FREEZE_IMMUNE.get(), FREEZE_IMMUNE_TICKS));
        }
    }

    // Runs after the mod's own jump physics
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void jump(LivingEvent.LivingJumpEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.hasEffect(FROZEN.get()) || entity.hasEffect(STUNNED.get()) || entity.hasEffect(ROOTED.get())) {
            Vec3 motion = entity.getDeltaMovement();
            entity.setDeltaMovement(motion.x, Math.min(0, motion.y), motion.z);
        }
    }

    @SubscribeEvent
    public static void knockBack(LivingKnockBackEvent event) {
        if (event.getEntity().hasEffect(OFF_BALANCE.get())) {
            event.setStrength(event.getStrength() * 1.5f);
        }
    }

    // Frozen entities cannot attack: players through AttackEntityEvent, mobs through their damage source
    @SubscribeEvent
    public static void playerAttack(AttackEntityEvent event) {
        if (has(event.getEntity(), FROZEN)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void mobAttack(LivingAttackEvent event) {
        if (has(event.getSource().getEntity(), FROZEN)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void useItem(PlayerInteractEvent.RightClickItem event) {
        if (has(event.getEntity(), FROZEN)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void useBlock(PlayerInteractEvent.RightClickBlock event) {
        if (has(event.getEntity(), FROZEN)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void mine(PlayerInteractEvent.LeftClickBlock event) {
        if (has(event.getEntity(), FROZEN)) {
            event.setCanceled(true);
        }
    }

    // Electrified interrupts channelled techniques
    @SubscribeEvent
    public static void added(MobEffectEvent.Added event) {
        if (event.getEffectInstance().getEffect() != ELECTRIFIED.get() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        ICultivatorTechniques techs = CultivatorTechniques.getCultivatorTechniques(player);
        boolean changed = false;

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            Technique tech = techs.getTechnique(i);

            if (tech != null && tech.isActive() && tech.getType() == Technique.useType.Channel) {
                tech.deactivate(player);
                changed = true;
            }
        }

        if (changed) {
            PacketHandler.sendCultivatorTechniquesToClient(player);
        }
    }
}
