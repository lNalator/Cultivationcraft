package lNalator.Cultivationcraft.Client.Renderers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.lwjgl.opengl.GL11;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;

import lNalator.Cultivationcraft.Common.Capabilities.CultivatorStats.CultivatorStats;
import lNalator.Cultivationcraft.Common.Blocks.Plants.ProceduralPlantBlock;
import lNalator.Cultivationcraft.Common.Blocks.Plants.entity.ProceduralPlantBlockEntity;
import lNalator.Cultivationcraft.Common.Blocks.Plants.world.ClientPlantCatalog;
import lNalator.Cultivationcraft.Common.Qi.Cultivation.CoreFormingCultivation;
import lNalator.Cultivationcraft.Common.Qi.Elements.Elements;
import lNalator.Cultivationcraft.Common.Qi.Techniques.DivineSenseTechnique;
import lNalator.Cultivationcraft.Common.Qi.Techniques.DivineSenseTechnique.SenseProfile;
import lNalator.Cultivationcraft.Cultivationcraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Tag-driven, client-only detection; never requests chunks or changes block
 * rendering. Also outlines the creatures and players in range with the glowing
 * effect, filtered by the Divine Sense radial menu (see DivineSenseTechnique.SENSE_*).
 */
@Mod.EventBusSubscriber(modid = Cultivationcraft.MODID, value = Dist.CLIENT)
public final class DivineSenseBlockRenderer {

    public static final TagKey<Block> VISIBLE_BLOCKS = TagKey.create(Registry.BLOCK_REGISTRY,
            new ResourceLocation(Cultivationcraft.MODID, "divine_sense_visible"));

    private record ScanSection(BlockPos origin, double distanceSquared) {

    }

    private record Target(BlockPos pos, double distanceSquared) {

    }

    private static final int SECTIONS_PER_TICK = 8;
    private static final List<ScanSection> pendingSections = new ArrayList<>();
    private static final PriorityQueue<Target> candidates = new PriorityQueue<>(
            Comparator.comparingDouble(Target::distanceSquared).reversed());
    private static SenseProfile scanProfile;
    private static Vec3 scanOrigin;
    private static int sectionIndex;
    private static final int SCAN_INTERVAL = 10;
    private static final List<BlockPos> nearby = new ArrayList<>();
    private static final BufferBuilder BUFFER = new BufferBuilder(32768);
    private static final BufferBuilder PLANT_BUFFER = new BufferBuilder(16384);
    private static final BufferBuilder PLAYER_BUFFER = new BufferBuilder(16384);
    private static ClientLevel scannedLevel;
    private static int scanTicks;

    // Only chooses which entities are outlined: the outlines follow them every frame
    private static final int ENTITY_SCAN_INTERVAL = 5;
    private static final List<Entity> sensedEntities = new ArrayList<>();
    // Entity id -> outline colour
    private static final Map<Integer, Integer> glowing = new HashMap<>();
    private static int entityScanTicks;

    // Hostiles in red (magenta in the Nether), other creatures in yellow
    private static final int HOSTILE_COLOR = 0xFF3030;
    private static final int NETHER_HOSTILE_COLOR = 0xFF40FF;
    private static final int CREATURE_COLOR = 0xFFE040;
    private static final int PLAYER_COLOR = 0xFFFFFF;

    // The client player's Divine Sense when it is active and they can use it, else null
    public static DivineSenseTechnique activeSense(Minecraft mc) {
        if (mc.player == null || mc.level == null || !mc.player.isAlive() || mc.player.isSpectator()) {
            return null;
        }
        return DivineSenseTechnique.getActive(mc.player);
    }

    private static void clearScan() {
        nearby.clear();
        pendingSections.clear();
        candidates.clear();
        scannedLevel = null;
        scanProfile = null;
        scanOrigin = null;
        sectionIndex = 0;
        scanTicks = 0;
        sensedEntities.clear();
        glowing.clear();
        entityScanTicks = 0;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        DivineSenseTechnique sense = activeSense(mc);
        if (sense == null) {
            clearScan();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        SenseProfile current = DivineSenseTechnique.getSenseProfile(mc.player);
        Vec3 origin = mc.player.getEyePosition();
        if (entityScanTicks-- <= 0) {
            scanEntities(mc, sense, current, origin);
            entityScanTicks = ENTITY_SCAN_INTERVAL - 1;
        }
        updateGlowing(mc, current, origin);
        if (!sense.senses(DivineSenseTechnique.SENSE_BLOCKS)) {
            nearby.clear();
            pendingSections.clear();
            candidates.clear();
            scanTicks = 0;
            scanProfile = null;
            return;
        }
        // Refresh immediately on a breakthrough, realm reset, teleport, or world change.
        if (scannedLevel != mc.level || !current.equals(scanProfile)
                || (scanOrigin != null && origin.distanceToSqr(scanOrigin) > 64)) {
            if (scannedLevel != mc.level) {
                clearScan();
            }
            pendingSections.clear();
            candidates.clear();
            scanTicks = 0;
            scannedLevel = mc.level;
            scanProfile = current;
        }
        if (pendingSections.isEmpty()) {
            if (scanTicks-- > 0) {
                return;
            }
            beginScan(mc, current, origin);
        }
        scanNextSections(mc);
    }

    private static void beginScan(Minecraft mc, SenseProfile current, Vec3 origin) {
        scanOrigin = origin;
        scanProfile = current;
        sectionIndex = 0;
        candidates.clear();
        int radius = current.radius();
        int minX = Mth.floor(origin.x - radius) >> 4, maxX = Mth.floor(origin.x + radius) >> 4;
        int minZ = Mth.floor(origin.z - radius) >> 4, maxZ = Mth.floor(origin.z + radius) >> 4;
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                var chunk = mc.level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }
                var sections = chunk.getSections();
                for (int index = 0; index < sections.length; index++) {
                    int y = chunk.getSectionYFromSectionIndex(index) * 16;
                    BlockPos start = new BlockPos(chunkX * 16, y, chunkZ * 16);
                    double distance = distanceSquaredToBox(origin, new AABB(start, start.offset(16, 16, 16)));
                    if (distance > radius * radius || sections[index].hasOnlyAir()
                            || !sections[index].maybeHas(state -> state.is(VISIBLE_BLOCKS))) {
                        continue;
                    }
                    pendingSections.add(new ScanSection(start, distance));
                }
            }
        }
        pendingSections.sort(Comparator.comparingDouble(ScanSection::distanceSquared));
    }

    private static void scanNextSections(Minecraft mc) {
        int processed = 0;
        while (sectionIndex < pendingSections.size() && processed++ < SECTIONS_PER_TICK) {
            ScanSection next = pendingSections.get(sectionIndex++);
            // Once enough nearby targets exist, farther sections cannot improve the selection.
            if (candidates.size() == scanProfile.maxHighlights()
                    && next.distanceSquared() > candidates.peek().distanceSquared()) {
                sectionIndex = pendingSections.size();
                break;
            }
            BlockPos start = next.origin();
            var chunk = mc.level.getChunkSource().getChunk(start.getX() >> 4, start.getZ() >> 4, ChunkStatus.FULL, false);
            if (chunk == null) {
                continue;
            }
            var section = chunk.getSection(chunk.getSectionIndex(start.getY()));
            if (!section.maybeHas(state -> state.is(VISIBLE_BLOCKS))) {
                continue;
            }
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (!section.getBlockState(x, y, z).is(VISIBLE_BLOCKS)) {
                            continue;
                        }
                        BlockPos pos = start.offset(x, y, z);
                        double distance = Vec3.atCenterOf(pos).distanceToSqr(scanOrigin);
                        if (distance >= scanProfile.radius() * scanProfile.radius()) {
                            continue;
                        }
                        if (candidates.size() < scanProfile.maxHighlights()) {
                            candidates.add(new Target(pos, distance)); 
                        }else if (distance < candidates.peek().distanceSquared()) {
                            candidates.poll();
                            candidates.add(new Target(pos, distance));
                        }
                    }
                }
            }
        }
        // Keep the last complete snapshot while scanning; partial replacements blink
        // distant targets off at the start of every refresh.
        if (sectionIndex >= pendingSections.size()) {
            nearby.clear();
            candidates.stream().sorted(Comparator.comparingDouble(Target::distanceSquared))
                    .forEach(target -> nearby.add(target.pos()));
            pendingSections.clear();
            scanTicks = SCAN_INTERVAL - 1;
        }
    }

    // The creatures and players in range, nearest first, with their own budget so ores cannot hide them
    private static void scanEntities(Minecraft mc, DivineSenseTechnique sense, SenseProfile profile, Vec3 origin) {
        sensedEntities.clear();
        boolean creatures = sense.senses(DivineSenseTechnique.SENSE_CREATURES);
        boolean players = sense.senses(DivineSenseTechnique.SENSE_PLAYERS);
        if (!creatures && !players) {
            return;
        }
        double radius = profile.radius();
        List<Entity> found = mc.level.getEntities(mc.player, new AABB(origin, origin).inflate(radius),
                entity -> entity instanceof LivingEntity && !(entity instanceof ArmorStand) && entity.isAlive()
                && !entity.isSpectator() && (entity instanceof Player ? players : creatures)
                && entity.getBoundingBox().getCenter().distanceToSqr(origin) < radius * radius);
        found.sort(Comparator.comparingDouble((Entity entity) -> entity.getBoundingBox().getCenter().distanceToSqr(origin)));
        sensedEntities.addAll(found.subList(0, Math.min(found.size(), profile.maxHighlights())));
    }

    private static double distanceSquaredToBox(Vec3 point, AABB box) {
        double x = point.x - Mth.clamp(point.x, box.minX, box.maxX);
        double y = point.y - Mth.clamp(point.y, box.minY, box.maxY);
        double z = point.z - Mth.clamp(point.z, box.minZ, box.maxZ);
        return x * x + y * y + z * z;
    }

    private static double smoothstep(double start, double end, double value) {
        double t = Mth.clamp((value - start) / (end - start), 0, 1);
        return t * t * (3 - 2 * t);
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        DivineSenseTechnique sense = activeSense(mc);
        if (sense == null || mc.options.hideGui) {
            return;
        }
        boolean blocks = sense.senses(DivineSenseTechnique.SENSE_BLOCKS) && scannedLevel == mc.level && !nearby.isEmpty();
        boolean players = sense.senses(DivineSenseTechnique.SENSE_PLAYERS) && !sensedEntities.isEmpty();
        if (!blocks && !players) {
            return;
        }
        SenseProfile current = DivineSenseTechnique.getSenseProfile(mc.player);
        var pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        Vec3 origin = mc.player.getEyePosition(event.getPartialTick());
        boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        float lineWidth = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
        var shader = RenderSystem.getShader();
        // AFTER_WEATHER already applies the view rotation to model-view. Use the
        // event's matrix exactly once, including in Fabulous graphics mode.
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.lineWidth(1);
        try {
            BUFFER.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            PLANT_BUFFER.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            PLAYER_BUFFER.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
            int highlighted = 0;
            for (BlockPos pos : blocks ? nearby : List.<BlockPos>of()) {
                if (highlighted >= current.maxHighlights()) {
                    break;
                }
                if (!mc.level.isLoaded(pos)) {
                    continue;
                }
                var state = mc.level.getBlockState(pos);
                if (!state.is(VISIBLE_BLOCKS)) {
                    continue;
                }
                double distance = Vec3.atCenterOf(pos).distanceTo(origin);
                if (distance >= current.radius()) {
                    continue;
                }
                var shape = state.getShape(mc.level, pos);
                if (shape.isEmpty()) {
                    continue;
                }
                AABB bounds = shape.bounds().move(pos).inflate(0.01);
                if (!event.getFrustum().isVisible(bounds)) {
                    continue;
                }
                // Clearer as targets approach, but almost invisible within arm's reach.
                // Use distance to the shape/camera for close fading, including third person.
                double closeFade = smoothstep(0.5, 4, Math.sqrt(distanceSquaredToBox(camera, bounds)));
                // Reach the realm's opacity ceiling three blocks inside the detection edge.
                double rangeFade = smoothstep(0, 3, current.radius() - distance);
                int alpha = (int) Math.round(255 * current.peakOpacity() * closeFade * rangeFade);
                if (alpha == 0) {
                    continue;
                }
                highlighted++;
                // Subtract in double precision before sending positions to the GPU.
                AABB relativeBounds = bounds.move(-camera.x, -camera.y, -camera.z);
                if (state.getBlock() instanceof ProceduralPlantBlock) {
                    float strength = plantStrength(mc.level, pos, state);
                    // The marker grows with the plant's Qi: 20% of the plant without Qi, its full size at the maximum
                    cross(pose.last().pose(), relativeBounds, plantColor(state, strength), alpha, Mth.lerp(strength, 0.2f, 1f));
                }else {
                    outline(BUFFER, pose.last().pose(), relativeBounds, 0x91DCCD, alpha);
                }
            }
            BufferUploader.drawWithShader(BUFFER.end());
            // Width is a draw state: use a separate batch to keep ore outlines thin.
            RenderSystem.lineWidth(2);
            BufferUploader.drawWithShader(PLANT_BUFFER.end());
            renderPlayerHitboxes(mc, event, current, camera, origin);
            BufferUploader.drawWithShader(PLAYER_BUFFER.end());
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.depthMask(depthWrite);
            if (depthTest) {
                RenderSystem.enableDepthTest(); 
            }else {
                RenderSystem.disableDepthTest();
            }
            if (blend) {
                RenderSystem.enableBlend(); 
            }else {
                RenderSystem.disableBlend();
            }
            RenderSystem.lineWidth(lineWidth);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderColor(1, 1, 1, 1);
        }
    }

    // Sensed entities and the colour of their outline, drawn by the vanilla glowing effect so it follows the model
    // (see MinecraftMixin and EntityMixin). Updated every tick
    private static void updateGlowing(Minecraft mc, SenseProfile current, Vec3 origin) {
        glowing.clear();
        for (Entity entity : sensedEntities) {
            // Players only show their hitbox (see renderPlayerHitboxes)
            if (entity instanceof Player || entity.isRemoved() || entity.level != mc.level) {
                continue;
            }
            double distance = entity.getBoundingBox().getCenter().distanceTo(origin);
            if (distance >= current.radius()) {
                continue;
            }
            glowing.put(entity.getId(), entityColor(mc.level, entity));
        }
    }

    // Players show their hitbox, not the glowing outline. A player of a higher realm is blurred: faint copies
    // of their hitbox trembling around them, further apart and fainter towards the edge of the detection radius
    private static void renderPlayerHitboxes(Minecraft mc, RenderLevelStageEvent event, SenseProfile current, Vec3 camera, Vec3 origin) {
        float partialTick = event.getPartialTick();
        var matrix = event.getPoseStack().last().pose();
        for (Entity entity : sensedEntities) {
            if (!(entity instanceof Player target) || entity.isRemoved() || entity.level != mc.level) {
                continue;
            }
            // Interpolated, so the hitbox follows the player smoothly between ticks and scans
            Vec3 position = entity.getPosition(partialTick);
            AABB bounds = entity.getBoundingBox().move(position.subtract(entity.position()));
            double distance = bounds.getCenter().distanceTo(origin);
            double blur = blur(mc.player, target, distance, current);
            if (distance >= current.radius() || !event.getFrustum().isVisible(bounds.inflate(1))) {
                continue;
            }
            double closeFade = smoothstep(0.5, 4, Math.sqrt(distanceSquaredToBox(camera, bounds)));
            double rangeFade = smoothstep(0, 3, current.radius() - distance);
            int alpha = (int) Math.round(255 * current.peakOpacity() * closeFade * rangeFade * (1 - 0.6 * blur));
            if (alpha == 0) {
                continue;
            }
            int color = entityColor(mc.level, entity);
            AABB relativeBounds = bounds.move(-camera.x, -camera.y, -camera.z);
            if (blur <= 0) {
                outline(PLAYER_BUFFER, matrix, relativeBounds, color, alpha);
                continue;
            }
            alpha /= 2;
            double time = (mc.level.getGameTime() + partialTick) * 0.3;
            for (int copy = 0; copy < 3; copy++) {
                double phase = time + copy * 2.1;
                AABB shifted = relativeBounds.move(Math.sin(phase) * blur * 0.4, Math.cos(phase * 1.3) * blur * 0.2,
                        Math.sin(phase * 0.7 + 1) * blur * 0.4).inflate(blur * 0.15 * copy);
                outline(PLAYER_BUFFER, matrix, shifted, color, alpha);
            }
        }
    }

    public static boolean isGlowing(Entity entity) {
        return glowColor(entity) >= 0;
    }

    // The outline colour of an entity sensed by Divine Sense, -1 if it is not sensed
    public static int glowColor(Entity entity) {
        if (!entity.level.isClientSide || Minecraft.getInstance().options.hideGui) {
            return -1;
        }
        Integer color = glowing.get(entity.getId());
        return color != null ? color : -1;
    }

    // A player of a higher realm hides their aura: blurred at the edge of the detection radius,
    // clear once they come a third of the way in
    private static double blur(Player observer, Player target, double distance, SenseProfile profile) {
        if (DivineSenseTechnique.getRealm(target) <= DivineSenseTechnique.getRealm(observer)) {
            return 0;
        }
        return smoothstep(profile.radius() * 2 / 3.0, profile.radius(), distance);
    }

    private static int entityColor(ClientLevel level, Entity entity) {
        if (entity instanceof Player player) {
            return playerColor(player);
        }
        if (entity instanceof Enemy) {
            return level.dimension() == Level.NETHER ? NETHER_HOSTILE_COLOR : HOSTILE_COLOR;
        }
        return CREATURE_COLOR;
    }

    // Players show the element of their core once they have one
    private static int playerColor(Player player) {
        var cultivation = CultivatorStats.getCultivatorStats(player).getCultivation();
        if (cultivation instanceof CoreFormingCultivation) {
            for (ResourceLocation elementId : cultivation.getElements()) {
                var element = Elements.getElement(elementId);
                if (element != null && !Elements.noElement.equals(elementId)) {
                    return element.color.getRGB() & 0xFFFFFF;
                }
            }
        }
        return PLAYER_COLOR;
    }

    private static int plantColor(BlockState state, float strength) {
        var entry = ClientPlantCatalog.get(state.getValue(ProceduralPlantBlock.SPECIES));
        if (entry == null) {
            return 0xFFFFFF;
        }
        ResourceLocation elementId = ResourceLocation.tryParse(entry.element);
        var element = elementId == null ? null : Elements.getElement(elementId);
        if (element == null) {
            return 0xFFFFFF;
        }
        int red = Math.round(Mth.lerp(strength, 255f, element.color.getRed()));
        int green = Math.round(Mth.lerp(strength, 255f, element.color.getGreen()));
        int blue = Math.round(Mth.lerp(strength, 255f, element.color.getBlue()));
        return (red << 16) | (green << 8) | blue;
    }

    // The plant's Qi, from 0 to 1
    private static float plantStrength(ClientLevel level, BlockPos pos, BlockState state) {
        // The block state supplies a stable fallback while block-entity data arrives.
        int growth = switch (state.getValue(ProceduralPlantBlock.TIER)) {
            case 3 ->
                1000;
            case 2 ->
                100;
            default ->
                0;
        };
        if (level.getBlockEntity(pos) instanceof ProceduralPlantBlockEntity plant) {
            growth = plant.getSpiritualGrowth();
        }
        // A logarithmic curve gives low-growth plants noticeable color quickly,
        // with diminishing gains toward full element color at maximum growth.
        double clampedGrowth = Mth.clamp((double) growth, 0, ProceduralPlantBlockEntity.MAX_SPIRITUAL_GROWTH);
        return (float) (Math.log1p(clampedGrowth)
                / Math.log1p(ProceduralPlantBlockEntity.MAX_SPIRITUAL_GROWTH));
    }

    private static void cross(Matrix4f matrix, AABB box, int color, int alpha, float scale) {
        // Three intersecting world-space axes, rather than a camera-facing marker.
        double x = (box.minX + box.maxX) * .5;
        double y = (box.minY + box.maxY) * .5;
        double z = (box.minZ + box.maxZ) * .5;
        double halfX = (box.maxX - box.minX) * .5 * scale;
        double halfY = (box.maxY - box.minY) * .5 * scale;
        double halfZ = (box.maxZ - box.minZ) * .5 * scale;
        line(PLANT_BUFFER, matrix, x - halfX, y, z, x + halfX, y, z, color, alpha);
        line(PLANT_BUFFER, matrix, x, y - halfY, z, x, y + halfY, z, color, alpha);
        line(PLANT_BUFFER, matrix, x, y, z - halfZ, x, y, z + halfZ, color, alpha);
    }

    private static void outline(BufferBuilder buffer, Matrix4f matrix, AABB box, int color, int alpha) {
        for (int corner = 0; corner < 8; corner++) {
            double x = (corner & 1) == 0 ? box.minX : box.maxX;
            double y = (corner & 2) == 0 ? box.minY : box.maxY;
            double z = (corner & 4) == 0 ? box.minZ : box.maxZ;
            if ((corner & 1) == 0) {
                line(buffer, matrix, x, y, z, box.maxX, y, z, color, alpha);
            }
            if ((corner & 2) == 0) {
                line(buffer, matrix, x, y, z, x, box.maxY, z, color, alpha);
            }
            if ((corner & 4) == 0) {
                line(buffer, matrix, x, y, z, x, y, box.maxZ, color, alpha);
            }
        }
    }

    private static void line(BufferBuilder buffer, Matrix4f matrix, double x, double y, double z,
            double endX, double endY, double endZ, int color, int alpha) {
        int red = (color >> 16) & 255, green = (color >> 8) & 255, blue = color & 255;
        buffer.vertex(matrix, (float) x, (float) y, (float) z).color(red, green, blue, alpha).endVertex();
        buffer.vertex(matrix, (float) endX, (float) endY, (float) endZ).color(red, green, blue, alpha).endVertex();
    }
}
