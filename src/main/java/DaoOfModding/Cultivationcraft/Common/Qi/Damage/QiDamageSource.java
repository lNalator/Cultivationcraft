package DaoOfModding.Cultivationcraft.Common.Qi.Damage;

import DaoOfModding.Cultivationcraft.Common.Qi.Elements.Elements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class QiDamageSource extends DamageSource
{
    public final ResourceLocation damageElement;
    // Internal damage bypasses armor, external does not
    protected boolean internal = false;

    protected Entity entity;
    protected Vec3 sourcePos;

    public QiDamageSource(String msgId, ResourceLocation element)
    {
        super(msgId);

        damageElement = element;

        // Bypass armor if this damage source has an element
        if (damageElement.compareTo(Elements.noElement) != 0)
            bypassArmor();
    }

    public QiDamageSource(DamageSource source)
    {
        super(source.msgId);

        ResourceLocation element = Elements.noElement;

        DamageSourceToDamageSource(this, source);

        if (source.isExplosion() || source.isFire())
            element = Elements.fireElement;

        if (getMsgId().compareTo(DamageSource.DRAGON_BREATH.getMsgId()) == 0)
            element = Elements.fireElement;
        else if (getMsgId().compareTo(DamageSource.LIGHTNING_BOLT.getMsgId()) == 0)
            element = Elements.lightningElement;
        else if (getMsgId().compareTo(DamageSource.CACTUS.getMsgId()) == 0)
            element = Elements.woodElement;
        else if (getMsgId().compareTo(DamageSource.SWEET_BERRY_BUSH.getMsgId()) == 0)
            element = Elements.woodElement;
        else if (getMsgId().compareTo(DamageSource.FREEZE.getMsgId()) == 0)
            element = Elements.iceElement;
        else if (getMsgId().compareTo(DamageSource.FALLING_STALACTITE.getMsgId()) == 0)
            element = Elements.earthElement;
        else if (getMsgId().compareTo(DamageSource.STALAGMITE.getMsgId()) == 0)
            element = Elements.earthElement;
        else if (getMsgId().compareTo(DamageSource.STARVE.getMsgId()) == 0)
            setInternal();
        else if (getMsgId().compareTo(DamageSource.DROWN.getMsgId()) == 0)
            setInternal();
        else if (getMsgId().compareTo(DamageSource.IN_WALL.getMsgId()) == 0)
            setInternal();

        damageElement = element;

        // Bypass armor if this damage source has an element
        if (damageElement.compareTo(Elements.noElement) != 0)
            bypassArmor();
    }

    public ResourceLocation getElement()
    {
        return damageElement;
    }

    public QiDamageSource setInternal()
    {
        internal = true;

        this.bypassArmor();
        this.bypassEnchantments();
        this.bypassMagic();

        return this;
    }

    public boolean isInternal()
    {
        return internal;
    }

    public static void DamageSourceToDamageSource(QiDamageSource to, DamageSource from)
    {
        if (from.isBypassArmor())
            to.bypassArmor();

        if (from.isExplosion())
            to.setExplosion();

        if (from.isFire())
            to.setIsFire();

        if (from.isFall())
            to.setIsFall();

        if (from.isBypassInvul())
            to.bypassInvul();

        if (from.isBypassEnchantments())
            to.bypassEnchantments();

        if (from.isBypassMagic())
            to.bypassMagic();

        if (from.isDamageHelmet())
            to.damageHelmet();

        if (from.isMagic())
            to.setMagic();

        if (from.isNoAggro())
            to.setNoAggro();

        if (from.isProjectile())
            to.setProjectile();

        to.entity = from.getEntity();
        to.sourcePos = from.getSourcePosition();
    }

    @Override
    public Vec3 getSourcePosition()
    {
        return sourcePos;
    }

    public Entity getEntity()
    {
        return entity;
    }
}