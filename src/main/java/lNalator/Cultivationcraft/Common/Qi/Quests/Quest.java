package lNalator.Cultivationcraft.Common.Qi.Quests;

import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class Quest {

    public static final ResourceLocation QI_SOURCE_MEDITATION = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.qisource");
    public static final ResourceLocation DAMAGE_TAKEN = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.damagetaken");
    public static final ResourceLocation DAMAGE_DEALT = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.damagedealt");
    public static final ResourceLocation DAMAGE_RESISTED = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.damageresisted");
    public static final ResourceLocation DAMAGE_ABSORBED = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.damageabsorbed");
    public static final ResourceLocation EXPLOSION_DAMAGE_TAKEN = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.explosiondamagetaken");
    public static final ResourceLocation EXPLOSION_DAMAGE_DEALT = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.explosiondamagedealt");
    public static final ResourceLocation ELEMENTALY_EFFECTED = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.elementalyeffected");
    public static final ResourceLocation ELEMENTAL_EFFECT_APPLIED = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.elementaleffectapplied");
    public static final ResourceLocation TIME_ALIVE = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.alive");
    public static final ResourceLocation LIVE = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.live");
    public static final ResourceLocation BOUNCE = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.bounce");
    public static final ResourceLocation GROWPLANT = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.growplant");
    public static final ResourceLocation GROW = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.grow");
    public static final ResourceLocation FLIGHT = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.fly");
    public static final ResourceLocation WALK = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.walk");
    public static final ResourceLocation SWIM = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.swim");
    public static final ResourceLocation JUMP = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.jump");
    public static final ResourceLocation HEAL = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.heal");
    public static final ResourceLocation DRAIN_STAMINA = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.drainstamina");
    public static final ResourceLocation QI_OVERFLOW = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.qioverflow");
    public static final ResourceLocation AURA_HIT = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.aurahit");
    public static final ResourceLocation FREEZE_ENTITY = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.freeze");
    public static final ResourceLocation EXTINGUISH = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.extinguish");
    public static final ResourceLocation LIGHTNING_CHAIN = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.lightningchain");
    public static final ResourceLocation ELEMENTAL_ITEM = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.elementalitem");
    public static final ResourceLocation HIT_STREAK = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.hitstreak");
    public static final ResourceLocation ECHO_HIT = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.echohit");
    public static final ResourceLocation POISON_MEDITATION = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.poisonmeditation");
    public static final ResourceLocation POISON_APPLIED = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.poisonapplied");
    public static final ResourceLocation TECHNIQUE_STREAK = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.techniquestreak");
    public static final ResourceLocation TECHNIQUE_USED = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.techniqueused");
    public static final ResourceLocation BIG_HIT = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.bighit");
    public static final ResourceLocation FIELD_MOVED = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.fieldmoved");
    public static final ResourceLocation KNOCKBACK_TAKEN = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.knockbacktaken");
    public static final ResourceLocation CRAWL = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.crawl");
    public static final ResourceLocation SLOW_FALL = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.slowfall");
    public static final ResourceLocation ENDER_TELEPORT = new ResourceLocation(Cultivationcraft.MODID, "cultivationcraft.quest.endertp");

    public final ResourceLocation mode;
    public final double complete;
    public final String extra;

    public Quest(ResourceLocation questMode, double questMax) {
        mode = questMode;
        complete = questMax;
        extra = null;
    }

    public Quest(ResourceLocation questMode, double questMax, String extraRequirement) {
        mode = questMode;
        complete = questMax;
        extra = extraRequirement;
    }

    public double progress(ResourceLocation progressMode, double amount, String extraRequirement) {
        // Damage events report generic and elemental progress separately. Match the
        // optional requirement exactly so generic quests only count the hit once.
        if (mode.equals(progressMode) && Objects.equals(extra, extraRequirement)) {
            return amount;
        }

        return 0;
    }

    public String getDescription() {
        String description = Component.translatable(mode.getPath()).getString();

        if (extra != null) {
            description = extra + " " + description;
        }

        return description;
    }

    public CompoundTag writeNBT() {
        CompoundTag nbt = new CompoundTag();

        nbt.putString("mode", mode.toString());
        nbt.putDouble("complete", complete);

        if (extra != null) {
            nbt.putString("extra", extra);
        }

        return nbt;
    }

    public static Quest readNBT(CompoundTag nbt) {
        if (nbt.contains("extra")) {
            return new Quest(new ResourceLocation(nbt.getString("mode")), nbt.getDouble("complete"), nbt.getString("extra"));
        }

        return new Quest(new ResourceLocation(nbt.getString("mode")), nbt.getDouble("complete"));
    }
}
