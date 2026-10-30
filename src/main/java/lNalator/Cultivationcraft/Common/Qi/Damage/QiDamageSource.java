package lNalator.Cultivationcraft.Common.Qi.Damage;

import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

public class QiDamageSource extends DamageSource {

    // Damage type of every Qi attack (data/cultivationcraft/damage_type/qi.json). The real message id and the
    // per-instance flags (bypass armor, internal...) are kept on the instance, as they were before 1.19.4
    public static final ResourceKey<DamageType> QI = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(Cultivationcraft.MODID, "qi"));

    public final ResourceLocation damageElement;
    // Internal damage bypasses armor, external does not
    protected boolean internal = false;

    protected final String msgId;
    // Damage type tags added to this instance on top of the ones of its damage type
    protected final Set<TagKey<DamageType>> extraTags = new HashSet<>();

    protected boolean doStatusEffect = true;

    public QiDamageSource(Level level, String msgId, ResourceLocation element, Boolean statusEffect) {
        super(qiType(level));
        this.msgId = msgId;

        damageElement = element;

        // Bypass armor if this damage source has an element
        if (damageElement != null || damageElement.compareTo(Elements.noElement) != 0) {
            extraTags.add(DamageTypeTags.BYPASSES_ARMOR);
        }

        doStatusEffect = statusEffect;
    }

    public QiDamageSource(Level level, String msgId, Entity attacker, ResourceLocation element, Boolean statusEffect) {
        super(qiType(level), attacker);
        this.msgId = msgId;
        damageElement = element;
        doStatusEffect = statusEffect;
    }

    public static QiDamageSource playerAttack(Player player, ResourceLocation element, Boolean statusEffect) {
        return new QiDamageSource(player.level(), "player", player, element, statusEffect);
    }

    public static QiDamageSource playerAttack(Player player, ResourceLocation element, String source, Boolean statusEffect) {
        return new QiDamageSource(player.level(), source, player, element, statusEffect);
    }

    // Wraps a vanilla damage source: keeps its damage type (so its tags) and message id
    public QiDamageSource(DamageSource source) {
        super(source.typeHolder(), source.getDirectEntity(), source.getEntity(), source.sourcePositionRaw());
        msgId = source.getMsgId();

        ResourceLocation element = Elements.noElement;

        if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(DamageTypeTags.IS_FIRE)) {
            element = Elements.fireElement;
        }

        if (source.is(DamageTypeTags.IS_FIRE)) {
            doStatusEffect = false;
        }

        if (source.is(DamageTypes.DRAGON_BREATH)) {
            element = Elements.fireElement;
        }else if (source.is(DamageTypes.LIGHTNING_BOLT)) {
            element = Elements.lightningElement;
        }else if (source.is(DamageTypes.CACTUS)) {
            element = Elements.woodElement;
        }else if (source.is(DamageTypes.SWEET_BERRY_BUSH)) {
            element = Elements.woodElement;
        }else if (source.is(DamageTypes.FREEZE)) {
            element = Elements.iceElement;
        }else if (source.is(DamageTypes.FALLING_STALACTITE)) {
            element = Elements.earthElement;
        }else if (source.is(DamageTypes.STALAGMITE)) {
            element = Elements.earthElement;
        }else if (source.is(DamageTypes.STARVE)) {
            setInternal();
        }else if (source.is(DamageTypes.DROWN)) {
            setInternal();
        }else if (source.is(DamageTypes.IN_WALL)) {
            setInternal();
        }

        damageElement = element;

        // Bypass armor if this damage source has an element
        if (damageElement.compareTo(Elements.noElement) != 0) {
            extraTags.add(DamageTypeTags.BYPASSES_ARMOR);
        }
    }

    protected static Holder<DamageType> qiType(Level level) {
        return level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(QI);
    }

    public ResourceLocation getElement() {
        return damageElement;
    }

    public QiDamageSource setInternal() {
        internal = true;

        extraTags.add(DamageTypeTags.BYPASSES_ARMOR);
        extraTags.add(DamageTypeTags.BYPASSES_ENCHANTMENTS);
        extraTags.add(DamageTypeTags.BYPASSES_EFFECTS);

        return this;
    }

    public boolean isInternal() {
        return internal;
    }

    public boolean doStatusEffect() {
        return doStatusEffect;
    }

    @Override
    public boolean is(TagKey<DamageType> tag) {
        return extraTags.contains(tag) || super.is(tag);
    }

    @Override
    public String getMsgId() {
        return msgId;
    }

    @Override
    public float getFoodExhaustion() {
        // Bypassing armor used to remove the exhaustion
        return extraTags.contains(DamageTypeTags.BYPASSES_ARMOR) ? 0 : super.getFoodExhaustion();
    }

    // Same as vanilla, but with this instance's message id: keeps the death.attack.<msgId> keys
    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        String key = "death.attack." + msgId;

        if (getEntity() == null && getDirectEntity() == null) {
            LivingEntity killer = victim.getKillCredit();
            return killer != null ? Component.translatable(key + ".player", victim.getDisplayName(), killer.getDisplayName()) : Component.translatable(key, victim.getDisplayName());
        }

        Component attacker = getEntity() == null ? getDirectEntity().getDisplayName() : getEntity().getDisplayName();
        ItemStack item = getEntity() instanceof LivingEntity living ? living.getMainHandItem() : ItemStack.EMPTY;
        return !item.isEmpty() && item.hasCustomHoverName() ? Component.translatable(key + ".item", victim.getDisplayName(), attacker, item.getDisplayName()) : Component.translatable(key, victim.getDisplayName(), attacker);
    }

    @Override
    public Vec3 getSourcePosition() {
        Vec3 pos = super.getSourcePosition();

        if (pos == null && getEntity() != null) {
            pos = getEntity().position();
        }

        return pos;
    }
}
