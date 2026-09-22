package DaoOfModding.Cultivationcraft.Client.GUI.Screens;

import DaoOfModding.Cultivationcraft.Common.Containers.AlchemyCauldronMenu;
import DaoOfModding.Cultivationcraft.Common.Containers.AlchemyQi;
import DaoOfModding.Cultivationcraft.Cultivationcraft;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.lwjgl.glfw.GLFW;

public class AlchemyCauldronScreen extends AbstractContainerScreen<AlchemyCauldronMenu> {
    private static final ResourceLocation FRAME = new ResourceLocation("textures/gui/advancements/window.png");
    private static final ResourceLocation FURNACE = new ResourceLocation("textures/gui/container/furnace.png");
    private static final int PANEL_X = 9, PANEL_Y = 18;
    private static final int CONTENT_WIDTH = 320, CONTENT_HEIGHT = 264;
    private static final int ELEMENT_X = 150, ELEMENT_Y = 88, ELEMENT_SPACING = 40;
    private double zoom = 1, panX, panY;
    private boolean panning;

    public AlchemyCauldronScreen(AlchemyCauldronMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        imageWidth = Math.min(360, width - 12);
        imageHeight = Math.min(310, height - 12);
        super.init();
        clampPan();
    }

    private int panelWidth() { return imageWidth - 18; }
    private int panelHeight() { return imageHeight - 27; }
    private double virtualX(double x) { return leftPos + (x - leftPos - PANEL_X - panX) / zoom; }
    private double virtualY(double y) { return topPos + (y - topPos - PANEL_Y - panY) / zoom; }
    private double screenX(double x) { return leftPos + PANEL_X + panX + (x - leftPos) * zoom; }
    private double screenY(double y) { return topPos + PANEL_Y + panY + (y - topPos) * zoom; }

    private boolean inPanel(double x, double y) {
        return x >= leftPos + PANEL_X && x < leftPos + PANEL_X + panelWidth()
                && y >= topPos + PANEL_Y && y < topPos + PANEL_Y + panelHeight();
    }

    private boolean inWindow(double x, double y) {
        return x >= leftPos && x < leftPos + imageWidth && y >= topPos && y < topPos + imageHeight;
    }

    private void clampPan() {
        double extraX = panelWidth() - CONTENT_WIDTH * zoom;
        double extraY = panelHeight() - CONTENT_HEIGHT * zoom;
        panX = extraX >= 0 ? extraX / 2 : Mth.clamp(panX, extraX, 0);
        panY = extraY >= 0 ? extraY / 2 : Mth.clamp(panY, extraY, 0);
    }

    @Override
    public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        fill(pose, leftPos + PANEL_X, topPos + PANEL_Y,
                leftPos + PANEL_X + panelWidth(), topPos + PANEL_Y + panelHeight(), 0xFF000000);
        double guiScale = minecraft.getWindow().getGuiScale();
        RenderSystem.enableScissor((int) ((leftPos + PANEL_X) * guiScale),
                (int) (minecraft.getWindow().getHeight() - (topPos + PANEL_Y + panelHeight()) * guiScale),
                (int) (panelWidth() * guiScale), (int) (panelHeight() * guiScale));
        // Transform the model-view matrix, including vanilla's slot items and drag previews.
        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.translate(leftPos + PANEL_X + panX, topPos + PANEL_Y + panY, 0);
        modelView.scale((float) zoom, (float) zoom, 1);
        modelView.translate(-leftPos, -topPos, 0);
        RenderSystem.applyModelViewMatrix();
        try {
            super.render(pose, (int) virtualX(mouseX), (int) virtualY(mouseY), partialTick);
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.disableScissor();
        }
        RenderSystem.disableDepthTest();
        drawFrame(pose);
        font.draw(pose, title, leftPos + 8, topPos + 6, 0x404040);
        RenderSystem.enableDepthTest();
        // Tooltips remain readable at normal screen scale and are not clipped to the panel.
        if (inPanel(mouseX, mouseY)) {
            renderTooltip(pose, mouseX, mouseY);
            if (hoveredSlot == null && menu.getCarried().isEmpty()) drawPanelTooltip(pose, mouseX, mouseY);
        } else {
            hoveredSlot = null;
            if (!menu.getCarried().isEmpty()) {
                itemRenderer.renderAndDecorateItem(menu.getCarried(), mouseX - 8, mouseY - 8);
                itemRenderer.renderGuiItemDecorations(font, menu.getCarried(), mouseX - 8, mouseY - 8);
            } else if (inWindow(mouseX, mouseY)) {
                renderTooltip(pose, Component.translatable("cultivationcraft.gui.alchemy.controls"), mouseX, mouseY);
            }
        }
    }

    private void drawFrame(PoseStack pose) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, FRAME);
        // Nine-slice the original 252x140 advancement frame, keeping its borders pixel-sized.
        frameSlice(pose, 0, 0, 9, 18, 0, 0, 9, 18);
        frameSlice(pose, 9, 0, imageWidth - 18, 18, 9, 0, 234, 18);
        frameSlice(pose, imageWidth - 9, 0, 9, 18, 243, 0, 9, 18);
        frameSlice(pose, 0, 18, 9, panelHeight(), 0, 18, 9, 113);
        frameSlice(pose, imageWidth - 9, 18, 9, panelHeight(), 243, 18, 9, 113);
        frameSlice(pose, 0, imageHeight - 9, 9, 9, 0, 131, 9, 9);
        frameSlice(pose, 9, imageHeight - 9, imageWidth - 18, 9, 9, 131, 234, 9);
        frameSlice(pose, imageWidth - 9, imageHeight - 9, 9, 9, 243, 131, 9, 9);
    }

    private void frameSlice(PoseStack pose, int x, int y, int w, int h, int u, int v, int sw, int sh) {
        blit(pose, leftPos + x, topPos + y, w, h, u, v, sw, sh, 256, 256);
    }

    @Override
    protected void renderBg(PoseStack pose, float partialTick, int mouseX, int mouseY) {
        for (Slot slot : menu.slots) drawSlot(pose, leftPos + slot.x, topPos + slot.y);
        for (int i = 0; i < 3; i++) drawSlot(pose, leftPos + 164 + i * 40, topPos + 30);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, FURNACE);
        // Vanilla's unlit furnace flame: decorative until alchemy recipes are implemented.
        blit(pose, leftPos + 51, topPos + 94, 56, 36, 14, 14);
        for (int i = 0; i < AlchemyQi.ELEMENTS.size(); i++) {
            ResourceLocation element = AlchemyQi.ELEMENTS.get(i);
            RenderSystem.setShaderTexture(0, new ResourceLocation(Cultivationcraft.MODID,
                    "textures/gui/plants/element_icons/" + element.getPath() + ".png"));
            blit(pose, leftPos + ELEMENT_X + (i % 4) * ELEMENT_SPACING,
                    topPos + ELEMENT_Y + (i / 4) * 38, 0, 0, 16, 16, 16, 16);
        }
    }

    private void drawSlot(PoseStack pose, int x, int y) {
        fill(pose, x - 1, y - 1, x + 17, y + 17, 0xFFDDDDDD);
        fill(pose, x - 1, y - 1, x + 16, y + 16, 0xFF373737);
        fill(pose, x, y, x + 16, y + 16, 0xFF737373);
    }

    @Override
    protected void renderLabels(PoseStack pose, int mouseX, int mouseY) {
        font.draw(pose, Component.translatable("cultivationcraft.gui.alchemy.ingredients"), 30, 14, 0xDDDDDD);
        font.draw(pose, Component.translatable("cultivationcraft.gui.alchemy.results"), 164, 14, 0xDDDDDD);
        font.draw(pose, Component.translatable("cultivationcraft.gui.alchemy.elemental_qi"), 150, 70, 0xDDDDDD);
        for (int i = 0; i < AlchemyQi.ELEMENTS.size(); i++) {
            String value = Integer.toString(menu.getElementalQi(i));
            int color = menu.getElementalQi(i) == 0 ? 0x777777 : 0xFFFFFF;
            float scale = Math.min(1f, 34f / Math.max(1, font.width(value)));
            pose.pushPose();
            pose.translate(ELEMENT_X + (i % 4) * ELEMENT_SPACING + 8, ELEMENT_Y + (i / 4) * 38 + 19, 0);
            pose.scale(scale, scale, 1);
            font.draw(pose, value, -font.width(value) / 2f, 0, color);
            pose.popPose();
        }
        font.draw(pose, playerInventoryTitle, AlchemyCauldronMenu.INVENTORY_X, 160, 0xDDDDDD);
    }

    private void drawPanelTooltip(PoseStack pose, int mouseX, int mouseY) {
        double x = virtualX(mouseX) - leftPos, y = virtualY(mouseY) - topPos;
        for (int i = 0; i < AlchemyQi.ELEMENTS.size(); i++) {
            int ex = ELEMENT_X + (i % 4) * ELEMENT_SPACING, ey = ELEMENT_Y + (i / 4) * 38;
            if (x >= ex - 8 && x < ex + 24 && y >= ey && y < ey + 30) {
                renderTooltip(pose, Component.translatable("cultivationcraft.gui.alchemy.qi_total",
                        Component.translatable(AlchemyQi.ELEMENTS.get(i).getPath()), menu.getElementalQi(i)), mouseX, mouseY);
                return;
            }
        }
        if (x >= 163 && x < 261 && y >= 29 && y < 47) {
            renderTooltip(pose, Component.translatable("cultivationcraft.gui.alchemy.results_reserved"), mouseX, mouseY);
        }
    }

    @Override
    protected boolean isHovering(int x, int y, int w, int h, double mouseX, double mouseY) {
        return inPanel(screenX(mouseX), screenY(mouseY)) && super.isHovering(x, y, w, h, mouseX, mouseY);
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        return !inWindow(screenX(mouseX), screenY(mouseY));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (inWindow(mouseX, mouseY) && !inPanel(mouseX, mouseY)) return true;
        double x = virtualX(mouseX), y = virtualY(mouseY);
        boolean overSlot = menu.slots.stream().anyMatch(slot -> isHovering(slot.x, slot.y, 16, 16, x, y));
        if (inPanel(mouseX, mouseY) && menu.getCarried().isEmpty() && !isQuickCrafting
                && !overSlot && (button == 2 || button == 0)) {
            panning = true;
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (panning) {
            panX += dx;
            panY += dy;
            clampPan();
            return true;
        }
        return super.mouseDragged(virtualX(mouseX), virtualY(mouseY), button, dx / zoom, dy / zoom);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (panning) { panning = false; return true; }
        return super.mouseReleased(virtualX(mouseX), virtualY(mouseY), button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!inPanel(mouseX, mouseY)) return super.mouseScrolled(mouseX, mouseY, delta);
        if (isQuickCrafting) return true;
        if (hasShiftDown()) {
            panY += delta * 20;
        } else {
            double contentX = virtualX(mouseX) - leftPos, contentY = virtualY(mouseY) - topPos;
            zoom = Mth.clamp(zoom * Math.pow(1.1, delta), 0.75, 1.75);
            panX = mouseX - leftPos - PANEL_X - contentX * zoom;
            panY = mouseY - topPos - PANEL_Y - contentY * zoom;
        }
        clampPan();
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_HOME && menu.getCarried().isEmpty() && !isQuickCrafting) {
            zoom = 1;
            panX = panY = 0;
            clampPan();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }
}
