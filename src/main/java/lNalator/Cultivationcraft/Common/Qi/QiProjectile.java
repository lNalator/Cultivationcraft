package lNalator.Cultivationcraft.Common.Qi;

import lNalator.Cultivationcraft.Client.Renderers.QiSourceRenderer;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques.QiEmission;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.ChargeModifier;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers.TechniqueModifier;
import lNalator.Cultivationcraft.Common.Register;
import lNalator.Cultivationcraft.Server.CultivatorAttackLogic;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public class QiProjectile extends AbstractHurtingProjectile {

    private static final EntityDataAccessor<Float> ID_SPEED = SynchedEntityData.defineId(QiProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ID_LIFE = SynchedEntityData.defineId(QiProjectile.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> ID_ELEMENT = SynchedEntityData.defineId(QiProjectile.class, EntityDataSerializers.STRING);

    int damage = 0;
    QiEmission tech;
    // Charge concept: how much this shot was charged (1 = not charged), for the Echo + Charge synergy. Server only
    float charge = 1;
    // Elasticity concept: bounces on blocks so far. Server only
    int bounces = 0;

    public QiProjectile(EntityType<? extends QiProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public QiProjectile(Level level, LivingEntity owner, double x, double y, double z, ResourceLocation projectileElement, int damageOnHit, float movementSpeed, QiEmission source, float lifetime) {
        super(Register.QIPROJECTILE.get(), owner, x, y, z, level);

        this.setPos(x, y, z);

        damage = damageOnHit;

        entityData.set(ID_ELEMENT, projectileElement.toString());
        entityData.set(ID_SPEED, movementSpeed / 2f);
        entityData.set(ID_LIFE, lifetime * 20);

        tech = source;
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(ID_ELEMENT, Elements.noElement.toString());
        entityData.define(ID_SPEED, 0f);
        entityData.define(ID_LIFE, 0f);
    }

    public int getBounces() {
        return bounces;
    }

    public void addBounce() {
        bounces++;
    }

    // Explosions (its own, with Explosion + Elasticity) must not push it: it kept gaining speed at every bounce
    @Override
    public boolean ignoreExplosion() {
        return true;
    }

    public void setCharge(float charge) {
        this.charge = charge;
    }

    public void scaleLife(float multiplier) {
        entityData.set(ID_LIFE, entityData.get(ID_LIFE) * multiplier);
    }

    public float getAlpha() {
        if (entityData.get(ID_LIFE) > 20) {
            return 0.8f;
        }

        return (entityData.get(ID_LIFE) / 20f) * 0.8f;
    }

    public void tick() {
        if (getOwner() == null) {
            this.discard();
            return;
        }

        QiSourceRenderer.element = Elements.getElement(getElement());
        QiSourceRenderer.qisource = null;
        QiSourceRenderer.target = null;

        // Apply technique modifier movement modifiers to this projectile
        Vec3 move = getDeltaMovement();

        if (getOwner() instanceof Player) {
            for (TechniqueModifier mod : CultivatorStats.getCultivatorStats((Player) getOwner()).getCultivation().getModifiers()) {
                move = mod.modifyMovement(move);
            }
        }

        setDeltaMovement(move);

        super.tick();

        if (!level.isClientSide) {
            entityData.set(ID_LIFE, entityData.get(ID_LIFE) - 1);
            Technique.tickTechniqueModifiers((Player) getOwner(), position(), getElement());
        }

        // <= : a scaled lifetime (Levitation, Elasticity) is not a whole number, so it never hit exactly 0 and the projectile never died
        if (entityData.get(ID_LIFE) <= 0) {
            this.discard();
        }

    }

    public void setDirection(Vec3 dir) {
        dir = dir.scale(entityData.get(ID_SPEED));

        setDeltaMovement(dir);

        dir = dir.scale(1 / 10f);
        xPower = dir.x;
        yPower = dir.y;
        zPower = dir.z;
    }

    public ResourceLocation getElement() {
        return new ResourceLocation(entityData.get(ID_ELEMENT));
    }

    public float getSize() {
        float size = 1;
        for (TechniqueModifier mod : CultivatorStats.getCultivatorStats((Player) getOwner()).getCultivation().getModifiers()) {
            size *= mod.getItemSize();
        }

        return size;
    }

    protected void onHitEntity(EntityHitResult hit) {
        // Do nothing if this is hitting its owner or another projectile
        if (this.ownedBy(hit.getEntity()) || hit.getEntity() instanceof QiProjectile || !hit.getEntity().isAlive()) {
            return;
        }

        if (getOwner() == null) {
            this.discard();
            return;
        }

        // The modifiers can tell a charged shot from a plain one (Echo + Charge synergy)
        ChargeModifier.hitCharge = charge;

        try {
            for (TechniqueModifier mod : CultivatorStats.getCultivatorStats((Player) getOwner()).getCultivation().getModifiers()) {
                mod.onHitEntity((Player) getOwner(), hit.getEntity(), damage, getElement(), position());
                damage *= mod.getDamageMultiplier((Player) getOwner(), new QiEmission());
            }
        } finally {
            ChargeModifier.hitCharge = 1;
        }

        CultivatorAttackLogic.attackEntity((Player) getOwner(), hit.getEntity(), -1, damage, null, getElement(), "QiProjectile");

        if (tech != null) {
            tech.levelUp((Player) getOwner(), damage);
        }

        this.discard();
    }

    protected void onHitBlock(BlockHitResult hit) {
        if (getOwner() == null) {
            this.discard();
            return;
        }

        boolean survive = false;

        for (TechniqueModifier mod : CultivatorStats.getCultivatorStats((Player) getOwner()).getCultivation().getModifiers()) {
            if (mod.onHitBlock((Player) getOwner(), hit, damage, getElement(), this)) {
                survive = true;
            }
        }

        BlockState blockstate = this.level.getBlockState(hit.getBlockPos());
        blockstate.onProjectileHit(this.level, blockstate, hit, this);

        Elements.getElement(getElement()).effectBlock(this.level, hit.getBlockPos(), damage);

        // Destroy this projectile if it hits a block
        if (!survive) {
            this.discard();
        }
    }

    public boolean hurt(DamageSource p_36910_, float p_36911_) {
        return false;
    }

    protected ParticleOptions getTrailParticle() {
        return Register.qiParticleType.get();
    }

    protected boolean shouldBurn() {
        return false;
    }
}
