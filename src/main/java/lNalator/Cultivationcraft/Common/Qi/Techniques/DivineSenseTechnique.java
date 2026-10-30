package lNalator.Cultivationcraft.Common.Qi.Techniques;

import lNalator.Cultivationcraft.Common.PlayerUtils;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.BodyPartNames;
import lNalator.Cultivationcraft.Client.Renderer;
import lNalator.Cultivationcraft.Common.Capabilities.BodyModifications.BodyModifications;
import lNalator.Cultivationcraft.Common.Capabilities.BodyModifications.IBodyModifications;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.ICultivatorStats;
import lNalator.Cultivationcraft.Common.Qi.BodyParts.BodyPartOption;
import lNalator.Cultivationcraft.Common.Qi.CultivationTypes;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Stats.StatIDs;
import lNalator.Cultivationcraft.Common.Qi.Techniques.TechniqueStats.DefaultTechniqueStatIDs;
import lNalator.Cultivationcraft.Cultivationcraft;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.CultivatorTechniques;
import lNalator.Cultivationcraft.Common.Capabilities.CultivatorTechniques.ICultivatorTechniques;
import lNalator.Cultivationcraft.Network.PacketHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraft.util.Mth;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.FoundationEstablishmentCultivation;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.QiCondenserCultivation;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CoreFormingCultivation;

import java.util.HashMap;
import java.util.UUID;

public class DivineSenseTechnique extends Technique {

    public static final ResourceLocation DETECTION_RADIUS = new ResourceLocation(Cultivationcraft.MODID,
            "cultivationcraft.tstat.divinesense_radius");
    public static final ResourceLocation MAX_HIGHLIGHTS = new ResourceLocation(Cultivationcraft.MODID,
            "cultivationcraft.tstat.divinesense_max_highlights");

    public record SenseProfile(int radius, int maxHighlights, double peakOpacity) {

    }

    // What Divine Sense shows, toggled in its radial menu (see DivineSenseMenuScreen): one bit per category
    public static final int SENSE_QI_SOURCES = 1;
    public static final int SENSE_BLOCKS = 2;
    public static final int SENSE_CREATURES = 4;
    public static final int SENSE_PLAYERS = 8;
    public static final int SENSE_TRIBULATIONS = 16;
    public static final int SENSE_ALL = SENSE_QI_SOURCES | SENSE_BLOCKS | SENSE_CREATURES | SENSE_PLAYERS | SENSE_TRIBULATIONS;

    protected int senseMask = SENSE_ALL;

    // Vigilance: a sensed player feels it, at most once every 5 s per observer (see DivineSensePingPacket)
    protected static final int VIGILANCE_INTERVAL = 10;
    protected static final int PING_COOLDOWN = 100;
    // Server side: game time of the last ping sent to each player sensed
    protected final HashMap<UUID, Long> lastPings = new HashMap<>();

    public int getSenseMask() {
        return senseMask;
    }

    public boolean senses(int category) {
        return (senseMask & category) != 0;
    }

    // Client side, from the radial menu: kept on the server so it is saved with the technique
    public void setSenseMask(int mask) {
        senseMask = mask & SENSE_ALL;
        sendInfo(senseMask);
    }

    @Override
    public void processInfo(Player player, int info) {
        senseMask = info & SENSE_ALL;
    }

    @Override
    public CompoundTag writeNBT() {
        CompoundTag nbt = super.writeNBT();
        nbt.putInt("senseMask", senseMask);

        return nbt;
    }

    @Override
    public void readNBTData(CompoundTag nbt) {
        super.readNBTData(nbt);

        senseMask = nbt.contains("senseMask") ? nbt.getInt("senseMask") & SENSE_ALL : SENSE_ALL;
    }

    // The player's Divine Sense technique in their technique slots, active or not
    public static DivineSenseTechnique find(Player player) {
        ICultivatorTechniques techniques = CultivatorTechniques.getCultivatorTechniques(player);

        for (int i = 0; i < CultivatorTechniques.numberOfTechniques; i++) {
            if (techniques.getTechnique(i) instanceof DivineSenseTechnique sense && sense.isValid(player)) {
                return sense;
            }
        }

        return null;
    }

    // The player's Divine Sense when it is active, on both sides
    public static DivineSenseTechnique getActive(Player player) {
        DivineSenseTechnique sense = find(player);

        return sense != null && sense.isActive() ? sense : null;
    }

    public static boolean senses(Player player, int category) {
        DivineSenseTechnique sense = getActive(player);

        return sense != null && sense.senses(category);
    }

    // Realms compared by vigilance: Foundation Establishment < Qi Condensation < Core Forming, stages do not count
    public static int getRealm(Player player) {
        var cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();

        if (cultivation instanceof CoreFormingCultivation) {
            return 3;
        }
        if (cultivation instanceof QiCondenserCultivation) {
            return 2;
        }
        if (cultivation instanceof FoundationEstablishmentCultivation) {
            return 1;
        }
        return 0;
    }

    public static SenseProfile getSenseProfile(Player player) {
        var cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();
        int minorStage = Mth.clamp(cultivation.getStage(), 1, Math.max(1, cultivation.getMaxStage())) - 1;
        // Realm entry bonuses are deliberately larger than the gains within a realm.
        if (cultivation instanceof CoreFormingCultivation) {
            return new SenseProfile(192 + minorStage * 8, 128 + minorStage * 8, 0.94 + minorStage * 0.005);
        }
        if (cultivation instanceof QiCondenserCultivation) {
            return new SenseProfile(64 + minorStage * 4, 64 + minorStage * 4, 0.85 + minorStage * 0.01);
        }
        if (cultivation instanceof FoundationEstablishmentCultivation) {
            return new SenseProfile(16 + minorStage * 2, 30 + minorStage * 2, 0.75 + minorStage * 0.02);
        }
        return new SenseProfile(16, 30, 0.75);
    }

    @Override
    public double getTechniqueStat(ResourceLocation stat, Player player) {
        if (DETECTION_RADIUS.equals(stat)) {
            return getSenseProfile(player).radius();
        }
        if (MAX_HIGHLIGHTS.equals(stat)) {
            return getSenseProfile(player).maxHighlights();
        }
        return super.getTechniqueStat(stat, player);
    }

    public DivineSenseTechnique() {
        super();

        langLocation = "cultivationcraft.technique.divinesense";
        Element = Elements.noElement;

        type = useType.Toggle;
        multiple = false;

        icon = new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/icons/divinesense.png");
        //setOverlay(new ResourceLocation(Cultivationcraft.MODID, "textures/techniques/overlays/divinesense.png"));

        stats.setStat(StatIDs.staminaDrain, 0.05f);
        addTechniqueStat(DefaultTechniqueStatIDs.qiCost, 1f);
        addTechniqueStat(DETECTION_RADIUS, 16);
        addTechniqueStat(MAX_HIGHLIGHTS, 30);

        effects.add(MobEffects.NIGHT_VISION);
    }

    @Override
    public void tickClient(TickEvent.PlayerTickEvent event) {
        if (PlayerUtils.isClientPlayerCharacter(event.player)) {
            Renderer.QiSourcesVisible = true;
            Renderer.DivineSenseMask = senseMask;
            Renderer.QiSenseRadius = getTechniqueStat(DETECTION_RADIUS, event.player);
            Renderer.QiSenseTick = event.player.level().getGameTime();
        }

        // Resource depletion is decided on the server, not from a stale client reserve.
    }

    @Override
    public void tickServer(TickEvent.PlayerTickEvent event) {
        super.tickServer(event);

        pingSensedPlayers(event.player);

        if (CultivatorStats.getCultivatorStats(event.player).getCultivationType() == CultivationTypes.QI_CONDENSER) {
            if (!CultivatorStats.getCultivatorStats(event.player).getCultivation().consumeQi(event.player, getTechniqueStat(DefaultTechniqueStatIDs.qiCost, event.player) / 20f)) {
                this.deactivate(event.player);
                lNalator.Cultivationcraft.Network.PacketHandler.sendCultivatorTechniquesToClient(event.player);
            }
        } else if (event.player.getFoodData().getFoodLevel() == 0) {
            this.deactivate(event.player);
            lNalator.Cultivationcraft.Network.PacketHandler.sendCultivatorTechniquesToClient(event.player);
        }
    }

    // Players sensed of the same realm or above feel it. A higher realm observer goes unnoticed by lower realms,
    // as long as it stays out of their own detection radius
    protected void pingSensedPlayers(Player observer) {
        if (observer.tickCount % VIGILANCE_INTERVAL != 0 || !senses(SENSE_PLAYERS) || observer.isSpectator()) {
            return;
        }

        int radius = getSenseProfile(observer).radius();
        int realm = getRealm(observer);
        long now = observer.level().getGameTime();

        for (Player target : observer.level().players()) {
            if (target == observer || !(target instanceof ServerPlayer serverTarget) || target.isSpectator() || !target.isAlive()) {
                continue;
            }

            double distance = target.distanceTo(observer);

            if (distance > radius) {
                continue;
            }

            if (getRealm(target) < realm && distance > getSenseProfile(target).radius()) {
                continue;
            }

            Long last = lastPings.get(target.getUUID());

            if (last != null && now - last < PING_COOLDOWN) {
                continue;
            }

            lastPings.put(target.getUUID(), now);
            PacketHandler.sendDivineSensePing(serverTarget, observer);
        }
    }

    @Override
    public void deactivate(Player player) {
        super.deactivate(player);

        if (PlayerUtils.isClientPlayerCharacter(player)) {
            Renderer.QiSourcesVisible = false;
        }
    }

    @Override
    public boolean isValid(Player player) {
        ICultivatorStats stats = CultivatorStats.getCultivatorStats(player);

        if (stats.getCultivationType() == CultivationTypes.BODY_CULTIVATOR) {
            // If player is a body cultivator and has cultivated Qi Sight then return true
            IBodyModifications modifications = BodyModifications.getBodyModifications(player);

            BodyPartOption eyes = BodyPartNames.getOption(BodyPartNames.startingEyesPart);

            if (!modifications.hasOption(eyes.getPosition(), eyes.getSubPosition())) {
                return false;
            }

            if (modifications.getOption(eyes.getPosition(), eyes.getSubPosition()) == eyes) {
                return true;
            }
        } else if (stats.getCultivationType() == CultivationTypes.QI_CONDENSER) {
            return true;
        }

        return false;
    }
}
