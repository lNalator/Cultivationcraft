package lNalator.Cultivationcraft.Common.Qi.Stats;

import lNalator.Cultivationcraft.Common.Capabilities.BodyModifications.BodyModifications;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.ICultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CoreFormingCultivation;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.FoundationEstablishmentCultivation;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.BodyPart;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.BodyPartOption;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.PlayerHealthManager;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Techniques.PassiveTechniques.PassiveTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;

import java.util.HashMap;
import java.util.UUID;

public class PlayerStatControl {

    PlayerStatModifications stats = new PlayerStatModifications();

    // Cultivation the realm bonuses were last computed for, so a breakthrough (whatever triggered it) re-applies them
    public CultivationType realmBonusSource = null;

    // Fixed UUIDs so each player's modifiers can be removed reliably and never stack
    private static final UUID HEALTH_MODIFIER_UUID = UUID.fromString("84d45106-aebb-4011-bf83-de53c55c3ac5");
    private static final UUID MOVEMENT_MODIFIER_UUID = UUID.fromString("b2051efe-8548-4173-8f32-ee0675e2de1d");
    private static final UUID ARMOR_MODIFIER_UUID = UUID.fromString("214421c7-5f17-495c-adc5-a731716b5d7b");
    private static final UUID ARMOR_TOUGHNESS_MODIFIER_UUID = UUID.fromString("78aeca6f-1be9-476c-9993-828637be2aaa");
    private static final UUID SWIM_MODIFIER_UUID = UUID.fromString("781519e4-4953-48c6-93ba-3a7640422b2a");
    private static final UUID ATTACK_MODIFIER_UUID = UUID.fromString("e44823a2-c178-4b9c-a671-9027d2a4e06d");
    private static final UUID RANGE_MODIFIER_UUID = UUID.fromString("e120d93c-8ad1-4fc2-aab9-af933ab728e5");
    private static final UUID STEP_HEIGHT_MODIFIER_UUID = UUID.fromString("2a4be101-0637-4ff0-a1db-51e7c1d25f60");

    // Setup players with the default player stats
    public PlayerStatControl() {
        setupStats(stats);
    }

    public void setupStats(PlayerStatModifications newStats) {
        newStats.setStat(StatIDs.weight, StatIDs.defaultWeight);
        newStats.setStat(StatIDs.maxHP, StatIDs.defaultMaxHP);
        newStats.setStat(StatIDs.maxStamina, StatIDs.defaultMaxStamina);
        newStats.setStat(StatIDs.lungCapacity, StatIDs.defaultLungCapacity);
        newStats.setStat(StatIDs.qiAbsorbRange, StatIDs.defaultQiAbsorbRange);
        newStats.setStat(StatIDs.staminaUse, StatIDs.defaultStaminaUse);
        newStats.setStat(StatIDs.healthRegen, StatIDs.defaultHealthRegen);
        newStats.setStat(StatIDs.healthStaminaConversion, StatIDs.defaulthealthStaminaConversion);
        newStats.setStat(StatIDs.movementSpeed, StatIDs.defaultMovementSpeed);
        newStats.setStat(StatIDs.swimSpeed, StatIDs.defaultSwimSpeed);
        newStats.setStat(StatIDs.attackRange, StatIDs.defaultAttackRange);
        newStats.setStat(StatIDs.jumpHeight, StatIDs.defaultJumpHeight);
        newStats.setStat(StatIDs.fallHeight, StatIDs.defaultFallHeight);
        newStats.setStat(StatIDs.legSupport, StatIDs.defaultLegSupport);
        newStats.setStat(StatIDs.armAttackModifier, StatIDs.defaultAttackModifier);
        newStats.setStat(StatIDs.attackRange, StatIDs.defaultAttackRange);
        newStats.setElementalStat(StatIDs.resistanceModifier, Elements.lightningElement, StatIDs.defaultLightningResist);
    }

    public PlayerStatModifications getStats() {
        return stats;
    }

    public void updateStats(Player player) {
        PlayerStatModifications newStats = new PlayerStatModifications();

        // Clear the existing stats
        setupStats(newStats);

        newStats.setStat(StatIDs.qiAbsorb, BodyPartQiCostController.calculateQiAbsorb(player));
        newStats.setStat(StatIDs.qiCost, BodyPartQiCostController.calculateQiCost(player));

        // Add all existing body part stats to the players stats
        for (BodyPart part : BodyModifications.getBodyModifications(player).getModifications().values()) {
            if (player.level.isClientSide()) {
                part.onLoad(player.getUUID());
            }

            newStats.combine(part.getStatChanges());
        }

        // Add all existing body part option stats to the players stats
        for (HashMap<String, BodyPartOption> options : BodyModifications.getBodyModifications(player).getModificationOptions().values()) {
            for (BodyPartOption part : options.values()) {
                if (player.level.isClientSide()) {
                    part.onLoad(player.getUUID());
                }

                newStats.combine(part.getStatChanges());
            }
        }

        // Add all existing stat modifiers on active techniques to the player stats
        ICultivatorTechniques techs = CultivatorTechniques.getCultivatorTechniques(player);

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            Technique tech = techs.getTechnique(i);
            if (tech != null && tech.isActive() && tech.getStats() != null) {
                newStats.combine(tech.getStats());
            }
        }

        for (PassiveTechnique passive : techs.getPassives()) {
            newStats.combine(passive.getStats());
        }

        calculateRealmBonuses(player, newStats);
        calculateVariantResistances(player, newStats);
        calculateSizeStatChanges(newStats);

        // Apply resistance caps
        BodyPartStatControl.applyCaps(player, newStats);

        stats = newStats;

        // Only apply player attribute modifiers on the server
        if (!player.level.isClientSide) {
            applyStats(player);
        }

        PlayerHealthManager.updateFoodStats(player);
        PlayerHealthManager.updateLungs(player);
    }

    // Applies additional stat changes based on player size
    protected void calculateSizeStatChanges(PlayerStatModifications newStats) {
        if (newStats.getStats().get(StatIDs.size) == null) {
            return;
        }

        PlayerStatModifications sizeChanges = new PlayerStatModifications();

        float sizeAdjustment = getSizeAdjustment(newStats);

        sizeChanges.setStat(StatIDs.attackRange, sizeAdjustment * StatIDs.defaultAttackRange);

        if (sizeAdjustment > 0) {
            sizeChanges.setStat(StatIDs.stepHeight, sizeAdjustment * 0.5f);
        }

        newStats.combine(sizeChanges);
    }

    public float getSizeAdjustment() {
        return getSizeAdjustment(stats);
    }

    public float getSizeAdjustment(PlayerStatModifications newStats) {
        if (!newStats.getStats().containsKey(StatIDs.size)) {
            return 0;
        }

        return newStats.getStats().get(StatIDs.size);
    }

    // Bonus per stage passed through each external realm: Foundation +1 max HP, Qi Condensation +1 max HP and +1% speed, Core Forming +2 max HP
    protected void calculateRealmBonuses(Player player, PlayerStatModifications newStats) {
        ICultivatorStats cultivatorStats = CultivatorStats.getCultivatorStats(player);
        realmBonusSource = cultivatorStats.getCultivation();

        if (cultivatorStats.getCultivationType() != CultivationTypes.QI_CONDENSER) {
            return;
        }

        float hp = 0;
        float speed = 0;

        // Each previous cultivation is a stage that has been broken through
        for (CultivationType passed = realmBonusSource.getPreviousCultivation(); passed != null; passed = passed.getPreviousCultivation()) {
            if (passed instanceof FoundationEstablishmentCultivation) {
                hp += 1;
            } else if (passed instanceof QiCondenserCultivation) {
                hp += 1;
                speed += 0.01f * StatIDs.defaultMovementSpeed;
            } else if (passed instanceof CoreFormingCultivation) {
                hp += 2;
            }
        }

        PlayerStatModifications bonuses = new PlayerStatModifications();
        bonuses.setStat(StatIDs.maxHP, hp);
        bonuses.setStat(StatIDs.movementSpeed, speed);
        newStats.combine(bonuses);
    }

    // Applies additional resistances for variant elements
    protected void calculateVariantResistances(Player player, PlayerStatModifications newStats) {
        PlayerStatModifications resistances = new PlayerStatModifications();

        for (ResourceLocation element : Elements.getElements()) {
            float resist = Elements.getElement(element).resistanceModifier(player);

            resistances.setElementalStat(StatIDs.resistanceModifier, element, resist);
        }

        newStats.combine(resistances);
    }

    protected void applyStats(Player player) {
        player.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_MODIFIER_UUID);
        player.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(MOVEMENT_MODIFIER_UUID);
        player.getAttribute(Attributes.ARMOR).removeModifier(ARMOR_MODIFIER_UUID);
        player.getAttribute(Attributes.ARMOR_TOUGHNESS).removeModifier(ARMOR_TOUGHNESS_MODIFIER_UUID);
        player.getAttribute(ForgeMod.SWIM_SPEED.get()).removeModifier(SWIM_MODIFIER_UUID);
        player.getAttribute(ForgeMod.REACH_DISTANCE.get()).removeModifier(RANGE_MODIFIER_UUID);
        player.getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(ATTACK_MODIFIER_UUID);
        player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get()).removeModifier(STEP_HEIGHT_MODIFIER_UUID);

        // Permanent so max health is saved with the player and health above 20 survives a relog
        player.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new AttributeModifier(HEALTH_MODIFIER_UUID, "BodyForgeHealth", stats.getStat(StatIDs.maxHP) - StatIDs.defaultMaxHP, AttributeModifier.Operation.ADDITION));
        player.getAttribute(Attributes.MOVEMENT_SPEED).addTransientModifier(new AttributeModifier(MOVEMENT_MODIFIER_UUID, "BodyForgeMove", stats.getStat(StatIDs.movementSpeed) * getLegWeightModifier() - StatIDs.defaultMovementSpeed, AttributeModifier.Operation.ADDITION));
        player.getAttribute(Attributes.ARMOR).addTransientModifier(new AttributeModifier(ARMOR_MODIFIER_UUID, "BodyForgeArmor", stats.getStat(StatIDs.armor), AttributeModifier.Operation.ADDITION));
        player.getAttribute(Attributes.ARMOR_TOUGHNESS).addTransientModifier(new AttributeModifier(ARMOR_TOUGHNESS_MODIFIER_UUID, "BodyForgeArmorToughness", stats.getStat(StatIDs.armorToughness), AttributeModifier.Operation.ADDITION));
        player.getAttribute(ForgeMod.SWIM_SPEED.get()).addTransientModifier(new AttributeModifier(SWIM_MODIFIER_UUID, "BodyForgeSwimSpeed", stats.getStat(StatIDs.swimSpeed) - StatIDs.defaultSwimSpeed, AttributeModifier.Operation.ADDITION));
        player.getAttribute(ForgeMod.REACH_DISTANCE.get()).addTransientModifier(new AttributeModifier(RANGE_MODIFIER_UUID, "BodyForgeReach", stats.getStat(StatIDs.attackRange) - StatIDs.defaultAttackRange, AttributeModifier.Operation.ADDITION));
        player.getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(new AttributeModifier(ATTACK_MODIFIER_UUID, "BodyForgeAttack", stats.getStat(StatIDs.armAttackModifier) - StatIDs.defaultAttackModifier, AttributeModifier.Operation.MULTIPLY_TOTAL));
        player.getAttribute(ForgeMod.STEP_HEIGHT_ADDITION.get()).addTransientModifier(new AttributeModifier(STEP_HEIGHT_MODIFIER_UUID, "BodyForgeStepHeight", stats.getStat(StatIDs.stepHeight), AttributeModifier.Operation.ADDITION));

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // Returns a modifier based on how overweight the player is
    public float getFlightWeightModifier() {
        float support = stats.getStat(StatIDs.wingSupport);
        float weight = stats.getStat(StatIDs.weight);

        // No modifier if the players legSupport is not less than the players weight
        if (support >= weight) {
            return 1;
        }

        // Return 0.1 if weight is (just under) double or more than the leg support
        return Math.max(0.1f, 2 - weight / support);
    }

    // Returns a modifier based on how overweight the player is
    public float getLegWeightModifier() {
        float support = stats.getStat(StatIDs.legSupport);
        float weight = stats.getStat(StatIDs.weight);

        // No modifier if the players legSupport is not less than the players weight
        if (support >= weight) {
            return 1;
        }

        // Return 0.1 if weight is (just under) double or more than the leg support
        return Math.max(0.1f, 2 - weight / support);
    }
}
