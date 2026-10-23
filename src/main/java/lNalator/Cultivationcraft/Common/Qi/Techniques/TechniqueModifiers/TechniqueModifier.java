package lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueModifiers;

import lNalator.Cultivationcraft.Client.GUI.animatedTexture;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.FlyingSwordEntity;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CoreFormingCultivation;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CultivationType;
import lNalator.Cultivationcraft.Common.Qi.Damage.QiDamageSource;
import lNalator.Cultivationcraft.Common.Qi.Quests.Quest;
import lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques.ItemEnhanceTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.QiCondenserTechniques.QiBarrierTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.Technique;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Network.PacketHandler;
import lNalator.mlmanimator.Client.Poses.PlayerPose;
import lNalator.mlmanimator.Client.Poses.PoseHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;

public class TechniqueModifier {

    public static final ResourceLocation ELEMENTAL_CATEGORY = new ResourceLocation(Cultivationcraft.MODID, "concept.category.elemental");
    public static final ResourceLocation MODIFIER_CATEGORY = new ResourceLocation(Cultivationcraft.MODID, "concept.category.modifier");
    public static final ResourceLocation RHYTHM_CATEGORY = new ResourceLocation(Cultivationcraft.MODID, "concept.category.rhythm");
    public static final ResourceLocation FIELD_CATEGORY = new ResourceLocation(Cultivationcraft.MODID, "concept.category.field");
    public static final ResourceLocation MASS_CATEGORY = new ResourceLocation(Cultivationcraft.MODID, "concept.category.mass");
    public static final ResourceLocation SIZE_CATEGORY = new ResourceLocation(Cultivationcraft.MODID, "concept.category.size");

    public static final int CONCEPT_QUEST_STAGE = 4;

    public ResourceLocation ID;
    public ResourceLocation CATEGORY;

    animatedTexture coreTexture = new animatedTexture(new ResourceLocation(Cultivationcraft.MODID, "textures/cores/blank.png"));
    ResourceLocation Element = null;
    float power = 1;
    boolean allowSameCategory = false;
    // Unique cores add their own effect and cannot be reinforced by choosing them again
    boolean unique = false;
    Quest unlockQuest;
    Quest stabiliseQuest;
    boolean flyingMount = false;

    Vec3 size = new Vec3(1, 1, 1);
    float itemSize = 1;

    HashMap<String, PlayerPose> poses = new HashMap<>();

    float damageMult = 1;
    float qiMult = 1;

    public TechniqueModifier() {
        ID = new ResourceLocation(Cultivationcraft.MODID, "concept.example");
        CATEGORY = new ResourceLocation(Cultivationcraft.MODID, "concept.category.example");
    }

    public ResourceLocation getID() {
        return ID;
    }

    public ResourceLocation getElement() {
        return Element;
    }

    public animatedTexture getCoreTexture() {
        return coreTexture;
    }

    public float getDamageMultiplier(Player owner, Technique tech) {
        return damageMult;
    }

    // Rhythm: speed of Qi projectiles and flying swords
    public float getTempoMultiplier(Player owner) {
        return 1;
    }

    // Rhythm: time between two aura pulses, and their damage
    public float getAuraIntervalMultiplier(Player owner) {
        return 1;
    }

    public float getAuraDamageMultiplier(Player owner) {
        return 1;
    }

    public int getRank(Player owner) {
        return CultivatorStats.getCultivatorStats(owner).getCultivation().getModifierRank(ID);
    }

    public boolean isUnique() {
        return unique;
    }

    public float getQiCostModifier(Technique tech) {
        return qiMult;
    }

    public Boolean hasElement() {
        if (getElement() != null) {
            return true;
        }

        return false;
    }

    public Boolean hasMount() {
        return flyingMount;
    }

    public BlockState getMountSource(Player owner) {
        return null;
    }

    // Every tick on both sides, for the owner of this core (the client owns the player's movement)
    public void playerTick(Player player) {
    }

    // Mass concepts only act on a real fall of over 2 blocks, not while swimming, climbing, gliding, riding or flying
    public static boolean massApplies(Player player) {
        return player.fallDistance > 2 && !player.isOnGround() && player.getDeltaMovement().y < 0 && !player.isInWater() && !player.isInLava()
                && !player.onClimbable() && !player.isFallFlying() && !player.isPassenger() && !player.getAbilities().flying;
    }

    public void tick(Player owner, Vec3 position, ResourceLocation element) {

    }

    public float getItemSize() {
        return itemSize;
    }

    public Vec3 getSize(Player owner) {
        return size;
    }

    public void doResize(Player owner, Technique tech) {
        if (tech instanceof QiBarrierTechnique) {
            PoseHandler.getPlayerPoseHandler(owner.getUUID()).resize(getSize(owner));
        } else if (tech instanceof ItemEnhanceTechnique) {
            // TODO : Release 0.2.2 version of mlmAnimator.
            // PoseHandler.getPlayerPoseHandler(owner.getUUID()).getPlayerModel().scaleItem(new Vec3(itemSize, itemSize, itemSize));
        }
    }

    public void tickClient(Player owner, Technique tech) {
        doResize(owner, tech);

        if (!poses.containsKey(tech.getClass().toString())) {
            return;
        }

        PoseHandler.addPose(owner.getUUID(), poses.get(tech.getClass().toString()));
    }

    public void onHitEntity(Player owner, Entity hit, float damage, ResourceLocation element, Vec3 pos) {
        onHitAll(owner, pos, damage, element);
    }

    // Return whether the projectile survives the hit
    public boolean onHitBlock(Player owner, BlockHitResult hit, float damage, ResourceLocation element, Entity projectile) {
        onHitAll(owner, projectile.position(), damage, element);

        return false;
    }

    public void onHitAll(Player owner, Vec3 pos, float damage, ResourceLocation element) {
    }

    public void onHitTaken(Player owner, float damage, ResourceLocation defensiveElement, QiDamageSource source) {

    }

    public Vec3 flyingSwordTargetOverride(FlyingSwordEntity sword, Entity target, Vec3 targetPos) {
        return targetPos;
    }

    public boolean flyingSwordMovementOverride(FlyingSwordEntity sword, Vec3 targetPos) {
        return false;
    }

    public Entity QiProjectileSpawnOverride(Player owner, Entity inputProjectile) {
        return inputProjectile;
    }

    public Quest getUnlockQuest() {
        return unlockQuest;
    }

    public Quest getStabaliseQuest() {
        return stabiliseQuest;
    }

    public boolean canUse(Player player) {
        CultivationType cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();

        for (TechniqueModifier modifier : cultivation.getModifiers()) {
            // An owned core can be chosen again to reinforce it, unless it is unique
            if (modifier.ID.compareTo(this.ID) == 0) {
                return !unique;
            }

            // Cannot be the same category as another active modifier if allowSameCategory is false
            if (!allowSameCategory && modifier.CATEGORY.compareTo(CATEGORY) == 0) {
                return false;
            }
        }

        return true;
    }

    // Whether this concept can be picked at a breakthrough: the first core (leaving Qi Condensation) is always the elemental one,
    // since non-elemental concepts can now be learnt during Qi Condensation
    public boolean canChooseAsCore(Player player) {
        if (!canUse(player) || !hasLearnt(player)) {
            return false;
        }

        return !(CultivatorStats.getCultivatorStats(player).getCultivation() instanceof QiCondenserCultivation) || CATEGORY.equals(ELEMENTAL_CATEGORY);
    }

    public boolean canLearn(Player player) {
        // Elements are learnt from the main element absorbed in Qi Condensation, and only one elemental core
        // is allowed: their own unlock quests could never be used, so they are disabled
        if (unlockQuest == null || CATEGORY.equals(ELEMENTAL_CATEGORY)) {
            return false;
        }

        CultivationType cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();

        // Concept quests start halfway through Qi Condensation
        if (cultivation instanceof CoreFormingCultivation) {
            return true;
        }

        return cultivation instanceof QiCondenserCultivation && cultivation.getStage() >= CONCEPT_QUEST_STAGE;
    }

    public boolean hasLearnt(Player player) {
        double progress = CultivatorStats.getCultivatorStats(player).getConceptProgress(ID);

        if (unlockQuest != null && progress >= unlockQuest.complete) {
            return true;
        }

        return false;
    }

    public Vec3 modifyMovement(Vec3 move) {
        return move;
    }

    // Instantly learn this modifier
    public void learn(Player player) {
        CultivatorStats.getCultivatorStats(player).setConceptProgress(ID, unlockQuest.complete);
        PacketHandler.sendCultivatorStatsToClient(player);
    }
}
