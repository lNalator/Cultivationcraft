package lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques;

import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.ICultivatorStats;
import lNalator.Cultivationcraft.Common.PlayerUtils;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.QiProjectile;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.TechniqueModifier;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.ChargeModifier;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultTechniqueStatIDs;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.TechniqueStatModification;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;


public class QiEmission extends Technique {

    public static final ResourceLocation amount = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.tstat.amount");

    public QiEmission() {
        super();

        langLocation = "cultivationcraft.technique.emission";
        Element = Elements.noElement;

        type = useType.Tap;
        multiple = false;

        icon = new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/icons/emission.png");

        canLevel = true;

        addMinTechniqueStat(DefaultTechniqueStatIDs.qiCost, 0.1);

        TechniqueStatModification qiCostModification = new TechniqueStatModification(DefaultTechniqueStatIDs.qiCost);
        TechniqueStatModification qiDamageModification = new TechniqueStatModification(DefaultTechniqueStatIDs.damage);
        TechniqueStatModification qiSpeedModification = new TechniqueStatModification(DefaultTechniqueStatIDs.movementSpeed);
        TechniqueStatModification lifetimeModification = new TechniqueStatModification(DefaultTechniqueStatIDs.lifetime);
        TechniqueStatModification amountModification = new TechniqueStatModification(amount);

        qiCostModification.addStatChange(DefaultTechniqueStatIDs.qiCost, -0.01);

        qiDamageModification.addStatChange(DefaultTechniqueStatIDs.damage, 0.01);
        qiDamageModification.addStatChange(DefaultTechniqueStatIDs.qiCost, 0.02);
        qiSpeedModification.addStatChange(DefaultTechniqueStatIDs.movementSpeed, 0.01);
        qiSpeedModification.addStatChange(DefaultTechniqueStatIDs.qiCost, 0.01);
        amountModification.addStatChange(amount, 0.005);
        amountModification.addStatChange(DefaultTechniqueStatIDs.qiCost, 0.01);
        lifetimeModification.addStatChange(DefaultTechniqueStatIDs.lifetime, 0.01);
        lifetimeModification.addStatChange(DefaultTechniqueStatIDs.qiCost, 0.01);

        addTechniqueStat(DefaultTechniqueStatIDs.qiCost, 10, qiCostModification);
        addTechniqueStat(DefaultTechniqueStatIDs.damage, 5, qiDamageModification);
        addTechniqueStat(DefaultTechniqueStatIDs.movementSpeed, 0.25, qiSpeedModification);
        addTechniqueStat(DefaultTechniqueStatIDs.lifetime, 1, lifetimeModification);
        addTechniqueStat(amount, 1, amountModification);
    }

    // Charge concept: holding the key charges the shot, released to fire. -1 when not charging
    protected int chargeStart = -1;
    protected float charge = 1;

    @Override
    public void useKeyPressed(boolean keyDown, Player player) {
        TechniqueModifier chargeCore = CultivatorStats.getCultivatorStats(player).getCultivation().getModifiers().stream()
                .filter(mod -> mod instanceof ChargeModifier).findFirst().orElse(null);

        if (chargeCore == null || cooldownCount > 0) {
            charge = 1;
            super.useKeyPressed(keyDown, player);
            return;
        }

        if (keyDown) {
            chargeStart = player.tickCount;
            return;
        }

        if (chargeStart >= 0) {
            float progress = Math.min(1, (player.tickCount - chargeStart) / (float) ChargeModifier.CHARGE_TICKS);
            charge = 1 + (((ChargeModifier) chargeCore).getMaxCharge(player) - 1) * progress;
            chargeStart = -1;

            activate(player);
            charge = 1;
        }
    }

    public boolean isValid(Player player) {
        ICultivatorStats stats = CultivatorStats.getCultivatorStats(player);

        if (stats.getCultivationType() == CultivationTypes.QI_CONDENSER) {
            return true;
        }

        return false;
    }

    public void activate(Player player) {
        super.activate(player);

        if (!player.level().isClientSide) {
            int number = (int) getTechniqueStat(amount, player);

            if (number == 1) {
                spawnProjectile(player, new Vec3(0, 0, 0)); 
            }else {
                spawnInCircle(player, number);
            }
        }

        deactivate(player);
    }

    // Spawn projectiles evenly in a circle infront of the player
    protected void spawnInCircle(Player player, int number) {
        float angleInterval = 360f / number;
        float distanceBetween = 0.5f + number / 10;

        for (int i = 0; i < number; i++) {
            float x = (float) (distanceBetween * Math.cos(Math.toRadians(angleInterval * i)));
            float y = (float) (distanceBetween * Math.sin(Math.toRadians(angleInterval * i)));

            spawnProjectile(player, new Vec3(x, y, 0).yRot((float) Math.toRadians(180 - player.getYHeadRot())));
        }
    }

    protected void spawnProjectile(Player player, Vec3 positionModifier) {
        // Try to consume Qi for this projectile, doing nothing if there is not enough Qi to do so
        // Happens once per projectile spawned
        if (!spendExternalQi(player, charge)) {
            return;
        }

        Vec3 pos = PlayerUtils.getPosition(player).add(positionModifier);
        float tempo = 1;
        for (TechniqueModifier mod : CultivatorStats.getCultivatorStats(player).getCultivation().getModifiers()) {
            tempo *= mod.getTempoMultiplier(player);
        }

        Entity projectile = new QiProjectile(player.level(), player, pos.x, pos.y, pos.z, getElement(), (int) (getTechniqueStat(DefaultTechniqueStatIDs.damage, player) * charge), (float) getTechniqueStat(DefaultTechniqueStatIDs.movementSpeed, player) * tempo, this, (float) getTechniqueStat(DefaultTechniqueStatIDs.lifetime, player));
        ((QiProjectile) projectile).setDirection(player.getLookAngle());

        for (TechniqueModifier mod : CultivatorStats.getCultivatorStats(player).getCultivation().getModifiers()) {
            projectile = mod.QiProjectileSpawnOverride(player, projectile);
        }

        player.level().addFreshEntity(projectile);
    }
}
