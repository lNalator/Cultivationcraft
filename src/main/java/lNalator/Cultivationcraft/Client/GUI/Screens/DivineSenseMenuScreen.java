package lNalator.Cultivationcraft.Client.GUI.Screens;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import lNalator.Cultivationcraft.Common.Qi.Techniques.DivineSenseTechnique;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

/**
 * Divine Sense radial menu, like the action menu of Dragon Block C: shown while its key is held, each click switches a
 * category ON or OFF, the centre switches them all. The mask is kept by the Divine Sense technique.
 */
public class DivineSenseMenuScreen extends Screen {

    private record Sector(int category, String name) {

    }

    private static final Sector[] SECTORS = {
            new Sector(DivineSenseTechnique.SENSE_QI_SOURCES, "cultivationcraft.divinesense.menu.qisources"),
            new Sector(DivineSenseTechnique.SENSE_BLOCKS, "cultivationcraft.divinesense.menu.blocks"),
            new Sector(DivineSenseTechnique.SENSE_CREATURES, "cultivationcraft.divinesense.menu.creatures"),
            new Sector(DivineSenseTechnique.SENSE_PLAYERS, "cultivationcraft.divinesense.menu.players"),
            new Sector(DivineSenseTechnique.SENSE_TRIBULATIONS, "cultivationcraft.divinesense.menu.tribulations")
    };

    private static final float CENTER_RADIUS = 22;
    private static final float INNER_RADIUS = 26;
    private static final float OUTER_RADIUS = 86;
    // Gap between two sectors, in degrees
    private static final float GAP = 2;
    private static final int SEGMENTS = 16;

    private static final int ON_COLOR = 0x91DCCD;
    private static final int OFF_COLOR = 0x303030;

    // Hovered: -1 for the centre, -2 for nothing
    private static final int CENTER = -1;
    private static final int NONE = -2;

    private final KeyMapping key;

    public DivineSenseMenuScreen(KeyMapping key) {
        super(Component.translatable("cultivationcraft.divinesense.menu"));
        this.key = key;
    }

    // Only for players with Divine Sense in their technique slots
    public static void open(KeyMapping key) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null && DivineSenseTechnique.find(mc.player) != null) {
            mc.setScreen(new DivineSenseMenuScreen(key));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // The key mapping is not updated while a screen is open: read the key itself
    private boolean keyHeld() {
        InputConstants.Key bound = key.getKey();
        long window = minecraft.getWindow().getWindow();

        if (bound.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
        }

        return InputConstants.isKeyDown(window, bound.getValue());
    }

    @Override
    public void tick() {
        if (!keyHeld() || DivineSenseTechnique.find(minecraft.player) == null) {
            onClose();
        }
    }

    private int hovered(double mouseX, double mouseY) {
        double dx = mouseX - width / 2.0, dy = mouseY - height / 2.0;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance <= CENTER_RADIUS) {
            return CENTER;
        }

        if (distance < INNER_RADIUS || distance > OUTER_RADIUS * 1.5) {
            return NONE;
        }

        // Clockwise from the top, sector 0 centred on the top
        double angle = Math.toDegrees(Math.atan2(dx, -dy));
        double span = 360.0 / SECTORS.length;

        return Mth.floor(Mth.positiveModulo(angle + span / 2, 360) / span) % SECTORS.length;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        DivineSenseTechnique sense = DivineSenseTechnique.find(minecraft.player);
        int target = hovered(mouseX, mouseY);

        if (button != 0 || sense == null || target == NONE) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int mask = sense.getSenseMask();

        if (target == CENTER) {
            mask = mask == DivineSenseTechnique.SENSE_ALL ? 0 : DivineSenseTechnique.SENSE_ALL;
        } else {
            mask ^= SECTORS[target].category();
        }

        sense.setSenseMask(mask);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));

        return true;
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        DivineSenseTechnique sense = DivineSenseTechnique.find(minecraft.player);

        if (sense == null) {
            return;
        }

        float centerX = width / 2f, centerY = height / 2f;
        int hovered = hovered(mouseX, mouseY);
        double span = 360.0 / SECTORS.length;
        Matrix4f matrix = poseStack.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < SECTORS.length; i++) {
            double start = i * span - span / 2 + GAP / 2, end = i * span + span / 2 - GAP / 2;
            ring(buffer, matrix, centerX, centerY, INNER_RADIUS, OUTER_RADIUS, start, end,
                    sense.senses(SECTORS[i].category()) ? ON_COLOR : OFF_COLOR, hovered == i ? 0.8f : 0.55f);
        }

        boolean all = sense.getSenseMask() == DivineSenseTechnique.SENSE_ALL;
        ring(buffer, matrix, centerX, centerY, 0, CENTER_RADIUS, 0, 360, all ? ON_COLOR : OFF_COLOR, hovered == CENTER ? 0.8f : 0.55f);

        Tesselator.getInstance().end();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();

        for (int i = 0; i < SECTORS.length; i++) {
            double angle = Math.toRadians(i * span);
            float labelRadius = (INNER_RADIUS + OUTER_RADIUS) / 2;
            int x = Math.round(centerX + (float) Math.sin(angle) * labelRadius);
            int y = Math.round(centerY - (float) Math.cos(angle) * labelRadius);
            boolean on = sense.senses(SECTORS[i].category());

            drawCenteredString(poseStack, font, Component.translatable(SECTORS[i].name()), x, y - 9, on ? 0xFFFFFF : 0x9A9A9A);
            drawCenteredString(poseStack, font, Component.translatable(on ? "cultivationcraft.divinesense.menu.on" : "cultivationcraft.divinesense.menu.off"),
                    x, y + 1, on ? 0x91DCCD : 0x9A9A9A);
        }

        drawCenteredString(poseStack, font, Component.translatable("cultivationcraft.divinesense.menu.all"), Math.round(centerX), Math.round(centerY) - 4, 0xFFFFFF);

        super.render(poseStack, mouseX, mouseY, partialTick);
    }

    // A ring section from start to end degrees, clockwise from the top
    private static void ring(BufferBuilder buffer, Matrix4f matrix, float centerX, float centerY, float inner, float outer,
            double start, double end, int color, float alpha) {
        int red = (color >> 16) & 255, green = (color >> 8) & 255, blue = color & 255, a = Math.round(255 * alpha);

        for (int segment = 0; segment < SEGMENTS; segment++) {
            double a0 = Math.toRadians(start + (end - start) * segment / SEGMENTS);
            double a1 = Math.toRadians(start + (end - start) * (segment + 1) / SEGMENTS);

            vertex(buffer, matrix, centerX, centerY, inner, a0, red, green, blue, a);
            vertex(buffer, matrix, centerX, centerY, outer, a0, red, green, blue, a);
            vertex(buffer, matrix, centerX, centerY, outer, a1, red, green, blue, a);
            vertex(buffer, matrix, centerX, centerY, inner, a1, red, green, blue, a);
        }
    }

    private static void vertex(BufferBuilder buffer, Matrix4f matrix, float centerX, float centerY, float radius, double angle,
            int red, int green, int blue, int alpha) {
        buffer.vertex(matrix, centerX + (float) Math.sin(angle) * radius, centerY - (float) Math.cos(angle) * radius, 0)
                .color(red, green, blue, alpha).endVertex();
    }
}
