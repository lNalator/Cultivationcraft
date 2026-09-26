package DaoOfModding.Cultivationcraft.Common.Alchemy;

import DaoOfModding.Cultivationcraft.Cultivationcraft;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** A damaging burst, deliberately independent of vanilla's block-damaging explosion path. */
public final class AlchemyFailureExplosion {
    private record Profile(double radius, float nearbyDamage, float refinerDamage, double knockback) {}
    private record Settings(boolean enabled, double edgeDamageMultiplier, Map<Integer, Profile> profiles) {}
    private static final Settings DEFAULTS = new Settings(true, .35, Map.of(
            1, new Profile(2.5, 2, 3, .15),
            2, new Profile(3.5, 3, 4.5f, .22),
            3, new Profile(4.5, 4, 6, .30),
            4, new Profile(6, 6, 9, .40)));
    private static Settings settings = DEFAULTS;

    public static void burst(ServerLevel level, BlockPos cauldron, int complexity, @Nullable Player refiner) {
        Settings current = settings;
        if (!current.enabled) return;
        Profile profile = current.profiles.get(Mth.clamp(complexity, 1, 4));
        // Just above the rim, so the cauldron itself doesn't shield the whole burst.
        Vec3 center = Vec3.atCenterOf(cauldron).add(0, .55, 0);
        int motes = Math.max(4, (int) Math.ceil(profile.radius * 3));
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, motes,
                profile.radius * .2, profile.radius * .1, profile.radius * .2, .035);
        // A widening ring makes the larger complexity profiles visibly distinct.
        for (int i = 0; i < motes; i++) {
            double angle = Math.PI * 2 * i / motes;
            level.sendParticles(ParticleTypes.POOF, center.x + Math.cos(angle) * profile.radius * .45,
                    center.y, center.z + Math.sin(angle) * profile.radius * .45, 1, .04, .04, .04, .025);
        }
        level.playSound(null, center.x, center.y, center.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                (float) Math.min(1.5, .55 + profile.radius * .1), .95f + level.random.nextFloat() * .15f);

        DamageSource source = DamageSource.explosion((LivingEntity) null);
        AABB area = new AABB(center, center).inflate(profile.radius);
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, area,
                target -> target.isAlive() && !target.isSpectator())) {
            if (entity == refiner) continue; // Apply the refiner's single, stronger hit below.
            Vec3 body = entity.getBoundingBox().getCenter();
            double distance = body.distanceTo(center);
            if (distance >= profile.radius) continue;
            double exposure = Explosion.getSeenPercent(center, entity);
            double falloff = Mth.lerp(distance / profile.radius, 1, current.edgeDamageMultiplier) * exposure;
            hurt(entity, source, (float) (profile.nearbyDamage * falloff), profile.knockback * falloff, center);
        }
        // The most recent Qi supplier receives channel backlash, even outside splash range.
        // Batch completion requires recent Qi input, and the refiner must still be in this level.
        if (refiner != null && refiner.level == level && refiner.isAlive() && !refiner.isSpectator()) {
            double push = refiner.distanceToSqr(center) < profile.radius * profile.radius ? profile.knockback : 0;
            hurt(refiner, source, profile.refinerDamage, push, center);
        }
    }

    private static void hurt(LivingEntity entity, DamageSource source, float damage, double knockback, Vec3 center) {
        if (damage <= 0 || !entity.hurt(source, damage) || knockback <= 0) return;
        entity.knockback(knockback, center.x - entity.getX(), center.z - entity.getZ());
        entity.hurtMarked = true;
    }

    public static final class Loader extends SimpleJsonResourceReloadListener {
        private static final ResourceLocation SETTINGS = new ResourceLocation(Cultivationcraft.MODID, "settings");
        public Loader() { super(new Gson(), "alchemy/failure_explosions"); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
            if (!files.containsKey(SETTINGS)) { settings = DEFAULTS; return; }
            JsonObject json = files.get(SETTINGS).getAsJsonObject();
            if (!json.has("enabled") || !json.get("enabled").isJsonPrimitive()
                    || !json.getAsJsonPrimitive("enabled").isBoolean())
                throw new JsonParseException("Alchemy explosion enabled must be a boolean");
            double edge = number(json, "edge_damage_multiplier", 0, 1);
            if (!json.has("complexities") || !json.get("complexities").isJsonObject())
                throw new JsonParseException("Missing alchemy explosion complexities object");
            JsonObject profiles = json.getAsJsonObject("complexities");
            Map<Integer, Profile> loaded = new HashMap<>();
            for (int complexity = 1; complexity <= 4; complexity++) {
                JsonObject profile = profiles.getAsJsonObject(Integer.toString(complexity));
                if (profile == null) throw new JsonParseException("Missing alchemy explosion complexity " + complexity);
                double radius = number(profile, "radius", .1, 32);
                float damage = (float) number(profile, "nearby_damage", 0, 1000);
                float refiner = (float) number(profile, "refiner_damage", damage, 1000);
                double knockback = number(profile, "knockback", 0, 2);
                loaded.put(complexity, new Profile(radius, damage, refiner, knockback));
            }
            // Publish only after the whole configuration has been validated.
            settings = new Settings(json.get("enabled").getAsBoolean(), edge, Map.copyOf(loaded));
        }
        private static double number(JsonObject object, String key, double min, double max) {
            if (object == null || !object.has(key)) throw new JsonParseException("Missing alchemy explosion setting: " + key);
            double value = object.get(key).getAsDouble();
            if (!Double.isFinite(value) || value < min || value > max)
                throw new JsonParseException("Alchemy explosion " + key + " must be between " + min + " and " + max);
            return value;
        }
    }
}
